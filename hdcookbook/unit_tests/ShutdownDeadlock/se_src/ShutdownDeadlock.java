

import java.io.File;
import com.hdcookbook.grin.util.AssetFinder;
import com.hdcookbook.grin.SEShow;
import com.hdcookbook.grin.Segment;
import com.hdcookbook.grin.io.binary.GrinBinaryReader;

import com.hdcookbook.grin.Feature;
import com.hdcookbook.grin.Director;
import com.hdcookbook.grin.animator.AnimationClient;
import com.hdcookbook.grin.animator.AnimationContext;
import com.hdcookbook.grin.animator.AnimationEngine;
import com.hdcookbook.grin.animator.DirectDrawEngine;

import com.hdcookbook.grin.util.AssetFinder;

import java.awt.Color;
import java.awt.Frame;
import java.awt.Rectangle;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.IOException;
import java.net.URL;




public class ShutdownDeadlock extends Frame implements AnimationContext {

    private DirectDrawEngine engine;

    private SEShow show;
    private MyDirector director;

    private final static long RUN_MINUTES = 10L * 60;

    public void startShow() {
        director = new MyDirector();
        engine = new DirectDrawEngine();
        engine.setFps(24000);
        engine.initialize(this);
        engine.start();
    }

    public void waitForStart() throws InterruptedException {
        director.waitForStart();
    }


    public long getElapsedTime() throws InterruptedException {
        return director.getElapsedTime();
    }

    public void destroyShow() {
        engine.destroy();
        engine = null;
    }

    //
    // Method of AnimationContext; called by animatino engine
    //
    public void animationInitialize() {
        show = new SEShow(director);
        show.setIsBinary(true);
        try {
            URL source = AssetFinder.getURL("my_show.grin");
            BufferedInputStream bis 
                = new BufferedInputStream(source.openStream());
            GrinBinaryReader reader = new GrinBinaryReader(bis);
            reader.readShow(show);
            bis.close();
            director.initialize();
        } catch (Exception ex) {
            ex.printStackTrace();
            System.exit(1);
        }
        engine.initClients(new AnimationClient[] { show } );
        engine.initContainer(this, new Rectangle(0,0, getWidth(),getHeight()));
    }

    //
    // Method of AnimationContext; called by animatino engine
    //
    public void animationFinishInitialization() {
        show.activateSegment(director.sInitialize);
    }

    public static void main(String[] args) {
        AssetFinder.setHelper(new AssetFinder() {
            protected void abortHelper() {
                System.exit(1);
            }
        });
        File[] assetDirs = new File[] { 
                new File("src"), new File("build") 
        };
        AssetFinder.setSearchPath(new String[0], assetDirs);

        ShutdownDeadlock frame = new ShutdownDeadlock();
        frame.setBackground(Color.black);
        frame.setSize(800, 800);
        frame.setVisible(true);
        frame.addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent e) {
                System.out.println("Error:  Window closed.");
                System.exit(1);
            }
        });

        try {
            //
            // First we start and destroy the show five times, to get JIT
            // out of the way...
            //
            for (int i = 0; i < 5; i++) {
                frame.startShow();
                frame.getElapsedTime();
                frame.destroyShow();
            }

            //
            // Then we get a max time over five iterations, to make sure
            // we give it long enough...
            //

            long max = 0;
            for (int i = 0; i < 5; i++) {
                frame.startShow();
                long t = frame.getElapsedTime();
                if (t > max) {
                    max = t;
                }
                frame.destroyShow();
            }
            System.out.println();
            System.out.println("Max time to load images:  " + max + " ms.");
            System.out.println();
            long limit = (max * 15) / 10;
            long timeLimit = System.currentTimeMillis() + RUN_MINUTES * 60L*1000;

            for (int r = 0; System.currentTimeMillis() < timeLimit; r++) {
                for (int i = 0; i < 25; i++) {
                    for (long tm = 0; tm < limit; tm += 25) {
                        frame.startShow();
                        frame.waitForStart();

                        long timeLeft = timeLimit - System.currentTimeMillis();
                        System.out.println("(" + (timeLeft / 1000 / 60) 
                                            + " minutes left in run)");
                        System.out.println();
                        System.out.println("WILL SLEEP FOR " + tm + " + " + i
                                           + " (repetition " + r + ").");
                        System.out.println();
                        if (tm+i > 0) {
                            Thread.sleep(tm+i);
                        }

                        frame.destroyShow();
                    }
                }
            }

        } catch (Exception ex) {
            ex.printStackTrace();
            System.exit(1);
        }

        System.out.println("Exiting normally.");
        System.exit(0);
    }

}

