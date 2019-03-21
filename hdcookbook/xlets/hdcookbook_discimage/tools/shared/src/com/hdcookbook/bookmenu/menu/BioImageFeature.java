
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

import com.hdcookbook.grin.io.binary.GrinDataInputStream;
import java.awt.Graphics2D;
import java.awt.Image;

import com.hdcookbook.grin.Node;
import com.hdcookbook.grin.Show;
import com.hdcookbook.grin.animator.DrawRecord;
import com.hdcookbook.grin.animator.RenderContext;
import com.hdcookbook.grin.features.Modifier;
import java.io.IOException;


/**
 * This is an extension GRIN feature introduced by our xlet.
 * It's designed to be the parent of a fixed image feature.  The
 * xlet can set a new image for us; when this is done it replaces
 * our child feature.
 * <p>
 * It might seem a little strange at first to do it this way, rather than
 * just have one feature and replace the image.  There's a reason, though.
 * For debugging, it's nice to have something reasonable happen in the
 * big JDK tool for displaying show files.  That tool doesn't know about
 * extension features, but when it encounters a modifier it can just show
 * the underlying feature instead.
 *
 *   @author     Bill Foote (http://jovial.com)
 **/
public class BioImageFeature extends Modifier implements Node {

    private Image replacementImage = null;
    private int width;
    private int height;
    private DrawRecord drawRecord = new DrawRecord();
    private boolean changed = true;

    /**
     * Create a new instance of this feature.  This is called from
     * our xlet's extension parser.
     **/
    public BioImageFeature(Show show) {
        super(show);
    }

    // called with the Show lock held, by the xlet's BioUpdate
    void setImage(Image newImage) {
        if (replacementImage != null) {
            replacementImage.flush();
        }
        replacementImage = newImage;
        width = newImage.getWidth(null);
        height = newImage.getHeight(null);
        changed = true;
    }

    protected void setActivateMode(boolean mode) {
        super.setActivateMode(mode);
        if (mode) {
            changed = true;
        }
    }


    /**
     * Called by the GRIN framework with the show lock held to
     * paint our representation
     *
     * @param g         The place we paint to
     **/
    public void paintFrame(Graphics2D g) {
        // called with the Show lock held
        if (replacementImage == null) {
            super.paintFrame(g);
        } else {
            g.drawImage(replacementImage, getX(), getY(), null);
        }
    }

    /**
     * {@inheritDoc}
     **/
    public void markDisplayAreasChanged() {
        super.markDisplayAreasChanged();
        drawRecord.setChanged();
    }


    /**
     * Called by the GRIN framework to figure out the extent of the
     * area we paint to.  This is called with the show lock held.
     **/
    public void  addDisplayAreas(RenderContext context) {
        if (replacementImage == null) {
            super.addDisplayAreas(context);
        } else {
            int x = getX();
            int y = getY();
            drawRecord.setArea(x, y, width, height);
            if (changed) {
                drawRecord.setChanged();
                changed = false;
            }
            context.addArea(drawRecord);
        }
    }

    public void readInstanceData(GrinDataInputStream in, int length) 
            throws IOException {
        
        in.readSuperClassData(this);
        
        // Nothing specific to this class to read...
    }
}
