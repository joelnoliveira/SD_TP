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

import java.util.Properties;
import java.rmi.registry.Registry;
import java.time.LocalDateTime;
import java.rmi.registry.LocateRegistry;
import rmi.RMIGatewayRemote;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;


public class Downloader {
    /**
     * Main method to start the Downloader.
     *
     * @param args Command-line arguments containing downloaderId and configuration file path.
     */
    public static void main(String args[]) {
        int downloaderId;
        int dataURLCounter = 0; // going to usefull in multicast for ordering and message loss control
        int linkSearchLimit, maxBytesTitle, maxBytesCitation, maxBytesTokens, maxBytesLinks, currentBytesLinks;
        int multicastDownloadersPort, multicastBarrelsPort, multticastTTL;
        String host, registryName, multicastAddress, multicastInterface;
        InetAddress group = null;
        SocketAddress socketAddress = null;
        NetworkInterface networkInterface = null;
        
        try {// Load configurations
            downloaderId = Integer.parseInt(args[0]);
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
            multicastBarrelsPort = Integer.parseInt(prop.getProperty("multicastBarrelsPort"));
            multicastAddress = prop.getProperty("multicastAddress");
            multicastInterface = prop.getProperty("multicastInterface");
            multticastTTL = Integer.parseInt(prop.getProperty("multicastTTL"));


            System.out.println(LocalDateTime.now() + " : Configurations sucessfully loaded!");
            
            MulticastSocket socket = null;
            try {// Create a multicast socket

                socket = new MulticastSocket(multicastDownloadersPort);
                System.out.println(LocalDateTime.now() + " : Multicast socket created!");

                group = InetAddress.getByName(multicastAddress);
                socketAddress = new InetSocketAddress(group, multicastBarrelsPort);
                networkInterface = NetworkInterface.getByName(multicastInterface);
                socket.joinGroup(socketAddress, networkInterface);
                socket.setTimeToLive(multticastTTL);

                try {// Create the registry

                    Registry registry = LocateRegistry.getRegistry(host);
                    System.out.println(LocalDateTime.now() + " : Registry found!");
        
                    // Look up the registry for the remote object
                    RMIGatewayRemote gateway = (RMIGatewayRemote) registry.lookup(registryName);
                    System.out.println(LocalDateTime.now() + " : Gateway object retrieved!");
        
                    try {//Process the URL
                        while (true) {
        
                            System.out.println("-------------------------");
        
                            URLData urlData = new URLData();
                            urlData.pageURL = gateway.getNextUrl();
                            urlData.downloaderId = downloaderId;
                            
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
                            // TODO: add checksum to the message
                            bytes = urlData.toString().getBytes(); // Convert the object to string and then to bytes and put it in the buffer
                            System.out.println("Sending: " + urlData.toString());
                            // Send a message
                            DatagramPacket packet = new DatagramPacket(bytes, bytes.length, group, multicastBarrelsPort);
                            socket.send(packet);
    
                            // TODO maybe implement threds
    
                            // Send data to the Index Storage Barrels by RELIABLE MULTICAST
    
                            // // Send a message
                            // String message = "Hello from Downloader";
                            // byte[] buffer = message.getBytes();
                            // DatagramPacket packet = new DatagramPacket(buffer, buffer.length, group, multicastBarrelsPort);
                            // socket.send(packet);
                            
                            // while (true) {
                                
                            //     // Receive a message
                            //     buffer = new byte[1000];
                            //     packet = new DatagramPacket(buffer, buffer.length);
                            //     socket.receive(packet);
                            //     String received = new String(packet.getData(), 0, packet.getLength());
                            //     System.out.println("Received: " + received + " from " + packet.getAddress() + ":" + packet.getPort());
                            // }
                        }

                        // ------------------------- Scanner -------------------------

                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    // ------------------------- CROWLER -------------------------
                            
                    
                } catch (Exception e) {
                    e.printStackTrace();
                }

                // ------------------------- REGISTRY -------------------------

            } catch (IOException e) {
                System.out.println("Error creating multicast socket: " + e.getMessage());
            } finally {
                if (socket != null) {
                    try {
                        socket.leaveGroup(socketAddress, networkInterface);
                        socket.close();
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
