package Server.RMI;

import Server.Common.Middleware;
import Server.Common.Trace;
import Server.Interface.IMiddleware;
import Server.Interface.IResourceServer;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;

public class RMIMiddleware extends Middleware {
    private static String middlewareName = "Server";
    private static final String s_rmiPrefix = "group_10_";
    private static String[] serverNames = {"Flights", "Cars", "Rooms"};
    private static String[] serverHosts = {"localhost", "localhost", "localhost"};

    // private staic int[] serverPorts = {30101, 30102, 30103, 30104};
    public RMIMiddleware(String name) {
        super(name);
    }

    @Override
    public boolean shutdown() {
        try {
            for (IResourceServer s : serverRMs) {
                s.shutdown();
            }
            TimerTask task = new TimerTask() {
                public void run() {
                    System.out.println("Exit Middleware");
                    System.exit(0);
                }
            };
            Timer timer = new Timer("MidTimer");

            timer.schedule(task, 2000); // exit server after 1s delay
            return true;
        } catch (RemoteException e) {
            Trace.error(e.getMessage());
            return false;
        }
    }

    public void connectClients() {
        try {
            IMiddleware resourceManager;
            // Dynamically generate the stub (client proxy)
            resourceManager = (IMiddleware) UnicastRemoteObject.exportObject(this, 50000);

            // Bind the remote object's stub in the registry
            Registry l_registry;
            try {
                l_registry = LocateRegistry.createRegistry(3010);
            } catch (RemoteException e) {
                l_registry = LocateRegistry.getRegistry(3010);
            }
            final Registry registry = l_registry;
            registry.rebind(s_rmiPrefix + middlewareName, resourceManager);

            Runtime.getRuntime().addShutdownHook(new Thread() {
                public void run() {
                    try {
                        registry.unbind(s_rmiPrefix + middlewareName);
                        System.out.println("'" + middlewareName + "' resource manager unbound");
                    } catch (Exception e) {
                        System.err
                                .println((char) 27 + "[31;1mServer exception: " + (char) 27 + "[0mUncaught exception");
                        e.printStackTrace();
                    }
                }
            });
            System.out.println("'" + middlewareName + "' resource manager server ready and bound to '" + s_rmiPrefix
                    + middlewareName + "'");
        } catch (Exception e) {
            System.err.println((char) 27 + "[31;1mServer exception: " + (char) 27 + "[0mUncaught exception");
            e.printStackTrace();
            System.exit(1);
        }

        // Create and install a security manager
        if (System.getSecurityManager() == null) {
            System.setSecurityManager(new SecurityManager());
        }
    }

    public void connectServers() {
        int server_num = serverHosts.length;
        String server;
        int port = 30101;
        String name;
        for (int i = 0; i < server_num; i++) {
            server = serverHosts[i];
            name = serverNames[i];
            // port = serverPorts[i]; // !Local test only
            try {
                boolean first = true;
                while (true) {
                    try {

                        Registry registry = LocateRegistry.getRegistry(server, port);
                        serverRMs[i] = (IResourceServer) registry.lookup(s_rmiPrefix + name);
                        System.out.println("Connected to '" + name + "' server [" + server + ":" + port + "/" + s_rmiPrefix + name + "]");
                        break;

                    } catch (NotBoundException | RemoteException e) {
                        if (first) {
                            System.out.println("Waiting for '" + name + "' server [" + server + ":" + port + "/" + s_rmiPrefix + name + "]");
                            first = false;
                        }
                    }
                    Thread.sleep(500);
                }
            } catch (Exception e) {
                System.err.println((char) 27 + "[31;1mServer exception: " + (char) 27 + "[0mUncaught exception");
                e.printStackTrace();
                System.exit(1);
            }
        }
    }

    public static void main(String args[]) {
        if (args.length != 3) {
            System.err.println("Length of args : " + args.length);
            throw new IllegalArgumentException(
                    "Usage : ./run_middleware [[Flight_hostname] [Car_hostname] [Room_hostname]]");
        }

        // Set up hosts
        for (int i = 0; i < args.length; i++) {
            serverHosts[i] = args[i];
        }

        RMIMiddleware rmiMiddleware = new RMIMiddleware(middlewareName);
        rmiMiddleware.connectServers(); // connect to 4 servers and get 4 rms
        // rmiMiddleware.connectServer("localhost", 0, middlewareName, -1);
        rmiMiddleware.connectClients();
    }
}
