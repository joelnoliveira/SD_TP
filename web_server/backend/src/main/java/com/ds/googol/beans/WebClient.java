package com.ds.googol.beans;
import java.rmi.Naming;
import java.rmi.RemoteException;

import meta1.*;

public class WebClient implements RMIClientsInterface{

    private int id, characterLimit;
    private String registryName;
    private RMIGatewayClientsInterface gateway;

    public WebClient(int id, String registryName, int characterLimit) {
        this.id = id;
        this.registryName = registryName;
        this.characterLimit = characterLimit;
        
        try{
            
            gateway = (RMIGatewayClientsInterface) Naming.lookup(registryName);
            System.out.println("WebClient started: " + id);
        
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public int getId() {
        return id;
    }

    @Override
    public void updateAdminConsole(String arg0) throws RemoteException {
        System.out.println("Hello from WebClient");
    }



}
