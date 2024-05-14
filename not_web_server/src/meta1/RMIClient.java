package meta1;

import java.io.FileInputStream;
import java.io.InputStream;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;
import java.util.Properties;
import java.util.Scanner;
import java.net.MalformedURLException;
import java.rmi.NotBoundException;

/**
 * The RMIClient class represents a client that interacts with the RMIServer through RMI (Remote Method Invocation).
 * It implements the RMIClientsInterface and extends the UnicastRemoteObject class.
 * This class provides methods for indexing URLs, searching for terms, querying pages, and viewing real-time updated administration page.
 */
public class RMIClient extends UnicastRemoteObject implements RMIClientsInterface{

    private boolean activeAdminConsole = false;
    private Scanner sc = new Scanner(System.in);
    private int id, characterLimit;
    private RMIGatewayClientsInterface gateway;

    /**
     * Constructs a new RMIClient object.
     * @throws Exception if an error occurs during initialization.
     */
    public RMIClient() throws Exception{
        
    }

    // ----------------- Remote methods -----------------

    public void updateAdminConsole(String message) throws RemoteException {

        // check if the client still has the admin console open
        if (activeAdminConsole){

            System.out.println(message + "Press any key to exit the administraion console");
        }
    }

    

    // ----------------- Local methods -----------------

    /**
     * Displays the menu options for the user.
     */
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

    /**
     * Checks if the input from the scanner is an integer.
     * If the input is not an integer, it prompts the user to enter an integer.
     *
     * @param sc the Scanner object used to read user input
     * @return the integer value entered by the user
     */
    private static int checkInt(Scanner sc) {
        while (!sc.hasNextInt()) {    
            System.out.println("You must enter an integer: ");
            sc.next();				  
        }
        int num = sc.nextInt();
        return num;
    }

    /**
     * Prints the contents of a list of strings, with pagination.
     * 
     * @param res the list of strings to be printed
     */
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

    /**
     * Starts the RMIClient by displaying a menu and processing user input until the user chooses to exit.
     * 
     * @param gateway the RMIGatewayClientsInterface object used for communication with the server
     * @throws RemoteException if a remote error occurs during communication with the server
     * @throws NotBoundException if the specified name is not currently bound
     * @throws MalformedURLException if the URL is not a valid URL
     * @throws InterruptedException if the current thread is interrupted while waiting
     */
    private void start(RMIGatewayClientsInterface gateway) throws RemoteException, NotBoundException, MalformedURLException, InterruptedException {
        int option;
        do {
            menu();
            option = checkInt(sc);
            sc.nextLine();
            options(gateway, option);
        } while (option != 5);
    }

    private void searchByTerms(RMIGatewayClientsInterface gateway) throws RemoteException, InterruptedException{

        int batchNumber = 0;
        String keywords, clientInput;
        List<String> results;

        System.out.println("Enter the search terms:");
        keywords = sc.nextLine().toLowerCase();

        System.out.println("Search result: ");
        results = gateway.searchByTerms(keywords, batchNumber++, characterLimit);

        while (results.size() != 0){
            
            System.out.println("Batch : " + batchNumber);
            
            for (String res : results){
                System.out.println(res);
            }

            System.out.println("Enter 's' to exit or any other key to continue");
            clientInput = sc.nextLine().toLowerCase();
            if (clientInput.equals("s")) return;
            results = gateway.searchByTerms(keywords, batchNumber++, characterLimit);
        }
        System.out.println("---End of results---");   
    }

    private void consultURL(RMIGatewayClientsInterface gateway)throws RemoteException, InterruptedException{

        int batchNumber = 0;
        String keywords, clientInput;
        List<String> results;

        System.out.println("Enter the URL you want to query:");
        keywords = sc.nextLine().toLowerCase();

        System.out.println("Search result: ");
        results = gateway.consultURL(keywords, batchNumber++);

        while (results.size() != 0){
            
            System.out.println("Batch : " + batchNumber);
            
            for (String res : results){
                System.out.println(res);
            }

            System.out.println("Enter 's' to exit or any other key to continue");
            clientInput = sc.nextLine().toLowerCase();
            if (clientInput.equals("s")) return;
            results = gateway.consultURL(keywords, batchNumber++);
        }
        System.out.println("---End of results---");  
    }

    /**
     * Performs different actions based on the provided option.
     *
     * @param gateway The RMIGatewayClientsInterface object used for communication with the server.
     * @param option The option selected by the user.
     * @throws RemoteException If a remote communication error occurs.
     * @throws InterruptedException If the thread is interrupted while waiting.
     */
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

                searchByTerms(gateway);
                break;
            case 3:

                consultURL(gateway);
                
                break;
            case 4:
                // Query real-time updated administration page
                activeAdminConsole = true;
                String message = gateway.subscribeAdminConsole(id, this);
                System.out.println(message);

                System.out.println("Press any key to exit the administraion console");
                sc.nextLine(); // exit after any key is pressed

                // wait for the client to exit the admin console
                activeAdminConsole = false;
                gateway.unsubscribeAdminConsole(id);
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

    /**
        * The main method of the RMIClient class.
        * It is the entry point of the program.
        *
        * @param args The command line arguments passed to the program.
        */
    public static void main(String[] args) {
        
        String registryName;

        try{
            RMIClient client = new RMIClient();
            client.id = Integer.parseInt(args[0]);

            Properties prop = new Properties();
            InputStream input = new FileInputStream(args[1]);
            prop.load(input);

            registryName = prop.getProperty("registryName");
            client.characterLimit = Integer.parseInt(prop.getProperty("characterLimit")); // characterLimit for short quote


            client.gateway = (RMIGatewayClientsInterface) Naming.lookup(registryName);

            client.start(client.gateway);
        
        }catch (Exception e){
            System.out.println("Gateway Unreachable, please try again later");
            // e.printStackTrace();
        }
    }
}