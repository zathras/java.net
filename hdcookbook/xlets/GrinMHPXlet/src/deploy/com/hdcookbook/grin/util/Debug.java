
/*  
 * Copyright (c) 2011, Oracle
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
package com.hdcookbook.grin.util;

/**
 * Debugging support.  This overrides the version in the GRIN library, to 
 * turn off debugging in a deployment xlet.
 * <p>
 * See the extensive class comments in the file
 * AuthoringTools/grin/library/src/com/hdcookbook/grin/util/Debug.java
 *
 * @author Bill Foote (http://jovial.com)
 */
public class Debug {

    /**
     * Variable to say that assertions are enabled.  If
     * set false, then javac should strip all assertions
     * out of the generated code.
     * <p>
     * Usage:
     * <pre>
     *     if (Debug.ASSERT && some condition that should be false) {
     *         Debug.println(something interesting);
     *     }
     * </pre>
     * <p>
     * Note that JDK 1.4's assertion facility can't be used
     * for Blu-Ray, since PBP 1.0 is based on JDK 1.3.
     **/
    public final static boolean ASSERT = false;
    /**
     * Debug level.  2 = noisy, 1 = some debug, 0 = none.
     **/
    public final static int LEVEL = 0;
   
    /**
     * Variable to say if time profiling is enabled.
     **/
    public final static boolean PROFILE = false;

    /**
     * Variable to say if animation profiling is enabled.
     *
     * @see Profile
     **/
    public final static boolean PROFILE_ANIMATION = false;

    /**
     * Variable to say if setup profiling is enabled.
     *
     * @see Profile
     **/
    public final static boolean PROFILE_SETUP = false;

    private Debug() {
    }

    public static void println() {
    }

    public static void println(Object o) {
    }

    /**
     * Called on assertion failure.  This is a useful during development:  When
     * you detect a condition that should be impossible, you can trigger an
     * assertion failure.  That means you've found a bug.  When an assertion
     * failure is detected, you basically want to shut everything down,
     * so that the developer notices immediately, and sees the message.
     **/
    public static void assertFail(String msg) {
        if (ASSERT) {
            try {
                throw new RuntimeException("\n***  Assertion failure:  " + msg + "  ***\n");
            } catch (RuntimeException ex) {
                printStackTrace(ex);
            }
            AssetFinder.abort();
        }
    }

    /**
     * Called on assertion failure.  This is a useful during development:  When
     * you detect a condition that should be impossible, you can trigger an
     * assertion failure.  That means you've found a bug.  When an assertion
     * failure is detected, you basically want to shut everything down,
     * so that the developer notices immediately, and sees the message.
     **/
    public static void assertFail() {
        if (ASSERT) {
            assertFail("");
        }
    }

    /**'
     * Print a stack trace to the debug log, if Debug.LEVEL > 0.  Note that you can
     * also easily use this for the equivalent of <code>Thread.dumpStack()</code> using this
     * bit of code:
     * <pre>
     *      try {
     *          throw new RuntimeException("STACK BACKTRACE");
     *      } catch (RuntimeException ex) {
     *          Debug.printStackTrace(ex);
     *      }
     * </pre>
     **/
    public static void printStackTrace(Throwable t) {
    }
}
