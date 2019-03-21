
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
import java.awt.Toolkit;
import org.dvb.ui.DVBBufferedImage;

import com.hdcookbook.gunbunny.util.Debug;
import com.hdcookbook.gunbunny.util.ImageUtil;

/**
 * An Animator using the direct draw drawing model
 *
 *   @author     Bill Foote (http://jovial.com)
 **/

public class DirectDrawAnimator extends Animator {

    private Rectangle position;
    private boolean firstFrame;
    private Container component;
    private DVBBufferedImage buffer;
    private Graphics2D bufferG;
    private Graphics2D componentG;
    private long startTime;
    private int startFrame;
    private int framesDropped;

    public DirectDrawAnimator() {
    }

    public Rectangle getPosition() {
        return position;
    }

    /**
     * Called to initialize an animator to start presenting at the
     * given frame.
     **/
    public synchronized void initAtFrame(int frame, Container component, 
                                         Rectangle position) 
    {
        this.component = component;
        this.position = position;
        this.firstFrame = true;
        createNewBuffer(256, 256);
                // Pick a big enough default size so that growth
                // is unlikley
        componentG = (Graphics2D) component.getGraphics();
        componentG.translate(position.x, position.y);
        componentG.setClip(0, 0, position.width, position.height);
        componentG.setComposite(AlphaComposite.Src);
        this.startFrame = frame;
        this.startTime = System.currentTimeMillis();
    }

    private synchronized void createNewBuffer(int width, int height) {
        if (buffer != null) {
            buffer.dispose();
        }
        buffer = new DVBBufferedImage(width, height);
        Object g = buffer.createGraphics();
        bufferG = (Graphics2D) g;
        bufferG.setComposite(AlphaComposite.Src);
    }

    /**
     * See superclass definition.
     **/
    public void destroy() {
        if (buffer != null) {
            buffer.dispose();
        }
    }

    /**
     * Get a buffer for double-buffered drawing.  If one is not needed
     * for this style of animation, return null.
     **/
    public synchronized DVBBufferedImage getDoubleBuffer(int width, int height) {
        if (width > buffer.getWidth() || height > buffer.getHeight()) {
            if (buffer.getWidth() > width) {
                width = buffer.getWidth();
            }
            if (buffer.getHeight() > height) {
                height = buffer.getHeight();
            }
            createNewBuffer(width, height);
        }
        return buffer;
    }

    /**
     * Get the graphics for drawing into the buffer returned by the last
     * call to getDoubleBuffer().  If one is not
     * needed for this style of animation, return null.  The drawing
     * mode of the graphics will be set to AlphaComposite.Src.
     **/
    public Graphics2D getDoubleBufferGraphics() {
        return bufferG;
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
        if (Debug.LEVEL > 0 && frame % 100 == 0) {
            Debug.println("Frame " + (frame - startFrame) + ", " 
                            + framesDropped + " frames dropped.");
        }
        long now = System.currentTimeMillis();
        long fTime = ((frame - startFrame) * 1000L) / 24L + startTime;
        if (now < fTime) {      // We're ahead
            Thread.sleep(fTime - now);
        } else {
            long nextF = ((frame + 1 - startFrame) * 1000L) / 24L + startTime;
            if (now >= nextF) {
                framesDropped++;
                return;
            }
        }
        if (firstFrame) {
            componentG.setColor(ImageUtil.colorTransparent);
            componentG.fillRect(0, 0, position.width, position.height);
        }
        synchronized(game) {
            game.advanceToFrame(frame);
            game.paintFrame(componentG, firstFrame, this);
        }
        Toolkit.getDefaultToolkit().sync();
        firstFrame = false;
    }
}
