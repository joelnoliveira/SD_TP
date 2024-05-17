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

public class Server extends UnicastRemoteObject implements RMIClientsInterface{

    private boolean activeAdminConsole = false;
    private int id;
    private RMIGatewayClientsInterface gateway;
    private SimpMessagingTemplate template;
    private String message;

    public Server(int id, String registryName, SimpMessagingTemplate template)throws RemoteException, MalformedURLException, NotBoundException{

        this.id = id;
        this.template = template;
        gateway = (RMIGatewayClientsInterface) Naming.lookup(registryName);
        System.out.println("Status Client Started, id=" + id);
    }

    @Override
    public void updateAdminConsole(String arg0) throws RemoteException {
        
        if (activeAdminConsole){

            System.out.println("Sending: " + arg0);
            message = arg0;
            message = message.replace("\n", "<br>");
            this.template.convertAndSend("/topic/messages", message);
        }
    }

    public String subscribeAdminConsole() throws RemoteException {

        activeAdminConsole = true;
        message = gateway.subscribeAdminConsole(id, this);
        message = message.replace("\n", "<br>");
        System.out.println("Subscribed to Admin Console: ");
        return message;
    }
    
    public void unsubscribeAdminConsole() throws RemoteException {

        activeAdminConsole = false;
        gateway.unsubscribeAdminConsole(id);
    }

    public String getMessage(){
        return message;
    }
}
