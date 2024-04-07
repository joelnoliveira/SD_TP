package rmi;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Map;

import barrels.RMIBarrelsInterface;

public interface RMIGatewayBarrelsInterface extends Remote {
    
    public void barrelSubscribe(int id, RMIBarrelsInterface barrel) throws RemoteException;
    public void barrelUnsubscribe(int id) throws RemoteException;
    public void addResponseTimeList(RMIBarrelsInterface barrel, Long a) throws RemoteException;
}