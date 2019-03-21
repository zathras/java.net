
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


/**
 * A node that contains a set of RunID values
 **/
public class RunIDNode implements TreeNode {

    private String name;
    private TreeNode parent;
    private Set refs;
    private TassDatabase db;
    private Object[] kids = null;

    public RunIDNode(String name, Set refs, TreeNode parent, TassDatabase db) {
	this.name = name;
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

    public String toString() {
	int sz = refs.size();
	return name;
    }
}
