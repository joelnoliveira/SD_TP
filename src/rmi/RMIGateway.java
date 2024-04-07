package rmi;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.rmi.RemoteException;
import java.rmi.registry.Registry;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.InputMismatchException;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

import barrels.RMIBarrelsInterface;


public class RMIGateway extends UnicastRemoteObject implements RMIGatewayDownloadersInterface, RMIGatewayBarrelsInterface, RMIGatewayClientsInterface {

    private int linkSearchDepth = 0, linkSearchCount = 0;
    private AtomicInteger messageId = new AtomicInteger(0);
    private LinkedBlockingQueue<String> urlQueue;
    private HashMap<Integer, RMIBarrelsInterface> barrels = new HashMap<>();
    private Map<RMIBarrelsInterface, Long> responseTimes = new HashMap<>();

    public RMIGateway() throws RemoteException {
        urlQueue = new LinkedBlockingQueue<>();
    }

    // private boolean wasUrlQueued(String url) {
            // TODO: FURUKAWA Implement this method
            // Maybe should be implemented by going to one barrel and checking if the URL is already there
            // This method should be used inside the addUrl method, in case the URL is already in the queue or in the barrels, then there is no need to add it again
    // }

    public synchronized void clientAddIndexerUrl(String url) throws InterruptedException {
        
        urlQueue.put(url);
        linkSearchCount = 0; // Reset the linkSearchCount, since the client is adding a new URL to be recursively indexed
    }

    public synchronized void addUrl(String url) throws InterruptedException { 
        // synchronized is used to guarantee that the add url and checking if the URL is already there are atomic operations
        
        if(linkSearchCount > linkSearchDepth) return;
        // if (!wasUrlQueued(url)) {
            
            urlQueue.put(url);
            linkSearchCount++;
        // }
    }

    public boolean wasUrlQueued(String url) {
        return urlQueue.contains(url);
    }

    public boolean wasUrlIndexed(String url) throws RemoteException {
        try{
            for (RMIBarrelsInterface barrel : barrels.values()) {
            if (barrel.hasURL(url)) {
                return true;
            }
        }
        } catch (RemoteException e) {
            System.err.println("Erro ao verificar se a URL foi indexada: " + e.getMessage());
        }
        return false;
    }
    public String getNextUrl() throws InterruptedException {
        return urlQueue.take();
    }

    public int getNewMessageId() {
        return messageId.getAndIncrement(); // This method is atomic
    }

    public void barrelSubscribe(int id, RMIBarrelsInterface barrel) throws RemoteException{

        barrels.put(id, barrel);
        barrel.sayHI();
        System.out.println(LocalDateTime.now() + " : Barrel " + id + " subscribed.");
    }

    public void barrelUnsubscribe(int id) throws RemoteException {

        barrels.remove(id);
        System.out.println(LocalDateTime.now() +" : Barrel " + id + " unsubscribed.");
    }
   
    private int getRandomBarrel() {
        Random rand = new Random();
        List<Integer> keys = new ArrayList<Integer>(barrels.keySet());
        Integer randomKey = keys.get(rand.nextInt(keys.size()));
        return randomKey;
    }

    public List<Integer> getBarrelsList() throws RemoteException {
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

    public List<String> searchByTerms(String keywords) throws RemoteException {

        int barrelId = getRandomBarrel();
        RMIBarrelsInterface barrel = barrels.get(barrelId);
        
        // Get the start time of the search
        long startTime = System.currentTimeMillis();
        
        // Initialize a list to store the search results
        List<String> results = new ArrayList<String>();
        
        // Create a new thread to perform the search
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
        // Get the end time of the search
        long endTime = System.currentTimeMillis();
        
        // Calculate the search time
        long searchTime = endTime - startTime;
        
        long mean = responseTimes.get(barrel);
        if (mean == 0) {
            mean = searchTime;
        } else {
            mean = (mean + searchTime) / 2;
        }
        // Store the response time for the barrel
        responseTimes.put(barrel, mean);
        
        // Return the search results
        return groupResults(results);
    }

    private List<String> groupResults(List<String> results) {
        int i = 0;
        List<String> groupedResults = new ArrayList<>();
        for (String result : results) {
            // Add a new page header every 10 results
            if (i % 10 == 0) {
                int page = i / 10 + 1;
                groupedResults.add("\n-------- Page " + page + " --------\n");
            }
            // Add the result to the current page
            groupedResults.add(result);
            i++;
        }
        // Return the grouped results
        return groupedResults;
    }
    
    public List<String> consult(String urlConsult) throws RemoteException {

        int barrelId = getRandomBarrel();
        RMIBarrelsInterface barrel = barrels.get(barrelId);

        // Get the start time of the query
        long startTime = System.currentTimeMillis();
        // Initialize a list to store the query results
        List<String> results = new ArrayList<>();
        // Create a new thread to perform the query
        Thread consultThread = new Thread(() -> {
            try {
                // Get the list of pages that link to the specified URL
                List<String> urlsResults = barrel.getPagesThatLinksTo(urlConsult);
                // Check if the list of URLs is empty
                if (urlsResults.isEmpty()) {
                    // Add a message to the results list if no results were found
                    results.add("No results found");
                } else {
                    // Add the list of URLs to the results list
                    results.addAll(urlsResults);
                }

            } catch (RemoteException e) {
                // Threat remote communication exceptions while querying
                System.err.println("Error while querying: " + e.getMessage());
            }
        });
        // Start the query thread
        consultThread.start();

        try {
            // Wait for the query to complete
            consultThread.join();
        } catch (InterruptedException e) {
            // Handle exceptions while waiting for the query to complete
            System.out.println("Error while waiting for the query to complete: " + e.getMessage());
        }
        // Get the end time of the query
        long endTime = System.currentTimeMillis();
        // Calculate the query time
        long consultTime = endTime - startTime;

        long mean = responseTimes.get(barrel);
        if (mean == 0) {
            mean = consultTime;
        } else {
            mean = (mean + consultTime) / 2;
        }
        // Store the response time for the barrel
        responseTimes.put(barrel, mean);
        // Return the query results
        return results;
    }

    public List<String> topSearches() throws RemoteException {
        Map<String, Integer> mergedTerms = new HashMap<>();
        ArrayList<String> topTerms = new ArrayList<>();
        // Create a new thread to retrieve the top searched terms
        Thread topSearchesThread = new Thread(() -> {
            for (RMIBarrelsInterface barrel : barrels.values()) {
                Map<String, Integer> terms;
                try {
                    terms = barrel.getSearchedTerms();
                    mergeMap(mergedTerms, terms);
                System.out.println("Merged terms: " + mergedTerms);
                } catch (RemoteException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
            List<Map.Entry<String, Integer>> sortedEntries = new ArrayList<>(mergedTerms.entrySet());
            sortedEntries.sort(Map.Entry.<String, Integer>comparingByValue().reversed());

            List<Map.Entry<String, Integer>> topEntries = sortedEntries.subList(0, Math.min(10, sortedEntries.size()));

            for (Map.Entry<String, Integer> entry : topEntries) {
                topTerms.add(entry.getKey() + " - " + entry.getValue());
            }
            if (topTerms.isEmpty()) {
                topTerms.add("No search terms found");
            }
        });
        // Start the thread to retrieve the top searched terms
        topSearchesThread.start();

        try {
            topSearchesThread.join();
        } catch (InterruptedException e) {
            System.out.println("Error while waiting for the search to complete: " + e.getMessage());
        }
        return topTerms;
    }

    public List<String> getMeanResponseTimeByBarrel() throws RemoteException {
        List<String> responseTimesList = new ArrayList<>();
        for (RMIBarrelsInterface barrel : barrels.values()) {
            long times = responseTimes.get(barrel);
            int barrelId = barrel.getBarrelID();
            responseTimesList.add("Barrel - " + barrelId + "-" + times + "ms");
        }
        return responseTimesList;
    }

    private void mergeMap(Map<String, Integer> map1, Map<String, Integer> map2) {
        for (Map.Entry<String, Integer> entry : map2.entrySet()) {
            String termo = entry.getKey();
            int contador = entry.getValue();
            map1.put(termo, map1.getOrDefault(termo, 0) + contador);
        }
    }

    public void addResponseTimeList(RMIBarrelsInterface barrel, Long a) throws RemoteException {
        responseTimes.put(barrel,a);
    }
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
                
            } catch (Exception e) {
                System.out.println(LocalDateTime.now() + " : RMIGateway failed: " + e);
            }
        } catch (Exception e) {
            e.printStackTrace(); // TODO Consider printing in output stream like sout so it can be seen in the logfile
            return;
        }
        
    }
}