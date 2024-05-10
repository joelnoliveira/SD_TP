package com.ds.googol.rmi;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * The RMIGatewayDownloadersInterface is a remote interface that defines the methods for interacting with the gateway downloaders in a distributed system.
 * This interface extends the Remote interface, which is the marker interface for remote objects.
 */
public interface RMIGatewayDownloadersInterface extends Remote {

    /**
     * Adds a URL to the gateway downloaders.
     * 
     * @param url the URL to be added
     * @throws RemoteException if a remote communication error occurs
     * @throws InterruptedException if the thread is interrupted while waiting
     */
    void addUrl(String url) throws RemoteException, InterruptedException;

    /**
     * Retrieves the next URL from the gateway downloaders.
     * 
     * @return the next URL
     * @throws RemoteException if a remote communication error occurs
     * @throws InterruptedException if the thread is interrupted while waiting
     */
    String getNextUrl() throws RemoteException, InterruptedException;

    /**
     * Retrieves a new message ID from the gateway downloaders.
     * 
     * @return the new message ID
     * @throws RemoteException if a remote communication error occurs
     */
    int getNewMessageId() throws RemoteException;
}