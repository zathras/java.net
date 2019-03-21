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
 * An Arc extension feature.  
 **/

import com.hdcookbook.grin.Feature;
import com.hdcookbook.grin.Node;
import com.hdcookbook.grin.Show;
import com.hdcookbook.grin.animator.DrawRecord;
import com.hdcookbook.grin.animator.RenderContext;
import com.hdcookbook.grin.commands.Command;
import com.hdcookbook.grin.io.binary.GrinDataInputStream;
import com.hdcookbook.grin.util.Debug;

import java.awt.Graphics2D;
import java.awt.Color;

import java.io.IOException;

public class Arc extends Feature implements Node {

    protected int x;
    protected int y;
    protected int width;
    protected int height;
    protected int startAngle;
    protected int arcAngle;
    protected Color color;


    private DrawRecord drawRecord = new DrawRecord();

    public Arc(Show show) {
        super(show);
    }

    //
    // We don't implement createClone(), so cloning is not
    // supported.
    //
    // It's easy enough to implement; we didn't here because we don't
    // need to, and because that's a way of demonstrating that you
    // aren't required to implement cloning.

    /**
     * {@inheritDoc}
     **/
    public int getX() {
        return x;
    }

    /**
     * {@inheritDoc}
     **/
    public int getY() {
        return y;
    }

    /**
     * {@inheritDoc}
     **/
    public void initialize() {
        // no initialization needed
    }
    
    /**
     * {@inheritDoc}
     **/
    public void destroy() {
    }

    /**
     * {@inheritDoc}
     **/
    public void setActivateMode(boolean mode) {
    }

    /**
     * {@inheritDoc}
     **/
    public int setSetupMode(boolean mode) {
        return 0;
    }

    /**
     * {@inheritDoc}
     **/
    public void doSomeSetup() {
    }

    /**
     * {@inheritDoc}
     **/
    public boolean needsMoreSetup() {
        return false;
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
    public void markDisplayAreasChanged() {
        drawRecord.setChanged();
    }

    /**
     * Set the arc angle, and mark that we need to re-draw ourselves.
     **/
    public void setArcAngle(int arcAngle) {
        this.arcAngle = arcAngle;
        drawRecord.setChanged();
    }
    
    /**
     * Set the start angle, and mark that we need to re-draw ourselves.
     **/
    public void setStartAngle(int startAngle) {
        this.startAngle = startAngle;
        drawRecord.setChanged();
    }

    public void setX(int x) {
         this.x = x;
    }

    public void setY(int y) {
         this.y = y;
    }

    /**
     * {@inheritDoc}
     **/
    public void addDisplayAreas(RenderContext context) {
        drawRecord.setArea(x, y, width, height);
            // We don't animate, so we don't have to think about setting
            // drawRecord.setChanged(), or anything like that.
            //
            // We might be overstating the draw area, depending on the
            // values of startAngle and arcAngle.  It's OK to overstate
            // the draw area.
        context.addArea(drawRecord);
    }

    /**
     * {@inheritDoc}
     **/
    public void paintFrame(Graphics2D gr) {
        gr.setColor(color);
        gr.fillArc(x, y, width-1, height-1, startAngle, arcAngle);
    }


    /**
     * {@inheritDoc}
     **/
    public void readInstanceData(GrinDataInputStream in, int length)
            throws IOException
    {
        in.readSuperClassData(this);
        color = in.readColor();
        x = in.readInt();
        y = in.readInt();
        width = in.readInt();
        height = in.readInt();
        startAngle = in.readInt();
        arcAngle = in.readInt();
    }

}
