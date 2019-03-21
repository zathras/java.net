
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
import java.io.IOException;
import java.io.EOFException;

import hat.model.*;

/**
 * Object that's used to read a Bod file.  This is not thread-safe (i.e.
 * only one read at a time!)
 *
 * @version     1.16, 10/08/98
 * @author      Bill Foote
 */

public class BodReader extends Reader {

    static int MAGIC_NUMBER = 0x0b0dd00d;
    static int LITTLE_ENDIAN_MAGIC_NUMBER = 0x0dd00d0b;
    private static int VERSION_NUMBER = 4;

    private static final int TYPE_OBJECT = 0;
    private static final int TYPE_CLASS = 1;
    private static final int TYPE_OBJECT_ARRAY = 2;
    private static final int TYPE_CHAR_ARRAY = 3;
    private static final int TYPE_OTHER_ARRAY = 4;

    private static final int NOT_ROOT = 0;
    private static final int ROOT_STATIC = 1;
    private static final int ROOT_JAVALOCAL = 2;
    private static final int ROOT_NATIVEREF = 4;

    public BodReader(DataInput in) {
	super(in);
    }

    public Snapshot read() throws IOException {
	// Certain data types simply aren't written out in a .bod file,
	// like byte[], double[], etc.
	snapshot.setUnresolvedObjectsOK(true);
	int i = in.readInt();
	if (i != VERSION_NUMBER) {
	    throw new IOException("Bad version number: " + i);
	}

	for (;;) {
	    int type;
	    try {
		type = in.readByte();
	    } catch (EOFException ignored) {
		break;
	    }
	    switch (type) {
		case TYPE_OBJECT:
		    readObject();
		    break;
		case TYPE_CLASS:
		    readClass();
		    break;
		case TYPE_OBJECT_ARRAY:
		    readObjectArray();
		    break;
		case TYPE_CHAR_ARRAY:
		    readCharArray();
		    break;
		case TYPE_OTHER_ARRAY:
		    readOtherArray();
		    break;
		default:
		    throw new IOException("Bad type value: " + type);
	    }
	}

	Snapshot result = snapshot;
	snapshot = null;
	return result;
    }

    private void readObject() throws IOException {
	int id = in.readInt();
	byte rootFlags = in.readByte();
	int threadId = in.readInt();
	int classId = in.readInt();
	int numFields = in.readInt();
	JavaThing[] fields = new JavaThing[numFields];
	JavaThing clazz = new JavaObjectRef(classId);
	for (int i = 0; i < numFields; i++) {
	    fields[i] = new JavaObjectRef(in.readInt());
	    // JavaObjectRef becomes JavaInt if the field ends up not
	    // being an object type
	}
	snapshot.addHeapObject(id, new JavaObject(clazz, fields, null));

	// if rootFlags & ROOT_STATIC, we don't care -- JavaClass will add
	// the root for us.

	if ((rootFlags & ROOT_JAVALOCAL) != 0) {
	    snapshot.addRoot(new Root(id, threadId, Root.JAVA_LOCAL, 
					"Java stack local"));
	}
	if ((rootFlags & ROOT_NATIVEREF) != 0) {
	    int type = (threadId == 0) ? Root.NATIVE_STATIC : Root.NATIVE_LOCAL;
	    snapshot.addRoot(new Root(id, threadId, type,
					"Native code reference"));
	}
    }

    private void readClass() throws IOException {
	int id = in.readInt();
	byte rootFlags = in.readByte();
	// We ignore this -- for now, just figure all classes are reachable.
	int threadId = in.readInt();
	int superClassId = in.readInt();
	String name = readString().replace('/', '.');

	int numStatics = in.readInt();
	JavaStatic[] statics = new JavaStatic[numStatics];
	for (int i = 0; i < numStatics; i++) {
	    String fieldName = readString();
	    String signature = readString();
	    JavaField f = new JavaField(fieldName, signature);
	    int v = in.readInt();
	    JavaThing value;
	    if (f.hasId()) {
		value = new JavaObjectRef(v);
	    } else {
		value = new JavaInt(v);
	    }
	    statics[i] = new JavaStatic(f, value);
	}

	int numFields = in.readInt();
	JavaField[] fields = new JavaField[numFields];
	for (int i = 0; i < numFields; i++) {
	    String fieldName = readString();
	    String signature = readString();
	    fields[i] = new JavaField(fieldName, signature);
	}
	int instSize = in.readInt();
	JavaClass c = new JavaClass(name, superClassId, fields, statics, null,
				    instSize);
	snapshot.addClass(id, c);
    }

    private void readObjectArray() throws IOException {
	int id = in.readInt();
	byte rootFlags = in.readByte();
	int threadId = in.readInt();

	int size = in.readInt();
	int len = in.readInt();
	int[] rawData = new int[len];
	for (int i = 0; i < len; i++) {
	    rawData[i] = in.readInt();
	}
	snapshot.addHeapObject(id, new JavaObjectArray(rawData, size, null));

	// if rootFlags & ROOT_STATIC, we don't care -- JavaClass will add
	// the root for us.

	if ((rootFlags & ROOT_JAVALOCAL) != 0) {
	    snapshot.addRoot(new Root(id, Root.JAVA_LOCAL, threadId,
					"Java stack local"));
	}
	if ((rootFlags & ROOT_NATIVEREF) != 0) {
	    int type = (threadId == 0) ? Root.NATIVE_STATIC : Root.NATIVE_LOCAL;
	    snapshot.addRoot(new Root(id, threadId, type,
					"Native code reference"));
	}
    }

    private void readCharArray() throws IOException {
	int id = in.readInt();
	byte rootFlags = in.readByte();
	// We ignore this for now
	int threadId = in.readInt();

	int size = in.readInt();
	String value = readString();
	byte[] val = new byte[value.length() * 2];
	for (int i = 0; i < value.length(); i++) {
	    int ch = value.charAt(i) & 0xffffffff;
	    val[i * 2] = (byte) (ch >> 8);
	    val[i * 2 + 1] = (byte) (ch & 0xff);
	}
	snapshot.addHeapObject(id, new JavaValueArray((byte) 'C', val, null));
    }

    private void readOtherArray() throws IOException {
	int id = in.readInt();
	byte rootFlags = in.readByte();
	// We ignore this for now
	int threadId = in.readInt();

	int size = in.readInt();
	snapshot.addHeapObject(id, new JavaObjectArray(new int[0], size, null));
    }

    private String readString() throws IOException {
	int len = in.readInt();
	char[] buf = new char[len];
	for (int i = 0; i < len; i++) {
	    buf[i] = in.readChar();
	}
	return new String(buf);
    }

}
