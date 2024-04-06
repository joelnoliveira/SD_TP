package rmi;

import java.io.FileInputStream;
import java.io.InputStream;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Properties;

public class RMIClient extends UnicastRemoteObject implements RMIClientsInterface{

    private int id;


    public RMIClient() throws Exception{
        
    }

    public void notify(String message) throws RemoteException{
        System.out.println("Client " + id + " received: " + message);
    }

    public static void main(String[] args) {
        
        String registryName;

        try{
            RMIClient client = new RMIClient();
            client.id = Integer.parseInt(args[0]);

            Properties prop = new Properties();
            InputStream input = new FileInputStream(args[1]);
            prop.load(input);

            registryName = prop.getProperty("registryName");

            RMIGatewayClientsInterface gateway = (RMIGatewayClientsInterface) Naming.lookup(registryName);

            gateway.clientSubscribe(client);

        }catch(Exception e){
            e.printStackTrace();
        }
    }
}
