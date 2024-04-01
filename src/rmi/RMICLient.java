package rmi;

import java.net.MalformedURLException;
import java.rmi.AccessException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.List;
import java.util.Scanner;

public class RMIClient {
    public void menu() {
        System.out.println("-----Menu-----");
        System.out.println("1- Indexar um novo URL");
        System.out.println("2- Consultar termos de pesquisa");
        System.out.println("3- Consultar lista de páginas com ligação para uma página específica");
        System.out.println("4- Consultar página de administração atualizada em tempo real");
        System.out.println("5- Sair");
        System.out.println("--------------");
        System.out.println("Digite a opção desejada: ");
    }
    public void options(InterfaceGateway gateway,int option,Scanner sc) throws RemoteException{
            switch(option){
                case 1:
                    System.out.println("Digite o URL que deseja indexar:");
                    //indexar o URL
                    String url = sc.nextLine();
                    gateway.addUrl2Queue(url);
                    System.out.println("URL " + url + " indexado com sucesso");
                    break;
                case 2:
                    System.out.println("Digite os termos de pesquisa: (separados por 'espaço','virgula','/', '_' ou '-')");
                    String keywords = sc.nextLine().toLowerCase();
                    List<String> res = gateway.searchByTerms(keywords, gateway.getRandomBarrel());
                    System.out.println("-----Resultado da pesquisa-----");
                    for (String r : res) {
                        System.out.println(r);
                    }
                    System.out.println("-------Fim da pesquisa-------");
                    break;
                case 3:
                    System.out.println("Digite o URL que deseja consultar: ");
                    //consultar lista de páginas com ligação para uma página específica
                    String url_consulta = sc.nextLine();
                    List<String> res1 =gateway.consult(url_consulta, gateway.getRandomBarrel());
                    System.out.println("Resultado da consulta: ");
                    for (String r : res1) {
                        System.out.println(r);
                    }
                    break;
                case 4:
                    System.out.println("Consultar página de administração atualizada em tempo real");
                    gateway.admin();
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
    public void start(InterfaceGateway gateway,Scanner sc) throws RemoteException,NotBoundException, MalformedURLException {
        int option;
        do {
            menu();
            option = checkInt(sc);
            sc.nextLine();
            options(gateway,option,sc);
        } while (option != 5);
    }

    public static void main(String[] args) throws AccessException, RemoteException, NotBoundException, MalformedURLException {
        Registry registry = LocateRegistry.getRegistry("localhost");
        InterfaceGateway gateway = (InterfaceGateway) registry.lookup("Gateway");
        System.out.println("Gateway connected");

        gateway.connect2Barrel();

        RMIClient client = new RMIClient();
        Scanner sc = new Scanner(System.in);
        client.start(gateway,sc);
    }
}
