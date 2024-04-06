package rmi;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface RMIGatewayClientsInterface extends Remote {
    
    public void clientSubscribe(RMIClientsInterface client) throws RemoteException;
    // public void clientUnsubscribe(RMIClientsInterface client) throws RemoteException;
    // public void clientNotify(String message) throws RemoteException;
    public void clientAddIndexerUrl(String url) throws RemoteException, InterruptedException;
}