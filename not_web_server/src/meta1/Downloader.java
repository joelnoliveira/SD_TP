package meta1;

import org.jsoup.Jsoup;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.SocketAddress;

import org.jsoup.nodes.Element;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import meta1.RMIGatewayDownloadersInterface;
import meta1.URLData;

import java.util.HashMap;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.concurrent.LinkedBlockingQueue;
import java.time.LocalDateTime;
import java.net.ConnectException;
import java.rmi.Naming;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;

/**
 * The Downloader class represents a downloader that sends and receives messages to and from barrels.
 * It utilizes multicast communication to send messages and receive acknowledgments.
 * The class contains methods for receiving messages, processing messages, checking acknowledgments,
 * and sending messages to barrels.
 */
public class Downloader {

    private int downloaderId, multicastBarrelsPort, ackReturnTimer;

    String ackType = "type1";

    private InetAddress group = null;
    private MulticastSocket socket = null;

    private LinkedBlockingQueue<String> receivedMessages = new LinkedBlockingQueue<>();
    private LinkedBlockingQueue<String> toBeSentMessages = new LinkedBlockingQueue<>();
    private LinkedBlockingQueue<String> missingAcksQueue = new LinkedBlockingQueue<>();
    private HashMap<Integer, String> messages = new HashMap<>(); // messages that have been sent to the barrels, when ack is received, remove from the list

    /**
     * Receives incoming datagrams and stores the received messages in a shared data structure.
     */
    private void receiver(){

        byte[] bytes = new byte[1024];
        String string;

        while (true) {
            try {
                DatagramPacket packet = new DatagramPacket(bytes, bytes.length);
                socket.receive(packet);
                string = new String(packet.getData(), 0, packet.getLength());
                // System.out.println("Received: " + string);
                receivedMessages.put(string);

            } catch (Exception e){
                e.printStackTrace();
            }
        }
    }

    /**
     * Processes the received messages and handles acknowledgments.
     */
    private void messageProcessor(){

        String message, type;
        System.out.println(LocalDateTime.now() + " : Message Processor thread started!");

        while (true){

            try {

                message = receivedMessages.take();
                //check the recived messages, pop the first message from the queue
                
                StringTokenizer st = new StringTokenizer(message, "\n");

                type = st.nextToken();

                if(type.equals(ackType)){ //if the message is an ack
                    //check if it is from this downloader
                    
                    int downloaderId = Integer.parseInt(st.nextToken());
                    int messageId = Integer.parseInt(st.nextToken());

                    if(downloaderId == this.downloaderId){
                        //remove the message from the list of messages that have been sent to the barrels
                        messages.remove(messageId);
                        System.out.println(LocalDateTime.now() + " : Acknowledgment received for the message with the id " + messageId);
                    }// else the message is not from this downloader, so ignore it
                }

            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Monitors the acknowledgment status of messages and takes appropriate actions based on the elapsed time.
     * This method runs in a separate thread and continuously checks for missing acknowledgments.
     * If an acknowledgment is missing for a certain amount of time, the corresponding message is added to the toBeSentMessages queue for retransmission.
     * If an acknowledgment is still missing after a recovery timer expires, a recovery request is added to the toBeSentMessages queue.
     */
    private void acksChecker(){

        System.out.println(LocalDateTime.now() + " : Acks Checker thread started!");

        int messageId;
        long timeStamp, timeElapsed;
        String message;

        while (true){

            try {

                message = missingAcksQueue.take(); // pop a missing ack from the queue
                // separate the timeStamp from the messageId

                // separate the timestamp and the message id
                String[] tokens = message.split(" ");
                timeStamp = Long.parseLong(tokens[0]);
                messageId = Integer.parseInt(tokens[1]);

                if(messages.containsKey(messageId)){ // if the acknoledgment still hasn't arrived, because the message is still in the messages list

                    // check elapsed time
                    if((timeElapsed = (System.currentTimeMillis() - timeStamp))/100 >= ackReturnTimer/100){ // if the waiting time has passed
        
                        toBeSentMessages.put(messages.get(messageId)); // add the message to the toBeSentMessages queue
                        System.out.println(LocalDateTime.now() + " : The acknlodgemnt timer expired, the message with the id " + messageId + " was added to the toBeSentMessages queue");

                        // add the new timestamp and message id to the missingAcksQueue queue so it can be checked again
                        missingAcksQueue.put(System.currentTimeMillis() + " " + messageId);

                    } else{ // if the waiting time hasn't passed
                        
                        Thread.sleep(ackReturnTimer - timeElapsed);

                        // check if the ack has already arrived
                        if(messages.containsKey(messageId)){ // if the ack still hasn't arrived
        
                            toBeSentMessages.put(messages.get(messageId)); // add the message to the toBeSentMessages queue
                            System.out.println(LocalDateTime.now() + " : Recovery Timer expired, the recover request for the message with the id " + messageId + " was added to the toBeSentMessages queue");

                            // add the new timestamp and message id to the missingAcksQueue queue so it can be checked again
                            missingAcksQueue.put(System.currentTimeMillis() + " " + messageId);
                        }// else the ack has already arrived
                    }
                }// else the ack has already arrived

            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Sends messages to the barrels.
     */
    private void sender(){

        byte[] bytes;
        int messageId;
        String string;

        while (true) {

            try {
                string = toBeSentMessages.take();

                messageId = Integer.parseInt(string.split("\n")[2]);

                // check if the message is already in the list of messages that have been sent to the barrels, if not add it
                if(!messages.containsKey(messageId)){

                    messages.put(messageId, string);
                    
                    // add the timestamp and the message id to the missingAcksQueue queue so it can be checked
                    missingAcksQueue.put(System.currentTimeMillis() + " " + messageId);
                }


                bytes = string.getBytes(); // Convert the object to string and then to bytes and put it in the buffer
                // System.out.println("Sending: " + string);
                // Send a message
                DatagramPacket packet = new DatagramPacket(bytes, bytes.length, group, multicastBarrelsPort);
                socket.send(packet);

            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (Exception e){
                e.printStackTrace();
            }
        }
    }

    /**
     * The main method is the entry point of the program.
     * It loads configurations, creates a multicast socket, and initializes threads for sending and receiving messages.
     * It continuously processes URLs by connecting to them, extracting information, and sending the data to the Index Storage Barrels.
     * 
     * @param args An array of command-line arguments. The first argument is the downloader ID, and the second argument is the path to the configuration file.
     */
    public static void main(String args[]) {

        int maxBytesTitle, maxBytesCitation, maxBytesTokens, maxBytesLinks, currentBytesLinks;
        int multicastDownloadersPort, multticastTTL;
        String registryName, multicastAddress, multicastInterface;
        SocketAddress socketAddress = null;
        NetworkInterface networkInterface = null;
        
        try {// Load configurations

            Downloader downloader = new Downloader();

            downloader.downloaderId = Integer.parseInt(args[0]);
            Properties prop = new Properties();
            InputStream input = new FileInputStream(args[1]);
            prop.load(input);
            
            registryName = prop.getProperty("registryName");
            maxBytesTitle = Integer.parseInt(prop.getProperty("maxBytesTitle"));
            maxBytesCitation = Integer.parseInt(prop.getProperty("maxBytesCitation"));
            maxBytesTokens = Integer.parseInt(prop.getProperty("maxBytesTokens"));
            maxBytesLinks = Integer.parseInt(prop.getProperty("maxBytesLinks"));

            multicastDownloadersPort = Integer.parseInt(prop.getProperty("multicastDownloadersPort"));
            downloader.multicastBarrelsPort = Integer.parseInt(prop.getProperty("multicastBarrelsPort"));
            multicastAddress = prop.getProperty("multicastAddress");
            multicastInterface = prop.getProperty("multicastInterface");
            multticastTTL = Integer.parseInt(prop.getProperty("multicastTTL"));
            downloader.ackReturnTimer = Integer.parseInt(prop.getProperty("ackReturnTimer"));


            System.out.println(LocalDateTime.now() + " : Configurations sucessfully loaded!");
            
            try {// Create a multicast socket

                downloader.socket = new MulticastSocket(multicastDownloadersPort);
                System.out.println(LocalDateTime.now() + " : Multicast socket created!");

                downloader.group = InetAddress.getByName(multicastAddress);
                socketAddress = new InetSocketAddress(downloader.group, downloader.multicastBarrelsPort);
                networkInterface = NetworkInterface.getByName(multicastInterface);
                downloader.socket.joinGroup(socketAddress, networkInterface);
                downloader.socket.setTimeToLive(multticastTTL);

                try {// Create the registry

                    // Registry registry = LocateRegistry.getRegistry(host);
                    System.out.println(LocalDateTime.now() + " : Registry found!");
        
                    // Look up the registry for the remote object
                    RMIGatewayDownloadersInterface gateway = (RMIGatewayDownloadersInterface) Naming.lookup(registryName);
                    System.out.println(LocalDateTime.now() + " : Gateway object retrieved!");

                    // -------- Threads --------
                    new Thread(() -> {
                        downloader.sender(); // Send messages to the Index Storage Barrels
                    }).start();

                    new Thread(() -> {
                        downloader.receiver(); // Recives acks from the Index Storage Barrels
                    }).start();
                    
                    new Thread(() -> {
                        downloader.acksChecker(); // Check if the acks have arrived
                    }).start();

                    new Thread(() -> {
                        downloader.messageProcessor(); // Process the received messages
                    }).start();
                     
                    String url = "";
                    while (true) {
                        try {//Process the URL
        
                            // System.out.println("-------------------------");
        
                            URLData urlData = new URLData();
                            urlData.pageURL = gateway.getNextUrl();
                            url = urlData.pageURL;

                            System.out.println(LocalDateTime.now() + " : Processing URL: " + url);

                            urlData.downloaderId = downloader.downloaderId;
                            
                            // System.out.println("Message number: " + urlData.messageId);
                            
                            // System.out.println("URL: " + urlData.pageURL);
                            
                            // Process the URL...
                            Document doc = Jsoup.connect(urlData.pageURL).get();
                            
                            // ------------------------- Title -------------------------
                            String string = doc.title();
                            byte[] bytes = string.getBytes();
                            int limit = Math.min(maxBytesTitle, bytes.length);
                            string = new String(bytes, 0, limit);
                            
                            urlData.title = string.toLowerCase().replace("\n", " ");
                            // System.out.println("Title: " + urlData.title);
                            
                            // ------------------------- Text Citation -------------------------
                            
                            Elements paragraphs = doc.select("p");
                            bytes = paragraphs.text().getBytes();
                            limit = Math.min(maxBytesCitation, bytes.length);
                            string = new String(bytes, 0, limit);
                            
                            urlData.textCitation = string.toLowerCase().replace("\n", " ");
                            
                            // System.out.println("Text Citation: " + urlData.textCitation);
                            
                            // ------------------------- Tokens -------------------------
                            
                            doc.select("button, .slide").remove(); // remove text from buttons and slides
                            string = doc.text();
                            bytes = string.getBytes();
                            limit = Math.min(maxBytesTokens, bytes.length);
                            urlData.tokens = new String(bytes, 0, limit);
                            urlData.tokens = urlData.tokens.toLowerCase().replace("\n", " ");
                            // System.out.println("Tokens: " + urlData.tokens);
                            
                            // if there was no tokens nor text citation, the page is not valid so we don't send it
                            if(urlData.tokens.length() == 0 && urlData.textCitation.length() == 0){
                                System.out.println(LocalDateTime.now() + "Error The page " + urlData.pageURL + " is not valid, it will not be sent to the barrels");
                                continue;
                            }
                                
                            // ------------------------- Links -------------------------
                            
                            Elements links = doc.select("a[href]");
                            StringBuilder coupleLinks = new StringBuilder();
                            currentBytesLinks = 0;
                            
                            for (Element link : links){
                                
                                // Check if the sum of the bytes of the links is greater than the limit
                                if(link.attr("abs:href").getBytes().length + currentBytesLinks > maxBytesLinks)break;
                                else if(currentBytesLinks > 0)coupleLinks.append(" ");
                                
                                currentBytesLinks += link.attr("abs:href").getBytes().length;
                                coupleLinks.append(link.attr("abs:href"));
                                
                                gateway.addUrl(link.attr("abs:href"));
                                // System.out.println(link.text() + "\n" + link.attr("abs:href") + "\n");
                            }
                            
                            urlData.links = coupleLinks.toString().replace("\n", " ");
                            
                            // ------------------------- Multicast -------------------------
                            urlData.messageId = gateway.getNewMessageId(); // Increment the message id
                            // TODO: maybe add checksum to the message
                            
                            downloader.toBeSentMessages.put(urlData.toString());

                            // Send data to the Index Storage Barrels by RELIABLE MULTICAST
                            
                            // ------------------------- Scanner -------------------------
                            
                        }   catch (ConnectException e) {
                                System.out.println(LocalDateTime.now() + " : Error Connection refused + " + e.getMessage());
                                
                            }
                        catch (Exception e) {
                            e.printStackTrace();
                            System.out.println(LocalDateTime.now() + " : Error processing URL: " + url + " " + e.getMessage());
                        } 
                    }
                    // ------------------------- CROWLER -------------------------
                            
                    
                } catch (Exception e) {
                    e.printStackTrace();
                }

                // ------------------------- REGISTRY -------------------------

            } catch (IOException e) {
                System.out.println(LocalDateTime.now() + "Error creating multicast socket: " + e.getMessage());
            } finally {
                if (downloader.socket != null) {
                    try {
                        downloader.socket.leaveGroup(socketAddress, networkInterface);
                        downloader.socket.close();
                    } catch (IOException e) {
                        System.out.println(LocalDateTime.now() + "Error closing multicast socket: " + e.getMessage());
                    }
                }
            }
            // ------------------------- MULTICAST -------------------------

        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
    }
}
