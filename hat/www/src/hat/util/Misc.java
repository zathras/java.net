
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
 * Miscellaneous functions I couldn't think of a good place to put.
 *
 * @version     1.2, 03/06/98
 * @author      Bill Foote
 */


public class Misc {

    private static char[] digits = { '0', '1', '2', '3', '4', '5', '6', '7',
                                     '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };
    
    public final static String toHex(int addr) {
	char[] buf = new char[8];
	int i = 0;
	for (int s = 28; s >= 0; s -= 4) {
	    buf[i++] = digits[(addr >> s) & 0xf];
	}
	return new String(buf);
    }
}
