package rmi;

import java.io.FileInputStream;
import java.io.InputStream;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.Registry;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import java.util.Iterator;

import barrels.IndexStorageBarrel;

public class RMIGateway extends UnicastRemoteObject implements RMIGatewayRemote {

    private ExecutorService executorService;
    private static final long HEARTBEAT_INTERVAL = 60;
    private static final long TIMEOUT = 15000;
    private Map<String, IndexStorageBarrel> activeBarrels;
    private int linkSearchDepth = 0, linkSearchCount = 0;
    private LinkedBlockingQueue<String> urlQueue;
    private AtomicInteger messageId = new AtomicInteger(0);
    private Map<IndexStorageBarrel, Long> responseTimes = new HashMap<>();


    /**
     * Constructs a new RMIGateway object.
     * Initializes the URL queue and barrels hashmap.
     *
     * @throws RemoteException If a remote communication-related exception occurs.
     */
    public RMIGateway() throws RemoteException {
        urlQueue = new LinkedBlockingQueue<>();
        activeBarrels = new HashMap<>();
    }


    /**
     * Gets a new unique message ID.
     *
     * @return The new message ID.
     */
    public int getNewMessageId() {
        return messageId.getAndIncrement(); // This method is atomic
    }

    /**
     * Retrieves a random IndexStorageBarrel from the collection of barrels.
     *
     * @return A random IndexStorageBarrel.
     */
    public IndexStorageBarrel getRandomBarrel() {
        Random rand = new Random();
        List<String> keys = new ArrayList<String>(activeBarrels.keySet());
        String randomKey = keys.get(rand.nextInt(keys.size()));
        return activeBarrels.get(randomKey);
    }

    /**
     * Adds a URL to the URL queue if it has not already been queued or indexed.
     * Synchronized to ensure atomicity between adding the URL and checking its status.
     *
     * @param url The URL to be added.
     * @throws RemoteException If a remote communication-related exception occurs.
     */
    public synchronized void addUrl(String url) throws RemoteException {
        // Synchronized is used to guarantee that the add URL and checking if the URL is already there are atomic operations

        if (linkSearchCount > linkSearchDepth) return;
        if (!wasUrlQueued(url) && !wasUrlIndexed(url)) {
            urlQueue.add(url);
        }
    }


    /**
     * Checks if a URL has already been queued for indexing.
     *
     * @param url The URL to check.
     * @return True if the URL has been queued, false otherwise.
     */
    public boolean wasUrlQueued(String url) {
        return urlQueue.contains(url);
    }

    /**
     * Checks if a URL has already been indexed in any of the barrels.
     *
     * @param url The URL to check.
     * @return True if the URL has been indexed, false otherwise.
     * @throws RemoteException If a remote communication-related exception occurs.
     */
    public boolean wasUrlIndexed(String url) throws RemoteException {
        try{
            for (IndexStorageBarrel barrel : activeBarrels.values()) {
            if (barrel.hasURL(url)) {
                return true;
            }
        }
        } catch (RemoteException e) {
            System.err.println("Erro ao verificar se a URL foi indexada: " + e.getMessage());
        }
        return false;
    }

    /**
     * Retrieves the next URL from the URL queue.
     * If the queue is empty, it returns null.
     *
     * @return The next URL in the queue, or null if the queue is empty.
     * @throws InterruptedException If the thread is interrupted while waiting.
     */
    public String getNextUrl() throws InterruptedException {
        try {
            if (urlQueue.isEmpty()) {
                return null;
            }
        } catch (Exception e) {
            System.err.println("Error while getting the next URL: " + e.getMessage());
        }
        return urlQueue.take();
    }

    public void connect2Barrel() throws RemoteException{
        try {
            //Connect to barrels
            Registry registry = LocateRegistry.getRegistry("localhost", 1098);
            IndexStorageBarrel barrel1 = (IndexStorageBarrel) registry.lookup("Barrel1");
            IndexStorageBarrel barrel2 = (IndexStorageBarrel) registry.lookup("Barrel2");
           
            //Add barrels to map
            activeBarrels.put("Barrel1", barrel1);
            activeBarrels.put("Barrel2", barrel2);
            
            System.out.println("Connected to barrels");
            
        } catch (RemoteException re) {
            retryConnection();
            System.err.println("Error: " + re.getMessage());
        } catch (NotBoundException nbe) {
            System.err.println("Error: " + nbe.getMessage());
        }
    }
    /**
     * Retries to connect to the barrels.
     */
    public void retryConnection(){
        int MAX_RETRIES = 3;
        int numRetries = 0;
        boolean connected = false;
        while (numRetries < MAX_RETRIES && !connected) {
            try {
                //Connect to barrels
            Registry registry = LocateRegistry.getRegistry("localhost", 1098);
            IndexStorageBarrel barrel1 = (IndexStorageBarrel) registry.lookup("Barrel1");
            IndexStorageBarrel barrel2 = (IndexStorageBarrel) registry.lookup("Barrel2");
           
            //Add barrels to map
            activeBarrels.put("Barrel1", barrel1);
            activeBarrels.put("Barrel2", barrel2);
            
            System.out.println("Connected to barrels");
            } catch (RemoteException re) {
                System.err.println("Error: " + re.getMessage());
                try {
                    Thread.sleep(1000); // Wait 1 second before trying again
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
                numRetries++;
            } catch (NotBoundException nbe) {
            System.err.println("Error: " + nbe.getMessage());
        }
    }
        if (!connected) {
            System.err.println("Failed to reconnect after " + MAX_RETRIES + " retries. Exiting...");
            System.exit(1);
        }
    }

    /**
     * Realiza uma pesquisa assíncrona nos barris de armazenamento de índice por termos-chave e retorna os resultados.
     * 
     * @param keywords As palavras-chave usadas na pesquisa.
     * @param barrel O barril de armazenamento de índice onde a pesquisa será realizada.
     * @return Uma lista de strings contendo os resultados da pesquisa.
     * @throws RemoteException Se ocorrer um problema de comunicação remota.
     */
    public List<String> searchByTerms(String keywords, IndexStorageBarrel barrel) throws RemoteException {
        // Get the start time of the search
        long startTime = System.currentTimeMillis();
        
        // Initialize a list to store the search results
        List<String> results = new ArrayList<String>();
        
        // Create a new thread to perform the search
        Thread searchThread = new Thread(() -> {
            try {
                // Get the URLs from the barrel by the given keywords
                HashSet<String> urls = barrel.getURLsByTerms(keywords);
                
                // Verifica se a lista de URLs é nula
                if (urls == null) {
                    // Add a message to the results list if no results were found
                    results.add("No results found");
                } else {
                    // For each URL in the list
                    for (String url : urls) {
                        try {
                            // Get the page title and short quote for the URL
                            String pageTitle = barrel.getPageTitle(url);
                            String shortQuote = barrel.getShortQuote(url);
                            // Add the page title, URL, and short quote to the results list
                            results.add(pageTitle + " - " + url + "\n" + shortQuote + "\n");
                        } catch (RemoteException e) {
                            // Threat remote communication exceptions while getting title or short quote
                            System.err.println("Error in obtaining short quote and/or title: " + e.getMessage());
                            e.printStackTrace();
                        }
                    }
                }
            } catch (RemoteException e) {
                // Threat remote communication exceptions while searching
                System.err.println("Error getting the search: " + e.getMessage());
            }
        });

        // Start the search thread
        searchThread.start();

        try {
            // Wait for the search to complete
            searchThread.join();
        } catch (InterruptedException e) {
            // Handle exceptions while waiting for the search to complete
            System.out.println("Error getting the search: " + e.getMessage());
        }
        
        // Get the end time of the search
        long endTime = System.currentTimeMillis();
        
        // Calculate the search time
        long searchTime = endTime - startTime;
        
        // Store the response time for the barrel
        responseTimes.put(barrel, searchTime);
        
        // Return the search results
        return groupResults(results);
    }

    /**
     * Groups the search results into pages, with each page containing up to 10 results.
     *
     * @param results The list of search results to be grouped.
     * @return A list of grouped search results, each page containing up to 10 results.
     */
    public List<String> groupResults(List<String> results) {
        int i = 0;
        List<String> groupedResults = new ArrayList<>();
        for (String result : results) {
            // Add a new page header every 10 results
            if (i % 10 == 0) {
                int page = i / 10 + 1;
                groupedResults.add("\n-------- Page " + page + " --------\n");
            }
            groupedResults.add(result);
            i++;
        }
        return groupedResults;
    }

    /**
     * Retrieves a list of pages linking to the specified URL from the given barrel.
     *
     * @param url_consulta The URL to query.
     * @param barrel The IndexStorageBarrel to query.
     * @return A list of pages linking to the specified URL.
     * @throws RemoteException If a remote communication-related exception occurs.
     */
    public List<String> consult(String url_consulta, IndexStorageBarrel barrel) throws RemoteException {

        long startTime = System.currentTimeMillis();
        List<String> results = new ArrayList<>();
        
        Thread consultThread = new Thread(() -> {
            try {
                List<String> urlsResults = barrel.getPagesThatLinksTo(url_consulta);
                if (urlsResults.isEmpty()) {
                    results.add("No results found");
                } else {
                    results.addAll(urlsResults);
                }
            } catch (RemoteException e) {
                System.err.println("Error while querying: " + e.getMessage());
            }
        });
        
        consultThread.start();

        try {
            consultThread.join();
        } catch (InterruptedException e) {
            System.out.println("Error while waiting for the query to complete: " + e.getMessage());
        }
        long endTime = System.currentTimeMillis();
        long consultTime = endTime - startTime;
        responseTimes.put(barrel, consultTime);
        return results;
    }

    /**
     * Retrieves the top searched terms from the given barrel.
     *
     * @param barrel The IndexStorageBarrel to retrieve the top searched terms from.
     * @return A list of the top searched terms.
     * @throws RemoteException If a remote communication-related exception occurs.
     */
    public List<String> topSearches(IndexStorageBarrel barrel) throws RemoteException {
        
        long startTime = System.currentTimeMillis();
        ArrayList<String> topTerms = new ArrayList<>();
        
        Thread topSearchesThread = new Thread(() -> {
            try {
                ArrayList<String> terms = barrel.getMostSearchedTerms(10);
                if (terms.isEmpty()) {
                    topTerms.add("No searched terms found");
                } else {
                    topTerms.addAll(terms);
                }
            } catch (RemoteException e) {
                System.err.println("Error while retrieving top searched terms: " + e.getMessage());
            }
        });
        
        topSearchesThread.start();

        try {
            topSearchesThread.join();
        } catch (InterruptedException e) {
            System.out.println("Error while waiting for the search to complete: " + e.getMessage());
        }
        long endTime = System.currentTimeMillis();
        long topSearchesTime = endTime - startTime;
        responseTimes.put(barrel, topSearchesTime);
        return topTerms;
    }


    /**
     * Retrieves a list of barrels available in the system.
     *
     * @return A list of barrel names.
     * @throws RemoteException If a remote communication-related exception occurs.
     */
    public List<String> getBarrelsList() throws RemoteException {
        List<String> list = new ArrayList<>();
        try {
            for (String key : activeBarrels.keySet()) {
                list.add(key);
            }
        } catch (Exception e) {
            System.err.println("Error while retrieving list of barrels: " + e.getMessage());
        }
        return list;
    }

    /**
     * Retrieves the URL queue used for indexing.
     *
     * @return The URL queue.
     */
    public LinkedBlockingQueue<String> getQueue() throws RemoteException{
        return urlQueue;
    }

    /**
     * Retrieves the response time from the given barrel.
     *
     * @param barrel The IndexStorageBarrel to measure response time from.
     * @return The response time in milliseconds.
     * @throws RemoteException If a remote communication-related exception occurs.
     */
    public List<String> getMeanResponseTimeByBarrel() throws RemoteException {
        long sum = 0;
        long mean = 0;
        Map <IndexStorageBarrel,Long> meanResponseTimes = new HashMap<>();
        for (IndexStorageBarrel barrel : activeBarrels.values()) {
            for (Long time : responseTimes.values()) {
                sum += time;
            }
            mean = sum / responseTimes.size();
            meanResponseTimes.put(barrel, mean);
        }
        List<String> responseTimesList = new ArrayList<>();
        for (Map.Entry<IndexStorageBarrel, Long> entry : meanResponseTimes.entrySet()) {
            responseTimesList.add(entry.getKey().toString() + " - " + entry.getValue() + "ms");
        }
        return responseTimesList;
    }

    private void startHeartbeat() {
        Thread heartThread = new Thread(() -> {
            while (true) {
                try{
                    checkHeartbeat();
                } catch (RemoteException e) {
                    System.err.println("Error while checking heartbeat: " + e.getMessage());
                }
                try {
                    Thread.sleep(HEARTBEAT_INTERVAL);
                } catch (InterruptedException e) {
                    System.err.println("Error while sleeping: " + e.getMessage());
                }
            }
            });
            heartThread.start();
        }

    /**
     * Checks the heartbeat of the barrels.
     * If a barrel does not respond within the timeout period, it is removed from the active barrels list.
     * 
     * @throws RemoteException If a remote communication-related exception occurs.
     * 
     */
    private void checkHeartbeat() throws RemoteException{
        List<IndexStorageBarrel> barrelsToRemove = new ArrayList<>();
        for (IndexStorageBarrel barrel : activeBarrels.values()) {
            try {
                long startTime = System.currentTimeMillis();
                barrel.ping();
                long endTime = System.currentTimeMillis();
                long responseTime = endTime - startTime;
                if (responseTime > TIMEOUT) {
                    barrelsToRemove.add(barrel);
                }
            }catch (RemoteException e) {
                barrelsToRemove.add(barrel);
            }
        }
        for (String key : activeBarrels.keySet()) {
            if (barrelsToRemove.contains(activeBarrels.get(key))) {
                activeBarrels.remove(key);
            }
        }
    }

    /**
     * Main method to start the RMIGateway.
     *
     * @param args Command-line arguments containing the configuration file path.
     */
    public static void main(String args[]) {
        int port;
        String defaultURL, registryName;
        try {
            // Create RMIGateway instance
            RMIGateway gateway = new RMIGateway();

            // Load configurations from properties file
            Properties prop = new Properties();
            InputStream input = new FileInputStream(args[0]);
            prop.load(input);

            port = Integer.parseInt(prop.getProperty("port"));
            defaultURL = prop.getProperty("defaultURL");
            registryName = prop.getProperty("registryName");
            gateway.linkSearchDepth = Integer.parseInt(prop.getProperty("linkSearchDepth"));
            System.out.println(LocalDateTime.now() + " : Configurations successfully loaded!");

            try {
                    // Creating the registry
                    Registry registry = LocateRegistry.createRegistry(port);
                    registry.rebind(registryName, gateway);
                    System.out.println(LocalDateTime.now() + " : RMIGateway is ready.");

                    // Add default URL to the queue
                    gateway.addUrl(defaultURL);
                    System.out.println(LocalDateTime.now() + " : Default URL added to the queue.");

                } catch (Exception e) {
                    System.out.println(LocalDateTime.now() + " : RMIGateway failed: " + e);
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException e1) {
                        e1.printStackTrace();
                        Thread.currentThread().interrupt();
                    }
                }
        } catch (Exception e2) {
            e2.printStackTrace(); // Print stack trace
            return;
        }
    }
}