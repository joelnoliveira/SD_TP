package rmi;

import java.net.MalformedURLException;
import java.rmi.AccessException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.List;
import java.util.Scanner;

public class RMICLient {
    private Scanner sc = new Scanner(System.in);
    /**
     * Represents a menu for the user interface.
     * This menu provides options for interacting with the system.
     */
    public void menu() {
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
     * Process user options based on the selected menu option.
     *
     * @param gateway The RMI gateway remote object.
     * @param option The selected option.
     * @throws RemoteException If a remote communication-related exception occurs.
    */
    public void options(RMIGatewayRemote gateway, int option) throws RemoteException {
        switch(option) {
            case 1:
                System.out.println("Enter the URL you want to index:");
                // Index the URL
                String url = sc.nextLine();
                gateway.addUrl(url);
                System.out.println("URL " + url + " indexed successfully");
                System.out.println(" ");
                break;
            case 2:
                System.out.println("Enter the search terms:");
                String keywords = sc.nextLine().toLowerCase();
                List<String> res = gateway.searchByTerms(keywords, gateway.getRandomBarrel());
                System.out.println("Search result: ");
                printPage(res);
                break;
            case 3:
                System.out.println("Enter the URL you want to query: ");
                // Query the list of pages linking to a specific page
                String urlConsult = sc.nextLine();
                List<String> res1 = gateway.consult(urlConsult, gateway.getRandomBarrel());
                System.out.println("Query result: ");
                for (String r : res1) {
                    System.out.println(r);
                }
                break;
            case 4:
                // Query real-time updated administration page
                System.out.println("Real-time updated administration page");
                List<String> res2 = gateway.topSearches(gateway.getRandomBarrel());
                System.out.println("Most searched terms: ");
                for (String r : res2) {
                    System.out.println(r);
                }
                List<String> barrels = gateway.getBarrelsList();
                System.out.println("Connected barrels: ");
                for (String b : barrels) {
                    System.out.println(b);
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

    /**
     * Checks if the input provided by the user is an integer.
     * If not, prompts the user to enter an integer until a valid input is received.
     *
     * @return The integer entered by the user.
     */
    public static int checkInt(Scanner sc) {
        while (!sc.hasNextInt()) {    
            System.out.println("You must enter an integer: ");
            sc.next();				  
        }
        int num = sc.nextInt();
        return num;
    }

    /**
     * Starts the user interface for interacting with the system.
     *
     * @param gateway The RMI gateway remote object.
     * @throws RemoteException If a remote communication-related exception occurs.
     * @throws NotBoundException If no registry is currently running.
     * @throws MalformedURLException If an unknown or invalid URL is encountered.
     */
    public void start(RMIGatewayRemote gateway) throws RemoteException, NotBoundException, MalformedURLException {
        int option;
        do {
            menu();
            option = checkInt(sc);
            sc.nextLine();
            options(gateway, option);
        } while (option != 5);
    }

    /**
     * Prints the contents of a page to the console.
     * Allows the user to navigate through the page by pressing 's' to exit or any other key to continue.
     *
     * @param res The list containing the contents of the page.
     */
    public void printPage(List<String> res) {
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
     * Tenta se reconectar ao registro RMI e realizar novamente as operações necessárias.
     * Se a reconexão for bem-sucedida, conecta aos barris e cria uma instância do cliente RMICLient.
     * Se todas as tentativas de reconexão falharem, exibe uma mensagem de erro e encerra a execução.
     * 
     * @throws RemoteException         se ocorrer um erro de comunicação remota.
     * @throws NotBoundException       se o nome especificado não estiver associado a um objeto remoto no registro RMI.
     * @throws MalformedURLException  se ocorrer um erro ao criar uma URL para o registro RMI.
     */
    public static void retryConnection() throws RemoteException, NotBoundException, MalformedURLException {
        final int MAX_RETRIES = 3;
        int numRetries = 0;
        boolean connected = false;
    
        while (numRetries < MAX_RETRIES && !connected) {
            try {
                // Tentar se reconectar ao registro RMI
                Registry registry = LocateRegistry.getRegistry("localhost");
    
                // Procurar pelo objeto remoto
                RMIGatewayRemote gateway = (RMIGatewayRemote) registry.lookup("Gateway");
                System.out.println("Reconnected to Gateway");

                // Conectar aos barris
                gateway.connect2Barrel();
        
                // Criar uma instância de RMIClient
                RMICLient client = new RMICLient();

                // Iniciar a interface do usuário
                client.start(gateway);
                // Conexão bem-sucedida
                connected = true;
            } catch (RemoteException | NotBoundException e) {
                // Falha ao reconectar, esperar um pouco antes de tentar novamente
                System.err.println("Failed to reconnect. Retrying...");
                try {
                    Thread.sleep(1000); // Esperar 1 segundo antes de tentar novamente
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
                numRetries++;
            }
        }
    
        if (!connected) {
            // Todas as tentativas de reconexão falharam
            System.err.println("Failed to reconnect after " + MAX_RETRIES + " retries. Exiting...");
            System.exit(1);
        }
    }
    
    /**
     * The main method to start the RMI client.
     * Connects to the RMI registry, looks up the remote gateway object,
     * and starts the user interface for interacting with the system.
     *
     * @param args The command-line arguments.
     * @throws AccessException If there is a security or permission violation.
     * @throws RemoteException If a remote communication-related exception occurs.
     * @throws NotBoundException If no registry is currently running.
     * @throws MalformedURLException If an unknown or invalid URL is encountered.
     */
    public static void main(String[] args) throws AccessException, RemoteException, NotBoundException, MalformedURLException {
        // Connect to the RMI registry
        try{
            Registry registry = LocateRegistry.getRegistry("localhost");

            // Look up the remote gateway object
            RMIGatewayRemote gateway = (RMIGatewayRemote) registry.lookup("Gateway");
            System.out.println("Gateway connected");
    
            // Connect to barrels
            gateway.connect2Barrel();
    
            // Create an instance of RMIClient
            RMICLient client = new RMICLient();
    
            // Start the user interface
            client.start(gateway);
        } catch (RemoteException e) {
            retryConnection();
            System.out.println("Error: " + e.getMessage());
        }
    }
}

