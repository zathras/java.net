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


/** 
 * The extensions parser for grinbunny.  This is needed for the GB:Arc
 * extension feature.  This is mostly meant to serve as an example of
 * how you add an extension feature to GRIN.
 */

package com.hdcookbook.grinbunny;

import com.hdcookbook.grin.Feature;
import com.hdcookbook.grin.Show;
import com.hdcookbook.grin.commands.Command;
import com.hdcookbook.grin.features.Modifier;
import com.hdcookbook.grin.io.text.ExtensionParser;
import com.hdcookbook.grin.io.text.ExtensionParserList;
import com.hdcookbook.grin.io.text.Lexer;
import com.hdcookbook.grin.media.MediaExtensionParser;

import com.hdcookbook.grin.fontstrip.FontStripExtensionCompiler;

import java.awt.Color;
import java.io.IOException;


public class BunnyExtensionParser implements ExtensionParser {

    ExtensionParserList otherParser;
   
    public BunnyExtensionParser() {
        otherParser = new ExtensionParserList();
        otherParser.addParser(new FontStripExtensionCompiler());
        otherParser.addParser(new MediaExtensionParser());
    }

    /**
     * {@inheritDoc}
     **/
    public Feature getFeature(Show show, String typeName,
                              String name, Lexer lexer)
                   throws IOException
    {
        if (typeName == null) {
            return null;
        }
        
        if (typeName.equals("GB:Arc")) {
            return parseArc(show, name, lexer); 
        } else if (typeName.startsWith("GB:")) {
            // "GB:" reserved for this parser
            return null;
        } else {
            return otherParser.getFeature(show, typeName, name, lexer);
        } 
    }

    private Feature parseArc(Show show, String name, Lexer lexer)
                throws IOException
    {
        lexer.parseExpected("{");
        int r = lexer.getInt();
        int g = lexer.getInt();
        int b = lexer.getInt();
        int a = lexer.getInt();
        lexer.parseExpected("}");
        checkColorValue("r", r);
        checkColorValue("g", r);
        checkColorValue("b", r);
        checkColorValue("alpha", r);
        Color color = new Color(r, g, b, a);
        lexer.parseExpected("x");
        int x = lexer.getInt();
        lexer.parseExpected("y");
        int y = lexer.getInt();
        lexer.parseExpected("width");
        int width = lexer.getInt();
        lexer.parseExpected("height");
        int height = lexer.getInt();
        lexer.parseExpected("startAngle");
        int startAngle = lexer.getInt();
        lexer.parseExpected("arcAngle");
        int arcAngle = lexer.getInt();
        lexer.parseExpected(";");
        return new SEArc(show, name, x, y, width, height, startAngle, arcAngle,
                         color);
    }

    private void checkColorValue(String name, int value) throws IOException {
        if (value < 0 || value > 255) {
            throw new IOException("Illegal color value for " + name + ":  "
                                  + value);
        }
    }

    /**
     * {@inheritDoc}
     **/
    public Modifier getModifier(Show show, String typeName,
                                String name, Lexer lexer) 
                throws IOException
    {   
        if (typeName == null) {
            return null;
        }
        
        if (typeName.startsWith("GB:")) {
            return null; // reserved for this parser
        } else {
            return otherParser.getModifier(show, typeName, name, lexer);
        } 
    }

    /**
     * {@inheritDoc}
     **/
    public Command getCommand(Show show, String typeName, Lexer lexer)
                           throws IOException
    {   if (typeName == null) {
            return null;
        }
        
        if (typeName.startsWith("GB:")) {
            return null; // reserved for this parser 
        } else {
            return otherParser.getCommand(show, typeName, lexer);
        } 
    }

}
