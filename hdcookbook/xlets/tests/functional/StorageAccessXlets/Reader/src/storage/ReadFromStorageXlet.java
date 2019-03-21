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

package storage;

import java.io.*;
import javax.tv.xlet.Xlet;
import javax.tv.xlet.XletContext;
import java.security.AccessControlException;
import java.security.Permission;

import net.java.bd.tools.logger.XletLogger;
import org.havi.ui.HScene;
import org.havi.ui.HSceneFactory;

/**
 * A test xlet that accesses persistent storage. This needs to be signed and
 * should have application file credentials to work.
 * This Xlet reads from the persistent storage, that the writer (WriteToStorageXlet)
 * has written to. The filename is computed as below:
 * String filename = System.getProperty("dvb.persistent.root")
 *              + "/" + grantorOrgID
 *              + "/" + grantorAppID
 *              + "/tmp.txt";
 * This application is able to access the local file above which is owned by the
 * grantor only if the credentials work. Note that the player maps the root digest
 * value for this application to the grantor's root digest if valid credentials
 * are provided in the PRF. 
 */
public class ReadFromStorageXlet implements Xlet {
    
    // For displaying the stacktrace.
    //List multiLineStatus = new ArrayList();
    XletContext context;

    public void initXlet(XletContext context) {       
        this.context = context;
        
        // initialize Logger
        XletLogger.setLogFile(System.getProperty("dvb.persistent.root")
               + "/" + context.getXletProperty("dvb.org.id")
               + "/" + context.getXletProperty("dvb.app.id") 
               + "/" + "log.txt");
    }
    
    public void startXlet() {       
        XletLogger.setVisible(true);
        accessPersistantStorage();
    }
    
    public void pauseXlet() {
        XletLogger.setVisible(false);   
    }
    public void destroyXlet(boolean unconditional) {
    }
    
    public void accessPersistantStorage() {
        String filename = System.getProperty("dvb.persistent.root")
              + "/7fff3456/4001/tmp.txt";
        XletLogger.log("File:" + filename);
        // BufferedReader br = null;
        FileInputStream fis = null;
        
        try {
           // The BufferedReader does not work on PS-3;throws an IOException
           //BufferedReader br = new BufferedReader(new FileReader(filename));
           //Logger.log("READ: " + br.readLine());
           //br.close();
           
           fis = new FileInputStream(filename);
           for (int i = 0; i < 10; i++) {
                System.out.println(fis.read());
           }
           XletLogger.log("READER test passed, accessed filesystem without SecurityException");
        } catch (AccessControlException ex) {
                XletLogger.log("Error in reading file", ex);
                Permission perm = ex.getPermission();
                if (perm != null)
                    XletLogger.log(perm.toString());
        } catch (IOException ex) {
                XletLogger.log("Error in reading file", ex);
        } catch (Exception ex) {
                XletLogger.log("Error in reading file", ex);
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (Throwable ignored) {
                }
            }
        }
    }
    
    public static void main(String[] args) {
            // just to fool netbeans...
    }
}
