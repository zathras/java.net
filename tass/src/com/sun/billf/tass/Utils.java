
package com.sun.billf.tass;

import java.io.IOException;
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;

public class Utils  {
    static String[] readStringList(String fileName) throws IOException {
	BufferedReader in = new BufferedReader(new FileReader(fileName));
	ArrayList list = new ArrayList();	// <string>
	for (;;) {
	    String line = in.readLine();
	    if (line == null) {
		break;
	    }
	    line = line.trim();
	    if (!line.startsWith("#") && !("".equals(line))) {
		list.add(line);
	    }
	}
	return (String[]) list.toArray(new String[list.size()]);
    }

    public static String stripAfterLastDot(String src) {
	String result = "";
	int pos = src.lastIndexOf('.');
	if (pos > 0) {
	    result = src.substring(0, pos);
	}
	return result;
    }
}
