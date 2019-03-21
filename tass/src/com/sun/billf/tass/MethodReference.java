
package com.sun.billf.tass;

//
// A method reference within a TassDatabase
//
public class MethodReference {

    String fqMethodName;
    String[] argName;

    MethodReference(String fqMethodName, String[] argName) {
	this.fqMethodName = fqMethodName;
	this.argName = argName;
    }

    public int hashCode() {
	return toString().hashCode();
    }

    public boolean equals(Object other) {
	if (other instanceof MethodReference) {
	    MethodReference mro = (MethodReference) other;
	    return toString().equals(mro.toString());
	}
	return false;
    }

    public String toString() {
	return toString(true);
    }

    public String toString(boolean fullyQualifyArgs) {
	return fqMethodName + getArgsAsString(fullyQualifyArgs);
    }

    public String shortName() {
	int pos = fqMethodName.lastIndexOf('.');
	return fqMethodName.substring(pos+1)  + getArgsAsString(false);
    }

    public String getClassName() {
	return Utils.stripAfterLastDot(fqMethodName);
    }
    
    public String getArgsAsString(boolean fullyQualifyArgs) {
	String result = "(";
	for (int i = 0; i < argName.length; i++) {
	    if (i > 0) {
		result = result + ", ";
	    }
	    if (fullyQualifyArgs) {
		result = result + argName[i];
	    } else {
		String an = argName[i];
		int pos = an.lastIndexOf('.');
		if (pos >= 0) {
		    an = an.substring(pos + 1);
		}
		result = result + an;
	    }
	}
	result = result + ")";
	return result;
    }

}
