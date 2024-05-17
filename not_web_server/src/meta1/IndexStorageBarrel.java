package meta1;

import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.SocketAddress;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
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
import java.util.concurrent.Semaphore;


/**
 * The `IndexStorageBarrel` class represents a barrel that stores and manages indexed data.
 * It extends the `UnicastRemoteObject` class and implements the `RMIBarrelsInterface` interface.
 * This class provides methods for retrieving URLs based on search terms, sorting URLs by relevance,
 * retrieving short quotes from URLs, retrieving page titles, and finding pages that link to a given URL.
 * It also contains inner classes and data structures for storing and processing data.
 */
public class IndexStorageBarrel extends UnicastRemoteObject implements RMIBarrelsInterface{
    /**
     * This class extends the ObjectOutputStream class to provide a custom implementation
     * for appending objects to an output stream without writing a header.
     */
    private class AppendableObjectOutputStream extends ObjectOutputStream {
        public AppendableObjectOutputStream(OutputStream out) throws IOException {
            super(out);
        }
    
        @Override
        protected void writeStreamHeader() throws IOException {
            // do not write a header, but reset
            reset();
        }
    }


    private int barrelId, multicastBarrelsPort, multicastDownloadersPort, messageRecoverySleepTime, lastChekedMessageID = -1;
    private int contiguosMessages = 0; // The number of contiguos messages received from the downloaders
    
    
    private String fromDownloaderType = "type0"; // The type of the message that comes from the downloaders
    private String toDownloaderType = "type1"; // The type of the message that goes to the downloaders (ACKS)
    private String recoveryType = "type2"; // The type of the message that goes to the barrels (Recovery requests)
    private String discoveryType = "type4"; // The type of the message that barrels send on startup to check if the other barrels have messages
    
    private InetAddress group = null;
    private MulticastSocket socket = null;

    RMIGatewayBarrelsInterface gateway;
    // Processed data
    /**
     * Represents a URL object.
     */
    private class URL{

        private int id = 0,relevance = 0; // Number of links that point to this url
        private String title, textCitation;
        private HashSet<Integer> links = new HashSet<>(); // The id (document id) of the links that points to this url
    }

    // Messages
    private Map<Integer, String> messagesMap = new HashMap<>(); // The map that will store the URLData string messages received from the downloaders, so others barrels can recover lost messages
    private Map<Integer, String> urlMap = new HashMap<>(); // Makes the correspondence between a number (aka doucument id) and a url, even tough the processed data is identified by url, the index will help save space when regestering for each url the links that point to it
    private Map<String, URL> urlDataMap = new HashMap<>(); // Makes the correspondence between a url and its data (the URLData object)
    private Map<String, HashSet<Integer>> index = new HashMap<>(); // makes the correspondence between a word and the urls that contain it

    // bloking queues
    private LinkedBlockingQueue<String> receivedMessages = new LinkedBlockingQueue<>();
    private LinkedBlockingQueue<Integer> dataToProcess = new LinkedBlockingQueue<>();
    private LinkedBlockingQueue<String> toBeSentMessages = new LinkedBlockingQueue<>();
    private LinkedBlockingQueue<String> missingMessages = new LinkedBlockingQueue<>();

    private ObjectOutputStream oos;
    private final Semaphore diskSemaphore = new Semaphore(1);

    // ---------------------- Remote methods ----------------------

    /**
     * Checks if the given URL exists in the urlDataMap.
     *
     * @param url the URL to check
     * @return true if the urlDataMap contains the given URL, false otherwise
     * @throws RemoteException if a remote exception occurs
     */
    public boolean hasURL(String url) throws RemoteException{
        // Check if the urlDataMap contains the given URL
        return urlDataMap.containsKey(url);
    }

    /**
     * Searches for the given terms in the index and retrieves the URLs associated with the terms.
     *
     * @param terms the terms to search for
     * @param batchNumber the batch number to be retrieved
     * @param citationLen the length of the citation to retrieve
     * @return a list of URLs associated with the given terms
     * @throws RemoteException if a remote exception occurs
     */
    public List<String> searchByTerms(String terms, int batchNumber, int citationLen) throws RemoteException{
        
        // Initialize an ArrayList to store individual terms
        ArrayList<String> termsList = new ArrayList<String>();
        // Split the input terms string into individual terms and add them to the list
        for (String term : terms.split(" ")) {
            termsList.add(term);
        }
        
        // Initialize a HashSet to store URLs
        HashSet<String> urls = new HashSet<String>();

        // Iterate over each term in the termsList

        for (String term : termsList) {
            // Retrieve the set of URLs associated with the current term from the index
            HashSet<Integer> termURLsIndexes = index.get(term);
            // Check if the term is found in the index
            if (termURLsIndexes == null)return new ArrayList<String>(); // If the term is not found, return an empty list

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
            if (urls.isEmpty()) {
                urls.addAll(termURLs);
            } else {
                urls.retainAll(termURLs);
            }
            //System.out.println(urls);
        }
        
        // Sort set of URLs by page links
        LinkedHashSet<String> sortedURLs = getSortedByPageLinks(urls);

        // convert the sortedURLs to an ArrayList
        List<String> sortedURLsList = new ArrayList<String>(sortedURLs);
        
        List<String> result = new ArrayList<String>();

        int start, end;
        start = batchNumber * 10;
        if(start >= sortedURLsList.size())return result; // if the batch number is too high, return an empty list
        end = Math.min(start + 10, sortedURLsList.size());

        sortedURLsList = sortedURLsList.subList(start, end);

        // Get the data associated with the URLs
        for (String url : sortedURLsList) {
            // Retrieve the page title and short quote for each URL

            URL urlData = urlDataMap.get(url);
            StringBuilder sb = new StringBuilder();
            sb.append(urlData.title + "\n");
            sb.append(url + "\n");

            if (urlData.textCitation.length() > citationLen) {
                sb.append(urlData.textCitation.substring(0, citationLen) + "\n");
            } else {
                // If textCitation is shorter, append the whole string
                sb.append(urlData.textCitation + "\n");
            }

            result.add(sb.toString());
        }
        return result;
    }

    /**
     * Retrieves a sorted set of URLs based on their relevance to the given term URLs.
     *
     * @param termURLs a HashSet of term URLs
     * @return a LinkedHashSet of URLs sorted by relevance
     * @throws RemoteException if a remote exception occurs
     */
    private LinkedHashSet<String> getSortedByPageLinks(HashSet<String> termURLs) throws RemoteException{
        // Initialize a HashMap to store URLs grouped by relevance
        HashMap<Integer, ArrayList<String>> notSortedURLs = new HashMap<Integer, ArrayList<String>>();

        URL urlData;
        // Sort URLs by relevance
        for (String url : termURLs) {
            if((urlData = urlDataMap.get(url)) == null){
                continue;
            }
            int rel = urlData.relevance;
            
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


    /**
     * Retrieves the urls that reference the given url.
     * @param url the url to search for
     * @param batchNumber the batch number to be retrieved
     * @return a list of urls that reference the given url
     * @throws RemoteException if a remote exception occurs
     */
    public List<String> getPagesThatLinksTo(String url, int batchNumber) throws RemoteException{
        
        int start, end, count = 0;
        start = batchNumber * 10;
        end = start + 10;

        // Initialize an ArrayList to store pages that link to the given URL
        List<String> results = new ArrayList<String>();
        HashSet<Integer> links;
        URL urldata = urlDataMap.get(url);

        if(urldata == null)return results; // if the url is not in the urlDataMap, return an empty list

        links = urldata.links;
        if(links == null)return results; // if the url has no links, return an empty list

        for (Integer link : links) {
            
            if(count >= end)break;
            if(count >= start)results.add(urlMap.get(link));
            count++;
        }
        // Return the list links to the given URL
        return results;
    }

    /**
     * Returns the ID of the barrel.
     *
     * @return the ID of the barrel
     * @throws RemoteException if a remote exception occurs
     */
    public int getBarrelID() throws RemoteException{
        return barrelId;
    }
    // ---------------------- Local methods ----------------------
    /**
     * Constructs a new IndexStorageBarrel object with the specified barrel ID.
     * 
     * @param barrelId the ID of the barrel
     * @throws RemoteException if a remote exception occurs
     */
    public IndexStorageBarrel(int barrelId) throws RemoteException {

        this.barrelId = barrelId;
        System.out.println(LocalDateTime.now() + " : System is starting up");

        try {

            File file = new File(barrelId + ".disk");
            boolean append = file.exists() && file.length() > 0;

            if(append){
                System.out.println(LocalDateTime.now() + " : Datafile found in disk, loading data objects");
                // Open the file and load the data objects
                try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
                    String str;
                    //Read objects from the file
                    while ((str = (String) ois.readObject()) != null)saveDiskMessage(str);
                } 
                catch (EOFException e) {
                    // EOFException is expected when there are no more objects to read
                }catch (IOException | ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }else {
                System.out.println(LocalDateTime.now() + " : No datafile found in disk, creating one");
                file.createNewFile();
            }

            // add a discovery message to the toBeSentMessages queue
            try {
                toBeSentMessages.put(discoveryType + "\n" + barrelId + "\n" + -1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            this.oos = append ? new AppendableObjectOutputStream(new FileOutputStream(file, true)) : new ObjectOutputStream(new FileOutputStream(file, true));
            
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {// thread to close the ObjectOutputStream
                if (oos != null) {

                    try {
                        System.out.println(LocalDateTime.now() + " : Shutting down, closing the ObjectOutputStream");
                        oos.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                    try {
                        gateway.barrelUnsubscribe(barrelId);
                    } catch (RemoteException e) {
                       // ignore, if doesn't work, maybe it never was subscribed
                    }
            }));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Saves the given string data to disk.
     *
     * @param str the string data to be saved
     */
    private void saveDataToDisk(String str){
        
        try {
            diskSemaphore.acquire();
            oos.writeObject(str);
            oos.flush();
            diskSemaphore.release();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Sends messages to the appropriate destinations based on their type.
     */
    private void sender(){

        System.out.println(LocalDateTime.now() + " : Sender thread started");

        String data = null;
        try {
            while (true) {

                data = toBeSentMessages.take(); // Pop the data from the toBeSentMessages queue

                if(data.startsWith(toDownloaderType)){ // if the message is a ack directed to a downloader
                    
                    byte [] buffer = data.getBytes();
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length, group, multicastDownloadersPort);
                    socket.send(packet);

                }else{ // if messages are directed to the barrels

                    byte [] buffer = data.getBytes();
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length, group, multicastBarrelsPort);
                    socket.send(packet);
                }

                // print the type of the message and the downloader id
                System.out.println(LocalDateTime.now() + " : The message with the type " + data.split("\n")[0] + " and the message id " + data.split("\n")[2] + " was sent");
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    } 

    /**
     * This method is responsible for processing the data received from the message queue.
     * It retrieves the message ID from the queue, processes the data, and updates the URL index accordingly.
     * The method is synchronized to ensure thread safety.
     */
    private synchronized void dataprocessor(){ // whis one and search should be synchronized

        System.out.println(LocalDateTime.now() + " : Data processor thread started");

        int messageId, id, currentPageId;
        URL url = null;
        
        try {
            
            while(true){ // get the message id from the queue

                messageId = dataToProcess.take();
                
                try(Scanner scanner = new Scanner(messagesMap.get(messageId))){

                    // Process the data
                    // Get the URLData object from the message
                    URLData urlData = new URLData();
                    // StringTokenizer tokens = new StringTokenizer(messagesMap.get(messageId), "\n"); // use only "\n" as delimiter
                    System.out.println(LocalDateTime.now() + " : The message with the id " + messageId + " is being processed");
                    scanner.nextLine(); // Skip the message type
                    scanner.nextLine(); // Skip the downloaderId
                    scanner.nextLine(); // Skip the messageId
                    urlData.pageURL = scanner.nextLine();
                    urlData.title = scanner.nextLine();
                    urlData.textCitation = scanner.nextLine();
                    urlData.tokens = scanner.nextLine();
                    if(scanner.hasNext())urlData.links = scanner.nextLine();
                    else urlData.links = "";
                    
                    // else {
                    //     urlData.links = urlData.tokens;
                    //     urlData.tokens = urlData.textCitation;
                    //     urlData.textCitation = ""; // the textCitation is empty
                    // }

                    id = urlMap.size(); // Document id
                    StringTokenizer tokens = new StringTokenizer(urlData.links);

                    String link;

                    // Check if the url is already in the urlDataMap
                    if(urlDataMap.get(urlData.pageURL) == null){ // if it is not

                        // create a new URL object
                        url = new URL();
                        currentPageId = id++;
                        url.id = currentPageId;
                        url.relevance = 0; // the relevance is 0 because the url is being added for the first time
                        url.title = urlData.title;
                        url.textCitation = urlData.textCitation;
                        urlMap.put(currentPageId, urlData.pageURL); // add the page url to the urlMap
                        urlDataMap.put(urlData.pageURL, url); // add the page url to the urlDataMap

                    }else{// the url is already in the urlDataMap

                        url = urlDataMap.get(urlData.pageURL);
                        currentPageId = url.id;
                        
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
                            url.id = id;
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
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Sends the missed messages to the barrel that requested them.
     * If the message is in the messagesMap, it adds the message ID to the toBeSentMessages queue.
     * Otherwise, it does nothing.
     *
     * @param messageId the ID of the missed message
     */
    private void sendMissedMessages(int messageId){ // send the missed messages to the barrel that requested them
        
        String data = null;
        // check if the message is in the messagesMap
        if((data = messagesMap.get(messageId)) != null){ // if it is

            try {
                toBeSentMessages.put(data); // add the message id to the toBeSentMessages queue
                System.out.println(LocalDateTime.now() + " : The requested recovery message with the id " + messageId + " was added to the toBeSentMessages queue");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }
        // else the message is not in the messagesMap so we cannot help
    }

    /**
     * Sends the message with the highest ID to answer the discovery message.
     */
    private void sendHighestDownloaderIDMessage(){
            
        String message;

        try {
            if (!messagesMap.isEmpty()) {
                int maxKey = Collections.max(messagesMap.keySet());

                message = messagesMap.get(maxKey);
                toBeSentMessages.put(message);
                System.out.println(LocalDateTime.now() + " : The message with the max id " + maxKey + " was sent to answer the discovery message");
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Processes the received messages in a separate thread.
     * This method continuously retrieves messages from the receivedMessages queue and performs different actions based on the message type.
     * If the message is from a downloader or a barrel, it is saved.
     * If the message is a recovery message, it checks if it didn't come from this barrel and sends missed messages if necessary.
     * If the message is a discovery message, it checks if it didn't come from this barrel and sends the highest downloader ID message.
     * This method runs until there are no more messages in the receivedMessages queue or an exception occurs.
     */
    private void messageProcessor(){

        System.out.println(LocalDateTime.now() + " : Message processor thread started");

        String data = null;

        //Pop the message from the receivedMessages queue
        try {
            
            while((data = receivedMessages.take()) != null){

                if(data.startsWith(fromDownloaderType)) saveDownloaderMessage(data); // if the data came from an downloader or a barrel
                else if(data.startsWith(recoveryType)){

                    // check if the message didnt came from this barrel
                    if(Integer.parseInt(data.split("\n")[1]) != barrelId)sendMissedMessages(Integer.parseInt(data.split("\n")[2]));
                    else{
                        System.out.println(LocalDateTime.now() + " : Barrel received its own recovery message");
                    }
                }
                else if(data.startsWith(discoveryType)){ // if the data came from a barrel

                    // check if the message didnt came from this barrel
                    if(Integer.parseInt(data.split("\n")[1]) != barrelId)sendHighestDownloaderIDMessage();
                    else{
                        System.out.println(LocalDateTime.now() + " : Barrel received its own discovery message");
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Saves a disk message in the RAM and adds it to the processing queue.
     * 
     * @param data The data of the disk message to be saved.
     */
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
            System.out.println(LocalDateTime.now() + " : the message with the id " + messageId + " was saved in disk");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Creates a recover request for missing messages.
     * This method continuously checks for missing messages and creates recover requests for them.
     * If a message hasn't arrived within a certain time frame, a recover request is created and added to the toBeSentMessages queue.
     * The method also adds the new timestamp and message ID to the missingMessages queue for further checking.
     * If the message has already arrived, no recover request is created.
     */
    private void createRecoverRequest(){

        System.out.println(LocalDateTime.now() + " : Recover request thread started");
        
        int messageId;
        long timeStamp, timeElapsed;
        String request;

        while(true){
            try{
                
                request = missingMessages.take(); // pop the request from the missingMessages queue
                
                // separate the timestamp and the message id
                String[] tokens = request.split(" ");
                timeStamp = Long.parseLong(tokens[0]);
                messageId = Integer.parseInt(tokens[1]);

                if(!messagesMap.containsKey(messageId)){ // if the message still hasn't arrived

                    // check elapsed time
                    if((timeElapsed = (System.currentTimeMillis() - timeStamp))/100 >= messageRecoverySleepTime/100){ // if the waiting time has passed
        
                        // create a recover request
                        String message = recoveryType + "\n" + barrelId + "\n" + messageId;
                        toBeSentMessages.put(message); // add the message to the toBeSentMessages queue
                        System.out.println(LocalDateTime.now() + " : Recovery Timer expired, the recover request for the message with the id " + messageId + " was added to the toBeSentMessages queue");

                        // add the new timestamp and message id to the missingMessages queue so it can be checked again
                        missingMessages.put(System.currentTimeMillis() + " " + messageId);

                    } else{ // if the waiting time hasn't passed
                        
                        Thread.sleep(messageRecoverySleepTime - timeElapsed);

                        // check if the message has already arrived
                        if(!messagesMap.containsKey(messageId)){ // if the message still hasn't arrived
        
                            // create a recover request
                            String message = recoveryType + "\n" + barrelId + "\n" + messageId;
                            toBeSentMessages.put(message); // add the message to the toBeSentMessages queue
                            System.out.println(LocalDateTime.now() + " : Recovery Timer expired, the recover request for the message with the id " + messageId + " was added to the toBeSentMessages queue");

                            // add the new timestamp and message id to the missingMessages queue so it can be checked again
                            missingMessages.put(System.currentTimeMillis() + " " + messageId);
                        }// else the message has already arrived
                    }
                }// else the message has already arrived
            }catch(Exception e){
                e.printStackTrace();
            }
        }
    }

    /**
     * Saves the downloader message in RAM, processes the data, and saves it to disk.
     * 
     * @param data The data of the downloader message.
     */
    private void saveDownloaderMessage(String data){ // save it on the ram

        int messageId, downloaderId;
        
        try (Scanner scanner = new Scanner(data)) {

            scanner.nextLine(); // Skip the message type since was already checked
            downloaderId = Integer.parseInt(scanner.nextLine());
            messageId = Integer.parseInt(scanner.nextLine());


            if(messagesMap.get(messageId) != null){
                System.out.println(LocalDateTime.now() + " : The message from the downloader " + downloaderId + " with the id " + messageId + " was already received");
                return; // The message was already received
            }

            // whether the previous messages arrived or not, this message will be saved
            // Save the data
            messagesMap.put(messageId, data);
            System.out.println(LocalDateTime.now() + " : The message from the downloader " + downloaderId + " with the id " + messageId + " was saved in ram");

            boolean contiguos = true;
            String message;

            for (int i = contiguosMessages; i < Math.max(messageId,messagesMap.size()); i++) { // update the number of contiguos messages

                if(contiguos && (messagesMap.get(i) != null))contiguosMessages++;
                else {// the messages are not contiguos

                    contiguos = false;
                    if((i > lastChekedMessageID) && (messagesMap.get(i) == null)){ // if the message hasn't arrived yet

                        lastChekedMessageID = i;
                        // add the timestamp and message id to the missingMessages queue so it can be recovered
                        System.out.println(LocalDateTime.now() + " : The message with the id " + i + " was added to the missingMessages queue");
                        missingMessages.put(System.currentTimeMillis() + " " + i);
                    }
                }
            }
            System.out.println(LocalDateTime.now() + " : The number of contiguos messages is " + contiguosMessages);
            
            dataToProcess.put(messageId); // Add the messageId to the queue so the main thread can process the data
            
            saveDataToDisk(data); // save the data in the disk

            message = toDownloaderType + "\n" + downloaderId + "\n" + messageId;
            toBeSentMessages.put(message); // add the message to the toBeSentMessages queue
            
            System.out.println(LocalDateTime.now() + " : the message with the id " + messageId + " was saved in disk");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Sorts a map by its keys (relevance values) in descending order.
     *
     * @param map the map to be sorted
     * @return a new map with the entries sorted by keys in descending order
     */
    private Map<Integer, ArrayList<String>> sortByKeys(HashMap<Integer, ArrayList<String>> map) {
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

    /**
     * The main method is the entry point of the program.
     * It loads the configurations, creates a multicast socket, starts multiple threads, and receives messages from downloaders and other barrels.
     *
     * @param args The command line arguments. The first argument is the barrel ID, and the second argument is the path to the configuration file.
     */
    public static void main(String[] args) {

        int multticastTTL, maxMulticastBytes;
        String registryName, multicastAddress, multicastInterface;

        maxMulticastBytes = 0;
        SocketAddress socketAddress0 = null; // Downloaders
        NetworkInterface networkInterface = null;
        
        try {
            IndexStorageBarrel barrel = new IndexStorageBarrel(Integer.parseInt(args[0]));
            
            // ----------------- Load the configurations -----------------

            Properties prop = new Properties();
            InputStream input = new FileInputStream(args[1]);
            prop.load(input);

            registryName = prop.getProperty("registryName");

            barrel.multicastDownloadersPort = Integer.parseInt(prop.getProperty("multicastDownloadersPort"));
            barrel.multicastBarrelsPort = Integer.parseInt(prop.getProperty("multicastBarrelsPort"));
            multicastAddress = prop.getProperty("multicastAddress");
            multicastInterface = prop.getProperty("multicastInterface");
            multticastTTL = Integer.parseInt(prop.getProperty("multicastTTL"));
            maxMulticastBytes+= Integer.parseInt(prop.getProperty("maxBytesTitle"));
            maxMulticastBytes+= Integer.parseInt(prop.getProperty("maxBytesCitation"));
            maxMulticastBytes+= Integer.parseInt(prop.getProperty("maxBytesTokens"));
            maxMulticastBytes+= Integer.parseInt(prop.getProperty("maxBytesLinks"));
            barrel.messageRecoverySleepTime = Integer.parseInt(prop.getProperty("messageRecoverySleepTime"));

            System.out.println(LocalDateTime.now() + " : Configurations sucessfully loaded!");

            // ----------------- Multicast ----------------- and registry
            
            try { // Create a multicast socket
                barrel.gateway = (RMIGatewayBarrelsInterface) Naming.lookup(registryName);
                barrel.gateway.barrelSubscribe(barrel.barrelId, barrel);

                barrel.gateway.addResponseTimeList(barrel.barrelId,0L);

                barrel.socket = new MulticastSocket(barrel.multicastBarrelsPort);
                System.out.println(LocalDateTime.now() + " : Multicast socket created!");

                barrel.group = InetAddress.getByName(multicastAddress);
                socketAddress0 = new InetSocketAddress(barrel.group, barrel.multicastDownloadersPort);
                
                networkInterface = NetworkInterface.getByName(multicastInterface);
                barrel.socket.joinGroup(socketAddress0, networkInterface);
                
                barrel.socket.setTimeToLive(multticastTTL);
                
                // ----------------- Start the threads -----------------
    
                // initiate sender thread
                new Thread(() -> {
                    barrel.sender();
                }).start();
    
                // initiate a thread that picks the messages from the receivedMessages queue save it on ram and put the message id in the dataToProcess queue
                new Thread(() -> {
                    barrel.messageProcessor();
                }).start();
    
                // initiate a thread to process the data and save 
                new Thread(() -> {
                    barrel.dataprocessor();
                }).start();

                // initiate a thread to create the recover requests
                new Thread(() -> {
                    barrel.createRecoverRequest();
                }).start();
    
                // initiate a listener thread to receive the messages from the downloaders and other barrels (main thread)
                
                System.out.println(LocalDateTime.now() + " : Startup finished!");

                // Receive messages
                while (true) {
                    
                    byte[] buffer =  new byte[maxMulticastBytes+1000]; // 1000 just to be sure
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    barrel.socket.receive(packet);
                    
                    String received = new String(packet.getData(), 0, packet.getLength());
                    // System.out.println("Received: " + received);

                    barrel.receivedMessages.put(received);
                }
            } catch (IOException e) {
                System.out.println("Error creating multicast socket: " + e.getMessage());
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (barrel.socket != null) {
                    try {
                        barrel.socket.leaveGroup(socketAddress0, networkInterface);
                        
                        barrel.socket.close();
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