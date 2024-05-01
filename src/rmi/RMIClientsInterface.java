package rmi;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * The RMIClientsInterface interface defines the methods that can be invoked by the server to notify clients.
 * It extends the Remote interface to support remote method invocation.
 */
public interface RMIClientsInterface extends Remote {
    /**
     * Notifies the client with a message.
     *
     * @param message the message to be sent to the client
     * @throws RemoteException if there is a communication error during the remote method invocation
     */
    public void notify(String message) throws RemoteException;
}