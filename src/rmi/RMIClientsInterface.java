package rmi;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface RMIClientsInterface extends Remote {
    public void notify(String message) throws RemoteException;
}