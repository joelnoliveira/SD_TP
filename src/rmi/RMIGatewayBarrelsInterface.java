package rmi;

import java.rmi.Remote;
import java.rmi.RemoteException;
import barrels.RMIBarrelsInterface;

public interface RMIGatewayBarrelsInterface extends Remote {
    public void subscribe(RMIBarrelsInterface barrel) throws RemoteException;
}