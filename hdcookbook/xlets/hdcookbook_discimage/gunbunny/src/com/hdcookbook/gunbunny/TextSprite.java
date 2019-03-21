
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
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Image;
import org.dvb.ui.DVBBufferedImage;

import com.hdcookbook.gunbunny.util.ImageUtil;

/**
 * A text sprite is a sprite that holds some text.
 * This class does not handle overlapping sprites.
 *
 *   @author     Bill Foote (http://jovial.com)
 **/

public class TextSprite extends Sprite {

    private String text;
    private String lastText;
    private Font font;
    private Component comp;
    private Color color;

    private int lastFrame = -1;
    private Rectangle pos = new Rectangle();
    private Rectangle lastPos = new Rectangle();
    private int ascent;
    private int descent;

    /** 
     * Create a TextSprite.
     **/
    public TextSprite(String text, Font font, Color color,
                      Component comp, int x, int y) {
        this.text = text;
        this.font = font;
        this.color = color;
        this.comp = comp;
        calculateMetrics(pos, x, y);
    }

    private void calculateMetrics(Rectangle r, int x, int y) {
        r.x = x;
        r.y = y;
        FontMetrics fm = comp.getFontMetrics(font);
        r.width = fm.stringWidth(text);
        ascent = fm.getMaxAscent();
        descent = fm.getMaxDescent();
        r.height = ascent + descent + 1;
    }


    /**
     * Advance the state of the sprite to the given frame.  A sprite
     * may only change position during a call to one of the nextFrame
     * methods.
     **/
    public void nextFrame() {
        lastPos.setBounds(pos);
        lastText = text;
    }

    /**
     * Advance the state of the sprite to the given frame.  A sprite
     * may only change position during a call to one of the nextFrame
     * methods.
     **/
    public void nextFrame(String newText) {
        lastPos.setBounds(pos);
        lastText = text;
        text = newText;
        calculateMetrics(pos, pos.x, pos.y);
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
        boolean moved = lastText !=  text || !pos.equals(lastPos);
        if (moved && animator != null && animator.needsErase()) {
            Rectangle r = animator.getScratchRectangle();
            r.setBounds(pos);
            r.add(lastPos.x, lastPos.y);
            r.add(lastPos.x+lastPos.width, lastPos.y+lastPos.height);
            DVBBufferedImage buf = animator.getDoubleBuffer(r.width, r.height);
            if (buf == null) {
                g.setColor(ImageUtil.colorTransparent);
                g.fillRect(r.x, r.y, r.width, r.height);
                g.setColor(color);
                g.setFont(font);
                g.drawString(text, pos.x, pos.y + ascent);
            } else {
                Graphics2D bufG = animator.getDoubleBufferGraphics();
                bufG.setColor(ImageUtil.colorTransparent);
                bufG.fillRect(0, 0, r.width, r.height);
                bufG.setColor(color);
                bufG.setFont(font);
                bufG.drawString(text,
                                (pos.x <= lastPos.x ? 0 : pos.x - lastPos.x),
                                ascent + (pos.y <= lastPos.y 
                                                ? 0 : pos.y - lastPos.y));
                g.drawImage(buf, r.x, r.y, r.x+r.width, r.y+r.height,
                                   0,   0,     r.width,     r.height, null);
            }
        } else if (moved || paintAll) {
            g.setColor(color);
            g.setFont(font);
            g.drawString(text, pos.x, pos.y + ascent);
        }
    }
}
