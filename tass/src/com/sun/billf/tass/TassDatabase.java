
package com.sun.billf.tass;

import java.io.IOException;
import java.io.File;
import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Set;
import java.util.Map;
import java.util.Iterator;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.bcel.util.ClassPath;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.util.SyntheticRepository;
import org.apache.bcel.classfile.Field;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.Type;

public class TassDatabase {

    private int lastTrackingNumber = 0;
    private RunID currentRunID = new RunID(0);

    private static int FILE_MAGIC_NUMBER=0x07a55d8a; // spells "tass data"
    //
    // I write a version number at the beginning, in case I ever have
    // to extend the database type.  The first version of TASS had
    // version number 1; all subsequent versions should be written to
    // accept all versions starting from 1.
    //
    private static int FILE_VERSION_NUMBER = 1;

    // HashMap<String, HashSet<RunID>> where the key is the fully-qualified
    // class name.
    private HashMap classReferences = new HashMap();

    // HashMap<String, HashSet<RunID>> where the key is the 
    // fully-qualified field name
    private HashMap fieldReferences = new HashMap();

    // HashMap<MethodReference, HashSet<RunID>> where the key is the 
    // a MethodReference.
    private HashMap methodReferences = new HashMap();

    // If the platform classes are added, this will hold the RunID
    // representing the platform classes.
    private RunID platformRunID = null;

    //
    // A map for platform methods from MethodReference
    // to the JavaClass where it was declared in the platform.
    // It's only set for methods that aren't static, and
    // aren't constructors.
    //
    private HashMap methodReferenceToClass = new HashMap();

    // See getPackagesUsed
    private HashMap packagesUsed = null;

    public TassDatabase() {
    }

    private RunID readRunID(DataInputStream in) throws IOException {
	int len = in.readInt();
	int[] ids = new int[len];
	for (int i = 0; i < ids.length; i++) {
	    ids[i] = in.readInt();
	}
	return new RunID(ids);
    }

    private HashMap readHashMap(DataInputStream in, boolean isMethodSet)
    			throws IOException
    {
	HashMap result = new HashMap();
	int len = in.readInt();
	for (int i = 0; i < len; i++) {
	    Object key;
	    if (isMethodSet) {
		key = readMethodReference(in);
	    } else {
		key = in.readUTF();
	    }
	    int len2 = in.readInt();
	    Set val = new HashSet();
	    for (int j = 0; j < len2; j++) {
		val.add(readRunID(in));
	    }
	    result.put(key, val);
	}
	return result;
    }

    private MethodReference readMethodReference(DataInputStream in) 
    			throws IOException
    {
	String fqMethodName = in.readUTF();
	String[] args = new String[in.readInt()];
	for (int i = 0; i < args.length; i++) {
	    args[i] = in.readUTF();
	}
	return new MethodReference(fqMethodName, args);
    }

    public void init(String fileName, boolean createIfNeeded) throws IOException
    {
	File f = new File(fileName);
	if (!f.exists()) {
	    if (!createIfNeeded) {
		throw new IOException(fileName + " no found.");
	    }
	    return;
	}
	DataInputStream in = new DataInputStream(new BufferedInputStream(
				new FileInputStream(fileName)));
	int magic = in.readInt();
	if (magic != FILE_MAGIC_NUMBER) {
	    throw new IOException("Bad magic number.  This must not be a TASS database file.");
	}
	int ver = in.readInt();
	if (ver != FILE_VERSION_NUMBER) {
	    throw new IOException("Version number " + ver + " unrecognized.");
	}
	lastTrackingNumber = in.readInt();
	currentRunID = new RunID(lastTrackingNumber);
	classReferences = readHashMap(in, false);
	fieldReferences = readHashMap(in, false);
	methodReferences = readHashMap(in, true);
    }

    private void writeRunID(DataOutputStream out, RunID rid) throws IOException
    {
	out.writeInt(rid.ids.length);
	for (int i = 0; i < rid.ids.length; i++) {
	    out.writeInt(rid.ids[i]);
	}
    }

    private void writeHashMap(DataOutputStream out, HashMap map, 
    			      boolean isMethodSet) throws IOException
    {
	out.writeInt(map.size());
	Set entries = map.entrySet();
	for (Iterator it = entries.iterator(); it.hasNext(); ){
	    Map.Entry entry = (Map.Entry) it.next();
	    if (isMethodSet) {
		writeMethodReference(out, (MethodReference) entry.getKey());
	    } else {
		out.writeUTF((String) entry.getKey());
	    }
	    Set ids = (Set) entry.getValue();
	    out.writeInt(ids.size());
	    for (Iterator it2 = ids.iterator(); it2.hasNext(); ) {
		RunID rid = (RunID) it2.next();
		writeRunID(out, rid);
	    }
	}
    }

    private void writeMethodReference(DataOutputStream out, MethodReference mr)
    			throws IOException
    {
	out.writeUTF(mr.fqMethodName);
	out.writeInt(mr.argName.length);
	for (int i = 0; i < mr.argName.length; i++) {
	    out.writeUTF(mr.argName[i]);
	}
    }

    public void write(String fileName) throws IOException {
	DataOutputStream out = new DataOutputStream(new BufferedOutputStream(
				new FileOutputStream(fileName)));
	out.writeInt(FILE_MAGIC_NUMBER);
	out.writeInt(FILE_VERSION_NUMBER);
	out.writeInt(lastTrackingNumber);
	writeHashMap(out, classReferences, false);
	writeHashMap(out, fieldReferences, false);
	writeHashMap(out, methodReferences, true);
	out.close();
	System.out.println("Written to " + fileName);
    }

    public int nextTrackingNumber() {
	lastTrackingNumber++;
	currentRunID = new RunID(lastTrackingNumber);
	return lastTrackingNumber;
    }

    public RunID getCurrentRunID() {
	return currentRunID;
    }

    private void addReference(HashMap map, Object key) {
	HashSet val = (HashSet) map.get(key);
	if (val == null) {
	    val = new HashSet();
	    map.put(key, val);
	}
	val.add(currentRunID);
    }

    public void addClassReference(String className) {
	addReference(classReferences, className);
    }

    public void addFieldReference(String fqFieldName) {
	addReference(fieldReferences, fqFieldName);
    }

    public MethodReference addMethodReference(String fqMethodName, 
    					      String[] argTypes) {
	MethodReference mr = new MethodReference(fqMethodName, argTypes);
	addReference(methodReferences, mr);
	return mr;
    }

    /**
     * @return a  HashMap<String, HashSet<RunID>> where the key is the 
     * fully-qualified class name
     **/
    public Map getClassReferences() {
	return classReferences;
    }

    /**
     * @return a Map<String, HashSet<RunID>> where the key is the 
     * fully-qualified field name
     **/
    public Map getFieldReferences() {
	return fieldReferences;
    }

    /**
     * @return a Map<Method Reference, Set<RunID>>
     **/
    public Map getMethodReferences() {
	return methodReferences; 
    }

    /**
     * @return a Map<String, Set<RunID>> where the key is the name of a
     * 	       package
     **/
    public synchronized Map getPackagesUsed() {
	if (packagesUsed != null) {
	    return packagesUsed;
	}
	packagesUsed = new HashMap();
	for (Iterator it = classReferences.entrySet().iterator(); it.hasNext();)
	{
	    Map.Entry entry = (Map.Entry) it.next();
	    String className = (String) entry.getKey();
	    Set newVal = (Set) entry.getValue();
	    String packageName = Utils.stripAfterLastDot(className);;
	    Set oldVal = (Set) packagesUsed.get(packageName);
	    if (oldVal == null) {
		packagesUsed.put(packageName, new HashSet(newVal));
	    } else {
		for (Iterator it2 = newVal.iterator(); it2.hasNext(); ) {
		    oldVal.add(it2.next());
		}
	    }
	}

	return packagesUsed;
    }

    private void mergeMap(Map parent, Map child) {
	for (Iterator it = child.entrySet().iterator(); it.hasNext(); ) {
	    Map.Entry entry = (Map.Entry) it.next();
	    Set old = (Set) entry.getValue();
	    HashSet newVal = new HashSet();
	    for (Iterator it2 = old.iterator(); it2.hasNext(); ) {
		RunID oldR = (RunID) it2.next();
		newVal.add(new RunID(currentRunID, oldR));
	    }
	    Set parentValue = (Set) parent.get(entry.getKey());
	    if (parentValue != null) {
		for (Iterator it2 = parentValue.iterator(); it2.hasNext(); ) {
		    newVal.add(it2.next());
		}
	    }
	    parent.put(entry.getKey(), newVal);
	}
    }

    public void addFrom(TassDatabase child) {
	mergeMap(classReferences, child.classReferences);
	mergeMap(fieldReferences, child.fieldReferences);
	mergeMap(methodReferences, child.methodReferences);
    }

    /**
     * Add the platform classes to this TASS database.  A RunID is
     * allocated to represent the platform.
     Initialize this reporter with the set of platfrom classes.  If this
     * is not called, then certain elements of the report won't be
     * present.
     **/
    public void addPlatform(String platformZipFileName) throws IOException {
	nextTrackingNumber();
	platformRunID = getCurrentRunID();
	ClassPath cp = new ClassPath(platformZipFileName);
	SyntheticRepository platformClasses 
		= SyntheticRepository.getInstance(cp);
	ZipFile pfz = new ZipFile(platformZipFileName);
	for (Enumeration e = pfz.entries(); e.hasMoreElements();) {
	    ZipEntry ze = (ZipEntry) e.nextElement();
	    String nm = ze.getName();
	    if (nm.endsWith(".class")) {
		nm = nm.substring(0, nm.length() - 6);
		nm = nm.replace('/', '.');
		try {
		    JavaClass cl = platformClasses.loadClass(nm);
		    addPlatformClass(cl);
		} catch (ClassNotFoundException ex) {
		    throw new IOException("Error in platform zip:  " + ex);
		}
	    }
	} 
    }

    private void addPlatformClass(JavaClass cl) throws IOException  {
	// assert cl != null
	if (cl.isPublic() || cl.isProtected()) {
	    String cn = cl.getClassName();
	    addClassReference(cn);
	    Field[] fields = cl.getFields();
	    for (int i = 0; i < fields.length; i++) {
		if (fields[i].isPublic() || fields[i].isProtected()) {
		    addFieldReference(cn + "." + fields[i].getName());
		}
	    }
	    Method[] methods = cl.getMethods();
	    for (int i = 0; i < methods.length; i++) {
		Method m = methods[i];
		if (m.isPublic() || m.isProtected()) {
		    String mn = cn + "." + m.getName();
		    Type[] mar = m.getArgumentTypes();
		    String[] mars = new String[mar.length];
		    for (int j = 0; j < mar.length; j++) {
			mars[j] = mar[j].toString();
		    }
		    MethodReference mr = addMethodReference(mn, mars);
		    if (!(m.isStatic() || "<init>".equals(m.getName()))) {
			methodReferenceToClass.put(mr, cl);
		    }
		}
	    }
	}
    }


    /**
     * @return  The RunID for platform classes, or null if they weren't
     *		added to this database.
     **/
    public RunID getPlatformRunID() {
	return platformRunID;
    }

    /**
     * @return the JavaClass that contains the given MethodReference
     **/
    public JavaClass getJavaClass(MethodReference mr) {
	return (JavaClass) methodReferenceToClass.get(mr);
    }

}
