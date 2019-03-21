/*  
 * Copyright (c) 2009, Sun Microsystems, Inc.
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

import com.hdcookbook.grinxlet.GrinXlet;
import com.hdcookbook.grin.util.Debug;

import java.awt.Container;
import java.awt.Dimension;
import java.awt.Rectangle;
import javax.tv.graphics.TVContainer;

import org.dvb.event.EventManager;
import org.dvb.event.UserEvent;
import org.dvb.event.UserEventListener;
import org.dvb.event.UserEventRepository;

import org.davic.resources.ResourceClient;
import org.davic.resources.ResourceProxy;

import org.havi.ui.HGraphicsConfiguration;
import org.havi.ui.HGraphicsDevice;
import org.havi.ui.HScreen;


/**
 * An xlet subclass that sets the screen to various resolutions, then
 * adjusts the AssetFinder path to get the corresponding assets.
 **/
public class ScaleXlet extends GrinXlet implements ResourceClient {

    private HGraphicsDevice graphicsDevice;
    private FirstScreen firstScreen = null;
        // Set to null when we're done with it
    private String[] initXletArgs;
        // Set to null when we're done
    private Container rootContainer;

        //
        // The string values need to be the same as the directory
        // names in ../build.xml
        //
    final static String FULL_HD = "FullHD";
    final static String HD_720 = "HD_720";
    final static String QHD = "QHD";
    final static String NTSC_SD = "NTSC_SD";
    private String resolution = null;   // Gets set == to one of the above

    protected void doInitXlet(String[] args) {
        initXletArgs = args;
        // super.doInitXlet is called after the user selects a resolution
    }

    public void startXlet() {
        boolean canStart;
        boolean startFirst;
        synchronized(this) {
            canStart = initXletArgs == null;
            startFirst = !canStart && firstScreen == null;
        }
        if (canStart) {
            super.startXlet();
        }
        if (startFirst) {
            firstScreen = new FirstScreen(this);
            rootContainer = TVContainer.getRootContainer(xletContext);
            rootContainer.setSize(1920, 1080);
            rootContainer.add(firstScreen);
            Rectangle r = new Rectangle();
            r.x = 0;
            r.y = 0;
            r.width = 1920;
            r.height = 1080;
            firstScreen.setBounds(r);
            
            UserEventRepository repo = new UserEventRepository("y");
            repo.addAllNumericKeys();
            EventManager.getInstance().addUserEventListener(firstScreen, repo);
            rootContainer.setVisible(true);
            rootContainer.repaint();
        }
    }

    public void pauseXlet() {
        synchronized(this) {
            if (initXletArgs != null) {
                return;
            }
        }
        super.pauseXlet();
    }

    /**
     * Called when the user selects a screen resolution, this launches
     * the real game xlet.
     **/
    public void launchRealXlet(String resolution, boolean fullHDScreen) {
        synchronized(this) {
            if (firstScreen == null) {
                return;
            }
            if (fullHDScreen) {
                this.resolution = FULL_HD;
            } else {
                this.resolution = resolution;
            }
        }
        EventManager.getInstance().removeUserEventListener(firstScreen);
        rootContainer.remove(firstScreen);
        initXletArgs[4] = initXletArgs[4] + resolution + "/";
        super.doInitXlet(initXletArgs);
        synchronized(this) {
            firstScreen = null;
            initXletArgs = null;
        }
        super.startXlet();
    }


    /**
     * {@inheritDoc}
     * <p>
     * This override of this method sets the HGraphicsDevice to QHD,
     * and sizes the component to QHD.
     **/
    protected Container getRootContainer() {
        graphicsDevice = HScreen.getDefaultHScreen()
                                .getDefaultHGraphicsDevice();
        HGraphicsConfiguration[] config = graphicsDevice.getConfigurations();

        Dimension pixelResolution = null;
        Dimension pixelAspectRatio = null;
        if (resolution == FULL_HD) {
            pixelResolution = new Dimension(1920, 1080);
            pixelAspectRatio = new Dimension(1, 1);
        } else if (resolution == HD_720) {
            pixelResolution = new Dimension(1280, 720);
            pixelAspectRatio = new Dimension(1, 1);
        } else if (resolution == QHD) {
            pixelResolution = new Dimension(960, 540);
            pixelAspectRatio = new Dimension(1, 1);
        } else if (resolution == NTSC_SD) {
            pixelResolution = new Dimension(720, 480);
            pixelAspectRatio = new Dimension(120, 99);
                // Note that this pixel aspect ratio is based on a theoretical
                // displayable area of 704x480, as per the BD spec.  
                // cf. part 3-2 sectoin 13.2 table 13-3.
        } else if (Debug.ASSERT) {
            Debug.assertFail();
        }
        HGraphicsConfiguration found = null;
        for (int i = 0; found == null && i < config.length; i++) {
            Dimension pr = config[i].getPixelResolution();
            Dimension pa = config[i].getPixelAspectRatio();
            if (pixelResolution.equals(pr) && pixelAspectRatio.equals(pa)) {
                found = config[i];
            } 
        }

        if (found == null) {
            if (Debug.LEVEL > 0) {
                Debug.println("*** Couldn't find resolution " + pixelResolution
                              + " at pixel aspect ratio " + pixelAspectRatio);
            }
        } else {
            try {
                graphicsDevice.reserveDevice(this);
                graphicsDevice.setGraphicsConfiguration(found);
                if (Debug.LEVEL > 0) {
                    Debug.println("Set screen size to " + pixelResolution);
                }
            } catch (Exception e) {
                if (Debug.LEVEL > 0) {
                    Debug.printStackTrace(e);
                }
            } finally {
                graphicsDevice.releaseDevice();
            }
        }

        Container root = rootContainer;
        root.setSize(pixelResolution.width, pixelResolution.height);
        if (Debug.LEVEL > 0) {
            Debug.println("Set root container size to " + root.getWidth()
                          + "x" +root.getHeight());
        }
        return root;
    }

    //
    // ResourceClient methods:
    //
    /**
     * {@inheritDoc}
     **/
    public void notifyRelease(ResourceProxy proxy) {
    }

    /**
     * {@inheritDoc}
     **/
    public void release(ResourceProxy proxy) {
    }

    /**
     * {@inheritDoc}
     **/
    public boolean requestRelease(ResourceProxy proxy, Object requestData) {
        return false;   
        // We release the graphicsDevice right after we get it, so we should
        // never get this call, but if we do then we're in the middle of using
        // it.
    }
}
