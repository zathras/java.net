
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

package hat.server;

import java.util.Vector;

import hat.model.*;
import hat.util.ArraySorter;
import hat.util.Comparer;

/**
 *
 * @version     1.4, 03/06/98
 * @author      Bill Foote
 */


class AllRootsQuery extends QueryHandler {

    public AllRootsQuery() {
    }

    public void run() {
	startHtml("All Members of the Rootset");

	Root[] roots = snapshot.getRoots();
	ArraySorter.sort(roots, new Comparer() {
	    public int compare(Object lhs, Object rhs) {
		Root left = (Root) lhs;
		Root right = (Root) rhs;
		int d = left.getType() - right.getType();
		if (d != 0) {
		    return -d;	// More interesting values are *higher*
		}
		return left.getDescription().compareTo(right.getDescription());
	    }
	});

	int lastType = Root.INVALID_TYPE;

	for (int i= 0; i < roots.length; i++) {
	    Root root = roots[i];

	    if (root.getType() != lastType) {
		lastType = root.getType();
		out.print("<h2>");
		print(root.getTypeName() + " References");
		out.println("</h2>");
	    }

	    printRoot(root);
	    if (root.getReferer() != null) {
		out.print("<small> (from ");
		printThingAnchorTag(root.getReferer().getId());
		print(root.getReferer().toString());
		out.print(")</a></small>");
	    }
	    out.print(" :<br>");

	    JavaThing t = snapshot.findThing(root.getId());
	    if (t != null) {	// It should always be
		print("--> ");
		printThing(t);
		out.println("<br>");
	    }
	}

	out.println("<h2>Other Queries</h2>");
	out.println("<ul>");
	out.println("<li>");
	printAnchorStart();
	out.print("\">");
	print("Show All Classes");
	out.println("</a>");
	out.println("</ul>");

	endHtml();
    }
}
