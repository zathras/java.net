
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
 * @version     1.3, 03/06/98
 * @author      Bill Foote
 */


/**
 * Represents a stack frame.
 */

public class StackFrame {

    //
    // Values for the lineNumber data member.  These are the same
    // as the values used in the JDK 1.2 heap dump file.
    //
    public final static int LINE_NUMBER_UNKNOWN = -1;
    public final static int LINE_NUMBER_COMPILED = -2;
    public final static int LINE_NUMBER_NATIVE = -3;

    private String methodName;
    private String methodSignature;
    private String className;
    private String sourceFileName;
    private int lineNumber;

    public StackFrame(String methodName, String methodSignature,
		      String className, String sourceFileName, int lineNumber) {
	this.methodName = methodName;
	this.methodSignature = methodSignature;
	this.className = className;
	this.sourceFileName = sourceFileName;
	this.lineNumber = lineNumber;
    }

    public void resolve(Snapshot snapshot) {
    }

    public String getMethodName() {
	return methodName;
    }

    public String getMethodSignature() {
	return methodSignature;
    }

    public String getClassName() {
	return className;
    }

    public String getSourceFileName() {
	return sourceFileName;
    }

    public String getLineNumber() {
	switch(lineNumber) {
	    case LINE_NUMBER_UNKNOWN:
		return "(unknown)";
	    case LINE_NUMBER_COMPILED:
		return "(compiled method)";
	    case LINE_NUMBER_NATIVE:
		return "(native method)";
	    default:
		return Integer.toString(lineNumber, 10);
	}
    }
}
