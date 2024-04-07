package rmi;

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
     * Retrieves the top searches performed by clients.
     * 
     * @return a list of top searches
     * @throws RemoteException if a remote communication error occurs
     */
    public List<String> topSearches() throws RemoteException;
    
    /**
     * Retrieves the list of barrels available in the system.
     * 
     * @return a list of barrels
     * @throws RemoteException if a remote communication error occurs
     */
    public List<Integer> getBarrelsList() throws RemoteException;
    
    /**
     * Retrieves the mean response time for each barrel in the system.
     * 
     * @return a list of mean response times by barrel
     * @throws RemoteException if a remote communication error occurs
     */
    public List<String> getMeanResponseTimeByBarrel() throws RemoteException;
}