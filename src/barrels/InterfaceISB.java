package barrels;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.LinkedHashSet;
import java.util.List;

public interface InterfaceISB extends Remote{

    void add2Queue(String url) throws RemoteException;

    void add2Index(String term, String url) throws RemoteException;

    void add2IndexedUrls(String term, String url) throws RemoteException;

    void addPageTitle(String url, String title) throws RemoteException;

    String getPageTitle(String url) throws RemoteException;

    LinkedHashSet<String> getURLsByTerms(String terms) throws RemoteException;

    String getShortQuote(String url) throws RemoteException;

    List<String> getURLsByLink(String url) throws RemoteException;

    void addPageContents(String url, String content) throws RemoteException;

    String getPageContents(String url) throws RemoteException;

    void addPageLinks(String url, List<String> link) throws RemoteException;

    List<String> getPageLinks(String url) throws RemoteException;

    void addPageLinksCount(String url, int count) throws RemoteException;

    int getPageLinksCount(String url) throws RemoteException;
}
