
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

import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import org.dvb.ui.DVBBufferedImage;

/**
 * An Animator is something that repeatedly repaints a set of sprites in
 * order to animate a game.
 *
 *   @author     Bill Foote (http://jovial.com)
 **/

public abstract class Animator {

    private Rectangle scratchRectangle = new Rectangle();

    /**
     * Get the x,y position and the width and height we're animating over
     **/
    public abstract Rectangle getPosition();
    /**
     * Called to initialize an animator to start presenting at the
     * given frame.
     **/
    public abstract void initAtFrame(int frame, Container container,
                                     Rectangle position);

    public abstract void destroy();

    /**
     * Get a buffer for double-buffered drawing.  If one is not needed
     * for this style of animation, return null.
     **/
    public abstract DVBBufferedImage getDoubleBuffer(int width, int height);

    /**
     * Get the graphics for drawing into the buffer returned by the last
     * call to getDoubleBuffer().  If one is not
     * mode of the graphics will be set to AlphaComposite.Src.
     **/
    public abstract Graphics2D getDoubleBufferGraphics();

    /**
     * Give a rectangle that can be used by our client to scribble on.
     * Maintaining the instance here make it easier to keep reusing
     * the same rectangle, rather than generating heap traffic.
     **/
    public Rectangle getScratchRectangle() {
        return scratchRectangle;
    }

    /**
     * Return true if this animator needs the sprites to erase themselves.
     **/
    public abstract boolean needsErase();


    /**
     * Called by the main loop once per frame.
     **/
    public abstract void animateGame(int frame, Game game) 
                throws InterruptedException;
}
