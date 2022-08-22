package Server.Common;

import Server.Interface.IMiddleware;
import Server.Interface.IResourceServer;
import Server.RMI.InvalidTransactionException;
import Server.RMI.TransactionAbortedException;

import java.rmi.RemoteException;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Timer;
import java.util.Vector;

public class Middleware implements IMiddleware {
    protected String middlewareName;
    protected static TransactionManager tm;
    protected static IResourceServer[] serverRMs = new IResourceServer[3];

    public Middleware(){};

    public Middleware(String p_name) {
        middlewareName = p_name;
        tm = new TransactionManager();
    }


    public IResourceServer getFlight() {
        return serverRMs[0];
    }

    public IResourceServer getCar() {
        return serverRMs[1];
    }

    public IResourceServer getRoom() {
        return serverRMs[2];
    }

    @Override
    public boolean addFlight(int id, int flightNum, int flightSeats, int flightPrice) throws RemoteException, InvalidTransactionException, TransactionAbortedException {
        return tm.addFlight(id, flightNum, flightSeats, flightPrice);
    }

    @Override
    public boolean addCars(int id, String location, int numCars, int price) throws RemoteException, InvalidTransactionException {
        return tm.addCars(id, location, numCars, price);
    }

    @Override
    public boolean addRooms(int id, String location, int numRooms, int price) throws RemoteException, InvalidTransactionException {
        return tm.addRooms(id, location, numRooms, price);
    }

    @Override
    public int newCustomer(int id) throws RemoteException, InvalidTransactionException {
        return tm.newCustomer(id);
    }

    @Override
    public boolean newCustomer(int id, int cid) throws RemoteException, InvalidTransactionException {
        return tm.newCustomer(id, cid);
    }

    @Override
    public boolean deleteFlight(int id, int flightNum) throws RemoteException, InvalidTransactionException {
        return tm.deleteFlight(id, flightNum);
    }

    @Override
    public boolean deleteCars(int id, String location) throws RemoteException, InvalidTransactionException {
        return tm.deleteCars(id, location);
    }

    @Override
    public boolean deleteRooms(int id, String location) throws RemoteException, InvalidTransactionException {
        return tm.deleteRooms(id, location);
    }

    @Override
    public boolean deleteCustomer(int id, int customerID) throws RemoteException, InvalidTransactionException {
        return tm.deleteCustomer(id, customerID);
    }

    @Override
    public int queryFlight(int id, int flightNumber) throws RemoteException, InvalidTransactionException, TransactionAbortedException {
        return tm.queryFlight(id, flightNumber);
    }

    @Override
    public int queryCars(int id, String location) throws RemoteException, InvalidTransactionException {
        return tm.queryCars(id, location);
    }

    @Override
    public int queryRooms(int id, String location) throws RemoteException, InvalidTransactionException {
        return tm.queryRooms(id, location);
    }

    @Override
    public String queryCustomerInfo(int id, int customerID) throws RemoteException, InvalidTransactionException {
        return tm.queryCustomerInfo(id, customerID);
    }

    @Override
    public int queryFlightPrice(int id, int flightNumber) throws RemoteException, InvalidTransactionException {
        return tm.queryFlightPrice(id, flightNumber);
    }

    @Override
    public int queryCarsPrice(int id, String location) throws RemoteException, InvalidTransactionException {
        return tm.queryCarsPrice(id, location);
    }

    @Override
    public int queryRoomsPrice(int id, String location) throws RemoteException, InvalidTransactionException {
        return tm.queryRoomsPrice(id, location);
    }

    @Override
    public boolean reserveFlight(int id, int customerID, int flightNumber) throws RemoteException, InvalidTransactionException {
        return tm.reserveFlight(id, customerID, flightNumber);
    }

    @Override
    public boolean reserveCar(int id, int customerID, String location) throws RemoteException, InvalidTransactionException {
        return tm.reserveCar(id, customerID, location);
    }

    @Override
    public boolean reserveRoom(int id, int customerID, String location) throws RemoteException, InvalidTransactionException {
        return tm.reserveRoom(id, customerID, location);
    }

    @Override
    public boolean bundle(int id, int customerID, Vector<String> flightNumbers, String location, boolean car, boolean room) throws RemoteException, InvalidTransactionException, TransactionAbortedException {
        return tm.bundle(id, customerID, flightNumbers, location, car, room);
    }

    public String getAnalytics() throws RemoteException {
        String res;
        res = getFlight().getFlightsAnalytics(3) + getCar().getCarsAnalytics(3) + getRoom().getRoomsAnalytics(3);
        return res;
    }

    public String getSummary() throws RemoteException, InvalidTransactionException {
        Vector<Integer> allCustomers = getAllCustomers();
        String s = "";
        for (Integer cid : allCustomers) {
            s += (queryCustomerInfo(1, cid) + "\n");
        }
        return s;
    }

    @Override
    public String getName() throws RemoteException {
        return middlewareName;
    }

    @Override
    public Vector<Integer> getAllCustomers() throws RemoteException {
        return getFlight().getAllCustomers();
    }

    @Override
    public int start() throws RemoteException {
        Trace.info("MID::start a transaction");
        int xid = tm.start();
        return xid;
    }

    @Override
    public boolean commit(int transactionId) throws RemoteException, InvalidTransactionException, TransactionAbortedException {
        Trace.info(MessageFormat.format("MID::commit({0}) called", transactionId));
        // commit before release locks
        try {
            return tm.commit(transactionId);
        } catch (TransactionAbortedException abort) {
            Trace.error(MessageFormat.format("MID::commit({0}) failed", transactionId));
            return false;
        }
    }

    @Override
    public void abort(int transactionId) throws RemoteException, InvalidTransactionException {
        Trace.info(MessageFormat.format("MID::abort({0}) called", transactionId));
        // commit before release locks
        tm.abort(transactionId);
    }

    @Override
    public boolean shutdown() throws RemoteException {
        return false;
    }
}
