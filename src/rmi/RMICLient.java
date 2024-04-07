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

    private Scanner sc = new Scanner(System.in);
    private int id;
    private RMIGatewayClientsInterface gateway;

    public RMIClient() throws Exception{
        
    }

    // ----------------- Remote methods -----------------

    public void notify(String message) throws RemoteException{
        System.out.println("Client " + id + " received: " + message);
    }

    

    // ----------------- Local methods -----------------

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

    private static int checkInt(Scanner sc) {
        while (!sc.hasNextInt()) {    
            System.out.println("You must enter an integer: ");
            sc.next();				  
        }
        int num = sc.nextInt();
        return num;
    }

    private void printPage(List<String> res) {
        String clientInput = "";
        int i = 0;
        for (String r : res) {
            System.out.println(r);
            if (i % 10 == 0 && i != 0) {
                System.out.println("Enter 's' to exit or any other key to continue");
                clientInput = sc.nextLine().toLowerCase();
                if (clientInput.equals("s")) {
                    break;
                }
            }
            i++;
        }
    }

    public void start(RMIGatewayClientsInterface gateway) throws RemoteException, NotBoundException, MalformedURLException, InterruptedException {
        int option;
        do {
            menu();
            option = checkInt(sc);
            sc.nextLine();
            options(gateway, option);
        } while (option != 5);
    }

    public void options(RMIGatewayClientsInterface gateway, int option) throws RemoteException, InterruptedException {
        switch(option) {
            case 1:
                System.out.println("Enter the URL you want to index:");
                // Index the URL
                String url = sc.nextLine();
                gateway.clientAddIndexerUrl(url);
                System.out.println("URL " + url + " indexed successfully");
                System.out.println(" ");
                break;
            case 2:
                System.out.println("Enter the search terms:");
                String keywords = sc.nextLine().toLowerCase();
                List<String> res = gateway.searchByTerms(keywords);
                System.out.println("Search result: ");
                printPage(res);
                break;
            case 3:
                System.out.println("Enter the URL you want to query: ");
                // Query the list of pages linking to a specific page
                String urlConsult = sc.nextLine();
                List<String> res1 = gateway.consult(urlConsult);
                System.out.println("Query result: ");
                for (String r : res1) {
                    System.out.println(r);
                }
                break;
            case 4:
                // Query real-time updated administration page
                System.out.println("Real-time updated administration page");
                List<String> res2 = gateway.topSearches();
                System.out.println("Most searched terms: ");
                for (String r : res2) {
                    System.out.println(r);
                }
                List<Integer> barrels = gateway.getBarrelsList();
                System.out.println("Connected barrels: ");
                for (int b : barrels) {
                    System.out.println("Barrel - " + b);
                }
                System.out.println("Response time: ");
                List<String> res3 = gateway.getMeanResponseTimeByBarrel();
                for (String r : res3) {
                    System.out.println(r);
                }
                break;
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

            client.start(client.gateway);
        
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
