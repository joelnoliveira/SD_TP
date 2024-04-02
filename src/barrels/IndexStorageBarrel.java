package barrels;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
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

import downloader.URLData;

public class IndexStorageBarrel{
    private class DownloadersData{

        private int processedMessages = 0; // The number of messages processed from the downloader
        private int contiguosMessages = 0; // The number of contiguos messages received from the downloaders
        private Map<Integer, String> messagesMap = new HashMap<>(); // The map that will store the URLData string messages received from the downloaders during a certain length, so others barrels can recover lost messages
    }

    private class URL{

        private int relevance = 0; // Number of links that point to this url
        private String title, url;
        private List<Integer> links; // The id (document id) of the links that points to this url
    }
    
    private Map<Integer, DownloadersData> downloadersDataMap = new HashMap<>(); // The map that will store the downloadersData objects where the key is the downloader's id
    private Map<Integer, URL> urlMap = new HashMap<>(); // Makes the correspondence between a number (aka doucument id) and a url
    private Map<String, HashSet<Integer>> index = new HashMap<>(); // makes the correspondence between a word and the urls that contain it
    
    // bloking queue
    private LinkedBlockingQueue<String> dataToProcess = new LinkedBlockingQueue<>();

    public void run() {
        System.out.println("IndexStorageBarrel is running");
    }

    public void processData(){

        int downloaderId, messageId, linkId;
        String data = null;
        
        try {
            
            while((data = dataToProcess.take()) != null){

                //tokenize the data
                StringTokenizer tokens = new StringTokenizer(data);
                downloaderId = Integer.parseInt(tokens.nextToken());
                messageId = Integer.parseInt(tokens.nextToken());

                // TODO add syncronized to were its needed to use threads

                // Process the data
                // Get the URLData object from the message
                URLData urlData = new URLData();
                tokens = new StringTokenizer(downloadersDataMap.get(downloaderId).messagesMap.get(messageId), "\n");
                tokens.nextToken(); // Skip the message type
                tokens.nextToken(); // Skip the downloaderId
                tokens.nextToken(); // Skip the messageId
                urlData.pageURL = tokens.nextToken();
                urlData.title = tokens.nextToken();
                urlData.textCitation = tokens.nextToken();
                urlData.tokens = tokens.nextToken();
                urlData.links = tokens.nextToken();

                linkId = urlMap.size();
                tokens = new StringTokenizer(urlData.links);

                // while(tokens.hasMoreTokens()){

                //     // check if the link is already in the urlMap, but check by string url not by id
                //     //

                // }





            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void saveDownloaderMessage(String data){

        boolean downloadersExists = false;
        int downloaderId, messageId;
        
        try (Scanner scanner = new Scanner(data)) {
            
            DownloadersData downloadersData = null;

            scanner.nextLine(); // Skip the message type since was already checked
            downloaderId = Integer.parseInt(scanner.nextLine());
            messageId = Integer.parseInt(scanner.nextLine());

            if((downloadersData = downloadersDataMap.get(downloaderId)) != null){

                downloadersExists = true;

                if(downloadersData.messagesMap.get(messageId) != null){
                    System.out.println("The message from the downloader " + downloaderId + " with the id " + messageId + " was already received");
                    return; // The message was already received
                    // TODO: Send a message to the downloader to stop sending the same message aka send an ACK
                }
            }else downloadersData = new DownloadersData();

            if(downloadersData.contiguosMessages > messageId){
                System.out.println("The message(s) sent from downloader" + downloaderId + " before the message with the id " + messageId + " haven't arrived yet"); 
                // Then the message(s) before this one was/were lost or haven't arrived yet

                // start a thread with a timer to wait for the lost messages, if they don't arrive in time,
                // ask another barrel for the lost messages

            }

            // whether the previous messages arrived or not, this message will be saved

            // Save the data
            downloadersData.messagesMap.put(messageId, data);
            if(!downloadersExists) downloadersDataMap.put(downloaderId, downloadersData);
            // else downloader exists, so the downloadersData object is already in the map

            for (int i = downloadersData.contiguosMessages; i < downloadersData.messagesMap.size(); i++) {

                if(downloadersData.messagesMap.get(i) != null)downloadersData.contiguosMessages++;
                else break; // It may have more messages saved but they are not contiguos
            }

            System.out.println(LocalDateTime.now() + " : The message from the downloader " + downloaderId + " with the id " + messageId + " was saved in ram," + " the number of contiguos messages is " + downloadersData.contiguosMessages);
            
            dataToProcess.put(downloaderId + " " + messageId); // Add the downloaderId and the messageId to the queue so the main thread can process the data

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void saveData(String data){
        
        if(data.startsWith("type0")) saveDownloaderMessage(data); // if the data came from an downloader
    }

    public static void main(String[] args) {
        int barrelId;
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
            // Load configurations
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

                    // TODO maybe start a thread to save the data, so the main thread can keep receiving messages
                    barrel.saveData(received);
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