/*  
 * Copyright (c) 2011, Oracle
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

package com.hdcookbook.grinxlet;

import com.hdcookbook.grin.util.Debug;

import java.io.BufferedOutputStream;
import java.io.PrintWriter;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.ServerSocket;
import java.net.BindException;

import java.util.LinkedList;
import java.util.ListIterator;

/**
 * A class to manage the debug log.  The log is managed as static
 * data; the constructor of this class is therefore private.  When
 * the debug log is active, a daemon thread runs that listens for
 * telnet connections to view the log.  The DebugLog class doesn't
 * maintain any references outside of its own data in order to aid
 * in xlet cleanup.  When the surrounding xlet is destroyed, there
 * might be a slight delay before the daemon thread terminates, but
 * because the DebugLog class doesn't maintain any references to
 * the xlet, there are no xlet references in the daemon thread's root
 * set.  This means that the rest of the xlet can be GC'd even if
 * the daemon thread hasn't quite terminated yet.
 **/


public class DebugLog implements Runnable {

    private final static int LISTEN_PORT = 6000;
    static Object LOCK = new Object();          // Also needed by XletDirector
    private static int numActivations = 0;
    private static boolean destroyed = false;

        // The data of the debug log.  It's package-private, because
        // XletDirector looks directly at the data.
    static LinkedList log = new LinkedList();   // <string>
    static boolean atEOLN = true;               // handle \n
    static boolean changed = false;
    static int linesRemoved = 0;  // Count of lines removed off top
    static final int MAX_DEBUG_LINES = 3000;
        // Assuming average 50 characters/line, that's 300K.  Remember, Java is
        // two bytes/character.

    //
    // No public constructor
    //
    private DebugLog() {
    }

    /**
     * Start the DebugLog listening for incoming telnet connections.  It's
     * OK if this is called multiple times; in this case, only one thread
     * will be created.  Each call to startDebugListener() should eventually
     * be balanced by a call to shutdownDebugListener(); after the last
     * shutdown call, telnet  connections are rendered inoperative, even
     * if startDebugListener is subsequently called.
     *
     * @throws IllegalStateException    if the DebugLog has been shut down
     **/
    public static void startDebugListener() {
        synchronized(LOCK) {
            if (destroyed) {
                throw new IllegalStateException();
            }
            numActivations++;
            if (numActivations > 1) {
                return;
            }
        }
        Runnable r = new DebugLog();
        Thread t = new Thread(r, "Debug Log Listener");
        t.setPriority(4);
        t.setDaemon(true);
        t.start();
    }

    public void run() {
        try {
            listenForDebugConnect();
        } catch (InterruptedException ex) {
            if (Debug.LEVEL > 0) {
                Debug.println("Debug log listener interrupted.");
            }
        } catch (IOException ex) {
            if (Debug.LEVEL > 0) {
                Debug.printStackTrace(ex);
                Debug.println("Debug log listener failed.");
            }
        }
        if (Debug.LEVEL > 0) {
            Debug.println("Debug log listener thread terminated.");
        }
    }

    /**
     * @see #startDebugListener()
     **/
    public static void shutdownDebugListener() {
        synchronized(LOCK) {
            numActivations--;
            if (numActivations < 1) {
                destroyed = true;
                LOCK.notifyAll();
            }
        }
    }

    private static void listenForDebugConnect() 
                throws InterruptedException, IOException
    {
        ServerSocket ss = null;
        int tries = 0;
        while (ss == null) {
            synchronized(LOCK) {
                if (destroyed) {
                    return;
                }
            }
            try {
                ss = new ServerSocket(LISTEN_PORT);
            } catch (BindException ex) {
                // Maybe the xlet that launched us is
                // on port 6000.  If so, it should terminate
                // soon and release the port, so we give it 2
                // seconds, checking every 200ms up to 10 times.
                if (tries > 10) {
                    throw ex;
                }
                tries++;
                synchronized (LOCK) {
                    if (destroyed) {
                        return;
                    }
                    LOCK.wait(200);
                }
            }
        }
        ss.setSoTimeout(1000);  // Check for destroy each second
        Socket sock = null;
        try {
            Debug.println("    InetAddress.getLocalHost gives " +
                                        InetAddress.getLocalHost());
        } catch (Throwable t) {
            Debug.println("    INetAddress.getLocalHost fails with " + t);
        }
        boolean messaged = false;
        for (;;) {
            synchronized(LOCK) {
                if (destroyed) {
                    try {
                        ss.close();
                    } catch (Throwable ignored) {
                    }
                    return;
                }
            }
            try {
                if (!messaged) {
                    Debug.println("Debug log available, listening on port "
                                  + LISTEN_PORT);
                    messaged = true;
                }
                sock = ss.accept();
            } catch (InterruptedIOException ex) {
                // We get this when the 1 second timeout passes.
                continue;
            }
            messaged = false;

            // Now sock is a socket that has requested to get the log.  We
            // assume that it's over a LAN, and thus fast enough so we don't
            // have to worry about time spent writing.

            InetAddress address = sock.getInetAddress();
            Debug.println("Sending debug log to " + address);
            sock.setSoTimeout(1000);    
                // We expect a LAN connection, so 1 second is plenty
            PrintWriter out = new PrintWriter(new BufferedOutputStream(
                                        sock.getOutputStream()));
            int linesSent = 0;
            try {
                for (;;) {
                    synchronized(LOCK) {
                        if (destroyed) {
                            return;
                        }
                        int available = log.size() + linesRemoved;
                        if (!atEOLN) {
                            available--;
                        }
                        if (linesSent < linesRemoved) {
                            linesSent = linesRemoved;
                        }
                        int i = linesSent - linesRemoved;
                        ListIterator iter = log.listIterator(i);
                        if (linesSent >= available) {
                            LOCK.wait();
                                // Wakes up on destroyed or when more
                                // debug log becomes available
                        } else {
                            while (linesSent < available) {
                                linesSent++;
                                out.print(iter.next());
                                out.println('\r');  // I ! <3 Windows 
                            }
                        }
                    }
                    if (out.checkError()) {
                        Debug.println("Telnet connection to " + address 
                                          + " broken.");
                        break;  // goes back to listening
                    }
                }
            } finally {
                try {
                    out.close();
                } catch (Throwable t) {
                }
                try {
                    sock.close();
                } catch (Throwable t) {
                }
            }
        }
    }

    /**
     * Add s to the debug log, without a line break at the end.
     **/
    public static void print(String s) {
        synchronized(LOCK) {
            for (;;) {
                int pos = s.indexOf('\n');
                if (pos == -1) {
                    break;
                }
                println(s.substring(0, pos));
                s = s.substring(pos+1, s.length());
            }
            if (s.length() > 0) {
                changed = true;
                if (atEOLN) {
                    log.add(s);
                    atEOLN = false;
                } else {
                    String start = (String) log.removeLast();
                    log.add(start + s);
                }
                LOCK.notifyAll();
            }
            while (log.size() > MAX_DEBUG_LINES) {
                log.removeFirst();
                linesRemoved++;
            }
        }
    }

    /**
     * Add s to the debug log, with a line break at the end.
     **/
    public static void println(String s) {
        synchronized(LOCK) {
            print(s);
            if (atEOLN) {
                changed = true;
                log.add("");
                while (log.size() > MAX_DEBUG_LINES) {
                    log.removeFirst();
                    linesRemoved++;
                }
            } else {
                atEOLN = true;
            } 
            LOCK.notifyAll();
        }
    }
}
