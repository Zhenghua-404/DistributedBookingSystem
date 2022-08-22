
import java.util.*;
public class Timer{
    long start_time;

    public Timer(){
        super();
    }

    public void startTime(){
        this.start_time = System.currentTimeMillis();    
    }

    public long getTime(){
        return (System.currentTimeMillis() - this.start_time);
    }
}