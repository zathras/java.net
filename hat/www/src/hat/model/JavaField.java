
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
 *
 * @version     1.3, 03/06/98
 * @author      Bill Foote
 */

public class JavaField {
    
    private String name;
    private String signature;

    public JavaField(String name, String signature) {
	this.name = name;
	this.signature = signature;
    }


    /**
     * @return true if the type of this field is something that has an ID.
     *		int fields, for exampe, don't.
     */
    public boolean hasId() {
	char ch = signature.charAt(0);
	return (ch == '[' || ch == 'L');
    }

    public String getName() {
	return name;
    }

    public String getSignature() {
	return signature;
    }

}
