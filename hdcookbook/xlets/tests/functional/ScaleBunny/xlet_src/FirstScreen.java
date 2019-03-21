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
import com.hdcookbook.grin.util.Debug;

import java.awt.Container;
import java.awt.Component;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Font;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import javax.tv.graphics.TVContainer;

import org.dvb.event.EventManager;
import org.dvb.event.UserEvent;
import org.dvb.event.UserEventListener;
import org.dvb.event.UserEventRepository;

import org.havi.ui.event.HRcCapabilities;
import org.bluray.ui.event.HRcEvent;


/**
 * The first screen of ScaleXlet.  It displays a prompt, and responds to
 * numeric keypresses.
 **/
public class FirstScreen extends Component implements UserEventListener {

    private ScaleXlet xlet;

    public FirstScreen(ScaleXlet xlet) {
        this.xlet = xlet;
    }

    /**
     * A remote control event that is coming in via
     * org.dvb.event.UserEventListener
     **/
    public void userEventReceived(UserEvent e) {
        if (e.getType() != HRcEvent.KEY_PRESSED) {
            return;
        }
        int code = e.getCode();
        if (code == KeyEvent.VK_1) {
            xlet.launchRealXlet(ScaleXlet.FULL_HD, false);
        } else if (code == KeyEvent.VK_2) {
            xlet.launchRealXlet(ScaleXlet.HD_720, false);
        } else if (code == KeyEvent.VK_3) {
            xlet.launchRealXlet(ScaleXlet.QHD, false);
        } else if (code == KeyEvent.VK_4) {
            xlet.launchRealXlet(ScaleXlet.NTSC_SD, false);
        } else if (code == KeyEvent.VK_5) {
            xlet.launchRealXlet(ScaleXlet.HD_720, true);
        } else if (code == KeyEvent.VK_6) {
            xlet.launchRealXlet(ScaleXlet.QHD, true);
        } else if (code == KeyEvent.VK_7) {
            xlet.launchRealXlet(ScaleXlet.NTSC_SD, true);
        }
    }

    public void paint(Graphics g) {
        g.setColor(Color.white);
        g.fillRect(0, 0, 1920, 1080);
        g.setColor(Color.blue);
        g.setFont(new Font("Lisa", Font.PLAIN, 48));
        g.drawString("Select HGraphicsConfiguration", 300, 300);
        g.drawString("1 - Full HD (1920x1080)", 350, 400);
        g.drawString("2 - 720 HD (1080x720)", 350, 460);
        g.drawString("3 - QHD (960x540)", 350, 520);
        g.drawString("4 - NTSC SD (720x480)", 350, 580);
        g.drawString("5 - Full HD with 720 HD show", 350, 660);
        g.drawString("6 - Full HD with QHD show", 350, 720);
        g.drawString("7 - Full HD with NTSC SD show", 350, 780);
    }

}
