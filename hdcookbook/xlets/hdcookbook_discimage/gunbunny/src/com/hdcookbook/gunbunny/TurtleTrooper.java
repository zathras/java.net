
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

import com.hdcookbook.gunbunny.util.ImageUtil;
import com.hdcookbook.gunbunny.util.Debug;

/**
 * Represents the state of one of the line of turtle  soldiers that
 * the gun bunny tries to blast.
 * 
 * @author Shant Mardigian
 * @author Bill Foote
 */
public class TurtleTrooper {

    private Image turtleTrooperImg;
    private Image turtleTrooperBlamImg;
    private ImageSprite sprite;
    private Rectangle ourBounds = new Rectangle();
    private boolean startBlam = false;
    private int blamFramesLeft = -1;    // -1 means not blamming
    
    private int xloc;
    private int yloc = 240;
    
    private boolean active = true;
    
    public TurtleTrooper(int x, Image turtleTrooperImg, 
                         Image turtleTrooperBlamImg)
    {
        this.turtleTrooperImg = turtleTrooperImg; 
        this.turtleTrooperBlamImg = turtleTrooperBlamImg;
        this.xloc = x;
        this.sprite = new ImageSprite(turtleTrooperImg);
        this.sprite.initPosition(this.xloc, this.yloc);
        this.sprite.getBounds(ourBounds);
    }

    /**
     * Figures out the state of this trooper for the given frame.
     **/
    public void nextFrame(int numFrames) {
        if (startBlam) {
            startBlam = false;
            blamFramesLeft = 10;
            sprite.nextFrame(turtleTrooperBlamImg);
        } else if (blamFramesLeft >= 0) {
            blamFramesLeft -= numFrames;
            if (blamFramesLeft <= 0) {
                blamFramesLeft = -1;
                sprite.nextFrameOff();
            } else {
                sprite.nextFrame(turtleTrooperBlamImg);
            }
        } else if (active) {
            sprite.nextFrame(turtleTrooperImg);
        } else {
            sprite.nextFrameOff();
        }
    }

    /**
     * Returns true iff the trooper is visible in the current frame.  An
     * exploding trooper is visible.
     **/
    public boolean isVisible() {
        return active || blamFramesLeft >= 0;
    }

    /** 
     * Paint the trooper's current state.
     **/
    public void paintFrame(Graphics2D g, boolean paintAll, Animator animator) {
        sprite.paintFrame(g, paintAll, animator);
    }

    /** 
     * Determine if the trooper is hit by something covering the area
     * passed in as hitRect.
     **/
    public boolean hitBy(Rectangle hitRect) {
        if (active && ourBounds.intersects(hitRect)) {
            if (Debug.LEVEL > 0) {
                Debug.println("Trooper hit!");
            }
            return true;
        }
        return false;
    }

    /**
     * Tells the trooper that he should start exploding with the next
     * frame.
     **/
    public void startBlamWithNextFrame() {
        active = false;
        startBlam = true;
    }

    /**
     * Tells a dead trooper that he should wake up with the next frame.
     **/
    public void awakenWithNextFrame() {
        active = true;
        startBlam = false;
        blamFramesLeft = -1;
    }
}
