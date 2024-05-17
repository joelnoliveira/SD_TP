package com.ds.googol.model;
import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;

import meta1.RMIClientsInterface;
import meta1.RMIGatewayClientsInterface;

public class WebClient extends UnicastRemoteObject implements RMIClientsInterface{

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
            if(gateway.clientAddIndexerUrl(url))return true;

        }catch (Exception e){
            e.printStackTrace();
        }

        return false;
    }

    public List<String> searchByTerms(String terms, int batchCount) {
        
        try{
            List<String> result = gateway.searchByTerms(terms, batchCount, characterLimit);

            // adapt the result to be displayed in the web page
            for(int i = 0; i < result.size(); i++){
			
                String string = result.get(i);
                // tokenize the string
    
                String[] tokens = string.split("\n");
                StringBuilder sb = new StringBuilder();
                
                sb.append(tokens[0]+"<br>");// add a line break after the term
                // add a clickable link to the URL if it exists
                if(tokens.length > 1)sb.append("<a href=\""+tokens[1]+"\">"+ tokens[1] +"</a><br>");
    
                for(int j = 2; j < tokens.length; j++){
                    sb.append(tokens[j]+"<br>");
                }
    
                result.set(i, sb.toString());
            }

            return result;

        }catch (Exception e){// if there is an error null is returned
            e.printStackTrace();
            return null;
        }
    }
    
    public List<String> consultURL(String urlConsult, int batchCount) {
        
        try{

            List<String> result = gateway.consultURL(urlConsult, batchCount);

            for(int i = 0; i < result.size(); i++){

                String string = result.get(i);
                // add a clickable link to the URL if it exists
                result.set(i, "<a href=\""+ string.replace("\n", "") +"\">"+ string +"</a><br>");

            }
            
            return result;

        }catch (Exception e){// if there is an error null is returned
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public void updateAdminConsole(String arg0) throws RemoteException {
    }
}
