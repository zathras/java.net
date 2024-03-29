
/* The contents of this file are subject to the Sun Public License
 * Version 1.0 (the "License"); you may not use this file except in
 * compliance with the License. A copy of the License is available at
 * http://www.sun.com/, and in the file LICENSE.html in the
 * doc directory.
 * 
 * The Original Code is HAT. The Initial Developer of the
 * Original Code is Bill Foote, with contributions from others
 * at JavaSoft/Sun. Portions created by Bill Foote and others
 * at Javasoft/Sun are Copyright (C) 1997-2004. All Rights Reserved.
 * 
 * In addition to the formal license, I ask that you don't
 * change the history or donations files without permission.
 */

package hat.util;
import java.util.*;

/**
 * A singleton utility class that sorts an array of objects.
 * <p>
 * Use:
 * <pre>
 *
 *  Stuff[] arr = ...;
 *  ArraySorter.sort(arr, new Comparer() {
 *      public int compare(Object lhs, Object rhs) {
 *          return ((String) lhs).compareTo((String) rhs);
 *      }
 *  });
 * </pre>
 *
 * @version     1.5, 03/06/98
 * @author      Bill Foote
 */

public class ArraySorter {

    /**
     * Sort the given array, using c for comparison
    **/
    static public void sort(Object[] arr, Comparer c)  {
	quickSort(arr, c, 0, arr.length-1);
    }


    /**
     * Sort an array of strings, using String.compareTo()
    **/
    static public void sortArrayOfStrings(Object[] arr) {
	sort(arr, new Comparer() {
	    public int compare(Object lhs, Object rhs) {
		return ((String) lhs).compareTo((String) rhs);
	    }
	});
    }


    static private void swap(Object[] arr, int a, int b) {
	Object tmp = arr[a];
	arr[a] = arr[b];
	arr[b] = tmp;
    }

    //
    // Sorts arr between from and to, inclusive.  This is a quick, off-the-top-
    // of-my-head quicksort:  I haven't put any thought into optimizing it.
    // I _did_ put thought into making sure it's safe (it will always
    // terminate).  Worst-case it's O(n^2), but it will usually run in
    // in O(n log n).  It's well-behaved if the list is already sorted,
    // or nearly so.
    //
    static private void quickSort(Object[] arr, Comparer c, int from, int to) {
	if (to <= from)
	    return;
	int mid = (from + to) / 2;
	if (mid != from)
	    swap(arr, mid, from);
	Object pivot = arr[from];   // Simple-minded, but reasonable
	int highestBelowPivot = from - 1;
	int low = from+1;
	int high = to;
	    // We now move low and high toward each other, maintaining the
	    // invariants:
	    //	    arr[i] <= pivot    for all i < low
	    //	    arr[i] > pivot     for all i > high
	    // As long as these invariants hold, and every iteration makes
	    // progress, we are safe.
	while (low <= high) {
	    int cmp = c.compare(arr[low], pivot);
	    if (cmp <= 0) {   // arr[low] <= pivot
		if (cmp < 0) {
		    highestBelowPivot = low;
		}
		low++;
	    } else {
		int c2;
		for (;;) {
			// arr[high] > pivot:
		    c2 = c.compare(arr[high], pivot);
		    if (c2 > 0) {
			high--;
			if (low > high) {
			    break;
			}
		    } else {
			break;
		    }
		}
		// At this point, low is never == high, BTW
		if (low <= high) {
		    swap(arr, low, high);
		    if (c2 < 0) {
			highestBelowPivot = low;
		    }
		    low++;
		    high--;
		}
	    }
	}
	// At this point, low == high+1
	// Now we just need to sort from from..highestBelowPivot
	// and from high+1..to
	if (highestBelowPivot > from) {
	    // pivot == pivot, so ensure algorithm terminates
	    swap(arr, from, highestBelowPivot);
	    quickSort(arr, c, from, highestBelowPivot-1);
	}
	quickSort(arr, c, high+1, to);
    }
}


