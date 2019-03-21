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

import java.net.Socket;
import java.io.IOException;
import com.hdcookbook.grin.util.Debug;

/**
 * Simulates the socket timeout functionality by performing "socket connect"
 * operation in a separate thread. The application waits for the socket
 * connection as long as it desires by setting a timeout.
 * The application resumes back after hitting the timeout, giving up on
 * the socket connection.
 * While the socket connection thread is still on its way to connect to the
 * remote host until either the TCP times out or the connection to the
 * remote host succeeds.
 * The delayed connection is, however, ignored by closing the socket.
 *
 * Also see: <hdcookbook_home>/xlets/tests/functional/socketTimeout
 */
class TimedSocket extends Thread {

    private int port;
    private String host;
    private long timeout;

    private Socket socket = null;
    private Exception sockExcep = null;
    private Object LOCK = new Object();

    private static int CONNECT_WAIT = 0;
    private static int CONNECTED = 1;
    private static int TIMED_OUT = 2;

    /**
     * <pre>
     * Valid connection state transitions are:
     * CONNECT_WAIT --> CONNECTED --> TIMED_OUT
     *           |----> TIMED_OUT
     *           |----> CONNECTED
     * </pre>
     */
    private int connState = CONNECT_WAIT;
    
    /**
     * Creates a socket that tries to connect to the remote host
     * within the specified timeout value.
     * <p>
     * Note: A new TimedSocket instance needs to be created each time a socket
     * connection is required. The same TimedSocket instance cannot be used
     * for more than one connection. In other words, getSocket() can be called
     * only once on a TimedSocket. 
     * <p>
     * @param host Remote host name
     * @param port port number of the remote host
     * @param timeout socket time out specified in milliseconds.
     *           A timeout value of zero means no timeout. In this
     *           case the getSocket() will wait until the socket is connected
     *           to the remote host or an Exception is thrown.
     * @see #getSocket()
     */                 
    TimedSocket (String host, int port, long timeout) {
        this.host = host;
        this.port = port;
        this.timeout = timeout;
        
        /**
         * Helps the system to know that it's OK to consider the xlet this
         * thread was created in as destroyed, even if this thread is blocked
         * on waiting for the socket.
         */
        this.setDaemon(true);
    }

   /**
    * Returns a socket connected to the remote host
    * @throws IOException if the socket times out or if any other exception
    *                   is incurred in the process of connecting to the remote host
    */
   Socket getSocket() throws IOException {
        long startTime = System.currentTimeMillis();
        this.start();
        long elapsedTime = 0;
        long waitPeriod = timeout;
        while (true) {
            try {
                synchronized (LOCK) {
                   if (connState == CONNECTED) {
                        return socket;
                   } else if (sockExcep != null) {
                        throw new IOException(sockExcep.getMessage());
                   } else if (connState == TIMED_OUT) {
                        throw new IOException("Socket timed out, waited for: " + elapsedTime + " ms" );
                   }
                   LOCK.wait(waitPeriod);
                   elapsedTime = System.currentTimeMillis() - startTime;
                   if (elapsedTime >= timeout) {
                       connState = TIMED_OUT;
                   } else {
                       waitPeriod = (timeout - elapsedTime);
                   }
                }
            } catch (InterruptedException e) {
                throw new IOException("Received:" + e.getMessage());
            }
        }
    }

    public void run() {
        try {
            // Don't hold any lock for no one should be waiting on a lock
            // during connection establishment.
            // The purpose of this class is not to wait for socket connection to
            // complete either with failure or with success. 
            //
            socket = new Socket (host, port);

            // Signal that the socket is now connected
            synchronized (LOCK) {
                if (connState != TIMED_OUT) {  // abort() takes precedence
                    connState = CONNECTED;
                }
                LOCK.notifyAll();
            }
        } catch (IOException e) {
            synchronized (LOCK) {
                sockExcep = e;
                LOCK.notifyAll();
            }
            return;
        } 
        boolean doClose = false;
        synchronized(LOCK) {
            if (connState == TIMED_OUT) {
                doClose = true;
            }
        }
        if (doClose) {
            close();
        }
    }

    public  void close() {
        try {
            socket.close();
        } catch (Exception e) {
            // ignore
        }
    }

   /**
    * Abort the socket connection attempt immediately 
    */
   public void abort() {
        //
        // Wake up getSocket() that is waiting on LOCK
        // and reset the timeout
        //
        synchronized (LOCK) {
            connState = TIMED_OUT;  // force socket timeout 
            if (Debug.LEVEL > 1) {
                Debug.println("Aborting socket connection..");
            }
            LOCK.notifyAll();
        }
     }  
}
