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


import java.io.FileOutputStream;
import java.io.File;
import java.io.BufferedOutputStream;
import java.io.PrintWriter;
import java.io.IOException;

/**
 * This class is a Java SE program that generates the source code for
 * the classload performance tests.  To launch, just run this class; to
 * parameterize the tests differently, modify the main method.  This
 * program generates an xlet class called 
 * "Test" that runs the test, then immediately destroys itself.  A driver
 * xlet can run this xlet with the app launching API, then time how long
 * it takes until it gets a notification that the xlet has been destroyed.
 * See README.txt for more details on what this program is meant to test.
 * <p>
 * By the way, yes, the name of this xlet is a play on Billy Idol's
 * old band, "Gen X".  Don't worry, it won't sell out like he did.
 *
 *    @author  Bill Foote, bill@jovial.com
 **/



public class GenXlet {


    public static File baseDir;

    /**
     * Generate an xlet simulating the given number of commands/class and
     * the given number of command classes.  Each command has one extra
     * method emitted to simulate the readInstanceData() method (albeit with
     * a mostly empty method body).  For more than one command/class,
     * an extra method is emitted to simulate the method with the switch()
     * statement in it.
     **/
    public static void genXlet(int numClasses, int commandsPerClass,
                               String dir, int orgID)
                throws IOException
    {
        System.out.println(dir +  ":  " + commandsPerClass * numClasses + 
                           " commands, " + numClasses + " classes.");
        int numMethods = commandsPerClass + 1;
        if (commandsPerClass > 1) {
            numMethods++;
        }
        File f = new File(baseDir, dir);
        boolean ok = f.mkdir();
        if (!ok) {
            throw new IOException("mkdir failed");
        }
        PrintWriter w;

            // Generate the test xlet

        w = getPrintWriter(dir + "/Test.java");
        w.println("import javax.tv.xlet.Xlet;");
        w.println("import javax.tv.xlet.XletContext;");
        w.println("import javax.tv.xlet.XletStateChangeException;");
        w.println();
        w.println("public class Test implements Xlet, Runnable {");
        w.println();
        w.println("    protected boolean s = false;");
        w.println("    private XletContext c;");
        w.println();
        w.println("    public void initXlet(XletContext xc) throws XletStateChangeException {");
        w.println("        c = xc;");
        w.println("    }");
        w.println();
        w.println("    public void startXlet() throws XletStateChangeException {");
        w.println("        if (!s) {");
        w.println("            s = true;");
        w.println("            (new Thread(this)).start();");
        w.println("        }");
        w.println("    }");
        w.println();
        w.println("    public void pauseXlet() {");
        w.println("    }");
        w.println();
        w.println("    public void destroyXlet(boolean arg) throws XletStateChangeException {");
        w.println("    }");
        w.println();
        w.println("    public void run() {");
        w.println("        Test[] objs = new Test[" + numClasses+"];");
        for (int i = 0; i < numClasses; i++) {
            w.println("        objs[" + i + "] = new " + getName(i) + "();");
        }
        w.println("        for (int i = 0; i < objs.length; i++) {");
        w.println("            objs[i].aa();");
        w.println("        }");
        w.println();
        w.println("        c.notifyDestroyed();");
        w.println("    }");
        w.println();
            // We generate a method that does nothing so that the methods
            // in the generated class aren't completely empty.
        w.println("    public void n() {");
        w.println("        if (s) {    // s is always false");
        w.println("            System.out.println();");  // never executed
        w.println("        }");
        w.println("    }");
        w.println();
            // An abstract method that takes the place of Command.execute()
            // for our performance evaluation
        w.println("    public void aa() {");
        w.println("    }");
        w.println();
        w.println("}");
        w.close();

            // Generate the small classes

        for (int i = 0; i < numClasses; i++) {
            String className = getName(i);
            w = getPrintWriter(dir + "/" + className + ".java");
                // Extending Test is pretty fair, since a command class
                // extends Command, which has some methods and a data member.
                // We make each method call n(), which does nothing, but was
                // written to be something a moderately clever compiler won't
                // optimize away.
            w.println("public class " + className + " extends Test {");
            for (int j = 0; j < numMethods; j++) {
                w.println("    public void " + getName(j) + "() {");
                w.println("        n();");
                if (j == 0) {
                    for (int k = 1; k < numMethods; k++) {
                        w.println("        " + getName(k) + "();");
                    }
                }
                w.println("    }");
            }
            w.println("}");
            w.close();
        }

            // Generate the PRF

        w = getPrintWriter(dir + "/bluray.Test.perm");
        w.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        w.println("<n:permissionrequestfile xmlns:n=\"urn:BDA:bdmv;PRF\" "
                  + "orgid=\"0x7fff0001\" appid=\"0x"
                  + Long.toHexString(orgID)
                  + "\">");
        w.println("</n:permissionrequestfile>");
        w.close();
    }

    public static PrintWriter getPrintWriter(String nm) throws IOException {
        return new PrintWriter(new BufferedOutputStream(
                        new FileOutputStream(new File(baseDir, nm))));
    }

    /**
     * Get a name for something, given the number.  Names are "aa", "ab" etc.
     * They are short to simulate obfuscated code.
     **/
    public static String getName(int num) {
        if (num >= 92) {        // results in "do", which is a Java keyword
            num++;
        }
        int d26 = num / 26;
        char c1 = ((char) ('a' + d26));
        int r = num - 26 * d26;
        char c2 = ((char) ('a' + r));
        return "" + c1 + c2;
    }

    public static void main(String[] args) {
        if (args.length == 0) {
            baseDir = new File(".");
        } else {
            baseDir = new File(args[0]);
        }
        if (!baseDir.exists()) {
            System.err.println("Target directory \"" + baseDir 
                                + "\" doesn't exist");
            System.exit(1);
        }
        try {
            genXlet(10, 1, "xlet1", 0x4001);
            genXlet(20, 1, "xlet2", 0x4002);
            genXlet(100, 1, "xlet3", 0x4003);
            genXlet(5, 2, "xlet4", 0x4004);
            genXlet(10, 2, "xlet5", 0x4005);
            genXlet(50, 2, "xlet6", 0x4006);
            genXlet(1, 10, "xlet7", 0x4007);
            genXlet(2, 10, "xlet8", 0x4008);
            genXlet(10, 10, "xlet9", 0x4009);
        } catch (IOException ex) {
            ex.printStackTrace();
            System.exit(1);
        }
        System.exit(0);
    }
}

