// -------------------------------
// adapted from Kevin T. Manley
// CSE 593
// -------------------------------

package Server.Common;

import java.util.logging.Level;
import java.util.logging.Logger;

// A simple wrapper around System.out.println, allows us to disable some
// of the verbose output from RM, TM, and WC if we want
public class Trace {
    private final static Logger logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

    public static void init(Level l) {
        logger.setLevel(l);
    }

    public static void info(String msg) {
        logger.info(getThreadID() + msg);
    }

    public static void warn(String msg) {
        logger.warning(getThreadID() + msg);
    }

    public static void error(String msg) {
        logger.severe(getThreadID() + msg);
    }

    private static String getThreadID() {
        String s = Thread.currentThread().getName();

        // Shorten
        // 	"RMI TCP Connection(x)-hostname/99.99.99.99"
        // to
        // 	"RMI TCP Cx(x)"
        if (s.startsWith("RMI TCP Connection(")) {
            return "RMI Cx" + s.substring(s.indexOf('('), s.indexOf(')')) + ")";
        }
        return s;
    }
}

