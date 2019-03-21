
/*  
 * Copyright (c) 2008, Sun Microsystems, Inc.
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

package com.hdcookbook.grinxlet;

import com.hdcookbook.grin.animator.ClockBasedEngine;
import com.hdcookbook.grin.animator.DirectDrawEngine;
import com.hdcookbook.grin.util.Debug;
import java.awt.AlphaComposite;
import java.awt.Container;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.util.LinkedList;
import java.util.Iterator;

/**
 * A double-buffered animation engine that uses direct draw, and
 * can be set in "debug draw" mode.
 *
 * @see com.hdcookbook.grin.animator.DirectDrawEngine
 **/
public class DebugDirectDrawEngine extends DirectDrawEngine {


    private Graphics2D containerG;
    private boolean debugDraw = false;
    private static Color red = new Color(255, 0, 0, 127);
    private static Color green = new Color(0, 255, 0, 127);
    private int fps = 24000;
    
    /**
     * Create a new DebugDirectDrawEngine.  
     **/
    public DebugDirectDrawEngine() {
    }

    /**
     * {@inheritDoc}
     **/
    public void initContainer(Container container, Rectangle bounds) {
        super.initContainer(container, bounds);
        containerG = (Graphics2D) container.getGraphics();
        if (Debug.ASSERT && containerG == null) {
            Debug.assertFail();  // Maybe container is invisible?
        }
    }

    /**
     * Tell us if we should step through each frame showing
     * erase, paint areas, then painted result
     **/
    public void setDebugDraw(boolean debugDraw) {
        this.debugDraw = debugDraw;
    }

    /**
     * {@inheritDoc}
     **/
    protected void callPaintTargets() throws InterruptedException {
        if (debugDraw) {
                // Paint the area to be erased red, and wait
            containerG.setColor(red);
            containerG.setComposite(AlphaComposite.SrcOver);
            for (int i = 0; i < getNumEraseTargets(); i++) {
                Rectangle a = getEraseTargets()[i];
                if (!a.isEmpty()) {
                    containerG.fillRect(a.x, a.y, a.width, a.height);
                }
            }
            Toolkit.getDefaultToolkit().sync();
            sleepQuarterFrame();

                // Paint the area to be drawn green, and wait
            containerG.setColor(green);
            for (int i = 0; i < getNumDrawTargets(); i++) {
                Rectangle a = getDrawTargets()[i];
                containerG.fillRect(a.x, a.y, a.width, a.height);
            }
            Toolkit.getDefaultToolkit().sync();
            sleepQuarterFrame();
            containerG.setComposite(AlphaComposite.Src);
        }
        super.callPaintTargets();       // This covers the red and green
    }

    /**
     * {@inheritDoc}
     **/
    public void setFps(int fps) {
        this.fps = fps;
        super.setFps(fps);
    }

    private void sleepQuarterFrame() throws InterruptedException {
        long ms = (1000000 / fps) / 4;
        Thread.sleep(ms);
    }
}
