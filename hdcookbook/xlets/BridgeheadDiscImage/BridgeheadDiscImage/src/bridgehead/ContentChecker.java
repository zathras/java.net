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

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.ArrayList;

/** 
 * Provides a check for the uploaded bundle before calling VFSUpdate.
 * The current checks are:
 * 1. BridgeheadXlet's jar and bdjo are not going to be overwritten,
 * 2. If index.bdmv is uploaded, that the new index's first playback is still 
 * the bridgehead bdjo.
 */
public class ContentChecker {

    static final String   BRIDGEHEAD_BDJO_NAME  = "90000" ;
    static final String[] BRIDGEHEAD_JAR_NAMES  = { "90000", "90001" };
    
    static void checkContent(String dir) throws DiscImageContentException, IOException {
        String[] root = new String[] { dir };
        ArrayList list = new ArrayList();
        findFiles(null, root, list);
        File[] flist = (File[])list.toArray(new File[]{});
        for (int i = 0; i < flist.length; i++) {
            checkFile(flist[i]);
        }       
    }

   static void findFiles(File path, String[] fs, ArrayList v) {
        for (int i = 0; i < fs.length; i++) {
           File f = new File(path, fs[i]);
           if (!f.isDirectory()) {
              v.add(f);
           } else {
              findFiles(path, f.list(), v);
           }
        }
   } 
   
   static void checkFile(File file) throws DiscImageContentException, IOException {
       String name = file.getAbsolutePath().toLowerCase();
       String fileName = file.getName();
       if (fileName.equals("index.bdmv")) {
           checkIndexBDMVContent(file);
       } else if (name.endsWith(".bdj")) {
           checkBDJOName(name);
       } else if (name.endsWith(".jar")) {
           checkJarName(name);
       }
   }
    
    private static void checkIndexBDMVContent(File file) throws DiscImageContentException, IOException {
        // Need to do a brute-force file parsing to check the first playback..        
        
        byte type = 0;
        byte[] name = new byte[5];

        DataInputStream din = new DataInputStream(new FileInputStream(file));
        din.skipBytes(8);
        int indexesAddress = din.readInt();
        din.skipBytes(indexesAddress - 12);  // gets to the top of indexes

        din.skipBytes(4); // skip indexes length

        type = din.readByte();  // first two bits show index object type  

        din.skipBytes(5);
        din.read(name);      // 5 bytes for name        

        din.close();

        int appType = ((type & 0x0C0) >> 6);
        if (appType == 1) {
            throw new DiscImageContentException("A new index.bdmv" +
                    " sets first playback as HDMV title");
        }
        try {
           byte[] b = BRIDGEHEAD_BDJO_NAME.getBytes("ISO646-US");
           if (!Arrays.equals(b, name)) {
               throw new DiscImageContentException("A new index.bdmv" +
                    " sets first playback other than " + BRIDGEHEAD_BDJO_NAME);

           }
        } catch (UnsupportedEncodingException e) {
            throw new IOException("UnsupportedEncodingException in parsing index.bdmv");
        }
    }
   
    private static void checkBDJOName(String name) throws DiscImageContentException {
            if (name.endsWith(BRIDGEHEAD_BDJO_NAME+".bdj")) {
                throw new DiscImageContentException("Content includes a bdjo file that" +
                        "will be overwriting the Bridgehead Xlet, " + name);
            }
    }
    
    private static void checkJarName(String name) throws DiscImageContentException {
        for (int i = 0; i < BRIDGEHEAD_JAR_NAMES.length; i++) {
            if (name.endsWith(BRIDGEHEAD_JAR_NAMES[i]+".jar")) {
                throw new DiscImageContentException("Content includes a jar file that" +
                      "will be overwriting the Bridgehead xlet's " + BRIDGEHEAD_JAR_NAMES[i] + ".jar");
            }
        }        
    }
}
