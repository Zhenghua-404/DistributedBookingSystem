package Server.Interface;

import Server.RMI.InvalidTransactionException;

import java.rmi.RemoteException;

public interface IResourceServer extends IResourceManager {
    public String getFlightsAnalytics(int threshold) throws RemoteException;

    public String getCarsAnalytics(int threshold) throws RemoteException;

    public String getRoomsAnalytics(int threshold) throws RemoteException;

    boolean shutdown() throws RemoteException;
    void abort(int xid) throws RemoteException, InvalidTransactionException;
    void commit(int xid) throws RemoteException, InvalidTransactionException;
}
