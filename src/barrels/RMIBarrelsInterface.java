package barrels;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Map;

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
     * Retrieves a set of URLs that contain the given search terms.
     *
     * @param terms the search terms
     * @return a LinkedHashSet of URLs that contain the search terms
     * @throws RemoteException if a remote communication error occurs
     */
    public LinkedHashSet<String> getURLsByTerms(String terms) throws RemoteException;
    
    /**
     * Retrieves a short quote from the given URL, limited to the specified character limit.
     *
     * @param url the URL to retrieve the quote from
     * @param characterLimit the maximum number of characters in the quote
     * @return a short quote from the URL
     * @throws RemoteException if a remote communication error occurs
     */
    public String getShortQuote(String url, int characterLimit) throws RemoteException;
    
    /**
     * Retrieves the title of the page associated with the given URL.
     *
     * @param url the URL to retrieve the page title from
     * @return the title of the page
     * @throws RemoteException if a remote communication error occurs
     */
    public String getPageTitle(String url) throws RemoteException;
    
    /**
     * Retrieves a list of URLs that link to the given URL.
     *
     * @param url the URL to retrieve the linking URLs from
     * @return an ArrayList of URLs that link to the given URL
     * @throws RemoteException if a remote communication error occurs
     */
    public ArrayList<String> getPagesThatLinksTo(String url) throws RemoteException;
    
    /**
     * Retrieves a map of searched terms and their frequencies.
     *
     * @return a Map containing the searched terms and their frequencies
     * @throws RemoteException if a remote communication error occurs
     */
    public Map<String, Integer> getSearchedTerms() throws RemoteException;
    
    /**
     * Retrieves the ID of the RMIBarrels object.
     *
     * @return the ID of the RMIBarrels object
     * @throws RemoteException if a remote communication error occurs
     */
    public int getBarrelID() throws RemoteException;
}