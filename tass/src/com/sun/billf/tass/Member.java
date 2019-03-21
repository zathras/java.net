
package com.sun.billf.tass;

import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.FieldOrMethod;
import org.apache.bcel.classfile.Field;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.Type;

import java.util.HashSet;

public class Member {

    public JavaClass cl;
    public FieldOrMethod member;

    public Member(JavaClass cl, FieldOrMethod member) {
	this.cl = cl;
	this.member = member;
    }

    public int hashCode() {
	return toString().hashCode();
    }

    public boolean equals(Object other) {
	return toString().equals(other.toString());
    }

    public String toString() {
	return cl.getClassName() + "." + baseToString(true);
    }

    /** 
     * @return  The string name of the "base" member, without the
     *		class name prepended, and with shortened argument names.
     **/
    public String baseToString() {
	return baseToString(false);
    }

    private String baseToString(boolean unique) {
	if (member instanceof Field) {
	    return member.getName();
	} else {
	    Method meth = (Method) member;
	    String result = member.getName() + "(";
	    Type[] args = meth.getArgumentTypes();
	    for (int i = 0; i < args.length; i++) {
		if (i > 0) {
		    result = result + ", ";
		}
		String type = args[i].toString();

		//
		// Here, we reduce the fully-qualified class name to
		// just the base class name.  It's possible that this
		// will make the name ambiguous, but it's much more
		// readable this way.
		//
		if (!unique) {
		    int pos = type.lastIndexOf('.');
		    if (pos > -1) {
			type = type.substring(pos+1);
		    }
		}
		result = result + type;
	    }
	    result = result + ")";
	    return result;
	}
    }

}
