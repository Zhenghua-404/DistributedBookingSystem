import Server.Common.Trace;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PerfRunner {
    private static int numClients = 1;
    private static int load = 1; // load = num of transactions per second
    private static int iteration = 10; //number of Transactions
    private static String middleHost = "localhost";
    private static String output_file = "output.log";

    private static ExecutorService executor;

    public PerfRunner() {
        // initialize threads for clients
        executor = Executors.newFixedThreadPool(numClients);
    }

    public void start() {
        long period = numClients * 1000 / load; //calculate sleep time using load

        for (int i = 0; i < numClients; i++) {
            executor.submit(new PerfClient(period, iteration, middleHost, "output/"+numClients + "-" + load + ".csv"));
        }
    }

    public static void main(String[] args) {
        // args: num_clients, load, iterations
        if (args.length > 0) {
            numClients = Integer.parseInt(args[0]);
        }

        if (args.length > 1) {
            load = Integer.parseInt(args[1]);
        }

        if (args.length > 2) {
            middleHost = args[2];
        }

        if (args.length > 3) {
            iteration = Integer.parseInt(args[3]);
        }

        if (args.length > 4) {
            output_file = args[4];
        }
        TransactionFactory.init(numClients, iteration/3);
        PerfRunner runner = new PerfRunner();
        Trace.info("PerfRunner::starting");
        runner.start();
        executor.shutdown();
    }
}
