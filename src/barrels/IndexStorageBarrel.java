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
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.InputMismatchException;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Scanner;

import downloader.URLData;

public class IndexStorageBarrel implements InterfaceISB{

    //Vê se concorda com a estrutura
    private HashMap<String, HashSet<String>> invertedIndex = new HashMap<String, HashSet<String>>();
    private HashMap<String, String> pageTitles = new HashMap<String, String>();
    private HashMap<String, String> pageContents = new HashMap<String, String>();
    private HashMap<String, ArrayList<String>>pageLinks = new HashMap<String, ArrayList<String>>();
    private HashMap<String, Integer> pageLinksCount = new HashMap<String, Integer>();
    private HashSet<String> indexedUrls = new HashSet<String>();
    private ArrayList<String> urlsQueue = new ArrayList<String>();
    private HashMap<String, Integer> termCounts = new HashMap<String, Integer>();
    
    //Construtor
    public IndexStorageBarrel(String name) throws RemoteException {
        this.invertedIndex = new HashMap<>();
        this.pageTitles = new HashMap<>();
        this.pageContents = new HashMap<>();
        this.pageLinks = new HashMap<>();
        this.pageLinksCount = new HashMap<>();
        this.indexedUrls = new HashSet<>();
    }

    public void run() {
        System.out.println("IndexStorageBarrel is running");
    }

    public void saveData(String data){

        // Deserialize the data
        try (Scanner scanner = new Scanner(data)) {
            scanner.nextLine(); // Skip the first line
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    //Vou colocar só as funções que tenho
    //Vamos ter que altera-las para que elas possam ser usadas aqui

    public synchronized void add2Queue(String url) throws RemoteException {
        if (!indexedUrls.contains(url) && !urlsQueue.contains(url)) {
            this.urlsQueue.add(url);   
        }
    }
    public ArrayList<String> getQueue(){
        return this.urlsQueue;
    }

    public void add2Index(String term, String url) throws RemoteException {
        HashSet<String> urls = invertedIndex.get(term);
        if (urls == null) {
            urls = new HashSet<String>();
            invertedIndex.put(term, urls);
            //System.out.println("Term: " + term + " added to inverted index");
        }
        urls.add(url);
        //System.out.println("URL: " + url + " added to term: " + term);
        termCounts.merge(term, 1, Integer::sum);
    }
    public void getInvertedIndex(){
        //System.out.println("Inverted Index: ");
        for (Map.Entry<String, HashSet<String>> entry : invertedIndex.entrySet()) {
            System.out.println("Term: " + entry.getKey() + " URLs: " + entry.getValue());
        }
    }

    public void add2IndexedUrls(String term,String url) throws RemoteException {
        indexedUrls.add(url);
    }

    public void addPageTitle(String url, String title) throws RemoteException {
        pageTitles.put(url, title);
    }

    public String getPageTitle(String url) throws RemoteException {
        return pageTitles.get(url);
    }

    public void addPageContents(String url, String text) throws RemoteException {
        pageContents.put(url, text);
    }

    public String getPageContents(String url) throws RemoteException {
        return pageContents.get(url);
    }

    public void addPageLinks(String url, ArrayList<String> links) throws RemoteException {
        pageLinks.put(url, links);
    }

    public List<String> getPageLinks(String url) throws RemoteException {
        return pageLinks.get(url);
    }

    public void addPageLinksCount(String url, int count) throws RemoteException {
        pageLinksCount.put(url, count);
    }

    public int getPageLinksCount(String url) throws RemoteException {
        return pageLinksCount.get(url);
    }

    public String getShortQuote(String url) throws RemoteException {
        String text = pageContents.get(url);
        if (text == null) {
            return "";
        }
        String quote = text.substring(0, Math.min(40, text.length()))+"...";
        return quote;
    }

    public LinkedHashSet<String> getURLsByTerms(String terms) throws RemoteException {
        List<String> termsList = new ArrayList<String>();
        for (String term : terms.split(" ")) {
            termsList.add(term);
        }
        HashSet<String> urls = new HashSet<String>();
        for (String term : termsList) {
            HashSet<String> termURLs = invertedIndex.get(term);
            if (termURLs == null) {
                return null;
            }
            if (urls.isEmpty()) {
                urls.addAll(termURLs);
            } else {
                urls.retainAll(termURLs);
            }
        }
        return getSortedByPageLinks(urls);
    }

    public LinkedHashSet<String> getSortedByPageLinks(HashSet<String> termURLs) throws RemoteException {
        HashMap <Integer,String> notSortedURLs = new HashMap<Integer,String>();

        for(String url: termURLs){
            notSortedURLs.put(pageLinksCount.get(url), url);
            //System.out.println("URL: " + url + " Links: " + getPageLinksCount(url));
        }

        Map<Integer,String> sortedURLs = sortByKeys(notSortedURLs);

        //System.out.println("Sorted URLs: " + sortedURLs);

        LinkedHashSet<String> sortedURLsSet = new LinkedHashSet<String>();
        List<Integer> keys = new ArrayList<>(sortedURLs.keySet());
        Collections.reverse(keys);

        for (Integer key : keys) {
            sortedURLsSet.add(sortedURLs.get(key));
        }

        return sortedURLsSet;
    }

    public static HashMap<Integer,String> sortByKeys(HashMap<Integer,String> map){
        ArrayList<Integer> keys = new ArrayList<Integer>(map.keySet());
        Collections.sort(keys);
        HashMap<Integer,String> sortedMap = new LinkedHashMap<Integer,String>();
        for(Integer key: keys){
            sortedMap.put(key, map.get(key));
        }
        return sortedMap;
    }
    
    public List<String> getURLsByLink(String url_consulta) {
        List<String> urls = new ArrayList<String>();
        try {
            for (String url : getPageLinks(url_consulta)) {
                urls.add(url);
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return urls;  
    }
    public ArrayList<String> getPagesThatLinksTo(String url){
        ArrayList<String> pages = new ArrayList<String>();
        for (String page : pageLinks.keySet()) {
            ArrayList<String> links = pageLinks.get(page);
            if (links.contains(url)) {
                pages.add(page);
            }
        }
        return pages;
    }
    public List<String> getURLs(){
        List<String> urls = new ArrayList<String>();
        for (String url : indexedUrls) {
            urls.add(url);
        }
        return urls;
    }
    public static void main(String[] args) {

        int multicastDownloadersPort, multicastBarrelsPort, multticastTTL, maxMulticastBytes;
        String host, registryName, multicastAddress, multicastInterface;
        Integer port;

        maxMulticastBytes = 0;
        InetAddress group = null;
        SocketAddress socketAddress0 = null; // Downloaders
        // SocketAddress socketAddress1 = null; // So barrels can communicate with each other
        NetworkInterface networkInterface = null;

        try {
            // Load configurations
            Properties prop = new Properties();
            InputStream input = new FileInputStream(args[0]);
            prop.load(input);

            
            host = prop.getProperty("host");
            port = Integer.parseInt(prop.getProperty("port"));
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
                    System.out.println("Received: " + received);

                    
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
            //Não sei se pode ficar aqui o barrel?
            try {
                //Create barrels
                InterfaceISB barrel1 = new IndexStorageBarrel("Barrel1");
                InterfaceISB barrel2 = new IndexStorageBarrel("Barrel2");

                //Create registry and bind barrels
                Registry registry = LocateRegistry.createRegistry(port);
                registry.rebind("Barrel1", barrel1);
                registry.rebind("Barrel2", barrel2);

                System.out.println("Barrels created and bound to registry");
                System.out.println("Press any key to exit...");
                Scanner scanner = new Scanner(System.in);
                scanner.nextLine();
                scanner.close();
                System.exit(0);
            } catch (Exception e) {
                System.err.println("Error starting barrels: " + e.getMessage());
                e.printStackTrace();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InputMismatchException e) {
            e.printStackTrace();
        }
    }
}