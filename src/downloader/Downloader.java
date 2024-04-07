package downloader;

import org.jsoup.Jsoup;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;

import org.jsoup.nodes.Element;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.util.HashMap;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.concurrent.LinkedBlockingQueue;
import java.rmi.registry.Registry;
import java.time.LocalDateTime;
import java.net.ConnectException;
import java.rmi.Naming;
import java.rmi.registry.LocateRegistry;
import rmi.RMIGatewayDownloadersInterface;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;

public class Downloader {

    private int downloaderId, multicastBarrelsPort, ackReturnTimer;

    String ackType = "type1";

    private InetAddress group = null;
    private MulticastSocket socket = null;

    private LinkedBlockingQueue<String> receivedMessages = new LinkedBlockingQueue<>();
    private LinkedBlockingQueue<String> toBeSentMessages = new LinkedBlockingQueue<>();
    private LinkedBlockingQueue<String> missingAcksQueue = new LinkedBlockingQueue<>();
    private HashMap<Integer, Boolean> ackedMessages = new HashMap<>();

    private void receiver(){

        byte[] bytes = new byte[1024];
        String string;

        while (true) {
            try {
                DatagramPacket packet = new DatagramPacket(bytes, bytes.length);
                socket.receive(packet);
                string = new String(packet.getData(), 0, packet.getLength());
                System.out.println("Received: " + string);
                receivedMessages.put(string);

            } catch (Exception e){
                e.printStackTrace();
            }
        }
    }

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
                        ackedMessages.put(messageId, true);
                    }
                }

            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    // private void acksChecker(){

    //     System.out.println(LocalDateTime.now() + " : Acks Checker thread started!");

    //     int messageId;
    //     long timeStamp, timeElapsed;
    //     String message;

    //     while (true){

    //         try {

    //             message = missingAcksQueue.take(); // pop a missing ack from the queue
    //             // separate the timeStamp from the messageId

    //             // separate the timestamp and the message id
    //             String[] tokens = message.split(" ");
    //             timeStamp = Long.parseLong(tokens[0]);
    //             messageId = Integer.parseInt(tokens[1]);

    //             if(!ackedMessages.containsKey(messageId)){ // if the message still hasn't arrived

    //                 // check elapsed time
    //                 if((timeElapsed = (System.currentTimeMillis() - timeStamp))/100 >= ackReturnTimer/100){ // if the waiting time has passed
        
    //                     // create a recover request
    //                     String message = recoveryType + "\n" + barrelId + "\n" + messageId;
    //                     toBeSentMessages.put(message); // add the message to the toBeSentMessages queue
    //                     System.out.println(LocalDateTime.now() + " : Recovery Timer expired, the recover request for the message with the id " + messageId + " was added to the toBeSentMessages queue");

    //                     // add the new timestamp and message id to the missingAcksQueue queue so it can be checked again
    //                     missingAcksQueue.put(System.currentTimeMillis() + " " + messageId);

    //                 } else{ // if the waiting time hasn't passed
                        
    //                     Thread.sleep(ackReturnTimer - timeElapsed);

    //                     // check if the message has already arrived
    //                     if(!ackedMessages.containsKey(messageId)){ // if the message still hasn't arrived
        
    //                         // create a recover request
    //                         String message = recoveryType + "\n" + barrelId + "\n" + messageId;
    //                         toBeSentMessages.put(message); // add the message to the toBeSentMessages queue
    //                         System.out.println(LocalDateTime.now() + " : Recovery Timer expired, the recover request for the message with the id " + messageId + " was added to the toBeSentMessages queue");

    //                         // add the new timestamp and message id to the missingAcksQueue queue so it can be checked again
    //                         missingAcksQueue.put(System.currentTimeMillis() + " " + messageId);
    //                     }// else the message has already arrived
    //                 }
    //             }// else the message has already arrived

    //         } catch (InterruptedException e) {
    //             e.printStackTrace();
    //         }
    //     }
    // }

    private void sender(){

        byte[] bytes;
        String string;

        while (true) {

            try {
                string = toBeSentMessages.take();
                bytes = string.getBytes(); // Convert the object to string and then to bytes and put it in the buffer
                System.out.println("Sending: " + string);
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

    public static void main(String args[]) {

        int maxBytesTitle, maxBytesCitation, maxBytesTokens, maxBytesLinks, currentBytesLinks;
        int multicastDownloadersPort, multticastTTL;
        String host, registryName, multicastAddress, multicastInterface;
        SocketAddress socketAddress = null;
        NetworkInterface networkInterface = null;
        
        try {// Load configurations

            Downloader downloader = new Downloader();

            downloader.downloaderId = Integer.parseInt(args[0]);
            Properties prop = new Properties();
            InputStream input = new FileInputStream(args[1]);
            prop.load(input);

            
            host = prop.getProperty("host");
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
                     
                    while (true) {
                        try {//Process the URL
        
                            System.out.println("-------------------------");
        
                            URLData urlData = new URLData();
                            urlData.pageURL = gateway.getNextUrl();
                            urlData.downloaderId = downloader.downloaderId;
                            
                            System.out.println("Message number: " + urlData.messageId);
                            
                            System.out.println("URL: " + urlData.pageURL);
                            
                            // Process the URL...
                            Document doc = Jsoup.connect(urlData.pageURL).get();
                            
                            // ------------------------- Title -------------------------
                            String string = doc.title();
                            byte[] bytes = string.getBytes();
                            int limit = Math.min(maxBytesTitle, bytes.length);
                            string = new String(bytes, 0, limit);
                            
                            urlData.title = string.toLowerCase().replace("\n", " ");
                            System.out.println("Title: " + urlData.title);
                            
                            // ------------------------- Text Citation -------------------------
                            
                            Elements paragraphs = doc.select("p");
                            bytes = paragraphs.text().getBytes();
                            limit = Math.min(maxBytesCitation, bytes.length);
                            string = new String(bytes, 0, limit);
                            
                            urlData.textCitation = string.toLowerCase().replace("\n", " ");
                            
                            System.out.println("Text Citation: " + urlData.textCitation);
                            
                            // ------------------------- Tokens -------------------------
                            
                            doc.select("button, .slide").remove(); // remove text from buttons and slides
                            string = doc.text();
                            bytes = string.getBytes();
                            limit = Math.min(maxBytesTokens, bytes.length);
                            urlData.tokens = new String(bytes, 0, limit);
                            urlData.tokens = urlData.tokens.toLowerCase().replace("\n", " ");
                            System.out.println("Tokens: " + urlData.tokens);
                            
                            
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
                                System.out.println(link.text() + "\n" + link.attr("abs:href") + "\n");
                            }
                            
                            urlData.links = coupleLinks.toString().toLowerCase().replace("\n", " ");
                            
                            // TODO send data by RELIABLE MULTICAST to the Index Storage Barrels
                            
                            // ------------------------- Multicast -------------------------
                            urlData.messageId = gateway.getNewMessageId(); // Increment the message id
                            // TODO: maybe add checksum to the message
                            
                            downloader.toBeSentMessages.put(urlData.toString());

                            // Send data to the Index Storage Barrels by RELIABLE MULTICAST
                            
                            // ------------------------- Scanner -------------------------
                            
                        }   catch (ConnectException e) {
                                System.err.println("Connection failed: " + e.getMessage());
                        }
                        catch (IOException e) {
                            e.printStackTrace();
                        } 
                    }
                    // ------------------------- CROWLER -------------------------
                            
                    
                } catch (Exception e) {
                    e.printStackTrace();
                }

                // ------------------------- REGISTRY -------------------------

            } catch (IOException e) {
                System.out.println("Error creating multicast socket: " + e.getMessage());
            } finally {
                if (downloader.socket != null) {
                    try {
                        downloader.socket.leaveGroup(socketAddress, networkInterface);
                        downloader.socket.close();
                    } catch (IOException e) {
                        System.out.println("Error closing multicast socket: " + e.getMessage());
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
