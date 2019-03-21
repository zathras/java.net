
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
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Image;
import org.dvb.ui.DVBBufferedImage;

import com.hdcookbook.gunbunny.util.ImageUtil;
import com.hdcookbook.gunbunny.util.Debug;

/**
 * An image sprite is a sprite that holds an image, and can have an x and
 * y velocity, and can be visible or invisible.  This class does not 
 * handle overlapping sprites.
 *
 *   @author     Bill Foote (http://jovial.com)
 **/

public class ImageSprite extends Sprite {

    private Image image;

    private int lastFrame = -1;
    private int x;
    private int y;
    private int lastX;
    private int lastY;
    private boolean imageOn;
    private boolean lastImageOn;
    private boolean imageReplaced = false;
    private int imageW;
    private int imageH;

    /** 
     * Create an ImageSprite from an already-prepared image.  The position
     * must be initialized using one of the initXXX calls.
     **/
    public ImageSprite(Image image) {
        this.image = image;
        imageW = image.getWidth(null);
        imageH = image.getHeight(null);
    }

    public int getWidth() {
        return imageW;
    }

    public int getHeight() {
        return imageH;
    }

    public void getBounds(Rectangle r) {
        r.x = x;
        r.y = y;
        r.width = imageW;
        r.height = imageH;
    }

    /**
     * Initialize our position so that we're centered over the provided
     * rectangle.
     **/
    public void initPositionCentered(int xs, int ys, int width, int height) {
        x = xs + (width - imageW) / 2;
        y = ys + (height - imageH) / 2;
        this.imageOn = true;
    }

    /**
     * Initialize our position with the given coordinates for our
     * upper left hand corner.
     **/
    public void initPosition(int x, int y) {
        this.x = x;
        this.y = y;
        this.imageOn = true;
    }

    /**
     * Initialize our "position" as invisible.
     **/
    public void initImageOff() {
        this.imageOn = false;
        this.x = 0;
        this.y = 0;
    }

    /**
     * Advance the state of the sprite to the given frame.  A sprite
     * may only change position during a call to one of the nextFrame
     * methods.
     **/
    public void nextFrame() {
        lastX = x;
        lastY = y;
        lastImageOn = imageOn;
    }

    /**
     * Advance the state of the sprite to the given frame.  A sprite
     * may only change position during a call to one of the nextFrame
     * methods.
     **/
    public void nextFrame(int newX, int newY) {
        lastX = x;
        lastY = y;
        x = newX;
        y = newY;
        lastImageOn = imageOn;
        imageOn = true;
    }

    /**
     * Advance the state of the sprite to the given frame.  A sprite
     * may only change position during a call to one of the nextFrame
     * methods.
     **/
    public void nextFrame(int newX, int newY, Image newImage) {
        lastX = x;
        lastY = y;
        x = newX;
        y = newY;
        lastImageOn = imageOn;
        imageOn = true;
        if (newImage == image) {
            return;
        }
        if (Debug.ASSERT) {
            if (newImage.getWidth(null) != imageW
                || newImage.getHeight(null) != imageH)
            {
                Debug.assertFail();
            }
        }
        image = newImage;
        imageReplaced = true;
    }

    /**
     * Advance the state of the sprite to the next frame.
     * The next frame will not show the image, but the old postion
     * will be erased if needed.
     **/
    public void nextFrameOff() {
        lastX = x;
        lastY = y;
        lastImageOn = imageOn;
        imageOn = false;
    }

    /**
     * Advance the state of the sprite to the next frame, with a new
     * image.
     **/
    public void nextFrame(Image newImage) {
        lastX = x;
        lastY = y;
        lastImageOn = imageOn;
        imageOn = true;
        if (newImage == image) {
            return;
        }
        if (Debug.ASSERT) {
            if (newImage.getWidth(null) != imageW
                || newImage.getHeight(null) != imageH)
            {
                Debug.assertFail();
            }
        }
        image = newImage;
        imageReplaced = true;
    }


    /**
     * Called to paint this sprite
     *
     * @param g         The place to paint the sprite
     * @param paintAll  true if we should paint everything, even if it's
     *                  identical to what was painted in the last frame
     * @param animator  The animator that's animating us, or null if
     *                  this is just a call from repaint.
     **/
    public void paintFrame(Graphics2D g, boolean paintAll, Animator animator) {
        boolean moved = lastX != x || lastY != y || imageOn != lastImageOn;
        if (imageReplaced) {
            moved = true;
            imageReplaced = false;
        }
        if ((!imageOn) && (!lastImageOn)) {
            return;
        } else if (moved && animator != null && animator.needsErase()) {
            Rectangle r = animator.getScratchRectangle();
            r.width = imageW;
            r.height = imageH;
            if (imageOn) {
                r.x = x;
                r.y = y;
                if (lastImageOn) {
                    r.add(lastX, lastY);
                    r.add(lastX + imageW, lastY + imageH);
                }
            } else {
                r.x = lastX;
                r.y = lastY;
            }
            DVBBufferedImage buf = animator.getDoubleBuffer(r.width, r.height);
            Graphics2D bufG;
            if (buf == null) {
                g.setColor(ImageUtil.colorTransparent);
                g.fillRect(r.x, r.y, r.width, r.height);
                if (!imageOn) {
                    g.drawImage(image, x, y, null);
                }
            } else {
                bufG = animator.getDoubleBufferGraphics();
                bufG.setColor(ImageUtil.colorTransparent);
                bufG.fillRect(0, 0, r.width, r.height);
                    // That fillRect is somewhat inefficient:  Instead,
                    // we could do two fillRect calls of just the part the
                    // image won't be on.
                if (imageOn) {
                    bufG.drawImage(image, (x <= lastX ? 0 : x - lastX),
                                          (y <= lastY ? 0 : y - lastY), null);
                }
                g.drawImage(buf, r.x, r.y, r.x+r.width, r.y+r.height,
                                  0,  0, r.width,    r.height, null);
            }
        } else if (moved || paintAll) {
            if (imageOn) {
                g.drawImage(image, x, y, null);
            }
        }
    }
}
