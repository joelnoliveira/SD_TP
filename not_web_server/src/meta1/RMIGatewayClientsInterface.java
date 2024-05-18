package meta1;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

/**
 * The RMIGatewayClientsInterface interface defines the methods that can be invoked remotely
 * on a RMIGatewayClients object. It extends the Remote interface to indicate that these methods
 * can be invoked remotely. This interface is a way for the clients to communicate with the Gateway.
 */
public interface RMIGatewayClientsInterface extends Remote {
    
    /**
     * Adds an indexer URL to the client's list of URLs.
     * 
     * @param url the URL to be added
     * @throws RemoteException if a remote communication error occurs
     * @throws InterruptedException if the thread is interrupted while waiting
     */
    public boolean clientAddIndexerUrl(String url) throws RemoteException, InterruptedException;
    

    /**
     * Searches for documents that contain the specified keywords.
     * 
     * @param keywords the keywords to search for
     * @param batchNumber the number of the batch to retrieve
     * @param shortQuoteCharLimit the maximum number of characters in a short quote
     * @return a list of documents that contain the specified keywords
     * @throws RemoteException if a remote communication error occurs
     */
    public List<String> searchByTerms(String keywords, int batchNumber, int shortQuoteCharLimit) throws RemoteException;

    /**
     * Retrieves the pages that link to a given URL.
     * 
     * @param urlConsult the URL to search for
     * @param batchNumber the number of the batch to retrieve
     * @return the pages that link to the URL
     * @throws RemoteException if a remote communication error occurs
     */
    public List<String> consultURL(String urlConsult, int batchNumber) throws RemoteException;
    
    /**
     * Subscribes the client to the admin console.
     * 
     * @param clientId the ID of the client to subscribe
     * @param client the RMIClientsInterface object used for communication with the client
     * @return a message indicating the result of the subscription
     * @throws RemoteException if a remote communication error occurs
     */
    public String subscribeAdminConsole(int clientId, RMIClientsInterface client) throws RemoteException;

    /**
     * Unsubscribes the client from the admin console.
     * 
     * @param clientId the ID of the client to unsubscribe
     * @throws RemoteException if a remote communication error occurs
     */
    public void unsubscribeAdminConsole(int clientId) throws RemoteException;
}