import Client.Command;
import Client.RMIClient;
import Server.Common.Trace;

import java.text.MessageFormat;
import java.util.Vector;
import java.util.concurrent.*;

/**
 * The Client to submit requests in a loop
 */

public class PerfClient implements Runnable {
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(10);

    /**
     * @param period period to submit a transaction based on load
     * @param iteration num of total transactions
     */
    private long period;
    private int iteration;
    private RMIClient client;
    ScheduledFuture<?> t;
    private CountDownLatch latch;
    private String outputfile;

    private Runnable makeTransaction = () -> {
        Vector<String> args = new Vector<>();
        try {
            int xid;
            Timer t_time = new Timer();
            Logs log = new Logs(outputfile);
            t_time.startTime();
            xid = client.getXid();
            Transaction t = TransactionFactory.factory.pick();
            for (Operation op : t) {
                args.clear();
                args.add("");
                args.add(Integer.toString(xid));
                args.addAll(op.args);
                Trace.info(MessageFormat.format("PerfClient::executing iteration[{0}] {1} {2}", iteration, op.cmd.name(), op.args));
                client.execute(op.cmd, args);
                //Thread.sleep(500);
            }
            args.clear();
            args.add("");
            args.add(Integer.toString(xid));
            client.execute(Command.Commit, args);
            log.write(xid+","+t_time.getTime());
            log.close();
            latch.countDown();
            iteration--;

        } catch (Exception e) {
            e.printStackTrace();
            Trace.error(e.getMessage());
        }
    };

    public PerfClient(long period, int iteration, String middleHost, String output_file) {
        this.period = period;
        this.iteration = iteration;
        this.outputfile = output_file;
        this.client = new RMIClient(middleHost);
        this.latch = new CountDownLatch(iteration);
        Trace.info("PerfClient::Connecting to server");
        this.client.connectServer();

    }

    @Override
    public void run() {
        t = scheduler.scheduleAtFixedRate(makeTransaction, 0, period, TimeUnit.MILLISECONDS);
        try {
            latch.await();
            scheduler.shutdownNow();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}
