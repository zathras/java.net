/*  
 * Copyright (c) 2011, Oracle
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
package com.hdcookbook.grinxlet;

import java.awt.Color;
import java.awt.Rectangle;
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
import java.awt.Point;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

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

import org.dvb.event.EventManager;
import org.dvb.event.UserEvent;
import org.dvb.event.UserEventListener;
import org.dvb.event.UserEventRepository;
import org.dvb.ui.DVBBufferedImage;
import org.dvb.ui.FontFactory;

import org.havi.ui.event.HRcCapabilities;
//import org.bluray.ui.event.HRcEvent;
import org.havi.ui.event.HRcEvent;

/** 
 * The xlet class for a grin-centric xlet project.  This is the debug version
 * of the xlet, with debug support turned on.
 * <p>
 * WARNING:  There are actually three copies of this class:  The debug
 * version, the deploy version, and the GrinView version.  The GrinView
 * version can be found under AuthoringTools/grin/jdktools/grinviewer.
 * It offers a subset of the public method of GrinXlet, e.g. to get
 * and set the list of animation clients.  This allows a GrinView
 * simulation of a wider range of functionalities on a desktop.
 * <p>
 * If you add features to the xlet versions of this class (both debug
 * and deploy), consider adding them to the GrinView version, too.
 **/

public class GrinXlet
        implements Xlet, AnimationContext, UserEventListener,
        MouseListener, MouseMotionListener {

    /**
     * The XletContext of our game xlet.  This is exposed as a public static
     * variable so that client code can access the XletContext.  It's set
     * as soon as we discover our context, and nulled when the xlet is
     * destroyed.
     * <p>
     * This is not available in the GrinView version of GrinXlet.
     **/
    public static XletContext xletContext;
    private static GrinXlet theInstance;
    public Show show;
    Container rootContainer;
    FontFactory fontFactory = null;
    AnimationEngine animationEngine;
    DebugDirectDrawEngine debugEngine = null;  // might stay null
    Director director;
    private String showFileName;
    private String showInitialSegment;
    private String showDirectorName;
    private boolean definesFonts;
    private File resourcesDir;
    boolean sendKeyUp = true;
    private int redKeyCode;
    private int greenKeyCode;
    private int blueKeyCode;
    private int yellowKeyCode;
    private int xletWidth;
    private int xletHeight;
    private Show[] keyInterestOrder;

    // A small show we use to manage a debug screen accessed with
    // the remote 0 key
    private Show xletShow;
    private XletDirector xletDirector;

    public GrinXlet() {
        theInstance = this;
    }

    /**
     * Get the instance of this singleton
     **/
    public static GrinXlet getInstance() {
        return theInstance;
    }

    /**
     * Get the list of animation clients
     **/
    public AnimationClient[] getAnimationClients() {
        AnimationClient[] clients = animationEngine.getAnimationClients();
        // We need to suppress xletShow from the list
        AnimationClient[] result = new AnimationClient[clients.length - 1];
        for (int i = 0; i < result.length; i++) {
            result[i] = clients[i];
        }
        return result;
    }

    /**
     * Reset the list of animation clients
     **/
    public void resetAnimationClients(AnimationClient[] clients) {
        AnimationClient[] real = new AnimationClient[clients.length + 1];
        for (int i = 0; i < clients.length; i++) {
            real[i] = clients[i];
        }
        real[clients.length] = xletShow;
        animationEngine.resetAnimationClients(real);
    }

    /**
     * Get the animation engine
     **/
    public AnimationEngine getAnimationEngine() {
        return animationEngine;
    }

    public void initXlet(XletContext context) {
        this.xletContext = context;

        String[] args =
                (String[]) xletContext.getXletProperty(xletContext.ARGS);
        if (args == null || args.length == 0) {
            args = (String[]) xletContext.getXletProperty("dvb.caller.parameters");
        }
        doInitXlet(args);
    }

    /**
     * Return the root container for this xlet, with the size set to
     * the desired size.  The default implementation of this method
     * returns a root container acquired from 
     * javax.tv.graphics.TVContainer.getRootContainer() (which in GEM
     * systems is an HScene).  It is sized to 1920x1080 by the default
     * implementation of this method.
     * <p>
     * Subclasses may override this method to use a different container
     * as the root container, or to set a different size.  The implementation
     * of the subclass method might also change the resolution of the
     * player, e.g. from full HD (1920x1080) to QHD (960x540).
     **/
    protected Container getRootContainer() {
        Container c = TVContainer.getRootContainer(xletContext);
        c.setSize(1920, 1080);
        return c;
    }

    /**
     * This method can be overridden by subclasses that want to look
     * for xlet arguments in places other than the default
     * XletContext.ARGS and "dvb.caller.parameters".  Subclasses that
     * override this should call super.doInitXlet(args) once they've
     * colleded their arguments.
     **/
    protected void doInitXlet(String[] args) {
        if (Debug.ASSERT && args.length != 5) {
            Debug.assertFail("Parameters:  <grin file> <initial segment> <director> <fontflag> <resources dir>\n    fontflag is -fonts or -nofonts");
        }
        showFileName = args[0];
        showInitialSegment = args[1];
        showDirectorName = args[2];
        definesFonts = "-fonts".equals(args[3]);
        String root = System.getProperty("bluray.vfs.root") + "/";
        resourcesDir = new File(root + args[4]);

        rootContainer = getRootContainer();
        xletWidth = rootContainer.getWidth();
        xletHeight = rootContainer.getHeight();

        animationEngine = createAnimationEngine();
        if (animationEngine instanceof DebugDirectDrawEngine) {
            debugEngine = (DebugDirectDrawEngine) animationEngine;
        }
        animationEngine.initialize(this);
    }

    /**
     * Create the animation engine for this xlet, set up for the desired
     * framerate.
     **/
    protected AnimationEngine createAnimationEngine() {
        DirectDrawEngine result = new DebugDirectDrawEngine();
        result.setFps(24000);
        return result;
    }

    public void startXlet() {
        rootContainer.setVisible(true);
        animationEngine.start();
    }

    public void pauseXlet() {
        rootContainer.setVisible(false);
        animationEngine.pause();
    }

    public void destroyXlet(boolean unconditional) {
        rootContainer = null;
        if (Debug.LEVEL > 0) {
            Debug.println("Destroying animation engine...");
        }
        animationEngine.destroy();
        EventManager.getInstance().removeUserEventListener(this);
        if (Debug.LEVEL > 0) {
            Debug.println("destroyXlet() completes successfully.");
            Debug.println();
            Debug.println();
            Debug.println();
            Debug.println();
        }
        xletContext = null;
        theInstance = null;
    }

    public void animationInitialize() throws InterruptedException {

        try {
            director = (Director) Class.forName(showDirectorName).newInstance();
        } catch (Throwable t) {
            Debug.assertFail("Can't create director:  " + t);
        }
        xletDirector = new XletDirector(this);
        if (definesFonts) {
            try {
                fontFactory = new FontFactory();
            } catch (Exception ex) {
                if (Debug.LEVEL > 0) {
                    Debug.printStackTrace(ex);
                    Debug.println();
                    Debug.println(" ***  Error!  FontFactory failed to initialize  ***");
                    Debug.println();
                }
            }
        }

        assignColorKeys();

        try {
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

                //
                // Manage font loading
                //
                protected Font getFontHelper(String name, int style, int size) {
                    if (fontFactory != null) {
                        try {
                            return fontFactory.createFont(name, style, size);
                        } catch (Exception ex) {
                            // ignored - an error is already reported by
                            // AssetFinder, if appropriate
                        }
                    }
                    return null;
                }

                //
                // Map the color keys according to the HAVi API.
                // cf. HD cookbook 19-4, "Those Crazy Color Keys".
                //
                protected int getColorKeyCodeHelper(Color c) {
                    if (c == Color.red) {
                        return redKeyCode;
                    } else if (c == Color.green) {
                        return greenKeyCode;
                    } else if (c == Color.blue) {
                        return blueKeyCode;
                    } else if (c == Color.yellow) {
                        return yellowKeyCode;
                    } else {
                        if (Debug.ASSERT) {
                            Debug.assertFail("unknown color key");
                        }
                        return 0;
                    }
                }
            });

            AssetFinder.setSearchPath(null, new File[]{resourcesDir});
            if (AssetFinder.tryURL("images.map") != null) {
                AssetFinder.setImageMap("images.map");
                if (Debug.LEVEL > 0) {
                    Debug.println("Found images.map, using mosaic.");
                }
            } else {
                if (Debug.LEVEL > 0) {
                    Debug.println("No images.map, not using mosaic.");
                }
            }

            GrinBinaryReader reader =
                    new GrinBinaryReader(AssetFinder.getURL(
                    showFileName).openStream());
            show = new Show(director);
            reader.readShow(show);

            reader = new GrinBinaryReader(AssetFinder.getURL(
                    "xlet_show.grin").openStream());
            xletShow = new Show(xletDirector);
            reader.readShow(xletShow);

        } catch (IOException e) {
            if (Debug.LEVEL > 0) {
                Debug.printStackTrace(e);
            }
            Debug.println("Error in reading the show file");
            throw new InterruptedException();
        }

        animationEngine.checkDestroy();
        animationEngine.initClients(new AnimationClient[]{show, xletShow});
        animationEngine.initContainer(rootContainer,
                new Rectangle(0, 0, xletWidth, xletHeight));

    }

    //
    // Assign the four color keys to a HAVI VK_ code.
    // cf. HD cookbook 19-4, "Those Crazy Color Keys".
    //
    private void assignColorKeys() {
        float[] hues = {getKeyHue(0), getKeyHue(1),
            getKeyHue(2), getKeyHue(3)};
        redKeyCode = getClosest(hues, Color.red);
        hues[redKeyCode] = 1000f;
        redKeyCode += HRcEvent.VK_COLORED_KEY_0;

        greenKeyCode = getClosest(hues, Color.green);
        hues[greenKeyCode] = 1000f;
        greenKeyCode += HRcEvent.VK_COLORED_KEY_0;

        blueKeyCode = getClosest(hues, Color.blue);
        hues[blueKeyCode] = 1000f;
        blueKeyCode += HRcEvent.VK_COLORED_KEY_0;

        yellowKeyCode = getClosest(hues, Color.yellow);
        hues[yellowKeyCode] = 1000f;
        yellowKeyCode += HRcEvent.VK_COLORED_KEY_0;
    }

    //
    // Used by assignColorKeys()
    //
    private int getClosest(float[] hues, Color goal) {
        float goalHue = getHue(goal);
        int result = -1;
        float resultDiff = 100f;
        for (int i = 0; i < hues.length; i++) {
            float diff = Math.abs(goalHue - hues[i]);
            if (diff > 0.5f && diff <= 1f) {
                diff = 1f - diff;
            }
            if (resultDiff >= diff) {
                result = i;
                resultDiff = diff;
            }
        }
        return result;
    }

    //
    // Used by assignColorKeys()
    //
    private float getKeyHue(int key) {
        key += HRcEvent.VK_COLORED_KEY_0;
        Color c = HRcCapabilities.getRepresentation(key).getColor();
        return getHue(c);
    }

    //
    // Used by assignColorKeys()
    //
    private float getHue(Color c) {
        float[] hsb = Color.RGBtoHSB(c.getRed(), c.getGreen(),
                c.getBlue(), null);
        return hsb[0] - ((float) Math.floor(hsb[0]));
    }

    public void animationFinishInitialization() {
        show.activateSegment(show.getSegment(showInitialSegment));
        xletShow.activateSegment(xletShow.getSegment("S:Initialize"));

        UserEventRepository userEventRepo = new UserEventRepository("x");
        userEventRepo.addAllArrowKeys();
        userEventRepo.addAllColourKeys();
        userEventRepo.addAllNumericKeys();
        userEventRepo.addKey(HRcEvent.VK_ENTER);
        userEventRepo.addKey(HRcEvent.VK_0);

        EventManager.getInstance().addUserEventListener(this, userEventRepo);
        pushKeyInterest(show);
        rootContainer.addMouseMotionListener(this);
            rootContainer.addMouseListener(this);
        rootContainer.requestFocus();
    }

    /**
     * A remote control event that is coming in via
     * org.dvb.event.UserEventListener
     **/
    public void userEventReceived(UserEvent e) {
        int type = e.getType();
        if (type == HRcEvent.KEY_PRESSED) {
            int code = e.getCode();
            xletShow.handleKeyPressed(code);  // debug menu always gets the key
            for (int i = 0; i < keyInterestOrder.length; i++) {
                boolean isHandled = keyInterestOrder[i].handleKeyPressed(code);
                if (isHandled) {
                    break;
                }
            }
        } else if (sendKeyUp && type == HRcEvent.KEY_RELEASED) {
            int code = e.getCode(); 
            xletShow.handleKeyReleased(code); // debug menu always gets the key
            for (int i = 0; i < keyInterestOrder.length; i++) {
                boolean isHandled = keyInterestOrder[i].handleKeyReleased(code);
                if (isHandled) {
                    break;
                }
            }
        // Note that Gun Bunny doesn't do anything on key_released,
        // or at least it didn't when this comment was written.
        // Because of this setting sendKeyUp false doesn't have
        // a visible effect on this particular game, but this xlet
        // can trivially be adapted for use as a generic game xlet.
        // If you do this, then being able to turn off key up events
        // is really useful.
        }
    }

   /**
     * Inserts a new Show at the top of the key interest stack.
     * KeyEvents are delivered to the shows
     * starting from the top of the key interest stack.
     * If the show's currently active segment do not have any rc_handler
     * that uses the key, then the the event
     * is sent to the next show on the stack.
     * MouseEvents are sent to all the shows in the key interest list.
     */
    public synchronized void pushKeyInterest(Show show) {
        if (keyInterestOrder == null) {
            keyInterestOrder = new Show[] {show} ;
            return;
        }

        Show[] newList = new Show[keyInterestOrder.length+1];
        newList[0] = show;
        for (int i = 0; i < keyInterestOrder.length; i++) {
            newList[i+1] = keyInterestOrder[i];
        }
        keyInterestOrder = newList;
    }
    /**
     * Removes the show at the top of the key interest stack.
     * KeyEvents are delivered to a show in the order
     * starting from the top of the key interest stack.
     * If the show's currently active segment do not have any rc_handler
     * that uses the key, then the the event
     * is sent to the next show on the stack.
     * MouseEvents are sent to all the shows in the key interest list.
     */
    public synchronized Show popKeyInterest() {
        if (keyInterestOrder == null || keyInterestOrder.length == 0) {
            return null;
        }

        Show show = keyInterestOrder[0];
        Show[] newList = new Show[keyInterestOrder.length-1];
        for (int i = 1; i < keyInterestOrder.length; i++) {
            newList[i-1] = keyInterestOrder[i];
        }
        keyInterestOrder = newList;
        return show;
    }


    /**
     * Mouse motion callback
     **/
    public void mouseMoved(MouseEvent e) {
        int x = e.getX();
        int y = e.getY();
        GrinXHelper helper = new GrinXHelper(xletShow);
        helper.setCommandNumber(GrinXHelper.MOUSE_MOVE);
        helper.setCommandObject(new Point(x,y));
        xletShow.runCommand(helper);

        for (int i = 0; i < keyInterestOrder.length; i++) {
            Show show = (Show) keyInterestOrder[i];
            helper = new GrinXHelper(show);
            helper.setCommandNumber(GrinXHelper.MOUSE_MOVE);
            helper.setCommandObject(new Point(x, y));
            show.runCommand(helper);
        }
    }

    /**
     * Mouse motion callback (when a button is down)
     **/
    public void mouseDragged(MouseEvent e) {
        int x = e.getX();
        int y = e.getY();
        GrinXHelper helper = new GrinXHelper(xletShow);
        helper.setCommandNumber(GrinXHelper.MOUSE_MOVE);
        helper.setCommandObject(new Point(x,y));
        xletShow.runCommand(helper);

        for (int i = 0; i < keyInterestOrder.length; i++) {
            Show show = (Show) keyInterestOrder[i];
            helper = new GrinXHelper(show);
            helper.setCommandNumber(GrinXHelper.MOUSE_MOVE);
            helper.setCommandObject(new Point(x, y));
            show.runCommand(helper);
        }
    }

    /**
     * Mouse clicked callback
     **/
    public void mouseClicked(MouseEvent e) {
        int x = e.getX();
        int y = e.getY();
        GrinXHelper helper = new GrinXHelper(xletShow);
        helper.setCommandNumber(GrinXHelper.MOUSE_CLICK);
        helper.setCommandObject(new Point(x,y));
        xletShow.runCommand(helper);

        for (int i = 0; i < keyInterestOrder.length; i++) {
            Show show = (Show) keyInterestOrder[i];
            helper = new GrinXHelper(show);
            helper.setCommandNumber(GrinXHelper.MOUSE_CLICK);
            helper.setCommandObject(new Point(x, y));
            show.runCommand(helper);
        }
    }

    /**
     * Mouse pressed callback
     **/
    public void mousePressed(MouseEvent e) {
    }

    /**
     * Mouse released callback
     **/
    public void mouseReleased(MouseEvent e) {
    }

    /**
     * Mouse entered callback
     **/
    public void mouseEntered(MouseEvent e) {
    }

    /**
     * Mouse exited callback
     **/
    public void mouseExited(MouseEvent e) {
    }
}
