package barrels;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;

public interface RMIBarrelsInterface extends Remote {
    public void sayHI() throws RemoteException;
    
}