

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

import java.io.PrintWriter;

import hat.model.*;

import java.net.URLEncoder;
import java.io.UnsupportedEncodingException;

/**
 *
 * @version     1.3, 11/13/97
 * @author      Bill Foote
 */


abstract class QueryHandler {

    protected String urlStart;
    protected String query;
    protected PrintWriter out;
    protected Snapshot snapshot;

    abstract void run();


    void setUrlStart(String s) {
	urlStart = s;
    }

    void setQuery(String s) {
	query = s;
    }

    void setOutput(PrintWriter o) {
	this.out = o;
    }

    void setSnapshot(Snapshot ss) {
	this.snapshot = ss;
    }

    protected String encodeForURL(String s) {
	try {
	    s = URLEncoder.encode(s, "UTF-8");
	} catch (UnsupportedEncodingException ex) {
	    // Should never happen
	    ex.printStackTrace();
	}
	return s;
    }

    protected void startHtml(String title) {
	out.print("<html><title>");
	print(title);
	out.println("</title>");
	out.println("<body bgcolor=\"#ffffff\"><center><h1>");
	print(title);
	out.println("</h1></center>");
    }

    protected void endHtml() {
	out.println("</body></html>");
    }

    protected void error(String msg) {
	out.println(msg);
    }

    protected void printAnchorStart() {
	out.print("<a href=\"");
	out.print(urlStart);
    }

    protected void printThingAnchorTag(int id) {
	printAnchorStart();
	out.print("object/");
	printHex(id);
	out.print("\">");
    }

    protected void printObject(JavaObject obj) {
	printThing(obj);
    }

    protected void printThing(JavaThing thing) {
	if (thing == null) {
	    out.print("null");
	    return;
	}
	if (thing instanceof JavaHeapObject) {
	    JavaHeapObject ho = (JavaHeapObject) thing;
	    int id = ho.getId();
	    if (id != -1) {
		if (ho.isNew())
	        out.println("<strong>");
		printThingAnchorTag(id);
	    }
	    print(thing.toString());
	    if (id != -1) {
		print("@0x");
		printHex(ho.getId());
		if (ho.isNew())
		    out.println("[new]</strong>");
		out.print(" (" + ho.getSize() + " bytes)");
		out.println("</a>");
	    }
	} else {
	    print(thing.toString());
	}
    }

    protected void printRoot(Root root) {
	StackTrace st = root.getStackTrace();
	if (st != null) {
	    printAnchorStart();
	    out.print("rootStack/");
	    printHex(root.getIndex());
	    out.print("\">");
	}
	print(root.getDescription());
	if (st != null) {
	    out.print("</a>");
	}
    }

    protected void printClass(JavaClass clazz) {
	if (clazz == null) {
	    out.println("null");
	    return;
	}
	String name = clazz.getName();
	printAnchorStart();
	out.print("class/");
	print(encodeForURL(name));
	out.print("\">");
	print(clazz.toString());
	out.println("</a>");
    }

    protected void printField(JavaField field) {
	out.print(field.getName() + " (" + field.getSignature() + ")");
    }

    protected void printStatic(JavaStatic member) {
	JavaField f = member.getField();
	printField(f);
	out.print(" : ");
	if (f.hasId()) {
	    JavaThing t = member.getValue();
	    printThing(t);
	} else {
	    out.print(member.getValue());
	}
    }
    
    protected void printStackTrace(StackTrace trace) {
	StackFrame[] frames = trace.getFrames();
	for (int i = 0; i < frames.length; i++) {
	    StackFrame f = frames[i];
	    String clazz = f.getClassName();
	    out.print("<font color=purple>");
	    print(clazz);
	    out.print("</font>");
	    print("." + f.getMethodName() + "(" + f.getMethodSignature() + ")");
	    out.print(" <bold>:</bold> ");
	    print(f.getSourceFileName() + " line " + f.getLineNumber());
	    out.println("<br>");
	}
    }

    protected void printHex(int addr) {
	out.print(hat.util.Misc.toHex(addr));
    }

    protected int parseHex(String value) {
	int result = 0;
	for(int i = 0; i < value.length(); i++) {
	    result *= 16;
	    char ch = value.charAt(i);
	    if (ch >= '0' && ch <= '9') {
		result += (ch - '0');
	    } else if (ch >= 'a' && ch <= 'f') {
		result += (ch - 'a') + 10;
	    } else if (ch >= 'A' && ch <= 'F') {
		result += (ch - 'A') + 10;
	    } else {
		throw new NumberFormatException("" + ch 
					+ " is not a valid hex digit");
	    }
	}
	return result;
    }

    protected void print(String str) {
	for (int i = 0; i < str.length(); i++) {
	    char ch = str.charAt(i);
	    if (ch == '<') {
		out.print("&lt;");
	    } else if (ch == '>') {
		out.print("&gt;");
	    } else if (ch == '"') {
		out.print("&quot;");
	    } else if (ch == '&') {
		out.print("&amp;");
	    } else if (ch < ' ') {
		// do nothing
	    } else {
		out.print(ch);
	    }
	}
    }

}
