
package com.sun.billf.tass.dbview;

import javax.swing.tree.TreeNode;
import java.util.Enumeration;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.Iterator;

import com.sun.billf.tass.TassDatabase;
import com.sun.billf.tass.Utils;

public class PrefixNode extends NonLeafNode implements TreeNode, Comparable {

    private String name;
    private TassDatabase db;
    private TopNode parent;
    private HashMap byPackage = new HashMap();
    Set refs = null;	// set by build

    public PrefixNode(String name, TassDatabase db, TopNode parent) {
	this.name = name;
	this.db = db;
	this.parent = parent;
    }

    public TreeNode getParent() {
	return parent;
    }

    ClassNode addClass(String className) {
	String pkgName = Utils.stripAfterLastDot(className);
	PackageNode child = (PackageNode) byPackage.get(pkgName);
	if (child == null) {
	    child = new PackageNode(pkgName, db, this);
	    byPackage.put(pkgName, child);
	}
	return child.addClass(className);
    }

    //
    // Called after we've added everything
    //
    Set build() {
	refs = new HashSet();
	for (Iterator it = byPackage.values().iterator(); it.hasNext(); ) {
	    PackageNode n = (PackageNode) it.next();
	    Set s = n.build();
	    for (Iterator it2 = s.iterator(); it2.hasNext(); ) {
		refs.add(it2.next());
	    }
	}
	build(byPackage.values());
	insertKid(new RunIDNode("references", refs, this, db));
	byPackage = null;
	return refs;
    }

    public String toString() {
	String str = refs.size() + ":  " + name;
	return str;
    }

    public int compareTo(Object other) {
	if (!(other instanceof PrefixNode)) {
	    return 1;
	}
	PrefixNode pno = (PrefixNode) other;
	return name.compareTo(pno.name);
    }
}
