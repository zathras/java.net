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

import java.awt.Rectangle;
import java.io.IOException;
import javax.tv.xlet.Xlet;
import javax.tv.xlet.XletContext;
import javax.tv.graphics.TVContainer;

import java.awt.Container;
import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.Image;

import com.hdcookbook.grin.Show;
import com.hdcookbook.grin.Director;
import com.hdcookbook.grin.animator.AnimationClient; 
import com.hdcookbook.grin.animator.AnimationContext;
import com.hdcookbook.grin.animator.DirectDrawEngine;
import com.hdcookbook.grin.io.binary.GrinBinaryReader;
import com.hdcookbook.grin.util.AssetFinder;
import com.hdcookbook.grin.util.Debug;

import org.dvb.event.EventManager;
import org.dvb.event.UserEvent;
import org.dvb.event.UserEventListener;
import org.dvb.event.UserEventRepository;
import org.bluray.ui.event.HRcEvent;
import org.dvb.ui.DVBBufferedImage;

/** 
 * The main class for the font strip demo xlet
 */

public class Main implements Xlet, AnimationContext, UserEventListener {
        
        public Show show;
        Container rootContainer;
        DirectDrawEngine animationEngine;
        XletContext context;
        
        public void initXlet(XletContext context) {
                
           this.context = context;
           
           rootContainer = TVContainer.getRootContainer(context);                       
           rootContainer.setSize(1920, 1080);
           
           animationEngine = new DirectDrawEngine();
           animationEngine.setFps(24000);
           animationEngine.initialize(this);
           
        }
        
        public void startXlet() {
           rootContainer.setVisible(true);
           animationEngine.start();        
        }
        
        public void pauseXlet() {
           rootContainer.setVisible(false);
           animationEngine.pause();
        }
        
        public void destroyXlet(boolean unconditional) {
           rootContainer = null;
           animationEngine.destroy();
           EventManager.getInstance().removeUserEventListener(this);          
        }
        
        public void animationInitialize() throws InterruptedException {
           
           try {
                // Set up AssetFinder so we use DVBBufferedImage.
                // See http://wiki.java.net/bin/view/Mobileandembedded/BDJImageMemoryManagement
                AssetFinder.setHelper(new AssetFinder() {
                    protected Image createCompatibleImageBufferHelper
                                        (Component c, int width, int height) 
                    {
                        return new DVBBufferedImage(width, height);
                    }
                    protected Graphics2D createGraphicsFromImageBufferHelper
                                        (Image buffer) 
                    {
                        Object g = ((DVBBufferedImage) buffer).createGraphics();
                        return (Graphics2D) g;
                    }
                    protected void destroyImageBufferHelper(Image buffer) {
                        ((DVBBufferedImage) buffer).dispose();
                    }
                });

               AssetFinder.setSearchPath(new String[]{""}, null);      
               GrinBinaryReader reader = 
                       new GrinBinaryReader(AssetFinder.getURL("show-lisa.grin").openStream());
               show = new Show(new Director());
               reader.readShow(show);
               
           } catch (IOException e) {
                if (Debug.LEVEL > 0) {
                    Debug.printStackTrace(e);
                    Debug.println("Error in reading the show file");
                }
                throw new InterruptedException();
           }
           
           animationEngine.checkDestroy();
           animationEngine.initClients(new AnimationClient[]{show});
           animationEngine.initContainer(rootContainer, new Rectangle(0,0,1920,1080));
           
        } 
        
        public void animationFinishInitialization() {
            show.activateSegment(show.getSegment("S:Initialize"));      
           
            UserEventRepository userEventRepo = new UserEventRepository("x");
            userEventRepo.addAllArrowKeys();
            userEventRepo.addAllColourKeys();
            userEventRepo.addAllNumericKeys();
            userEventRepo.addKey(HRcEvent.VK_ENTER);
            userEventRepo.addKey(HRcEvent.VK_POPUP_MENU);

            EventManager.getInstance().addUserEventListener(this, userEventRepo);          
            rootContainer.requestFocus();          
        }

    /**
     * A remote control event that is coming in via
     * org.dvb.event.UserEventListener
     **/
    public void userEventReceived(UserEvent e) {
        if (e.getType() == HRcEvent.KEY_PRESSED) {
            show.handleKeyPressed(e.getCode());
        }
    }   
}
