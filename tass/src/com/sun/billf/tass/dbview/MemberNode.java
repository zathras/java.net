
package com.sun.billf.tass.dbview;

import javax.swing.tree.TreeNode;

import java.util.Collection;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Set;
import java.util.HashSet;

import com.sun.billf.tass.MethodReference;
import com.sun.billf.tass.RunID;
import com.sun.billf.tass.TassDatabase;


public class MemberNode implements TreeNode, Comparable {

    private String name;
    private ClassNode parent;
    private MethodReference method;
    private Set refs;
    private TassDatabase db;
    private Object[] kids = null;

    public MemberNode(MethodReference method, Set refs, ClassNode parent,
    		      TassDatabase db) 
    {
	this.method = method;
	this.name = method.shortName();
	this.parent = parent;
	this.refs = refs;
	this.db = db;
    }

    public MemberNode(String fieldName, Set refs, ClassNode parent, 
    		      TassDatabase db) 
    {
	this.method = null;
	this.name = fieldName;
	this.parent = parent;
	this.refs = refs;
	this.db = db;
    }

    private synchronized void makeKids() {
	if (kids == null) {
	    Set s = refs;
	    Object pfr = db.getPlatformRunID();
	    if (pfr != null && s.contains(pfr)) {
		s = new HashSet(s);
		s.remove(pfr);
	    }
	    kids = s.toArray();
	    Arrays.sort(kids);
	    for (int i = 0; i < kids.length; i++) {
		kids[i] = new LeafNode(((RunID) kids[i]).toString(), this);
	    }
	}
    }

    public Enumeration children() {
	makeKids();
	return new Enumeration() {
	    private int pos = 0;
	    public boolean hasMoreElements() {
		return pos < kids.length;
	    }
	    public Object nextElement() {
		return kids[pos++];
	    }
	};
    }

    public boolean getAllowsChildren() {
	return true;
    }

    public TreeNode getChildAt(int childIndex) {
	makeKids();
	return (TreeNode) kids[childIndex];
    }

    public int getChildCount() {
	makeKids();
	return kids.length;
    }

    public TreeNode getParent() {
	return parent;
    }

    public int getIndex(TreeNode child) {
	makeKids();
	for (int i = 0; i < kids.length; i++) {
	    if (kids[i] == child) {
		return i;
	    }
	}
	return -1;
    }

    public boolean isLeaf() {
	return false;
    }

    public int compareTo(Object other) {
	if (!(other instanceof MemberNode)) {
	    return 1;
	}
	MemberNode mno = (MemberNode) other;
	if (method == null && mno.method != null) {
	    return -1;
	} else if (method != null && mno.method == null) {
	    return 1;
	} else {
	    return name.compareTo(mno.name);
	}
    }

    public String toString() {
	int sz = refs.size();
	Object pfr = db.getPlatformRunID();
	if (pfr != null && refs.contains(pfr)) {
	    sz--;
	}
	return sz + ":  " + name;
    }
}
