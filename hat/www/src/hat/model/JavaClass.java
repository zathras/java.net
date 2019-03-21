 
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

import java.util.Vector;

/**
 *
 * @version     1.16, 10/08/98
 * @author      Bill Foote
 */


public class JavaClass extends JavaHeapObject {

    private String name;
    private JavaThing superclass = null;  // JavaObjectRef before resolve
    private int totalNumFields;
    private JavaField[] fields;		// non-static fields
    private JavaStatic[] statics;
    private int instanceSize;	// Size of an instance, including VM overhead
    private Snapshot mySnapshot;	// Who I belong to.  Set on resolve.

    private int[] rawData;	// null after resolve() done

    private Vector instances = new Vector();	// Vector<JavaHeapObject>
    private JavaClass[] subclasses = new JavaClass[0];

    public JavaClass(String name, int superclassId, 
		     JavaField[] fields, JavaStatic[] statics,
		     StackTrace stackTrace, int instanceSize) {
	super(stackTrace);
	this.name = name;
	this.superclass = new JavaObjectRef(superclassId);
	this.fields = fields;
	this.statics = statics;
	this.instanceSize = instanceSize;
    }

    public void resolve(Snapshot snapshot) {
	if (mySnapshot != null) {
	    return;
	}
	mySnapshot = snapshot;
	resolveSuperclass(snapshot);
	if (superclass != null) {
	    ((JavaClass) superclass).addSubclass(this);
	}
	for (int i = 0; i < statics.length; i++) {
	    statics[i].resolve(this, snapshot);
	}
	snapshot.getJavaLangClass().addInstance(this);
	super.resolve(snapshot);
	return;
    }

    /**
     * Resolve our superclass.  This might be called well before
     * all instances are available (like when reading deferred
     * instances in a 1.2 dump file :-)  Calling this is sufficient
     * to be able to explore this class' fields.
     */
    public void resolveSuperclass(Snapshot snapshot) {
	if (superclass == null) {
	    // We must be java.lang.Object, so we have no superclass.
	} else {
	    superclass = superclass.dereference(snapshot, null);
	    if (superclass == snapshot.getNullThing()) {
		superclass = null;
	    } else {
		try {
		    JavaClass sc = (JavaClass) superclass;
		    sc.resolveSuperclass(snapshot);
		    totalNumFields = fields.length + sc.totalNumFields;
		} catch (ClassCastException ex) {
		    System.out.println("Warning!  Superclass of " + name + " is " + superclass);
		    superclass = null;
		}
	    }
	}
    }

    /**
     * Get a numbered field from this class
     */
    public JavaField getField(int i) {
	if (i < 0 || i >= fields.length) {
	    throw new Error("No field " + i + " for " + name);
	}
	return fields[i];
    }

    /**
     * Get the total number of fields that are part of an instance of
     * this class.  That is, include superclasses.
     */
    public int getNumFieldsForInstance() {
	return totalNumFields;
    }

    /**
     * Get a numbered field from all the fields that are part of instance
     * of this class.  That is, include superclasses.
     */
    public JavaField getFieldForInstance(int i) {
	if (superclass != null) {
	    JavaClass sc = (JavaClass) superclass;
	    if (i < sc.totalNumFields) {
		return sc.getFieldForInstance(i);
	    }
	    i -= sc.totalNumFields;
	}
	return getField(i);
    }

    /**
     * Get the class responsible for field i, where i is a field number that
     * could be passed into getFieldForInstance.
     *
     * @see JavaClass.getFieldForInstance()
     */
    public JavaClass getClassForField(int i) {
	if (superclass != null) {
	    JavaClass sc = (JavaClass) superclass;
	    if (i < sc.totalNumFields) {
		return sc.getClassForField(i);
	    }
	}
	return this;
    }

    public String getName() {
	return name;
    }

    public JavaHeapObject[] getInstances(boolean includeSubclasses) {
	Vector v = instances;
	if (includeSubclasses && subclasses.length > 0) {
	    v = (Vector) v.clone();
	    for (int i = 0; i < subclasses.length; i++) {
		subclasses[i].addSubclassInstances(v);
	    }
	}
	JavaHeapObject[] result = new JavaHeapObject[v.size()];
	for (int i = 0; i < result.length; i++) {
	    result[i] = (JavaHeapObject) v.elementAt(i);
	}
	return result;
    }

    /** 
     * @return a count of the instances of this class
     */
    public int getInstancesCount(boolean includeSubclasses) {
	int result = instances.size();
	if (includeSubclasses) {
	    for (int i = 0; i < subclasses.length; i++) {
		result += subclasses[i].getInstancesCount(includeSubclasses);
	    }
	}
	return result;
    }

    public JavaClass[] getSubclasses() {
	return subclasses;
    }

    /**
     * This can only safely be called after resolve()
     */
    public JavaClass getSuperclass() {
	return (JavaClass) superclass;
    }

    public JavaField[] getFields() {
	return fields;
    }

    /**
     * Includes superclass fields
     */
    public JavaField[] getFieldsForInstance() {
	Vector v = new Vector();
	addFields(v);
	JavaField[] result = new JavaField[v.size()];
	for (int i = 0; i < v.size(); i++) {
	    result[i] = (JavaField) v.elementAt(i);
	}
	return result;
    }

    private void addFields(Vector v) {
	if (superclass != null) {
	    ((JavaClass) superclass).addFields(v);
	}
	for (int i = 0; i < fields.length; i++) {
	    v.addElement(fields[i]);
	}
    }

    public JavaStatic[] getStatics() {
	return statics;
    }

    public String toString() {
	return "class " + name;
    }

    public int compareTo(JavaThing other) {
	if (other instanceof JavaClass) {
	    return name.compareTo(((JavaClass) other).name);
	}
	return super.compareTo(other);
    }


    /**
     * @return true iff a variable of type this is assignable from an instance
     *		of other
     */
    public boolean isAssignableFrom(JavaClass other) {
	if (this == other) {
	    return true;
	} else if (other == null) {
	    return false;
	} else {
	    return isAssignableFrom((JavaClass) other.superclass);
	    // Trivial tail recursion:  I have faith in javac.
	}
    }

    /**
     * Describe the reference that this thing has to target.  This will only
     * be called if target is in the array returned by getChildrenForRootset.
     */
     public String describeReferenceTo(JavaThing target, Snapshot ss) {
	for (int i = 0; i < statics.length; i++) {
	    JavaField f = statics[i].getField();
	    if (f.hasId()) {
		JavaThing other = statics[i].getValue();
		if (other == target) {
		    return "static field " + f.getName();
		}
	    }
	}
	return super.describeReferenceTo(target, ss);
    }

    /**
     * @return the size of an instance of this class.  Gives 0 for an array
     * 		type.
     */
    public int getInstanceSize() {
	return instanceSize;
    }

    /**
     * @return The size of all instances of this class.  Correctly handles
     *		arrays.
     */
    public long getTotalInstanceSize() {
	int count = instances.size();
	if (count == 0 || instanceSize != 0) {
	    return count * instanceSize;
	}
	long result = 0;
	for (int i = 0; i < count; i++) {
	    JavaThing t = (JavaThing) instances.elementAt(i);
	    result += t.getSize();
	}
	return result;
    }

    /**
     * @return the size of this object
     */
    public int getSize() {
	JavaClass cl = mySnapshot.getJavaLangClass();
	if (cl == null) {
	    return 0;
	} else {
	    return cl.getInstanceSize();
	}
    }

    public void visitReferencedObjects(JavaHeapObjectVisitor v) {
	for (int i = 0; i < statics.length; i++) {
	    JavaField f = statics[i].getField();
	    if (!v.exclude(this, f) && f.hasId()) {
		JavaThing other = statics[i].getValue();
		if (other != null && other instanceof JavaHeapObject) {
		    v.visit((JavaHeapObject) other);
		}
	    }
	}
    }

    private void addSubclassInstances(Vector v) {
	for (int i = 0; i < subclasses.length; i++) {
	    subclasses[i].addSubclassInstances(v);
	}
	for (int i = 0; i < instances.size(); i++) {
	    v.addElement(instances.elementAt(i));
	}
    }

    void addInstance(JavaHeapObject inst) {
	instances.addElement(inst);
    }

    private void addSubclass(JavaClass sub) {
	JavaClass newValue[] = new JavaClass[subclasses.length + 1];
	System.arraycopy(subclasses, 0, newValue, 0, subclasses.length);
	newValue[subclasses.length] = sub;
	subclasses = newValue;
    }
}
