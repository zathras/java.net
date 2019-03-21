
package com.sun.billf.tass;

import java.io.IOException;
import java.io.PrintStream;

import org.apache.bcel.classfile.Attribute;
import org.apache.bcel.classfile.Code;
import org.apache.bcel.classfile.CodeException;
import org.apache.bcel.classfile.Constant;
import org.apache.bcel.classfile.ConstantClass;
import org.apache.bcel.classfile.ConstantDouble;
import org.apache.bcel.classfile.ConstantFieldref;
import org.apache.bcel.classfile.ConstantFloat;
import org.apache.bcel.classfile.ConstantInteger;
import org.apache.bcel.classfile.ConstantInterfaceMethodref;
import org.apache.bcel.classfile.ConstantLong;
import org.apache.bcel.classfile.ConstantMethodref;
import org.apache.bcel.classfile.ConstantNameAndType;
import org.apache.bcel.classfile.ConstantPool;
import org.apache.bcel.classfile.ConstantString;
import org.apache.bcel.classfile.ConstantUtf8;
import org.apache.bcel.classfile.ConstantValue;
import org.apache.bcel.classfile.Deprecated;
import org.apache.bcel.classfile.ExceptionTable;
import org.apache.bcel.classfile.Field;
import org.apache.bcel.classfile.InnerClass;
import org.apache.bcel.classfile.InnerClasses;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.LineNumber;
import org.apache.bcel.classfile.LineNumberTable;
import org.apache.bcel.classfile.LocalVariable;
import org.apache.bcel.classfile.LocalVariableTable;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.classfile.Signature;
import org.apache.bcel.classfile.SourceFile;
import org.apache.bcel.classfile.StackMap;
import org.apache.bcel.classfile.StackMapEntry;
import org.apache.bcel.classfile.Synthetic;
import org.apache.bcel.classfile.Unknown;

import org.apache.bcel.generic.Type;

//
//  TassVisitor
//
//  This visitor class is used by TassChecker.  It visits
//  each node in a JavaClass in turn.  It's an implementation
//  of the Visitor pattern, as used by BCEL.  See
//  JavaClass.visit(Visitor) for details.
//
//  This implementation class is intentionally package-private.

class TassVisitor implements org.apache.bcel.classfile.Visitor {

    //
    // The visitee  we're a helper for
    //
    private Visitee visitee;

    private ConstantPool currConstantPool = null;

    TassVisitor(Visitee visitee) {
	this.visitee = visitee;
    }

    private void visitCodeIfNotNull(Code c) {
	if (c != null) {
	    c.accept(this);
	}
    }

    private void visitConstantPoolIfNotNull(ConstantPool c) {
	if (c != null) {
	    ConstantPool old = currConstantPool;
	    currConstantPool = c;
	    c.accept(this);
	    currConstantPool = old;
	}
    }

    public void visitCode(Code obj) {
	// We don't need to check the actual bytecodes, because any
	// references to types or other methods go through the constant
	// pool.  We just scan through the entire constant pool; we don't
	// care which bit of code causes a reference to land there.
	Attribute[] attributes = obj.getAttributes();
	for(int i=0; i < attributes.length; i++) {
	    attributes[i].accept(this);
	}
	CodeException[] ce = obj.getExceptionTable();
	for (int i = 0; i < ce.length; i++) {
	    ce[i].accept(this);
	}
    }

    public void visitCodeException(CodeException obj) {
    }

    public void visitConstantClass(ConstantClass obj) {
	String cl = obj.getConstantValue(currConstantPool).toString();
	visitee.checkForClass(cl);
    }

    public void visitConstantDouble(ConstantDouble obj) {
    }

    private ConstantNameAndType getNameAndType(int nameAndTypeIndex) {
	if (currConstantPool == null) {
	    return null;
	}
	Constant c = currConstantPool.getConstant(nameAndTypeIndex);
	if (c instanceof ConstantNameAndType) {
	    return (ConstantNameAndType) c;
	} else {
	    return null;
	}
    }

    public void visitConstantFieldref(ConstantFieldref obj) {
	int nti = obj.getNameAndTypeIndex();
	ConstantNameAndType nt = getNameAndType(nti);
	if (nt == null) {
	    visitee.reportError("Error in constant pool for class.");
	} else {
	    String cl = obj.getClass(currConstantPool);
	    String nm = nt.getName(currConstantPool);
	    Type t = Type.getType(nt.getSignature(currConstantPool));
	    visitee .checkFieldRef(cl, nm);
	    visitee .checkType(t);
	}
    }

    public void visitConstantFloat(ConstantFloat obj) {
    }

    public void visitConstantInteger(ConstantInteger obj) {
    }

    public void visitConstantInterfaceMethodref(ConstantInterfaceMethodref obj) {
	int nti = obj.getNameAndTypeIndex();
	ConstantNameAndType nt = getNameAndType(nti);
	if (nt == null) {
	    visitee .reportError("Error in constant pool for class.");
	} else {
	    String cl = obj.getClass(currConstantPool);
	    String nm = nt.getName(currConstantPool);
	    String sig = nt.getSignature(currConstantPool);
	    Type t = Type.getReturnType(sig);
	    Type[] args = Type.getArgumentTypes(sig);
	    visitee.checkMethodRef(cl, nm, args);
	    visitee.checkType(t);
	}
    }

    public void visitConstantLong(ConstantLong obj) {
    }

    public void visitConstantMethodref(ConstantMethodref obj) {
	int nti = obj.getNameAndTypeIndex();
	ConstantNameAndType nt = getNameAndType(nti);
	if (nt == null) {
	    visitee.reportError("Error in constant pool for class.");
	} else {
	    String cl = obj.getClass(currConstantPool);
	    String nm = nt.getName(currConstantPool);
	    String sig = nt.getSignature(currConstantPool);
	    Type t = Type.getReturnType(sig);
	    Type[] args = Type.getArgumentTypes(sig);
	    visitee.checkMethodRef(cl, nm, args);
	    visitee.checkType(t);
	}
    }

    public void visitConstantNameAndType(ConstantNameAndType obj) {
	// We can safely ignore this record; it is referred to by
	// things we do check, like ConstantMethodref.
    }

    public void visitConstantPool(ConstantPool obj) {
	Constant[] consts = obj.getConstantPool();
	if (consts != null) {
	    for (int i = 0; i < consts.length; i++) {
		if (consts[i] != null) {
		    consts[i].accept(this);
		}
	    }
	}
    }

    public void visitConstantString(ConstantString obj) {
    }

    public void visitConstantUtf8(ConstantUtf8 obj) {
    }

    public void visitConstantValue(ConstantValue obj) {
    }

    public void visitDeprecated(Deprecated obj) {
    }

    public void visitExceptionTable(ExceptionTable obj) {
	String[] exNames = obj.getExceptionNames();
	for (int i = 0; i < exNames.length; i++) {
	    visitee.checkForClass(exNames[i]);
	}
    }

    public void visitField(Field obj) {
	visitee.checkType(obj.getType());
	Attribute[] attributes = obj.getAttributes();
	for(int i=0; i < attributes.length; i++) {
	    attributes[i].accept(this);
	}
    }

    public void visitInnerClass(InnerClass obj) {
    }

    public void visitInnerClasses(InnerClasses obj) {
    }

    public void visitJavaClass(JavaClass obj) {
	String sc = obj.getSuperclassName() ;
	if (sc != null) {
	    visitee.checkForClass(sc);
	}
	String[] ifs = obj.getInterfaceNames();
	for (int i = 0; i < ifs.length; i++) {
	    visitee.checkForClass(ifs[i]);
	}
	Field[] fields = obj.getFields();
	for(int i=0; i < fields.length; i++) {
	    fields[i].accept(this);
	}
	Method[] methods = obj.getMethods();
	for(int i=0; i < methods.length; i++) {
	    methods[i].accept(this);
	}
	Attribute[] attributes = obj.getAttributes();
	for(int i=0; i < attributes.length; i++) {
	    attributes[i].accept(this);
	}
	visitConstantPoolIfNotNull(obj.getConstantPool());

    }

    public void visitLineNumber(LineNumber obj) {
    }

    public void visitLineNumberTable(LineNumberTable obj) {
	LineNumber[] numbers = obj.getLineNumberTable();
	for(int i=0; i < numbers.length; i++) {
	    numbers[i].accept(this);
	}
    }

    public void visitLocalVariable(LocalVariable obj) {
	Type t = Type.getType(obj.getSignature());
	visitee.checkType(t);
    }

    public void visitLocalVariableTable(LocalVariableTable obj) {
	LocalVariable[] vars = obj.getLocalVariableTable();
	for(int i=0; i < vars.length; i++) {
	    vars[i].accept(this);
	}
    }

    public void visitMethod(Method obj) {
	Type[] args = obj.getArgumentTypes();
	for (int i = 0; i < args.length; i++) {
	    visitee.checkType(args[i]);
	}
	visitCodeIfNotNull(obj.getCode());
	visitee.checkType(obj.getReturnType());
	Attribute[] attributes = obj.getAttributes();
	for(int i=0; i < attributes.length; i++) {
	    attributes[i].accept(this);
	}
    }

    public void visitSignature(Signature obj) {
	// This is a "GJ Attribute," that is, something related to
	// Java generics.  These don't apply to CDC.
    }

    public void visitSourceFile(SourceFile obj) {
    }

    public void visitStackMap(StackMap obj) {
	StackMapEntry[] sm = obj.getStackMap();
	for (int i = 0; i < sm.length; i++) {
	    sm[i].accept(this);
	}
    }

    public void visitStackMapEntry(StackMapEntry obj) {
    }

    public void visitSynthetic(Synthetic obj) {
    }

    public void visitUnknown(Unknown obj) {
    }

}

