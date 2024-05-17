package meta1;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * The RMIClientsInterface interface defines the methods that can be invoked by the server to notify clients.
 * It extends the Remote interface to support remote method invocation.
 */
public interface RMIClientsInterface extends Remote {
    
    /**
     * Updates the client's administration console with a message. This method is called by the Gateway.
     * 
     * @param message the message to display
     * @throws RemoteException if a remote communication error occurs
     */
    public void updateAdminConsole(String message) throws RemoteException;
}