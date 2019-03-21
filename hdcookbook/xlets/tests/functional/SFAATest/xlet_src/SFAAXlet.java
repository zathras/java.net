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

import com.hdcookbook.grinxlet.GrinXlet;
import com.hdcookbook.grin.animator.AnimationEngine;
import com.hdcookbook.grin.util.Debug;

import java.awt.Container;
import java.awt.Dimension;
import javax.tv.graphics.TVContainer;


import org.davic.resources.ResourceClient;
import org.davic.resources.ResourceProxy;

import org.havi.ui.HGraphicsConfiguration;
import org.havi.ui.HGraphicsDevice;
import org.havi.ui.HScreen;


/**
 * An xlet subclass that sets the screen to QHD (960x540).
 * <p>
 * Note that the debug screen isn't adjusted, so 3/4 of it is just
 * cut off, but that's not too damaging:  You can still connect to
 * port 6000 for the debug log.  It wouldn't be all that hard to have
 * a QHD version of the debug screen, though; filed as P4 RFE 149.
 **/
public class SFAAXlet extends GrinXlet implements ResourceClient {

    private HGraphicsDevice graphicsDevice;
    private Container root;
    /**
     * {@inheritDoc}
     * <p>
     * This override of this method sets the HGraphicsDevice to QHD,
     * and sizes the component to QHD.
     **/
    protected Container getRootContainer() {
        graphicsDevice = HScreen.getDefaultHScreen()
                                .getDefaultHGraphicsDevice();
        HGraphicsConfiguration[] config = graphicsDevice.getConfigurations();

        // QHD_960_540
        // HD_1920_1080
        Dimension configWanted = new Dimension(960, 540);
        HGraphicsConfiguration found = null;
        for (int i = 0; found == null && i < config.length; i++) {
            Dimension d = config[i].getPixelResolution();
            if (d.equals(configWanted)) {
                found = config[i];
            } 
        }

        if (found != null) {
            try {
                graphicsDevice.reserveDevice(this);
                graphicsDevice.setGraphicsConfiguration(found);
                if (Debug.LEVEL > 0) {
                    Debug.println("Set screen size to " + configWanted);
                }
            } catch (Exception e) {
                if (Debug.LEVEL > 0) {
                    Debug.printStackTrace(e);
                }
            } finally {
                graphicsDevice.releaseDevice();
            }
        }

        root = TVContainer.getRootContainer(xletContext);
        root.setSize(960, 540);
        root.setVisible(true);
        if (Debug.LEVEL > 0) {
            Debug.println("Set root container size to " + root.getWidth()
                          + "x" +root.getHeight());
        }

        Container c = new Container();
        root.add(c);
        c.setBounds(0, 0, 960, 540);
        c.setVisible(true);
        return c;
    }


    public Container getSFAAContainer() {
        return root;
    }

    //
    // ResourceClient methods:
    //
    /**
     * {@inheritDoc}
     **/
    public void notifyRelease(ResourceProxy proxy) {
    }

    /**
     * {@inheritDoc}
     **/
    public void release(ResourceProxy proxy) {
    }

    /**
     * {@inheritDoc}
     **/
    public boolean requestRelease(ResourceProxy proxy, Object requestData) {
        return false;   
        // We release the graphicsDevice right after we get it, so we should
        // never get this call, but if we do then we're in the middle of using
        // it.
    }

    public void destroyXlet(boolean unconditional) {
    }
}
