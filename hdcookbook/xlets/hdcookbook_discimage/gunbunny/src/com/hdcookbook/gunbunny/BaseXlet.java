
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


package com.hdcookbook.gunbunny;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.AlphaComposite;

import javax.media.ControllerListener;
import javax.media.ControllerEvent;
import javax.media.EndOfMediaEvent;
import javax.media.Player;
import javax.media.Manager;

import javax.tv.service.selection.NormalContentEvent;
import javax.tv.service.selection.ServiceContext; 
import javax.tv.service.selection.ServiceContextEvent;
import javax.tv.service.selection.ServiceContextException;
import javax.tv.service.selection.ServiceContextFactory; 
import javax.tv.service.selection.ServiceContextListener;
import javax.tv.xlet.Xlet;
import javax.tv.xlet.XletContext;
import javax.tv.xlet.XletStateChangeException;

import org.bluray.net.BDLocator;
import org.bluray.ui.event.HRcEvent;

import org.davic.media.MediaLocator;


import org.dvb.event.EventManager;
import org.dvb.event.UserEvent;
import org.dvb.event.UserEventListener;
import org.dvb.event.UserEventRepository;
import org.havi.ui.HBackgroundConfigTemplate;
import org.havi.ui.HBackgroundConfiguration;
import org.havi.ui.HScene;
import org.havi.ui.HSceneFactory;

import com.hdcookbook.gunbunny.util.Debug;
import com.hdcookbook.gunbunny.util.ImageUtil;


/**
 * Base xlet for the Gun Bunny game.  This does some generic
 * xlet things, but leaves all of the Gun Bunny specific stuff to
 * a subclass.
 * 
 * @author Shant Mardigian
 * @author Bill Foote
 *
 */
public abstract class BaseXlet 
        extends Component  
        implements Xlet, Runnable, UserEventListener, ServiceContextListener,
                   ControllerListener
{

    private Thread mainThread;
    private boolean mainThreadRunning;
    
    protected XletContext xletContext; 
    protected ServiceContext serviceContext;
    
    protected HScene scene;
    
    protected static final int width = 1920;
    protected static final int height = 1080;
    
    private boolean isPresenting = false;
    private boolean destroyed = false;
    private boolean running = false;
    private Player player = null;

    public BaseXlet() {
    }

    /** 
     * State variable where we record when a destroy has been requested.
     * Threads in our xlet should consult this regularly, and bail out when
     * it becomes true.
     **/
    public synchronized boolean getDestroyed() {
        return destroyed;
    }

    public void initXlet(XletContext ctx) throws XletStateChangeException {
        Debug.setXlet(this);
        this.xletContext = ctx;
    }

    public void pauseXlet() {
        // ignored
    }


    public void startXlet() throws XletStateChangeException {
        ServiceContextFactory scf = ServiceContextFactory.getInstance();
        try {
            serviceContext = scf.getServiceContext(xletContext); 
        } catch (ServiceContextException ex) {
            if (Debug.LEVEL > 0) {
                Debug.printStackTrace(ex);
                Debug.assertFail();
            }
        }
        serviceContext.addListener(this);

        mainThread = new Thread(this, getClass().getName() + " thread");
        mainThreadRunning = true;
        mainThread.start();
    }
    
    public void destroyXlet(boolean unconditional) 
                throws XletStateChangeException 
    {
        synchronized(this) {
            if (destroyed) {
                return;
            }
            destroyed = true;
            notifyAll();
        }
        mainThread.interrupt();
        synchronized(this) {
            while (mainThreadRunning) {
                try {
                    wait();
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        serviceContext.removeListener(this);
        ImageUtil.discardImages();
        EventManager.getInstance().removeUserEventListener(this);
        if (scene != null) {
            scene.remove(this);
            Graphics2D g = (Graphics2D) scene.getGraphics();
            g.setColor(new Color(0, 0, 0, 0));
            g.setComposite(AlphaComposite.Src);
            g.fillRect(0,0 ,1920, 1080);
        }
        if (player != null) {
            player.removeControllerListener(this);
        }
    }

    /**
     * Called by the xlet subclass once it's loaded all of its assets
     * and it's ready for video to start playing
     **/
    protected void startVideo(String playlist) {
        try {
            MediaLocator stars = new MediaLocator(new BDLocator(playlist));
            player = Manager.createPlayer(stars);
        } catch (Exception ex) {
            if (Debug.ASSERT) {
                Debug.printStackTrace(ex);
                Debug.assertFail(ex.toString());
            }
        }
        player.addControllerListener(this);
        player.prefetch();
        player.start();
        waitForStarted(5000);
    }

    /**
     * This is where the main xlet thread executes.  It's our frame pump.
     **/
    public final void run() {
        waitForPresenting();

        scene = HSceneFactory.getInstance().getDefaultHScene();        
        scene.setLayout(null);
        scene.setBounds(0, 0, width, height);
        setBounds(0, 0, width, height);
        scene.add(this);
        scene.setVisible(true);
        setVisible(true);

        UserEventRepository userEventRepo = new UserEventRepository("evt");
        userEventRepo.addAllArrowKeys();
        userEventRepo.addAllColourKeys();
        userEventRepo.addAllNumericKeys();
        userEventRepo.addKey(HRcEvent.VK_ENTER);
        userEventRepo.addKey(HRcEvent.VK_POPUP_MENU);
        EventManager.getInstance().addUserEventListener(this, userEventRepo);
        requestFocus();

        try {
            doXletLoop();
        } catch (InterruptedException ex) {
            // This is OK
        }
        synchronized (this) {
            mainThreadRunning = false;
            notifyAll();
        }
        if (player != null) {
            player.stop();
        }
    }

    /** 
     * The xlet needs to override this for the body of the main execution
     * loop.  It should poll getDestroyed() from time to time, and bail
     * out if it finds it to be true.  It should also gracefully handle
     * it if the thread it's running in is interrupted.
     **/
    abstract protected void doXletLoop() throws InterruptedException;

    private void waitForPresenting() {
        if (serviceContext.getService() != null) {
            synchronized(this) {
                isPresenting = true;
            }
        }
        synchronized (this) {
            while (!isPresenting && !destroyed) {
                // isPresenting set by NormalContentEvent
                try {
                    wait();
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }

    /** 
     * Get a system callback via ServiceContextListener
     **/
    public void receiveServiceContextEvent(ServiceContextEvent e)  {
        if (e instanceof NormalContentEvent) {
            synchronized(this) {
                isPresenting = true;
                this.notifyAll(); 
            }
        }
    }

    /**
     * From ControllerListener
     **/
    public synchronized void controllerUpdate(ControllerEvent event) {
        if (event instanceof EndOfMediaEvent && !destroyed) {
            player.start();
            // On PC players, this causes a noticable pause in
            // the xlet.  To get around this, we just made the
            // playlist we're playing repeat the same video clip
            // of a starfield over and over again, for a total length
            // of an hour or two.  As a result, it's very unlikely
            // a viewer will see anything other than seamless video.
        }
        notifyAll();
        // When waiting for starting, we just poll the player status,
        // so no matter the event we notifyAll().
    }

    //
    // Wait until the state of our player is "started".
    //
    private synchronized boolean waitForStarted(long timeout) {
        long tm = 0;
        if (timeout > 0) {
            tm = System.currentTimeMillis();
        }
        for (;;) {
            if (player.getState() == Player.Started) {
                if (Debug.LEVEL > 0) {
                    Debug.println("Player is in started state");
                }
                return true;
            }
            if (!waitWithTimeout(tm, timeout)) {
                return false;
            }
        }
    }

    //
    // Wait a bit.  Return true if we got notified, false if we timed out
    // or were interrupted.
    private boolean waitWithTimeout(long startTime, long timeout) {
        try {
            if (timeout <= 0) {
                wait();
                return true;
            } else {
                long t = timeout - (System.currentTimeMillis() - startTime);
                if (t <= 0) {
                    return true;
                }
                wait(t);
                t = timeout - (System.currentTimeMillis() - startTime);
                return t > 0;
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    /**
     * Callback from the system via UserEventListener when a remote
     * control keypress is received.
     **/
    public synchronized void userEventReceived(UserEvent e) {
        if (e.getType() == HRcEvent.KEY_PRESSED) {
            switch(e.getCode()){
            
            case HRcEvent.VK_POPUP_MENU:
                popupKeyPressed();
                break;

            case HRcEvent.VK_0:
            case HRcEvent.VK_1:                
            case HRcEvent.VK_2:
            case HRcEvent.VK_3:
            case HRcEvent.VK_4:
            case HRcEvent.VK_5:
            case HRcEvent.VK_6:
            case HRcEvent.VK_7:
            case HRcEvent.VK_8:
            case HRcEvent.VK_9:
                numberKeyPressed(e.getCode() - HRcEvent.VK_0);
                break;

            case HRcEvent.VK_COLORED_KEY_0:
            case HRcEvent.VK_COLORED_KEY_1:
            case HRcEvent.VK_COLORED_KEY_2:
            case HRcEvent.VK_COLORED_KEY_3:
            case HRcEvent.VK_COLORED_KEY_4:
            case HRcEvent.VK_COLORED_KEY_5:
                colorKeyPressed(e.getCode() - HRcEvent.VK_COLORED_KEY_0);
                break;
                
            case HRcEvent.VK_ENTER:
                enterKeyPressed();
                break;

            case HRcEvent.VK_LEFT:
                arrowLeftKeyPressed();
                break;

            case HRcEvent.VK_RIGHT:
                arrowRightPressed();
                break;

            case HRcEvent.VK_UP:
                arrowUpPressed();
                break;

            case HRcEvent.VK_DOWN:
                arrowDownPressed();
                break;

            }            
        }    
    }

    /**
     * Subclasses should override this if they're interested in getting
     * this event.
     **/
    protected void numberKeyPressed(int value){}

    /**
     * Subclasses should override this if they're interested in getting
     * this event.
     **/
    protected void colorKeyPressed(int value){}

    /**
     * Subclasses should override this if they're interested in getting
     * this event.
     **/
    protected void popupKeyPressed(){}

    /**
     * Subclasses should override this if they're interested in getting
     * this event.
     **/
    protected void enterKeyPressed(){}

    /**
     * Subclasses should override this if they're interested in getting
     * this event.
     **/
    protected void arrowLeftKeyPressed(){}

    /**
     * Subclasses should override this if they're interested in getting
     * this event.
     **/
    protected void arrowRightPressed(){}

    /**
     * Subclasses should override this if they're interested in getting
     * this event.
     **/
    protected void arrowUpPressed(){}

    /**
     * Subclasses should override this if they're interested in getting
     * this event.
     **/
    protected void arrowDownPressed(){}
}
