
package com.sun.billf.tass;

import java.io.IOException;
import java.io.PrintStream;
import java.io.InputStream;
import java.util.zip.ZipFile;
import java.util.zip.ZipEntry;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Stack;

import org.apache.bcel.util.ClassPath;
import org.apache.bcel.util.SyntheticRepository;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.ClassParser;
import org.apache.bcel.classfile.ClassFormatException;
import org.apache.bcel.classfile.Field;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.classfile.Visitor;
import org.apache.bcel.generic.Type;
import org.apache.bcel.verifier.structurals.UninitializedObjectType;
import org.apache.bcel.generic.ArrayType;
import org.apache.bcel.generic.ObjectType;


/**
 * This class is the entry point to the TASS checking module.
 * TASS stands for "Test Application Signature Safety," and
 * it brings you the news about an application's conformance
 * to platform API signature rules.  
 * <p>
 * This tool inspects appliation classes one at a time.  For
 * each class, it considers all external references to API
 * elements:  classes, methods, fields, etc.  For each external
 * reference, it checks if it's in one of the platform packages,
 * that is, if it's in a package or a subpackage of one of the
 * supplied platform package names..
 * If it isn't, it ignores it; this tool doesn't try to check
 * applictions for appliction bugs like this.
 * <p>
 * If an external reference is to a platform class, this tool checks
 * it.  It checks the referred-to element against the platform class
 * file stubs, provided.  If the referred-to element is there, then
 * checking proceeds.  If, however, the element isn't present, an
 * error message is (optionally) created, and an error is indicated.
 *
 *
 *	@author		Bill Foote	bill.foote@sun.com
 **/

public class TassChecker implements Visitee {

    //
    // Where error messages get sent
    //
    private PrintStream	errOut = null;

    //
    // Number of errors so far
    //
    private int numErrors = 0;

    //
    // The platform package names.  These packages and their
    // subpackages are checked.
    //
    private String[] platformPackages;

    //
    // The repository of platform classes
    //
    private SyntheticRepository platformClasses;

    //
    // The maximum class file version #
    //
    private int maxClassFileVersionMajor;
    private int maxClassFileVersionMinor;

    //
    // Our visitor - it's a helper, used to visit the nodes
    // of a JavaClass for us.
    //
    private Visitor visitor;

    //
    // Flag set true if a visit traversal sees an error
    //
    private boolean errorSeenThisVisit;

    //
    // Name of the class being visited
    //
    private String classBeingVisited;

    //
    // All the errors we've seen in this class.  This is used
    // to screen out duplicates.
    //
    private HashSet errorsThisClass;

    //
    // Where the platform classes were read from
    //
    private String platformZipFileName;

    /**
     * Create a new TassChecker.  It must be initialized before
     * use.
     *
     * @see #init
     **/
    public TassChecker() {
	this.visitor = new TassVisitor(this);
    }

    /** 
     * Initialize a TassChecker
     *
     * @param	stubsZipFile	The name of the .zip file olding the platform
     *				stubs
     * @param	platformPackages  The string names of the platform packages
     * @param   maxClassFileVersion  The maximum acceptable class file version
     *				     for application classes, e.g.
     *				     "47.0" for PBP 1.0.
     * @throws IOException	If on occurs reading the stubs
     * @throws IllegalArgumentException   if maxClassfileVersion can't be parsed
     **/
    public void init(String stubsZipFile, String[] platformPackages,
    		     String maxClassFileVersion) 
	    throws IOException
    {
	this.platformPackages = platformPackages;
	ClassPath cp = new ClassPath(stubsZipFile);
	this.platformZipFileName = stubsZipFile;
	platformClasses = SyntheticRepository.getInstance(cp);
	int pos = maxClassFileVersion.indexOf('.');
	if (pos < 0) {
	    throw new IllegalArgumentException(maxClassFileVersion);
	}
	String s = maxClassFileVersion.substring(0, pos);
	maxClassFileVersionMajor = Integer.parseInt(s);
	s = maxClassFileVersion.substring(pos+1);
	maxClassFileVersionMinor = Integer.parseInt(s);
    }

    /**
     * Supply a place to send error messages.  If left unset, error
     * messages will not be generated.
     *
     * @param	out	The output stream
     **/
    public void setErrorOutput(PrintStream out) {
	errOut = out;
    }

    private void checkClassVersion(JavaClass cl) {
	boolean bad;
	int maj = cl.getMajor();
	int minor = cl.getMinor();
	if (maj < maxClassFileVersionMajor) {
	    bad = false;
	} else if (maj > maxClassFileVersionMajor) {
	    bad = true;
	} else {
	    bad = minor > maxClassFileVersionMinor;
	}
	if (bad) {
	    reportError("Class file version number " + maj + "." + minor
			+ " is greater than maximum " 
			+ maxClassFileVersionMajor + "."
			+ maxClassFileVersionMinor);
	}
    }

    /**
     * Check if the .class in the give file is OK.  If an error
     * is found, emits one or more messages to the error output 
     * stream, and returns false.  Returns true if no errors found.
     * 
     * @see	#setErrorOutput(java.io.PrintStream)
     *
     * @param fileName	The name of the .class file
     **/
    public boolean classIsOK(String fileName) throws IOException {
	ClassParser cp = new ClassParser(fileName);
	JavaClass cl;
	try {
	    cl = cp.parse();
	} catch (ClassFormatException ex) {
	    reportError("Error parsing " + fileName + ":  " + ex);
	    return false;
	}
	errorSeenThisVisit = false;
	classBeingVisited = cl.getClassName();
	errorsThisClass = new HashSet();

	checkClassVersion(cl);

	cl.accept(visitor);
	if (errorSeenThisVisit) {
	    return false;
	} else {
	    return true;
	}
    }


    /**
     * Check if the .class files in the given archive are OK.  If an error
     * is found, emits one or more messages to the error output 
     * stream, and returns false.  Returns true if no errors found.
     * 
     * @see	#setErrorOutput(java.io.PrintStream)
     *
     * @param fileName	The name of the zip archive containing
     *			.class files
     **/
    public boolean zipIsOK(String fileName) throws IOException {
	ZipFile classesFile = new ZipFile(fileName);
	errorSeenThisVisit = false;
	for (Enumeration e = classesFile.entries(); e.hasMoreElements();) {
	    ZipEntry ze = (ZipEntry) e.nextElement();
	    String nm = ze.getName();
	    if (nm.endsWith(".class")) {
		InputStream is = classesFile.getInputStream(ze);
		ClassParser cp = new ClassParser(is, nm);
		JavaClass cl;
		try {
		    cl = cp.parse();
		} catch (ClassFormatException ex) {
		    reportError("Error parsing " + nm + " in " + fileName 
				 + ":  " + ex);
		    return false;
		}
		classBeingVisited = cl.getClassName();
		errorsThisClass = new HashSet();
		checkClassVersion(cl);
		cl.accept(visitor);
	    }
	}
	if (errorSeenThisVisit) {
	    return false;
	} else {
	    return true;
	}
    }

    /**
     * Return the total number of errors seen
     *
     * @return the number of errors
     **/
    public int getNumErrors() {
	return numErrors;
    }

    //
    // Report an error to the error output, if there haven't
    // been too many.
    //
    /**
     * Internal Visitee implementation method
     **/
    public void reportError(String msg) {
	errorSeenThisVisit = true;
	if (!(errorsThisClass.add(msg))) {	// Screen out dups
	    return;
	}
	numErrors++;
	if (errOut == null) {
	    return;
	} else if (numErrors == 101) {
	    errOut.println("More than 100 errors seen.  Ignoring subsequent errors.");
	} else if (numErrors <= 100) {
	    errOut.println("Error in " + classBeingVisited + ":  " + msg);
	}
    }

    private boolean isInPlatformPackage(String name) {
	for (int i = 0; i < platformPackages.length; i++) {
	    if (name.startsWith(platformPackages[i])) {
		return true;
	    }
	}
	return false;
    }

    //
    // Check for the existance of a class, and return it.
    // If not a platfrom class or not found, return null.
    // If a platform class and not found, also report error.
    private JavaClass checkClass(String name) {
	if (!isInPlatformPackage(name)) {
	    return null;
	}
	try {
	    JavaClass cl = platformClasses.loadClass(name);
	    // assert cl != null
	    if (!cl.isPublic()) {
		reportError("Platform class " + name + " is not public.");
		return null;
	    }
	    return cl;	// Found it
	} catch (ClassNotFoundException ex) {
	    reportError("Platform class " + name + " not found.");
	    return null;
	}
    }

    //
    // Check to see if a given class causes a problem.  If so, report
    // it, and set the error flag.  This is called from TassVisitor when
    // it sees a reference to the named class.
    //
    /**
     * Internal Visitee implementation method
     **/
    public void checkForClass(String name) {
	checkClass(name);
    }

    //
    // Check that a Type is valid
    //
    /**
     * Internal Visitee implementation method
     **/
    public void checkType(Type t) {
	if (t instanceof UninitializedObjectType) {
	    checkType(((UninitializedObjectType) t).getInitialized());
	} else if (t instanceof ArrayType) {
	    checkType(((ArrayType) t).getElementType());
	} else if (t instanceof ObjectType) {
	    checkForClass(((ObjectType) t).getClassName());
	}
    }

    //
    // Check that a reference to a field is valid
    //
    /**
     * Internal Visitee implementation method
     **/
    public void checkFieldRef(String className, String fieldName) {
	Stack toCheck = new Stack();
	JavaClass c = checkClass(className);
	if (c == null) {
	    // Either an error was reported, or it's not a system class.
	    // Either way, our job is done.
	    return;
	}
	toCheck.push(c);
	while (!toCheck.empty()) {
	    c = (JavaClass) toCheck.pop();
	    Field[] fields = c.getFields();
	    for(int i=0; i < fields.length; i++) {
		if (fields[i].getName().equals(fieldName)) {
		    if (!fields[i].isPublic() && !fields[i].isProtected()) {
			reportError("Platform Field " + className + "." 
				    + fieldName + 
				    " is not exposed to applications.");
		    }
		    return;
		}
	    }
	    JavaClass[] ifs = c.getInterfaces();
	    for (int i = 0; i < ifs.length; i++) {
		toCheck.push(ifs[i]);
	    }
	    if (!("java.lang.Object".equals(c.getClassName()))) {
		toCheck.push(c.getSuperClass());
	    }
	}
	reportError("Platform field " + className + "." + fieldName 
		    + " not found.");
	return;
    }

    private String argsAsString(Type[] args) {
	String argsAsString = "(";
	for (int j = 0; j < args.length; j++) {
	    if (j > 0) {
		argsAsString += ", ";
	    }
	    argsAsString += args[j].toString();
	}
	argsAsString += ")";
	return argsAsString;
    }

    private boolean methodMatches(Method m, String name, Type[] args) {
	if (!(m.getName().equals(name))) {
	    return false;
	}
	Type[] mat = m.getArgumentTypes();
	if (mat.length != args.length) {
	    return false;
	}
	for (int i = 0; i < args.length; i++) {
	    if (!(args[i].getSignature().equals(mat[i].getSignature()))) {
		return false;
	    }
	}
	return true;
    }

    //
    // Check that a reference to a method is valid
    //
    /**
     * Internal Visitee implementation method
     **/
    public void checkMethodRef(String className, String methodName, Type[] args)
    {
	Stack toCheck = new Stack();
	JavaClass c = checkClass(className);
	if (c == null) {
	    // Either an error was reported, or it's not a system class.
	    // Either way, our job is done.
	    return;
	}
	toCheck.push(c);
	while (!toCheck.empty()) {
	    c = (JavaClass) toCheck.pop();
	    Method[] methods = c.getMethods();
	    for(int i=0; i < methods.length; i++) {
		if (methodMatches(methods[i], methodName, args)) {
		    if (!methods[i].isPublic() && !methods[i].isProtected()) {
			reportError("Platform method " + className + "." 
				    + methodName + argsAsString(args) + 
				    " is not exposed to applications.");
		    }
		    return;
		}
	    }
	    JavaClass[] ifs = c.getInterfaces();
	    for (int i = 0; i < ifs.length; i++) {
		toCheck.push(ifs[i]);
	    }
	    if (!("java.lang.Object".equals(c.getClassName()))) {
		toCheck.push(c.getSuperClass());
	    }
	}
	reportError("Platform method " + className + "." + methodName 
		    + argsAsString(args) + " not found.");
	return;
    }

}
