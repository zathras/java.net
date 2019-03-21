
package com.sun.billf.tass.dbview;

import javax.swing.tree.TreeNode;

import java.util.Enumeration;


public class LeafNode implements TreeNode, Comparable {

    private String name;
    private TreeNode parent;

    public LeafNode(String name, TreeNode parent) {
	this.name = name;
	this.parent = parent;
    }

    public Enumeration children() {
	return null;
    }

    public boolean getAllowsChildren() {
	return false;
    }

    public TreeNode getChildAt(int childIndex) {
	return null;
    }

    public int getChildCount() {
	return 0;
    }

    public TreeNode getParent() {
	return parent;
    }

    public int getIndex(TreeNode child) {
	return -1;
    }

    public boolean isLeaf() {
	return true;
    }

    public int compareTo(Object other) {
	if (!(other instanceof LeafNode)) {
	    return 1;
	} else {
	    LeafNode lno = (LeafNode) other;
	    return name.compareTo(lno.name);
	}
    }

    public String toString() {
	return name;
    }
}
