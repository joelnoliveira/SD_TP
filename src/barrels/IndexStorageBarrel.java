package barrels;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.RandomAccessFile;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.SocketAddress;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Scanner;
import java.util.StringTokenizer;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;

import downloader.URLData;

public class IndexStorageBarrel extends Thread{

    private static int barrelId;
    private int processedMessages = 0; // The number of messages processed from the downloader
    private int contiguosMessages = 0; // The number of contiguos messages received from the downloaders

    // Processed data
    private class URL{

        private int id = 0,relevance = 0; // Number of links that point to this url
        private String title, textCitation;
        private HashSet<Integer> links = new HashSet<>(); // The id (document id) of the links that points to this url
    }

    // Messages
    private Map<Integer, String> messagesMap = new HashMap<>(); // The map that will store the URLData string messages received from the downloaders during a certain length, so others barrels can recover lost messages
    private Map<Integer, String> urlMap = new HashMap<>(); // Makes the correspondence between a number (aka doucument id) and a url, even tough the processed data is identified by url, the index will help save space when regestering for each url the links that point to it
    private Map<String, URL> urlDataMap = new HashMap<>(); // Makes the correspondence between a url and its data (the URLData object)
    private Map<String, HashSet<Integer>> index = new HashMap<>(); // makes the correspondence between a word and the urls that contain it
    
    // bloking queues
    private LinkedBlockingQueue<String> receivedMessages = new LinkedBlockingQueue<>();
    private LinkedBlockingQueue<Integer> dataToProcess = new LinkedBlockingQueue<>();
    private LinkedBlockingQueue<String> recoversToRequest = new LinkedBlockingQueue<>();

    private ObjectOutputStream oos;
    private final Semaphore diskSemaphore = new Semaphore(1);


    public void sender(){

        // Pop the messageId from the recoversToRequest queue
        // 
        // Send a recover request to the other barrels
    } 

    synchronized void processData(){ // whis one and search should be synchronized

        int downloaderId, messageId, id, currentPageId;
        String data = null;
        URL url = null;
        
        try {
            
            while(true){ // get the message id from the queue

                messageId = dataToProcess.take();

                // TODO add syncronized to were its needed to use threads

                // Process the data
                // Get the URLData object from the message
                URLData urlData = new URLData();
                StringTokenizer tokens = new StringTokenizer(messagesMap.get(messageId), "\n"); // use only "\n" as delimiter
                System.out.println(LocalDateTime.now() + " : The message with the id " + messageId + " is being processed");
                tokens.nextToken(); // Skip the message type
                tokens.nextToken(); // Skip the downloaderId
                tokens.nextToken(); // Skip the messageId
                urlData.pageURL = tokens.nextToken();
                urlData.title = tokens.nextToken();
                urlData.textCitation = tokens.nextToken();
                urlData.tokens = tokens.nextToken();
                if(tokens.hasMoreTokens())urlData.links = tokens.nextToken();
                else {
                    urlData.links = urlData.tokens;
                    urlData.tokens = urlData.textCitation;
                    urlData.textCitation = ""; // the textCitation is empty
                }

                id = urlMap.size(); // Document id
                tokens = new StringTokenizer(urlData.links);

                String link;

                // Check if the url is already in the urlDataMap
                if(urlDataMap.get(urlData.pageURL) == null){ // if it is not

                    // create a new URL object
                    url = new URL();
                    url.id = currentPageId = id++;
                    url.relevance = 0; // the relevance is 0 because the url is being added for the first time
                    url.title = urlData.title;
                    url.textCitation = urlData.textCitation;
                    urlMap.put(id, urlData.pageURL); // add the page url to the urlMap
                    urlDataMap.put(urlData.pageURL, url); // add the page url to the urlDataMap

                }else{// the url is already in the urlDataMap

                    url = urlDataMap.get(urlData.pageURL);
                    currentPageId = urlDataMap.get(urlData.pageURL).id;
                    
                    // Check if the title and by consequence the textCitation are there, if not add them
                    if(url.title == null){
                        url.title = urlData.title;
                        url.textCitation = urlData.textCitation;
                    }
                }

                while(tokens.hasMoreTokens()){ // process the links

                    link = tokens.nextToken();

                    // check if the referenced link is already in the urlDataMap and by consequence in the urlMap
                    if(urlDataMap.get(link) == null){ // if it is not

                        // create a new URL object
                        url = new URL();
                        url.relevance = 1; // the relevance is 1 because the link is being added for the first time, and because it was referenced by the current page
                        urlMap.put(id++, link); // add the link to the urlMap
                        url.links.add(currentPageId); //  add the id of the referencing page to the URL object
                        urlDataMap.put(link, url); // add the link to the urlDataMap

                    }else{// the link is already in the urlDataMap

                        url = urlDataMap.get(link);

                        // try to add the id of the referencing page to the URL object
                        if(url.links.add(currentPageId)==true)url.relevance++; // increment the relevance, because the link was referenced by the current page
                        // else the link was already referenced by the current page (repeated link in the page)                        
                    }
                }

                // Process the tokens and textCitation words
                tokens = new StringTokenizer(urlData.tokens);

                String word;

                while(tokens.hasMoreTokens()){ // process the tokens

                    word = tokens.nextToken();

                    // Check if the word is already in the index
                    if(index.get(word) == null){ // if it is not

                        HashSet<Integer> urls = new HashSet<>();
                        urls.add(currentPageId);
                        index.put(word, urls);

                    }else index.get(word).add(currentPageId); // the word is already in the index
                }

                tokens = new StringTokenizer(urlData.textCitation);

                while(tokens.hasMoreTokens()){ // process the textCitation

                    word = tokens.nextToken();

                    // Check if the word is already in the index
                    if(index.get(word) == null){ // if it is not

                        HashSet<Integer> urls = new HashSet<>();
                        urls.add(currentPageId);
                        index.put(word, urls);

                    }else index.get(word).add(currentPageId); // the word is already in the index
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void saveData(){

        String data = null;

        //Pop the message from the receivedMessages queue
        try {
            
            while((data = receivedMessages.take()) != null){

                if(data.startsWith("type0")) saveDownloaderMessage(data); // if the data came from an downloader

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public IndexStorageBarrel(){
        
        // Open the file and load the data objects
        // try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(barrelId + ".storage"))) {
            
            String str;
            // Read objects from the file
            // while ((str = (String) ois.readObject()) != null)saveDiskMessage(str); // Save the data in the ram TODO


        
        // } catch (IOException | ClassNotFoundException e) {
        //     e.printStackTrace();
        // }

        
        // try {
        //     File file = new File("indexStorageBarrel.txt");
        //     if (!file.exists()) {
        //         file.createNewFile();
        //     }
        //     oos = new ObjectOutputStream(new FileOutputStream(file, true));
        // } catch (IOException e) {
        //     e.printStackTrace();
        // }
    }

    public void run() {
        System.out.println("IndexStorageBarrel is running");
    }

    private void loadDataFromDisk(int messageId){
        // TODO allow for load some specific data from the disk, example, when some messages are cleared from the ram, they can be loaded from the disk when needed

    }
    private void saveDataToDisk(String str){
        // TODO
    }

    
    synchronized String search(int batchNumber, String query){
        return "result";
    }

    private void saveDiskMessage(String data){ // save it on the ram

        int messageId, downloaderId;
        
        try (Scanner scanner = new Scanner(data)) {

            scanner.nextLine(); // Skip the message type since was already checked
            downloaderId = Integer.parseInt(scanner.nextLine());
            messageId = Integer.parseInt(scanner.nextLine());

            // whether the previous messages arrived or not, this message will be saved

            // Save the data
            messagesMap.put(messageId, data);

            for (int i = contiguosMessages; i < messagesMap.size(); i++) {

                if(messagesMap.get(i) != null)contiguosMessages++;
                else break; // It may have more messages saved but they are not contiguos
            }

            System.out.println(LocalDateTime.now() + " : The message from the downloader " + downloaderId + " with the id " + messageId + " was saved in ram," + " the number of contiguos messages is " + contiguosMessages);
            
            dataToProcess.put(messageId); // Add the messageId to the queue they can later be processed

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void saveDownloaderMessage(String data){ // save it on the ram

        int messageId, downloaderId;
        
        try (Scanner scanner = new Scanner(data)) {

            scanner.nextLine(); // Skip the message type since was already checked
            downloaderId = Integer.parseInt(scanner.nextLine());
            messageId = Integer.parseInt(scanner.nextLine());


            if(messagesMap.get(messageId) != null){
                System.out.println("The message from the downloader " + downloaderId + " with the id " + messageId + " was already received");
                return; // The message was already received
                // TODO: Send a message to the downloader to stop sending the same message aka send an ACK
            }

            if(contiguosMessages > messageId){
                System.out.println("The message(s) sent from downloader" + downloaderId + " before the message with the id " + messageId + " haven't arrived yet"); 
                // Then the message(s) before this one was/were lost or haven't arrived yet

                // start a thread with a timer to wait for the lost messages, if they don't arrive in time,
                // ask another barrel for the lost messages

            }

            // whether the previous messages arrived or not, this message will be saved

            // Save the data
            messagesMap.put(messageId, data);

            for (int i = contiguosMessages; i < messagesMap.size(); i++) {

                if(messagesMap.get(i) != null)contiguosMessages++;
                else break; // It may have more messages saved but they are not contiguos
            }

            System.out.println(LocalDateTime.now() + " : The message from the downloader " + downloaderId + " with the id " + messageId + " was saved in ram," + " the number of contiguos messages is " + contiguosMessages);
            
            dataToProcess.put(messageId); // Add the messageId to the queue so the main thread can process the data

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public static void main(String[] args) {
        int multicastDownloadersPort, multicastBarrelsPort, multticastTTL, maxMulticastBytes;
        String host, registryName, multicastAddress, multicastInterface;

        IndexStorageBarrel barrel = new IndexStorageBarrel();

        maxMulticastBytes = 0;
        InetAddress group = null;
        SocketAddress socketAddress0 = null; // Downloaders
        // SocketAddress socketAddress1 = null; // So barrels can communicate with each other
        NetworkInterface networkInterface = null;

        try {
            barrelId = Integer.parseInt(args[0]);
            
            // ----------------- Load the configurations -----------------

            Properties prop = new Properties();
            InputStream input = new FileInputStream(args[1]);
            prop.load(input);

            
            host = prop.getProperty("host");
            registryName = prop.getProperty("registryName");

            multicastDownloadersPort = Integer.parseInt(prop.getProperty("multicastDownloadersPort"));
            multicastBarrelsPort = Integer.parseInt(prop.getProperty("multicastBarrelsPort"));
            multicastAddress = prop.getProperty("multicastAddress");
            multicastInterface = prop.getProperty("multicastInterface");
            multticastTTL = Integer.parseInt(prop.getProperty("multicastTTL"));
            maxMulticastBytes+= Integer.parseInt(prop.getProperty("maxBytesTitle"));
            maxMulticastBytes+= Integer.parseInt(prop.getProperty("maxBytesCitation"));
            maxMulticastBytes+= Integer.parseInt(prop.getProperty("maxBytesTokens"));
            maxMulticastBytes+= Integer.parseInt(prop.getProperty("maxBytesLinks"));

            System.out.println(LocalDateTime.now() + " : Configurations sucessfully loaded!");

            // ----------------- Start the threads -----------------

            // initiate a recovery thread to send recover request of the lost messages to other barrels (sender maybe)
            // new Thread(() -> {
                // TODO
            // }).start();

            // initiate a thread that picks the messages from the receivedMessages queue save it on ram and put the message id in the dataToProcess queue
            new Thread(() -> {
                barrel.saveData();
            }).start();

            // initiate a thread to process the data and save 
            new Thread(() -> {
                barrel.processData();
            }).start();

            // initiate a listener thread to receive the messages from the downloaders and other barrels (main thread)

            // ----------------- Multicast -----------------

            MulticastSocket socket = null;
            try { // Create a multicast socket
                socket = new MulticastSocket(multicastBarrelsPort);
                System.out.println(LocalDateTime.now() + " : Multicast socket created!");

                group = InetAddress.getByName(multicastAddress);
                socketAddress0 = new InetSocketAddress(group, multicastDownloadersPort);
                // socketAddress1 = new InetSocketAddress(group, multicastBarrelsPort); // TODO later so it can communicate with other barrels
                networkInterface = NetworkInterface.getByName(multicastInterface);
                socket.joinGroup(socketAddress0, networkInterface);
                // socket.joinGroup(socketAddress1, networkInterface);
                socket.setTimeToLive(multticastTTL);

                // Receive a message from the downloaders
                while (true) {
                    
                    byte[] buffer =  new byte[maxMulticastBytes+1000]; // 1000 just to be sure
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    socket.receive(packet);
                    
                    String received = new String(packet.getData(), 0, packet.getLength());
                    // System.out.println("Received: " + received);

                    barrel.receivedMessages.put(received);
                }
                // // Receive a message
                // byte[]buffer = new byte[1000];
                // String received = new String(packet.getData(), 0, packet.getLength());
                // System.out.println("Received: " + received + " from " + packet.getAddress() + ":" + packet.getPort());
                
                // // Send a message
                // String message = "Hello from Barrel";
                // buffer = message.getBytes();
                // packet = new DatagramPacket(buffer, buffer.length, group, multicastDownloadersPort);
                // socket.send(packet);

            } catch (IOException e) {
                System.out.println("Error creating multicast socket: " + e.getMessage());
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (socket != null) {
                    try {
                        socket.leaveGroup(socketAddress0, networkInterface);
                        // socket.leaveGroup(socketAddress1, networkInterface);
                        socket.close();
                    } catch (IOException e) {
                        System.out.println("Error closing multicast socket: " + e.getMessage());
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}