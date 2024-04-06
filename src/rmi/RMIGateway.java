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