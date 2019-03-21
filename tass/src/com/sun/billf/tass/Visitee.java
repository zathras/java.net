
package com.sun.billf.tass;

import org.apache.bcel.generic.Type;

interface Visitee {

    /**
     * Check a named class.  This is called when a reference to a
     * named class is encountered.
     **/
    void checkForClass(String name);

    /**
     * Check a type.  This is called when a reference to a type
     * is encountered.  The type might be UninitializedObjectType,
     * and array type, or an object type (a class).
     *
     * @see #checkForClass(String)
     **/
    void checkType(Type t);

    /**
     * Called when some kind of error is encountered.
     **/
    void reportError(String description);

    /**
     * Check a reference to a field.  Called when a field reference
     * is encountered in the application being checked.
     **/
    void checkFieldRef(String className, String fieldName);

    /**
     * Check a reference to a method.  Called when a method reference
     * is encountered in the application being checked.
     **/
    void checkMethodRef(String className, String methodName, Type[] args);

}
