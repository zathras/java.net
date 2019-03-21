
package com.sun.billf.tass.dbview;

import javax.swing.tree.TreeNode;

import java.util.Collection;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Set;


public abstract class NonLeafNode implements TreeNode {

    private Object[] kids = null;

    /**
     * Returns the set of RunID's under this node, not including the
     * platform RunID (if present)
     **/
    abstract Set build();

    protected void build(Collection vals) {
	kids = new Object[vals.size()];
	vals.toArray(kids);
	Arrays.sort(kids);
    }

    protected void insertKid(TreeNode kid) {
	Object[] newKids = new Object[kids.length+1];
	newKids[0] = kid;
	for (int i = 0; i < kids.length; i++) {
	    newKids[i+1] = kids[i];
	}
	kids = newKids;
    }

    public Enumeration children() {
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
	return (TreeNode) kids[childIndex];
    }

    public int getChildCount() {
	return kids.length;
    }

    public int getIndex(TreeNode child) {
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

}
