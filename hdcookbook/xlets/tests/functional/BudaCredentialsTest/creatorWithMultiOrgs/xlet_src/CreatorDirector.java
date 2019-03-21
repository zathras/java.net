
import com.hdcookbook.grin.Director;
import com.hdcookbook.grin.animator.AnimationEngine;
import com.hdcookbook.grin.animator.ClockBasedEngine;
import com.hdcookbook.grin.features.Box;
import com.hdcookbook.grin.features.InterpolatedModel;
import com.hdcookbook.grin.features.Translator;
import com.hdcookbook.grin.features.Text;
import com.hdcookbook.grin.util.ManagedImage;
import com.hdcookbook.grin.util.Debug;
import com.hdcookbook.grinxlet.GrinXlet;


import java.awt.Component;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.net.InetAddress;
import java.net.URL;
import java.net.URLConnection;
import java.io.File;
import java.io.InputStream;
import java.io.DataInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import com.hdcookbook.grinxlet.GrinXlet;
import javax.tv.xlet.XletContext;

import org.dvb.io.persistent.FileAccessPermissions;
import org.dvb.io.persistent.FileAttributes;

/**
 **/

public class CreatorDirector extends Director implements Runnable {

    Text resultsFeature;
    Text pfFeature;

    public void startTest() {
        (new Thread(this)).start();
    }

    private void testRead(String dir, String name, String contents) 
            throws IOException 
    {
        File file = new File(dir, name);
        Debug.println("Testing read of " + file);
        Debug.println("    exists?  " + file.exists());
        BufferedReader rdr = new BufferedReader(new FileReader(file));
        String read = rdr.readLine();
        Debug.println("    read:  \"" + read + "\"");
        rdr.close();
        if (!(contents.equals(read))) {
            throw new IOException("Contents mismatch:  " + contents + " != "
                                  + read);
        }
    }

    private void testWrite(String dir, String name, String contents)
            throws IOException 
    {
        File file = new File(dir, name);
        Debug.println("Testing write of " + file);
        Debug.println("    exists?  " + file.exists());
        BufferedWriter w = new BufferedWriter(new FileWriter(file));
        w.write(contents, 0, contents.length());
        w.newLine();
        Debug.println("    wrote:  \"" + contents + "\"");
        w.close();
        //
        // One might be tempted to think it necessary to change the
        // file attributes of any potentially newly-created file, using
        // org.dvb.io.persistent.FileAttributes.  However, MHP clause
        // 12.6.2.7.2 says:
        //
        //    An application's right to access a file is the union of the 
        //    rights granted by the credential mechanism and the right 
        //    granted by the file permission mechanism.
        //
        //  We're, of course, using credentials.  Good thing, too, since
        // FileAttributes seems to be effectively unimplemented
        // (FileAttributes.setFileAttributes() an other methods seem
        // to throw IOException for no particularly well-justified reason).
    }

    private void listDirectory(String dirS, String indent) throws IOException {
        try {
            File dir = new File(dirS);
            if (dir.isDirectory()) {
                Debug.println(indent + dir + " <dir>");
                String[] contents = dir.list();
                if (contents == null) {
                    Debug.println(indent + "    <no directory>");
                    return;
                } else if (contents.length == 0) {
                    return;
                }
                for (int i = 0; i < contents.length; i++) {
                    File f = new File(dir, contents[i]);
                    listDirectory(f.getPath(), indent);
                }
            } else {
                Debug.println(indent + dir);
            }
        } catch (Exception ex) {
            Debug.printStackTrace(ex);
            Debug.println("Ignoring exception listing directory");
        }
    }

    public void run() {
        pfFeature = (Text) getFeature("F:PassOrFail");
        resultsFeature = (Text) getFeature("F:Results");
        ArrayList results = new ArrayList();
        boolean fail = false;
        try {
            XletContext context = GrinXlet.xletContext;
            String root = System.getProperty("bluray.bindingunit.root");
            Debug.println("root is " + root);
            String orgID = (String) context.getXletProperty("dvb.org.id");
            Debug.println("org ID is " + orgID);
            String dir = root + File.separator + orgID;
            results.add("Using directory " + dir);
            testWrite(dir, "output_1.txt", "First file contents");
            testRead(dir, "output_1.txt", "First file contents");
            testWrite(dir, "output_2.txt", "Second file contents");
            testRead(dir, "output_2.txt", "Second file contents");
            listDirectory(root, "after:    ");
        } catch (Throwable ex) {
            Debug.println("Got exception:  " + ex);
            Debug.printStackTrace(ex);
            fail = true;
            results.add(ex.toString());
        }
        String[] sa = (String[]) results.toArray(new String[results.size()]);
        if (fail) {
            pfFeature.setText(new String[] { "FAIL" });
        } else {
            pfFeature.setText(new String[] { "PASS" });
        }
        resultsFeature.setText(sa);
        Debug.println("********  results *********");
        Debug.println();
        if (fail) {
            Debug.println("       FAIL");
        } else {
            Debug.println("       PASS");
        }
        Debug.println();
        for (int i = 0; i < sa.length; i++) {
            Debug.println(sa[i]);
        }
        Debug.println();
        Debug.println("**************************");

        getShow().segmentDone();
    }

}
