package rmi;

import java.io.FileInputStream;
import java.io.InputStream;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;
import java.util.Properties;
import java.util.Scanner;
import java.util.Scanner;
import java.net.MalformedURLException;
import java.rmi.NotBoundException;


public class RMIClient extends UnicastRemoteObject implements RMIClientsInterface{

    private int id;
    private RMIGatewayClientsInterface gateway;

    public RMIClient() throws Exception{
        
    }

    public void notify(String message) throws RemoteException{
        System.out.println("Client " + id + " received: " + message);
    }

    private void menu() {
        System.out.println("-----Menu-----");
        System.out.println("1- Index a new URL");
        System.out.println("2- Search for terms");
        System.out.println("3- List pages linking to a specific page");
        System.out.println("4- View real-time updated administration page");
        System.out.println("5- Exit");
        System.out.println("--------------");
        System.out.println("Enter the desired option: ");
    }

    private void options(int option){

        try (Scanner sc = new Scanner(System.in)){
            
            switch(option) {

                case 1:

                    System.out.println("Enter the URL you want to index:");
                    // Index the URL
                    String url = sc.nextLine();
                    gateway.clientAddIndexerUrl(url);
                    System.out.println("URL " + url + " indexed successfully");
                    System.out.println(" ");
                    break;

                // case 2:

                //     System.out.println("Enter the search terms:");
                //     String keywords = sc.nextLine().toLowerCase();
                //     List<String> res = gateway.searchByTerms(keywords, gateway.getRandomBarrel());
                //     System.out.println("Search result: ");
                //     printPage(res);
                //     break;

                // case 3:

                //     System.out.println("Enter the URL you want to query: ");
                //     // Query the list of pages linking to a specific page
                //     String urlConsult = sc.nextLine();
                //     List<String> res1 = gateway.consult(urlConsult, gateway.getRandomBarrel());
                //     System.out.println("Query result: ");
                //     for (String r : res1) {
                //         System.out.println(r);
                //     }
                //     break;

                // case 4:

                //     // Query real-time updated administration page
                //     System.out.println("Real-time updated administration page");
                //     List<String> res2 = gateway.topSearches();
                //     System.out.println("Most searched terms: ");
                //     for (String r : res2) {
                //         System.out.println(r);
                //     }
                //     List<String> barrels = gateway.getBarrelsList();
                //     System.out.println("Connected barrels: ");
                //     for (String b : barrels) {
                //         System.out.println(b);
                //     }
                //     System.out.println("Response time: ");
                //     List<String> res3 = gateway.getMeanResponseTimeByBarrel();
                //     for (String r : res3) {
                //         System.out.println(r);
                //     }
                //     break;

                case 5:

                    System.out.println("Exiting");
                    System.out.println("Goodbye!");
                    System.exit(0);
                    // Exit
                    break;

                default:
                    System.out.println("Invalid option");
                    // Invalid option
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        
        String registryName;

        try{
            RMIClient client = new RMIClient();
            client.id = Integer.parseInt(args[0]);

            Properties prop = new Properties();
            InputStream input = new FileInputStream(args[1]);
            prop.load(input);

            registryName = prop.getProperty("registryName");

            client.gateway = (RMIGatewayClientsInterface) Naming.lookup(registryName);

            client.gateway.clientSubscribe(client);

            int option;
            String inputString;
            
            //read from the input and assure that it is an integer
            try (Scanner sc = new Scanner(System.in)){

                client.menu();
                while (sc.hasNextLine()){
                    client.menu();
                    inputString = sc.nextLine();
                    try{
                        option = Integer.parseInt(inputString);
                        switch(option) {

                            case 1:
            
                                System.out.println("Enter the URL you want to index:");
                                // Index the URL
                                inputString = sc.nextLine();

                                client.gateway.clientAddIndexerUrl(inputString);
                                System.out.println("URL " + inputString + " indexed successfully");
                                System.out.println(" ");
                                break;
            
                            // case 2:
            
                            //     System.out.println("Enter the search terms:");
                            //     String keywords = sc.nextLine().toLowerCase();
                            //     List<String> res = gateway.searchByTerms(keywords, gateway.getRandomBarrel());
                            //     System.out.println("Search result: ");
                            //     printPage(res);
                            //     break;
            
                            // case 3:
            
                            //     System.out.println("Enter the URL you want to query: ");
                            //     // Query the list of pages linking to a specific page
                            //     String urlConsult = sc.nextLine();
                            //     List<String> res1 = gateway.consult(urlConsult, gateway.getRandomBarrel());
                            //     System.out.println("Query result: ");
                            //     for (String r : res1) {
                            //         System.out.println(r);
                            //     }
                            //     break;
            
                            // case 4:
            
                            //     // Query real-time updated administration page
                            //     System.out.println("Real-time updated administration page");
                            //     List<String> res2 = gateway.topSearches();
                            //     System.out.println("Most searched terms: ");
                            //     for (String r : res2) {
                            //         System.out.println(r);
                            //     }
                            //     List<String> barrels = gateway.getBarrelsList();
                            //     System.out.println("Connected barrels: ");
                            //     for (String b : barrels) {
                            //         System.out.println(b);
                            //     }
                            //     System.out.println("Response time: ");
                            //     List<String> res3 = gateway.getMeanResponseTimeByBarrel();
                            //     for (String r : res3) {
                            //         System.out.println(r);
                            //     }
                            //     break;
            
                            case 5:
            
                                System.out.println("Exiting");
                                System.out.println("Goodbye!");
                                System.exit(0);
                                // Exit
                                break;
            
                            default:
                                System.out.println("Invalid option");
                                // Invalid option
                        }
                    }catch (NumberFormatException e){
                        System.out.println("Invalid option");
                    }
                }
                
            } catch (Exception e) {
                e.printStackTrace();
            }
        
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
