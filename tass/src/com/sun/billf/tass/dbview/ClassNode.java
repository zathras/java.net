
package com.sun.billf.tass.dbview;

import javax.swing.tree.TreeNode;
import java.util.Enumeration;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;

import com.sun.billf.tass.TassDatabase;
import com.sun.billf.tass.Utils;

public class ClassNode extends NonLeafNode implements TreeNode, Comparable {

    private String name;
    private TassDatabase db;
    private PackageNode parent;
    private LinkedList builder = new LinkedList();
    // set by build:
    Set refs = null;	
    boolean notInPlatform;

    public ClassNode(String name, TassDatabase db, PackageNode parent) {
	this.name = name;
	this.db = db;
	this.parent = parent;
    }

    public TreeNode getParent() {
	return parent;
    }

    public void addMember(MemberNode child) {
	builder.add(child);
    }

    //
    // Called after we've added everything
    //
    Set build() {
	refs = (Set) db.getClassReferences().get(name);
	if (db.getPlatformRunID() != null) {
	    if (refs.contains(db.getPlatformRunID())) {
		notInPlatform = false;
		refs = new HashSet(refs);
		refs.remove(db.getPlatformRunID());
	    } else {
		notInPlatform = true;
	    }
	}
	build(builder);
	insertKid(new RunIDNode("references", refs, this, db));
	builder = null;
	return refs;
    }

    public String toString() {
	String str = refs.size() + ":  " + name;
	if (notInPlatform) {
	    str = str + " (not in platform)";
	}
	return str;
    }

    public int compareTo(Object other) {
	if (!(other instanceof ClassNode)) {
	    return 1;
	}
	ClassNode pno = (ClassNode) other;
	return name.compareTo(pno.name);
    }
}
