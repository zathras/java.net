/*
 * Copyright (c) 2009, Sun Microsystems, Inc.
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *  * Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *  * Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *  * Neither the name of Sun Microsystems nor the names of its contributors
 *    may be used to endorse or promote products derived from this software
 *    without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 *  Note:  In order to comply with the binary form redistribution
 *         requirement in the above license, the licensee may include
 *         a URL reference to a copy of the required copyright notice,
 *         the list of conditions and the disclaimer in a human readable
 *         file with the binary form of the code that is subject to the
 *         above license.  For example, such file could be put on a
 *         Blu-ray disc containing the binary form of the code or could
 *         be put in a JAR file that is broadcast via a digital television
 *         broadcast medium.  In any event, you must include in any end
 *         user licenses governing any code that includes the code subject
 *         to the above license (in source and/or binary form) a disclaimer
 *         that is at least as protective of Sun as the disclaimers in the
 *         above license.
 *
 *         A copy of the required copyright notice, the list of conditions and
 *         the disclaimer will be maintained at
 *         https://hdcookbook.dev.java.net/misc/license.html .
 *         Thus, licensees may comply with the binary form redistribution
 *         requirement with a text file that contains the following text:
 *
 *             A copy of the license(s) governing this code is located
 *             at https://hdcookbook.dev.java.net/misc/license.html
 */

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.InputStream;
import java.io.StreamTokenizer;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.StringTokenizer;
import java.util.NoSuchElementException;

import com.hdcookbook.grin.util.Debug;

/**
 *  A simple parser for parsing the rss feed data from yahoo
 *  weather web service. This parser uses BufferedReader and
 *  StringTokenizer instead of XML parser APIs. When parsing
 *  a simple XML document and this parser is good enough.
 */
class XMLParser {

    public static FeedData parse(InputStream in)
            throws IOException {

        StringTokenizer st;
        FeedData fd = new FeedData();

        BufferedReader br = new BufferedReader(
                new InputStreamReader(in));
        String xmldata;

        xmldata = getElement(br, "location");
        if (xmldata == null) {
            return null;
        }
        st = new StringTokenizer(xmldata, "< > = \"");
        while (st.hasMoreTokens()) {
            String attrName = st.nextToken();
            if (attrName.equals("city")) {
                fd.location = st.nextToken();
                while (!(attrName = st.nextToken()).equals("region")) {
                    fd.location += "  " + attrName;
                }
                Debug.println("location:" + fd.location);
            }
            if (attrName.equals("region")) {
                fd.location += "  " + st.nextToken();
                Debug.println("location:" + fd.location);
            }
            if (attrName.equals("country")) {
                fd.location += "  " + st.nextToken();
                Debug.println("location:" + fd.location);
            }
        }
        xmldata = getElement(br, "wind");
        if (xmldata != null) {
            st = new StringTokenizer(xmldata, "< > = \"");
            while (st.hasMoreTokens()) {
                String attrName = st.nextToken();
                if (attrName.equals("direction")) {
                    fd.direction = st.nextToken();
                    Debug.println("direction:" + fd.direction);
                }
                if (attrName.equals("speed")) {
                    fd.speed = st.nextToken() + " mph";
                    Debug.println("speed:" + fd.speed);
                }
            }
        }

        /* Determine if the present time is a day or
         * night. The day and night information is used in fetching the
         * yahoo icon for the current weather condition
         */
        xmldata = getElement(br, "astronomy");
        String riseH = null, riseM = null;
        String setH = null, setM = null;
        int rh = 0, rm = 0;
        int sh = 0, sm = 0;
        if (xmldata != null) {
            st = new StringTokenizer(xmldata, "< > = : / \"");
            while (st.hasMoreTokens()) {
                String attrName = st.nextToken();
                if (attrName.equals("sunrise")) {
                    riseH = st.nextToken();
                    riseM = st.nextToken();
                }
                if (attrName.equals("sunset")) {
                    setH = st.nextToken();
                    setM = st.nextToken();
                }
            }
            try {
                rh = Integer.parseInt(riseH);
                rm = Integer.parseInt(riseM);
                sh = Integer.parseInt(setH);
                sm = Integer.parseInt(setM);
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
        }
        xmldata = getElement(br, "condition");
        String time = null;
        String am_pm = null;
        if (xmldata != null) {
            st = new StringTokenizer(xmldata, "< > = \" /");
            while (st.hasMoreTokens()) {
                String attrName = st.nextToken();
                if (attrName.equals("text")) {
                    fd.condition = st.nextToken();
                    while (!(attrName = st.nextToken()).equals("code")) {
                        fd.condition += " " + attrName;
                    }
                    Debug.println("condition:" + fd.condition);
                }
                if (attrName.equals("code")) {
                    fd.imageCode = st.nextToken();
                    Debug.println("code:" + fd.imageCode);
                }
                if (attrName.equals("temp")) {
                    fd.temp = st.nextToken() + " F";
                    Debug.println("temp:" + fd.temp);
                }
                if (attrName.equals("date")) {
                    try {
                        st.nextToken();      // ignore day of week
                        st.nextToken();      // ignore day of month
                        st.nextToken();         // ignore month
                        st.nextToken();         // ignore year
                        time = st.nextToken();
                        am_pm = st.nextToken();
                    } catch (NoSuchElementException e) {
                        Debug.printStackTrace(e);
                    }
                }
            }
        }
        fd.isDayTime = isItDayTime(time, am_pm, rh, rm, sh, sm);
        Debug.println("is day time:" + fd.isDayTime);

        xmldata = getElement(br, "forecast");
        if (xmldata != null) {
            st = new StringTokenizer(xmldata, "< > = \"");
            while (st.hasMoreTokens()) {
                String attrName = st.nextToken();
                if (attrName.equals("day")) {
                    fd.day1 = st.nextToken();
                }
                if (attrName.equals("low")) {
                    fd.day1Low = st.nextToken();
                }
                if (attrName.equals("high")) {
                    fd.day1High = st.nextToken();
                }
                if (attrName.equals("text")) {
                    fd.day1Condition = st.nextToken();
                    while (!(attrName = st.nextToken()).equals("code")) {
                        fd.day1Condition += " " + attrName;
                    }
                }
                if (attrName.equals("code")) {
                    fd.day1Code = st.nextToken();
                }
            }
        }
        xmldata = getElement(br, "forecast");
        if (xmldata != null) {
            st = new StringTokenizer(xmldata, "< > = \"");
            while (st.hasMoreTokens()) {
                String attrName = st.nextToken();
                if (attrName.equals("day")) {
                    fd.day2 = st.nextToken();
                }
                if (attrName.equals("low")) {
                    fd.day2Low = st.nextToken();
                }
                if (attrName.equals("high")) {
                    fd.day2High = st.nextToken();
                }
                if (attrName.equals("text")) {
                    fd.day2Condition = st.nextToken();
                    while (!(attrName = st.nextToken()).equals("code")) {
                        fd.day2Condition += " " + attrName;
                    }
                }
                if (attrName.equals("code")) {
                    fd.day2Code = st.nextToken();
                }
            }
        }
        return fd;
    }

    static String getElement(BufferedReader br, String element)
            throws IOException {
        element = "<yweather:" + element;
        String xmldata;
        while ((xmldata = br.readLine()) != null) {
            if (xmldata.startsWith(element)) {
                return xmldata;
            }
        }
        return null;
    }

    public static void main(String args[]) throws IOException {
        FileInputStream in = new FileInputStream(args[0]);
        parse(in);
        in.close();
    }

    /** This code will work for most time zones, where sunrise 
     *  is before 12:00 pm and sunset is before 12:00 am.
     *  For zones where this constraint does not hold, the
     *  solution is not guaranteed to work. However, at this 
     *  point in time, yahoo does not have weather report for
     *  these places.
     */
    public static boolean isItDayTime(String time, String am_pm,
            int rh, int rm,
            int sh, int sm) {
        if ((time == null)  || (am_pm == null)) {
            return true;
        }
        int colonIndex = time.indexOf(":");
        String hour = time.substring(0, colonIndex);
        String min = time.substring(colonIndex + 1);
        int h = 0, m = 0;
        try {
            h = Integer.parseInt(hour);
            m = Integer.parseInt(min);
        } catch (Exception e) {
            Debug.printStackTrace(e);
        }
        boolean dayTime = false;
        if (am_pm.equalsIgnoreCase("AM") && (h != 12)) {
            if (h > rh) {
                dayTime = true;
            } else if ((h == rh) && (m >= rm)) {
                dayTime = true;
            }
        } else {
            if ((h < sh) || (h == 12)) {
                dayTime = true;
            } else if ((h == sh) && (m <= sm)) {
                dayTime = true;
            }
        }
        return dayTime;
    }
}

