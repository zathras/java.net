/*
 * StringUtil.java
 *
 * Copyright (C) 2005-2008 Tommi Laukkanen
 * http://www.substanceofcode.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.substanceofcode.utils;

import java.io.UnsupportedEncodingException;
import java.util.Vector;
import com.sugree.utils.URLEncoder;

/**
 *
 * @author Tommi Laukkanen (tlaukkanen at gmail dot com)
 */
public class StringUtil {

    /** Creates a new instance of StringUtil */
    private StringUtil() {
    }

    /**
     * Split string into multiple strings
     * @param original      Original string
     * @param separator     Separator string in original string
     * @return              Splitted string array
     */
    public static String[] split(String original, String separator) {
        Vector nodes = new Vector();

        // Parse nodes into vector
        int index = original.indexOf(separator);
        while (index >= 0) {
            nodes.addElement(original.substring(0, index));
            original = original.substring(index + separator.length());
            index = original.indexOf(separator);
        }
        // Get the last node
        nodes.addElement(original);

        // Create splitted string array
        String[] result = new String[nodes.size()];
        if (nodes.size() > 0) {
            for (int loop = 0; loop < nodes.size(); loop++) {
                result[loop] = (String) nodes.elementAt(loop);
            }
        }
        return result;
    }

    /* Replace all instances of a String in a String.
     *   @param  s  String to alter.
     *   @param  f  String to look for.
     *   @param  r  String to replace it with, or null to just remove it.
     */
    public static String replace(String s, String f, String r) {
        if (s == null) {
            return s;
        }
        if (f == null) {
            return s;
        }
        if (r == null) {
            r = "";
        }
        int index01 = s.indexOf(f);
        while (index01 != -1) {
            s = s.substring(0, index01) + r + s.substring(index01 + f.length());
            index01 += r.length();
            index01 = s.indexOf(f, index01);
        }
        return s;
    }

    /**
     * Method removes HTML tags from given string.
     *
     * @param text  Input parameter containing HTML tags (eg. <b>cat</b>)
     * @return      String without HTML tags (eg. cat)
     */
    public static String removeHtml(String text) {
        try {
            int idx = text.indexOf("<");
            if (idx == -1) {
                text = decodeEntities(text);
                return text;
            }

            String plainText = "";
            String htmlText = text;
            int htmlStartIndex = htmlText.indexOf("<", 0);
            if (htmlStartIndex == -1) {
                return text;
            }
            htmlText = StringUtil.replace(htmlText, "</p>", "\r\n");
            htmlText = StringUtil.replace(htmlText, "<br/>", "\r\n");
            htmlText = StringUtil.replace(htmlText, "<br>", "\r\n");
            while (htmlStartIndex >= 0) {
                plainText += htmlText.substring(0, htmlStartIndex);
                int htmlEndIndex = htmlText.indexOf(">", htmlStartIndex);
                htmlText = htmlText.substring(htmlEndIndex + 1);
                htmlStartIndex = htmlText.indexOf("<", 0);
            }
            plainText = plainText.trim();
            plainText = decodeEntities(plainText);
            return plainText;
        } catch (Exception e) {
            return text;
        }
    }

    public static String decodeEntities(String html) {
        String result = StringUtil.replace(html, "&lt;", "<");
        result = StringUtil.replace(result, "&gt;", ">");
        result = StringUtil.replace(result, "&nbsp;", " ");
        result = StringUtil.replace(result, "&amp;", "&");
        result = StringUtil.replace(result, "&auml;", "ä");
        result = StringUtil.replace(result, "&ouml;", "ö");
        result = StringUtil.replace(result, "&quot;", "'");
        result = StringUtil.replace(result, "&lquot;", "'");
        result = StringUtil.replace(result, "&rquot;", "'");
        result = StringUtil.replace(result, "&#xd;", "\r");
        return result;
    }


    /** URL encode given string */
    public static String urlEncode(String s) {
        if (s != null) {
                        try {
                                return URLEncoder.encode(s, "UTF-8");
                        } catch (UnsupportedEncodingException ue) {
                                try {
                                        s = new String(s.getBytes("UTF-8"), "ISO-8859-1");
                                } catch (UnsupportedEncodingException e) {
                                }
                                StringBuffer tmp = new StringBuffer();
                                try {
                                        for (int i=0; i<s.length(); i++) {
                                                int b = (int) s.charAt(i);
                                                if ((b >= 0x30 && b <= 0x39) || (b >= 0x41 && b <= 0x5A) || (b >= 0x61 && b <= 0x7A)) {
                                                        tmp.append((char) b);
                                                } else if (b == 0x20) {
                                                        tmp.append("+");
                                                } else {
                                                        tmp.append("%");
                                                        if (b <= 0xf) {
                                                                tmp.append("0");
                                                        }
                                                        tmp.append(Integer.toHexString(b));
                                                }
                                        }
                                } catch (Exception e) {
                                }
                                return tmp.toString();
                        }
                }
        return null;
    }
}
