/*  
 * Copyright (c) 2009, Sun Microsystems, Inc.
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
 * The compile-time SE version of BouncingArc.  A single SEBouncingArc feature
 * is compiled into the following structure:
 * <pre>
 *     SEGroup
 *         SETranslatorModel (with pre-computed coordinates for bouncing)
 *         SETranslator
 *             SEBouncingArc (which isa SEArc)
 * </pre>
 **/

import com.hdcookbook.grin.Feature;
import com.hdcookbook.grin.SENode;
import com.hdcookbook.grin.SEShow;
import com.hdcookbook.grin.SEShowVisitor;
import com.hdcookbook.grin.Show;
import com.hdcookbook.grin.animator.DrawRecord;
import com.hdcookbook.grin.animator.RenderContext;
import com.hdcookbook.grin.commands.Command;
import com.hdcookbook.grin.features.SEGroup;
import com.hdcookbook.grin.features.SETranslator;
import com.hdcookbook.grin.features.SETranslatorModel;
import com.hdcookbook.grin.io.ShowBuilder;
import com.hdcookbook.grin.io.binary.GrinDataOutputStream;
import com.hdcookbook.grin.util.Debug;

import java.awt.Graphics2D;
import java.awt.Color;

import java.io.IOException;

public class SEBouncingArc extends SEArc {

    private int bounceHeight;
    private int bouncePeriod;

    public SEBouncingArc(
                Show show, String name, int x, int y, int width, int height,
                int startAngle, int arcAngle, Color color, int bounceHeight,
                int bouncePeriod)
    {
        super(show, name, x, y, width, height, startAngle, arcAngle, color);
        this.bounceHeight = bounceHeight;
        this.bouncePeriod = bouncePeriod;
    }

    /**
     * {@inheritDoc}
     **/
    public void postProcess(ShowBuilder builder) throws IOException {
        super.postProcess(builder);
        int[] frames = new int[bouncePeriod];
        int[][] values = new int[2][];
        int[] yValues = new int[bouncePeriod];
        int[] xValues = new int[bouncePeriod];
        values[SETranslator.Y_FIELD] = yValues;
        values[SETranslator.X_FIELD] = xValues;
        for (int i = 0; i < bouncePeriod; i++) {
            double period = bouncePeriod;
            double x = i - (period / 2.0);
            x = x * 2.0 / period;       // x between -1 and 1
            x = x * x;
            frames[i] = i;
            xValues[i] = 0;  
                // bulder.makeTranslatorModel optimizes this array away
            yValues[i] = (int) (0.5 + x * bounceHeight);
                // We could cut down on the number of frames by using the
                // runtime linear interpolation built into InterpolatedModel
        }
        SETranslatorModel model 
            = builder.makeTranslatorModel(null, frames, values, true, 0, 1,
                                          new Command[0]);
        SETranslator translator = new SETranslator(builder.getShow(), null);
        translator.setupModelIsRelative(false);
        translator.setup(model, this);
        SEGroup group = new SEGroup(builder.getShow());
        group.setup(new Feature[] { model, translator });
        builder.injectParent(group, this);
        builder.addSyntheticFeature(model);
        builder.addSyntheticFeature(translator);
    }

    /**
     * {@inheritDoc}
     **/
    public String toString() {
        if (name == null) {
            return "Playground:bouncing_arc @" + Integer.toHexString(hashCode());
        } else {
            return "Playground:bouncing_arc " + name;
        }
    }
}
