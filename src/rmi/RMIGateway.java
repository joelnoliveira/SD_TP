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

    // Não sei como você quer que escolha de qual barrel pegar, então fiz um random
    // Só que quando inicializar tem que adicionar os barrels no map

    public IndexStorageBarrel getRandomBarrel() {
        Random rand = new Random();
        List<String> keys = new ArrayList<String>(barrels.keySet());
        String randomKey = keys.get(rand.nextInt(keys.size()));
        return barrels.get(randomKey);
    }

    public synchronized void addUrl(String url) throws RemoteException { 
        // Synchronized is used to guarantee that the add url and checking if the URL is already there are atomic operations
        
        if(linkSearchCount > linkSearchDepth) return;
            if (!wasUrlQueued(url) && !wasUrlIndexed(url)) {
            urlQueue.add(url);
        }
    }

    public boolean wasUrlQueued(String url) {
        return urlQueue.contains(url) ;
    }

    public boolean wasUrlIndexed(String url) throws RemoteException {
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

        List<String> results = new ArrayList<String>();
        Thread searchThread = new Thread(() -> {
            try {
                HashSet<String> urls = barrel.getURLsByTerms(keywords);
                if (urls == null) {
                    results.add("Não foram encontrados resultados para a pesquisa");
                } else {
                    int i = 0;
                    for (String url : urls) {
                        if (i % 10 == 0) {
                            int page = i / 10 + 1;
                            results.add("\n-------- Página " + page + " --------\n");
                        }
                        try {
                            results.add(barrel.getPageTitle(url));
                            results.add(url);
                            results.add(barrel.getShortQuote(url));
                            results.add("\n");
                            i++;
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

        return results;
    }

    public List<String> consult(String url_consulta, IndexStorageBarrel barrel) throws RemoteException {

        List<String> results = new ArrayList<>();
        
        Thread consultThread = new Thread(()-> {
            try {
                List<String> urls_results = barrel.getPagesThatLinksTo(url_consulta);
                if (urls_results.isEmpty()) {
                    results.add("Nenhum resultado encontrado");
                } else {
                    results.addAll(urls_results);
                    }
            } catch (RemoteException e) {
                System.err.println("Erro ao consultar: " + e.getMessage());
            }
        });
        
       consultThread.start();

       try {
           consultThread.join();
        } catch (InterruptedException e) {
              System.out.println("Erro ao aguardar a conclusão da consulta: " + e.getMessage());
        }

        return results;
    }

    public List<String> topSearches(IndexStorageBarrel barrel) throws RemoteException {
        
        ArrayList<String> topTerms = new ArrayList<>();
        
        Thread topSearchesThread = new Thread(() -> {
            try {
                ArrayList<String> terms = barrel.getMostSearchedTerms(10);
                if (terms.isEmpty()) {
                    topTerms.add("Nenhum termo pesquisado");
                }
            } catch (RemoteException e) {
                System.err.println("Erro ao obter termos mais pesquisados: " + e.getMessage());
            }
        });
        
        topSearchesThread.start();

        try {
            topSearchesThread.join();
        } catch (InterruptedException e) {
            System.out.println("Erro ao aguardar a conclusão da pesquisa: " + e.getMessage());
        }

        return topTerms;
    }

    public List<String> getBarrelsList() throws RemoteException{
        List<String> list = new ArrayList<>();
        try {
            for (String key : barrels.keySet()) {
                list.add(key);
            }
        } catch (Exception e) {
            System.err.println("Erro ao obter lista de barrels: " + e.getMessage());
        }
        return list;
    }

    public LinkedBlockingQueue<String> getQueue() {
        return urlQueue;
    }

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