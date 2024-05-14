package meta1;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface RMIGatewayClientsInterface extends Remote {
    
    /**
     * Adds an indexer URL to the client's list of URLs.
     * 
     * @param url the URL to be added
     * @throws RemoteException if a remote communication error occurs
     * @throws InterruptedException if the thread is interrupted while waiting
     */
    public void clientAddIndexerUrl(String url) throws RemoteException, InterruptedException;
    
    /**
     * Searches for documents containing the specified keywords and returns a list of matching documents.
     * 
     * @param keywords the keywords to search for
     * @param shortQuoteCharLimit the character limit for short quotes in the search results
     * @return a list of matching documents
     * @throws RemoteException if a remote communication error occurs
     */
    public List<String> searchByTerms(String keywords, int shortQuoteCharLimit) throws RemoteException;
    
    /**
     * Retrieves the content of a document from the specified URL.
     * 
     * @param urlConsult the URL of the document to retrieve
     * @return the content of the document
     * @throws RemoteException if a remote communication error occurs
     */
    public List<String> consult(String urlConsult) throws RemoteException;
    
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