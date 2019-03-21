/*  
 * Copyright (c) 2008, Sun Microsystems, Inc.
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
package com.hdcookbook.bookmenu.menu;

import com.hdcookbook.grin.Feature;
import com.hdcookbook.grin.Show;
import com.hdcookbook.grin.commands.Command;
import com.hdcookbook.grin.io.text.SEGenericCommand;
import com.hdcookbook.grin.features.Modifier;
import com.hdcookbook.grin.io.text.SEGenericModifier;
import com.hdcookbook.grin.io.binary.GrinDataOutputStream;
import com.hdcookbook.grin.io.text.ExtensionParser;
import com.hdcookbook.grin.io.text.Lexer;
import java.io.IOException;
import java.util.ArrayList;

public class MenuExtensionParser implements ExtensionParser {

    public Feature getFeature(Show show, String typeName, String name, Lexer lexer) 
            throws IOException {
        return null; // Custom feature is not used in the MenuXlet
    }

    public Modifier getModifier(Show show, String typeName, final String name, Lexer lexer) 
            throws IOException {
            String arg = lexer.getString();
            lexer.parseExpected(";");        
            
        Modifier mod = null;
        if ("BOOK:bio_image".equals(typeName)) {
            mod = new SEGenericModifier(show) {
                public void writeInstanceData(GrinDataOutputStream out) 
                        throws IOException {
                    out.writeSuperClassData(this);
                    // nothing specific to this class to record.
                }
                public String getRuntimeClassName() {
                    return BioImageFeature.class.getName();
                }
            };
            mod.setName(name);
        }

        return mod;
    }

    public Command getCommand(Show show, String typeName, Lexer lexer) 
            throws IOException {
       
        ArrayList args = new ArrayList();
        for (;;) {
            String tok = lexer.getString();
            if (tok == null) {
                lexer.parseExpected(";");
            } else if (";".equals(tok)) {
                break;
            } else {
                args.add(tok);
            }
        } 
        
        final String arg = (String) args.get(0);
        
        return new SEGenericCommand(show) {
            public String getArgument() {
                return arg;
            }
            public void execute() { // nothing to do.
            }
            public void writeInstanceData(GrinDataOutputStream out) 
                    throws IOException {
                out.writeSuperClassData(this);
                out.writeString(arg);              
            }
            public String getRuntimeClassName() {
                return PlayVideoCommand.class.getName();
            }   
        };
    }
}
