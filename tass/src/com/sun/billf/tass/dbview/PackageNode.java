
package com.sun.billf.tass.dbview;

import javax.swing.tree.TreeNode;
import java.util.Enumeration;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.Iterator;
import java.util.Set;

import com.sun.billf.tass.TassDatabase;
import com.sun.billf.tass.Utils;

public class PackageNode extends NonLeafNode implements TreeNode, Comparable {

    private String name;
    private TassDatabase db;
    private PrefixNode parent;
    private HashMap byClass = new HashMap();

    public PackageNode(String name, TassDatabase db, PrefixNode parent) {
	this.name = name;
	this.db = db;
	this.parent = parent;
    }

    public TreeNode getParent() {
	return parent;
    }

    ClassNode addClass(String className) {
	ClassNode child = (ClassNode) byClass.get(className);
	if (child == null) {
	    child = new ClassNode(className, db, this);
	    byClass.put(className, child);
	}
	return child;
    }

    //
    // Called after we've added everything
    //
    Set build() {
	Set refs = new HashSet();
	for (Iterator it = byClass.values().iterator(); it.hasNext(); ) {
	    ClassNode n = (ClassNode) it.next();
	    Set s = n.build();
	    for (Iterator it2 = s.iterator(); it2.hasNext(); ) {
		refs.add(it2.next());
	    }
	}
	build(byClass.values());
	insertKid(new RunIDNode("references", refs, this, db));
	byClass = null;
	return refs;
    }

    public String toString() {
	Set refs = (Set) db.getPackagesUsed().get(name);
	int count = refs.size();
	boolean notInPlatform = false;
	if (db.getPlatformRunID() != null) {
	    if (refs.contains(db.getPlatformRunID())) {
		count--;
	    } else {
		notInPlatform = true;
	    }
	}
	String str = count + ":  " + name;
	if (notInPlatform) {
	    str = str + " (not in platform)";
	}
	return str;
    }

    public int compareTo(Object other) {
	if (!(other instanceof PackageNode)) {
	    return 1;
	}
	PackageNode pno = (PackageNode) other;
	return name.compareTo(pno.name);
    }
}
