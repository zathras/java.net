
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
 *
 * @version     1.11, 10/08/98
 * @author      Bill Foote
 */


public class JavaObjectArray extends JavaHeapObject {

    private JavaThing[] values;
    private int size;	// Size of object in bytes
    private JavaClass clazz = null;	// The class of this object

    private int[] rawData;	// null after resolve() done
    private int elementClassID;	// Either the elementClassID or the
    private int arrayClassID;   // arrayClassID is known.

    /**
     * Construct a new JavaObjectArray.
     */
    public JavaObjectArray(int[] rawData, int size, StackTrace st) {
	this(rawData, size, st, 0);
	this.arrayClassID = 0;
    }

    public JavaObjectArray(int[] rawData, int size, StackTrace st,
			   int elementClassID) {
	super(st);
	this.rawData = rawData;
	this.size = size;
	this.elementClassID = elementClassID;
    }

    public JavaObjectArray(int[] rawData, int size, StackTrace st,
			   int elementClassID, int arrayClassID) {
	this(rawData, size, st, elementClassID);
	this.arrayClassID = arrayClassID;
    }

    public JavaClass getClazz() {
	return clazz;
    }

    public void resolve(Snapshot snapshot) {
	values = new JavaThing[rawData.length];
	for (int i = 0; i < rawData.length; i++) {
	    values[i] = snapshot.findThing(rawData[i]);
	}
	rawData = null;
	int elID = elementClassID;
	if (arrayClassID != 0) {	// Modern heap dumps do this
	    JavaThing t = snapshot.findThing(arrayClassID);
	    if (t != null && t instanceof JavaClass) {
		clazz = (JavaClass) t;
	    } else {
		// This really shouldn't happen, but given the
		// pre-1.5 variance in hprof files, it's wise
		// to be sure.
		elID = arrayClassID;
	    }
	} 
	if (clazz == null) {
	    if (elID != 0) {
		JavaThing t = snapshot.findThing(elementClassID);
		if (t != null && t instanceof JavaClass) {
		    JavaClass el = (JavaClass) t;
		    String nm = el.getName();
		    if (!nm.startsWith("[")) {
			nm = "L" + el.getName() + ";";
		    }
		    clazz = snapshot.getArrayClass(nm);
		}
	    }
	}
	if (clazz == null) {
	    snapshot.getOtherArrayType().addInstance(this);
	} else {
	    clazz.addInstance(this);
	}
	super.resolve(snapshot);
    }

    public JavaThing[] getValues() {
	return values;
    }

    public String toString() {
	if (clazz == null) {
	    return "array";
	} else {
	    return "Instance of " + clazz.getName();
	}
    }

    public int compareTo(JavaThing other) {
	if (other instanceof JavaObjectArray) {
	    return 0;
	}
	return super.compareTo(other);
    }

    public int getSize() {
	return size;
    }

    public void visitReferencedObjects(JavaHeapObjectVisitor v) {
	for (int i = 0; i < values.length; i++) {
	    if (values[i] != null && values[i] instanceof JavaHeapObject) {
		v.visit((JavaHeapObject) values[i]);
	    }
	}
    }

    /**
     * Describe the reference that this thing has to target.  This will only
     * be called if target is in the array returned by getChildrenForRootset.
     */
    public String describeReferenceTo(JavaThing target, Snapshot ss) {
	for (int i = 0; i < values.length; i++) {
	    if (values[i] == target) {
		return "Element " + i + " of " + this;
	    }
	}
	return super.describeReferenceTo(target, ss);
    }

}
