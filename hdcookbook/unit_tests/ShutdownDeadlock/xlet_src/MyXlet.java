

import com.hdcookbook.grinxlet.GrinXlet;
import com.hdcookbook.grinxlet.DebugLog;

import com.hdcookbook.grin.Director;
import com.hdcookbook.grin.Show;
import com.hdcookbook.grin.Segment;
import com.hdcookbook.grin.GrinXHelper;
import com.hdcookbook.grin.animator.AnimationClient;
import com.hdcookbook.grin.animator.AnimationContext;
import com.hdcookbook.grin.animator.AnimationEngine;
import com.hdcookbook.grin.animator.DirectDrawEngine;
import com.hdcookbook.grin.commands.Command;
import com.hdcookbook.grin.io.binary.GrinBinaryReader;
import com.hdcookbook.grin.util.AssetFinder;
import com.hdcookbook.grin.util.Debug;


import java.awt.Color;
import java.awt.Rectangle;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import javax.tv.xlet.Xlet;
import javax.tv.xlet.XletContext;
import javax.tv.graphics.TVContainer;

import java.awt.Container;
import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Image;

import org.dvb.ui.DVBBufferedImage;


/**
 * This test xlet overrides most of the "guts" of GrinXlet
 * so that it can create and tear down a show over and over again.
 * cf. README.txt
 **/
public class MyXlet extends GrinXlet {

    private ShutdownDeadlock pilot;

    private MyDirector director;
    private Show show;
    private String showFileName;
    private String showInitialSegment;
    private String showDirectorName;
    private boolean started = false;

    private File resourcesDir;
    private AnimationEngine animationEngine;
    private Container rootContainer;


    public AnimationClient[] getAnimationClients() {
        Debug.assertFail();
        return null;
    }

    public void resetAnimationClients(AnimationClient[] clients) {
        Debug.assertFail();
    }

    public void initXlet(XletContext context) {
        DebugLog.startDebugListener();
        super.initXlet(context);
    }

    protected void doInitXlet(String[] args)  {
        if (Debug.ASSERT && args.length != 5) {
            Debug.assertFail("Parameters:  <grin file> <initial segment> <director> <fontflag> <resources dir>\n    fontflag is -fonts or -nofonts");
        }
        showFileName = args[0];
        showInitialSegment = args[1];
        showDirectorName = args[2];
        // args[3] is about definesFonts, which we ignore
        String root = System.getProperty("bluray.vfs.root") + "/";
        resourcesDir = new File(root + args[4]);
        AssetFinder.setHelper(new AssetFinder() {
            // Set up AssetFinder so we use DVBBufferedImage.
            // See http://wiki.java.net/bin/view/Mobileandembedded/BDJImageMemoryManagement

            protected Image createCompatibleImageBufferHelper(Component c, int width, int height) {
                return new DVBBufferedImage(width, height);
            }

            protected Graphics2D createGraphicsFromImageBufferHelper(Image buffer) {
                Object g = ((DVBBufferedImage) buffer).createGraphics();
                return (Graphics2D) g;
            }

            protected void destroyImageBufferHelper(Image buffer) {
                ((DVBBufferedImage) buffer).dispose();
            }
        });

        AssetFinder.setSearchPath(null, new File[]{resourcesDir});

        rootContainer = getRootContainer();
        pilot = new ShutdownDeadlock(this, 
                                     (Graphics2D) rootContainer.getGraphics());

    }

    public void startXlet() {
        rootContainer.setVisible(true);
        if (started) {
            return;
        }
        started = true;
        pilot.start();
    }

    public void pauseXlet() {
        // We don't implement pausing
    }

    public void destroyXlet(boolean unconditional) {
        rootContainer = null;
        if (Debug.LEVEL > 0) {
            Debug.println("Destroying animation engine...");
        }
        try {
            pilot.stop();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
        if (Debug.LEVEL > 0) {
            Debug.println("destroyXlet() completes successfully.");
            Debug.println();
            Debug.println();
            Debug.println();
            Debug.println();
        }
        xletContext = null;

        DebugLog.shutdownDebugListener();
    }

    public void startShow() {
        director = new MyDirector();
        DirectDrawEngine dde = new DirectDrawEngine();
        dde.setFps(24000);
        animationEngine = dde;
        animationEngine.initialize(this);
        animationEngine.start();
    }

    public long getElapsedTime() throws InterruptedException {
        return director.getElapsedTime();
    }

    public void waitForStart() throws InterruptedException {
        director.waitForStart();
    }


    public void destroyShow() {
        animationEngine.destroy();
    }

    public void animationInitialize() throws InterruptedException {
        try {
            BufferedInputStream bis = new BufferedInputStream(
                            AssetFinder.getURL(showFileName).openStream());
            GrinBinaryReader reader = new GrinBinaryReader(bis);
            show = new Show(director);
            reader.readShow(show);
            bis.close();
                // @@@@  Do this for the real GrinXlet!
        } catch (IOException e) {
            if (Debug.LEVEL > 0) {
                Debug.printStackTrace(e);
            }   
            Debug.println("Error in reading the show file");
            throw new InterruptedException();
        }           

        animationEngine.checkDestroy();
        animationEngine.initClients(new AnimationClient[]{show});
        animationEngine.initContainer(rootContainer,
                    new Rectangle(0, 0, 1080, 1080));
             // Note that the width is only 1080, not 1920.  This gives
             // us space for progress and debug messages in the rightmost
             // 840 pixels of the screen.
    }



    public void animationFinishInitialization() {
        director.initialize();
        show.activateSegment(show.getSegment(showInitialSegment));
    }


}
