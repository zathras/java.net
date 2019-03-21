

import java.awt.Color;
import java.awt.Font;
import java.awt.Rectangle;
import java.awt.Graphics2D;
import java.awt.Toolkit;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.IOException;
import java.net.URL;

import com.hdcookbook.grin.util.Debug;



public class ShutdownDeadlock implements Runnable {

    private final static int WARMUP = 5;
    private static final Font theFont = new Font("SansSerif", Font.PLAIN, 36);

    private MyXlet xlet;
    private boolean stop = false;
    private boolean stopped = false;
    private Graphics2D graphics;
    private Thread thread;
    

    public ShutdownDeadlock(MyXlet xlet, Graphics2D graphics) {
        this.xlet = xlet;
        this.graphics = graphics;
    }

    public void start() {
        thread = new Thread(this, "ShutdownDeadlock");
        thread.setDaemon(false);
        thread.run();
    }

    public synchronized void stop() throws InterruptedException {
        stop = true;
        notifyAll();
        while (!stopped) {
            wait();
        }
    }

    private boolean checkStop() {
        synchronized(this) {
            if (stop) {
                stopped = true;
                notifyAll();
                showMessage("ShutdownDeadlock thread stopped.");
                return true;
            }
        }
        return false;
    }

    private static String fixedMessage = "Debug log on port 6000";

    private void showMessage(String message) {
        Debug.println(message);
        graphics.setColor(Color.black);
        graphics.fillRect(1080, 200, 840, 840);
        graphics.setColor(Color.green);
        graphics.setFont(theFont);
        graphics.drawString(fixedMessage, 1110, 300);
        graphics.drawString(message, 1100, 500);
        Toolkit.getDefaultToolkit().sync();
    }

    public void run() {
        showMessage("ShutdownDeadlock thread started.");

        try {
            //
            // First we start and destroy the show five times, to get JIT
            // out of the way...
            //
            for (int i = 0; i < WARMUP; i++) {
                if (checkStop()) {
                   return;
                }
                showMessage("JIT warmup " + (i+1) + " of " + WARMUP);
                xlet.startShow();
                xlet.getElapsedTime();
                showMessage("pausing a second...");
                Thread.sleep(1000);
                xlet.destroyShow();
            }

            //
            // Then we get a max time over five iterations, to make sure
            // we give it long enough...
            //
            long max = 0;
            for (int i = 0; i < WARMUP; i++) {
                if (checkStop()) {
                    return;
                }
                showMessage("Time startup " + (i+1) + " of " + WARMUP 
                            + ", max = " + max);
                xlet.startShow();
                long t = xlet.getElapsedTime();
                if (t > max) {
                    max = t;
                }
                xlet.destroyShow();
            }
            if (max == 0) {
                max = 10000;
            }
            fixedMessage = "Debug to 6000, max time is " + max + " ms.";
            Debug.println();
            Debug.println("Max time to load images:  " + max + " ms.");
            Debug.println();
            long limit = (max * 15) / 10;

            int increment = (int) (max / 10);
            if (increment < 1) {
                increment = 1;
            }

            for (int r = 0; ; r++) {
                if (checkStop()) {
                    return;
                }
                for (int i = 0; i < increment; i++) {
                    for (long tm = 0; tm < limit; tm += increment) {
                        if (checkStop()) {
                            return;
                        }
                        xlet.startShow();
                        xlet.waitForStart();

                        Debug.println();
                        showMessage("WILL SLEEP FOR " + tm + " + " + i
                                           + " (repetition " + r + ").");
                        Debug.println();
                        if (tm+i > 0) {
                            synchronized(this) {
                                if (!stop) {
                                    try {
                                        wait(tm+i);
                                    } catch (InterruptedException ex) {
                                        stop = true;
                                    }
                                }
                            }
                        }
                        showMessage("Destroying show...");
                        xlet.destroyShow();
                        showMessage("Show destroyed.");
                    }
                }
            }
        } catch (Exception ex) {
            Debug.printStackTrace(ex);
            showMessage(ex.toString());
            synchronized(this) {
                stopped = true;
            }
        }
    }

}

