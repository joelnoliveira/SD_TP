package rmi;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

import barrels.InterfaceISB;

public interface RMIGatewayRemote extends Remote {
    
    void addUrl2Queue(String url) throws RemoteException;

    List<String> searchByTerms(String keywords, InterfaceISB barrel) throws RemoteException;

    List<String> consult(String url_consulta, InterfaceISB barrel) throws RemoteException;

    void admin() throws RemoteException;

    void connect2Barrel() throws RemoteException;

    InterfaceISB getRandomBarrel() throws RemoteException;

    String getNextUrl() throws InterruptedException, RemoteException;
}