package rmi;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface RMIGatewayDownloadersInterface extends Remote {

    void addUrl(String url) throws RemoteException, InterruptedException;
    String getNextUrl() throws RemoteException, InterruptedException;
    int getNewMessageId() throws RemoteException;
}