
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
 * Query to show the StackTrace for a given root
 *
 * @version     1.3, 03/06/98
 * @author      Bill Foote
 */


class RootStackQuery extends QueryHandler {

    public RootStackQuery() {
    }

    public void run() {
	int index = parseHex(query);
	Root root = snapshot.getRootAt(index);
	if (root == null) {
	    error("Root at " + index + " not found");
	    return;
	}
	StackTrace st = root.getStackTrace();
	if (st == null) {
	    error("No stack trace for " + root.getDescription());
	    return;
	}
	startHtml("Stack Trace for " + root.getDescription());
	out.println("<p>");
	printStackTrace(st);
	out.println("</p>");
	endHtml();
    }

}
