

package com.sun.billf.tass;

import java.io.IOException;
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;

/**
 * This little main program excercises TASS, which stands for
 * "Test Application Signature Safety".  It checks application
 * classes against a platform definition, in the form of stub
 * classes.  If an application class has a reference to an element
 * in a platform package that does not exist, then it gives you the
 * bad news.
 * <p>
 * The Test class isn't really meant to be incorporated into a tool.
 * Rather, TassChecker is expected to be used directly, as one component
 * within a content verification tool.
 * <p>
 * This program can also give you a list of which platform packages
 * and classes are and aren't used.  This might be interesting to
 * understand platform API usage patterns of a set of TV applications.
 *
 *
 *	@author		Bill Foote	bill.foote@sun.com
 **/


public class Test {

    /**
     * Print a usage message describing the command-line arguments.  Running
     * this program with no arguments (or invalid arguments) will produce
     * this message.
     **/
    public static void usage() {
	System.out.println();
	System.out.println("Usage:  java com.sun.billf.tass.Test \\");
	System.out.println("              <zip file containing platform stubs> \\");
	System.out.println("              <text file listing platform packages> \\");
	System.out.println("              <max class file version, e.g. 47.0 for PBP 1.0> \\");
	System.out.println("              <list of application .class or .zip files>");
	System.out.println();
	System.out.println("If the application passes, this program  exits with an exit code of 0;");
	System.out.println("if it does not, it prints some messages and exits with an exit code of 1.");
	System.out.println();
    }


    public static void main(String[] args) {
	int argsUsed = 0;
	while (args.length > argsUsed && args[argsUsed].startsWith("-")) {
	    // I used to have command-line arguments starting with - here
	    usage();
	    System.exit(1);
	    argsUsed++;
	}
	if (args.length - argsUsed < 4) {
	    usage();
	    System.exit(1);
	}
	String stubsZipFile = args[argsUsed++];
	TassChecker checker = new TassChecker();
	checker.setErrorOutput(System.out);
	boolean ok = true;
	try {
	    String[] packages = Utils.readStringList(args[argsUsed++]);
	    String maxClassFileVersion = args[argsUsed++];
	    checker.init(stubsZipFile, packages, maxClassFileVersion);
	    for (int i = argsUsed; i < args.length; i++) {
		String fileName = args[i];
		boolean result;
		if (fileName.endsWith(".class")) {
		    result = checker.classIsOK(fileName);
		} else {
		    result = checker.zipIsOK(fileName);
		}
		if (!result) {
		    ok = false;
		}
	    }
	} catch (IOException ex) {
	    ex.printStackTrace();
	    System.out.println();
	    usage();
	    System.out.println();
	    System.out.println("Error:  " + ex);
	    System.out.println("Exiting with exit code 1");
	    System.out.println();
	    System.exit(1);
	}
	if (ok) {
	    System.out.println();
	    System.out.println("Application passes.  Exiting with exit code 0");
	    System.out.println();
	    System.exit(0);
	} else {
	    System.out.println();
	    System.out.println("Application had " + checker.getNumErrors()
	    			+ " error(s).  Exiting with exit code 1");
	    System.out.println();
	    System.exit(1);
	}
    }
}
