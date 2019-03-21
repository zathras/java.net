
/* The contents of this file are subject to the Sun Public License
 * Version 1.0 (the "License"); you may not use this file except in
 * compliance with the License. A copy of the License is available at
 * http://www.sun.com/, and in the file LICENSE.html in the
 * doc directory.
 * 
 * The Original Code is HAT. The Initial Developer of the
 * Original Code is Bill Foote, with contributions from others
 * at JavaSoft/Sun. Portions created by Bill Foote and others
 * at Javasoft/Sun are Copyright (C) 1997-2004. All Rights Reserved.
 * 
 * In addition to the formal license, I ask that you don't
 * change the history or donations files without permission.
 */

package hat.server;

/**
 *
 * @version     1.14, 03/06/98
 * @author      Bill Foote
 */


import java.net.Socket;
import java.net.ServerSocket;
import java.net.InetAddress;

import java.io.InputStream;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.Writer;
import java.io.BufferedWriter;
import java.io.PrintWriter;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.BufferedOutputStream;

import hat.model.Snapshot;

public class QueryListener implements Runnable {


    private Snapshot snapshot;
    private int port;

    public QueryListener(int port) {
	this.port = port;
	this.snapshot = null;	// Client will setModel when it's ready
    }

    public void setModel(Snapshot ss) {
	this.snapshot = ss;
    }

    public void run() {
	try {
	    waitForRequests();
	} catch (IOException ex) {
	    ex.printStackTrace();
	    System.exit(1);
	}
    }

    private void waitForRequests() throws IOException {
	ServerSocket ss = new ServerSocket(port);
	Thread last = null;
	for (;;) {
	    Socket s = ss.accept();
	    Thread t = new Thread(new HttpReader(s, snapshot));
	    if (snapshot == null) {
		t.setPriority(Thread.NORM_PRIORITY+1);
	    } else {
		t.setPriority(Thread.NORM_PRIORITY-1);
		if (last != null) {
		    try {
			last.setPriority(Thread.NORM_PRIORITY-2);
		    } catch (Throwable ignored) {
		    }
		    // If the thread is no longer alive, we'll get a 
		    // NullPointerException
		}
	    }
	    t.start();
	    last = t;
	}
    }

}
