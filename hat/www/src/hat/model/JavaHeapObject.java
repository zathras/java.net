
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
 * @version     1.2, 10/08/98
 * @author      Bill Foote
 */

/**
 * Represents an object that's allocated out of the Java heap.  It occupies
 * memory in the VM, and is the sort of thing that in a JDK 1.1 VM had
 * a handle.  It can be a 
 * JavaClass, a JavaObjectArray, a JavaValueArray or a JavaObject.
 */

public abstract class JavaHeapObject extends JavaThing {

    private int id = -1;
    // If one or more roots refer to us, this is one of them:
    private Root root = null;	
    private boolean isNew = false;
    private StackTrace allocatedFrom;

    //
    // Who we refer to.  This is heavily optimized for space, because it's
    // well worth trading a bit of speed for less swapping.
    // referers and referersLen go through two phases:  Building and
    // resolved.  When building, referers might have duplicates, but can
    // be appended to.  When resolved, referers has no duplicates or
    // empty slots.
    //
    private JavaThing[] referers = null;
    private int referersLen = 0;	// -1 when resolved

    protected JavaHeapObject() {
    }

    protected JavaHeapObject(StackTrace allocatedFrom) {
	this.allocatedFrom = allocatedFrom;
    }

    /**
     * Do any initialization this thing needs after its data is read in.
     * Subclasses that override this should call super.resolve().
     */
    public void resolve(Snapshot snapshot) {
	if (allocatedFrom != null) {
	    allocatedFrom.resolve(snapshot);
	}
    }

    //
    //  Eliminate duplicates from referers, and size the array exactly.
    // This sets us up to answer queries.  See the comments around the
    // referers data member for details.
    //
    void setupReferers() {
	if (referersLen > 1) {
	    // Copy referers to map, screening out duplicates
	    Hashtable map = new Hashtable();
	    for (int i = 0; i < referersLen; i++) {
		if (map.get(referers[i]) == null) {
		    map.put(referers[i], referers[i]);
		}
	    }

	    // Now copy into the array
	    referers = new JavaThing[map.size()];
	    Enumeration e = map.elements();
	    for (int i = 0; i < referers.length; i++) {
		referers[i] = (JavaThing) e.nextElement();
	    }
	}
	referersLen = -1;
    }

    /**
     * @return the id of this thing, or -1 if it hasn't one
     */
    public int getId() {
	return id;
    }

    /**
     * @return the StackTrace of the point of allocation of this object,
     *		or null if unknown
     */
    public StackTrace getAllocatedFrom() {
	return allocatedFrom;
    }

    void setId(int id) {
	this.id = id;
    }

    public boolean isNew() {
	return isNew;
    }

    void setNew(boolean flag) {
	isNew = flag;
    }

    /**
     * Tell the visitor about all of the objects we refer to
     */
    abstract public void visitReferencedObjects(JavaHeapObjectVisitor v);
    
    void addReferenceFrom(JavaHeapObject other) {
	if (referersLen == 0) {
	    referers = new JavaThing[1];	// It was null
	} else if (referersLen == referers.length) {
	    JavaThing[] copy = new JavaThing[(3 * (referersLen + 1)) / 2];
	    System.arraycopy(referers, 0, copy, 0, referersLen);
	    referers = copy;
	}
	referers[referersLen++] = other;
	// We just append to referers here.  Measurements have shown that
	// around 10% to 30% are duplicates, so it's better to just append
	// blindly and screen out all the duplicates at once.
    }

    void addReferenceFromRoot(Root r) {
	if (root == null) {
	    root = r;
	} else {
	    root = root.mostInteresting(r);
	}
    }

    /**
     * If the rootset includes this object, return a Root describing one
     * of the reasons why.
     */
    public Root getRoot() {
	return root;
    }

    /**
     * Tell who refers to us.
     *
     * @return an Enumeration of JavaHeapObject instances
     */
    public Enumeration getReferers() {
	if (referersLen != -1) {
	    throw new RuntimeException("not resolved");
	}
	return new Enumeration() {

	    private int num = 0;

	    public boolean hasMoreElements() {
		return referers != null && num < referers.length;
	    }

	    public Object nextElement() {
		return referers[num++];
	    }
	};
    }

    /** 
     * Given other, which the caller promises is in referers, determines if
     * the reference is only a weak reference.
     */
    public boolean refersOnlyWeaklyTo(Snapshot ss, JavaThing other) {
	return false;
    }

    /**
     * Describe the reference that this thing has to target.  This will only
     * be called if target is in the array returned by getChildrenForRootset.
     */
    public String describeReferenceTo(JavaThing target, Snapshot ss) {
	return "??";
    }

    public boolean isHeapAllocated() {
	return true;
    }

}
