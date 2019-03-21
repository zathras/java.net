

import java.net.Socket;
import java.net.ServerSocket;
import java.net.InetAddress;

import java.io.DataOutputStream;
import java.io.OutputStream;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.EOFException;

import java.util.Date;

/**
 * This hacky class is used to trigger a heap dump on a running VM.  To
 * use, start this program like this: <pre>
 *
 *     java NetDumper /tmp/foo.hprof 7000
 *
 * </pre>
 * This will cause it to listen to a ServerSocket on port 7000.  Next,
 * invoke the program you want a heap dump for, like this: <pre>
 *
 *     java -Xrunhprof:net=localhost:7000,format=b com.random.MyProgram
 * 
 * </pre>
 * It will connect to NetDumper.  Whenever you press return in the NetDumper
 * window, it will request a heap dump, and write it to /tmp/foo.hprof.
 **/

public class NetDumper implements Runnable {

    //
    // Data members
    //
    private InputStream in;
    private OutputStream out;
    private int identifierSize;
    private byte[] buf = new byte[16 * 1024];

    public NetDumper(InputStream in, OutputStream out) {
	this.in = in;
	this.out = out;
    }

    public void run() {
	try {
	    for(;;) {
		int read = in.read(buf);
		if (read == -1) {
		    System.out.println("EOF seen, terminating.");
		    out.close();
		    System.exit(0);
		}
		out.write(buf, 0, read);
	    }
	} catch (IOException ex) {
	    ex.printStackTrace();
	    System.exit(1);
	}
    }


    private static void usage() {
	System.out.println("Usage:  java NetDumper <file> <port>");
	System.exit(1);
    }

    public static void main(String args[]) {
	if (args.length != 2) {
	    usage();
	}
	try {
	    String file = args[0];
	    int port = Integer.parseInt(args[1], 10);
	    ServerSocket ss = new ServerSocket(port);

	    System.out.println("Listenening on port " + port);
	    Socket s = ss.accept();
	    InputStream in = new BufferedInputStream(s.getInputStream());

	    System.out.println("Writing to file " + file);
	    OutputStream out = new BufferedOutputStream(new FileOutputStream(file));
	   
	    NetDumper d = new NetDumper(in, out);
	    (new Thread(d)).start();

	    DataOutputStream toVM = new DataOutputStream(s.getOutputStream());
	    for (;;) {
		System.out.print("Press return to request heap dump...");
		System.out.flush();
		for (;;) {
		    int ch = System.in.read();
		    if (ch == -1) {
			System.out.println("EOF on stdin, aborting.");
			System.exit(1);
		    }
		    if (ch == '\n') {
			break;
		    }
		}
		System.out.println("Requesting heap dump.");
		toVM.writeByte(2);	// HPROF_CMD_DUMP_HEAP
		toVM.writeInt(1);	// seq_num
		toVM.writeInt(0);	// length
		toVM.flush();
	    }

	} catch (Exception ex) {
	    ex.printStackTrace();
	}
    }


}

