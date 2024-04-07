package rmi;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface RMIGatewayClientsInterface extends Remote {
    
    // public void clientSubscribe(RMIClientsInterface client) throws RemoteException;
    // public void clientUnsubscribe(RMIClientsInterface client) throws RemoteException;
    // public void clientNotify(String message) throws RemoteException;
    public void clientAddIndexerUrl(String url) throws RemoteException, InterruptedException;
    public List<String> searchByTerms(String keywords) throws RemoteException;
    public List<String> consult(String urlConsult) throws RemoteException;
    public List<String> topSearches() throws RemoteException;
    public List<Integer> getBarrelsList() throws RemoteException;
    public List<String> getMeanResponseTimeByBarrel() throws RemoteException;
}