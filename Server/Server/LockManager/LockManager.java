package Server.LockManager;

import Server.Common.Trace;
import Server.LockManager.TransactionLockObject.LockType;

import java.util.BitSet;
import java.util.Vector;

public class LockManager {
    private static int TABLE_SIZE = 2039;
    private static int DEADLOCK_TIMEOUT = 10000;

    private static final TPHashTable lockTable = new TPHashTable(LockManager.TABLE_SIZE);
    private static final TPHashTable stampTable = new TPHashTable(LockManager.TABLE_SIZE);
    private static final TPHashTable waitTable = new TPHashTable(LockManager.TABLE_SIZE);

    public LockManager() {
        super();
    }

    public boolean Lock(int xid, String data, LockType lockType) throws DeadlockException {
        // if any parameter is invalid, then return false
        if (xid < 0) {
            return false;
        }

        if (data == null) {
            return false;
        }

        if (lockType == LockType.LOCK_UNKNOWN) {
            return false;
        }


        // Two objects in lock table for easy lookup
        TransactionLockObject xLockObject = new TransactionLockObject(xid, data, lockType);
        DataLockObject dataLockObject = new DataLockObject(xid, data, lockType);

        // The remove only compares xid of TransactionObject, no need to create new objects each time
        TransactionLockObject oldXLockObject = new TransactionLockObject();
        DataLockObject oldDataLockObject = new DataLockObject();
        WaitLockObject oldWaitLockObject = new WaitLockObject();
        // Return true when there is no lock conflict or throw a deadlock exception
        try {
            boolean bConflict = true;
            BitSet bConvert = new BitSet(1);
            while (bConflict) {
                Trace.info("LM::lock(" + xid + ", " + data + ", " + lockType + ") called");
                synchronized (lockTable) {
                    // Check if this lock request conflicts with existing locks
                    bConflict = LockConflict(dataLockObject, bConvert);
                    if (!bConflict) {
                        // No lock conflict
                        synchronized (stampTable) {
                            // Remove the timestamp (if any) for this lock request
                            TimeObject timeObject = new TimeObject(xid);
                            stampTable.remove(timeObject);
                        }
                        synchronized (waitTable) {
                            // Remove the entry for this transaction from waitTable (if it
                            // is there) as it has been granted its lock request
                            WaitLockObject waitLockObject = new WaitLockObject(xid, data, lockType);
                            waitTable.remove(waitLockObject);
                        }

                        if (bConvert.get(0)) {
                            //TODO: Lock conversion
                            //there is no redundant lock, there is on conflict, and we are converting the locking type
                            oldXLockObject.set(xid, data, LockType.LOCK_READ);
                            oldDataLockObject.set(xid, data, LockType.LOCK_READ);
                            lockTable.remove(oldXLockObject);
                            lockTable.remove(oldDataLockObject);
//                            waitTable.remove(oldWaitLockObject);
                            lockTable.add(xLockObject);
                            lockTable.add(dataLockObject);
//                            waitTable.add(new WaitLockObject(xid, data, lockType));

                            Trace.info("LM::Lock(" + dataLockObject.getXId() + ", " + dataLockObject.getDataName() + ") converted to " + xLockObject.getLockType());
                        } else {
                            // Lock request that is not lock conversion
                            lockTable.add(xLockObject);
                            lockTable.add(dataLockObject);

                            Trace.info("LM::lock(" + xid + ", " + data + ", " + lockType + ") granted");
                        }
                    }
                }
                if (bConflict) {
                    Trace.info("LM::lock(" + xid + ", " + data + ", " + lockType + ") has conflict");
                    // Lock conflict exists, wait
                    WaitLock(dataLockObject);
                }
            }
        } catch (DeadlockException deadlock) {
            throw deadlock;
        } catch (RedundantLockRequestException redundantlockrequest) {
            // Ignore redundant lock requests
            Trace.info("LM::lock(" + xid + ", " + data + ", " + lockType + ") " + redundantlockrequest.getLocalizedMessage());
            return true;
        }

        return true;
    }


    // Remove all locks for this transaction in the lock table
    public boolean UnlockAll(int xid) {
        // If any parameter is invalid, then return false
        if (xid < 0) {
            return false;
        }

        TransactionLockObject lockQuery = new TransactionLockObject(xid, "", LockType.LOCK_UNKNOWN); // Only used in elements() call below.
        synchronized (lockTable) {
            Vector<TransactionObject> vect = lockTable.elements(lockQuery);

            TransactionLockObject xLockObject;
            Vector<TransactionObject> waitVector;
            WaitLockObject waitLockObject;
            int size = vect.size();

            for (int i = (size - 1); i >= 0; i--) {
                xLockObject = (TransactionLockObject) vect.elementAt(i);
                lockTable.remove(xLockObject);

                Trace.info("LM::unlock(" + xid + ", " + xLockObject.getDataName() + ", " + xLockObject.getLockType() + ") unlocked");

                DataLockObject dataLockObject = new DataLockObject(xLockObject.getXId(), xLockObject.getDataName(), xLockObject.getLockType());
                lockTable.remove(dataLockObject);

                // Check if there are any waiting transactions
                synchronized (waitTable) {
                    // Get all the transactions waiting on this dataLock
                    waitVector = waitTable.elements(dataLockObject);
                    int waitSize = waitVector.size();
                    for (int j = 0; j < waitSize; j++) {
                        waitLockObject = (WaitLockObject) waitVector.elementAt(j);
                        if (waitLockObject.getLockType() == LockType.LOCK_WRITE) {
                            if (j == 0) {
                                // Get all other transactions which have locks on the
                                // data item just unlocked
                                Vector<TransactionObject> vect1 = lockTable.elements(dataLockObject);
                                int vectlSize = vect1.size();

                                boolean free = true;
                                for (int k = 0; k < vectlSize; k++) {
                                    DataLockObject l_dl = (DataLockObject) vect1.elementAt(k);
                                    if (l_dl.getXId() != waitLockObject.getXId()) {
                                        // Some other transaction still has a lock on the data item
                                        // just unlocked. So, WRITE lock cannot be granted
                                        free = false;
                                        break;
                                    }
                                }
                                // Remove interrupted thread from waitTable only if no
                                // other transaction has locked this data item
                                if (!free) {
                                    break;
                                }

                                waitTable.remove(waitLockObject);
                                try {
                                    synchronized (waitLockObject.getThread()) {
                                        waitLockObject.getThread().notify();
                                    }
                                } catch (Exception e) {
                                    System.out.println("Exception on unlock\n" + e.getMessage());
                                }
                            }

                            // Stop granting READ locks as soon as you find a WRITE lock
                            // request in the queue of requests
                            break;
                        } else if (waitLockObject.getLockType() == LockType.LOCK_READ) {
                            // Remove interrupted thread from waitTable
                            waitTable.remove(waitLockObject);

                            try {
                                synchronized (waitLockObject.getThread()) {
                                    waitLockObject.getThread().notify();
                                }
                            } catch (Exception e) {
                                System.out.println("Exception e\n" + e.getMessage());
                            }
                        }
                    }
                }
            }
        }

        return true;
    }


    // Returns true if the lock request on dataObj conflicts with already existing locks. If the lock request is a
    // redundant one (for eg: if a transaction holds a read lock on certain data item and again requests for a read
    // lock), then this is ignored. This is done by throwing RedundantLockRequestException which is handled
    // appropriately by the caller. If the lock request is a conversion from READ lock to WRITE lock, then bitset
    // is set.
    private boolean LockConflict(DataLockObject dataLockObject, BitSet bitset) throws DeadlockException, RedundantLockRequestException {
        Vector<TransactionObject> vect = lockTable.elements(dataLockObject);
        System.out.println("All locks are: " + vect);
        int size = vect.size();

        // As soon as a lock that conflicts with the current lock request is found, return true
        for (int i = 0; i < size; i++) {
            DataLockObject l_dataLockObject = (DataLockObject) vect.elementAt(i);
            if (dataLockObject.getXId() == l_dataLockObject.getXId()) {
                // The transaction already has a lock on this data item which means that it is either
                // relocking it or is converting the lock
                if (dataLockObject.getLockType() == LockType.LOCK_READ) {
                    // Since transaction already has a lock (may be READ, may be WRITE. we don't
                    // care) on this data item and it is requesting a READ lock, this lock request
                    // is redundant.
                    throw new RedundantLockRequestException(dataLockObject.getXId(), "redundant READ lock request");
                } else if (dataLockObject.getLockType() == LockType.LOCK_WRITE) {
                    // Transaction already has a lock and is requesting a WRITE lock
                    // now there are two cases to analyze here
                    // (1) transaction already had a READ lock
                    // (2) transaction already had a WRITE lock
                    // Seeing the comments at the top of this function might be helpful

                    //TODO: Lock conversion
                    //if there is a write lock already
                    if (l_dataLockObject.getLockType() == LockType.LOCK_WRITE) {
                        //redundant write locking request
                        throw new RedundantLockRequestException(dataLockObject.getXId(), "redundant WRITE lock request");
                    }

                    if (l_dataLockObject.getLockType() == LockType.LOCK_READ) {
                        //we now need to convert the locking type
                        bitset.set(0, true);
                    }
                }
            } else if (dataLockObject.getLockType() == LockType.LOCK_READ) {
                if (l_dataLockObject.getLockType() == LockType.LOCK_WRITE) {
                    // Transaction is requesting a READ lock and some other transaction
                    // already has a WRITE lock on it ==> conflict
                    Trace.info("LM::lockConflict(" + dataLockObject.getXId() + ", " + dataLockObject.getDataName() + ") Want READ, someone has WRITE");
                    return true;
                }
            } else if (dataLockObject.getLockType() == LockType.LOCK_WRITE) {
                // Transaction is requesting a WRITE lock and some other transaction has either
                // a READ or a WRITE lock on it ==> conflict
                Trace.info("LM::lockConflict(" + dataLockObject.getXId() + ", " + dataLockObject.getDataName() + ") Want WRITE, someone has READ or WRITE");
                return true;
            }
        }

        // No conflicting lock found, return false
        return false;

    }

    private void WaitLock(DataLockObject dataLockObject) throws DeadlockException {
        Trace.info("LM::waitLock(" + dataLockObject.getXId() + ", " + dataLockObject.getDataName() + ", " + dataLockObject.getLockType() + ") called");

        // Check timestamp or add a new one.
        //
        // Will always add new timestamp for each new lock request since
        // the timeObject is deleted each time the transaction succeeds in
        // getting a lock (see Lock())
        TimeObject timeObject = new TimeObject(dataLockObject.getXId());
        TimeObject timestamp = null;
        long timeBlocked = 0;
        Thread thisThread = Thread.currentThread();
        WaitLockObject waitLockObject = new WaitLockObject(dataLockObject.getXId(), dataLockObject.getDataName(), dataLockObject.getLockType(), thisThread);

        synchronized (stampTable) {
            Vector<TransactionObject> vect = stampTable.elements(timeObject);
            if (vect.size() == 0) {
                // add the time stamp for this lock request to stampTable
                stampTable.add(timeObject);
                timestamp = timeObject;
            } else if (vect.size() == 1) {
                // Lock operation could have timed out; check for deadlock
                TimeObject prevStamp = (TimeObject) vect.firstElement();
                timestamp = prevStamp;
                timeBlocked = timeObject.getTime() - prevStamp.getTime();
                if (timeBlocked >= LockManager.DEADLOCK_TIMEOUT) {
                    // The transaction has been waiting for a period greater than the timeout period
                    cleanupDeadlock(prevStamp, waitLockObject);
                }
            }
            // Shouldn't be more than one time stamp per transaction because the transaction can be blocked
            // on just one lock request
        }

        // Suspend thread and wait until notified
        synchronized (waitTable) {
            if (!waitTable.contains(waitLockObject)) {
                // Register this transaction in the waitTable if it is not already there
                waitTable.add(waitLockObject);
            }
            // Else lock manager already knows the transaction is waiting
        }

        synchronized (thisThread) {
            try {
                thisThread.wait(LockManager.DEADLOCK_TIMEOUT - timeBlocked);
                TimeObject currTime = new TimeObject(dataLockObject.getXId());
                timeBlocked = currTime.getTime() - timestamp.getTime();
                // Check if the transaction has been waiting for a period greater than the timeout period
                if (timeBlocked >= LockManager.DEADLOCK_TIMEOUT) {
                    cleanupDeadlock(timestamp, waitLockObject);
                } else {
                    return;
                }
            } catch (InterruptedException e) {
                System.out.println("Thread interrupted");
            }
        }
    }


    // CleanupDeadlock cleans up stampTable and waitTable, and throws DeadlockException
    private void cleanupDeadlock(TimeObject timeObject, WaitLockObject waitLockObject) throws DeadlockException {
        Trace.info("LM::deadlock(" + waitLockObject.getXId() + ", " + waitLockObject.getDataName() + ", " + waitLockObject.getLockType() + ") called");
        synchronized (stampTable) {
            synchronized (waitTable) {
                stampTable.remove(timeObject);
                waitTable.remove(waitLockObject);
            }
        }
        throw new DeadlockException(waitLockObject.getXId(), "Sleep timeout: deadlocked");
    }
}
