

package com.sun.billf.tass;

import java.io.IOException;
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.io.PrintStream;
import java.io.InputStream;
import java.util.zip.ZipFile;
import java.util.zip.ZipEntry;
import java.util.Enumeration;
import java.util.HashSet;

import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.ClassParser;
import org.apache.bcel.classfile.ClassFormatException;
import org.apache.bcel.classfile.Field;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.classfile.Visitor;
import org.apache.bcel.generic.Type;
import org.apache.bcel.verifier.structurals.UninitializedObjectType;
import org.apache.bcel.generic.ArrayType;
import org.apache.bcel.generic.ObjectType;

/**
 * This little main uses the TASS classes to create and/or add to a
 * database of member usage.  It first attempts to read the named
 * TASS database, which by convention should end in ".tdb".  If the
 * file does not exist, a new, empty database will be created.  Then,
 * a .zip file containing application classes is read, and static references
 * from the application to platform classes are calculated.  Every .class
 * file contained withing the .zip archive is read; no attempt is made
 * to discover unused application classes or methods.  Finally, an updated 
 * database is written, with the record of referenes for each platform 
 * member updated.
 * <p>
 * Each run of AddToTassDatabase is tracked with an integer ID number,
 * assigned sequentially.  This allows the party that runs TASS to anonymously
 * track back to the application that contains a given reference or pattern
 * of references.
 * <p>
 * See the usage message for details on command-line arguments.
 *
 *
 *	@author		Bill Foote	bill.foote@sun.com
 **/


public class AddToTassDatabase implements Visitee {

    private TassDatabase db;
    private int trackingNumber = -1;
    private String[] platformPackages;
    private Visitor visitor;
    private int numErrors = 0;

    //
    // Flag set true if a visit traversal sees an error
    //
    private boolean errorSeenThisVisit;

    //
    // Name of the class being visited
    //
    private String classBeingVisited;

    //
    // All the errors we've seen in this class.  This is used
    // to screen out duplicates.
    //
    private HashSet errorsThisClass;

    public AddToTassDatabase(TassDatabase db) {
	this.db = db;
	this.visitor = new TassVisitor(this);
    }

    /**
     * Print a usage message describing the command-line arguments.  Running
     * this program with no arguments (or invalid arguments) will produce
     * this message.
     **/
    public static void usage() {
	System.out.println();
	System.out.println("Usage:  java com.sun.billf.tass.AddToTassDatabase \\");
	System.out.println("              <.tdb database file> \\");
	System.out.println("              <text file listing platform packages> \\");
	System.out.println("              <list .class or .zip files making up one application>");
	System.out.println();
	System.out.println("If all goes well, the program exits with an exit code of 0, and prints the");
	System.out.println("message \"OK, tracking number = <number>\" to stdout.  Non-fatal errors");
	System.out.println("may be printed to stderr.");
	System.out.println("If there is an error, it prints one or more messages to stderr and");
	System.out.println("exits with an exit code of 1.");
	System.out.println();
	System.out.println("NOTE:  A single run of AddToTassDatabase adds exactly one program to a given");
	System.out.println("       .tdb file.  To add n applications, you should run this program n times.");
	System.out.println();
    }

    public int getTrackingNumber() {
	return trackingNumber;
    }

    /**
     * This must only be called after the database is initialized
     **/
    public void init(String[] packages) throws IOException
    {
	this.platformPackages = packages;
	this.trackingNumber = db.nextTrackingNumber();
    }

    /**
     * Add the .class files in the given archive.  If an error is found,
     * emits one or more messages to stderr, and returns false. 
     * 
     * @param fileName	The name of the zip archive containing
     *			.class files
     *
     * @return  true if no errors found
     **/
    public boolean addZipFile(String fileName) throws IOException {
	ZipFile classesFile = new ZipFile(fileName);
	errorSeenThisVisit = false;
	for (Enumeration e = classesFile.entries(); e.hasMoreElements();) {
	    ZipEntry ze = (ZipEntry) e.nextElement();
	    String nm = ze.getName();
	    if (nm.endsWith(".class")) {
		InputStream is = classesFile.getInputStream(ze);
		ClassParser cp = new ClassParser(is, nm);
		JavaClass cl;
		try {
		    cl = cp.parse();
		} catch (ClassFormatException ex) {
		    reportError("Error parsing " + nm + " in " + fileName 
				 + ":  " + ex);
		    return false;
		}
		classBeingVisited = cl.getClassName();
		errorsThisClass = new HashSet();
		cl.accept(visitor);
	    }
	}
	if (errorSeenThisVisit) {
	    return false;
	} else {
	    return true;
	}
    }

    public boolean addClassFile(String fileName) throws IOException {
	ClassParser cp = new ClassParser(fileName);
	JavaClass cl;
	try {
	    cl = cp.parse();
	} catch (ClassFormatException ex) {
	    reportError("Error parsing " + fileName + ":  " + ex);
	    return false;
	}
	errorSeenThisVisit = false;
	classBeingVisited = cl.getClassName();
	errorsThisClass = new HashSet();

	cl.accept(visitor);
	if (errorSeenThisVisit) {
	    return false;
	} else {
	    return true;
	}
    }


    private boolean isInPlatformPackage(String name) {
	for (int i = 0; i < platformPackages.length; i++) {
	    if (name.startsWith(platformPackages[i])) {
		return true;
	    }
	}
	return false;
    }

    //*****************************************
    //    Visitee Implementation Methods:
    //*****************************************

    /**
     * Internal Visitee implementation method
     **/
    public void checkForClass(String name) {
	if (isInPlatformPackage(name)) {
	    db.addClassReference(name);
	}
    }

    /**
     * Internal Visitee implementation method
     **/
    public void checkType(Type t) {
	if (t instanceof UninitializedObjectType) {
	    checkType(((UninitializedObjectType) t).getInitialized());
	} else if (t instanceof ArrayType) {
	    checkType(((ArrayType) t).getElementType());
	} else if (t instanceof ObjectType) {
	    checkForClass(((ObjectType) t).getClassName());
	}
    }

    //
    // This probably never gets called in practice.  It only can be
    // called for things like parsing errors, since we're not checking
    // references against a platform definition.
    //
    /**
     * Internal Visitee implementation method
     **/
    public void reportError(String msg) {
	errorSeenThisVisit = true;
	if (!(errorsThisClass.add(msg))) {	// Screen out dups
	    return;
	}
	numErrors++;
	if (numErrors == 101) {
	    System.err.println("More than 100 errors seen.  Ignoring subsequent errors.");
	} else if (numErrors <= 100) {
	    System.err.println("Error in " + classBeingVisited + ":  " + msg);
	}
    }

    /**
     * Internal Visitee implementation method
     **/
    public void checkFieldRef(String className, String fieldName) {
	if (!isInPlatformPackage(className)) {
	    return;
	}
	db.addClassReference(className);
	db.addFieldReference(className + "." + fieldName);
    }

    /**
     * Internal Visitee implementation method
     **/
    public void checkMethodRef(String className, String methodName, Type[] args)
    {
	if (!isInPlatformPackage(className)) {
	    return;
	}
	db.addClassReference(className);
	String[] argTypeNames = new String[args.length];
	for (int i = 0; i < args.length; i++) {
	    argTypeNames[i] = args[i].toString();
	}
	db.addMethodReference(className + "." + methodName, argTypeNames);
    }

    //**********************************************
    //    End Of Visitee Implementation Methods.
    //**********************************************

    public static void main(String[] args) {
	int argsUsed = 0;
	if (args.length - argsUsed < 3) {
	    usage();
	    System.exit(1);
	}
	String databaseFileName = args[argsUsed++];
	TassDatabase db = new TassDatabase();
	AddToTassDatabase adder = new AddToTassDatabase(db);
	boolean ok = true;
	try {
	    String[] packages = Utils.readStringList(args[argsUsed++]);
	    db.init(databaseFileName, true);
	    adder.init(packages);
	    for (int i = argsUsed; i < args.length; i++) {
		String fileName = args[i];
		boolean result;
		if (fileName.endsWith(".class")) {
		    result = adder.addClassFile(fileName);
		} else {
		    result = adder.addZipFile(fileName);
		}
		if (!result) {
		    ok = false;
		}
	    }
	    if (ok) {
		db.write(databaseFileName);
	    }
	} catch (IOException ex) {
	    ex.printStackTrace(System.err);
	    System.err.println();
	    usage();
	    System.err.println();
	    System.err.println("Error:  " + ex);
	    System.err.println("Exiting with exit code 1");
	    System.err.println();
	    System.exit(1);
	}
	if (ok) {
	    System.out.println("OK, tracking number = " 
	    				+ adder.getTrackingNumber());
	    System.exit(0);
	} else {
	    System.err.println("Error.  Database not updated.");
	    System.exit(1);
	}
    }
}
