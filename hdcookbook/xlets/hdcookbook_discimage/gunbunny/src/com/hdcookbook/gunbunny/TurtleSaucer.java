
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

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.util.Random;

import com.hdcookbook.gunbunny.util.ImageUtil;
import com.hdcookbook.gunbunny.util.Debug;

/**
 * Represents the state of the flying saucer that flys across the
 * top of the screen.
 * 
 * @author Shant Mardigian
 * @author Bill Foote
 **/
public class TurtleSaucer {
    private Image turtleSaucerBlam;
    private Image turtleSaucer;
    private Rectangle ourBounds = new Rectangle();
    private ImageSprite sprite;
    private boolean active = false;
    private boolean startBlam = false;
    private int blamFramesLeft = -1;    // -1 means not blamming
    private int dx;
    private int xloc;
    private int yloc = 80;
    private Random random = new Random();
    
    public TurtleSaucer(Image turtleSaucer, Image turtleSaucerBlam) {
        this.turtleSaucer = turtleSaucer;
        this.turtleSaucerBlam = turtleSaucerBlam;
        this.sprite = new ImageSprite(turtleSaucer);
        this.sprite.initImageOff();
    }

    /**
     * Moves the saucer to where it should be at the given frame.  This
     * takes care of randomly starting a saucer on its path from time
     * to time, too.
     **/
    public void nextFrame(int numFrames) {
        for (int i = 0; i < numFrames; i++) {
            if (startBlam) {
                startBlam = false;
                blamFramesLeft = 14;
            } 
            if (blamFramesLeft >= 0) {
                xloc += dx;
                blamFramesLeft--;
            } else if (!active && random.nextInt(24 * 5) == 7) {
                // Make a saucer every 5 seconds or so
                active = true;
                if (random.nextInt(2) == 0)  {
                    xloc = -10;
                    dx = 15;
                } else {
                    xloc = 1840;
                    dx = -15;
                }
            } else if (active) {
                xloc += dx;
                if (xloc > 1840 || xloc < -10) {
                    active = false;
                }
            }
        }
        if (active) {
            sprite.nextFrame(xloc, yloc, turtleSaucer);
        } else if (blamFramesLeft >= 0) {
            sprite.nextFrame(xloc, yloc, turtleSaucerBlam);
        } else {
            sprite.nextFrameOff();
        }
    }

    /**
     * Paint the state of the saucer at the current frame.
     **/
    public void paintFrame(Graphics2D g, boolean paintAll, Animator animator) {
        sprite.paintFrame(g, paintAll, animator);
    }

    /** 
     * Determine if the saucer is hit by something covering the area
     * passed in as hitRect.
     **/
    public boolean hitBy(Rectangle hitRect) {
        // This hit calculation isn't quite right in the case where
        // we skip frames, because it doesn't account for the horizontal
        // motion.  The easiest fix would be to go frame-by-frame.
        // I'm on a book deadline, so I didn't do that; it rarely
        // skips frames anyway, and when it does a user won't be so
        // surprised by a near miss, I'm betting.
        sprite.getBounds(ourBounds);
        if (active && ourBounds.intersects(hitRect)) {
            if (Debug.LEVEL > 0) {
                Debug.println("Saucer hit!");
            }
            active = false;
            startBlam = true;
            return true;
        }
        return false;
    }
}
