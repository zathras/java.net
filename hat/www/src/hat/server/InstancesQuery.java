
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

/**
 *
 * @version     1.2, 11/13/97
 * @author      Bill Foote
 */


class InstancesQuery extends QueryHandler {

    private boolean includeSubclasses;
    private boolean newObjects;

    public InstancesQuery(boolean includeSubclasses) {
	this.includeSubclasses = includeSubclasses;
    }

    public InstancesQuery(boolean includeSubclasses, boolean newObjects) {
	this.includeSubclasses = includeSubclasses;
	this.newObjects = newObjects;
    }

    public void run() {
	JavaClass clazz = snapshot.findClass(query);
	String instancesOf;
	if (newObjects)
	    instancesOf = "New instances of ";
	else
	    instancesOf = "Instances of ";
	if (includeSubclasses) {
	    startHtml(instancesOf + query + " (including subclasses)");
	} else {
	    startHtml(instancesOf + query);
	}
	if (clazz == null) {
	    error("Class not found");
	} else {
	    out.print("<strong>");
	    printClass(clazz);
	    out.print("</strong><br><br>");
	    JavaHeapObject[] objects = clazz.getInstances(includeSubclasses);
	    long totalSize = 0;
	    long instances = 0;
	    for (int i = 0; i < objects.length; i++) {
		if (newObjects && !objects[i].isNew())
		    continue;
		printThing(objects[i]);
		out.println("<br>");
		totalSize += objects[i].getSize();
		instances++;
	    }
	    out.println("<h2>Total of " + instances + " instances occupying " + totalSize + " bytes.</h2>");
	}
	endHtml();
    }
}
