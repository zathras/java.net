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

import com.hdcookbook.grin.SEShow;
import com.hdcookbook.grin.Show;
import com.hdcookbook.grin.Feature;
import com.hdcookbook.grin.SENode;
import com.hdcookbook.grin.SEShowVisitor;
import com.hdcookbook.grin.features.Modifier;
import com.hdcookbook.grin.commands.Command;
import com.hdcookbook.grin.io.ShowBuilder;
import com.hdcookbook.grin.io.binary.GrinDataOutputStream;
import com.hdcookbook.grin.io.text.ExtensionParser;
import com.hdcookbook.grin.io.text.Lexer;
import com.hdcookbook.grin.util.AssetFinder;

import java.io.IOException;

/*
 * An example of how to use a custom extension parser.
 *
 * This class gets loaded and used only on JavaSE.  This class implements
 * ExtensionParser, so that the grin text file parser can call into it
 * for parsing EXAMPLE:oval custom Feature.  Also, this class extends Oval, 
 * to inherit the extension functionality
 * defined in Oval, so that GrinView can demonstrate the Oval extension feature 
 * when working with a grin text show file as opposed to the binary.
 * 
 * Note that it is not a requirement for the SE version of the extension to be
 * a subclass of the ME extension class.  The only requirement is that
 * the SE version be a type of SENode.  It is easier to be a subclass to 
 * implement SENode.writeInstanceData(GrinDataOutputStream) in the SE version,
 * but one can choose to create a completely different class.
 */

public class OvalExtensionCompiler extends Oval 
        implements ExtensionParser, SENode {
        
    public OvalExtensionCompiler() {
        super(null);  // Called by the parser.  Show object can only be set later here.  
    }
    
    public OvalExtensionCompiler(Show show) {
        super(show);  // Called by the binary reader.  Show is already determined.
    }

    public Feature getFeature(Show show, String typeName, String name, Lexer lexer)
            throws IOException {

        if ("EXAMPLE:oval".equals(typeName)) {

            // arguments are - x, y, w, h, color_value ";", where color_value is "{" r g b a "}".
            this.x = lexer.getInt();
            this.y = lexer.getInt();
            this.w = lexer.getInt();
            this.h = lexer.getInt();

            lexer.parseExpected("{");
            int r = lexer.getInt();
            int g = lexer.getInt();
            int b = lexer.getInt();
            int a = lexer.getInt();
            lexer.parseExpected("}");

            lexer.parseExpected(";");

            this.color = AssetFinder.getColor(r, g, b, a);
            this.name = name;
            this.show = show;

            return this;
        }

        return null;
    }

    public Modifier getModifier(Show show, String typeName, String name, Lexer lexer)
            throws IOException {
        return null; // not used in this example
    }

    public Command getCommand(Show show, String typeName, Lexer lexer)
            throws IOException {
        return null; // not used in this example
    }

    public void writeInstanceData(GrinDataOutputStream out) throws IOException {
        
        out.writeSuperClassData(this);
        
        out.writeInt(this.x);
        out.writeInt(this.y);
        out.writeInt(this.w);
        out.writeInt(this.h);
        out.writeColor(this.color);
        
    }

    /** 
     * For xlet runtime, instantiate and use the base class, Oval.
     */
    public String getRuntimeClassName() {
        return Oval.class.getName();
    }

    /**
     * For a visitor method
     */
    public void accept(SEShowVisitor visitor) {
        visitor.visitUserDefinedFeature(this);
    } 

    /**
     * {@inheritDoc}
     **/
    public void postProcess(ShowBuilder builder) throws IOException {
    }

    /**
     * {@inheritDoc}
     **/
    public void changeFeatureReference(Feature from, Feature to) {
    }
}
