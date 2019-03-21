
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

/**
 * Base class for comparison of two objects.
 * @see VectorSorter
 *
 * @version     1.3, 03/06/98
 * @author      Bill Foote
 */

abstract public class Comparer {

    /**
     * @return a number <, == or > 0 depending on lhs compared to rhs
     * @see java.lang.String.compareTo
    **/
    abstract public int compare(Object lhs, Object rhs);
}

