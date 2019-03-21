
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
import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.Dimension;
import java.awt.Toolkit;
import org.dvb.ui.DVBBufferedImage;

import org.bluray.ui.SyncFrameAccurateAnimation;
import org.bluray.ui.AnimationParameters;

import com.hdcookbook.gunbunny.util.Debug;
import com.hdcookbook.gunbunny.util.ImageUtil;

/**
 * An Animator using the sync frame accurate animation drawing model.  We
 * use SFAA in what will probably be the most common mode:  with one
 * buffer.  This basically turns it into a double buffer that's
 * synchronized with the video pipeline.
 *
 *   @author     Bill Foote (http://jovial.com)
 **/

public class SFAAAnimator extends Animator {

    private Rectangle position;
    private boolean firstFrame;
    private Container container;
    private SyncFrameAccurateAnimation sfaa;
    private int startFrame;
    private int framesDropped;
    private boolean destroyed = false;

    public SFAAAnimator() {
    }

    public Rectangle getPosition() {
        return position;
    }

    /**
     * Called to initialize an animator to start presenting at the
     * given frame.
     **/
    public synchronized void initAtFrame(int frame, Container container, 
                                         Rectangle position) 
    {
        this.container = container;
        this.position = position;
        this.firstFrame = true;
        this.startFrame = frame;
        Dimension sz = new Dimension(position.width, position.height);
        AnimationParameters ap = new AnimationParameters();
        ap.threadPriority = Thread.NORM_PRIORITY - 1;
        SyncFrameAccurateAnimation.setDefaultFrameRate(
                SyncFrameAccurateAnimation.FRAME_RATE_24);
        this.sfaa = SyncFrameAccurateAnimation.getInstance(sz, 1, ap);
        container.add(this.sfaa);
        this.sfaa.setLocation(position.x, position.y);
        this.sfaa.setVisible(true);
        this.sfaa.start();
    }

    public void destroy() {
        synchronized(this) {
            destroyed = true;
        }
        container.remove(sfaa);
        sfaa.stop();
        sfaa.destroy();
    }

    /**
     * Get a buffer for double-buffered drawing.  If one is not needed
     * for this style of animation, return null.
     **/
    public synchronized DVBBufferedImage getDoubleBuffer(int width, int height) 
    {
        return null;
    }

    /**
     * Get the graphics for drawing into the buffer returned by the last
     * call to getDoubleBuffer().  If one is not
     * needed for this style of animation, return null.  The drawing
     * mode of the graphics will be set to AlphaComposite.Src.
     **/
    public Graphics2D getDoubleBufferGraphics() {
        return null;
    }

    /**
     * Return true if this animator needs the sprites to erase themselves.
     **/
    public boolean needsErase() {
        return true;
    }

    /**
     * Called by the main loop once per frame.
     **/
    public void animateGame(int frame, Game game) throws InterruptedException {
        synchronized(this) {
            if (destroyed) {
                return;
            }
        }
        Graphics2D g = sfaa.startDrawing(frame - startFrame);
        if (g == null) {
            framesDropped++;
        } else {
            if (firstFrame) {
                g.setColor(ImageUtil.colorTransparent);
                g.fillRect(0, 0, position.width, position.height);
            }
            synchronized(game) {
                game.advanceToFrame(frame);
                game.paintFrame(g, firstFrame, this);
            }
            firstFrame = false;
            synchronized(this) {
                if (destroyed) {
                    return;
                }
            }
            sfaa.finishDrawing(frame - startFrame);
        }
        if (Debug.LEVEL > 0 && frame % 100 == 0) {
            Debug.println("Frame " + (frame - startFrame) + ", " 
                            + framesDropped + " frames dropped.");
        }
    }
}
