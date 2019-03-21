
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

import hat.model.*;
import hat.util.ArraySorter;
import hat.util.Comparer;

/**
 *
 * @version     1.7, 03/06/98
 * @author      Bill Foote
 */


class AllClassesQuery extends QueryHandler {

    boolean excludePlatform;

    public AllClassesQuery(boolean excludePlatform) {
	this.excludePlatform = excludePlatform;
    }

    public void run() {
	if (excludePlatform) {
	    startHtml("All Classes (excluding platform)");
	} else {
	    startHtml("All Classes (including platform)");
	}

	JavaClass[] classes = snapshot.getClasses();
	if (excludePlatform) {
	    classes = PlatformClasses.filterPlatformClasses(classes);
	}
	ArraySorter.sort(classes, new Comparer() {
	    public int compare(Object lhs, Object rhs) {
		String left = ((JavaClass) lhs).getName();
		String right = ((JavaClass) rhs).getName();
		if (left.startsWith("[") != right.startsWith("[")) {
		    // In ancient heap dumps, arrays were
		    // displayed as ugly "[C" or "L[Foo[]" instead
		    // of "char[]" and "Foo[]".  Back then, we sorted
		    // them at the end.  Kept for legacy.
		    if (left.startsWith("[")) {
			return 1;
		    } else {
			return -1;
		    }
		}
		boolean lc = left.indexOf(".") != -1;
		boolean rc = right.indexOf(".") != -1;
		if (lc != rc) {
		    // The default package comes first
		    if (!lc) {
			return -1;
		    } else {
			return 1;
		    }
		}
		return left.compareTo(right);
	    }
	});

	String lastPackage = null;
	for (int i = 0; i < classes.length; i++) {
	    String name = classes[i].getName();
	    int pos = name.lastIndexOf(".");
	    String pkg;
	    if (name.startsWith("[")) {		// Only in ancient heap dumps
		pkg = "<Arrays>";
	    } else if (pos == -1) {
		pkg = "<Default Package>";
	    } else {
		pkg = name.substring(0, pos);
	    }
	    if (!pkg.equals(lastPackage)) {
		out.print("<h2>Package ");
		print(pkg);
		out.println("</h2>");
	    }
	    lastPackage = pkg;
	    printClass(classes[i]);
	    out.println("<br>");
	}

	out.println("<h2>Other Queries</h2>");
	out.println("<ul>");

	out.println("<li>");
	printAnchorStart();
	if (excludePlatform) {
	    out.print("allClassesWithPlatform/\">");
	    print("All classes including platform");
	} else {
	    out.print("\">");
	    print("All classes excluding platform");
	}
	out.println("</a>");

	out.println("<li>");
	printAnchorStart();
	out.print("showRoots/\">");
	print("Show all members of the rootset");
	out.println("</a>");

	out.println("<li>");
	printAnchorStart();
	out.print("showInstanceCounts/includePlatform/\">");
	print("Show instance counts for all classes (including platform)");
	out.println("</a>");

	out.println("<li>");
	printAnchorStart();
	out.print("showInstanceCounts/\">");
	print("Show instance counts for all classes (excluding platform)");
	out.println("</a>");

	out.println("</ul>");

	endHtml();
    }

    
}
