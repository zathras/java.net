
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

import com.hdcookbook.grin.util.Debug;
import com.hdcookbook.grin.Show;
import com.hdcookbook.grin.features.Text;
import com.hdcookbook.grin.Segment;

/** 
 * This class manages the user input from the alphabet grid that's
 * used to unlock the "BONUS 1" feature.
 *
 *   @author     Bill Foote (http://jovial.com)
 **/
public class UserInputManager {

    private MenuXlet xlet;
    private Text messageFeature;
    private Segment bonusSegment;
    private String[] originalMessage;
    private String[] ourMessage = new String[1];
    private String[] currentMessage;    // either original or our
    private int tries;
    private boolean firstChar = true;

    public UserInputManager(MenuXlet xlet) {
        this.xlet = xlet;
    }

    public void init() {
        messageFeature = (Text) xlet.show.getFeature("F:UserInput.Message");
        bonusSegment = xlet.show.getSegment("S:StartBonusVideo");
        if (Debug.ASSERT) {
            if (messageFeature == null || bonusSegment == null) {
                Debug.assertFail();
            }
        }
        originalMessage = messageFeature.getText();
        currentMessage = originalMessage;
    }

    public synchronized void destroy() {
    }

    /**
     * Called when a button is pressed in the alphabet grid UI.
     *
     * @param text  The uppercase letter to add, or one of the special 
     *              values "-enter-" or "-init-".
     **/
    public void setText(String text) {
        // We're called from a command, so we know the show lock
        // is being held.
        if ("-init-".equals(text)) {
            tries = 0;
            firstChar = true;
            if (currentMessage != originalMessage) {
                currentMessage = originalMessage;
                messageFeature.setText(originalMessage);
            }
        } else if ("-enter-".equals(text)) {
            handleEnter();
        } else {
            if (currentMessage == originalMessage) {
                currentMessage = ourMessage;
            }
            if (firstChar) {
                ourMessage[0] = "";
            }
            ourMessage[0] = ourMessage[0] + text;
            firstChar = false;
            if (ourMessage[0].length() > 15) {
                ourMessage[0] = ourMessage[0].substring(1);
            }
            messageFeature.setText(ourMessage);
        }
    }

    private void handleEnter() {
        tries++;
        if ("BLURAY".equals(ourMessage[0])) {
            xlet.show.activateSegment(bonusSegment);
        } else if (tries > 2) {
            ourMessage[0] = "The code is \"BLURAY\"";
            firstChar = true;
            messageFeature.setText(ourMessage);
        } else {
            ourMessage[0] = "Please try again...";
            firstChar = true;
            messageFeature.setText(ourMessage);
        }
    }
}
