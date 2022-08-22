import Client.Command;

import java.util.Vector;

public class Operation {
    Command cmd;
    Vector<String> args;

    public Command getCmd() {
        return cmd;
    }

    public void setCmd(Command cmd) {
        this.cmd = cmd;
    }

    public Vector<String> getArgs() {
        return args;
    }

    public void setArgs(Vector<String> args) {
        this.args = args;
    }
}
