
package com.sun.billf.tass.dbview;

import javax.swing.JFrame;
import javax.swing.JTree;
import javax.swing.JScrollPane;
import java.io.IOException;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.sun.billf.tass.TassDatabase;
import com.sun.billf.tass.MethodReference;
import com.sun.billf.tass.Utils;

/**
 * This class implements a simple UI viewer of a TassDatabase.
 * It has a main method, and is invoked with a single argument,
 * the name of the database.
 **/

public class TdbViewer {

    private TassDatabase db;
    private TopNode topNode;

    public static void usage() {
	System.out.println();
	System.out.println("Usage:  java com.sun.billf.tas.dbview.TdbViewer \\");
	System.out.println("    <.tdb database file> [<platform classes.zip>]");
	System.out.println();
	System.out.println("This creates a UI for viewing a TASS database file.");
	System.out.println();
    }


    private void init(String fn, String platform) throws IOException {
	db = new TassDatabase();
	db.init(fn, false);
	if (platform != null) {
	    db.addPlatform(platform);
	}

	topNode = new TopNode(fn, db);

	//
	// First, add the classes.  This safeguards us in the case
	// where a class has no visible members.
	//
	Iterator it = db.getClassReferences().keySet().iterator();
	while (it.hasNext()) {
	    String className = (String) it.next();
	    topNode.addClass(className);
	}

	//
	// Next, add the methods
	//
	it = db.getMethodReferences().entrySet().iterator();
	while (it.hasNext()) {
	    Map.Entry entry = (Map.Entry) it.next();
	    MethodReference mr = (MethodReference) entry.getKey();
	    String className = mr.getClassName();
	    Set refs = (Set) entry.getValue();
	    ClassNode parent = topNode.addClass(className);
	    MemberNode node = new MemberNode(mr, refs, parent, db);
	    parent.addMember(node);
	}

	//
	// Add the fields
	//
	it = db.getFieldReferences().entrySet().iterator();
	while (it.hasNext()) {
	    Map.Entry entry = (Map.Entry) it.next();
	    String fieldName = (String) entry.getKey();
	    int pos = fieldName.lastIndexOf('.');
	    String className = fieldName.substring(0, pos);
	    String shortName = fieldName.substring(pos+1);
	    Set refs = (Set) entry.getValue();
	    ClassNode parent = topNode.addClass(className);
	    MemberNode node = new MemberNode(shortName, refs, parent, db);
	    parent.addMember(node);
	}

	//
	// Finally, build the read-only structure
	//
	topNode.build();

	JFrame fm = new JFrame(fn);
	fm.addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent e) {
                System.exit(0);
            }
        });

	JTree tree = new JTree(topNode);
	JScrollPane sp = new JScrollPane(tree);
	fm.getContentPane().add(sp);
	fm.setSize(700, 560);
	fm.setVisible(true);
    }


    public static void main(String[] args) {
	String platform = null;
	if (args.length == 2) {
	    platform = args[1];
	} else if (args.length != 1) {
	    usage();
	    System.exit(1);
	}
	TdbViewer v = new TdbViewer();
	try {
	    v.init(args[0], platform);
	} catch (IOException ex) {
	    usage();
	    ex.printStackTrace();
	    System.exit(1);
	}
    }

}
