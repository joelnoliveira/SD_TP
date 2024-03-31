package rmi;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface InterfaceGateway extends Remote {
    void indexURL(String url) throws RemoteException;
    List<String> searchByTerms(String keywords) throws RemoteException;
    void consult(String url_consulta) throws RemoteException;
    void admin() throws RemoteException;
    String getNextUrl() throws RemoteException, InterruptedException;
    void addUrl(String attr);
}
