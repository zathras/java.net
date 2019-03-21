
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

import java.lang.reflect.*;
import java.net.*;

import com.hdcookbook.grin.util.Debug;

public class SocketDemo {

    public static Socket createSocket(String host, int port) {
        boolean PBP11Stack = true;
        Class socketClass = null;
        Class paramTypes[];
        Constructor socketConstructor = null;
        Socket theSocket=null;

        // look for the default socket constructor which is only public in
        // CDC/PBP 1.1 (protected in CDC/PBP1.0)
        try {
            Debug.println("Looking for PBP 1.1's no-argument socket constructor...");
            // lookup the Socket class
            socketClass = Class.forName("java.net.Socket");
            paramTypes = new Class[0];
            // lookup the public constructor which does not require parameters
            socketConstructor = socketClass.getConstructor(paramTypes);
        } catch (NoSuchMethodException e) {
            // No public constructor was found
            PBP11Stack=false;
        } catch (ClassNotFoundException e) {
            // there's a more fundamental problem - should never occur
            Debug.printStackTrace(e);
        }

        if (PBP11Stack) {
            try {
                Debug.println("We are on PBP1.1 or better");
                // create socket object
                // Socket s1=new Socket();
                theSocket = (Socket) socketConstructor.newInstance(new Object[0]);

                Class[] addrParamTypes = new Class[] { String.class, Integer.TYPE };
                Class addrClass = Class.forName("java.net.InetSocketAddress");
                Constructor addrConstructor = addrClass.getConstructor(addrParamTypes);
                Object[] addrParams = new Object[] { host, new Integer(port) };
                Object remoteHost = addrConstructor.newInstance(addrParams);
                // does "new InetSocketAddress(host, port)"

                // the following connect call sets a timeout of 5000ms
                // s1.connect(remoteHost, 5000);
                paramTypes = new Class[2];
                // here we need to use SocketAddress, the base class for 
                // InetSocketAddress
                paramTypes[0] = Class.forName("java.net.SocketAddress");
                paramTypes[1] = Integer.TYPE;
                Method connectMethod 
                     = socketClass.getMethod("connect", paramTypes);
                Object args1[] = new Object[2];
                args1[0] = remoteHost;
                args1[1] = new Integer(5000);
                Debug.println("connecting socket with 5000 ms timeout.");
                connectMethod.invoke(theSocket, args1);
                Debug.println("Connected.");
            } catch (InvocationTargetException e) {
                Debug.println("" + e + ":");
                Debug.printStackTrace(e.getTargetException());
                return null;
            } catch (Exception e) {
                Debug.printStackTrace(e);
                return null;
            }
        } else {
            // the block below works both on PBP 1.0 and PBP 1.1
            // but there's no way to set a timeout
            Debug.println("We are on PBP1.0");
            try {
                Debug.println("Connecting socket with default timeout.");
                theSocket = new Socket(host, port);
               Debug.println("Connected.");
            } catch (Exception e) {
                Debug.printStackTrace(e);
                return null;
            }
        }
        return theSocket;
    }
}

