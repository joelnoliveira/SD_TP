/**
 * This class represents the RMIGateway, which acts as a remote gateway for clients to interact with the system.
 * It implements the RMIGatewayDownloadersInterface, RMIGatewayBarrelsInterface, and RMIGatewayClientsInterface interfaces.
 * The RMIGateway is responsible for managing the URL queue, interacting with barrels, and performing search and query operations.
 * It also keeps track of response times for each barrel.
 */
package meta1;

import java.io.FileInputStream;
import java.io.InputStream;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.Semaphore;

/**
 * The RMIGateway class represents a gateway for remote method invocation (RMI) in a distributed system.
 * It implements the RMIGatewayDownloadersInterface, RMIGatewayBarrelsInterface, and RMIGatewayClientsInterface interfaces.
 * This class is responsible for managing the URL queue, barrels, and client subscriptions.
 * It provides methods for adding URLs to the queue, checking if a URL has been queued or indexed, retrieving the next URL from the queue,
 * generating message IDs, subscribing and unsubscribing barrels, retrieving the list of barrels, searching for URLs by keywords,
 * querying for pages that link to a specific URL, retrieving the top searched terms, and more.
 * 
 * This class extends the UnicastRemoteObject class to enable remote method invocation.
 * It also uses various data structures such as LinkedBlockingQueue, HashMap, and List to store and manage data.
 * 
 * Note: This class assumes that the RMIBarrelsInterface interface is available and properly implemented by the barrels.
 * 
 * @see RMIGatewayDownloadersInterface
 * @see RMIGatewayBarrelsInterface
 * @see RMIGatewayClientsInterface
 */
/**
 * The RMIGateway class represents the gateway for the RMI-based search engine system.
 * It implements the RMIGatewayDownloadersInterface, RMIGatewayBarrelsInterface, and RMIGatewayClientsInterface interfaces.
 * This class is responsible for managing the URL queue, interacting with barrels, and performing search and query operations.
 */
public class RMIGateway extends UnicastRemoteObject implements RMIGatewayDownloadersInterface, RMIGatewayBarrelsInterface, RMIGatewayClientsInterface {

    private int linkSearchDepth = 0, linkSearchCount = 0;
    private AtomicInteger messageId = new AtomicInteger(0);
    private Semaphore mainSemaphore = new Semaphore(0);
    private LinkedBlockingQueue<String> urlQueue;

    private Map<Integer, RMIBarrelsInterface> barrels = new HashMap<>();
    private Map<Integer, Long> responseTimes = new HashMap<>();

    private Map<String, Integer> termCounts = new HashMap<>(); // The number of times a term was searched
    private Map<Integer, RMIClientsInterface> adminConsoleClients = new HashMap<>();
    private Map<Integer, Semaphore> adminConsoleSemaphores = new HashMap<>(); // the number of permits will increase with the number of updates

    /**
     * Represents an RMIGateway object.
     * This class provides a gateway for RMI communication.
     */
    public RMIGateway() throws RemoteException {
        urlQueue = new LinkedBlockingQueue<>();
    }
    
    /**
     * Retrieves the statistics for the administration page.
     * 
     * @return a string representing the statistics for the administration page
     */
    private String getStatistics() {

        StringBuilder sb = new StringBuilder();
        sb.append("-- Real-time updated administration page --\nMost searched terms:\n");
        List<String> res2 = topSearches();
        for (String r : res2) {
            sb.append(r + "\n");
        }
        List<Integer> barrels = getBarrelsList();
        sb.append("Connected barrels:\n");
        for (int b : barrels) {
            sb.append("Barrel" + b + "\n");
        }
        sb.append("Mean Response time:\n");
        List<String> res3 = getMeanResponseTimeByBarrel();
        for (String r : res3) {
            sb.append(r + "\n");
        }
        return sb.toString();
    }
    
    private void adminConsoleUpdater(int clientId) {
        
        int count = 0;

        while(adminConsoleClients.containsKey(clientId)){// check if the client is still subscribed
            
            try {
                adminConsoleSemaphores.get(clientId).acquire(); // wait for the semaphore to be released

                if(!adminConsoleClients.containsKey(clientId))break; // check if the client is still subscribed
                count = adminConsoleSemaphores.get(clientId).availablePermits(); // get the number of permits available

                adminConsoleClients.get(clientId).updateAdminConsole(getStatistics()); // update the admin console

                adminConsoleSemaphores.get(clientId).acquire(count); // update the semaphore

            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (RemoteException e) {
                
                adminConsoleClients.remove(clientId);
                adminConsoleSemaphores.remove(clientId);
                System.err.println( LocalDateTime.now() + " : Error while updating the admin console: " + e.getMessage());
                return;
            }
        }

        adminConsoleSemaphores.remove(clientId);
    }

    /**
     * Subscribes a client with the specified ID to the RMIGateway, and returns the current latest statistics.
     * 
     * @param clientId the ID of the client to subscribe
     * @param client the RMIClientsInterface object representing the client
     * @throws RemoteException if a remote communication error occurs
     */
    public String subscribeAdminConsole(int clientId, RMIClientsInterface client) throws RemoteException {

        adminConsoleSemaphores.put(clientId, new Semaphore(0));
        adminConsoleClients.put(clientId, client);
        System.out.println(LocalDateTime.now() + " : Admin console client " + clientId + " subscribed.");

        new Thread(() -> {
            
            adminConsoleUpdater(clientId); // start a new thread per client to update its admin console
        }).start();

        return getStatistics();
    }

    /**
     * Unsubscribes an admin console client with the specified ID from the RMIGateway.
     * 
     * @param clientId the ID of the client to unsubscribe
     * @throws RemoteException if a remote communication error occurs
     */
    public void unsubscribeAdminConsole(int clientId) throws RemoteException {

        adminConsoleClients.remove(clientId);
        System.out.println(LocalDateTime.now() + " : Admin console client " + clientId + " unsubscribed.");
    }

    /**
     * Adds a URL to the URL queue for indexing.
     * This method is synchronized to ensure thread safety.
     * 
     * @param url the URL to be added to the queue
     * @throws InterruptedException if the thread is interrupted while waiting to add the URL to the queue
     */
    public synchronized boolean clientAddIndexerUrl(String url) throws InterruptedException, RemoteException{
        
        // check if the URL is already in the queue or in a barrel, don't add it again
        if (wasUrlQueued(url) || wasUrlIndexed(url)) {
            System.out.println(LocalDateTime.now() + " : URL " + url + " was already queued or indexed.");
            return false;
        }

        urlQueue.put(url);
        linkSearchCount = 0; // Reset the linkSearchCount, since the client is adding a new URL to be recursively indexed
        return true;
    }

    /**
     * Adds a URL to the URL queue for processing.
     * 
     * @param url the URL to be added
     * @throws InterruptedException if the thread is interrupted while waiting to add the URL
     */
    public synchronized void addUrl(String url) throws InterruptedException { 
        // synchronized is used to guarantee that the add url and checking if the URL is already there are atomic operations
        
        if(linkSearchCount > linkSearchDepth) return;
        
        // If the URL is already in the queue or in a barrel, don't add it again
        try {
            if (wasUrlQueued(url) || wasUrlIndexed(url)) {

                System.out.println(LocalDateTime.now() + " : URL " + url + " was already queued or indexed.");
                return;
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        urlQueue.put(url);
        System.out.println(LocalDateTime.now() + " : URL " + url + " added to the queue.");
        linkSearchCount++;
    }

    /**
     * Checks if a given URL was queued for processing.
     *
     * @param url the URL to check
     * @return true if the URL was queued, false otherwise
     */
    public boolean wasUrlQueued(String url) {
        return urlQueue.contains(url);
    }

    /**
     * Checks if a given URL has been indexed by any of the RMIBarrelsInterface instances.
     *
     * @param url the URL to check
     * @return true if the URL has been indexed, false otherwise
     * @throws RemoteException if a remote exception occurs while checking the URL
     */
    public boolean wasUrlIndexed(String url) throws RemoteException {

        int barrelId;
        try{
            barrelId = getRandomBarrel();

            if (barrelId == -1)return false; // If there are no barrels, return false

            RMIBarrelsInterface barrel = barrels.get(barrelId);
            if (barrel.hasURL(url))return true;

        } catch (RemoteException e) {
            System.err.println("Erro ao verificar se a URL foi indexada: " + e.getMessage());
        }
        return false;
    }

    /**
     * Retrieves the next URL from the URL queue.
     *
     * @return the next URL in the queue
     * @throws InterruptedException if the thread is interrupted while waiting for the next URL
     */
    public String getNextUrl() throws InterruptedException {
        return urlQueue.take();
    }

    /**
     * Returns a new message ID.
     *
     * @return the new message ID
     */
    public int getNewMessageId() {
        return messageId.getAndIncrement(); // This method is atomic
    }

    /**
     * Subscribes a barrel with the given ID to the RMIGateway.
     * 
     * @param id the ID of the barrel to subscribe
     * @param barrel the RMIBarrelsInterface object representing the barrel
     * @throws RemoteException if a remote communication error occurs
     */
    public void barrelSubscribe(int id, RMIBarrelsInterface barrel) throws RemoteException{

        barrels.put(id, barrel);
        System.out.println(LocalDateTime.now() + " : Barrel " + id + " subscribed.");
        
        if(adminConsoleClients.size() > 0)mainSemaphore.release(); // only update statistics if there are clients connected, to prevent overflow of the counter
        
    }

    /**
     * Removes a barrel with the specified ID from the list of subscribed barrels.
     * 
     * @param id the ID of the barrel to unsubscribe
     * @throws RemoteException if a remote communication error occurs
     */
    public void barrelUnsubscribe(int id) throws RemoteException {

        barrels.remove(id);
        System.out.println(LocalDateTime.now() +" : Barrel " + id + " unsubscribed.");
        
        if(adminConsoleClients.size() > 0)mainSemaphore.release(); // only update statistics if there are clients connected, to prevent overflow of the counter
    }
   
    /**
     * Returns a random barrel from the barrels map.
     *
     * @return The key of a randomly selected barrel.
     */
    private int getRandomBarrel() {
        Random rand = new Random();
        List<Integer> keys = new ArrayList<Integer>(barrels.keySet());
        if (keys.isEmpty()) {
            return -1;
        }
        Integer randomKey = keys.get(rand.nextInt(keys.size()));
        return randomKey;
    }

    /**
     * Retrieves a list of barrel IDs.
     * 
     * @return A list of barrel IDs.
     * @throws RemoteException if a remote communication error occurs.
     */
    private List<Integer> getBarrelsList() {
        List<Integer> list = new ArrayList<>();
        try {
            for(int id : barrels.keySet()) {
                list.add(id);
            }
        } catch (Exception e) {
            System.err.println("Error while retrieving list of barrels: " + e.getMessage());
        }
        return list;
    }

    
    public List<String> searchByTerms(String keywords, int batchNumber, int shortQuoteCharLimit) throws RemoteException {

        int barrelId;
        long startTime, endTime, searchTime, mean;
        List<String> results;
        RMIBarrelsInterface barrel;
        
        barrelId = getRandomBarrel();
        if (barrelId == -1)return new ArrayList<>(); // If there are no barrels, return an empty list
        barrel = barrels.get(barrelId);
        

        // Get the start time of the search
        startTime = System.currentTimeMillis();
        
        results = barrel.searchByTerms(keywords, batchNumber, shortQuoteCharLimit);
        
        // Get the end time of the search
        endTime = System.currentTimeMillis();
        
        // Calculate the search time
        searchTime = endTime - startTime;
        
        mean = responseTimes.get(barrelId);
        if (mean == 0) {
            mean = searchTime;
        } else {
            mean = (mean + searchTime) / 2;
        }
        // Store the response time for the barrel
        responseTimes.put(barrelId, mean);

        for (String term : keywords.split(" ")) {
            
            // Update the term count
            if(!termCounts.containsKey(term))termCounts.put(term, 1);
            else termCounts.merge(term, 1, Integer::sum);
        }

        if(adminConsoleClients.size() > 0)mainSemaphore.release(); // only update statistics if there are clients connected, to prevent overflow of the counter
        
        // Return the search results
        return results;
    }
    
    public List<String> consultURL(String urlConsult, int batchNumber) throws RemoteException {
        
        int barrelId;
        long startTime, endTime, consultTime, mean;
        List<String> results;
        RMIBarrelsInterface barrel;

        
        barrelId = getRandomBarrel();
        if (barrelId == -1)return new ArrayList<>(); // If there are no barrels, return an empty list
        barrel = barrels.get(barrelId);

        // Get the start time of the query
        startTime = System.currentTimeMillis();

        
        // Get the list of pages that link to the specified URL
        results = barrel.getPagesThatLinksTo(urlConsult, batchNumber);

        // Get the end time of the query
        endTime = System.currentTimeMillis();
        // Calculate the query time
        consultTime = endTime - startTime;

        mean = responseTimes.get(barrelId);
        if (mean == 0) {
            mean = consultTime;
        } else {
            mean = (mean + consultTime) / 2;
        }
        // Store the response time for the barrel
        responseTimes.put(barrelId, mean);

        if(adminConsoleClients.size() > 0)mainSemaphore.release(); // only update statistics if there are clients connected, to prevent overflow of the counter

        // Return the query results
        return results;
    }

    
    private List<String> topSearches(){
        
        // Convert the map values to a list
        List<Map.Entry<String, Integer>> list = new ArrayList<>(termCounts.entrySet());
        
        // Sort the list in descending order of values
        list.sort(Map.Entry.<String, Integer>comparingByValue().reversed());

        // Get the top 10 keys
        List<String> top10 = new ArrayList<>();

        for (int i = 0; i < Math.min(10, list.size()); i++) {
            top10.add(list.get(i).getKey() + " - " + list.get(i).getValue());
        }


        return top10;
    }

    /**
     * Retrieves the mean response time for each barrel.
     * 
     * @return A list of strings representing the mean response time for each barrel.
     * @throws RemoteException If a remote communication error occurs.
     */
    private List<String> getMeanResponseTimeByBarrel() {

        long times;
        List<String> responseTimesList = new ArrayList<>();
        
        for (int barrelId : barrels.keySet()) {
            
            if(responseTimes.get(barrelId) == null)times = 0;
            else times = responseTimes.get(barrelId);
            responseTimesList.add("Barrel" + barrelId + " - " + times + " ms");
        }
        return responseTimesList;
    }

    /**
     * Adds a response time to the responseTimes map.
     * 
     * @param barrelId the ID of the barrel.
     * @param a the response time to be added.
     * @throws RemoteException if a remote communication error occurs.
     */
    public void addResponseTimeList(int barrelId, Long a) throws RemoteException {
        responseTimes.put(barrelId,a);
    }
    
    /**
     * The main method is the entry point of the RMIGateway application.
     * It loads the configurations from a properties file, creates the necessary RMI registries,
     * and adds a default URL to the queue if provided.
     *
     * @param args The command line arguments. The first argument should be the path to the properties file.
     */
    public static void main(String args[]) {

        int gatewayDownloadersPort, gatewayBarrelsPort, gatewayClientsPort;
        String defaultURL, gatewayDownloadersName, gatewayBarrelsName, gatewayClientsName;

        try {
            RMIGateway gateway = new RMIGateway();

            Properties prop = new Properties();
            InputStream input = new FileInputStream(args[0]);
            prop.load(input);

            gatewayDownloadersPort = Integer.parseInt(prop.getProperty("gatewayDownloadersPort"));
            gatewayDownloadersName = prop.getProperty("gatewayDownloadersName");
            gatewayBarrelsPort = Integer.parseInt(prop.getProperty("gatewayBarrelsPort"));
            gatewayBarrelsName = prop.getProperty("gatewayBarrelsName");
            gatewayClientsPort = Integer.parseInt(prop.getProperty("gatewayClientsPort"));
            gatewayClientsName = prop.getProperty("gatewayClientsName");

            defaultURL = prop.getProperty("defaultURL");
            gateway.linkSearchDepth = Integer.parseInt(prop.getProperty("linkSearchDepth"));

            System.out.println(LocalDateTime.now() + " : Configurations sucessfully loaded!");
            
            try {
    
                // Creating the registries
                LocateRegistry.createRegistry(gatewayDownloadersPort).rebind(gatewayDownloadersName, gateway); // So downloaders can acess the urlQueue and associated methods
                LocateRegistry.createRegistry(gatewayBarrelsPort).rebind(gatewayBarrelsName, gateway);
                LocateRegistry.createRegistry(gatewayClientsPort).rebind(gatewayClientsName, gateway);
                System.out.println(LocalDateTime.now() + " : RMIGateway is ready.");

                if(defaultURL.length()>0){

                    gateway.addUrl(defaultURL);
                    System.out.println(LocalDateTime.now() + " : Default URL added to the queue.");
                }

                // The main thread will now be responsible for updating the semaphores of the admin console clients

                int count;
                while(true){
                    
                    gateway.mainSemaphore.acquire(); // wait for the semaphore to be released
                    count = gateway.mainSemaphore.availablePermits(); // get the number of permits available

                    for(int clientId : gateway.adminConsoleClients.keySet()){

                        gateway.adminConsoleSemaphores.get(clientId).release(count+1); // update the semaphore, +1 beacuse 1 permit was released on acquire
                    }

                    gateway.mainSemaphore.acquire(count); // update main semaphore
                }
            } catch (Exception e) {
                System.out.println(LocalDateTime.now() + " : RMIGateway failed: " + e);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
        
    }
}