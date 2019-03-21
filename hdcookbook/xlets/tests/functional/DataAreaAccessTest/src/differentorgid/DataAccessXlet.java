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

package differentorgid;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import javax.tv.xlet.Xlet;
import javax.tv.xlet.XletContext;
import javax.tv.xlet.XletStateChangeException;
import org.bluray.ti.DiscManager;
import org.havi.ui.HScene;
import org.havi.ui.HSceneFactory;
import shared.DataAccessTest;
import net.java.bd.tools.logger.LwText;

public class DataAccessXlet implements Xlet {
    
    XletContext context;
    DataAccessTest test;
    HScene scene;
    LwText textComponent;

    public void initXlet(XletContext arg0) 
            throws XletStateChangeException {
        this.context = arg0;
        test = new DataAccessTest(context);
        
        scene = HSceneFactory.getInstance().getDefaultHScene(); 
        scene.setBackgroundMode(HScene.BACKGROUND_FILL);
        textComponent = new LwText("DifferentOrgID Log", Color.white, Color.black);
        scene.add(textComponent);

        scene.setBounds(100, 420, 1720, 560); 
        textComponent.setSize(1720, 560);
        scene.validate();        
    }

    public void startXlet() throws XletStateChangeException {
        scene.setVisible(true);
        try { 
           String discOrgId = test.getDiscOrgID();
           String xletOrgId = test.getXletOrgID();
           String appId = test.getXletAppID();
           String adaRoot = System.getProperty("dvb.persistent.root");
           String buRoot = System.getProperty("bluray.bindingunit.root");
           String discId = DiscManager.getDiscManager().getCurrentDisc().getId();
           char sep = File.separatorChar;

           ArrayList results = new ArrayList();
           String adaPath = adaRoot + sep + discOrgId + sep + appId;  
           String budaPath = buRoot + sep + discOrgId + sep + discId;
           try {
               results.add("Xlet context class:  " + context.getClass().getName());
               results.add("");
               // Try to figure out whose middleware this is, so we know
               // whom to contact.
           } catch (Throwable t) {
           }

           String result;

           results.add("discOrgId = " + discOrgId + ", xletOrgId = "+xletOrgId);
           
           result = test.tryRead(adaPath);
           results.add("orgId !=, ADA  disc's org read  : " + result);
           result = test.tryWrite(adaPath, "test2.txt");
           results.add("orgId !=, ADA  disc's org write : " + result);
           result = test.tryRead(budaPath);
           boolean dodgersBUDA = "Y".equals(result);
           results.add("orgId !=, BUDA disc's org read  : " + result);
           result = test.tryWrite(budaPath, "test2.txt");
           dodgersBUDA = dodgersBUDA && "Y".equals(result);
           results.add("orgId !=, BUDA disc's org write : " + result);

           String adaPath2 = adaRoot + sep + xletOrgId + sep + appId;
           String budaPath2 = buRoot + sep + xletOrgId + sep + discId;
            
           result = test.tryRead(adaPath2);
           boolean giantsADA = "Y".equals(result);
           results.add("orgId !=, ADA  xlet's org read  : " + result);
           result = test.tryWrite(adaPath2, "test2.txt");
           giantsADA = giantsADA && "Y".equals(result);
           results.add("orgId !=, ADA  xlet's org write : " + result);
           result = test.tryRead(budaPath2);
           results.add("orgId !=, BUDA xlet's org read  : " + result);
           result = test.tryWrite(budaPath2, "test2.txt");
           results.add("orgId !=, BUDA xlet's org write : " + result);
           results.add("");
           results.add(" ADA Giants  test:  " + (giantsADA ? "PASS" : "FAIL"));
           result = dodgersBUDA ? "PASS" 
                        : "FAIL - see 3-2 s. 11.5.3, Disc_Organization_ID" ;
           results.add("BUDA Dodgers test:  " + result);

           String[] lines = (String[]) results.toArray(new String[results.size()]);
           textComponent.setText(lines);        
        } catch (IOException e) {
           textComponent.setText("Error in getting orgID from id.bdmv, aborting.");
        }
        scene.repaint();
    }

    public void pauseXlet() {
    }
    
    public void destroyXlet(boolean arg0) 
        throws XletStateChangeException { 
        scene.remove(textComponent);
        scene = null;
    }  
}
