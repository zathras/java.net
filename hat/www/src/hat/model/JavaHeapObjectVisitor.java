
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

package hat.model;

/**
 * A visitor for a JavaThing.  @see JavaThing#visitReferencedObjects()
 *
 * @version     1.6, 10/08/98
 * @author      Bill Foote
 */


abstract public class JavaHeapObjectVisitor {
    abstract public void visit(JavaHeapObject other);

    /**
     * Should the given field be excluded from the set of things visited?
     * @return true if it should.
     */
    public boolean exclude(JavaClass clazz, JavaField f) {
	return false;
    }

    /**
     * @return true iff exclude might ever return true
     */
    public boolean mightExclude() {
	return false;
    }

}

