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
    public void options(RMIGatewayRemote gateway,int option,Scanner sc) throws RemoteException{
            switch(option){
                case 1:
                    System.out.println("Digite o URL que deseja indexar:");
                    //indexar o URL
                    String url = sc.nextLine();
                    gateway.addUrl(url);
                    System.out.println("URL " + url + " indexado com sucesso");
                    System.out.println(" ");
                    break;
                case 2:
                    System.out.println("Digite os termos de pesquisa:");
                    String keywords = sc.nextLine().toLowerCase();
                    List<String> res = gateway.searchByTerms(keywords, gateway.getRandomBarrel());
                    for (String r : res) {
                        System.out.println(r);
                    }
                    break;
                case 3:
                    System.out.println("Digite o URL que deseja consultar: ");
                    //consultar lista de páginas com ligação para uma página específica
                    String url_consulta = sc.nextLine();
                    List<String> res1 = gateway.consult(url_consulta, gateway.getRandomBarrel());
                    System.out.println("Resultado da consulta: ");
                    for (String r : res1) {
                        System.out.println(r);
                    }
                    break;
                case 4:
                    //consultar página de administração atualizada em tempo real
                    System.out.println("Página de administração atualizada em tempo real");
                    List <String> res2 = gateway.topSearches(gateway.getRandomBarrel());
                    System.out.println("Termos mais pesquisados: ");
                    for (String r : res2) {
                        System.out.println(r);
                    }
                    List<String> barrels = gateway.getBarrelsList();
                    System.out.println("Barrels conectados: ");
                    for (String b : barrels) {
                        System.out.println(b);
                    }
                    System.out.println("Tempo de resposta: " + gateway.calcularTempoResposta(gateway.getRandomBarrel()) + "ms");
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
    public void start(RMIGatewayRemote gateway,Scanner sc) throws RemoteException,NotBoundException, MalformedURLException {
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
        RMIGatewayRemote gateway = (RMIGatewayRemote) registry.lookup("Gateway");
        System.out.println("Gateway connected");

        gateway.connect2Barrel();

        RMICLient client = new RMICLient();
        Scanner sc = new Scanner(System.in);
        client.start(gateway,sc);
    }
}
