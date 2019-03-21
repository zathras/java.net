/*
 * HttpTranferStatus.java
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
 * This class is a container for transferred bytes count information.
 * @author Tommi Laukkanen (tlaukkanen at gmail dot com)
 */
public class HttpTransferStatus {

    private static long totalBytesReceived = 0;
    private static long totalBytesSent = 0;
    
    public static long getTotalBytesReceived() {
        return totalBytesReceived;
    }
    
    public static long getTotalBytesSent() {
        return totalBytesSent;
    }
    
    public static long getTotalBytesTransfered() {
        return totalBytesReceived + totalBytesSent;
    }
    
    public static void addReceivedBytes(long byteCount) {
        totalBytesReceived += byteCount;
    }
    
    public static void addSentBytes(long byteCount) {
        totalBytesSent += byteCount;
    }
    
}
