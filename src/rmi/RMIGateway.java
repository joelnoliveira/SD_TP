package rmi;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.rmi.RemoteException;
import java.rmi.registry.Registry;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
import java.time.LocalDateTime;
import java.util.InputMismatchException;
import java.util.Properties;
import java.util.concurrent.LinkedBlockingQueue;

public class RMIGateway extends UnicastRemoteObject implements RMIGatewayRemote {

    private int linkSearchDepth = 0, linkSearchCount = 0;
    private LinkedBlockingQueue<String> urlQueue;

    public RMIGateway() throws RemoteException {
        urlQueue = new LinkedBlockingQueue<>();
    }

    // private boolean wasUrlQueued(String url) {
            // TODO: FURUKAWA Implement this method
            // Maybe should be implemented by going to one barrel and checking if the URL is already there
            // This method should be used inside the addUrl method, in case the URL is already in the queue or in the barrels, then there is no need to add it again
    // }

    public synchronized void addUrl(String url) { 
        // synchronized is used to guarantee that the add url and checking if the URL is already there are atomic operations
        
        if(linkSearchCount > linkSearchDepth) return;
        // if (!wasUrlQueued(url)) {
            
            urlQueue.add(url);
            linkSearchCount++;
        // }
    }

    public String getNextUrl() throws InterruptedException {
        return urlQueue.take();
    }

    public static void main(String args[]) {

        int port;
        String defaultURL, registryName;

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
            
            try {
    
                // Creating the registry
                Registry registry = LocateRegistry.createRegistry(port);
                registry.rebind(registryName, gateway);
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