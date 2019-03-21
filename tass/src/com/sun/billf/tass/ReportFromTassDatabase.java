
package com.sun.billf.tass;

import java.io.IOException;
import java.io.PrintStream;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.Enumeration;

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
 * This class reports on the APIs used by the set of applications reflected
 * within a TassDatabase.  It generates a report of how many applications
 * use each package, each class, and each class member.  It just needs to
 * be invoked with with the name of a .tdb TASS database file, and it
 * will always create all three reports.
 * <p>
 * Optionally, this program can also be invoked with a .zip file
 * containing the .class files that define a platform.  If this is
 * given, the reports will indicate if references are to platform-defined
 * elements or not.  Further, an indication of references to a superclass
 * definition of a method is given.
 * <p>
 * See the usage message for details on command-line arguments.
 **/
public class ReportFromTassDatabase {

    /**
     * The code to run a report is written generically, to iterate
     * over a Map of elements.  When run, an instance of this functor
     * lets the reporting method interpret key values.
     **/
    private interface KeyInterpreter extends Comparator {

	String printString(Object key);

	void sectionBreak(Object last, Object current);

	/**
	 * Get the set of run IDs that contain a reference to key,
	 * including key's declarations in superclasses.  This only
	 * makes sense for methods, and can only be calculated if
	 * the platform has been read in.
	 **/
	Set getSuperReferences(Object key);

	boolean supportsSuperReferences();
    }

    private TassDatabase db;

    public ReportFromTassDatabase(TassDatabase db) {
	this.db = db;
    }

    private void reportUsage(Map elements, KeyInterpreter keyInterpreter)
    {
	Object[] keys = elements.keySet().toArray();
	Arrays.sort(keys, keyInterpreter);
	Object last = null;
	for (int i = 0; i < keys.length; i++) {
	    keyInterpreter.sectionBreak(last, keys[i]);
	    last = keys[i];
	    Set val = (Set) elements.get(keys[i]);
	    int count = val.size();
	    boolean notInPlatform = false;
	    if (db.getPlatformRunID() != null)  {
		if (val.contains(db.getPlatformRunID())) {
		    count--;
		} else {
		    notInPlatform = true;
		}
	    }
	    String str = "" + count;
	    if (keyInterpreter.supportsSuperReferences()) {
		if (str.length() < 9) {
		    str = "         ".substring(str.length()) + str; 
		}
		Set supRefs = keyInterpreter.getSuperReferences(keys[i]);
		String str2;
		if (supRefs == null) {
		    str2 = "        ";
		} else {
		    count = supRefs.size();
		    if (db.getPlatformRunID() != null 
		    	&& supRefs.contains(db.getPlatformRunID())) 
		    {
			count--;
		    }
		    str2 = "(s" + count + ")";
		    if (str2.length() < 8) {
			str2 = "        ".substring(str2.length()) + str2;
		    }
		}
		str = str + str2 + ":  ";
	    } else {
		str = str + ":  ";
		if (str.length() < 12) {
		    str = "            ".substring(str.length()) + str; 
		}
	    }
	    System.out.print(str);
	    str = keyInterpreter.printString(keys[i]);
	    if (notInPlatform) {
		str = "(" + str + " - not in platform)";
	    }
	    System.out.println(str);
	}
    }

    public void reportPackageUsage() {
	System.out.println("");
	System.out.println("************************************************");
	System.out.println("        REPORT OF PLATFORM PACKAGE USAGE        ");
	System.out.println("************************************************");
	System.out.println("");
	System.out.println("");
	reportUsage(db.getPackagesUsed(), new KeyInterpreter() {

	    public int compare(Object o1, Object o2) {
		return o1.toString().compareToIgnoreCase(o2.toString());
	    }

	    public String printString(Object key) {
		return (String) key;
	    }

	    public void sectionBreak(Object last, Object current) {
	    }

	    public Set getSuperReferences(Object key) {
		return null;
	    }
	    
	    public boolean supportsSuperReferences() {
		return false;
	    }

	});
	System.out.println("");
	System.out.println("");
    }

    public void reportClassUsage() {
	System.out.println("");
	System.out.println("**********************************************");
	System.out.println("        REPORT OF PLATFORM CLASS USAGE        ");
	System.out.println("**********************************************");
	System.out.println("");
	System.out.println("");
	reportUsage(db.getClassReferences(), new KeyInterpreter() {

	    public int compare(Object o1, Object o2) {
		String s1 = (String) o1;
		String s2 = (String) o2;
		// Make sure package names get grouped...
		s1 = Utils.stripAfterLastDot(s1) + " " + s1;
		s2 = Utils.stripAfterLastDot(s2) + " " + s2;
		return s1.compareToIgnoreCase(s2);
	    }

	    public String printString(Object key) {
		return (String) key;
	    }

	    public void sectionBreak(Object last, Object current) {
		String p1 = "";
		if (last != null) {
		    p1 = Utils.stripAfterLastDot((String) last);
		}
		String p2 = Utils.stripAfterLastDot((String) current);
		if (!p1.equals(p2)) {
		    if (last != null) {
			System.out.println();
		    }
		    System.out.println("Package " + p2);
		    System.out.println();
		}
	    }

	    public Set getSuperReferences(Object key) {
		return null;
	    }
	    
	    public boolean supportsSuperReferences() {
		return false;
	    }

	});
	System.out.println("");
	System.out.println("");
    }

    public void reportMemberUsage() {
	System.out.println("");
	System.out.println("***********************************************");
	System.out.println("        REPORT OF PLATFORM MEMBER USAGE        ");
	System.out.println("***********************************************");
	System.out.println("");
	System.out.println("");
	HashMap members = new HashMap(db.getMethodReferences());
	members.putAll(db.getFieldReferences());
	reportUsage(members, new KeyInterpreter() {

	    private String sortString(Object obj) {
		if (obj instanceof String) {	// Field
		    String f = (String) obj;
		    String pkg = Utils.stripAfterLastDot(f);
		    pkg = Utils.stripAfterLastDot(pkg);
		    return pkg + " " + f;
		} else {	// Method
		    MethodReference m = (MethodReference) obj;
		    int pos = m.fqMethodName.lastIndexOf(".");
		    String className = m.fqMethodName.substring(0, pos);
		    String methodName = m.fqMethodName.substring(pos+1);
		    String pkg = Utils.stripAfterLastDot(className);
		    return pkg + " " + className + "~()" + methodName
		    	   + m.getArgsAsString(false);
		}
	    }

	    public int compare(Object o1, Object o2) {
		String s1 = sortString(o1);
		String s2 = sortString(o2);
		// Make sure package names get grouped, and fields
		// come before methods.
		return s1.compareToIgnoreCase(s2);
	    }

	    public String printString(Object key) {
		if (key instanceof String) {
		    return (String) key;
		} else {
		    MethodReference m = (MethodReference) key;
		    return m.toString(false);
		}
	    }

	    private String className(Object key) {
		if (key instanceof String) {
		    return Utils.stripAfterLastDot((String) key);
		} else {
		    MethodReference m = (MethodReference) key;
		    return Utils.stripAfterLastDot(m.fqMethodName);
		}
	    }


	    public void sectionBreak(Object last, Object current) {
		String lastClass = "";
		String lastPkg = "";
		if (last != null) {
		    lastClass = className(last);
		    lastPkg = Utils.stripAfterLastDot(lastClass);
		}
		String thisClass = className(current);
		String thisPkg = Utils.stripAfterLastDot(thisClass);
		boolean blankb4 = false;
		if (!lastPkg.equals(thisPkg)) {
		    if (last != null) {
			System.out.println();
		    }
		    System.out.println("Package " + thisPkg);
		    System.out.println();
		    blankb4 = true;
		}
		if (!lastClass.equals(thisClass)) {
		    if (!blankb4) {
			System.out.println();
		    }
		    System.out.println("  Class " + thisClass);
		    System.out.println();
		}
	    }

	    public Set getSuperReferences(Object key) {
		if (!(key instanceof MethodReference)) {
		    return null;
		}
		MethodReference mr = (MethodReference) key;
		JavaClass cl = (JavaClass) db.getJavaClass(mr);
		if (cl== null) {
		    return null;
		}
		HashSet result = new HashSet();
		int pos = mr.fqMethodName.lastIndexOf(".");
		String methodName = mr.fqMethodName.substring(pos+1);
		JavaClass[] cla = { cl };
		addRefs(methodName, mr.argName, cla, result);
		addRefs(methodName, mr.argName, cl.getAllInterfaces(), result);
		addRefs(methodName, mr.argName, cl.getSuperClasses(), result);
		return result;
	    }

	    private void addRefs(String methodName, String[] argName,
	    			 JavaClass[] types, Set result) 
	    {
		for (int i = 0; i < types.length; i++) {
		    String cn = types[i].getClassName();
		    MethodReference mr 
		        = new MethodReference(cn + "." + methodName, argName);
		    Set val = (Set) db.getMethodReferences().get(mr);
		    if (val != null) {
			for (Iterator it = val.iterator(); it.hasNext(); ) {
			    result.add(it.next());
			}
		    }
		}
	    }
	    
	    public boolean supportsSuperReferences() {
		return true;
	    }

	});
	System.out.println("");
	System.out.println("");
	System.out.println("NOTE:  Non-static methods show a second reference count, like \"(s12)\".  This");
	System.out.println("       is the count of references to this method, including any references");
	System.out.println("       to a declaration of this method in a superclass or an implemented");
	System.out.println("       interface.  Such references mean this method might be called; futher");
	System.out.println("       analysis is required to determine if it acutally is.");
	System.out.println("");
	System.out.println("NOTE:  A valid application may contain a method reference identified as being");
	System.out.println("       not a part of the platform in this report, because the application was");
	System.out.println("       built against a platform library that contained a method override ");
	System.out.println("       For example, a valid application might contain a");
	System.out.println("       reference to java.util.HashSet.toArray(Object[]), even though the");
	System.out.println("       method is declared inthe superclass java.util.Set, if the application");
	System.out.println("       is built against a library where HashSet overrides this method.");
	System.out.println("");
	System.out.println("");
    }

    public static void usage() {
	System.out.println();
	System.out.println("Usage:  java com.sun.billf.tass.ReportFromTaassDatabase \\");
	System.out.println("            <.tdb database file> [<platform classes.zip>] \\");
	System.out.println();
	System.out.println("This generates a report of platform packages, classes and class members.");
	System.out.println("Each of these is shown with the number of different applications that use it.");
	System.out.println();
    }


    public static void main(String[] args) {
	if (args.length != 2 && args.length != 1) {
	    usage();
	    System.exit(1);
	}
	TassDatabase db = new TassDatabase();
	ReportFromTassDatabase r = new ReportFromTassDatabase(db);
	try {
	    db.init(args[0], false);
	    if (args.length == 2) {
		r.db.addPlatform(args[1]);
	    }
	} catch (IOException ex) {
	    ex.printStackTrace(System.err);
	    System.exit(1);
	}
	r.reportPackageUsage();
	r.reportClassUsage();
	r.reportMemberUsage();
	System.exit(0);
    }
}
