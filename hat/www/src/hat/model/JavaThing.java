
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

import java.util.Enumeration;
import java.util.Hashtable;


/**
 *
 * @version     1.11, 10/08/98
 * @author      Bill Foote
 */


/**
 * Represents a java "Thing".  A thing is anything that can be the value of
 * a field.  This includes JavaHeapObject, JavaObjectRef, and JavaValue.
 */

public abstract class JavaThing {

    protected JavaThing() {
    }

    /**
     * If this is a forward reference, figure out what it really
     * refers to.
     *
     * @param snapshot	The snapshot this is for
     * @param field	The field this thing represents.  If null, it is
     *			assumed this thing is an object (and never a value).
     */
    public JavaThing dereference(Snapshot shapshot, JavaField field) {
	return this;
    }
    

    /**
     * Are we the same type as other?
     *
     * @see JavaObject.isSameTypeAs()
     */
    public boolean isSameTypeAs(JavaThing other) {
	return getClass() == other.getClass();
    }
    /**
     * @return true iff this represents a heap-allocated object
     */
    abstract public boolean isHeapAllocated();

    /**
     * @return the size of this object, in bytes, including VM overhead
     */
    abstract public int getSize();

    /**
     * @return a human-readable string representation of this thing
     */
    abstract public String toString();

    /**
     * Compare our string representation to other's
     * @see java.lang.String.compareTo()
     */
    public int compareTo(JavaThing other) {
	return toString().compareTo(other.toString());
    }

}