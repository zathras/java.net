
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


package com.hdcookbook.bookmenu.monitor;

import java.rmi.Remote;
import java.rmi.RemoteException;

import org.dvb.application.AppProxy;
import org.dvb.application.AppsDatabase;
import org.dvb.application.AppStateChangeEvent;
import org.dvb.application.AppStateChangeEventListener;
import org.dvb.application.AppID;

import com.hdcookbook.bookmenu.MonitorIXCInterface;
import com.hdcookbook.grin.util.Debug;

/**
 * This class is the object we export via inter-xlet communication.
 * When another xlet asks us to do something via this interface, we do
 * it.
 *
 *   @author     Bill Foote (http://jovial.com)
 **/
public class MonitorIXCListener 
        implements MonitorIXCInterface, AppStateChangeEventListener {

    /**
     * The org ID for all of our xlets
     **/
    public final static int ORG_ID = 0x56789abc;

    /**
     * The App ID for the menu app.  This must be the same as the ID
     * in the bdjo file.
     **/
    public final static int MENU_APP_ID = 0x4002;

    /**
     * The App ID for the game app.  This must be the same as the ID
     * in the bdjo file.
     **/
    public final static int GAME_APP_ID = 0x0003;

    private MonitorXlet xlet;
    private AppProxy gameXlet;  // Set while we're playing game

    public MonitorIXCListener(MonitorXlet xlet) {
        this.xlet = xlet;
    }

    private AppProxy getAppProxy(int orgID, int appID) {
        AppsDatabase db = AppsDatabase.getAppsDatabase();
        return db.getAppProxy(new AppID(orgID, appID));
    }

    public void init() {
    }

    /** 
     * Called by the menu xlet to start the game.
     **/
    public void startGame(String s) throws RemoteException {
        if (Debug.LEVEL > 0) {
            Debug.println("****  Monitor xlet to start game ***");
        }
        gameXlet = getAppProxy(ORG_ID, GAME_APP_ID);
        gameXlet.addAppStateChangeEventListener(this);
        try {
            getAppProxy(ORG_ID, MENU_APP_ID).stop(false);
            gameXlet.start();
        } catch (Throwable ignored) {
            if (Debug.LEVEL > 0) {
                Debug.printStackTrace(ignored);
                Debug.println();
                Debug.println("***  Failed to start game.  For this to work, the monitor xlet must");
                Debug.println("***  be signed.  Perhaps it wasn't signed correctly?");
                Debug.println();
            }
        }
    }

    /**
     * This could be called by another xlet to start the menu.  Gun Bunny
     * doesn't use this, though.  Instead, the monitor xlet monitors the
     * state of Gun Bunny, and when the Gun Bunny xlet is destroyed, it
     * automatically launches the menu xlet.
     **/
    public void startMenu(String s) throws RemoteException {
        doStartMenu();
    }

    private void doStartMenu() {
        if (Debug.LEVEL > 0) {
            Debug.println("****  Monitor xlet to start menu  ***");
        }
        try {
            gameXlet.removeAppStateChangeEventListener(this);
            gameXlet.stop(false);
        } catch (Throwable ignored) {
            if (Debug.LEVEL > 0) {
                Debug.printStackTrace(ignored);
                Debug.println();
                Debug.println("***  Failed to start menu.  For this to work, the monitor xlet must");
                Debug.println("***  be signed.  Perhaps it wasn't signed correctly?");
                Debug.println();
            }
        }
        try {
            gameXlet = null;
            getAppProxy(ORG_ID, MENU_APP_ID).start();
        } catch (Throwable ignored) {
            if (Debug.LEVEL > 0) {
                Debug.printStackTrace(ignored);
                Debug.println();
            }
        }
    }

    /**
     * Callback from system via AppStateChangeEventListener
     **/
    public void stateChange(AppStateChangeEvent event) {
        // Automatically re-launch the menu when the game puts itself
        // in the destroyed state
        if (Debug.LEVEL > 0) {
            Debug.println();
            Debug.println("*** Monitor gets AppStateChangeEvent");
            Debug.println("    " + event);
            Debug.println("    App ID:  " + event.getAppID().getAID());
            Debug.println();
        }
        if (!event.hasFailed() && event.getAppID().getAID() == GAME_APP_ID) {
            if (event.getToState() == AppProxy.DESTROYED) {
                doStartMenu();
            }
        }
    }

    public void destroy() {
        AppProxy g = gameXlet;
        if (g != null) {
            g.removeAppStateChangeEventListener(this);
        }
    }
}
