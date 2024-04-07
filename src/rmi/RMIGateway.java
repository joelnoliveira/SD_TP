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
import java.util.InputMismatchException;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

import barrels.IndexStorageBarrel;
import barrels.RMIBarrelsInterface;


public class RMIGateway extends UnicastRemoteObject implements RMIGatewayDownloadersInterface, RMIGatewayBarrelsInterface, RMIGatewayClientsInterface {

    private int linkSearchDepth = 0, linkSearchCount = 0;
    private AtomicInteger messageId = new AtomicInteger(0);
    private LinkedBlockingQueue<String> urlQueue;
    private List<RMIBarrelsInterface> barrels = new ArrayList<>(); // TODO consider use hasmap instead of list
    private List<RMIClientsInterface> clients = new ArrayList<>(); // consider use hasmap instead of list


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

    public String getNextUrl() throws InterruptedException {
        return urlQueue.take();
    }

    public int getNewMessageId() {
        return messageId.getAndIncrement(); // This method is atomic
    }

    public void barrelSubscribe(RMIBarrelsInterface barrel) throws RemoteException{
        barrels.add(barrel);
        barrel.sayHI();
    }

    public void clientSubscribe(RMIClientsInterface client) throws RemoteException {
        clients.add(client);
        client.notify("You are now subscribed to the gateway.");
    }

    private IndexStorageBarrel getRandomBarrel() {
        return (IndexStorageBarrel) barrels.get((int) (Math.random() * barrels.size()));
    }

    // public List<String> searchByTerms(String keywords) throws RemoteException {

    //     // Get the start time of the search
    //     long startTime = System.currentTimeMillis();
        
    //     // Initialize a list to store the search results
    //     List<String> results = new ArrayList<String>();

    //     // Get a random barrel
    //     IndexStorageBarrel barrel = getRandomBarrel();
        
    //     // Create a new thread to perform the search
    //     Thread searchThread = new Thread(() -> {
    //         try {
    //             // Get the URLs from the barrel by the given keywords
    //             HashSet<String> urls = barrel.getURLsByTerms(keywords);
                
    //             // Verifica se a lista de URLs é nula
    //             if (urls == null) {
    //                 // Add a message to the results list if no results were found
    //                 results.add("No results found");
    //             } else {
    //                 // For each URL in the list
    //                 for (String url : urls) {
    //                     try {
    //                         // Get the page title and short quote for the URL
    //                         String pageTitle = barrel.getPageTitle(url);
    //                         String shortQuote = barrel.getShortQuote(url);
    //                         // Add the page title, URL, and short quote to the results list
    //                         results.add(pageTitle + " - " + url + "\n" + shortQuote + "\n");
    //                     } catch (RemoteException e) {
    //                         // Threat remote communication exceptions while getting title or short quote
    //                         System.err.println("Error in obtaining short quote and/or title: " + e.getMessage());
    //                         e.printStackTrace();
    //                     }
    //                 }
    //             }
    //         } catch (RemoteException e) {
    //             // Threat remote communication exceptions while searching
    //             System.err.println("Error getting the search: " + e.getMessage());
    //         }
    //     });

    //     // Start the search thread
    //     searchThread.start();

    //     try {
    //         // Wait for the search to complete
    //         searchThread.join();
    //     } catch (InterruptedException e) {
    //         // Handle exceptions while waiting for the search to complete
    //         System.out.println("Error getting the search: " + e.getMessage());
    //     }
        
    //     // Get the end time of the search
    //     long endTime = System.currentTimeMillis();
        
    //     // Calculate the search time
    //     long searchTime = endTime - startTime;
        
    //     long mean = responseTimes.get(barrel);
    //     if (mean == 0) {
    //         mean = searchTime;
    //     } else {
    //         mean = (mean + searchTime) / 2;
    //     }
    //     // Store the response time for the barrel
    //     responseTimes.put(barrel, mean);
        
    //     // Return the search results
    //     return groupResults(results);
    // }
    
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