import Client.Command;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.IntStream;

/**
 * Make random operations
 */
public class TransactionFactory {

    Random rand;
    int numClients;

    public static TransactionFactory factory;
    private final List<Command> addCmds = new ArrayList<>(Arrays.asList(Command.AddFlight, Command.AddCars, Command.AddRooms));
    //    private final List<Command> queryCmds = new ArrayList<>(Arrays.asList(Command.QueryFlight, Command.QueryCars, Command.QueryRooms));
    private List<Command> cmds = new ArrayList<>(Arrays.asList(Command.AddFlight, Command.AddCars, Command.AddRooms, Command.QueryFlight, Command.QueryCars, Command.QueryRooms));
    private int factor;

    /**
     * Initialize a Transaction Factory
     * @param numClients number of clients
     * @param factor factor for dataset size (numClients * factor)
     */
    public static void init(int numClients, int factor) {
        if (factory == null) {
            factory = new TransactionFactory(numClients, factor);
            factory.rand = new Random();
        }
    }

    private TransactionFactory(int numClients, int factor) {
        this.numClients = numClients;
        this.factor = factor;
    }

    public Vector<String> generateArgs(int num) {
        Vector<String> result = new Vector<>();
        for (int i = 0; i < num; i++) {
            result.add(Integer.toString(ThreadLocalRandom.current().nextInt(1, 501))); // a large dataset
        }
        return result;
    }

    public Transaction pick() {
        // for add, two arguments
        int numOps = ThreadLocalRandom.current().nextInt(1, 6);
        Transaction result = new Transaction();
        IntStream.range(0, numOps).mapToObj(i -> new Operation()).forEach(op -> {
            Command cmd = cmds.get(rand.nextInt(cmds.size()));
            op.setCmd(cmd);
            if (addCmds.contains(cmd)) {
                op.setArgs(generateArgs(3));
            } else {
                op.setArgs(generateArgs(1));
            }
            result.addOperation(op);
        });
        return result;
    }

}
