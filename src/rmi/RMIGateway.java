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
import java.util.HashSet;
import java.util.InputMismatchException;
import java.util.List;
import java.util.Properties;
import java.util.Scanner;
import java.util.concurrent.LinkedBlockingQueue;
import barrels.IndexStorageBarrel;

public class RMIGateway extends UnicastRemoteObject implements InterfaceGateway {

    private int messageURLCounter = 0; // is incremented every time a URL is requested from the queue
    private LinkedBlockingQueue<String> urlQueue;
    private IndexStorageBarrel barrel;

    public RMIGateway() throws RemoteException {
        urlQueue = new LinkedBlockingQueue<>();
        barrel = new IndexStorageBarrel();
    }

    // private boolean wasUrlQueued(String url) {
            // TODO: FURUKAWA Implement this method
            // Maybe should be implemented by going to one barrel and checking if the URL is already there
            // This method should be used inside the addUrl method, in case the URL is already there, there is no need to add to the queue again
    // }

    public synchronized void addUrl(String url) { 
        // synchronized is used to guarantee that the add url and checking if the URL is already there are atomic operations
        
        // if (!wasUrlQueued(url)) {
            // urlQueue.add(url);
        urlQueue.add(url);
    }
    public String getNextUrl() throws InterruptedException {
        return messageURLCounter++ + " " + urlQueue.take();
    }

    // Vou colocar só as funções que tenho
    //
        public synchronized void indexURL(String url) throws RemoteException {
        //System.out.println("Indexing URL: " + url);
        if (this.urlQueue != null && !this.urlQueue.contains(url)) {
            this.urlQueue.add(url);
        }
    }
    public List<String> searchByTerms(String keywords) throws RemoteException {
        //System.out.println("Consulting keywords: " + keywords);
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
    public void consult(String url_consulta) {
        System.out.println("Consulting URL: " + url_consulta);
        List<String> urls_results = barrel.getURLsByLink(url_consulta);
        if (urls_results == null) {
            System.out.println("No results found");
        } else {
            for (String url : urls_results) {
                System.out.println(url);
            }
        }
    }
    public void admin() {
        //TODO: Admin page
        System.out.println("Admin page");
    }

    public static void main(String args[]) {

        int port;
        String defaultURL, registryName;

        try {

            Properties prop = new Properties();
            InputStream input = new FileInputStream(args[0]);
            prop.load(input);

            port = Integer.parseInt(prop.getProperty("port"));
            defaultURL = prop.getProperty("defaultURL");
            registryName = prop.getProperty("registryName");

            System.out.println(LocalDateTime.now() + " : Configurations sucessfully loaded!");
            
        } catch (IOException e) {
            e.printStackTrace(); // TODO Consider printing in output stream like sout so it can be seen in the logfile
            return;
        } catch (InputMismatchException e) {
            e.printStackTrace();
            return;
        }

        try {
            // Instantiating the object
            RMIGateway obj = new RMIGateway();
            System.out.println(LocalDateTime.now() + " : RMIGateway instantiated.");

            // Creating the registry
            Registry registry = LocateRegistry.createRegistry(port);
            registry.rebind(registryName, obj);
            System.out.println(LocalDateTime.now() + " : RMIGateway is ready.");
            
            obj.addUrl(defaultURL);
            System.out.println(LocalDateTime.now() + " : Default URL added to the queue.");

        } catch (Exception e) {
            System.out.println(LocalDateTime.now() + " : RMIGateway failed: " + e);
        }
    }
}