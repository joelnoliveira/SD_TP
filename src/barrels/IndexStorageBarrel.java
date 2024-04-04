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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Scanner;
import java.util.StringTokenizer;
import java.util.concurrent.LinkedBlockingQueue;

import downloader.URLData;

public class IndexStorageBarrel{

    // Messages
        private int processedMessages = 0; // The number of messages processed from the downloader
        private int contiguosMessages = 0; // The number of contiguos messages received from the downloaders
        private Map<Integer, String> messagesMap = new HashMap<>(); // The map that will store the URLData string messages received from the downloaders during a certain length, so others barrels can recover lost messages

    // Processed data
    private class URL{

        private int id = 0,relevance = 0; // Number of links that point to this url
        private String title, textCitation;
        private HashSet<Integer> links; // The id (document id) of the links that points to this url
    }
    
    private Map<Integer, String> urlMap = new HashMap<>(); // Makes the correspondence between a number (aka doucument id) and a url, even tough the processed data is identified by url, the index will help save space when regestering for each url the links that point to it
    private Map<String, URL> urlDataMap = new HashMap<>(); // Makes the correspondence between a url and the URLData object
    private Map<String, HashSet<Integer>> index = new HashMap<>(); // makes the correspondence between a word and the urls that contain it
    private Map<String, Integer> termCounts = new HashMap<>(); // The number of times a term was searched
    
    // Blocking queue
    private LinkedBlockingQueue<String> dataToProcess = new LinkedBlockingQueue<>();

    public void run() {
        System.out.println("IndexStorageBarrel is running");
    }

    private void loadDataFromDisk(){
        // TODO

    }
    private void saveDataToDisk(){
        // TODO

    }

    synchronized void processData(){

        int downloaderId, messageId, id, currentPageId;
        String data = null;
        URL url = null;
        
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
                tokens = new StringTokenizer(messagesMap.get(messageId), "\n"); // use only "\n" as delimiter
                tokens.nextToken(); // Skip the message type
                tokens.nextToken(); // Skip the downloaderId
                tokens.nextToken(); // Skip the messageId
                urlData.pageURL = tokens.nextToken();
                urlData.title = tokens.nextToken();
                urlData.textCitation = tokens.nextToken();
                urlData.tokens = tokens.nextToken();
                urlData.links = tokens.nextToken();

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
                    if(urlDataMap.get(tokens.nextToken()) == null){ // if it is not

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

    private void saveDownloaderMessage(String data){

        int downloaderId, messageId;
        
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
            // else downloader exists, so the downloadersData object is already in the map

            for (int i = contiguosMessages; i < messagesMap.size(); i++) {

                if(messagesMap.get(i) != null)contiguosMessages++;
                else break; // It may have more messages saved but they are not contiguos
            }

            System.out.println(LocalDateTime.now() + " : The message from the downloader " + downloaderId + " with the id " + messageId + " was saved in ram," + " the number of contiguos messages is " + contiguosMessages);
            
            dataToProcess.put(downloaderId + " " + messageId); // Add the downloaderId and the messageId to the queue so the main thread can process the data

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void saveData(String data){
        
        if(data.startsWith("type0")) saveDownloaderMessage(data); // if the data came from an downloader
    }

    public long ping() throws RemoteException{
        return System.currentTimeMillis();
    }

    public ArrayList<String> getMostSearchedTerms(int cap) throws RemoteException{
        // Convert termCounts map entries to a list for sorting
        List<Map.Entry<String, Integer>> sortedTerms = new ArrayList<>(termCounts.entrySet());
        // Sort the list in reverse order based on the values (term counts)
        sortedTerms.sort(Collections.reverseOrder(Map.Entry.comparingByValue()));
        // Initialize a list to store the top terms
        ArrayList<String> topTerms = new ArrayList<>();
        // Iterate over the sorted terms list and add top terms to the result list
        for (int i = 0; i < cap && i < sortedTerms.size(); i++) {
            topTerms.add(i + ". " + sortedTerms.get(i).getKey());
        }
        // Return the list of top terms
        return topTerms;
    }
    
    public String getShortQuote(String url)  throws RemoteException{
        // Retrieve URL data from the map based on the provided URL
        URL urlData = urlDataMap.get(url);
        // Check if URL data is found
        if (urlData == null) {
            // Return a message indicating that the URL is not found
            return "URL not found";
        }
        // Retrieve the text content from the URL data
        String text = urlData.textCitation;
        // Check if text content exists
        if (text == null) {
            // If no text content is available, return an empty string
            return "";
        }
        // Extract a short quote from the text content, limited to 40 characters or the length of the text if shorter
        String quote = text.substring(0, Math.min(40, text.length())) + "...";
        // Return the short quote
        return quote;
    }
    
    public String getPageTitle(String url) throws RemoteException {
        // Retrieve URL data from the map based on the provided URL
        URL urlData = urlDataMap.get(url);
        // Check if URL data is found
        if (urlData == null) {
            // Return a message indicating that the URL is not found
            return "URL not found";
        }
        // Return the title of the URL data
        return urlData.title;
    }
    
    public LinkedHashSet<String> getURLsByTerms(String terms) throws RemoteException{
        // Initialize an ArrayList to store individual terms
        ArrayList<String> termsList = new ArrayList<String>();
        // Split the input terms string into individual terms and add them to the list
        for (String term : terms.split(" ")) {
            termsList.add(term);
            // Increment the count of each term in the termCounts map
            termCounts.merge(term, 1, Integer::sum);
        }
        // Initialize a HashSet to store URLs
        HashSet<String> urls = new HashSet<String>();
        // Flag to track if it's the first term in the loop
        boolean firstTerm = true;
        // Iterate over each term in the termsList
        for (String term : termsList) {
            // Retrieve the set of URLs associated with the current term from the index
            HashSet<Integer> termURLsIndexes = index.get(term);
            // Check if the term is found in the index
            if (termURLsIndexes == null) {
                return null; // Term not found
            }
            // Initialize a HashSet to store URLs associated with the current term
            HashSet<String> termURLs = new HashSet<>();
            // Iterate over each index in the set of URLs associated with the current term
            for (int index : termURLsIndexes) {
                // Retrieve the URL from the urlMap based on the index and add it to the termURLs set
                String url = urlMap.get(index);
                termURLs.add(url);
            }
            // If it's the first term, add all URLs associated with the first term to the urls set
            // Otherwise, retain only the common URLs between the current set and the set of URLs associated with the previous term
            if (firstTerm) {
                urls.addAll(termURLs);
                firstTerm = false;
            } else {
                urls.retainAll(termURLs);
            }
        }
        // Return a sorted set of URLs by page links
        return getSortedByPageLinks(urls);
    }
    
    public LinkedHashSet<String> getSortedByPageLinks(HashSet<String> termURLs) throws RemoteException{
        // Initialize a HashMap to store URLs grouped by relevance
        HashMap<Integer, ArrayList<String>> notSortedURLs = new HashMap<Integer, ArrayList<String>>();
    
        // Group URLs by number of links (caso seja isso)
        //for (String url : termURLs) {
        //    int nLinks = urlDataMap.get(url).links.size();
        //    if (!notSortedURLs.containsKey(nLinks)) {
        //        notSortedURLs.put(nLinks, new ArrayList<String>());
        //    }
        //    notSortedURLs.get(nLinks).add(url);
        //}

        // Sort URLs by relevance
        for (String url : termURLs) {
            int rel = urlDataMap.get(url).relevance;
            if (!notSortedURLs.containsKey(rel)) {
                notSortedURLs.put(rel, new ArrayList<String>());
            }
            notSortedURLs.get(rel).add(url);
        }
    
        // Sort URLs by relevance
        Map<Integer, ArrayList<String>> sortedURLs = sortByKeys(notSortedURLs);
    
        // Initialize a LinkedHashSet to store sorted URLs
        LinkedHashSet<String> sortedURLsSet = new LinkedHashSet<String>();
    
        // Get the keys (relevance values) in descending order
        List<Integer> keys = new ArrayList<>(sortedURLs.keySet());
    
        // Add URLs to the sorted set in descending relevance order
        for (Integer key : keys) {
            List<String> urls = sortedURLs.get(key);
            Collections.sort(urls); // Sort URLs lexicographically
            sortedURLsSet.addAll(urls);
        }
    
        // Return the sorted set of URLs
        return sortedURLsSet;
    }
    
    public static Map<Integer, ArrayList<String>> sortByKeys(HashMap<Integer, ArrayList<String>> map) {
        // Convert the map entries to a list
        ArrayList<Map.Entry<Integer, ArrayList<String>>> list = new ArrayList<>(map.entrySet());
        
        // Sort the list of entries by keys (relevance values) in descending order
        list.sort((o1, o2) -> o2.getKey().compareTo(o1.getKey()));
    
        // Create a LinkedHashMap to store the sorted entries
        Map<Integer, ArrayList<String>> sortedMap = new LinkedHashMap<>();
        
        // Add the sorted entries to the LinkedHashMap
        for (Map.Entry<Integer, ArrayList<String>> entry : list) {
            sortedMap.put(entry.getKey(), entry.getValue());
        }
        
        // Return the sorted map
        return sortedMap;
    }
    
    public ArrayList<String> getPagesThatLinksTo(String url) throws RemoteException{
        // Initialize an ArrayList to store pages that link to the given URL
        ArrayList<String> pages = new ArrayList<String>();
    
        // Iterate over each entry in the urlDataMap
        for (Map.Entry<String, URL> entry : urlDataMap.entrySet()) {
            String page = entry.getKey(); // Get the page URL
            HashSet<Integer> urlsIDs = entry.getValue().links; // Get the set of URLs linked from the page
            ArrayList<String> links = new ArrayList<String>();
            
            // Convert the set of URL IDs to a list of URLs
            for (int id : urlsIDs) {
                links.add(urlMap.get(id));
            }
    
            // Check if the list of links contains the given URL
            if (links.contains(url)) {
                pages.add(page); // If the URL is found, add the page to the list
            }
        }
        
        // Return the list of pages that link to the given URL
        return pages;
    }
    
    public boolean hasURL(String url) throws RemoteException{
        // Check if the urlDataMap contains the given URL
        return urlDataMap.containsKey(url);
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