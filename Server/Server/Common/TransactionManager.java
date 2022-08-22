package Server.Common;

import Server.Interface.IResourceServer;
import Server.LockManager.DeadlockException;
import Server.LockManager.LockManager;
import Server.LockManager.TransactionLockObject.LockType;
import Server.RMI.InvalidTransactionException;
import Server.RMI.TransactionAbortedException;

import java.rmi.RemoteException;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class TransactionManager extends Middleware {
    private static Integer xid = 0;
    // key is xid, value is a set of related ResourceServer
    private ConcurrentHashMap<Integer, Set<IResourceServer>> activeTransacs = new ConcurrentHashMap<>();
    private ConcurrentHashMap<Integer, ScheduledFuture<?>> timers = new ConcurrentHashMap<>();
    static ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1024);
    private static final long delay = 10; // TTL is 10 minutes
    private static LockManager lm;

    public TransactionManager() {
        lm = new LockManager();
    }

    private void addTimer(int id) {

        Runnable task = () -> {
            Trace.info("Timer::Timer for " + id + " is up");
            try {
                abort(id);
            } catch (Exception e) {
                Trace.error("Timer::Abort for " + id + " failed");
                e.printStackTrace();
            }
        };
        ScheduledFuture<?> timer = executor.schedule(task, delay, TimeUnit.MINUTES);
        timers.put(id, timer);
    }

    private void removeTimer(int id) {
        timers.get(id).cancel(true);
        timers.remove(id);
    }

    private void resetTimer(int id) {
        timers.get(id).cancel(true);
        addTimer(id); // overwrite with a new timer
    }

    @Override
    synchronized public int start() throws RemoteException {
        xid += 1;
        // Create a new xid, initialize the set of Ts
        activeTransacs.put(xid, new HashSet<>());
        addTimer(xid);

        return xid;
    }

    @Override
    public boolean commit(int transactionId) throws RemoteException, TransactionAbortedException, InvalidTransactionException {
        // release all locks
        Set<IResourceServer> RMs = activeTransacs.get(transactionId);
        if (RMs == null) {
            throw new InvalidTransactionException(transactionId, MessageFormat.format("xid {0} is not active", transactionId));
        }
        for (IResourceServer rm : RMs) { // tell each server to commit
            rm.commit(transactionId);
        }
        activeTransacs.remove(transactionId);
        removeTimer(transactionId);
        lm.UnlockAll(transactionId);
        return false;
    }

    @Override
    public void abort(int transactionId) throws RemoteException, InvalidTransactionException {
        Set<IResourceServer> RMs = activeTransacs.get(transactionId);
        if (RMs == null) {
            throw new InvalidTransactionException(transactionId, MessageFormat.format("xid {0} is not active", transactionId));
        }
        for (IResourceServer rm : RMs) { // tell each server to abort
            rm.abort(transactionId);
        }
        activeTransacs.remove(transactionId);
        removeTimer(transactionId);
        lm.UnlockAll(transactionId);
    }

    @Override
    public boolean shutdown() {
        return false;
    }


    @Override
    public boolean addFlight(int id, int flightNum, int flightSeats, int flightPrice) throws RemoteException, InvalidTransactionException, TransactionAbortedException {
        try {
            // 1. If the xid is not an active T
            // 2. If lock param check failed
            if (!activeTransacs.containsKey(id) || !lm.Lock(id, Flight.getKey(flightNum), LockType.LOCK_WRITE)) {
                throw new InvalidTransactionException(id);
            }
            resetTimer(id);
            activeTransacs.get(id).add(getFlight()); // add the flight server
            return getFlight().addFlight(id, flightNum, flightSeats, flightPrice);
        } catch (DeadlockException d) {
            abort(id);
            throw new TransactionAbortedException(id);
        }
    }

    @Override
    public boolean addCars(int id, String location, int numCars, int price) throws RemoteException, InvalidTransactionException {
        try {
            // 1. If the xid is not an active T
            // 2. If lock param check failed
            if (!activeTransacs.containsKey(id) || !lm.Lock(id, Car.getKey(location), LockType.LOCK_WRITE)) {
                throw new InvalidTransactionException(id);
            }
            resetTimer(id);
            activeTransacs.get(id).add(getCar());
            return getCar().addCars(id, location, numCars, price);
        } catch (DeadlockException d) {
            abort(id);
            return false;
        }
    }

    @Override
    public boolean addRooms(int id, String location, int numRooms, int price) throws RemoteException, InvalidTransactionException {
        try {
            // 1. If the xid is not an active T
            // 2. If lock param check failed
            if (!activeTransacs.containsKey(id) || !lm.Lock(id, Room.getKey(location), LockType.LOCK_WRITE)) {
                throw new InvalidTransactionException(id);
            }
            resetTimer(id);
            activeTransacs.get(id).add(getRoom());
            return getRoom().addRooms(id, location, numRooms, price);
        } catch (DeadlockException d) {
            abort(id);
            return false;
        }
    }

    @Override
    public int newCustomer(int id) throws RemoteException, InvalidTransactionException {
        try {
            //if there is no such transaction, throw exception
            if (!activeTransacs.containsKey(id)) {
                throw new InvalidTransactionException(id);
            }
            resetTimer(id);
            //ask flight server for a new cid
            int cid = getFlight().newCustomer(id);
            //lock this cid
            if (!lm.Lock(id, Customer.getKey(cid), LockType.LOCK_WRITE)) {
                throw new InvalidTransactionException(id);
            }
            //associate three servers with this xid
            activeTransacs.get(id).add(getRoom());
            activeTransacs.get(id).add(getFlight());
            activeTransacs.get(id).add(getCar());
            //send this cid to the other two server
            getRoom().newCustomer(id, cid);
            getCar().newCustomer(id, cid);
            return cid;
        } catch (Exception e) {
            e.printStackTrace();
            abort(id);
            return -1;
        }
    }

    @Override
    public boolean newCustomer(int id, int cid) throws RemoteException, InvalidTransactionException {
        try {
            //if there is no such transaction, throw execption
            if (!activeTransacs.containsKey(id)) {
                throw new InvalidTransactionException(id);
            }
            resetTimer(id);

            //lock this cid
            if (!lm.Lock(id, Customer.getKey(cid), LockType.LOCK_WRITE)) {
                throw new InvalidTransactionException(id);
            }
            //associate three servers with this xid
            activeTransacs.get(id).add(getRoom());
            activeTransacs.get(id).add(getFlight());
            activeTransacs.get(id).add(getCar());
            //ask three servers to add this customer
            boolean res = getFlight().newCustomer(id, cid) &&
                            getCar().newCustomer(id, cid) &&
                            getRoom().newCustomer(id, cid);
            return res;
        } catch (DeadlockException e) {
            e.printStackTrace();
            abort(id);
            return false;
        }
    }

    @Override
    public boolean deleteCustomer(int id, int customerID) throws RemoteException, InvalidTransactionException {
        try {
            if (!activeTransacs.containsKey(id)) {
                throw new InvalidTransactionException(id);
            }
            resetTimer(id);

            if (!lm.Lock(id, Customer.getKey(customerID), LockType.LOCK_WRITE)) {
                throw new InvalidTransactionException(id);
            }

            activeTransacs.get(id).add(getFlight());
            activeTransacs.get(id).add(getCar());
            activeTransacs.get(id).add(getRoom());

            return getFlight().deleteCustomer(id, customerID) && getCar().deleteCustomer(id, customerID) && getRoom().deleteCustomer(id, customerID);
        } catch (DeadlockException e) {
            e.printStackTrace();
            abort(id);
            return false;
        }
    }

    @Override
    public boolean deleteFlight(int id, int flightNum) throws RemoteException, InvalidTransactionException {
        try {
            if (!activeTransacs.containsKey(id)) {
                throw new InvalidTransactionException(id);
            }

            resetTimer(id);
            if (!lm.Lock(id, Flight.getKey(flightNum), LockType.LOCK_WRITE)) {
                throw new InvalidTransactionException(id);
            }

            activeTransacs.get(id).add(getFlight());

            return getFlight().deleteFlight(id, flightNum);
        } catch (DeadlockException e) {
            e.printStackTrace();
            abort(id);
            return false;
        }
    }

    @Override
    public boolean deleteCars(int id, String location) throws RemoteException, InvalidTransactionException {
        try {
            if (!activeTransacs.containsKey(id)) {
                throw new InvalidTransactionException(id);
            }
            resetTimer(id);

            if (!lm.Lock(id, Car.getKey(location), LockType.LOCK_WRITE)) {
                throw new InvalidTransactionException(id);
            }

            activeTransacs.get(id).add(getCar());

            return getCar().deleteCars(id, location);
        } catch (DeadlockException e) {
            e.printStackTrace();
            abort(id);
            return false;
        }
    }

    @Override
    public boolean deleteRooms(int id, String location) throws RemoteException, InvalidTransactionException {
        try {
            if (!activeTransacs.containsKey(id)) {
                throw new InvalidTransactionException(id);
            }
            resetTimer(id);

            if (!lm.Lock(id, Room.getKey(location), LockType.LOCK_WRITE)) {
                throw new InvalidTransactionException(id);
            }

            activeTransacs.get(id).add(getRoom());

            return getRoom().deleteRooms(id, location);
        } catch (DeadlockException e) {
            e.printStackTrace();
            abort(id);
            return false;
        }
    }

    @Override
    public int queryFlight(int id, int flightNumber) throws RemoteException, InvalidTransactionException, TransactionAbortedException {
        Trace.info("TM::queryFlight(" + xid + ", " + flightNumber + ") called");
        try {
            if (!checkIdAndGetLock(id, Flight.getKey(flightNumber), LockType.LOCK_READ)) {
                throw new InvalidTransactionException(id);
            }
            activeTransacs.get(id).add(getFlight()); // add the flight server
            return getFlight().queryFlight(id, flightNumber);
        } catch (DeadlockException d) {
            abort(id);
            return -1;
        }
    }

    @Override
    public int queryCars(int id, String location) throws RemoteException, InvalidTransactionException {
        try {
            if (!checkIdAndGetLock(id, Car.getKey(location), LockType.LOCK_READ)) {
                throw new InvalidTransactionException(id);
            }
            activeTransacs.get(id).add(getCar()); // add the flight server
            return getCar().queryCars(id, location);
        } catch (DeadlockException d) {
            abort(id);
            return -1;
        }
    }

    @Override
    public int queryRooms(int id, String location) throws RemoteException, InvalidTransactionException {
        try {
            if (!checkIdAndGetLock(id, Room.getKey(location), LockType.LOCK_READ)) {
                throw new InvalidTransactionException(id);
            }
            activeTransacs.get(id).add(getRoom()); // add the flight server
            return getRoom().queryRooms(id, location);
        } catch (DeadlockException d) {
            abort(id);
            return -1;
        }
    }

    @Override
    public String queryCustomerInfo(int id, int customerID) throws RemoteException, InvalidTransactionException {
        try {
            if (!checkIdAndGetLock(id, Customer.getKey(customerID), LockType.LOCK_READ)) {
                throw new InvalidTransactionException(id);
            }
            activeTransacs.get(id).add(getRoom());
            activeTransacs.get(id).add(getFlight());
            activeTransacs.get(id).add(getCar());
            String result = getFlight().queryCustomerInfo(id, customerID) + getCar().queryCustomerInfo(id, customerID)
                    + getRoom().queryCustomerInfo(id, customerID);
            if (result.isBlank()) {
                return "The customer has no reservation";
            }
            return result;
        } catch (DeadlockException e) {
            abort(id);
            return "TM::Failed getting customer info";
        }
    }

    @Override
    public int queryFlightPrice(int id, int flightNumber) throws RemoteException, InvalidTransactionException {
        try {
            if (!checkIdAndGetLock(id, Flight.getKey(flightNumber), LockType.LOCK_READ)) {
                throw new InvalidTransactionException(id);
            }
            activeTransacs.get(id).add(getFlight()); // add the flight server
            return getFlight().queryFlightPrice(id, flightNumber);
        } catch (DeadlockException d) {
            abort(id);
            return -1;
        }
    }

    @Override
    public int queryCarsPrice(int id, String location) throws RemoteException, InvalidTransactionException {
        try {
            if (!checkIdAndGetLock(id, Car.getKey(location), LockType.LOCK_READ)) {
                throw new InvalidTransactionException(id);
            }
            activeTransacs.get(id).add(getCar()); // add the flight server
            return getCar().queryCarsPrice(id, location);
        } catch (DeadlockException d) {
            abort(id);
            return -1;
        }
    }

    @Override
    public int queryRoomsPrice(int id, String location) throws RemoteException, InvalidTransactionException {
        try {
            if (!checkIdAndGetLock(id, Room.getKey(location), LockType.LOCK_READ)) {
                throw new InvalidTransactionException(id);
            }
            activeTransacs.get(id).add(getRoom()); // add the flight server
            return getRoom().queryRoomsPrice(id, location);
        } catch (DeadlockException d) {
            abort(id);
            return -1;
        }
    }

    @Override
    public boolean reserveFlight(int id, int customerID, int flightNumber) throws RemoteException, InvalidTransactionException {
        try {
            if (!checkIdAndGetLock(id, Flight.getKey(flightNumber), LockType.LOCK_WRITE) || !checkIdAndGetLock(id, Customer.getKey(customerID), LockType.LOCK_WRITE)) {
                throw new InvalidTransactionException(id);
            }
            activeTransacs.get(id).add(getFlight());
            return getFlight().reserveFlight(id, customerID, flightNumber);
        } catch (DeadlockException e) {
            abort(id);
            return false;
        }
    }

    @Override
    public boolean reserveCar(int id, int customerID, String location) throws RemoteException, InvalidTransactionException {
        try {
            if (!checkIdAndGetLock(id, Car.getKey(location), LockType.LOCK_WRITE) || !checkIdAndGetLock(id, Customer.getKey(customerID), LockType.LOCK_WRITE)) {
                throw new InvalidTransactionException(id);
            }
            activeTransacs.get(id).add(getFlight());
            return getCar().reserveCar(id, customerID, location);
        } catch (DeadlockException e) {
            abort(id);
            return false;
        }
    }

    @Override
    public boolean reserveRoom(int id, int customerID, String location) throws RemoteException, InvalidTransactionException {
        try {
            if (!checkIdAndGetLock(id, Room.getKey(location), LockType.LOCK_WRITE) || !checkIdAndGetLock(id, Customer.getKey(customerID), LockType.LOCK_WRITE)) {
                throw new InvalidTransactionException(id);
            }
            activeTransacs.get(id).add(getRoom());
            return getRoom().reserveRoom(id, customerID, location);
        } catch (DeadlockException e) {
            abort(id);
            return false;
        }
    }

    @Override
    public boolean bundle(int id, int customerID, Vector<String> flightNumbers, String location, boolean car, boolean room) throws RemoteException, InvalidTransactionException, TransactionAbortedException {
        try {
            //check flight number
            for (String s : flightNumbers) {
                if (!checkIdAndGetLock(id, Flight.getKey(Integer.parseInt(s)), LockType.LOCK_WRITE)) {
                    Trace.error("Error on flights");
                    throw new InvalidTransactionException(id);
                }
            }
            //check car
            if (car && !checkIdAndGetLock(id, Car.getKey(location), LockType.LOCK_WRITE)) {
                Trace.error("Error on cars");

                throw new InvalidTransactionException(id);
            }
            //check room
            if (room && !checkIdAndGetLock(id, Room.getKey(location), LockType.LOCK_WRITE)) {
                Trace.error("Error on rooms");

                throw new InvalidTransactionException(id);
            }

            //booking
            HashMap<Integer, Integer> flightsNeeded = new HashMap<Integer, Integer>();
            for (String flight : flightNumbers) {
                int flightNumber = Integer.parseInt(flight);
                flightsNeeded.put(flightNumber, flightsNeeded.getOrDefault(flightNumber, 0) + 1);
            }
            boolean res = true;
            for (Integer flightNumber : flightsNeeded.keySet()) {
                res &= getFlight().queryFlight(id, flightNumber) >= flightsNeeded.get(flightNumber);
            }
            if (car) {
                res &= getCar().queryCars(id, location) >= 1;
            }
            if (room) {
                res &= getRoom().queryRooms(id, location) >= 1;
            }
            if (res) {
                for (String flightNumber : flightNumbers) {
                    activeTransacs.get(id).add(getFlight());
                    res &= getFlight().reserveFlight(id, customerID, Integer.parseInt(flightNumber));
                }
                if (car) {
                    activeTransacs.get(id).add(getCar());
                    res &= getCar().reserveCar(id, customerID, location);
                }
                if (room) {
                    activeTransacs.get(id).add(getRoom());
                    res &= getRoom().reserveRoom(id, customerID, location);
                }
            }
            return res;
        } catch (DeadlockException e) {
            abort(id);
            return false;
        }
    }

    /**
     * A helper method to get check input xid
     */
    private boolean checkIdAndGetLock(int id, String key, LockType lockType) throws DeadlockException {

        boolean result = activeTransacs.containsKey(id) && lm.Lock(id, key, lockType);
        if (result) resetTimer(id); // valid id, reset timer for client
        return result;
    }
}
