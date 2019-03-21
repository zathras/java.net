
/*
 * The contents of this file are subject to the Sun Public License
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
 * 
 */

package hat;

import java.io.IOException;
import java.io.File;

import hat.model.Snapshot;
import hat.model.ReachableExcludes;
import hat.server.QueryListener;

/**
 *
 * @version     1.12, 06/03/99
 * @author      Bill Foote
 */


public class Main {

    private static String VERSION_STRING = "HAT version 1.1";

    private static void usage_donationware() {
	System.out.println();
	System.out.println();
	System.out.println();
	System.out.println("HAT is \"donationware.\"  If you use and enjoy this program, please consider");
	System.out.println("making a donation to a charity.  Please see DONATIONS.html");
	System.out.println("in the doc directory for more information.");
	System.out.println();
    }

    private static void usage() {
	System.err.println("Usage:  hat [-stack=<bool>] [-refs=<bool>] [-port=<port>] [-baseline=<file> -debug=<int>] [-version] <file>");
	System.err.println();
	System.err.println("\t-stack false:     Turn off tracking object allocatoin call stack.");
	System.err.println("\t-refs false:      Turn off tracking of references to objects");
	System.err.println("\t-port <port>:     Set the port for the HTTP server.  Defaults to 7000");
	System.err.println("\t-exclude <file>:  Specify a file that lists data members that should");
	System.err.println("\t\t\t  be excluded from the reachableFrom query.");
	System.err.println("\t-baseline <file>: Specify a baseline object dump.  Objects in");
	System.err.println("\t\t\t  both heap dumps with the same ID and same class will");
	System.err.println("\t\t\t  be marked as not being \"new\".");
	System.err.println("\t-debug <int>:     Set debug level.");
	System.err.println("\t\t\t    0:  No debug output");
	System.err.println("\t\t\t    1:  Debug hprof file parsing");
	System.err.println("\t-version          Report version number");
	System.err.println("\t-donationware     Give information on the status of HAT");
	System.err.println("\t<file>            The file to read");
	System.err.println();
	System.err.println("For a JDK 1.2 (or better) dump file, you may specify which dump in the file");
	System.err.println("by appending \"#<number>\" to the file name, i.e. \"foo.hprof#3\".");
	System.err.println();
	System.err.println("All boolean options default to \"true\"");
	System.exit(1);
    }

    //
    // Convert s to a boolean.  If it's invalid, abort the program.
    //
    private static boolean booleanValue(String s) {
	if ("true".equalsIgnoreCase(s)) {
	    return true;
	} else if ("false".equalsIgnoreCase(s)) {
	    return false;
	} else {
	    usage();
	    return false;	// Never happens
	}
    }

    public static void main(String[] args) {
	if (args.length < 1) {
	    usage();
	}
	int portNumber = 7000;
	boolean callStack = true;
	boolean calculateRefs = true;
	String baselineDump = null;
	String excludeFileName = null;
	int debugLevel = 0;
	for (int i = 0; ; i += 2) {
	    if (i > (args.length - 1)) {
	        usage();
	    }
	    if ("-version".equals(args[i])) {
		System.out.println(VERSION_STRING);
		System.exit(0);
	    }
	    if ("-donationware".equals(args[i])) {
		usage_donationware();
		System.exit(0);
	    }
	    if (i == (args.length - 1)) {
		break;
	    }
	    String key = args[i];
	    String value = args[i+1];
	    if ("-stack".equals(key)) {
		callStack = booleanValue(value);
	    } else if ("-refs".equals(key)) {
		calculateRefs = booleanValue(value);
	    } else if ("-port".equals(key)) {
		portNumber = Integer.parseInt(value, 10);
	    } else if ("-exclude".equals(key)) {
		excludeFileName = value;
	    } else if ("-baseline".equals(key)) {
		baselineDump = value;
	    } else if ("-debug".equals(key)) {
		debugLevel = Integer.parseInt(value, 10);
	    }
	}
	String fileName = args[args.length - 1];
	Snapshot model = null;
	QueryListener listener = new QueryListener(portNumber);
	Thread t = new Thread(listener, "Query Listener");
	t.setPriority(Thread.NORM_PRIORITY+1);
	t.start();
	System.out.println("Started HTTP server on port " + portNumber);

	File excludeFile = null;
	if (excludeFileName != null) {
	    excludeFile = new File(excludeFileName);
	    if (!excludeFile.exists()) {
		System.out.println("Exclude file " + excludeFile 
				    + " does not exist.  Aborting.");
		System.exit(1);
	    }
	}

	System.out.println("Reading from " + fileName + "...");
	try {
	    model = hat.parser.Reader.readFile(fileName, callStack, debugLevel);
	} catch (IOException ex) {
	    ex.printStackTrace();
	    System.exit(1);
	} catch (RuntimeException ex) {
	    ex.printStackTrace();
	    System.exit(1);
	}
	System.out.println("Snapshot read, resolving...");
	model.resolve(calculateRefs);
	System.out.println("Snapshot resolved.");

	if (excludeFile != null) {
	    model.setReachableExcludes(new ReachableExcludes(excludeFile));
	}

	if (baselineDump != null) {
	    System.out.println("Reading baseline snapshot...");
	    Snapshot baseline = null;
	    try {
		baseline = hat.parser.Reader.readFile(baselineDump, false, 
						      debugLevel);
	    } catch (IOException ex) {
		ex.printStackTrace();
		System.exit(1);
	    } catch (RuntimeException ex) {
		ex.printStackTrace();
		System.exit(1);
	    }
	    baseline.resolve(false);
	    System.out.println("Discovering new objects...");
	    model.markNewRelativeTo(baseline);
	    baseline = null;	// Guard against conservative GC
	}

	listener.setModel(model);
	System.out.println("Server is ready.");
    }
}
