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


package client;

import java.io.BufferedOutputStream;
import java.io.BufferedInputStream;
import java.io.DataOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

/**
 * This little program will make a ".hdcvfs" stream.  It can send
 * it out a socket, or copy it to a file.  It's streamed to a socket
 * by Uploader, or copied to a file if you invoke it with the right
 * command-line arguments.
 * Copying to a file can be useful to create a .hdcvfs image that can
 * be placed on a web server, if you want to upload a disc image from
 * a fixed web server using the bridgehead xlet.
 **/

public class HdcVFSMaker {

    public static void sendHdcVFSImage(File baseDir, DataOutputStream out) 
            throws IOException
    {
        
        if (!baseDir.exists()) {
            System.out.println("Couldn't locate " + baseDir);
            return;
        }

        ArrayList<String> files = new ArrayList<String>();
        ArrayList<Integer> lengths = new ArrayList<Integer>();
        addFiles(baseDir, "", files, lengths);

        int totalLength = 0;
        for (int len : lengths) {
            totalLength += len;
        }
        System.out.println("Writing " + files.size() + " files, " 
                           + totalLength + " bytes.");
        out.writeInt(files.size());
        out.writeInt(totalLength);

        byte[] buffer = new byte[4096];
        System.out.print("    Wrote file");
        for (int i = 0; i < files.size(); i++) {
            out.writeUTF(files.get(i));
            int remaining = lengths.get(i);
            out.writeInt(remaining);
            File f = new File(baseDir, files.get(i));
            DataInputStream in = new DataInputStream(new FileInputStream(f));
            while (remaining > 0) {
                int len = remaining;
                if (len > buffer.length) {
                    len = buffer.length;
                }
                in.readFully(buffer, 0, len);
                out.write(buffer, 0, len);
                remaining -= len;
            }
            in.close();
            System.out.print(" " + (i+1));
            System.out.flush();
        }
        System.out.println();
    }

    private static void addFiles(File baseDir, String path, 
                                 ArrayList<String> files, 
                                 ArrayList<Integer> lengths) 
            throws IOException 
    {
        File dir = new File(baseDir, path);
        File[] ourFiles = dir.listFiles();
        String sep = "";
        if (path.length() > 0) {
            sep = "/";
                // I do mean "/", and not File.separatorChar.  This needs
                // to be the seperator char on the player, not on the local
                // PC, and anyway, "/" works on Windows, Linux and Mac.
        }
        for (File  f : ourFiles) {
            if (f.isDirectory()) {
                addFiles(baseDir, path + sep + f.getName(), files, lengths);
            } else {
                long len = f.length();
                if (len > Integer.MAX_VALUE) {
                    throw new IOException("File " + f.getAbsolutePath() 
                                + "'s length of " + len + " is too long");
                }
                String name = path + sep + f.getName();
                System.out.println("    Adding " + name + ", length " + len);
                files.add(name);
                lengths.add((int) len);
            }
        }
    }

    public static void makeHdcVFSFile(String baseDirS, String destS) {
        File baseDir = new File(baseDirS);
        File dest = new File(destS);
        System.out.println("Creating " + dest + " from directory " + baseDir);
        try {
            DataOutputStream out 
                = new DataOutputStream(
                    new BufferedOutputStream(
                        new FileOutputStream(dest)));
            sendHdcVFSImage(baseDir, out);
            out.flush();
            out.close();
            System.out.println("Wrote all files to " + dest);
        } catch (IOException ex) {
            ex.printStackTrace();
            System.exit(1);
        }
    }
}
