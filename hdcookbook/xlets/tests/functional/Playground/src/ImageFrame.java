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


/** 
 * An image_frame extension feature, that puts a frame around a fixed_image.
 * This is a contrived extension feature that was created to test parsing
 * a forward reference to a feature from an extension parser.  The RFE
 * for this was number 130.
 **/

import com.hdcookbook.grin.Feature;
import com.hdcookbook.grin.Node;
import com.hdcookbook.grin.Show;
import com.hdcookbook.grin.animator.DrawRecord;
import com.hdcookbook.grin.animator.RenderContext;
import com.hdcookbook.grin.commands.Command;
import com.hdcookbook.grin.features.FixedImage;
import com.hdcookbook.grin.features.Box;
import com.hdcookbook.grin.io.binary.GrinDataInputStream;
import com.hdcookbook.grin.util.Debug;

import java.awt.Graphics2D;
import java.awt.Color;
import java.awt.Rectangle;
import java.util.HashMap;

import java.io.IOException;

public class ImageFrame extends Box implements Node {

    protected FixedImage fixedImage;

    public ImageFrame(Show show) {
        super(show);
        this.width = Integer.MIN_VALUE;
        this.fillColor = null;
    }

    /**
     * We un-implement Box's version of createClone()
     **/
    protected Feature createClone(HashMap clones) {
        throw new UnsupportedOperationException(getClass().getName()
                                                    + ".createClone()");
    }

    /**
     * We un-implement Box's version of initializeClone()
     **/
    protected void initializeClone(Feature original, HashMap clones) {
    }

    /**
     * {@inheritDoc}
     **/
    public int getX() {
        return fixedImage.getX();
    }

    /**
     * {@inheritDoc}
     **/
    public int getY() {
        return fixedImage.getY();
    }

    /**
     * {@inheritDoc}
     **/
    public void nextFrame() {
        // We don't animate, so there's nothing to update
    }

    /**
     * {@inheritDoc}
     **/
    public void addDisplayAreas(RenderContext context) {
        //
        // This is a bit tricky.  Whenever this is called, we know the
        // scene graph is stable.  Namely, our fixedImage is stable, so
        // we grab its bounds, and re-size ourselves (if needed) to still
        // fit around the image.
        //
        // We just inherit paintFrame() from box, but since we modify
        // its instance variables to fit the box around us, that works.
        Rectangle bounds = fixedImage.getMutablePlacement();
        int bX = bounds.x - outlineWidthX;
        int bY = bounds.y - outlineWidthY;
        int bH = bounds.height + 2*outlineWidthX;
        int bW = bounds.width + 2*outlineWidthY;
        if (x != bX || y != bY || height != bH || width != bW) {
            x = bX;
            y = bY;
            height = bH;
            width = bW;
            markDisplayAreasChanged();
        }
        super.addDisplayAreas(context);
    }


    /**
     * {@inheritDoc}
     **/
    public void readInstanceData(GrinDataInputStream in, int length)
            throws IOException
    {
        in.readSuperClassData(this);
        outlineWidthX = in.readInt();
        outlineWidthY = in.readInt();
        outlineColor = in.readColor();
        fixedImage = (FixedImage) in.readFeatureReference();
    }

}
