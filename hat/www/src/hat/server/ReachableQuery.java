

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

import hat.model.*;
import java.util.Vector;
import java.util.Hashtable;
import java.util.Enumeration;

import hat.util.ArraySorter;
import hat.util.Comparer;

/**
 *
 * @version     1.9, 10/08/98
 * @author      Bill Foote
 */


class ReachableQuery extends QueryHandler {
	// We inherit printFullClass from ClassQuery


    public ReachableQuery() {
    }

    public void run() {
	startHtml("Objects Reachable From 0x" + query);
	int id = parseHex(query);
	JavaHeapObject root = snapshot.findThing(id);
	final Hashtable bag = new Hashtable();
	final ReachableExcludes excludes = snapshot.getReachableExcludes();
	final Hashtable fieldsExcluded = new Hashtable();  // Bag<String>
	final Hashtable fieldsUsed = new Hashtable();	// Bag<String>
	JavaHeapObjectVisitor visitor = new JavaHeapObjectVisitor() {
	    public void visit(JavaHeapObject t) {
		// Size is zero for things like integer fields
		if (t != null && t.getSize() > 0 && bag.get(t) == null) {
		    bag.put(t, t);
		    t.visitReferencedObjects(this);
		}
	    }

	    public boolean mightExclude() {
		return excludes != null;
	    }

	    public boolean exclude(JavaClass clazz, JavaField f) {
		if (excludes == null) {
		    return false;
		}
		String nm = clazz.getName() + "." + f.getName();
		if (excludes.isExcluded(nm)) {
		    fieldsExcluded.put(nm, nm);
		    return true;
		} else {
		    fieldsUsed.put(nm, nm);
		    return false;
		}
	    }
	};
	// Put the closure of root and all objects reachable from root into
	// bag (depth first), but don't include root:
	visitor.visit(root);
	bag.remove(root);

	// Now grab the elements into a vector, and sort it in decreasing size
	JavaThing[] things = new JavaThing[bag.size()];
	int i = 0;
	for (Enumeration e = bag.elements(); e.hasMoreElements(); ) {
	    things[i++] = (JavaThing) e.nextElement();
	}
	ArraySorter.sort(things, new Comparer() {
	    public int compare(Object lhs, Object rhs) {
		JavaThing left = (JavaThing) lhs;
		JavaThing right = (JavaThing) rhs;
		int diff = right.getSize() - left.getSize();
		if (diff != 0) {
		    return diff;
		}
		return left.compareTo(right);
	    }
	});

	// Now, print out the sorted list, but start with root
	long totalSize = root.getSize();
	long instances = 1;
	out.print("<strong>");
	printThing(root);
	out.println("</strong><br>");
	out.println("<br>");
	for (i = 0; i < things.length; i++) {
	    printThing(things[i]);
	    totalSize += things[i].getSize();
	    instances++;
	    out.println("<br>");
	}

	printFields(fieldsUsed, "Data Members Followed");
	printFields(fieldsExcluded, "Excluded Data Members");
	out.println("<h2>Total of " + instances + " instances occupying " + totalSize + " bytes.</h2>");

	endHtml();
    }
    
    private void printFields(Hashtable ht, String title) {
	if (ht.size() == 0) {
	    return;
	}
	out.print("<h3>");
	print(title);
	out.println("</h3>");

	String[] fields = new String[ht.size()];
	int i = 0;
	for (Enumeration e = ht.elements(); e.hasMoreElements(); ) {
	    fields[i++] = (String) e.nextElement();
	}
	ArraySorter.sortArrayOfStrings(fields);

	for (i = 0; i < fields.length; i++) {
	    print(fields[i]);
	    out.println("<br>");
	}
    }
}
