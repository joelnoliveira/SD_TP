package rmi;

import java.net.MalformedURLException;
import java.rmi.AccessException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Scanner;

public class RMIClient {
    private InterfaceGateway service;
    public void menu() {
        System.out.println("1- Indexar um novo URL");
        System.out.println("2- Consultar termos de pesquisa");
        System.out.println("3- Consultar lista de páginas com ligação para uma página específica");
        System.out.println("4- Consultar página de administração atualizada em tempo real");
        System.out.println("5- Sair");
        System.out.println("Digite a opção desejada: ");
    }
    public void options(int option,Scanner sc) throws RemoteException{
            switch(option){
                case 1:
                    System.out.println("Digite o URL que deseja indexar:");
                    //indexar o URL
                    String url = sc.nextLine();
                    this.service.indexURL(url);
                    break;
                case 2:
                    System.out.println("Digite os termos de pesquisa: (separados por 'espaço','virgula','/', '_' ou '-')");
                    String keywords = sc.nextLine().toLowerCase();
                    this.service.searchByTerms(keywords);
                    break;
                case 3:
                    System.out.println("Digite o URL que deseja consultar: ");
                    //consultar lista de páginas com ligação para uma página específica
                    String url_consulta = sc.nextLine();
                    this.service.consult(url_consulta);
                    break;
                case 4:
                    System.out.println("Consultar página de administração atualizada em tempo real");
                    this.service.admin();
                    break;
                case 5:
                    System.out.println("Sair");
                    //sair
                    break;
                default:
                    System.out.println("Opção inválida");
                    //opção inválida
            }
    }
    public static int checkInt(Scanner sc) {
        while (!sc.hasNextInt()) {    
            System.out.println("Deve inserir um numero inteiro: ");
            sc.next();				  
        }
        int num = sc.nextInt();
        return num;
    }
    public void start(Scanner sc) throws RemoteException,NotBoundException, MalformedURLException {
        int option;
        do {
            menu();
            option = checkInt(sc);
            sc.nextLine();
            options(option,sc);
        } while (option != 5);
    }
    public static void main(String[] args) throws AccessException, RemoteException, NotBoundException, MalformedURLException {
            Registry registry = LocateRegistry.getRegistry("localhost", 1099);
            InterfaceGateway service = (InterfaceGateway) registry.lookup("service");
            RMIClient cliente = new RMIClient();
            Scanner sc = new Scanner(System.in);
            cliente.service = service;
            cliente.start(sc);
    }
}
