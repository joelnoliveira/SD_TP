package rmi;

import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;
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

import barrels.IndexStorageBarrel;
import downloader.URLData;

public class RMIGateway extends UnicastRemoteObject implements RMIGatewayRemote {
    private Map<String, IndexStorageBarrel> barrels;
    private int linkSearchDepth = 0, linkSearchCount = 0;
    private LinkedBlockingQueue<String> urlQueue;

    public RMIGateway() throws RemoteException {
        urlQueue = new LinkedBlockingQueue<>();
        barrels = new HashMap<>();
    }

    public IndexStorageBarrel getRandomBarrel() {
        Random rand = new Random();
        List<String> keys = new ArrayList<String>(barrels.keySet());
        String randomKey = keys.get(rand.nextInt(keys.size()));
        return barrels.get(randomKey);
    }

    public synchronized void addUrl(String url) { 
        // synchronized is used to guarantee that the add url and checking if the URL is already there are atomic operations
        
        if(linkSearchCount > linkSearchDepth) return;
            if (!wasUrlQueued(url) && !wasUrlIndexed(url)) {
            urlQueue.add(url);
        }
    }

    public boolean wasUrlQueued(String url) {
        return urlQueue.contains(url) ;
    }

    public boolean wasUrlIndexed(String url) {
        for (IndexStorageBarrel barrel : barrels.values()) {
            if (barrel.hasURL(url)) {
                return true;
            }
        }
        return false;
    }

    public String getNextUrl() throws InterruptedException {
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

    public List<String> searchByTerms(String keywords, IndexStorageBarrel barrel) throws RemoteException {
        System.out.println("Consulting keywords: " + keywords);
        HashSet<String> urls = barrel.getURLsByTerms(keywords);
        List<String> results = new ArrayList<String>();
    
        if (urls == null) {
            results.add("Não foram encontrados resultados para a pesquisa");
        } else {
            int i = 0;
            for (String url : urls) {
                if (i % 10 == 0) {
                    int page = i / 10 + 1;
                    results.add("\n-------- Página " + page + " --------\n");
                }
                results.add(barrel.getPageTitle(url));
                results.add(url);
                results.add(barrel.getShortQuote(url));
                results.add("\n");
                i++;
            }
        }
    
        return results;
    }

    public List<String> consult(String url_consulta, IndexStorageBarrel barrel) throws RemoteException {
        System.out.println("Consultando URL: " + url_consulta);
        List<String> urls_results = barrel.getPagesThatLinksTo(url_consulta);
        System.out.println("URLs encontrados: " + urls_results.size());
        List<String> results = new ArrayList<>();
    
        if (urls_results == null || urls_results.isEmpty()) {
            results.add("Nenhum resultado encontrado");
        } else {
            for (String result : urls_results) {
                results.add(result);
            }
        }
    
        return results;
    }   

    public List<String> topSearches(IndexStorageBarrel barrel) throws RemoteException {
        List<String> topTerms = barrel.getMostSearchedTerms(10);

        return topTerms;
    }

    public List<String> getBarrelsList(){
        List<String> list = new ArrayList<>();
        for (String key : barrels.keySet()) {
            list.add(key);
        }
        return list;
    }

    public LinkedBlockingQueue<String> getQueue() {
        return urlQueue;
    }

    public long calcularTempoResposta(IndexStorageBarrel barrel) throws RemoteException {
        long startTime = System.currentTimeMillis();

        long pongTime = barrel.ping();

        long endTime = System.currentTimeMillis();

        long tempoResposta = endTime - startTime;

        long tempoIdaVolta = endTime - pongTime;

        // Imprime o tempo de resposta e o tempo de ida e volta
        System.out.println("Tempo de resposta: " + tempoResposta + "ms");
        System.out.println("Tempo de ida e volta (round-trip time): " + tempoIdaVolta + "ms");

        return tempoResposta;
    }
    public static void main(String args[]) {

        int port;
        String defaultURL, registryName;
        int tries = 0;
        try {
            RMIGateway gateway = new RMIGateway();

            Properties prop = new Properties();
            InputStream input = new FileInputStream(args[0]);
            prop.load(input);

            port = Integer.parseInt(prop.getProperty("port"));
            defaultURL = prop.getProperty("defaultURL");
            registryName = prop.getProperty("registryName");
            gateway.linkSearchDepth = Integer.parseInt(prop.getProperty("linkSearchDepth"));
            System.out.println(LocalDateTime.now() + " : Configurations sucessfully loaded!");
            while(tries < 3){
                try {
                    // Creating the registry
                    Registry registry = LocateRegistry.createRegistry(port);
                    registry.rebind(registryName, gateway);
                    System.out.println(LocalDateTime.now() + " : RMIGateway is ready.");
                
                    gateway.addUrl(defaultURL);
                    System.out.println(LocalDateTime.now() + " : Default URL added to the queue.");
    
                } catch (Exception e) {
                    System.out.println(LocalDateTime.now() + " : RMIGateway failed: " + e);
                    try{
                        Thread.sleep(5000);
                    } catch (InterruptedException e1) {
                        e1.printStackTrace();
                        Thread.currentThread().interrupt();
                    }
                    tries++;
                }
            }
        } catch (Exception e2) {
            e2.printStackTrace(); // TODO Consider printing in output stream like sout so it can be seen in the logfile
            return;
        }
    }
}