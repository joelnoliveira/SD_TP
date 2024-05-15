package com.ds.googol.beans;
import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.List;

import meta1.RMIClientsInterface;
import meta1.RMIGatewayClientsInterface;

public class WebClient implements RMIClientsInterface{

    private int id, characterLimit;
    private RMIGatewayClientsInterface gateway;

    public WebClient(int id, String registryName, int characterLimit)throws RemoteException, MalformedURLException, NotBoundException{
        this.id = id;
        this.characterLimit = characterLimit;
        
            
        gateway = (RMIGatewayClientsInterface) Naming.lookup(registryName);
        System.out.println("WebClient started: " + id);
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

    public List<String> searchByTerms(String terms, int batchCount) {
        
        try{
            return gateway.searchByTerms(terms, batchCount, characterLimit);

        }catch (Exception e){// if there is an error null is returned
            e.printStackTrace();
            return null;
        }
    }
    
    public List<String> consultURL(String urlConsult, int batchCount) {
        
        try{
            return gateway.consultURL(urlConsult, batchCount);

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
