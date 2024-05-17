package com.ds.googol.model;
import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;

import meta1.RMIClientsInterface;
import meta1.RMIGatewayClientsInterface;

/**
 * The WebClient class represents a client in the distributed system.
 * It implements the RMIClientsInterface to support RMI communication.
 */
public class WebClient extends UnicastRemoteObject implements RMIClientsInterface{

    private int id, characterLimit;
    private RMIGatewayClientsInterface gateway;

    /**
     * Constructor for the WebClient class.
     * 
     * @param id the ID of the client
     * @param registryName the name of the RMI registry
     * @param characterLimit the character limit for the search results
     * @throws RemoteException if an RMI error occurs
     * @throws MalformedURLException if the URL is invalid
     * @throws NotBoundException if the RMI registry is not bound
     */
    public WebClient(int id, String registryName, int characterLimit)throws RemoteException, MalformedURLException, NotBoundException{
        this.id = id;
        this.characterLimit = characterLimit;
        
            
        gateway = (RMIGatewayClientsInterface) Naming.lookup(registryName);
        System.out.println("WebClient started: " + id);
    }

    /**
     * Returns the ID of the client.
     * 
     * @return the ID of the client
     */
    public int getId() {
        return id;
    }

    /**
     * Adds a URL to the Queue in the gateway.
     * 
     * @param url the URL to add
     * @return true if the URL was added successfully, false otherwise
     */
    public boolean addURL(String url) {

        try{
            if(gateway.clientAddIndexerUrl(url))return true;

        }catch (Exception e){
            e.printStackTrace();
        }

        return false;
    }

    /**
     * Asks the gateway to search for the terms.
     * 
     * @param terms the terms to search for
     * @param batchCount the batch count
     * @return the search results
     */
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
    
    /**
     * Asks the gateway to consult the URL.
     * 
     * @param urlConsult the URL to consult
     * @param batchCount the batch count
     * @return the URL data
     */
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

    /**
     * Unused in the WebClient class.
     */
    @Override
    public void updateAdminConsole(String arg0) throws RemoteException {
    }
}
