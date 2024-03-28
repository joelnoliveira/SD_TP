package rmi;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.rmi.RemoteException;
import java.rmi.registry.Registry;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
import java.util.InputMismatchException;
import java.util.Properties;
import java.util.concurrent.LinkedBlockingQueue;

public class RMIGateway extends UnicastRemoteObject implements RMIGatewayRemote {

    private LinkedBlockingQueue<String> urlQueue;

    public RMIGateway() throws RemoteException {
        urlQueue = new LinkedBlockingQueue<>();
    }

    public void addUrl(String url) {
        urlQueue.add(url);
    }

    public String getNextUrl() throws InterruptedException {
        return urlQueue.take();
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
            
        } catch (IOException e) {
            e.printStackTrace();
            return;
        } catch (InputMismatchException e) {
            e.printStackTrace();
            return;
        }

        try {

            RMIGateway obj = new RMIGateway();
            Registry registry = LocateRegistry.createRegistry(port);
            registry.rebind(registryName, obj);
            System.out.println("RMIGateway is ready.");
            
            obj.addUrl(defaultURL);

        } catch (Exception e) {
            System.out.println("RMIGateway failed: " + e);
        }
    }
}