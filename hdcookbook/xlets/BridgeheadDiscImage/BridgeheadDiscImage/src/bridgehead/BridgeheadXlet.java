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

package bridgehead;

import java.awt.event.KeyEvent;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.InputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.net.UnknownHostException;
import javax.tv.service.SIManager;
import javax.tv.service.selection.ServiceContextFactory;
import javax.tv.service.selection.ServiceContextException;
import javax.tv.xlet.XletContext;
import org.bluray.ti.DiscManager;
import org.bluray.ti.Title;
import org.bluray.ti.selection.TitleContext;
import org.bluray.vfs.VFSManager;
import org.bluray.vfs.PreparingFailedException;

import net.java.bd.tools.logger.XletLogger;
import org.bluray.net.BDLocator;
import org.bluray.ui.event.HRcEvent;
import org.dvb.event.EventManager;
import org.dvb.event.UserEvent;
import org.dvb.event.UserEventListener;
import org.dvb.event.UserEventRepository;

/**
 * A bootstrap xlet that opens up a ServerSocket,
 * downloads a ".hdcvfs" image containing a new BDMV structure,
 * and performs VFS update with the new disc image.
 */

public class BridgeheadXlet implements javax.tv.xlet.Xlet, Runnable, UserEventListener {

    public static final int PORT = 4444;
    
    private XletContext  context;
    private String       bindingUnitDir;    
    private Thread       thread;
    private ServerSocket ssocket;
    private byte[] buffer = null;
   
    private final static int NO_OPTION = 0;
    private final static int TITLE_SELECT_OPTION = 1;
    private final static int UPLOAD_PC_OPTION = 2;
    private final static int UPLOAD_PC_NO_ERASE_OPTION = 3;
    private final static int UPLOAD_URL_OPTION = 4;
    private final static int UNDO_VFS_OPTION = 5;
    private final static int ERASE_OPTION = 6;
    private int option = NO_OPTION;
    private String uploadURL = null;

    private boolean initialized = false;
    private boolean enterKeyPressed = false;
    private boolean waitingForEnter = false;
    private String titleToSelect;
    
    public void initXlet(XletContext context) {
        this.context = context;
        
        String root = System.getProperty("bluray.bindingunit.root");
        String orgID = (String) context.getXletProperty("dvb.org.id");
        String appID = (String) context.getXletProperty("dvb.app.id");
        String discID = DiscManager.getDiscManager().getCurrentDisc().getId();

        bindingUnitDir = root + "/" + orgID + "/" + discID;
        
        String ada = System.getProperty("dvb.persistent.root")
               + "/" + orgID + "/" + appID;
     
        boolean b = XletLogger.initializeSetup(false);
        // Set the logging output file, if desired.
        // This can be useful on software players, where you can
        // read it off your PC's hard disc, but on real hardware
        // players it's hard to read the file, and persistent storage
        // isn't really intended for big files.
        //   XletLogger.setLogFile(ada + "/" + "log.txt");
        XletLogger.log("BindingRoot: " + bindingUnitDir);
        
        UserEventRepository uer = new UserEventRepository("BridgeheadXlet");
        uer.addKey(KeyEvent.VK_ENTER);
        uer.addKey(KeyEvent.VK_0);
        uer.addKey(KeyEvent.VK_1);
        uer.addKey(KeyEvent.VK_2);        
        uer.addKey(KeyEvent.VK_3);        
        uer.addKey(KeyEvent.VK_4);        
        EventManager em = EventManager.getInstance();
        em.addUserEventListener(this, uer);        

        //
        // Check for xlet args, so we can autolaunch accordingly.
        //
        String[] args =
            (String[]) context.getXletProperty(context.ARGS);
        if (args == null || args.length == 0) {
            args = (String[])
                context.getXletProperty("dvb.caller.parameters");
                        // These are passed via the app launching and
                        // listing API.
        }
        if (args != null && args.length > 0) {
            XletLogger.log("Xlet arguments:");
            for (int i = 0; i < args.length; i++) {
                XletLogger.log("    " + i + ":  " + args[i]);
            }
            if ("undo_vfs".equals(args[0])) {
                option = UNDO_VFS_OPTION;
            } else if ("connect_url".equals(args[0]) && args.length > 1) {
                option = UPLOAD_URL_OPTION;
                uploadURL = args[1];
            } else if ("connect_server_socket".equals(args[0])) {
                option = UPLOAD_PC_OPTION;
            } else if ("erase_vfs".equals(args[0])) {
                option = ERASE_OPTION;
            } else {
                XletLogger.log("Unrecognized xlet arguments.  Valid options");
                XletLogger.log("    undo_vfs               undo VFS update");
                XletLogger.log("    connect_url <url>      uploads from <url>");
                XletLogger.log("    connect_server_socket  uplaods from PC");
                XletLogger.log("    erase_vfs              Erasese VFS contents");
            }
        }
        synchronized(this) {
            initialized = true;
        }
    }

    private void readTitleFile() {
        titleToSelect = "bd://1";
        File titleF = new File(bindingUnitDir + "/" + "title.txt");
        XletLogger.log(" ");
        if (!titleF.exists()) {
            XletLogger.log("No file named " + titleF.getAbsolutePath());
            XletLogger.log("Will use default title:  " + titleToSelect);
        } else {
            BufferedReader in = null;
            try {
                in = new BufferedReader(new InputStreamReader(
                        new FileInputStream(titleF), "UTF-8"));
                titleToSelect = in.readLine();
                XletLogger.log("Read title from " + titleF.getAbsolutePath()
                                + ":  " + titleToSelect);
            } catch (IOException ex) {
                XletLogger.log("Error reading " + titleF, ex);
            } finally {
                if (in != null) {
                    try {
                        in.close();
                    } catch (IOException ex) {
                    }
                }
            }
        }
        XletLogger.log(" ");
    }
    
    public void startXlet() {
  
        XletLogger.setVisible(true);  
        readTitleFile();
  
        // If the player doesn't support VFS, stop.
        if (!isPlayerCompatible()) {
            XletLogger.log("*******************************************");
            XletLogger.log("This player doesn't support VFS.");
            XletLogger.log("Sorry, but I can't do anything for you.");
            return;
        }
   
        XletLogger.log("Current VFS state:  " + getVFSState());
        if (option == NO_OPTION) {
            showIntroMessage();
        } else {
            processOption();
        }
    }
    
    public void showIntroMessage() {
        
        XletLogger.log("*******************************************");
        XletLogger.log("***** Welcome to the Bridgehead Xlet ******");
        XletLogger.log("Reinsert the disc any time to get back to this screen.");
        XletLogger.log(" ");
        XletLogger.log("Press 0 or Enter to start title " + titleToSelect);
        XletLogger.log("Press 1 to erase the BUDA, upload a new disc image from your PC and do a VFS update.");
        XletLogger.log("Press 2 to upload a disc image update from your PC and do a VFS update (no erase).");
        XletLogger.log("Press 3 to cancel previous VFS updates and go back to the optical disc.");
        XletLogger.log("Press 4 to erase contents of the VFS directory.");
        XletLogger.log("*******************************************");      
        XletLogger.log("*******************************************");
        XletLogger.log("");
               
    }

    public synchronized void waitForKey() throws InterruptedException {
        enterKeyPressed = false;
        waitingForEnter = true;
        XletLogger.log("Press enter/OK to continue...");
        while (!enterKeyPressed) {
            wait();
        }
        waitingForEnter = false;
    }
     
    public synchronized void userEventReceived(UserEvent ue) {   
        if (ue.getType()==HRcEvent.KEY_PRESSED 
             && ue.getCode() == KeyEvent.VK_ENTER) 
        {
            enterKeyPressed = true;
            notifyAll();
            if (waitingForEnter) {
                return;
            }
        }
        if (ue.getType()==HRcEvent.KEY_PRESSED && waitingForEnter) {
            XletLogger.log("Please press enter/OK");
            return;
        }
        if (!initialized || option != NO_OPTION) {
            return;
        }
        
        if (ue.getType()==HRcEvent.KEY_PRESSED) {
            switch (ue.getCode()) {
                case KeyEvent.VK_1:
                    option = UPLOAD_PC_OPTION;
                    break;
                case KeyEvent.VK_2:
                    option = UPLOAD_PC_NO_ERASE_OPTION;
                    break;
                case KeyEvent.VK_3:
                    option = UNDO_VFS_OPTION;
                    break;
                case KeyEvent.VK_4:
                    option = ERASE_OPTION;
                    break;
                case KeyEvent.VK_0:
                case KeyEvent.VK_ENTER:
                    option = TITLE_SELECT_OPTION;
                    break;
                default:
                    return; // don't do anything if the keyevent is none of the above.
            }

            processOption();
        }
    }

    public synchronized void processOption() {
        if (thread != null) {
            return;
        }
        thread = new Thread(this);
        thread.start();
    }
    
    public void run() {  
        String bumfxml = bindingUnitDir + "/" + "manifest.xml";
        String bumfsf = bindingUnitDir + "/" + "manifest.sf";
        
        try {
            switch (option) {
                case UPLOAD_PC_OPTION:
                    XletLogger.log("Erasing VFS.");
                    eraseContents("", new File(bindingUnitDir));
                    // And fall through to the "no erase" option
                case UPLOAD_PC_NO_ERASE_OPTION:
                    XletLogger.log("Uploading from PC.");
                    doDownload(bindingUnitDir);
                    doVFSUpdate(bumfxml, bumfsf);
                    waitForKey();
                    doTitleSelection();
                    break;
                case UPLOAD_URL_OPTION:
                    XletLogger.log("Erasing VFS.");
                    eraseContents("", new File(bindingUnitDir));
                    XletLogger.log("Uploading from " + uploadURL);
                    downloadFromURL(bindingUnitDir, uploadURL);
                    bumfxml = bindingUnitDir + "/" + "manifest.xml";
                    bumfsf = bindingUnitDir + "/" + "manifest.sf";
                    doVFSUpdate(bumfxml, bumfsf);
                    waitForKey();
                    doTitleSelection();
                    break;
                case UNDO_VFS_OPTION:
                    doVFSUpdate(null, null);
                    waitForKey();
                    doTitleSelection();
                    break;
                case ERASE_OPTION:
                    eraseContents("", new File(bindingUnitDir));
                    break;
                case TITLE_SELECT_OPTION:
                    XletLogger.log("Selecting title.");
                    doTitleSelection();
                    break;
                default:
                    XletLogger.log("Internal error - unrecognized option "
                                   + option);
            }
        } catch (Exception e) {
            XletLogger.log("Error!", e);
        }
        readTitleFile();
        showIntroMessage();
        cleanup();
    }
    
    public void pauseXlet() {
        XletLogger.setVisible(false);
    }
    
    public void destroyXlet(boolean unconditional) {
        cleanup();
    }
    
    public void cleanup() {
        if (thread != null) {
            thread.interrupt(); 
            thread = null;
        }
        if (ssocket != null) {
            try {
              ssocket.close();  
            } catch (Exception ex) {             
            }
        }
        option = NO_OPTION;
    }

    // Check that this player is supporting VFS.
    private boolean isPlayerCompatible() {
        String lsLevel = System.getProperty("bluray.localstorage.level");
        if (lsLevel.equals("-1")) {
            XletLogger.log("VFS is not supported, bluray.localstorage.level=" + lsLevel);
            return false;
        } else if (lsLevel.equals("0")) {
            XletLogger.log("No storage device, bluray.localstorage.level=" + lsLevel);
            return false;
        }
        // Check that this player supports network access.
        if (!"YES".equals(System.getProperty("bluray.profile.2"))) {
            XletLogger.log("Not a profile 2 player");
            return false;
        } 
        
        return true;
    }
    
    private String getHostIP() {
        try {
           return InetAddress.getLocalHost().getHostAddress(); 
        } catch (Throwable t) { 
            XletLogger.log("Can't get local host:  ", t);
            return "<unknown>";
        }
    }
    
    public void doDownload(String downloadDir) 
           throws IOException, DiscImageContentException 
    {
        XletLogger.log("Waiting for the client connect.");
        ssocket = new ServerSocket(PORT);
        
        XletLogger.log("*** Host IP is " + getHostIP() + ", listening on port " + PORT);        
        Socket clientSocket = ssocket.accept();
        
        XletLogger.log("Accepted connection, start downloading");

        try {
            OutputStream out = clientSocket.getOutputStream();
            downloadFromSocket(downloadDir, clientSocket.getInputStream());
            out.close();
        } finally {
            ssocket.close();
        }
    }

    public void downloadFromURL(String downloadDir, String urlString)
            throws IOException, DiscImageContentException 
    {
        URL url = new URL(urlString);
        downloadFromSocket(downloadDir, url.openStream());
    }


    public void downloadFromSocket(String downloadDir, InputStream stream) 
                throws IOException, DiscImageContentException 
    {
        DataInputStream din = new DataInputStream(
                                new BufferedInputStream(stream));

        try {
            int numFiles = din.readInt();
            int totalLength = din.readInt();
            int totalRead = 0;
            XletLogger.log("Download directory:  " + downloadDir);
            XletLogger.log("Downloading " + numFiles + " files, total length = "
                           + totalLength + ".");

            buffer = new byte[4096];
            for (int i = 0; i < numFiles; i++) {
                String name = din.readUTF();
                int len = din.readInt();
                XletLogger.log("    Reading " + name + " (" + len + " bytes)");
                readFile(din, name, len, downloadDir);
                ContentChecker.checkFile(new File(downloadDir, name));
                totalRead += len;
                float percent = totalRead;
                percent /= totalLength;
                percent *= 100;
                XletLogger.log("        File read, "+ percent + "% downloaded");
            }
            buffer = null;
        } finally {
            din.close();
        }
    }

    private void readFile(DataInputStream din, String name, 
                          int remaining, String dir)
            throws IOException 
    {
        File file = new File(dir, name);
        if (!file.exists()) {
           file.getParentFile().mkdirs();
        }
  
        FileOutputStream out = new FileOutputStream(file);
       
        while (remaining > 0) {
            int len = remaining;
            if (len > buffer.length) {
                len = buffer.length;
            }
            din.readFully(buffer, 0, len);
            out.write(buffer, 0, len);
            remaining -= len;
        }
        
        out.close();
    }
    
    public void doVFSUpdate(String xmlFile, String sigFile) 
            throws PreparingFailedException {
        XletLogger.log("Calling VFS update");     
        VFSManager mgr = VFSManager.getInstance();
        mgr.requestUpdating(xmlFile, sigFile, true);
        XletLogger.log("VFS update done; state now " + getVFSState());
    }
    
    private String getVFSState() {
        VFSManager mgr = VFSManager.getInstance();
        int state = mgr.getState();
        switch (state) {
            case VFSManager.STABLE:
                return "STABLE";
            case VFSManager.PREPARING:
                return "PREPARING";
            case VFSManager.PREPARED:
                return "PREPARED";
            case VFSManager.UPDATING:
                return "UPDATING";
            default:
                return "unknown state " + state;
        }
    }
    
    public void doTitleSelection() 
            throws ServiceContextException {
        
        try {
            XletLogger.log("Selecting title " + titleToSelect
                           + " on the disc.");
            BDLocator loc = new BDLocator(titleToSelect);
            ServiceContextFactory factory = ServiceContextFactory.getInstance(); 
            TitleContext titleContext =
                (TitleContext) factory.getServiceContext(context);
            Title title = (Title) SIManager.createInstance().getService(loc);
            titleContext.start(title, true);        
        } catch (SecurityException ex) {
            XletLogger.log("Can't get TitleContext", ex);
            return;
        } catch (javax.tv.locator.InvalidLocatorException ex) {
            XletLogger.log("Error in making locator", ex);
        } catch (org.davic.net.InvalidLocatorException ex) {
            XletLogger.log("Error in making locator", ex);
        }
    }

    //
    // Remove the contents of the given directory.
    //
    private static void eraseContents(String indent, File dir) {
        try {
            if (!dir.isDirectory()) {
                XletLogger.log(dir + " is not a directory.");
                return;
            }
            indent = indent + "    ";
            File[] files = dir.listFiles();
            for (int i = 0; i < files.length; i++) {
                if (files[i].isDirectory()) {
                    eraseContents(indent, files[i]);
                }
                XletLogger.log(indent + "Erasing " + files[i]);
                files[i].delete();
                if (files[i].exists()) {
                    XletLogger.log(indent + "    Error!  It still exists!");
                }
            }
        } catch (Exception ex) {
            XletLogger.log("Erasing failed:  " + ex);
        }
    }
}
