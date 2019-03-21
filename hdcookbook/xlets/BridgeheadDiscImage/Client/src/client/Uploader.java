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

/**
 *
 */

package client;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.Socket;

public class Uploader {

    public static final int DEFAULT_PORT = 4444;

    public static void doUpload(String ip, int port, String dir) {
        
        File srcDir = new File(dir);
        if (!srcDir.exists()) {
            System.out.println("Couldn't locate " + srcDir);
            return;
        }
        
        System.out.println("IP=" + ip + ":" + port);
        System.out.println("dir=" + srcDir);
        Socket socket;
        try {
            socket = new Socket(ip, port);
            DataOutputStream out 
                 = new DataOutputStream(
                     new BufferedOutputStream(socket.getOutputStream()));
            InputStream in = socket.getInputStream();
            HdcVFSMaker.sendHdcVFSImage(srcDir, out);
            out.flush();
            try {
                while (in.read() != -1) {
                    // Wait for EOF on the return socket.  On some player/PC
                    // combos, closing the output socket before the player has
                    // read all of the bytes causes a "socket reset" error
                    // in the middle of reading.
                }
            } catch (Exception ex) {
                // We can get client reset here if the timing is off.
                // Semantically, it means the same to us as does EOF.
            }
            in.close();
            out.close();
            System.out.println("Finished sending data.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void usage() {
        System.out.println();
        System.out.println("Usage:  java client.Uploader [<dir> <dest>]");
        System.out.println();
        System.out.println("    This program recursively descends a directory, and makes a .hdcvfs stream");
        System.out.println("    for upload to the bridgehead xlet.  The directory is typically created");
        System.out.println("    with the cookbook bumfgenerator program.");
        System.out.println();
        System.out.println("    (no arguments)               launches upload GUI");
        System.out.println("    <dir> <player ip address>    uploads without a GUI");
        System.out.println("    <dir> <file>.hdcvfs          Copies stream to .hdvfs file");
        System.out.println();
        System.out.println("   An .hdcvfs file can be put on a web server, for upload with a");
        System.out.println("   bridgehead xlet that downloads it .hdcvfs file.");
        System.out.println();
        System.exit(1);
    }
    public static void main(String args[]) {
        if (args.length == 0) {
            System.out.println("Invoke with \"-help\" for more options.");
            System.out.println("These optional allow operation without a GUI.");
            ClientFrame.launchGUI();
        } else if (args.length != 2) {
            usage();
        } else {
            String dir = args[0];
            String dest = args[1];
            if (dest.endsWith(".hdcvfs")) {
                HdcVFSMaker.makeHdcVFSFile(dir, dest);
            } else {
                Uploader.doUpload(dest, DEFAULT_PORT, dir);
            }
            System.exit(0);
        }
    }
}
