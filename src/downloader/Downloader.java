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

import java.util.InputMismatchException;
import java.util.Properties;
import java.util.Scanner;
import java.util.StringTokenizer;
import java.rmi.registry.Registry;
import java.time.LocalDateTime;
import java.rmi.registry.LocateRegistry;
import rmi.RMIGatewayRemote;

import java.net.MulticastSocket;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;

public class Downloader {
    public static void main(String args[]) {

        int countTokens, linkSearchLimit, tokenLimit, multicastDownloadersPort, multicastBarrelsPort, multticastTTL;
        String host, registryName, multicastAddress, multicastInterface;
        InetAddress group = null;
        SocketAddress socketAddress = null;
        NetworkInterface networkInterface = null;
        
        try {

            Properties prop = new Properties();
            InputStream input = new FileInputStream(args[0]);
            prop.load(input);

            
            host = prop.getProperty("host");
            registryName = prop.getProperty("registryName");
            linkSearchLimit = Integer.parseInt(prop.getProperty("linkSearchLimit"));
            tokenLimit = Integer.parseInt(prop.getProperty("tokenLimit"));
            multicastDownloadersPort = Integer.parseInt(prop.getProperty("multicastDownloadersPort"));
            multicastBarrelsPort = Integer.parseInt(prop.getProperty("multicastBarrelsPort"));
            multicastAddress = prop.getProperty("multicastAddress");
            multicastInterface = prop.getProperty("multicastInterface");
            multticastTTL = Integer.parseInt(prop.getProperty("multicastTTL"));


            System.out.println(LocalDateTime.now() + " : Configurations sucessfully loaded!");
            
            MulticastSocket socket = null;
            try {
                socket = new MulticastSocket(multicastDownloadersPort);
                System.out.println(LocalDateTime.now() + " : Multicast socket created!");

                group = InetAddress.getByName(multicastAddress);
                socketAddress = new InetSocketAddress(group, multicastBarrelsPort);
                networkInterface = NetworkInterface.getByName(multicastInterface);
                socket.joinGroup(socketAddress, networkInterface);
                socket.setTimeToLive(multticastTTL);

                // Send a message
                String message = "Hello from Downloader";
                byte[] buffer = message.getBytes();
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length, group, multicastBarrelsPort);
                socket.send(packet);
                
                while (true) {
                    
                    // Receive a message
                    buffer = new byte[1000];
                    packet = new DatagramPacket(buffer, buffer.length);
                    socket.receive(packet);
                    String received = new String(packet.getData(), 0, packet.getLength());
                    System.out.println("Received: " + received + " from " + packet.getAddress() + ":" + packet.getPort());
                }

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

        } catch (IOException e) {
            e.printStackTrace();
            return;
        } catch (InputMismatchException e) {
            e.printStackTrace();
            return;
        }

        try {
            // Create the registry
            Registry registry = LocateRegistry.getRegistry(host);
            System.out.println(LocalDateTime.now() + " : Registry found!");

            // Look up the registry for the remote object
            RMIGatewayRemote gateway = (RMIGatewayRemote) registry.lookup(registryName);
            System.out.println(LocalDateTime.now() + " : Gateway object retrieved!");

            try {
                while (linkSearchLimit >= 0) {

                    System.out.println("-------------------------");
                    System.out.println("Remaining links until limit: " + linkSearchLimit);

                    String urlWithCounter = gateway.getNextUrl();

                    System.out.println("URL with counter retrieved from the URL Queue: " + urlWithCounter);

                    try (Scanner scanner = new Scanner(urlWithCounter)){

                        int messageNumber = scanner.nextInt(); // going to usefull in multicast for ordering
                        String url = scanner.next();
    
                        System.out.println("Message number: " + messageNumber);
    
                        System.out.println("URL: " + url);
    
                        // Process the URL...
                        countTokens = 0;
                        Document doc = Jsoup.connect(url).get();
                        StringTokenizer tokens = new StringTokenizer(doc.text());
            
                        while (tokens.hasMoreElements() && countTokens++ < tokenLimit){
            
                            System.out.println(tokens.nextToken().toLowerCase());
                        }
                        
                        Elements links = doc.select("a[href]");

                        // TODO citação de texto??
    
                        for (Element link : links){
    
                            gateway.addUrl(link.attr("abs:href"));
                            System.out.println(link.text() + "\n" + link.attr("abs:href") + "\n");
                        }
    
                        // TODO send data by RELIABLE MULTICAST to the Index Storage Barrels

                        // TODO maybe implement threds

                        // Send data to the Index Storage Barrels by RELIABLE MULTICAST


    
                        linkSearchLimit--;
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
                    
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
