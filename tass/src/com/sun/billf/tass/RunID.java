
package com.sun.billf.tass;


//
// The identifier of a given application run in a TassDatabase.
// It's a list of integers, with [length-1] being the application
// run number, and all the other entries being a consolidation run
// number.

public class RunID implements Comparable {
    int[] ids;

    RunID(int simpleRunNumber) {
	ids = new int[1];
	ids[0] = simpleRunNumber;
    }

    RunID(int[] ids) {
	this.ids = ids;
    }

    RunID(RunID parent, RunID child) {
	this.ids = new int[parent.ids.length + child.ids.length];
	int i;
	for (i = 0; i < parent.ids.length; i++) {
	    ids[i] = parent.ids[i];
	}
	for (i = 0; i < child.ids.length; i++) {
	    ids[parent.ids.length + i] = child.ids[i];
	}
    }

    public int hashCode() {
	int result = 0;
	for (int i = 0; i < ids.length; i++) {
	    result *= 101;
	    result += ids[i];
	}
	return result;
    }

    public boolean equals(Object other) {
	if (other instanceof RunID) {
	    RunID ro = (RunID) other;
	    if (ids.length != ro.ids.length) {
		return false;
	    }
	    for (int i = 0; i < ids.length; i++) {
		if (ids[i] != ro.ids[i]) {
		    return false;
		}
	    }
	    return true;
	}
	return false;
    }

    public String toString() {
	String result = "RunID<";
	for (int i = 0; i < ids.length; i++) {
	    if (i > 0) {
		result = result + ",";
	    }
	    result = result + ids[i];
	}
	return result + ">";
    }

    public int compareTo(Object other) {
	if (!(other instanceof RunID)) {
	    return -1;
	}
	RunID rio = (RunID) other;
	int len = Math.min(ids.length, rio.ids.length);
	for (int i = 0; i < len; i++) {
	    if (ids[i] < rio.ids[i]) {
		return -1;
	    } else if (ids[i] > rio.ids[i]) {
		return 1;
	    }
	}
	if (ids.length < rio.ids.length) {
	    return -1;
	} else if (ids.length > rio.ids.length) {
	    return 1;
	}
	return 0;
    }
}
