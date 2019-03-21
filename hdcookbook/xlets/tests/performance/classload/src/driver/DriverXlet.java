/*  
 * Copyright (c) 2008, Sun Microsystems, Inc.
 * 
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 
 *  * Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *  * Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *  * Neither the name of Sun Microsystems nor the names of its contributors
 *    may be used to endorse or promote products derived from this software
 *    without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * 
 *  Note:  In order to comply with the binary form redistribution 
 *         requirement in the above license, the licensee may include 
 *         a URL reference to a copy of the required copyright notice, 
 *         the list of conditions and the disclaimer in a human readable 
 *         file with the binary form of the code that is subject to the
 *         above license.  For example, such file could be put on a 
 *         Blu-ray disc containing the binary form of the code or could 
 *         be put in a JAR file that is broadcast via a digital television 
 *         broadcast medium.  In any event, you must include in any end 
 *         user licenses governing any code that includes the code subject 
 *         to the above license (in source and/or binary form) a disclaimer 
 *         that is at least as protective of Sun as the disclaimers in the 
 *         above license.
 * 
 *         A copy of the required copyright notice, the list of conditions and
 *         the disclaimer will be maintained at 
 *         https://hdcookbook.dev.java.net/misc/license.html .
 *         Thus, licensees may comply with the binary form redistribution
 *         requirement with a text file that contains the following text:
 * 
 *             A copy of the license(s) governing this code is located
 *             at https://hdcookbook.dev.java.net/misc/license.html
 */

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Toolkit;
import java.util.ArrayList;
import java.util.Arrays;
import javax.tv.xlet.Xlet;
import javax.tv.xlet.XletContext;
import javax.tv.xlet.XletStateChangeException;
import org.dvb.application.AppID;
import org.dvb.application.AppProxy;
import org.dvb.application.AppsDatabase;
import org.dvb.application.AppStateChangeEvent;
import org.dvb.application.AppStateChangeEventListener;
import org.havi.ui.HScene;
import org.havi.ui.HSceneFactory;
import net.java.bd.tools.logger.LwText;

public class DriverXlet implements Xlet, Runnable, AppStateChangeEventListener {

    public final static int NUM_TESTS = 9;
    public final static int NUM_ITERATIONS = 99;    // Make it odd, please
    private Thread mainThread = new Thread(this);
        // Set null on destroy; synchronized on this
    private boolean started = false;
    private HScene scene;
    private LwText textC;
    private String[] results = new String[NUM_TESTS * 2 + 3];
    private int nextLine = 0;
    private int[] times = new int[NUM_ITERATIONS];
    private boolean testRunning;
    private Graphics sceneGraphics;

    public void initXlet(XletContext xc) throws XletStateChangeException {
        scene = HSceneFactory.getInstance().getDefaultHScene();
        results[nextLine++] = "   ***  Classloading Performance  ***";
        for (int i = nextLine; i < results.length; i++) {
            results[i] = "";
        }
        textC = new LwText(results, Color.white, Color.black);
        scene.add(textC);
        scene.setBounds(100, 100, 1720, 880);
        textC.setSize(1720, 880);
        scene.setVisible(true);
        scene.validate();
        scene.repaint();
        sceneGraphics = scene.getGraphics();
        sceneGraphics.setFont(new Font("SansSerif", Font.PLAIN, 48));
    }

    public void pauseXlet() {
    }

    public void startXlet() throws XletStateChangeException {
        if (!started) {
            started = true;
            mainThread.start();
        }
    }

    public void destroyXlet(boolean arg0) throws XletStateChangeException {
        try {
            synchronized(this) {
                while (mainThread != null) {
                    mainThread.interrupt();
                    try {
                        wait();
                    } catch (InterruptedException ex) {
                        return;
                    }
                }
            }
        } finally {
            scene.remove(textC);
        }
    }

    public void run() {
        try {
            results[nextLine++] = "Normal GC.  Mean, median, standard deviation, in ms:";
            //
            // First, run through the xlets so that we initialize the
            // xlet launching subsystem.  Not doing this caused a
            // very high standard deviation for the first test only,
            // so this is an important step for good results.
            //
            for (int i = 0; i < NUM_TESTS; i++) {
                runTest(i, -1, false);
            }
            runTests(false);
            results[nextLine++] = "GC time suppressed by running GC before each test:";
            runTests(true);
            textC.repaint();
        } catch (InterruptedException ex) {
            //  discarded
        } finally {
            synchronized(this) {
                mainThread = null;
                notifyAll();
            }
        }
    }

    private void runTests(boolean gcBeforeTest) throws InterruptedException {
        for (int i = 0; i < NUM_TESTS; i++) {
            for (int j = 0; j < NUM_ITERATIONS; j++) {
                runTest(i, j, gcBeforeTest);
                times[j] = runTest(i, j, gcBeforeTest);
            }

            double mean = 0.0;
            for (int j = 0; j < NUM_ITERATIONS; j++) {
                mean += times[j];
            }
            mean /= NUM_ITERATIONS;
            Arrays.sort(times);
            int median = times[NUM_ITERATIONS / 2];  // NUM_ITERATIONS is odd
            double deviation = 0;
            for (int j = 0; j < NUM_ITERATIONS; j++) {
                double d = mean - times[j];
                deviation += d * d;
            }
            deviation = Math.sqrt(deviation / NUM_ITERATIONS);
            results[nextLine++] = "    " + (i+1) + ":  " + mean + "   " + median
                                  + "    " + deviation;
        }
    }
   
    //
    // Returns the number of milliseconds the test takes
    //
    private int runTest(int testNum, int iter, boolean gcBeforeTest) 
                throws InterruptedException
    {
        AppProxy app = getAppProxy(0x7fff0001, 0x4001 + testNum);
        app.addAppStateChangeEventListener(this);
        testRunning = true;
        if (gcBeforeTest) {
            System.gc();
        }
        long startTime = System.currentTimeMillis();
        app.start();
        synchronized(this) {
            while (testRunning) {
                wait();
            }
        }
        int result = (int) (System.currentTimeMillis() - startTime);
        sceneGraphics.setColor(Color.black);
        sceneGraphics.fillRect(300, 200, 400, 100);
        sceneGraphics.setColor(Color.green);
        sceneGraphics.drawString("" + (testNum + 1) + ", " + (iter+1) + ":  " 
                                    + result,
                                 310, 280);
        Toolkit.getDefaultToolkit().sync();
        app.removeAppStateChangeEventListener(this);
        return result;
    }

    //
    // Defined by AppStateChangeEventListener
    //
    public void stateChange(AppStateChangeEvent evt) {
        if (evt.getToState() == AppProxy.DESTROYED) {
            synchronized(this) {
                testRunning = false;
                notifyAll();
            }
        }
    }
    
    private AppProxy getAppProxy(int orgID, int appID) {
        AppsDatabase db = AppsDatabase.getAppsDatabase();
        return db.getAppProxy(new AppID(orgID, appID));
    }    
}

