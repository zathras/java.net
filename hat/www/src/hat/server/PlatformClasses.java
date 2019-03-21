
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

package hat.server;

import hat.model.JavaClass;

import java.util.LinkedList;
import java.io.InputStream;
import java.io.Reader;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.IOException;

/**
 * This class is a helper that determines if a class is a "platform"
 * class or not.  It's a platform class if its name starts with one of
 * the prefixes to be found in /resources/platform_names.txt.
 *
 * @author      Bill Foote
 */

public class PlatformClasses  {

    static String[] names = null;


    public static synchronized String[] getNames() {
	if (names == null) {
	    LinkedList list = new LinkedList();
	    InputStream str 
		= PlatformClasses.class
		    .getResourceAsStream("/resources/platform_names.txt");
	    if (str != null) {
		try {
		    BufferedReader rdr 
			= new BufferedReader(new InputStreamReader(str));
		    for (;;) {
			String s = rdr.readLine();
			if (s == null) {
			    break;
			} else if (s.length() > 0) {
			    list.add(s);
			}
		    }
		    rdr.close();
		    str.close();
		} catch (IOException ex) {
		    ex.printStackTrace();
		    // Shouldn't happen, and if it does, continuing
		    // is the right thing to do anyway.
		}
	    }
	    int num = list.size();
	    names = new String[num];
	    names = (String[]) list.toArray(names);
	}
	return names;
    }


    public static boolean isPlatformClass(String name) {
	String[] nms = getNames();
	for (int i = 0; i < nms.length; i++) {
	    if (name.startsWith(nms[i])) {
		return true;
	    }
	}
	return false;
    }

    /**
     * filters out the platfrom classes from the input array.  The
     * input array is destroyed in the process.
     **/
    public static JavaClass[] filterPlatformClasses(JavaClass[] classes) {
	int num = 0;
	for (int i = 0; i < classes.length; i++) {
	    if (!isPlatformClass(classes[i].getName())) {
		classes[num++] = classes[i];
	    }
	}
	JavaClass[] result = new JavaClass[num];
	for (int i = 0; i < num; i++) {
	    result[i] = classes[i];
	}
	return result;
    }
}
