
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

/**
 **/

public class WildcardDirector extends Director implements Runnable {

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
        BufferedWriter w = new BufferedWriter(new FileWriter(file));
        w.write(contents, 0, contents.length());
        w.newLine();
        Debug.println("    wrote:  \"" + contents + "\"");
        w.close();
    }

    private void testMkdirs(String dir, String name) throws IOException {
        File file = new File(dir, name);
        Debug.println("Testing mkdirs of " + file);
        boolean ok = file.mkdirs();
        if (!ok) {
            throw new IOException("mkdirs() failed for " + file);
        }
    }
    private void testDelete(String dir, String name) throws IOException {
        File file = new File(dir, name);
        Debug.println("Testing delete of " + file);
        Debug.println("    exists?  " + file.exists());
        boolean deleted = file.delete();
        Debug.println("    deleted?  " + deleted);
        Debug.println("    exists?  " + file.exists());
        if (file.exists()) {
            throw new IOException("Couldn't delete " + file);
        }
    }

    private void listDirectory(String dirS, String indent) {
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
            String dir = root + File.separator + "7fff0002";
            results.add("Using directory " + root);
            results.add("Note that this uses a different org ID!");
            listDirectory(root, "before:    ");
            try {
                testWrite(dir, "output_1.txt", "First file contents");
                results.add("Successfully created output_1.txt");
            } catch (Exception ex) {
                results.add("Failed output_1.txt write:  " + ex);
                fail = true;
            }
            testRead(dir, "output_1.txt", "First file contents");
            testMkdirs(dir, "mydir/subdir");
            results.add("Successfully created mydir/subdir");
            testWrite(dir, "mydir/subdir/dashwild.txt", "/- wildcards work!");
            results.add("Successfully created mydir/subdir/dashwild.txt");
            testRead(dir, "mydir/subdir/dashwild.txt", "/- wildcards work!");
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
