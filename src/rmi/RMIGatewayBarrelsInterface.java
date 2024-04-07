package rmi;

import java.rmi.Remote;
import java.rmi.RemoteException;
import barrels.RMIBarrelsInterface;

public interface RMIGatewayBarrelsInterface extends Remote {
    public void barrelSubscribe(RMIBarrelsInterface barrel) throws RemoteException;
    public void barrelUnsubscribe(RMIBarrelsInterface barrel) throws RemoteException;
}