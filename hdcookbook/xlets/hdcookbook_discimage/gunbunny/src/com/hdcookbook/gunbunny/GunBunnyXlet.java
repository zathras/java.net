
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

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;

import javax.tv.xlet.XletStateChangeException;

import org.dvb.ui.FontFactory;

import com.hdcookbook.gunbunny.util.ImageUtil;
import com.hdcookbook.gunbunny.util.Debug;

/**
 * The xlet class for Gun Bunny.
 * 
 * @author Shant Mardigian
 * @author Bill Foote
 *
 */
public class GunBunnyXlet extends BaseXlet {

    private static int frameWidth = 40;
    private Color frameColor = new Color(0, 0, 128, 255);

    private Game game = null;
    private Animator animator = null;
    private Rectangle animPos;
    int frame;
    public FontFactory fontFactory;

    /** 
     * Inherited from Component, this should only really be called if
     * we're doing repaint draw.  However, it's important that we handle
     * it correctly - it might be generated on a PC player, or on some
     * other multi-function device where we might receive an expose
     * event.
     **/
    public void paint(Graphics gArg) {
        Graphics2D g = (Graphics2D) gArg;
        g.setComposite(AlphaComposite.Src);
        g.setColor(frameColor);
        g.fillRect(0, 0, width, frameWidth);
        g.fillRect(0, 0, frameWidth, height);
        g.fillRect(width - frameWidth, 0, frameWidth, height);
        g.fillRect(0, height-frameWidth, width, frameWidth);

        Game gm;
        synchronized (this) {
            gm = game;
        }
        if (gm != null) {
            g.translate(animPos.x, animPos.y);
            g.setClip(0, 0, animPos.width, animPos.height);
            gm.paintFrame(g, true, null);
        }
    }


    protected void doXletLoop() throws InterruptedException {
        try {
            fontFactory = new FontFactory();
        } catch (Exception ex) {
            if (Debug.ASSERT) {
                Debug.printStackTrace(ex);
                Debug.assertFail(ex.toString());
            }
        }
        Game gm = new Game();
        gm.initialize(this);
        frame = 0;
        animPos = new Rectangle();
        animPos.x = frameWidth + 10;
        animPos.y = frameWidth + 10;
        animPos.width = width - 2*animPos.x;
        animPos.height = height - 2*animPos.y;
        setAnimator(new DirectDrawAnimator());
        synchronized(this) {
            game = gm;
        }
        startVideo("bd://0.PLAYLIST:00003");
        try {
            for (;;) {
                if (getDestroyed()) {
                    return;             // End xlet
                } else if (Thread.interrupted()) {
                    throw new InterruptedException();
                }
                Animator a;
                int f;
                synchronized(this) {
                    a = animator;
                }
                animator.animateGame(frame, game);
                synchronized(this) {
                    frame++;
                }
            }
        } finally {
            animator.destroy();
        }
    }

    private synchronized void setAnimator(Animator newAnimator) {
        if (Debug.LEVEL > 0) {
            Debug.println("Setting animator to " 
                           + newAnimator.getClass().getName());
        }
        synchronized(this) {
            if (animator != null) {
                animator.destroy();
            }
            animator = newAnimator;
            animator.initAtFrame(frame, scene, animPos);
        }
    }

    /**
     * Destroy ourself.  This is called when we go to GAME_OVER.  When
     * we destroy ourself, the monitor xlet notices, and it re-launches
     * the menu xlet.
     **/
    public void destroySelf() {
        if (Debug.LEVEL > 0) {
            Debug.println();
            Debug.println("*******************************");
            Debug.println("*    GUN BUNNY BIDS ADIEU     *");
            Debug.println("*******************************");
            Debug.println();
        }
        try {
            destroyXlet(true);
        } catch (XletStateChangeException ignored) {
        }
        if (Debug.LEVEL > 0) {
            Debug.println("Calling notifyDestroyed...");
        }
        xletContext.notifyDestroyed();
    }
    
    /**
     * See superclass definition.
     **/
    protected void numberKeyPressed(int value) {
        Animator newAnimator = null;
        if (Debug.LEVEL > 0) {
            Debug.println("NUMBER KEY:  " + value);
        }
        if (value == 1 && !(animator instanceof DirectDrawAnimator)) {
            setAnimator(new DirectDrawAnimator());
        } else if (value == 2 && !(animator instanceof SFAAAnimator)) {
            setAnimator(new SFAAAnimator());
        } else if (value == 3 && !(animator instanceof RepaintDrawAnimator)) {
            setAnimator(new RepaintDrawAnimator());
        } else if (value == 0) {
            if (game != null) {
               game.handleEnd();
            }
        }
    }
    
    /**
     * See superclass definition.
     **/
    protected void colorKeyPressed(int value) {
    }
    
    /**
     * See superclass definition.
     **/
    protected void popupKeyPressed() {
    }
    
    /**
     * See superclass definition.
     **/
    protected void enterKeyPressed() {
        Game g = game;
        if (g != null) {
            g.handleEnter();
        }
    }
        
    /**
     * See superclass definition.
     **/
    protected void arrowLeftKeyPressed(){
        Game g = game;
        if (g != null) {
            g.handleLeft();
        }
    }
    
    /**
     * See superclass definition.
     **/
    protected void arrowRightPressed(){
        Game g = game;
        if (g != null) {
            g.handleRight();
        }
    }
    
    /**
     * See superclass definition.
     **/
    protected void arrowUpPressed(){
        Game g = game;
        if (g != null) {
            g.handleUp();
        }
    }
    
    /**
     * See superclass definition.
     **/
    protected void arrowDownPressed(){
        Game g = game;
        if (g != null) {
            g.handleDown();
        }
    }    
}
