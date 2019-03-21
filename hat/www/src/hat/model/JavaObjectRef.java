
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
 * A forward reference to an object.  This is an intermediate representation
 * for a JavaThing, when we have the thing's ID, but we might not have read
 * the thing yet.
 *
 * @version     1.5, 10/08/98
 * @author      Bill Foote
 */




public class JavaObjectRef extends JavaThing {

    private int id;

    public JavaObjectRef(int id) {
	this.id = id;
    }


    public boolean isHeapAllocated() {
	return true;
    }

    public JavaThing dereference(Snapshot snapshot, JavaField field) {
	if (field != null && !field.hasId()) {
	    // If this happens, we must be a field that represents an int.
	    // (This only happens with .bod-style files)
	    return new JavaInt(id);
	}
	if (id == 0) {
	    return snapshot.getNullThing();
	}
	JavaThing result = snapshot.findThing(id);
	if (result == null) {
	    if (!snapshot.getUnresolvedObjectsOK()) {
		String msg = "Warning!  Failed to resolve object id 0x"
				+ hat.util.Misc.toHex(id);
		if (field != null) {
		    msg += " for field " + field.getName()
			    + " (signature " + field.getSignature() + ")";
		}
		System.out.println(msg);
		Thread.dumpStack();
	    }
	    result = new HackJavaValue("Unresolved object 0x" 
					+ hat.util.Misc.toHex(id), 0);
	}
	return result;
    }

    public int getSize() {
	return 0;
    }

    public String toString() {
	return "Unresolved object 0x" + hat.util.Misc.toHex(id);
    }
}
