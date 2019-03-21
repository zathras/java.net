
import com.hdcookbook.grin.Director;
import com.hdcookbook.grin.animator.AnimationEngine;
import com.hdcookbook.grin.animator.ClockBasedEngine;
import com.hdcookbook.grin.features.Box;
import com.hdcookbook.grin.features.InterpolatedModel;
import com.hdcookbook.grin.features.Translator;
import com.hdcookbook.grin.features.Text;
import com.hdcookbook.grin.util.ManagedImage;
import com.hdcookbook.grin.util.Debug;
import com.hdcookbook.grinxlet.GrinXlet;


import java.awt.Component;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.net.InetAddress;
import java.net.URL;
import java.net.URLConnection;
import java.io.InputStream;
import java.io.DataInputStream;
import java.util.ArrayList;

/**
 **/

public class CreatorDirector extends Director implements Runnable {

    Text resultsFeature;
    Text pfFeature;

    public void startTest() {
        (new Thread(this)).start();
    }

    public void run() {
        pfFeature = (Text) getFeature("F:PassOrFail");
        resultsFeature = (Text) getFeature("F:Results");
        ArrayList results = new ArrayList();
        boolean fail = false;
        try {
            throw new RuntimeException("Java SE has no BUDA");
        } catch (Throwable ex) {
            Debug.println("Got exception:  " + ex);
            Debug.printStackTrace(ex);
            fail = true;
            results.add(ex.toString());
        }
        String[] sa = (String[]) results.toArray(new String[results.size()]);
        if (fail) {
            pfFeature.setText(new String[] { "FAIL" });
        } else {
            pfFeature.setText(new String[] { "PASS" });
        }
        resultsFeature.setText(sa);
        Debug.println("********  results *********");
        Debug.println();
        if (fail) {
            Debug.println("       FAIL");
        } else {
            Debug.println("       PASS");
        }
        Debug.println();
        for (int i = 0; i < sa.length; i++) {
            Debug.println(sa[i]);
        }
        Debug.println();
        Debug.println("**************************");

        getShow().segmentDone();
    }

}
