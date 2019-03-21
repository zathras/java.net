
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
 * @version     1.26, 10/08/98
 * @author      Bill Foote
 */


import java.util.Hashtable;
import java.util.Enumeration;
import java.util.Vector;

/**
 * Represents a snapshot of the Java objects in the VM at one instant.
 * This is the top-level "model" object read out of a single .hprof or .bod
 * file.
 */

public class Snapshot {

    private Hashtable heapObjects = new Hashtable();	// Hashtable<Integer, JavaHeapObject>
    private Vector roots = new Vector();	// Vector<Root>
    private Hashtable classes = new Hashtable(); // Hashtable<String, JavaClass>

    private JavaThing nullThing;
    private JavaClass weakReferenceClass;
    private JavaClass javaLangClass;
    private JavaClass otherArrayType;
    private ReachableExcludes reachableExcludes = null;  // Stuff to exclude from reachable query
    private boolean hasNewSet = false;	// True iff some heap objects have isNew set
    private boolean unresolvedObjectsOK = false;	// Used for .bod legacy

    public Snapshot() {
	nullThing = new HackJavaValue("<null>", 0);
    }

    public void addHeapObject(int id, JavaHeapObject ho) {
	heapObjects.put(new Integer(id), ho);
	ho.setId(id);
    }

    public void addRoot(Root r) {
	r.setIndex(roots.size());
	roots.addElement(r);
    }

    public void addClass(int id, JavaClass c) {
	addHeapObject(id, c);
	classes.put(c.getName(), c);
    }

    private void addFakeClass(JavaClass c) {
	classes.put(c.getName(), c);
	c.resolve(this);
    }

    /**
     * @return true iff it's possible that some JavaThing instances might
     * 		isNew set
     *
     * @see JavaThing.isNew()
     */
    public boolean getHasNewSet() {
	return hasNewSet;
    }

    //
    // Used in the body of resolve()
    //
    private static class MyVisitor extends JavaHeapObjectVisitor {
	JavaHeapObject t;
	public void visit(JavaHeapObject other) {
	    other.addReferenceFrom(t);
	}
    }

    /**
     * Called after reading complete, to initialize the structure
     */
    public void resolve(boolean calculateRefs) {
	System.out.println("Resolving " + heapObjects.size() + " objects...");

	// First, resolve the classes.  All classes must be resolved before
	// we try any objects, because the objects use classes in their
	// resolution.
	//
	// We can't rely on classes to hold all of the classes, because
	// there might be two classes with the same name.
	javaLangClass = findClass("java.lang.Class");
	if (javaLangClass == null) {
	    System.out.println("WARNING:  hprof file does not include java.lang.Class!");
	    addFakeClass(new JavaClass("java.lang.Class", 0, new JavaField[0], 
				       new JavaStatic[0], null, 0));
	}
	for (Enumeration e = heapObjects.elements(); e.hasMoreElements(); ) {
	    JavaHeapObject t = (JavaHeapObject) e.nextElement();
	    if (t instanceof JavaClass) {
		t.resolve(this);
	    }
	}

	// Now, resolve everything else.
	for (Enumeration e = heapObjects.elements(); e.hasMoreElements(); ) {
	    JavaHeapObject t = (JavaHeapObject) e.nextElement();
	    if (!(t instanceof JavaClass)) {
		t.resolve(this);
	    }
	}

	weakReferenceClass = findClass("java.lang.ref.Reference");
	if (weakReferenceClass == null)  {	// JDK 1.1.x
	    weakReferenceClass = findClass("sun.misc.Ref");
	}

	if (calculateRefs) {
	    calculateReferencesToObjects();
	    System.out.print("Eliminating duplicate references");
	    System.out.flush();
	    // This println refers to the *next* step
	}
	int count = 0;
	for (Enumeration e = heapObjects.elements(); e.hasMoreElements(); ) {
	    JavaHeapObject t = (JavaHeapObject) e.nextElement();
	    t.setupReferers();
	    ++count;
	    if (calculateRefs && count % 500 == 0) {
		System.out.print(".");
		System.out.flush();
	    }
	}
	if (calculateRefs) {
	    System.out.println("");
	}
    }
    
    private void calculateReferencesToObjects() {
	System.out.print("Chasing references, expect " 
			 + (heapObjects.size() / 500) + " dots");
	System.out.flush();
	int count = 0;
	MyVisitor visitor = new MyVisitor();
	for (Enumeration e = heapObjects.elements(); e.hasMoreElements(); ) {
	    JavaHeapObject t = (JavaHeapObject) e.nextElement();
	    visitor.t = t;
	    // call addReferenceFrom(t) on all objects t references:
	    t.visitReferencedObjects(visitor);
	    ++count;
	    if (count % 500 == 0) {
		System.out.print(".");
		System.out.flush();
	    }
	}
	System.out.println();
	for (int i = 0; i < roots.size(); i++) {
	    Root r = (Root) roots.elementAt(i);
	    r.resolve(this);
	    JavaHeapObject t = findThing(r.getId());
	    if (t != null) {
		t.addReferenceFromRoot(r);
	    }
	}
    }

    public void markNewRelativeTo(Snapshot baseline) {
	hasNewSet = true;
	for (Enumeration e = heapObjects.elements(); e.hasMoreElements(); ) {
	    JavaHeapObject t = (JavaHeapObject) e.nextElement();
	    boolean isNew;
	    int thingID = t.getId();
	    if (thingID == 0 || thingID == -1) {
		isNew = false;
	    } else {
		JavaThing other = baseline.findThing(t.getId());
		if (other == null) {
		    isNew = true;
		} else {
		    isNew = !t.isSameTypeAs(other);
		}
	    }
	    t.setNew(isNew);
	}
    }

    public Enumeration getThings() {
        return heapObjects.elements();
    }


    public JavaHeapObject findThing(int id) {
	return (JavaHeapObject) heapObjects.get(new Integer(id));
    }

    public JavaClass findClass(String name) {
	return (JavaClass) classes.get(name);
    }

    /**
     * Return an array of all of the classes in this snapshot.
     * The callee may modify the array.
     **/
    public JavaClass[] getClasses() {
	JavaClass[] result = new JavaClass[classes.size()];
	int i = 0;
	for (Enumeration e = classes.elements(); e.hasMoreElements(); ) {
	    result[i++] = (JavaClass) e.nextElement();
	}
	return result;
    }

    public Root[] getRoots() {
	Root[] result = new Root[roots.size()];
	for (int i = 0; i < roots.size(); i++) {
	    result[i] = (Root) roots.elementAt(i);
	}
	return result;
    }

    public Root getRootAt(int i) {
	return (Root) roots.elementAt(i);
    }

    public ReferenceChain[] 
    rootsetReferencesTo(JavaHeapObject target, boolean includeWeak) {
	Vector fifo = new Vector();  // This is slow... A real fifo would help
	    // Must be a fifo to go breadth-first
	Hashtable visited = new Hashtable();
	// Objects are added here right after being added to fifo.
	Vector result = new Vector();
	visited.put(target, target);
	fifo.addElement(new ReferenceChain(target, null));

	while (fifo.size() > 0) {
	    ReferenceChain chain = (ReferenceChain) fifo.elementAt(0);
	    fifo.removeElementAt(0);
	    JavaHeapObject curr = chain.getObj();
	    if (curr.getRoot() != null) {
		result.addElement(chain);
		// Even though curr is in the rootset, we want to explore its
		// referers, because they might be more interesting.
	    }
	    Enumeration referers = curr.getReferers();
	    while (referers.hasMoreElements()) {
		JavaHeapObject t = (JavaHeapObject) referers.nextElement();
		if (t != null && visited.get(t) == null) {
		    if (includeWeak || !t.refersOnlyWeaklyTo(this, curr)) {
			visited.put(t, t);
			fifo.addElement(new ReferenceChain(t, chain));
		    }
		}
	    }
	}

	ReferenceChain[] realResult = new ReferenceChain[result.size()];
	for (int i = 0; i < result.size(); i++) {
	    realResult[i] = (ReferenceChain) result.elementAt(i);
	}
	return realResult;
    }

    public boolean getUnresolvedObjectsOK() {
	return unresolvedObjectsOK;
    }

    public void setUnresolvedObjectsOK(boolean v) {
	unresolvedObjectsOK = v;
    }

    public JavaClass getWeakReferenceClass() {
	return weakReferenceClass;
    }

    JavaClass getJavaLangClass() {
	return javaLangClass;
    }

    JavaClass getOtherArrayType() {
	if (otherArrayType == null) {
	    synchronized(this) {
		if (otherArrayType == null) {
		    addFakeClass(new JavaClass("[<other>", 0, new JavaField[0], 
					       new JavaStatic[0], null, 0));
		    otherArrayType = findClass("[<other>");
		}
	    }
	}
	return otherArrayType;
    }

    JavaClass getArrayClass(String elementSignature) {
	JavaClass clazz;
	synchronized(classes) {
	    clazz = findClass("[" + elementSignature);
	    if (clazz == null) {
		clazz = new JavaClass("[" + elementSignature, 0,
				       new JavaField[0],
				       new JavaStatic[0], null, 0);
		addFakeClass(clazz);
		// This is needed because the JDK only creates Class structures
		// for array element types, not the arrays themselves.  For
		// analysis, though, we need to pretend that there's a
		// JavaClass for the array type, too.
	    }
	}
	return clazz;
    }

    public JavaThing getNullThing() {
	return nullThing;
    }

    public void setReachableExcludes(ReachableExcludes e) {
	reachableExcludes = e;
    }

    public ReachableExcludes getReachableExcludes() {
	return reachableExcludes;
    }

}
