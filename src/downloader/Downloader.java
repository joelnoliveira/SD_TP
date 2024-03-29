import org.jsoup.Jsoup;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.jsoup.nodes.Element;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.util.InputMismatchException;
import java.util.Properties;
import java.util.StringTokenizer;
import java.rmi.registry.Registry;
import java.time.LocalDateTime;
import java.rmi.registry.LocateRegistry;
import rmi.RMIGatewayRemote;

public class Downloader {
    public static void main(String args[]) {

        int countTokens, linkSearchLimit;

        String host, registryName;
        
        try {

            Properties prop = new Properties();
            InputStream input = new FileInputStream(args[0]);
            prop.load(input);

            
            host = prop.getProperty("host");
            registryName = prop.getProperty("registryName");
            linkSearchLimit = Integer.parseInt(prop.getProperty("linkSearchLimit"));

            System.out.println(LocalDateTime.now() + " : Configurations sucessfully loaded!");
            
        } catch (IOException e) {
            e.printStackTrace();
            return;
        } catch (InputMismatchException e) {
            e.printStackTrace();
            return;
        }

        try {
            // Create the registry
            Registry registry = LocateRegistry.getRegistry(host);
            System.out.println(LocalDateTime.now() + " : Registry found!");

            // Look up the registry for the remote object
            RMIGatewayRemote gateway = (RMIGatewayRemote) registry.lookup(registryName);
            System.out.println(LocalDateTime.now() + " : Gateway object retrieved!");

            try {
                while (linkSearchLimit >= 0) {

                    System.out.println("-------------------------");
                    System.out.println("Remaining links until limit: " + linkSearchLimit);
                    
                    String url = gateway.getNextUrl();

                    // Process the URL...
                    countTokens = 0;
                    Document doc = Jsoup.connect(url).get();
                    StringTokenizer tokens = new StringTokenizer(doc.text());
        
                    while (tokens.hasMoreElements() && countTokens++ < 100){
        
                        System.out.println(tokens.nextToken().toLowerCase());
                    }
                    
                    Elements links = doc.select("a[href]");

                    for (Element link : links){

                        gateway.addUrl(link.attr("abs:href"));
                        System.out.println(link.text() + "\n" + link.attr("abs:href") + "\n");
                    }

                    // TODO send data by RELIABLE MULTICAST to the Index Storage Barrels

                    linkSearchLimit--;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
