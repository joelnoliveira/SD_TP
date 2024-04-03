package rmi;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

import barrels.IndexStorageBarrel;

public interface RMIGatewayRemote extends Remote {

    List<String> searchByTerms(String keywords, IndexStorageBarrel barrel) throws RemoteException;

    List<String> consult(String url_consulta, IndexStorageBarrel barrel) throws RemoteException;

    List<String> topSearches(IndexStorageBarrel barrel) throws RemoteException;

    void connect2Barrel() throws RemoteException;

    IndexStorageBarrel getRandomBarrel() throws RemoteException;

    List<String> getBarrelsList() throws RemoteException;

    long calcularTempoResposta(IndexStorageBarrel barrel) throws RemoteException;

    LinkedBlockingQueue<String> getQueue() throws RemoteException;

    String getNextUrl() throws InterruptedException;

    void addUrl(String url) throws RemoteException;
}