import Client.Command;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Transaction implements Iterable<Operation> {

    List<Operation> operations;
    public Transaction() {
        operations = new ArrayList<>();
    }

    public void addOperation(Operation op) {
        operations.add(op);
    }

    public List<Operation> getOperations() {
        return operations;
    }

    @Override
    public Iterator<Operation> iterator() {
        return operations.iterator();
    }
}
