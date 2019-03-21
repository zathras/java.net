/*
 * HttpAbstractUtil.java
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

/**
 * HttpAbstractUtil
 * 
 * @author Tommi Laukkanen (tlaukkanen at gmail dot com)
 */
public abstract class HttpAbstractUtil {

    protected static String cookie;
    protected static String username;
    protected static String password;
    
    public static String getCookie() {
        return cookie;
    }
    
    public static void setCookie(String value) {
        cookie = value;
    }
    
    public static void setBasicAuthentication(String username, String password) {
        HttpAbstractUtil.username = username;
        HttpAbstractUtil.password = password;
    }
    
    /** Creates a new instance of HttpAbstractUtil */
    public HttpAbstractUtil() {
    }

}
