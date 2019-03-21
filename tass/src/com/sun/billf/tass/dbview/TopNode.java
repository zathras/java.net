
package com.sun.billf.tass.dbview;

import javax.swing.tree.TreeNode;

import java.util.Vector;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.Iterator;

import com.sun.billf.tass.TassDatabase;

public class TopNode extends NonLeafNode implements TreeNode {

    private String name;
    private TassDatabase db;
    private HashMap byPrefix = new HashMap();
    private Set refs = null;	// set by build

    public TopNode(String name, TassDatabase db) {
	this.name = name;
	this.db = db;
    }

    ClassNode addClass(String className) {
	String prefix = getPrefix(className);
	PrefixNode child = (PrefixNode) byPrefix.get(prefix);
	if (child == null) {
	    child = new PrefixNode(prefix, db, this);
	    byPrefix.put(prefix, child);
	}
	return child.addClass(className);
    }

    public TreeNode getParent() {
	return null;
    }

    //
    // Called after we've added everything
    //
    Set build() {
	refs = new HashSet();
	for (Iterator it = byPrefix.values().iterator(); it.hasNext(); ) {
	    PrefixNode n = (PrefixNode) it.next();
	    Set s = n.build();
	    for (Iterator it2 = s.iterator(); it2.hasNext(); ) {
		refs.add(it2.next());
	    }
	}
	build(byPrefix.values());
	insertKid(new RunIDNode("references", refs, this, db));
	byPrefix = null;
	return refs;
    }

    private String getPrefix(String name) {
	int pos = name.indexOf('.');
	if (pos < 0) {
	    return name;
	}
	return name.substring(0, pos);
    }

    public String toString() {
	return "" + refs.size() + ":  " + name;
    }
}
