

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


/**
 * This little main program will merge two or more TASS databases into
 * a single database.  It keeps track of which application is responsible
 * for each reference using a hierarchical numbering scheme.  For example,
 * assume company X generates a tass database.  In this database, run
 * 42 generated a reference R.  If this database is the second one merged
 * into the merged database, reference R will be identified with "2.42"
 * internally.
 * <p>
 * To use this, just invoke it with the name of the merged database first,
 * and one or more databases to merge into it.  It will always add entries
 * to a database, so don't merge the same database in twice.  There's
 * no way to remove entries from a database; one must remove the database
 * and build it back up from scratch.
 * <p>
 * See the usage message for details on command-line arguments.
 *
 *
 *	@author		Bill Foote	bill.foote@sun.com
 **/


public class MergeTassDatabases {

    private TassDatabase mergedDB;
    private int trackingNumber = -1;

    public MergeTassDatabases(TassDatabase db) {
	this.mergedDB = db;
    }

    /**
     * Print a usage message describing the command-line arguments.  Running
     * this program with no arguments (or invalid arguments) will produce
     * this message.
     **/
    public static void usage() {
	System.out.println();
	System.out.println("Usage:  java com.sun.billf.tass.MergeTassDatabases \\");
	System.out.println("              <destination .tdb database file> \\");
	System.out.println("              <list of source database file(s)>");
	System.out.println();
	System.out.println("If all goes well, the program exits with an exit code of 0, and prints the");
	System.out.println("message \"OK, tracking number = <number>\" to stdout.");
	System.out.println("If there is an error, it prints one or more messages to stderr and");
	System.out.println("exits with an exit code of 1.");
	System.out.println();
    }

    public int getTrackingNumber() {
	return trackingNumber;
    }

    /**
     * This must only be called after the database is initialized
     **/
    public void addDatabase(String dbFileName) throws IOException
    {
	trackingNumber = mergedDB.nextTrackingNumber();
	TassDatabase child = new TassDatabase();
	child.init(dbFileName, false);
	mergedDB.addFrom(child);
	System.out.println("    merged " + dbFileName + " as ID " 
			   + trackingNumber + ".");
    }

    public static void main(String[] args) {
	int argsUsed = 0;
	if (args.length - argsUsed < 2) {
	    usage();
	    System.exit(1);
	}
	String databaseFileName = args[argsUsed++];
	TassDatabase db = new TassDatabase();
	MergeTassDatabases merger = new MergeTassDatabases(db);
	try {
	    db.init(databaseFileName, true);
	    for (int i = argsUsed; i < args.length; i++) {
		String fileName = args[i];
		merger.addDatabase(fileName);
	    }
	    db.write(databaseFileName);
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
	System.out.println("OK, tracking number = " 
	    				+ merger.getTrackingNumber());
	System.exit(0);
    }
}
