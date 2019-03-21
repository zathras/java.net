
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
 * @version     1.7, 10/08/98
 * @author      Bill Foote
 */

/**
 * Represents the value of a static field of a JavaClass
 */

public class JavaStatic {

    private JavaField field;
    private JavaThing value;

    public JavaStatic(JavaField field, JavaThing value) {
	this.field = field;
	this.value = value;
    }

    public void resolve(JavaClass clazz, Snapshot snapshot) {
	value = value.dereference(snapshot, field);
	if (value.isHeapAllocated()) {
	    JavaHeapObject ho = (JavaHeapObject) value;
	    String s = "Static reference from " + clazz.getName()
		       + "." + field.getName();
	    snapshot.addRoot(new Root(ho.getId(), clazz.getId(), 
				      Root.JAVA_STATIC, s));
	}
    }

    public JavaField getField() {
	return field;
    }

    public JavaThing getValue() {
	return value;
    }
}
