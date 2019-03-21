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

package hellotvxlet;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import javax.tv.service.SIManager;
import javax.tv.service.selection.ServiceContextFactory;
import javax.tv.service.selection.ServiceContextException;
import javax.tv.xlet.XletContext;
import org.bluray.ti.DiscManager;
import org.bluray.ti.Title;
import org.bluray.ti.selection.TitleContext;
import org.bluray.vfs.VFSManager;
import org.havi.ui.HScene;
import org.havi.ui.HSceneFactory;

import net.java.bd.tools.logger.XletLogger;


/**
 * A simple VFS update test.  Replaces itself with a new xlet jar.
 */

public class HelloTVXlet implements javax.tv.xlet.Xlet, Runnable {
   
    /** !!CHANGE!!  Should be the URL where the three "downloads" files reside.  **/
    static String HOSTDIR = "http://javaweb.sfbay.sun.com/~csaito/bd-j/BindingUnitTest/";  
    static String[] downloads = { "sample.sf", "sample.xml", "00000.jar" };
    
    private XletContext  context;
    private String       bindingUnitDir;    

    public void initXlet(XletContext context) {
        this.context = context;
        
        String root = System.getProperty("bluray.bindingunit.root");
        String orgID = (String) context.getXletProperty("dvb.org.id");
        String discID = DiscManager.getDiscManager().getCurrentDisc().getId();
        //String discID = Integer.toHexString(Integer.parseInt(discID0, 16));
        
        //Set the logging output file
        XletLogger.setLogFile(System.getProperty("dvb.persistent.root")
               + "/" + context.getXletProperty("dvb.org.id")
               + "/" + context.getXletProperty("dvb.app.id")
               + "/" + "log.txt");

        bindingUnitDir = root + File.separator + orgID + File.separator + discID;
        XletLogger.log("BindingRoot: " + bindingUnitDir);
    } 
    
    public void startXlet() {
        XletLogger.log("Starting the xlet...");        
        XletLogger.setVisible(true); 
        
        // Check that this player is supporting VFS.
        String lsLevel = System.getProperty("bluray.localstorage.level");
        if (lsLevel.equals("-1")) {
            XletLogger.log("VFS is not supported");
            return;
        } else if (lsLevel.equals("0")) {
            XletLogger.log("No storage device");
            return;
        }
        
        // Check that this player supports network access.
        if (!"YES".equals(System.getProperty("bluray.profile.2"))) {
            XletLogger.log("Not a profile 2 player, stopping the test run");
            return;
        }
        
        if (new File(bindingUnitDir, downloads[0]).exists()) {
            // If files are found from a previous run, just clean up and exit.
            XletLogger.log("Downloaded files found, deleting them.");
            cleanup();
            XletLogger.log("Cleanup finished, restart the xlet to run the test.");
            return;
        }
        
        new Thread(this).start();  // start the test
    }
    
    public void run() {        
        try {
            URL host = new URL(HOSTDIR);
            for (int i = 0; i < downloads.length; i++) {
                URL  url  = new URL(host, downloads[i]);
                File file = new File(bindingUnitDir, downloads[i]);
                XletLogger.log("Downloading " + downloads[i] + " to " + bindingUnitDir);
                downloadFile(url.openStream(), new FileOutputStream(file));
            }
        } catch (Exception e) {
            XletLogger.log("Download failed", e);
            return;
        }

        VFSManager manager = VFSManager.getInstance();
        XletLogger.log("Finished downloading, calling VFS update");
        try {
            manager.requestUpdating(bindingUnitDir + File.separator + "sample.xml",
                    bindingUnitDir + File.separator + "sample.sf",
                    true);
        } catch (Exception e) {
            XletLogger.log("Failed to request VFS update", e);
            return;
        }
        
        XletLogger.log("Getting TitleContext");
        TitleContext titleContext;
        
        try {
            titleContext = 
                    (TitleContext) ServiceContextFactory.getInstance().getServiceContext(context);
        } catch (SecurityException ex) {
            XletLogger.log("Can't get TitleContext", ex);
            return;
        } catch (ServiceContextException ex) {
            XletLogger.log("Can't get TitleContext", ex);
            return;
        }
        Title title = (Title) titleContext.getService();
        XletLogger.log("Restarting the title " + title.getLocator());
        titleContext.start(title, true); // this should terminate this xlet.  
    }
    
    public void pauseXlet() {
        XletLogger.setVisible(false);
    }
    
    public void destroyXlet(boolean unconditional) {
    }
    
    private void downloadFile(InputStream in, OutputStream out)
            throws IOException {
        BufferedInputStream bufferedInputStream =
                new BufferedInputStream(in);
        ByteArrayOutputStream byteArrayOutputStream =
                new ByteArrayOutputStream();

        int start = 0;
        int length = 1024;
        int offset = -1;
        byte[] buffer = new byte[length];
        while ((offset = bufferedInputStream.read(buffer, start, length)) != -1) {
            byteArrayOutputStream.write(buffer, start, offset);
        }

        bufferedInputStream.close();
        byteArrayOutputStream.flush();
        buffer = byteArrayOutputStream.toByteArray();
        out.write(buffer);
        out.flush();
        byteArrayOutputStream.close();
        out.close();
    }  
   
    private void cleanup() {                
        for (int i = 0; i < downloads.length; i++) {
           new File(bindingUnitDir, downloads[i]).delete();
        }
    }
}
