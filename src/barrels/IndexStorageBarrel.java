import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.SocketAddress;
import java.time.LocalDateTime;
import java.util.InputMismatchException;
import java.util.Properties;

public class IndexStorageBarrel{
    
    public void run() {
        System.out.println("IndexStorageBarrel is running");
    }

    public static void main(String[] args) {

        int multicastDownloadersPort, multicastBarrelsPort, multticastTTL;
        String host, registryName, multicastAddress, multicastInterface;
        InetAddress group = null;
        SocketAddress socketAddress0 = null; // Downloaders
        // SocketAddress socketAddress1 = null; // So barrels can communicate with each other
        NetworkInterface networkInterface = null;

        try {

            Properties prop = new Properties();
            InputStream input = new FileInputStream(args[0]);
            prop.load(input);

            
            host = prop.getProperty("host");
            registryName = prop.getProperty("registryName");
            multicastDownloadersPort = Integer.parseInt(prop.getProperty("multicastDownloadersPort"));
            multicastBarrelsPort = Integer.parseInt(prop.getProperty("multicastBarrelsPort"));
            multicastAddress = prop.getProperty("multicastAddress");
            multicastInterface = prop.getProperty("multicastInterface");
            multticastTTL = Integer.parseInt(prop.getProperty("multicastTTL"));

            System.out.println(LocalDateTime.now() + " : Configurations sucessfully loaded!");

            MulticastSocket socket = null;
            try {
                socket = new MulticastSocket(multicastBarrelsPort);
                System.out.println(LocalDateTime.now() + " : Multicast socket created!");

                group = InetAddress.getByName(multicastAddress);
                socketAddress0 = new InetSocketAddress(group, multicastDownloadersPort);
                // socketAddress1 = new InetSocketAddress(group, multicastBarrelsPort); // TODO later
                networkInterface = NetworkInterface.getByName(multicastInterface);
                socket.joinGroup(socketAddress0, networkInterface);
                // socket.joinGroup(socketAddress1, networkInterface);
                socket.setTimeToLive(multticastTTL);
                
                // Receive a message
                byte[]buffer = new byte[1000];
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);
                String received = new String(packet.getData(), 0, packet.getLength());
                System.out.println("Received: " + received + " from " + packet.getAddress() + ":" + packet.getPort());
                
                // Send a message
                String message = "Hello from Barrel";
                buffer = message.getBytes();
                packet = new DatagramPacket(buffer, buffer.length, group, multicastDownloadersPort);
                socket.send(packet);

            } catch (IOException e) {
                System.out.println("Error creating multicast socket: " + e.getMessage());
            } finally {
                if (socket != null) {
                    try {
                        socket.leaveGroup(socketAddress0, networkInterface);
                        // socket.leaveGroup(socketAddress1, networkInterface);
                        socket.close();
                    } catch (IOException e) {
                        System.out.println("Error closing multicast socket: " + e.getMessage());
                    }
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        } catch (InputMismatchException e) {
            e.printStackTrace();
        }
    }
}