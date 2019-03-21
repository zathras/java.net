
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
 * @version     1.9, 10/08/98
 * @author      Bill Foote
 */


public class JavaObject extends JavaHeapObject {

    private JavaThing clazz;	// JavaObjectRef before, JavaClass after resolve
    private JavaThing[] fields;

    /**
     * Construct a new JavaObject.  
     *
     * @param clazz The class of this object (or a forward ref)
     * @param fields The fields of this object, starting from those inherited
     *		     from the top of the inheritance hierarchy, progressing
     *		     downwards.  Fields must be in the same
     *		     order as in the class.
     */
    public JavaObject(JavaThing clazz, JavaThing[] fields, StackTrace st) {
	super(st);
	this.clazz = clazz;
	this.fields = fields;
    }

    public void resolve(Snapshot snapshot) {
	clazz = clazz.dereference(snapshot, null);
	JavaClass cl = (JavaClass) clazz;
	cl.resolve(snapshot);
	for (int i = 0; i < fields.length; i++) {
	    fields[i] = fields[i].dereference(snapshot, 
					      cl.getFieldForInstance(i));
	}
	cl.addInstance(this);
	super.resolve(snapshot);
    }

    /**
     * Are we the same type as other?  We are iff our clazz is the
     * same type as other's.
     */
    public boolean isSameTypeAs(JavaThing other) {
	if (!(other instanceof JavaObject)) {
	    return false;
	}
	JavaObject oo = (JavaObject) other;
	return getClazz().getName().equals(oo.getClazz().getName());
    }

    /**
     * Return our JavaClass object.  This may only be called after resolve.
     */
    public JavaClass getClazz() {
	return (JavaClass) clazz;
    }

    public JavaThing[] getFields() {
	return fields;
    }

    public String toString() {
	return getClazz().getName();
    }

    public int compareTo(JavaThing other) {
	if (other instanceof JavaObject) {
	    JavaObject oo = (JavaObject) other;
	    return getClazz().getName().compareTo(oo.getClazz().getName());
	}
	return super.compareTo(other);
    }

    public void visitReferencedObjects(JavaHeapObjectVisitor v) {
	for (int i = 0; i < fields.length; i++) {
	    if (fields[i] != null) {
		if (v.mightExclude()
		    && v.exclude(getClazz().getClassForField(i), 
				 getClazz().getFieldForInstance(i)))
		{
		    // skip it
		} else if (fields[i] instanceof JavaHeapObject) {
		    v.visit((JavaHeapObject) fields[i]);
		}
	    }
	}
    }

    public boolean refersOnlyWeaklyTo(Snapshot ss, JavaThing other) {
	if (ss.getWeakReferenceClass() != null) {
	    if (ss.getWeakReferenceClass().isAssignableFrom(getClazz())) {
		// The only object type in a sun.misc.Ref is thing, and it
		// is the first field.  Similarly, in JDK 1.2, referent
		// is treated specially, and is the first thing.
		//
		// REMIND:  This introduces a dependency on the JDK 
		// 	implementation that is undesirable.
		for (int i = 1; i < fields.length; i++) {
		    if (fields[i] == other) {
			return false;
		    }
		}
		return true;
	    }
	}
	return false;
    }

    /**
     * Describe the reference that this thing has to target.  This will only
     * be called if target is in the array returned by getChildrenForRootset.
     */
    public String describeReferenceTo(JavaThing target, Snapshot ss) {
	for (int i = 0; i < fields.length; i++) {
	    if (fields[i] == target) {
		JavaField f = getClazz().getFieldForInstance(i);
		return "field " + f.getName();
	    }
	}
	return super.describeReferenceTo(target, ss);
    }

    /**
     * @return the size of this object
     */
    public int getSize() {
	if (clazz == null) {
	    return 0;
	} else {
	    return getClazz().getInstanceSize();
	}
    }
    
}
