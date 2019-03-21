
/*
 * The contents of this file are subject to the Sun Public License
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
 * 
 */

package hat.model;

/**
 * This is used to represent values that the program doesn't really understand.
 * This includes the null vlaue, and unresolved references (which shouldn't
 * happen in well-formed hprof files).
 *
 *
 * @version     1.4, 10/08/98
 * @author      Bill Foote
 */




public class HackJavaValue extends JavaValue {

    private String value;
    private int size;

    public HackJavaValue(String value, int size) {
	this.value = value;
	this.size = size;
    }

    public String toString() {
	return value;
    }

    public int getSize() {
	return size;
    }

}
