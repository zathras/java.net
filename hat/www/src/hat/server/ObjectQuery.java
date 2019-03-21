
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

import  java.util.Enumeration;

import hat.model.*;
import hat.util.ArraySorter;
import hat.util.Comparer;

/**
 *
 * @version     1.13, 10/08/98
 * @author      Bill Foote
 */


class ObjectQuery extends ClassQuery {
	// We inherit printFullClass from ClassQuery

    public ObjectQuery() {
    }

    public void run() {
	startHtml("Object at 0x" + query);
	int id = parseHex(query);
	JavaHeapObject thing = snapshot.findThing(id);
	//
	// In the following, I suppose we really should use a visitor
	// pattern.  I'm not that strongly motivated to do this, however:
	// This is the only typecase there is, and the default for an
	// unrecognized type is to do something reasonable.
	//
	if (thing == null) {
	    error("object not found");
	} else if (thing instanceof JavaClass) {
	    printFullClass((JavaClass) thing);
	} else if (thing instanceof JavaValueArray) {
	    print(((JavaValueArray) thing).toString(true));
	    printAllocationSite(thing);
	    printReferencesTo(thing);
	} else if (thing instanceof JavaObjectArray) {
	    printFullObjectArray((JavaObjectArray) thing);
	    printAllocationSite(thing);
	    printReferencesTo(thing);
	} else if (thing instanceof JavaObject) {
	    printFullObject((JavaObject) thing);
	    printAllocationSite(thing);
	    printReferencesTo(thing);
	} else {
	    // We should never get here
	    print(thing.toString());
	    printReferencesTo(thing);
	}
	endHtml();
    }
    
    
    private void printFullObject(JavaObject obj) {
	out.print("<h1>instance of ");
	print(obj.toString());
	out.print(" <small>(" + obj.getSize() + " bytes)</small>");
	out.println("</h1>\n");

	out.println("<h2>Class:</h2>");
	printClass(obj.getClazz());

	out.println("<h2>Instance data members:</h2>");
	final JavaThing[] things = obj.getFields();
	final JavaField[] fields = obj.getClazz().getFieldsForInstance();
	Integer[] hack = new Integer[things.length];
	for (int i = 0; i < things.length; i++) {
	    hack[i] = new Integer(i);
	}
	ArraySorter.sort(hack, new Comparer() {
	    public int compare(Object lhs, Object rhs) {
		JavaField left = fields[((Integer) lhs).intValue()];
		JavaField right = fields[((Integer) rhs).intValue()];
		return left.getName().compareTo(right.getName());
	    }
	});
	for (int i = 0; i < things.length; i++) {
	    int index = hack[i].intValue();
	    printField(fields[index]);
	    out.print(" : ");
	    printThing(things[index]);
	    out.println("<br>");
	}
    }

    private void printFullObjectArray(JavaObjectArray arr) {
	JavaThing[] values = arr.getValues();
	out.println("<h1>Array of " + values.length + " objects</h1>");

	out.println("<h2>Class:</h2>");
	printClass(arr.getClazz());

	out.println("<h2>Values</h2>");
	for (int i = 0; i < values.length; i++) {
	    out.print("" + i + " : ");
	    printThing(values[i]);
	    out.println("<br>");
	}
    }

    //
    // Print the StackTrace where this was allocated
    //
    private void printAllocationSite(JavaHeapObject obj) {
	StackTrace trace = obj.getAllocatedFrom();
	if (trace == null) {
	    return;
	}
	out.println("<h2>Object allocated from:</h2>");
	printStackTrace(trace);
    }
}
