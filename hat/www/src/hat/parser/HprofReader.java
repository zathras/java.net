
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

package hat.parser;

import java.io.InputStream;
import java.io.FileInputStream;
import java.io.BufferedInputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.EOFException;
import java.util.Date;
import java.util.Hashtable;

import hat.model.*;

/**
 * Object that's used to read a JDK 1.2 hprof file.
 *
 * @version     1.20, 06/03/99
 * @author      Bill Foote
 */

public class HprofReader extends Reader {

    final static int MAGIC_NUMBER = 0x4a415641;	
    // That's "JAVA", the first part of "JAVA PROFILE ..."
    private final static String[] VERSIONS = {
	    " PROFILE 1.0\0",
	    " PROFILE 1.0.1\0",
    };
    private final static int VERSION_JDK12BETA3 = 0;
    private final static int VERSION_JDK12BETA4 = 1;
    // These version numbers are indices into VERSIONS.  The instance data
    // member version is set to one of these, and it drives decisions when
    // reading the file.
    //
    // Version 1.0.1 added HPROF_GC_PRIM_ARRAY_DUMP, which requires no
    // version-sensitive parsing.
    //
    // Version 1.0.1 changed the type of a constant pool entry from a signature
    // to a typecode.  JDK 1.5.0-beta2-b51 is still Version 1.0.l.
    //
    // This parser is current through hprof.h version 1.10, put back by
    // Sheng on 4/14/98, but it works with JDK 1.5.0.

    //
    // Record types:
    //
    static final int HPROF_UTF8          = 0x01;
    static final int HPROF_LOAD_CLASS    = 0x02;
    static final int HPROF_UNLOAD_CLASS  = 0x03;
    static final int HPROF_FRAME         = 0x04;
    static final int HPROF_TRACE         = 0x05;
    static final int HPROF_ALLOC_SITES   = 0x06;
    static final int HPROF_HEAP_SUMMARY  = 0x07;
 
    static final int HPROF_START_THREAD  = 0x0a;
    static final int HPROF_END_THREAD    = 0x0b;
 
    static final int HPROF_HEAP_DUMP     = 0x0c;

    static final int HPROF_CPU_SAMPLES	 = 0x0d;
    static final int HPROF_CONTROL_SETTINGS = 0x0e;
    static final int HPROF_LOCKSTATS_WAIT_TIME = 0x10;
    static final int HPROF_LOCKSTATS_HOLD_TIME = 0x11;
 
    static final int HPROF_GC_ROOT_UNKNOWN       = 0xff;
    static final int HPROF_GC_ROOT_JNI_GLOBAL    = 0x01;
    static final int HPROF_GC_ROOT_JNI_LOCAL     = 0x02;
    static final int HPROF_GC_ROOT_JAVA_FRAME    = 0x03;
    static final int HPROF_GC_ROOT_NATIVE_STACK  = 0x04;
    static final int HPROF_GC_ROOT_STICKY_CLASS  = 0x05;
    static final int HPROF_GC_ROOT_THREAD_BLOCK  = 0x06;
    static final int HPROF_GC_ROOT_MONITOR_USED  = 0x07;
    static final int HPROF_GC_ROOT_THREAD_OBJ    = 0x08;
 
    static final int HPROF_GC_CLASS_DUMP         = 0x20;
    static final int HPROF_GC_INSTANCE_DUMP      = 0x21;
    static final int HPROF_GC_OBJ_ARRAY_DUMP         = 0x22;
    static final int HPROF_GC_PRIM_ARRAY_DUMP         = 0x23;

    //
    // Typecodes (from typecodes.h in the VM)
    //
    static final int T_CLASS = 2;	// Really an object
    static final int T_BOOLEAN = 4;
    static final int T_CHAR= 5;
    static final int T_FLOAT = 6;
    static final int T_DOUBLE = 7;
    static final int T_BYTE = 8;
    static final int T_SHORT = 9;
    static final int T_INT = 10;
    static final int T_LONG = 11;

    private int version;	// The version of .hprof being read

    private int debugLevel;
    private int currPos;	// Current position in the file

    private int dumpsToSkip;
    private boolean callStack;	// If true, read the call stack of objects

    private int identifierSize;		// Size, in bytes, of identifiers.
    private Hashtable names;		// Hashtable<Integer, String>

    // Hashtable<Integer, ThreadObject>, used to map the thread sequence number
    // (aka "serial number") to the thread object ID for 
    // HPROF_GC_ROOT_THREAD_OBJ.  ThreadObject is a trivial inner class,
    // at the end of this file.
    private Hashtable threadObjects;

    // Hashtable<Integer, String>, maps class object ID to class name
    // (with / converted to .)
    private Hashtable classNameFromObjectID;

    // Hashtable<Integer, Integer>, maps class serial # to class object ID
    private Hashtable classNameFromSerialNo;

    // Hashtable<Integer, StackFrame> maps stack frame ID to StackFrame.
    // Null if we're not tracking them.
    private Hashtable stackFrames;

    // Hashtable<Integer, StackTrace> maps stack frame ID to StackTrace
    // Null if we're not tracking them.
    private Hashtable stackTraces;

    // Buffer for storing instances.  I could use a ByteArrayOutputStream,
    // but that ends up doing an entire unnecessary copy, and doesn't
    // allow me to peek back.
    private byte[] instanceBuf = new byte[256];
    private int instanceBufLen = 0;

    public HprofReader(DataInput in, int dumpNumber, boolean callStack,
		       int debugLevel) {
	super(in);
	this.dumpsToSkip = dumpNumber - 1;
	this.callStack = callStack;
	this.debugLevel = debugLevel;
	names = new Hashtable();
	threadObjects = new Hashtable(43);
	classNameFromObjectID = new Hashtable();
	if (callStack) {
	    stackFrames = new Hashtable(43);
	    stackTraces = new Hashtable(43);
	    classNameFromSerialNo = new Hashtable();
	}
    }

    public Snapshot read() throws IOException {
	currPos = 4;	// 4 because of the magic number
	version = readVersionHeader();
	identifierSize = in.readInt();
	currPos += 4;
	if (identifierSize != 4) {
	    throw new IOException("I'm sorry, but I can't deal with an identifier size of " + identifierSize + ".  I can only deal with 4.");
	}
	System.out.println("Dump file created " + (new Date(in.readLong())));
	currPos += 8;

	for (;;) {
	    int type;
	    try {
		type = in.readUnsignedByte();
	    } catch (EOFException ignored) {
		break;
	    }
	    in.readInt();	// Timestamp of this record
	    int length = in.readInt();
	    if (debugLevel > 0) {
		System.out.println("Read record type " + type 
				   + ", length " + length
				   + " at position 0x" + toHex(currPos));
	    }
	    if (length < 0) {
		throw new IOException("Bad record length of " + length 
				      + " at byte 0x" + toHex(currPos+5)
				      + " of file.");
	    }
	    currPos += 9 + length;
	    switch (type) {
		case HPROF_UTF8: {
		    int id = readID();
		    byte[] chars = new byte[length - identifierSize];
		    in.readFully(chars);
		    names.put(new Integer(id), new String(chars));
		    break;
		}
		case HPROF_LOAD_CLASS: {
		    int serialNo = in.readInt();	// Not used
		    int classID = readID();
		    int stackTraceSerialNo = in.readInt();
		    int classNameID = readID();
		    Integer classIdI = new Integer(classID);
		    String nm = getNameFromID(classNameID).replace('/', '.');
		    classNameFromObjectID.put(classIdI, nm);
		    if (classNameFromSerialNo != null) {
			classNameFromSerialNo.put(new Integer(serialNo), nm);
		    }
		    break;
		}
		case HPROF_HEAP_DUMP: {
		    if (dumpsToSkip <= 0) {
			readHeapDump(length, currPos);
			return snapshot;	// We don't care about the rest
		    } else {
			dumpsToSkip--;
			skipBytes(length);
			break;
		    }
		}

		case HPROF_FRAME: {
		    if (stackFrames == null) {
			skipBytes(length);
		    } else {
			int id = readID();
			String methodName = getNameFromID(readID());
			String methodSig = getNameFromID(readID());
			String sourceFile = getNameFromID(readID());
			int classSer = in.readInt();
			String className = (String)
			      classNameFromSerialNo.get(new Integer(classSer));
			int lineNumber = in.readInt();
			if (lineNumber < StackFrame.LINE_NUMBER_NATIVE) {
			    System.out.println("Warning:  Weird stack frame line number:  " + lineNumber);
			    lineNumber = StackFrame.LINE_NUMBER_UNKNOWN;
			}
			stackFrames.put(new Integer(id),
					new StackFrame(methodName, methodSig,
						       className, sourceFile,
						       lineNumber));
		    }
		    break;
		}
		case HPROF_TRACE: {
		    if (stackTraces == null) {
			skipBytes(length);
		    } else {
			int serialNo = in.readInt();
			int threadSeq = in.readInt();	// Not used
			StackFrame[] frames = new StackFrame[in.readInt()];
			for (int i = 0; i < frames.length; i++) {
			    int fid = readID();
			    frames[i] = (StackFrame) 
				    stackFrames.get(new Integer(fid));
			    if (frames[i] == null) {
				throw new IOException("Stack frame 0x" + toHex(fid) + " not found");
			    }
			}
			stackTraces.put(new Integer(serialNo),
					new StackTrace(frames));
		    }
		    break;
		}
		case HPROF_UNLOAD_CLASS:
		case HPROF_ALLOC_SITES:
		case HPROF_START_THREAD:
		case HPROF_END_THREAD:
		case HPROF_HEAP_SUMMARY:
    		case HPROF_CPU_SAMPLES:
    		case HPROF_CONTROL_SETTINGS:
    		case HPROF_LOCKSTATS_WAIT_TIME:
    		case HPROF_LOCKSTATS_HOLD_TIME:
		{
		    // Ignore these record types
		    skipBytes(length);
		    break;
		}
		default: {
		    skipBytes(length);
		    System.out.println("Warning:  Ignoring unrecognized record type " + type);
		}
	    }
	}
	return snapshot;
    }

    private void skipBytes(int length) throws IOException {
	for (int i = 0; i < length; i++) {
	    in.readUnsignedByte();
	}
    }
    
    private int readVersionHeader() throws IOException {
	int candidatesLeft = VERSIONS.length;
	boolean[] matched = new boolean[VERSIONS.length];
	for (int i = 0; i < candidatesLeft; i++) {
	    matched[i] = true;
	}

	int pos = 0;
	while (candidatesLeft > 0) {
	    char c = (char) in.readByte();
	    currPos++;
	    for (int i = 0; i < VERSIONS.length; i++) {
		if (matched[i]) {
		    if (c != VERSIONS[i].charAt(pos)) {	  // Not matched
			matched[i] = false;
			--candidatesLeft;
		    } else if (pos == VERSIONS[i].length() - 1) {  // Full match
			return i;
		    }
		}
	    }
	    ++pos;
	}
	throw new IOException("Version string not recognized at byte " + (pos+3));
    }

    private void readHeapDump(int bytesLeft, int posAtEnd) throws IOException {
	while (bytesLeft > 0) {
	    int type = in.readUnsignedByte();
	    if (debugLevel > 0) {
		System.out.println("    Read heap sub-record type " + type 
				   + " at position 0x" 
				   + toHex(posAtEnd - bytesLeft));
	    }
	    bytesLeft--;
	    switch(type) {
		case HPROF_GC_ROOT_UNKNOWN: {
		    int id = readID();
		    bytesLeft -= identifierSize;
		    snapshot.addRoot(new Root(id, 0, Root.UNKNOWN, ""));
		    break;
		}
		case HPROF_GC_ROOT_THREAD_OBJ: {
		    int id = readID();
		    int threadSeq = in.readInt();
		    int stackSeq = in.readInt();
		    bytesLeft -= identifierSize + 8;
		    threadObjects.put(new Integer(threadSeq), 
				      new ThreadObject(id, stackSeq));
		    break;
		}
		case HPROF_GC_ROOT_JNI_GLOBAL: {
		    int id = readID();
		    int globalRefId = readID();	// Ignored, for now
		    bytesLeft -= 2*identifierSize;
		    snapshot.addRoot(new Root(id, 0, Root.NATIVE_STATIC, ""));
		    break;
		}
		case HPROF_GC_ROOT_JNI_LOCAL: {
		    int id = readID();
		    int threadSeq = in.readInt();
		    int depth = in.readInt();
		    bytesLeft -= identifierSize + 8;
		    ThreadObject to = getThreadObjectFromSequence(threadSeq);
		    StackTrace st = getStackTraceFromSerial(to.stackSeq);
		    if (st != null) {
			st = st.traceForDepth(depth+1);
		    }
		    snapshot.addRoot(new Root(id, to.threadId, 
					      Root.NATIVE_LOCAL, "", st));
		    break;
		}
		case HPROF_GC_ROOT_JAVA_FRAME: {
		    int id = readID();
		    int threadSeq = in.readInt();
		    int depth = in.readInt();
		    bytesLeft -= identifierSize + 8;
		    ThreadObject to = getThreadObjectFromSequence(threadSeq);
		    StackTrace st = getStackTraceFromSerial(to.stackSeq);
		    if (st != null) {
			st = st.traceForDepth(depth+1);
		    }
		    snapshot.addRoot(new Root(id, to.threadId, 
					      Root.JAVA_LOCAL, "", st));
		    break;
		}
		case HPROF_GC_ROOT_NATIVE_STACK: {
		    int id = readID();
		    int threadSeq = in.readInt();
		    bytesLeft -= identifierSize + 4;
		    ThreadObject to = getThreadObjectFromSequence(threadSeq);
		    StackTrace st = getStackTraceFromSerial(to.stackSeq);
		    snapshot.addRoot(new Root(id, to.threadId, 
					      Root.NATIVE_STACK, "", st));
		    break;
		}
		case HPROF_GC_ROOT_STICKY_CLASS: {
		    int id = readID();
		    bytesLeft -= identifierSize;
		    snapshot.addRoot(new Root(id, 0, Root.SYSTEM_CLASS, ""));
		    break;
		}
		case HPROF_GC_ROOT_THREAD_BLOCK: {
		    int id = readID();
		    int threadSeq = in.readInt();
		    bytesLeft -= identifierSize + 4;
		    ThreadObject to = getThreadObjectFromSequence(threadSeq);
		    StackTrace st = getStackTraceFromSerial(to.stackSeq);
		    snapshot.addRoot(new Root(id, to.threadId, 
				     Root.THREAD_BLOCK, "", st));
		    break;
		}
		case HPROF_GC_ROOT_MONITOR_USED: {
		    int id = readID();
		    bytesLeft -= identifierSize;
		    snapshot.addRoot(new Root(id, 0, Root.BUSY_MONITOR, ""));
		    break;
		}
		case HPROF_GC_CLASS_DUMP: {
		    int bytesRead = readClass();
		    bytesLeft -= bytesRead;
		    break;
		}
		case HPROF_GC_INSTANCE_DUMP: {
		    int bytesRead = readInstance();
		    bytesLeft -= bytesRead;
		    break;
		}
		case HPROF_GC_OBJ_ARRAY_DUMP: {
		    int bytesRead = readArray(false);
		    bytesLeft -= bytesRead;
		    break;
		}
		case HPROF_GC_PRIM_ARRAY_DUMP: {
		    int bytesRead = readArray(true);
		    bytesLeft -= bytesRead;
		    break;
		}
		default: {
		    throw new IOException("Unrecognized heap dump sub-record type:  " + type);
		}
	    }
	}
	if (bytesLeft != 0) {
	    throw new IOException("Error reading heap dump:  Byte count is " + bytesLeft + " instead of 0");
	}
	if (debugLevel > 0) {
	    System.out.println("    Finished heap sub-records.");
	}
	readDeferredInstances();
	if (debugLevel > 0) {
	    System.out.println("    Finished processing instances in heap dump.");
	}
    }

    //
    // Now we swap to a ByteArrayInputStream to read the instances
    // we deferred on before.  When we're done, we switch back to the
    // original input stream.
    //
    private void readDeferredInstances() throws IOException {
	DataInput oldIn = in;
	in = new DataInputStream(new ByteArrayInputStream(instanceBuf));
	int read = 0;
	while (read < instanceBufLen) {
	    read += readInstanceDeferred();
	}
	instanceBuf = null;	// We're done, and should enable GC
	in = oldIn;
    }

    private int readID() throws IOException {
	// REMIND: Someday we should handle an identifierSize other than 4 here.
	return in.readInt();
    }

    //
    // Read a java value.  If result is non-null, it's expected to be an
    // array of one element.  We use it to fake multiple return values.
    // @returns the number of bytes read
    //
    private int readValue(JavaThing[] resultArr) throws IOException {
	byte type = in.readByte();
	return 1 + readValueForType(type, resultArr);
    }

    private int readValueForType(byte type, JavaThing[] resultArr)
	    throws IOException {
	if (version >= VERSION_JDK12BETA4) {
	    type = signatureFromTypeId(type);
	}
	return readValueForTypeSignature(type, resultArr);
    }

    private int readValueForTypeSignature(byte type, JavaThing[] resultArr)
	    throws IOException {
	switch (type) {
	    case '[': 
	    case 'L': {
		int id = readID();
		if (resultArr != null) {
		    resultArr[0] = new JavaObjectRef(id);
		}
		return 4;
	    }
	    case 'Z': {
		int b = in.readByte();
		if (b != 0 && b != 1) {
		    System.out.println("Warning!  Illegal boolean value read");
		}
		if (resultArr != null) {
		    resultArr[0] = new JavaBoolean(b != 0);
		}
		return 1;
	    }
	    case 'B': {
		byte b = in.readByte();
		if (resultArr != null) {
		    resultArr[0] = new JavaByte(b);
		}
		return 1;
	    }
	    case 'S': {
		short s = in.readShort();
		if (resultArr != null) {
		    resultArr[0] = new JavaShort(s);
		}
		return 2;
	    }
	    case 'C': {
		char ch = in.readChar();
		if (resultArr != null) {
		    resultArr[0] = new JavaChar(ch);
		}
		return 2;
	    }
	    case 'I': {
		int val = in.readInt();
		if (resultArr != null) {
		    resultArr[0] = new JavaInt(val);
		}
		return 4;
	    }
	    case 'J': {
		long val = in.readLong();
		if (resultArr != null) {
		    resultArr[0] = new JavaLong(val);
		}
		return 8;
	    }
	    case 'F': {
		float val = in.readFloat();
		if (resultArr != null) {
		    resultArr[0] = new JavaFloat(val);
		}
		return 4;
	    }
	    case 'D': {
		double val = in.readDouble();
		if (resultArr != null) {
		    resultArr[0] = new JavaDouble(val);
		}
		return 8;
	    }
	    default: {
		throw new IOException("Bad value signature:  " + type);
	    }
	}
    }

    private ThreadObject getThreadObjectFromSequence(int threadSeq) 
	    throws IOException {
	ThreadObject to = (ThreadObject) 
		threadObjects.get(new Integer(threadSeq));
	if (to == null) {
	    throw new IOException("Thread " + threadSeq + 
			          " not found for JNI local ref");
	}
	return to;
    }

    private String getNameFromID(int id) throws IOException {
	return getNameFromID(new Integer(id));
    }

    private String getNameFromID(Integer id) throws IOException {
	if (id.intValue() == 0) {
	    return "";
	}
	String result = (String) names.get(id);
	if (result == null) {
	    if (debugLevel > 0) {
		System.out.println("WARNING:  Name not found at 0x" 
				   + toHex(id.intValue()));
		return "unresolved name 0x" + toHex(id.intValue());
	    } else {
		throw new IOException("Name not found for id 0x" 
				      + toHex(id.intValue()));
	    }
	}
	return result;
    }

    private StackTrace getStackTraceFromSerial(int ser) throws IOException {
	if (stackTraces == null) {
	    return null;
	}
	StackTrace result = (StackTrace) stackTraces.get(new Integer(ser));
	if (result == null) {
	    throw new IOException("Stack trace not found for serial # " + ser);
	}
	return result;
    }

    //
    // Handle a HPROF_GC_CLASS_DUMP
    // Return number of bytes read
    //
    private int readClass() throws IOException {
	int id = readID();
	StackTrace stackTrace = getStackTraceFromSerial(in.readInt());
	int superId = readID();
	int classLoaderId = readID();	// Ignored for now
	int signersId = readID();	// Ignored for now
	int protDomainId = readID();	// Ignored for now
	int reserved1 = readID();
	int reserved2 = readID();
	int instanceSize = in.readInt();
	int bytesRead = 7 * identifierSize + 8;

	int numConstPoolEntries = in.readUnsignedShort();
	bytesRead += 2;
	for (int i = 0; i < numConstPoolEntries; i++) {
	    int index = in.readUnsignedShort();	// unused
	    bytesRead += 2;
	    bytesRead += readValue(null);	// We ignore the values
	}

	int numStatics = in.readUnsignedShort();
	bytesRead += 2;
	JavaThing[] valueBin = new JavaThing[1];
	JavaStatic[] statics = new JavaStatic[numStatics];
	for (int i = 0; i < numStatics; i++) {
	    int nameId = readID();
	    bytesRead += identifierSize;
	    byte type = in.readByte();
	    bytesRead++;
	    bytesRead += readValueForType(type, valueBin);
	    String fieldName = getNameFromID(nameId);
	    if (version >= VERSION_JDK12BETA4) {
		type = signatureFromTypeId(type);
	    }
	    String signature = "" + ((char) type);
	    JavaField f = new JavaField(fieldName, signature);
	    statics[i] = new JavaStatic(f, valueBin[0]);
	}

	int numFields = in.readUnsignedShort();
	bytesRead += 2;
	JavaField[] fields = new JavaField[numFields];
	for (int i = 0; i < numFields; i++) {
	    int nameId = readID();
	    bytesRead += identifierSize;
	    byte type = in.readByte();
	    bytesRead++;
	    String fieldName = getNameFromID(nameId);
	    if (version >= VERSION_JDK12BETA4) {
		type = signatureFromTypeId(type);
	    }
	    String signature = "" + ((char) type);
	    fields[i] = new JavaField(fieldName, signature);
	}
	String name = (String) classNameFromObjectID.get(new Integer(id));
	if (name == null) {
	    throw new IOException("Class name not found for 0x" + toHex(id));
	}
	JavaClass c = new JavaClass(name, superId, fields, statics, stackTrace,
				    instanceSize);
	snapshot.addClass(id, c);

	return bytesRead;
    }

    private String toHex(int addr) {
	return hat.util.Misc.toHex(addr);
    }

    //
    // Append len bytes to the instanceBuf
    //
    private void appendToInstanceBuf(int len) throws IOException {
	int newLen = instanceBufLen + len;
	if (newLen > instanceBuf.length) {
	    int sz = (3 * (instanceBuf.length + 1)) / 2;
	    sz = (sz > newLen) ? sz : newLen;
	    byte[] newBuf = new byte[sz];
	    System.arraycopy(instanceBuf, 0, newBuf, 0, instanceBufLen);
	    instanceBuf = newBuf;
	}
	in.readFully(instanceBuf, instanceBufLen, len);
	instanceBufLen = newLen;
    }

    //
    // Handle a HPROF_GC_INSTANCE_DUMP
    // Return number of bytes read
    //
    private int readInstance() throws IOException {
	int size = 2 * identifierSize + 8;
	appendToInstanceBuf(size);
	byte[] b = instanceBuf;
	int l = instanceBufLen;
	int b1 = b[l - 4] & 0xff;
	int b2 = b[l - 3] & 0xff;
	int b3 = b[l - 2] & 0xff;
	int b4 = b[l - 1] & 0xff;
	// Get the # of bytes for the field values:
	int bytesFollowing = (b1 << 24) | (b2 << 16) | (b3 << 8) | (b4 << 0);
	appendToInstanceBuf(bytesFollowing);
	return size + bytesFollowing;
    }

    //
    // Do the actual work of an HPROF_GC_INSTANCE_BUF, This is called
    // after we're sure that any classes it needs have been read in.
    //

    private int readInstanceDeferred() throws IOException {
	int id = readID();
	StackTrace stackTrace = getStackTraceFromSerial(in.readInt());
	int classID = readID();
	int bytesFollowing = in.readInt();
	int bytesRead = 2 * identifierSize + 8;
	// This is why we need to defer this:  To guarantee the class
	// will be there.
	JavaClass clazz = (JavaClass) snapshot.findThing(classID);
	if (clazz == null) {
	    throw new IOException("Class 0x" + toHex(classID)
				  + " not found.");
	}
	clazz.resolveSuperclass(snapshot);
	JavaThing[] valueBin = new JavaThing[1];
	int target = clazz.getNumFieldsForInstance();
	JavaThing[] fieldValues = new JavaThing[target];
	int fieldNo = 0;
	JavaField[] fields = clazz.getFields();
	// Target is used to compensate for the fact that the dump
	// file starts field values from the leaf working upwards
	// in the inheritance hierarchy, whereas JavaObject starts
	// with the top of the inheritance hierarchy and works down.
	target -= fields.length;
	JavaClass currClass = clazz;
	for (int i = 0; i < fieldValues.length; i++) {
	    while (fieldNo >= fields.length) {
		currClass = currClass.getSuperclass();
		fields = currClass.getFields();
		fieldNo = 0;
		target -= fields.length;
	    }
	    JavaField f = fields[fieldNo];
	    byte type = (byte) f.getSignature().charAt(0);
	    bytesRead += readValueForTypeSignature(type, valueBin);
	    fieldValues[target + fieldNo] = valueBin[0];
	    fieldNo++;
	}
	snapshot.addHeapObject(id, new JavaObject(clazz, fieldValues, stackTrace));
	return bytesRead;
    }

    //
    // Handle a HPROF_GC_OBJ_ARRAY_DUMP or HPROF_GC_PRIM_ARRAY_DUMP
    // Return number of bytes read
    //
    private int readArray(boolean isPrimitive) throws IOException {
	int id = readID();
	StackTrace stackTrace = getStackTraceFromSerial(in.readInt());
	int num = in.readInt();
	int bytesRead = identifierSize + 8;
	int elementClassID;
	if (isPrimitive) {
	    elementClassID = in.readByte();
	    bytesRead++;
	} else {
	    elementClassID = readID();
	    bytesRead += identifierSize;
	}
	
	// Check for primitive arrays:
	byte primitiveSignature = 0x00;
	int elSize = 0;
	String primArrType = null;
	if (isPrimitive || version < VERSION_JDK12BETA4) {
	    switch (elementClassID) {
		case T_BOOLEAN: {
		    primitiveSignature = (byte) 'Z';
		    elSize = 1;
		    primArrType = "boolean[]";
		    break;
		}
		case T_CHAR: {
		    primitiveSignature = (byte) 'C';
		    elSize = 2;
		    primArrType = "char[]";
		    break;
		}
		case T_FLOAT: {
		    primitiveSignature = (byte) 'F';
		    elSize = 4;
		    primArrType = "float[]";
		    break;
		}
		case T_DOUBLE: {
		    primitiveSignature = (byte) 'D';
		    elSize = 8;
		    primArrType = "double[]";
		    break;
		}
		case T_BYTE: {
		    primitiveSignature = (byte) 'B';
		    elSize = 1;
		    primArrType = "byte[]";
		    break;
		}
		case T_SHORT: {
		    primitiveSignature = (byte) 'S';
		    elSize = 2;
		    primArrType = "short[]";
		    break;
		}
		case T_INT: {
		    primitiveSignature = (byte) 'I';
		    elSize = 4;
		    primArrType = "int[]";
		    break;
		}
		case T_LONG: {
		    primitiveSignature = (byte) 'J';
		    elSize = 8;
		    primArrType = "long[]";
		    break;
		}
	    }
	    if (version >= VERSION_JDK12BETA4 && primitiveSignature == 0x00) {
		throw new IOException("Unrecognized typecode:  " 
					+ elementClassID);
	    }
	}
	if (primitiveSignature != 0x00) {
	    byte[] data = new byte[elSize * num];
	    bytesRead += data.length;
	    in.readFully(data);
	    if (version < VERSION_JDK12BETA4) {
		primArrType = null;	// They weren't named
	    }
	    JavaValueArray va 
		= new JavaValueArray(primitiveSignature, primArrType, 
				     data, stackTrace);
	    snapshot.addHeapObject(id, va);
	} else {
	    int arrayClassID = 0;
	    int[] data = new int[num];
	    int sz = data.length * identifierSize;
	    bytesRead += sz;
	    for (int i = 0; i < data.length; i++) {
		data[i] = readID();
	    }
	    if (version >= VERSION_JDK12BETA4) {
		// It changed from the ID of the object describing the
		// class of element types to the ID of the object describing
		// the type of the array.
		arrayClassID = elementClassID;
		elementClassID = 0;
	    }
	    JavaObjectArray arr = 
		new JavaObjectArray(data, sz, stackTrace,
				    elementClassID, arrayClassID);
	    snapshot.addHeapObject(id, arr);
	}
	return bytesRead;
    }

    private byte signatureFromTypeId(byte typeId) throws IOException {
	switch (typeId) {
	    case T_CLASS: {
		return (byte) 'L';
	    }
	    case T_BOOLEAN: {
		return (byte) 'Z';
	    }
	    case T_CHAR: {
		return (byte) 'C';
	    }
	    case T_FLOAT: {
		return (byte) 'F';
	    }
	    case T_DOUBLE: {
		return (byte) 'D';
	    }
	    case T_BYTE: {
		return (byte) 'B';
	    }
	    case T_SHORT: {
		return (byte) 'S';
	    }
	    case T_INT: {
		return (byte) 'I';
	    }
	    case T_LONG: {
		return (byte) 'J';
	    }
	    default: {
		throw new IOException("Invalid type id of " + typeId);
	    }
	}
    }

    //
    // A trivial data-holder class for HPROF_GC_ROOT_THREAD_OBJ.
    //
    private class ThreadObject {

	int threadId;
	int stackSeq;

	ThreadObject(int threadId, int stackSeq) {
	    this.threadId = threadId;
	    this.stackSeq = stackSeq;
	}
    }

}
