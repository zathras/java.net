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

import com.hdcookbook.grin.Director;
import com.hdcookbook.grin.Segment;
import com.hdcookbook.grin.features.Text;
import com.hdcookbook.grin.media.Playlist;
import com.hdcookbook.grin.media.PlayerWrangler;
import com.hdcookbook.grin.util.Debug;
import com.hdcookbook.grinxlet.GrinXlet;


public class MyDirector extends Director {

    public Text fResults;
    public Text fTrim;
    private Segment sVideoDone;
    private int count = 0;

    public static int numBuffers = -1;
    public static long trim;            // nanosecondsQ
    public static int xOffset = 0;
    public static int yOffset = 0;
    public static int xScaleOffset = 0;
    public static int yScaleOffset = 0;
    public static boolean offsetScale = false;  // else offset position
    private static MyDirector theDirector = null;

    public MyDirector() {
        theDirector = this;
    }

    public void initialize() {
        PlayerWrangler.getInstance().initialize(
                    GrinXlet.getInstance().getAnimationEngine());
        fResults = (Text) getFeature("F:Results");
        fTrim = (Text) getFeature("F:Text.Trim");
        sVideoDone = getSegment("S:VideoDone");
    }

    public void setNumBuffers(int num) {
        numBuffers = num;
    }

    public void adjustTrim(int adjustMS) {
        trim += adjustMS * 1000000L;
        String s = "SFAA Trim value:  " + (trim / 1000000L) + " ms";
        fTrim.setText(new String[] { s });
    }

    /**
     * {@inheritDoc}
     **/
    public void notifyDestroyed() {
        PlayerWrangler.getInstance().destroy();
        SFAADirector.stopSFAA();
    }

    public void toggleOffset() {
        if (Debug.LEVEL > 0) {
            offsetScale = !offsetScale;
            Debug.println("Now adjusting " + (offsetScale ? "scale." : "position."));
            SFAADirector.printTimeOffset();
        }
    }

    public void adjustOffset(int dx, int dy) {
        if (Debug.LEVEL > 0) {
            if (offsetScale) {
                xScaleOffset += dx;
                yScaleOffset += dy;
                Debug.println("Scale offset:  " + xScaleOffset + ", " + yScaleOffset);
            } else {
                xOffset += dx;
                yOffset += dy;
                Debug.println("Position offset:  " + xOffset + ", " + yOffset);
            }
            SFAADirector.printTimeOffset();
        }
    }

    public static void finishPlayingVideo() {
        MyDirector d = theDirector;
        if (d == null) {
            return;
        }
        d.getShow().activateSegment(d.sVideoDone);
    }

}
