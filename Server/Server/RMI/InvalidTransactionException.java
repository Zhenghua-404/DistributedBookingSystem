package Server.RMI;

public class InvalidTransactionException extends Exception {
    private int m_xid = 0;

    public InvalidTransactionException(int xid)
    {
        super("The transaction " + xid + " is invalid. Operation failed.\nPlease check your xid");
        m_xid = xid;
    }
    public InvalidTransactionException(int xid, String msg)
    {
        super("The transaction " + xid + " is invalid: " + msg);
        m_xid = xid;
    }

    int getXId()
    {
        return m_xid;
    }

}
