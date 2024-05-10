package gateway;

import java.rmi.Remote;
import java.rmi.RemoteException;
import barrels.RMIBarrelsInterface;

/**
 * The RMIGatewayBarrelsInterface interface defines the methods for interacting with the gateway for barrels in a distributed system using RMI.
 * This interface extends the Remote interface to enable remote method invocation.
 */
public interface RMIGatewayBarrelsInterface extends Remote {
    
    /**
     * Subscribes a barrel with the given ID to the gateway.
     * 
     * @param id The ID of the barrel to subscribe.
     * @param barrel The RMIBarrelsInterface object representing the barrel to subscribe.
     * @throws RemoteException if a remote communication error occurs.
     */
    public void barrelSubscribe(int id, RMIBarrelsInterface barrel) throws RemoteException;
    
    /**
     * Unsubscribes a barrel with the given ID from the gateway.
     * 
     * @param id The ID of the barrel to unsubscribe.
     * @throws RemoteException if a remote communication error occurs.
     */
    public void barrelUnsubscribe(int id) throws RemoteException;
    
    /**
     * Adds the response time of a barrel to the response time list maintained by the gateway.
     * 
     * @param barrelId The ID of the barrel.
     * @param a The response time to add to the list.
     * @throws RemoteException if a remote communication error occurs.
     */
    public void addResponseTimeList(int barrelId, Long a) throws RemoteException;
}