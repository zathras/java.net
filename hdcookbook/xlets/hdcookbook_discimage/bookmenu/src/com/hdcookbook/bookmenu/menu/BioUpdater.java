
/*  
 * Copyright (c) 2007, Sun Microsystems, Inc.
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

package com.hdcookbook.bookmenu.menu;

import com.hdcookbook.grin.Show;
import com.hdcookbook.grin.Feature;
import com.hdcookbook.grin.Segment;
import com.hdcookbook.grin.util.Debug;
import com.hdcookbook.grin.util.AssetFinder;

import java.awt.Toolkit;
import java.awt.Image;
import java.awt.MediaTracker;
import java.io.InputStream;
import java.io.BufferedInputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import java.net.URL;

/**
 * This class manages updating the Gun Bunny bio image by
 * reading it from the Internet.  It runs in its own thread.
 *
 *   @author     Bill Foote (http://jovial.com)
 **/
public class BioUpdater implements Runnable {

    private MenuXlet xlet;
    private boolean destroyed = false;
    private boolean running = false;

    private Segment bioAvailableSegment;
    private Segment bioNotAvailableSegment;
    private BioImageFeature bioImageFeature;

    private int currentBioVersion = 0;
    private int availableBioVersion = 0;
    private byte[] bioImageDate = null;
    private boolean checkVersion = false;
    private boolean downloadImage = false;
    private InputStream currentInputStream = null;

    /**
     * Create the BioUpdater
     **/
    public BioUpdater(MenuXlet xlet) {
        this.xlet = xlet;
    }

    /** 
     * Start the bio updater.  This hooks us into the GRIN show, and
     * starts up a thread that checks for a new bio image on the
     * Internet.
     **/
    public void start() {
        bioAvailableSegment = xlet.show.getSegment("S:Bio.Available");
        bioNotAvailableSegment = xlet.show.getSegment("S:Bio.NotAvailable");
        bioImageFeature = (BioImageFeature) xlet.show.getFeature("F:Bio.Bunny");
        if (Debug.ASSERT) {
            if (bioAvailableSegment == null || bioNotAvailableSegment == null
                || bioImageFeature == null) 
            {
                Debug.assertFail();
            }
        }
        checkVersion = true;
        startThread();
    }

    //
    // Start our thread up, or re-start it if it's waiting on a
    // condition variable
    //
    private synchronized void startThread() {
        if (destroyed || running) {
            notifyAll();        // Starts thread if it was waiting
            return;
        }
        running = true;
        Thread t = new Thread(this, "BioUpdater");
        t.setPriority(4);
        t.start();
    }

    /**
     * Destroy this BioUpdater.  This should be called on xlet
     * termination, to make sure the BioUpdater thread is killed,
     * even if it's in the middle of trying to read from the Internet.
     **/
    public synchronized void destroy() {
        if (currentInputStream != null) {
            try {
                currentInputStream.close();  // So that it fails
            } catch (IOException ex) {
            }
        }
        destroyed = true;
        notifyAll();
    }

    /**
     * Make the UI state agree with the our state:  Either a new
     * bio is available for download, or one isn't.
     **/
    public synchronized void activateRightSegment() {
        if (currentBioVersion < availableBioVersion) {
            xlet.show.activateSegment(bioAvailableSegment);
        } else {
            xlet.show.activateSegment(bioNotAvailableSegment);
        }
    }

    /** 
     * Start the process of asynchronously downloading a new biography.
     **/
    public void downloadBio() {
        synchronized(this) {
            downloadImage = true;
            startThread();
        }
    }

    /**
     * This is the bio updating thread man loop.
     **/
    public void run() {
            // This thread should probably terminate and re-start,
            // rather than waiting until the user elects to download.
            // This would be an optimization you'd want to do in a
            // commercial title.
        for (;;) {
            boolean doCheck;
            boolean doDownload;
            synchronized(this) {
                for (;;) {
                    if (destroyed) {
                        running = false;
                        return;
                    }
                    doCheck = checkVersion;
                    doDownload = downloadImage;
                    checkVersion =  false;
                    downloadImage = false;
                    if (doCheck || doDownload) {
                        break;
                    }
                    try {
                        wait();
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                        running = false;
                        return;
                    }
                }
            }
            if (doCheck) {
                readStream(false);
            }
            if (doDownload) {
                readStream(true);
            }
        }
    }

    private InputStream getInputStream(String name)  throws IOException {
        URL u = new URL("http://hdcookbook.com/" + name);
        try {
            return u.openStream();
        } catch (SecurityException ex) {
            //  This means we're not signed.  Rather than just fail to have
            //  the bio feature, we fake it out by reading from the mounted
            //  JAR filesystem.
            u = AssetFinder.getURL("FakeNetwork/" + name);
            try {
                // Simulate a visible network delay of 2 seconds
                Thread.sleep(2000);
            } catch (InterruptedException ex2) {
                Thread.currentThread().interrupt();
                return null;
            }
            return u.openStream();
        }
    }

    private void readStream(boolean forDownload) {
        InputStream is = null;
        String name = (forDownload) ? "disc000001/bio00001_img.png"
                                    : "disc000001/bio00001_ver.txt";
        try {
            is = getInputStream(name);
            synchronized(this) {
                if (destroyed) {
                    is.close();
                    return;
                }
                currentInputStream = is;
            }
            if (forDownload) {
                doDownloadImage(is);
            } else {
                doCheckVersion(is);
            }
        } catch (IOException ex) {
            if (Debug.LEVEL > 0) {
                Debug.printStackTrace(ex);
            }
        } finally {
            synchronized(this) {
                currentInputStream = null;
            }
            if (is != null) {
                try {
                    is.close();
                } catch (Exception ex) {
                }
            }
        }
    }

    private void doCheckVersion(InputStream is) throws IOException {
        BufferedReader rdr = new BufferedReader(new InputStreamReader(is));
        String line = rdr.readLine();
        int version = 0;
        try {
            version = Integer.parseInt(line);
        } catch (NumberFormatException ex) {
            if (Debug.LEVEL > 0) {
                Debug.printStackTrace(ex);
            }
            return;
        }
        boolean changeUI = false;
        synchronized(this) {
            availableBioVersion = version;
            if (availableBioVersion > currentBioVersion) {
                changeUI = true;
            }
        }
        if (changeUI) {
            synchronized(xlet.show) {
                // Always acquire show lock first
                Segment s = xlet.show.getCurrentSegment();
                if (s == bioAvailableSegment || s == bioNotAvailableSegment) {
                    activateRightSegment();
                }
            }
        }
    }

    private void doDownloadImage(InputStream is) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] imageData = null;
        try {
            byte[] buf = new byte[4096];
            for (;;) {
                int num = is.read(buf);
                if (num == -1) {
                    break;
                }
                bos.write(buf, 0, num);
                imageData = bos.toByteArray();
            }
        } catch (IOException ex) {
                // Oh well, the network failed.  A download is still
                // available, so no reason to change the UI.  It would
                // be nice to tell the user that it failed, and we don't
                // do that here.
        } finally {
            try {
                bos.close();    // never fails
                is.close();
            } catch (IOException ex) {
            }
        }
        if (imageData != null) {
            Image newImage = Toolkit.getDefaultToolkit().createImage(imageData);
            MediaTracker tracker = new MediaTracker(xlet.scene);
            tracker.addImage(newImage, 0);
            try {
                tracker.waitForAll();
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
            synchronized(xlet.show) {
                bioImageFeature.setImage(newImage);
                Segment s = xlet.show.getCurrentSegment();
                synchronized(this) {
                    currentBioVersion = availableBioVersion;
                    if (s == bioAvailableSegment || s == bioNotAvailableSegment)
                    {
                        activateRightSegment();
                    }
                    notifyAll();
                }
            }
        }
    }
}
