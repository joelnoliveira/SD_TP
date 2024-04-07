package barrels;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;

public interface RMIBarrelsInterface extends Remote {
    
    public void sayHI() throws RemoteException;
    public boolean hasURL(String url) throws RemoteException;
    public LinkedHashSet<String> getURLsByTerms(String terms) throws RemoteException;
    public String getShortQuote(String url)  throws RemoteException;
    public String getPageTitle(String url) throws RemoteException;
    public ArrayList<String> getPagesThatLinksTo(String url) throws RemoteException;
    public Map<String, Integer> getSearchedTerms() throws RemoteException;
    public int getBarrelID() throws RemoteException;
}