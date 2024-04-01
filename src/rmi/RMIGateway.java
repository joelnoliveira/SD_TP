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
import java.util.Scanner;
import java.util.concurrent.LinkedBlockingQueue;
import barrels.InterfaceISB;

public class RMIGateway extends UnicastRemoteObject implements InterfaceGateway {

    private int messageURLCounter = 0; // is incremented every time a URL is requested from the queue
    private LinkedBlockingQueue<String> urlQueue;
    private Map<String, InterfaceISB> barrels;

    public RMIGateway() throws RemoteException {
        urlQueue = new LinkedBlockingQueue<>();
        barrels = new HashMap<>();
    }

    public String getNextUrl() throws InterruptedException {
        return messageURLCounter++ + " " + urlQueue.take();
    }

    public InterfaceISB getRandomBarrel() {
        Random rand = new Random();
        List<String> keys = new ArrayList<String>(barrels.keySet());
        String randomKey = keys.get(rand.nextInt(keys.size()));
        return barrels.get(randomKey);
    }

    public void connect2Barrel() throws RemoteException{
        try {
            //Connect to barrels
            Registry registry = LocateRegistry.getRegistry("localhost", 1099);
            InterfaceISB barrel1 = (InterfaceISB) registry.lookup("Barrel1");
            InterfaceISB barrel2 = (InterfaceISB) registry.lookup("Barrel2");
           
            //Add barrels to map
            barrels.put("Barrel1", barrel1);
            barrels.put("Barrel2", barrel2);
            
            System.out.println("Connected to barrels");
            
        } catch (Exception e) {
            throw new RemoteException("Error connecting to barrels: " + e.getMessage());
        }
    }
    public synchronized void addUrl2Queue(String url) throws RemoteException {
        System.out.println("Indexing URL: " + url);
        if (urlQueue != null && !urlQueue.contains(url) && !getRandomBarrel().getURLs().contains(url)) {
            urlQueue.add(url);
        }
    }
    public List<String> searchByTerms(String keywords, InterfaceISB barrel) throws RemoteException {
        System.out.println("Consulting keywords: " + keywords);
        HashSet<String> urls = barrel.getURLsByTerms(keywords);
        List<String> results = new ArrayList<String>();

        if (urls == null) {
            results.add("No results found");
        } else {
            int i = 0;
            for (String url : urls) {
                if (i % 10 == 0) {
                    int pagina = i / 10 + 1;
                    results.add("\n-------- Página " + pagina + " --------\n");
                }
                results.add(barrel.getPageTitle(url));
                results.add(url);
                results.add(barrel.getShortQuote(url));
                if (i % 10 == 0) {
                    System.out.println("Para passar à próxima página, pressione ENTER");
                    Scanner scanner = new Scanner(System.in);
                    scanner.nextLine();
                    scanner.close();
                }
                i++;
            }
        }
        return results;
    }
    public List<String> consult(String url_consulta, InterfaceISB barrel) throws RemoteException {
        System.out.println("Consulting URL: " + url_consulta);
        List<String> urls_results = barrel.getPagesThatLinksTo(url_consulta);
        if (urls_results == null) { 
            urls_results = new ArrayList<String>();
            urls_results.add("No results found");
        }
        return urls_results;
    } 
    public void admin() {
        //TODO: Admin page
        System.out.println("Admin page");
    }

    public static void main(String args[]) {
        int tries = 0;
        int port;
        //String defaultURL; 
        String registryName;

        try {

            Properties prop = new Properties();
            InputStream input = new FileInputStream(args[0]);
            prop.load(input);

            port = Integer.parseInt(prop.getProperty("port"));
            //defaultURL = prop.getProperty("defaultURL");
            registryName = prop.getProperty("registryName");

            System.out.println(LocalDateTime.now() + " : Configurations sucessfully loaded!");
            
        } catch (IOException e) {
            e.printStackTrace(); // TODO Consider printing in output stream like sout so it can be seen in the logfile
            return;
        } catch (InputMismatchException e) {
            e.printStackTrace();
            return;
        }
        while (tries < 3) {
            try{
                RMIGateway gw = new RMIGateway();
                System.out.println(LocalDateTime.now() + " : RMIGateway instantiated.");

                //Creating the registry
                Registry registry = LocateRegistry.createRegistry(port);
                registry.rebind(registryName, gw);

                System.out.println(LocalDateTime.now() + " : RMIGateway is ready.");
                System.out.println("Press ENTER to exit.");
                Scanner scanner = new Scanner(System.in);
                scanner.nextLine();
                scanner.close();
                System.exit(0);
            } catch (Exception e) {
                System.out.println(LocalDateTime.now() + " : RMIGateway failed: " + e);
                System.out.println("Retrying in 5 seconds...");
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e1) {
                    e1.printStackTrace();
                }
                tries++;
            }
            if (tries == 3) {
                System.out.println(LocalDateTime.now() + " : RMIGateway failed 3 times. Exiting...");
                System.exit(1);
            }
        }
    }
}