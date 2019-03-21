

import com.hdcookbook.grin.Director;
import com.hdcookbook.grin.Feature;
import com.hdcookbook.grin.Show;
import com.hdcookbook.grin.animator.SFAAEngine;
import com.hdcookbook.grin.animator.AnimationClient;
import com.hdcookbook.grin.animator.AnimationContext;
import com.hdcookbook.grin.features.InterpolatedModel;
import com.hdcookbook.grin.features.Translator;
import com.hdcookbook.grin.features.Assembly;
import com.hdcookbook.grin.media.PlayerWrangler;
import com.hdcookbook.grin.media.Playlist;
import com.hdcookbook.grin.io.binary.GrinBinaryReader;
import com.hdcookbook.grin.util.AssetFinder;
import com.hdcookbook.grin.util.Debug;
import com.hdcookbook.grinxlet.GrinXlet;

import org.bluray.ui.SyncFrameAccurateAnimation;
import org.bluray.ui.AnimationParameters;
import org.bluray.ui.FrameAccurateAnimationTimer;

import java.awt.Container;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.io.IOException;
import javax.media.Time;

public class SFAADirector extends Director implements AnimationContext {

    private static SFAAEngine engine = null;
    private static Object LOCK = new Object();
    private static final Rectangle sfaaBounds = new Rectangle(0, 0, 960, 540);

    private InterpolatedModel fPosition;
    private InterpolatedModel fScale;
    private Assembly fAssembly;
    private Feature[] fAssemblyPart;
    private Show subShow;

    public SFAADirector() {
    }

    private void initialize() {
        fPosition = (InterpolatedModel) getFeature("F:Position");
        fScale = (InterpolatedModel) getFeature("F:Scale");
        fAssembly = (Assembly) getFeature("F:Assembly");
        fAssemblyPart = new Feature[Data.bearPart.length];
        for (int i = 0; i < fAssemblyPart.length; i++) {
            fAssemblyPart[i] = getPart(fAssembly, Data.bearPart[i]);
        }
    }

    public void heartbeat() {
        long time;
        synchronized(LOCK) {
            if (engine == null) {
                return;
            }
            time = engine.getAnimationFrameTime().getNanoseconds();
        }
        time += MyDirector.trim;
        time -= 1000000000L * 1001 / 1000 / 24 / 2;
                // Subtracts off half a frame
        int frame = (int) (time * 24 * 1000 / 1001 / 1000000000L);
        //
        // Fix up the values of xOffset and yOffset - the values provided didn't
        // accurately register to the video, so we compensate here.
        //
        int xOffset = 28;
        int yOffset = 9;
        if (frame < 0) {
            frame = Data.bearX.length - 1;
        } else if (frame >= Data.bearX.length) {
            frame = Data.bearX.length - 1;
            MyDirector.finishPlayingVideo();
        } else if (frame >= 241) {
            //  leave offset alone
        } else if (frame >= 200) {
            xOffset = 13;
            yOffset = 25;
        } else if (frame >= 169) {
            xOffset = 11;
            yOffset = 25;
        }
        fPosition.setField(Translator.X_FIELD, 
                           Data.bearX[frame] + xOffset + MyDirector.xOffset);
        fPosition.setField(Translator.Y_FIELD, 
                           Data.bearY[frame] + yOffset + MyDirector.yOffset);
        fAssembly.setCurrentFeature(fAssemblyPart[frame]);
        fScale.setField(InterpolatedModel.SCALE_X_FACTOR_FIELD, 
                        Data.bearScaleX[frame] + MyDirector.xScaleOffset);
        fScale.setField(InterpolatedModel.SCALE_Y_FACTOR_FIELD, 
                        Data.bearScaleY[frame] + MyDirector.yScaleOffset);
    }

    public static void startSFAA() {
        if (Debug.LEVEL > 0) {
            Debug.println("Starting SFAA");
        }
        engine = new SFAAEngine(0, true);
        
        AnimationParameters p = new AnimationParameters();
        p.threadPriority = Thread.NORM_PRIORITY - 1;
        p.scaleFactor = 1;
        p.repeatCount = null;
        p.lockedToVideo = false;
        Time start = new Time(0L);
        Time stop = new Time(9000 * Time.ONE_SECOND);
        p.faaTimer = FrameAccurateAnimationTimer.getInstance(start, stop);
        Dimension d = new Dimension(sfaaBounds.width, sfaaBounds.height);
        int numBuffers = MyDirector.numBuffers;
        SyncFrameAccurateAnimation sfaa 
            = SyncFrameAccurateAnimation.getInstance(d, numBuffers, p);
        Debug.println("Created sfaa with " + numBuffers + " buffers.");
        SFAAXlet xlet = (SFAAXlet) GrinXlet.getInstance();
        Container c = xlet.getSFAAContainer();
        c.add(sfaa);
        sfaa.setLocation(sfaaBounds.x, sfaaBounds.y);
        engine.setSFAA(sfaa);

        SFAADirector director = new SFAADirector();
        engine.initialize(director);
    }

    /**
     * Stop the SFAA engine, if one is running.
     *
     * @return  A message giving performance statistics
     **/
    public static String stopSFAA() {
        SFAAEngine e = null;
        synchronized(LOCK) {
            if (engine == null) {
                return "No running engine";
            }
            e = engine;
            engine = null;
        }
        String result = "Skipped frames:  " + e.getSkippedFrames()
                        + " of " + e.getFrameNumber() + " total frames.";
        Debug.println("Stopping the SFAA engine, result:  " + result);
        e.destroy();
        Debug.println("SFAA engine destroyed.");
        return result;
    }

    public void animationInitialize() throws InterruptedException {
        Director director = this;
        try {
            GrinBinaryReader reader = new GrinBinaryReader(AssetFinder.getURL
                                    ("sfaa_show.grin").openStream());
            subShow = new Show(director);
            reader.readShow(subShow);
        } catch (IOException ex) {
            if (Debug.LEVEL > 0) {
                Debug.printStackTrace(ex);
                Debug.println("Error reading sfaa_show.grin");
            }
            throw new InterruptedException();
        }

        engine.checkDestroy();
        engine.initClients(new AnimationClient[] { subShow });
        initialize();
        SFAAXlet xlet = (SFAAXlet) GrinXlet.getInstance();
        Container c = xlet.getSFAAContainer();
        engine.initContainer(c, sfaaBounds);
    }

    public void animationFinishInitialization() {
        subShow.activateSegment(subShow.getSegment("S:Initialize"));
    }

    public static void printTimeOffset() {
        if (Debug.LEVEL > 0) {
            long et = engine.getAnimationFrameTime().getNanoseconds();
            long mt = PlayerWrangler.getInstance().getMediaTime();
            Debug.println("SFAA leads meadia by " + (et - mt) + " ns.");
        }
    }
}
