package com.ds.googol.beans;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.util.List;

import meta1.RMIClientsInterface;
import meta1.RMIGatewayClientsInterface;

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

    public boolean addURL(String url) {

        try{
            gateway.clientAddIndexerUrl(url);

        }catch (Exception e){
            e.printStackTrace();
            return false;
        }

        return true;
    }

    public List<String> searchByTerms(String terms) {
        
        try{
            return gateway.searchByTerms(terms, characterLimit);

        }catch (Exception e){// if there is an error null is returned
            e.printStackTrace();
            return null;
        }
    }
    
    public List<String> consult(String urlConsult) {
        
        try{
            return gateway.consult(urlConsult);

        }catch (Exception e){// if there is an error null is returned
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public void updateAdminConsole(String arg0) throws RemoteException {
        System.out.println("Hello from WebClient");
    }



}
