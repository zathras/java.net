
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

import org.dvb.event.EventManager;
import org.dvb.event.UserEvent;
import org.dvb.event.UserEventListener;
import org.dvb.event.UserEventRepository;
import org.dvb.dsmcc.ServiceDomain;
import org.dvb.io.ixc.IxcRegistry;
import org.havi.ui.HScene;
import org.havi.ui.HSceneFactory;

import org.bluray.net.BDLocator;
import org.bluray.ui.event.HRcEvent;

import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseEvent;
import java.awt.Rectangle;
import java.io.File;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;

import javax.tv.service.selection.NormalContentEvent;
import javax.tv.service.selection.ServiceContext;
import javax.tv.service.selection.ServiceContextEvent;
import javax.tv.service.selection.ServiceContextException;
import javax.tv.service.selection.ServiceContextFactory;
import javax.tv.service.selection.ServiceContextListener;
import javax.tv.xlet.Xlet;
import javax.tv.xlet.XletContext;
import javax.tv.xlet.XletStateChangeException;

import com.hdcookbook.grin.Show;
import com.hdcookbook.grin.animator.AnimationEngine;
import com.hdcookbook.grin.animator.AnimationClient;
import com.hdcookbook.grin.animator.AnimationContext;
import com.hdcookbook.grin.animator.DirectDrawEngine;
import com.hdcookbook.grin.animator.RepaintDrawEngine;
import com.hdcookbook.grin.util.Debug;
import com.hdcookbook.grin.util.AssetFinder;
import com.hdcookbook.bookmenu.MonitorIXCInterface;


/**
 * Xlet for the main menu/controller in the HD Cookbook disc
 *
 *   @author     Bill Foote (http://jovial.com)
 **/
public class MenuXlet implements Xlet, UserEventListener, 
                                 MouseListener, MouseMotionListener,
                                 ServiceContextListener, AnimationContext
{

    public XletContext context;
    public HScene scene;
    public MenuDiscNavigator navigator;
    public AnimationEngine engine;
    public MenuDirector director;
    public Show show;

    private boolean destroyed = false;
    private boolean isPresenting = false;       // Set by service context event
    private ServiceContext ourServiceContext;
    private MonitorIXCInterface monitorXlet = null;
    private ServiceDomain assetsJar;

    public void initXlet(XletContext ctx) throws XletStateChangeException {
        this.context = ctx;
        if (Debug.LEVEL > 0) {
            Debug.println("MenuXlet in initXlet");
        }
        DirectDrawEngine dde = new DirectDrawEngine();
        dde.setFps(24000);
        engine = dde;
        engine.initialize(this);
    }

    public void startXlet() throws XletStateChangeException {
        if (Debug.LEVEL > 0) {
            Debug.println("MenuXlet in startXlet");
        }
        engine.start();
    }

    public void pauseXlet() {
        if (Debug.LEVEL > 0) {
            Debug.println("MenuXlet in pauseXlet");
        }
        engine.pause();
    }

    public void destroyXlet(boolean unconditional) 
            throws XletStateChangeException 
    {
        if (Debug.LEVEL > 0) {
            Debug.println("MenuXlet in destroyXlet");
        }
        synchronized (this) {
            if (destroyed) {
                return;
            }
            destroyed = true;
            notifyAll();
        }
        engine.destroy();
        director.destroy();
        if (navigator != null) {
            navigator.destroy();
        }
        if (assetsJar != null) {
            try {
                assetsJar.detach();
                        // For greater memory efficiency, one can
                        // detach assetsJar as soon as everything has
                        // been read from it.  That could be done by
                        // triggering a command from GRIN once all of
                        // the initialization is done.
            } catch (Exception ex) {
                if (Debug.LEVEL > 0) {
                    Debug.printStackTrace(ex);
                }
            }
        }
        if (ourServiceContext != null) {
            ourServiceContext.removeListener(this);
        }
        EventManager.getInstance().removeUserEventListener(this);
    }

    public void animationInitialize() throws InterruptedException {
        ServiceContextFactory scf = ServiceContextFactory.getInstance();
        try {
            ourServiceContext = scf.getServiceContext(context);
        } catch (ServiceContextException ex) {
            if (Debug.ASSERT) {
                Debug.printStackTrace(ex);
                Debug.assertFail();
            }
        }
        ourServiceContext.addListener(this);
        waitForServiceContextPresenting();
        scene = HSceneFactory.getInstance().getDefaultHScene();
        scene.setLayout(null);
        scene.setBounds(0, 0, 1920, 1080);
        scene.setVisible(true);
        navigator = new MenuDiscNavigator(this);

        assetsJar = new ServiceDomain();
        try {
            BDLocator loc = new BDLocator("bd://JAR:00004");
            assetsJar.attach(loc);
        } catch (Exception ex) {
            if (Debug.LEVEL > 0) {
                // If this happens, it's a bug.
                Debug.printStackTrace(ex);
            }
        }
        File[] path = { assetsJar.getMountPoint() } ;
        AssetFinder.setHelper(new MenuAssetFinder(this));
        AssetFinder.setSearchPath(null, path);
        if (AssetFinder.tryURL("images.map") != null) {
            if (Debug.LEVEL > 0) {
                Debug.println("Found images.map, using mosaic.");
            }
            AssetFinder.setImageMap("images.map");
        } else if (Debug.LEVEL > 0) {
            Debug.println("No images.map, not using mosaic.");
        }

        navigator.init();

        director = new MenuDirector(this);
        director.init();
        show = director.createShow();

        engine.checkDestroy();

        AnimationClient[] clients = { show };
        engine.initClients(clients);
        Rectangle bounds = new Rectangle(0, 0, 1920, 1080);
        engine.initContainer(scene, bounds);
    }

    public void animationFinishInitialization() throws InterruptedException {
        System.gc();
        show.activateSegment(show.getSegment("S:Initialize"));

        UserEventRepository userEventRepo = new UserEventRepository("x");
        userEventRepo.addAllArrowKeys();
        userEventRepo.addAllColourKeys();
        userEventRepo.addAllNumericKeys();
        userEventRepo.addKey(HRcEvent.VK_ENTER);
        userEventRepo.addKey(HRcEvent.VK_POPUP_MENU);
        EventManager.getInstance().addUserEventListener(this, userEventRepo);

        scene.addMouseMotionListener(this);
        scene.addMouseListener(this);
        scene.requestFocus();
    }

    /**
     * Callback from system via ServiceContextListener.
     **/
    public void receiveServiceContextEvent(ServiceContextEvent e)  {
        if (e instanceof NormalContentEvent) {
            synchronized(this) {
                isPresenting = true;
                notifyAll();
            }
        }
    }

    private void waitForServiceContextPresenting() {
        synchronized(this) {
            if (ourServiceContext.getService() != null) {
                isPresenting = true;
            }
            for (;;) {
                if (isPresenting) {
                    if (Debug.LEVEL > 0) {
                        Debug.println("Service context is presenting");
                    }
                    return;
                } else if (destroyed) {
                    return;
                }
                try {
                    if (Debug.LEVEL > 0) {
                        Debug.println("Waiting for service context to present");
                    }
                    wait();
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    return;     // Bail out
                }
            }
        }
    }

    /**
     * Get the remote IXC object that we use to communicate with the
     * monitor xlet.
     **/
    public synchronized MonitorIXCInterface getMonitorXlet() {
        int tries = 0;
        while (monitorXlet == null) {           // See continue in loop body
            boolean notBound = false;
            String orgID = (String) context.getXletProperty("dvb.org.id");
            String appID = (String) context.getXletProperty("dvb.app.id");
            int appIDint = -1;
            try {
                appIDint =  Integer.parseInt(appID, 16);
            } catch (Exception ignored) {
                if (Debug.LEVEL > 0) {
                    Debug.printStackTrace(ignored);
                }
            }
            String name = "/" + orgID + 
                          "/" + Integer.toHexString(appIDint - 1) +
                          "/Monitor";
            // The monitor app's app ID is one less than ours.
            try {
                monitorXlet = tryIXCRegistry(name);
            } catch (NotBoundException ex) {
                notBound = true;
            }

            if (monitorXlet == null) {
                int orgIDint = (int) Long.parseLong(orgID, 16);
                if (orgIDint < 0) {
                    // There was a spec bug in MHP/GEM that suggested
                    // a very strange negative hex number as the org ID.
                    // That bug was fixed after players shipped, so it's
                    // a good idea to try under the other name, too.
                    orgIDint = -orgIDint;
                    name = "/-" + Integer.toHexString(orgIDint) + 
                           "/" + Integer.toHexString(appIDint - 1) +
                           "/Monitor";
                    try {
                        monitorXlet = tryIXCRegistry(name);
                    } catch (NotBoundException ex) {
                        notBound = true;
                    }
                }
            }
            if (monitorXlet == null) {
                // Keep trying for two seconds...
                try {
                    tries++;
                    if (tries < 21) {
                        Thread.sleep(100);
                        continue;
                    }
                } catch (InterruptedException ex2) {
                    Thread.currentThread().interrupt();
                }
                // Give up, but provide a stub so at least we don't
                // get null pointer exceptions.  If we get here, then
                // there's  some kind of bug; this just adds a little bit
                // of robustness in a bad situation.
                if (Debug.LEVEL > 0) {
                    Debug.println();
                    Debug.println("***  Monitor xlet not found!  ***");
                    Debug.println("This is a serious bug; all monitor app "
                                  + "functionality won't be available.");
                    Debug.println();
                }
                monitorXlet = new MonitorIXCInterface() {
                    public void startGame(String s) {
                    }
                    public void startMenu(String s) {
                    }
                };
            }
        }
        return monitorXlet;
    }

    //
    // Try looking up the monitor xlet at the given name.  
    // Throws NotBoundException if there's no error, but it doesn't
    // find the mnitor xlet.
    //
    private MonitorIXCInterface tryIXCRegistry(String name) 
                throws NotBoundException 
    {
        NotBoundException notBound = null;
        try {
            try {
                if (Debug.LEVEL > 0) {
                    Debug.println("Connecting to IXC object at " + name);
                }
                return (MonitorIXCInterface) IxcRegistry.lookup(context, name);
            } catch (RemoteException ignored) {
                if (Debug.LEVEL > 0) {
                    Debug.printStackTrace(ignored);
                    // Must be a bug
                }
            } catch (NotBoundException ex) {
                // Maybe the monitor xlet hasn't had time to start yet
                notBound = ex;
            }
        } catch (Throwable ignored) {
            // Player bug:  sometimes a player will throw the wrong kind of
            // exception.
        }
        if (notBound != null) {
            throw notBound;
        }
        return null;
    }

    /**
     * Stop running the menu (that is, us), and start running the
     * Gun Bunny game.  This just passes the request on to the
     * monitor xlet.
     **/
    public void startGame() {
        try {
            getMonitorXlet().startGame("");
        } catch (RemoteException ignored) {
            if (Debug.LEVEL > 0) {
                Debug.printStackTrace(ignored);
            }
        }
    }


    /**
     * Mouse motion callback
     **/
    public void mouseMoved(MouseEvent e) {
        show.handleMouseMoved(e.getX(), e.getY(), false);
    }

    /**
     * Mouse motion callback (when a button is down)
     **/
    public void mouseDragged(MouseEvent e) {
        show.handleMouseMoved(e.getX(), e.getY(), false);
    }

    /**
     * Mouse clicked callback
     **/
    public void mouseClicked(MouseEvent e) {
    }

    /**
     * Mouse pressed callback
     **/
    public void mousePressed(MouseEvent e) { 
        show.handleMousePressed(e.getX(), e.getY(), false);
    }

    /**
     * Mouse released callback
     **/
    public void mouseReleased(MouseEvent e) { }

    /**
     * Mouse entered callback
     **/
    public void mouseEntered(MouseEvent e) { }

    /**
     * Mouse exited callback
     **/
    public void mouseExited(MouseEvent e) { }

    /**
     * A remote control event that is coming in via
     * org.dvb.event.UserEventListener
     **/
    public void userEventReceived(UserEvent e) {
        if (e.getType() == HRcEvent.KEY_PRESSED) {
            show.handleKeyPressed(e.getCode());
        }
    }

}

