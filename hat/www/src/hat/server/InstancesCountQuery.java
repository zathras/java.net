
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
import hat.util.ArraySorter;
import hat.util.Comparer;


/**
 *
 * @version     1.7, 10/08/98
 * @author      Bill Foote
 */


class InstancesCountQuery extends QueryHandler {


    private boolean excludePlatform;

    public InstancesCountQuery(boolean excludePlatform) {
	this.excludePlatform = excludePlatform;
    }

    public void run() {
	if (excludePlatform) {
	    startHtml("Instance Counts for All Classes (excluding platform)");
	} else {
	    startHtml("Instance Counts for All Classes (including platform)");
	}

	JavaClass[] classes = snapshot.getClasses();
	if (excludePlatform) {
	    classes = PlatformClasses.filterPlatformClasses(classes);
	}
	ArraySorter.sort(classes, new Comparer() {
	    public int compare(Object lhso, Object rhso) {
		JavaClass lhs = (JavaClass) lhso;
		JavaClass rhs = (JavaClass) rhso;
		int diff = lhs.getInstancesCount(false) 
				- rhs.getInstancesCount(false);
		if (diff != 0) {
		    return -diff;	// Sort from biggest to smallest
		}
		String left = lhs.getName();
		String right = rhs.getName();
		if (left.startsWith("[") != right.startsWith("[")) {
		    // Arrays at the end
		    if (left.startsWith("[")) {
			return 1;
		    } else {
			return -1;
		    }
		}
		return left.compareTo(right);
	    }
	});

	String lastPackage = null;
	long totalSize = 0;
	long instances = 0;
	for (int i = 0; i < classes.length; i++) {
	    JavaClass clazz = classes[i];
	    int count = clazz.getInstancesCount(false);
	    print("" + count);
	    printAnchorStart();
	    out.print("instances/" + encodeForURL(classes[i].getName()));
	    out.print("\"> ");
	    if (count == 1) {
		print("instance");
	    } else {
		print("instances");
	    }
	    out.print("</a> ");
	    if (snapshot.getHasNewSet()) {
		JavaHeapObject[] objects = clazz.getInstances(false);
		int newInst = 0;
		for (int j = 0; j < objects.length; j++) {
		    if (objects[j].isNew()) {
			newInst++;
		    }
		}
		print("(");
		printAnchorStart();
		out.print("newInstances/" + encodeForURL(classes[i].getName()));
		out.print("\">");
		print("" + newInst + " new");
		out.print("</a>) ");
	    }
	    print("of ");
	    printClass(classes[i]);
	    out.println("<br>");
	    instances += count;
	    totalSize += classes[i].getTotalInstanceSize();
	}
	out.println("<h2>Total of " + instances + " instances occupying " + totalSize + " bytes.</h2>");

	out.println("<h2>Other Queries</h2>");
	out.println("<ul>");

	out.print("<li>");
	printAnchorStart();
	if (!excludePlatform) {
	    out.print("showInstanceCounts/\">");
	    print("Show instance counts for all classes (excluding platform)");
	} else {
	    out.print("showInstanceCounts/includePlatform/\">");
	    print("Show instance counts for all classes (including platform)");
	}
	out.println("</a>");

	out.print("<li>");
	printAnchorStart();
	out.print("allClassesWithPlatform/\">");
	print("Show All Classes (including platform)");
	out.println("</a>");

	out.print("<li>");
	printAnchorStart();
	out.print("\">");
	print("Show All Classes (excluding platform)");
	out.println("</a>");

	out.println("</ul>");

	endHtml();
    }

    
}
