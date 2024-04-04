package rmi;

import java.io.FileInputStream;
import java.io.InputStream;
import java.rmi.RemoteException;
import java.rmi.registry.Registry;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

import barrels.IndexStorageBarrel;

public class RMIGateway extends UnicastRemoteObject implements RMIGatewayRemote {
    private Map<String, IndexStorageBarrel> barrels;
    private int linkSearchDepth = 0, linkSearchCount = 0;
    private LinkedBlockingQueue<String> urlQueue;
    private AtomicInteger messageId = new AtomicInteger(0);

    /**
     * Constructs a new RMIGateway object.
     * Initializes the URL queue and barrels hashmap.
     *
     * @throws RemoteException If a remote communication-related exception occurs.
     */
    public RMIGateway() throws RemoteException {
        urlQueue = new LinkedBlockingQueue<>();
        barrels = new HashMap<>();
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
        List<String> keys = new ArrayList<String>(barrels.keySet());
        String randomKey = keys.get(rand.nextInt(keys.size()));
        return barrels.get(randomKey);
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
            for (IndexStorageBarrel barrel : barrels.values()) {
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
            barrels.put("Barrel1", barrel1);
            barrels.put("Barrel2", barrel2);
            
            System.out.println("Connected to barrels");
            
        } catch (Exception e) {
            throw new RemoteException("Error connecting to barrels: " + e.getMessage());
        }
    }

    /**
     * Searches for web pages containing the specified keywords in the given barrel.
     *
     * @param keywords The keywords to search for.
     * @param barrel The IndexStorageBarrel to search in.
     * @return A list of search results containing page titles, URLs, and short quotes.
     * @throws RemoteException If a remote communication-related exception occurs.
     */
    public List<String> searchByTerms(String keywords, IndexStorageBarrel barrel) throws RemoteException {

        List<String> results = new ArrayList<String>();
        Thread searchThread = new Thread(() -> {
            try {
                HashSet<String> urls = barrel.getURLsByTerms(keywords);
                if (urls == null) {
                    results.add("Não foram encontrados resultados para a pesquisa");
                } else {
                    for (String url : urls) {
                        try {
                            results.add(barrel.getPageTitle(url) + " - " + url + "\n" + barrel.getShortQuote(url) + "\n");
                        } catch (RemoteException e) {
                            System.err.println("Erro ao obter título ou citação curta: " + e.getMessage());
                            e.printStackTrace();
                        }
                    }
                }
            } catch (RemoteException e) {
                System.err.println("Erro ao pesquisar: " + e.getMessage());
            }
        });

        searchThread.start();

        try {
            searchThread.join();
        } catch (InterruptedException e) {
            System.out.println("Erro ao aguardar a conclusão da pesquisa: " + e.getMessage());
        }

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
            for (String key : barrels.keySet()) {
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
    public long getResponseTime(IndexStorageBarrel barrel) throws RemoteException {
        long startTime = System.currentTimeMillis();
        try {
            barrel.ping();
        } catch (RemoteException e) {
            System.err.println("Erro ao obter tempo de resposta: " + e.getMessage());
        }
        long endTime = System.currentTimeMillis();

        long tempoResposta = endTime - startTime;

        return tempoResposta;
    }
    /**
     * Main method to start the RMIGateway.
     *
     * @param args Command-line arguments containing the configuration file path.
     */
    public static void main(String args[]) {
        int port;
        String defaultURL, registryName;
        int tries = 0;
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

            // Retry to start RMIGateway for 3 attempts
            while (tries < 3) {
                try {
                    // Creating the registry
                    Registry registry = LocateRegistry.createRegistry(port);
                    registry.rebind(registryName, gateway);
                    System.out.println(LocalDateTime.now() + " : RMIGateway is ready.");

                    // Add default URL to the queue
                    gateway.addUrl(defaultURL);
                    System.out.println(LocalDateTime.now() + " : Default URL added to the queue.");

                    break; // Exit loop if successful
                } catch (Exception e) {
                    System.out.println(LocalDateTime.now() + " : RMIGateway failed: " + e);
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException e1) {
                        e1.printStackTrace();
                        Thread.currentThread().interrupt();
                    }
                    tries++;
                }
            }
        } catch (Exception e2) {
            e2.printStackTrace(); // Print stack trace
            return;
        }
    }
}