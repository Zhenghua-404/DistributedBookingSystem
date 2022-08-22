package Server.Interface;

import Server.RMI.InvalidTransactionException;
import Server.RMI.TransactionAbortedException;

import java.rmi.RemoteException;

public interface IMiddleware extends IResourceManager, ITransactionManager {
    String getSummary() throws RemoteException, InvalidTransactionException;
    String getAnalytics() throws RemoteException;
}
