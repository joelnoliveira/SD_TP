package barrels;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface RMIBarrelsInterface extends Remote {
    public void sayHI() throws RemoteException;
}