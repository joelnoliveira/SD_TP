package com.ds.googol.model;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

import org.springframework.messaging.simp.SimpMessagingTemplate;

import meta1.RMIClientsInterface;
import meta1.RMIGatewayClientsInterface;
import com.ds.googol.model.Server;

/**
 * The Server class represents a server in the distributed system.
 * It implements the RMIClientsInterface to support RMI communication.
 */
public class Server extends UnicastRemoteObject implements RMIClientsInterface{

    private boolean activeAdminConsole = false;
    private int id;
    private RMIGatewayClientsInterface gateway;
    private SimpMessagingTemplate template;
    private String message;

    /**
     * Constructor for the Server class.
     * 
     * @param id the ID of the server
     * @param registryName the name of the RMI registry
     * @param template the SimpMessagingTemplate object
     * @throws RemoteException if an RMI error occurs
     * @throws MalformedURLException if the URL is invalid
     * @throws NotBoundException if the RMI registry is not bound
     */
    public Server(int id, String registryName, SimpMessagingTemplate template)throws RemoteException, MalformedURLException, NotBoundException{

        this.id = id;
        this.template = template;
        gateway = (RMIGatewayClientsInterface) Naming.lookup(registryName);
        System.out.println("Status Client Started, id=" + id);
    }

    /**
     * Updates the admin console with a message. Is called by the gateway.
     * 
     * @param arg0 the message to update the admin console with
     * @throws RemoteException if an RMI error occurs
     */
    @Override
    public void updateAdminConsole(String arg0) throws RemoteException {
        
        if (activeAdminConsole){

            System.out.println("Sending: " + arg0);
            message = arg0;
            message = message.replace("\n", "<br>");
            this.template.convertAndSend("/topic/messages", message);
        }
    }

    /**
     * Subscribes to the admin console.
     * 
     * @return the message to subscribe to the admin console
     * @throws RemoteException if an RMI error occurs
     */
    public String subscribeAdminConsole() throws RemoteException {

        activeAdminConsole = true;
        message = gateway.subscribeAdminConsole(id, this);
        message = message.replace("\n", "<br>");
        System.out.println("Subscribed to Admin Console: ");
        return message;
    }
    
    /**
     * Unsubscribes from the admin console.
     * 
     * @throws RemoteException if an RMI error occurs
     */
    public void unsubscribeAdminConsole() throws RemoteException {

        activeAdminConsole = false;
        gateway.unsubscribeAdminConsole(id);
    }

    /**
     * Returns the message.
     * 
     * @return the message
     */
    public String getMessage(){
        return message;
    }
}
