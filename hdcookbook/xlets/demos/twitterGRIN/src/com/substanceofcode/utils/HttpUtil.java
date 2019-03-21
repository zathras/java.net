/*
 * HttpUtil.java
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

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Date;

import com.hdcookbook.grin.util.Debug;

import com.sugree.twitter.JSONTwitterParser;

/**
 *
 * @author Tommi Laukkanen
 */
public class HttpUtil extends HttpAbstractUtil {

    /** Total bytes transfered */
    private static long totalBytes = 0;
        private static String userAgent = "curl/7.18.0 (i486-pc-linux-gnu) libcurl/7.18.0 OpenSSL/0.9.8g zlib/1.2.3.3 libidn/1.1";
        private static boolean alternateAuthen = false;
        private static boolean optimizeBandwidth = true;
        private static String contentType = "application/x-www-form-urlencoded";
    
    /** Creates a new instance of HttpUtil */
    public HttpUtil() {
    }

        public static void setUserAgent(String userAgent) {
                HttpUtil.userAgent = userAgent;
        }

        public static void setAlternateAuthentication(boolean flag) {
                HttpUtil.alternateAuthen = flag;
        }

        public static void setOptimizeBandwidth(boolean flag) {
                HttpUtil.optimizeBandwidth = flag;
        }

        public static void setContentType(String contentType) {
                if (contentType == null) {
                        HttpUtil.contentType = "application/x-www-form-urlencoded";
                } else {
                        HttpUtil.contentType = contentType;
                }
        }

    public static String doPost(String url, String query) throws IOException, Exception {
        return doRequest(url, prepareQuery(query), "POST");
    }

    public static String doPost(String url, byte[] query) throws IOException, Exception {
        return doRequest(url, query, "POST");
    }

    public static String doGet(String url, String query) throws IOException, Exception {
                String fullUrl = url;
                query = prepareQuery(query);
                if (query.length() > 0) {
                        fullUrl += "?"+query;
                }
        return doRequest(fullUrl, "", "GET");
    }

    public static String doRequest(String url, String query, String requestMethod) throws IOException, Exception {
                return doRequest(url, query.getBytes(), requestMethod);
        }

    public static String doRequest(String url, byte[] query, String requestMethod) throws IOException, Exception {
                String response = "";
        int status = -1;
        String message = null;
                int depth = 0;
                boolean redirected = false;
        String auth = null;
        InputStream is = null;
        OutputStream os = null;
        HttpURLConnection con = null;
                long timeOffset = new Date().getTime();

        while (con == null) {
            con = (HttpURLConnection) (new URL(url)).openConnection();
            con.setRequestMethod(requestMethod);
            if (!alternateAuthen && username != null && password != null && username.length() > 0) {
                String userPass;
                Base64 b64 = new Base64();
                userPass = username + ":" + password;
                userPass = b64.encode(userPass.getBytes());
                con.setRequestProperty("Authorization", "Basic " + userPass);
            }
            con.setRequestProperty("User-Agent", HttpUtil.userAgent);
                        if (!optimizeBandwidth) {
                    con.setRequestProperty("Accept", "*/*");
                        }

            if(query.length > 0) {
                con.setRequestProperty("Content-Type", contentType);
                    con.setRequestProperty("Content-Length", "" + query.length);
                os = con.getOutputStream();
                //os.write(query);
                                int n = query.length;
                for(int i = 0; i < n; i++) {
                                        os.write(query[i]);
                                        if (i%500 == 0 || i == n-1) {
                                        }
                }
                    os.close();
                    os = null;
            }

            status = con.getResponseCode();
            message = con.getResponseMessage();
                        timeOffset = con.getDate()-timeOffset+new Date().getTime();
            switch (status) {
                                case HttpURLConnection.HTTP_OK:
                                case HttpURLConnection.HTTP_NOT_MODIFIED:
                                case HttpURLConnection.HTTP_BAD_REQUEST:
                                        break;
                                case HttpURLConnection.HTTP_MOVED_TEMP:
                                case HttpURLConnection.HTTP_MOVED_PERM:
                                        if (depth > 2) {
                                                throw new IOException("Too many redirect");
                                        }
                                        redirected = true;
                                        url = con.getHeaderField("location");
                                        con.disconnect();
                                        con = null;
                                        depth++;
                                        break;
                                case 100:
                                        throw new IOException("unexpected 100 Continue");
                                default:
                                        con.disconnect();
                                        con = null;
                                        throw new IOException("Response status not OK:"+status+" "+message);
            }
        }

        is = con.getInputStream();
                if (!redirected) {
                        response = getUpdates(con, is, os);
                } else {
            try {
                if (con != null) {
                    con.disconnect();
                }
                if (os != null) {
                    os.close();
                }
                if (is != null) {
                    is.close();
                }
            } catch (IOException ioe) {
                throw ioe;
            }
                }
                if (status == HttpURLConnection.HTTP_BAD_REQUEST) {
                        Debug.println(response);
                        throw new IOException("Response status not OK:"+status+" "+message+" "+JSONTwitterParser.parse400(response));
                }

                // maybe needed?  controller.setServerTimeOffset(new Date().getTime()-timeOffset);
                return response;
        }

    private static String getUpdates(HttpURLConnection con, InputStream is, OutputStream os)  throws IOException {
        StringBuffer stb = new StringBuffer();
                char[] buffer = new char[1024];
        int ch = 0;
                InputStreamReader rdr = new InputStreamReader(is, "UTF-8");
        try {
                        for (;;) {
                                int n = rdr.read(buffer, 0, buffer.length);
                                if (n == -1) {
                                        break;
                                }
                                stb.append(buffer, 0, n);
                        }
        } catch (IOException ioe) {
            throw ioe;
        } finally {
            try {
                if (os != null) {
                    os.close();
                }
                if (rdr != null) {
                    rdr.close();
                }
                if (con != null) {
                    con.disconnect();
                }
            } catch (IOException ioe) {
                throw ioe;
            }
        }
        return stb.toString();
    }

        private static String prepareQuery(String query) {
                if (alternateAuthen && username != null && password != null && username.length() > 0) {
                        String userPass;
                        Base64 b64 = new Base64();
                        userPass = username + ":" + password;
                        userPass = b64.encode(userPass.getBytes());
                        if (query.length() > 0) {
                                query += "&";
                        }
                        query += "__token__="+StringUtil.urlEncode(userPass);
                }
                return query;
        }
}
