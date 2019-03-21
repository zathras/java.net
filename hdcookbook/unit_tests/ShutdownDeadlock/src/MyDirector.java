

import com.hdcookbook.grin.Director;
import com.hdcookbook.grin.Segment;
import com.hdcookbook.grin.util.Debug;

public class MyDirector extends Director {

    Segment sInitialize;
    Segment sRun;
    private long startTime = -1;
    private long elapsedTime = -1;


    public void initialize() {
        sInitialize = getSegment("S:Initialize");
        sRun = getSegment("S:Run");
    }

    public synchronized void 
    notifySegmentActivated(Segment newSegment, Segment oldSegment) {
        if (newSegment == sInitialize) {
            startTime = System.currentTimeMillis();
            if (startTime == -1) {
                startTime = 0;
            }
            notifyAll();
        } else if (newSegment == sRun) {
            elapsedTime = System.currentTimeMillis() - startTime;
            if (elapsedTime < 0) {
                elapsedTime = 0;
            }
            notifyAll();
        }
    }

    public synchronized void waitForStart() throws InterruptedException {
        while (startTime == -1) {
            wait();
        }
    }

    public synchronized long getElapsedTime() throws InterruptedException {
        while (elapsedTime == -1) {
            wait();
        }
        return elapsedTime;
    }

}
