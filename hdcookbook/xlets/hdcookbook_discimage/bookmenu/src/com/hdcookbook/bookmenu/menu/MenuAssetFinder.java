
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

package com.hdcookbook.bookmenu.menu;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Color;
import java.awt.Font;
import java.awt.Image;

import com.hdcookbook.grin.util.Debug;
import com.hdcookbook.grin.util.AssetFinder;

import org.dvb.ui.FontFactory;
import org.dvb.ui.DVBBufferedImage;

/**
 * This class lets us hook in "under" GRIN, to tell GRIN where
 * to find assets, and do other xlet-specific things.
 *
 *   @author     Bill Foote (http://jovial.com)
 **/
public class MenuAssetFinder extends AssetFinder {

    private FontFactory factory;
    private MenuXlet xlet;

    public MenuAssetFinder(MenuXlet xlet) {
        this.xlet = xlet;
    }

    protected void abortHelper() {
        if (Debug.LEVEL > 0) {
            Debug.println();
            Debug.println("******************************");
            Debug.println("*     ABORTING DISC PLAY     *");
            Debug.println("******************************");
            Debug.println();
        }
        try {
            xlet.destroyXlet(true);
        } catch (Throwable ignored) {
            if (Debug.LEVEL > 0) {
                Debug.printStackTrace(ignored);
            }
        }
        xlet.navigator.startVideoAt(null);
    }

    protected Font getFontHelper(String fontName, int style, int size) {
        try {
            synchronized(this) {
                if (factory == null) {
                    factory = new FontFactory();
                }
            }
            return factory.createFont(fontName, style, size);
        } catch (Exception ex) {
            if (Debug.LEVEL > 0) {
                Debug.printStackTrace(ex);
                Debug.println("***  Font " + fontName + " not found.  ***");
            }
        }
        return null;
    }

    /**
     * {@inheritDoc}
     **/
    protected Image createCompatibleImageBufferHelper(Component c,
                                                      int width, int height) 
    {
        return new DVBBufferedImage(width, height);
    }

    /**
     * {@inheritDoc}
     **/
    protected Graphics2D createGraphicsFromImageBufferHelper(Image buffer) {
        Object g = ((DVBBufferedImage) buffer).createGraphics();
        return (Graphics2D) g;
    }

    /**
     * {@inheritDoc}
     **/
    protected void destroyImageBufferHelper(Image buffer) {
            ((DVBBufferedImage) buffer).dispose();
    }

}
