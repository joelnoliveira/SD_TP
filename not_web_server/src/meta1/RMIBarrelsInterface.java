package meta1;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

/**
 * The RMIBarrelsInterface interface defines the methods that can be invoked remotely
 * on a RMIBarrels object. It extends the Remote interface to indicate that these methods
 * can be invoked remotely.
 */
public interface RMIBarrelsInterface extends Remote {
    
    /**
     * Checks if a given URL exists in the RMIBarrels object.
     *
     * @param url the URL to check
     * @return true if the URL exists, false otherwise
     * @throws RemoteException if a remote communication error occurs
     */
    public boolean hasURL(String url) throws RemoteException;
    
    /**
     * Retrieves the data of the pages that contain a given term.
     * 
     * @param term the term to search
     * @param batchNumber the batch number to retrieve
     * @param citationLen the length of the citation to retrieve
     * @return the data of the pages that contain the term
     * @throws RemoteException if a remote communication error occurs
     */
    public List<String> searchByTerms(String terms, int batchNumber, int citationLen) throws RemoteException;
    
    /**
     * Retrieves the pages that link to a given URL.
     *
     * @param url the URL to check
     * @param batchNumber the batch number to retrieve
     * @return the pages that link to the URL
     * @throws RemoteException if a remote communication error occurs
     */
    public List<String> getPagesThatLinksTo(String url, int batchNumber) throws RemoteException;
    
    /**
     * Retrieves the ID of the RMIBarrels object.
     *
     * @return the ID of the RMIBarrels object
     * @throws RemoteException if a remote communication error occurs
     */
    public int getBarrelID() throws RemoteException;
}