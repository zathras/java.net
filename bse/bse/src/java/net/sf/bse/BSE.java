package net.sf.bse;

/*
 * Copyright (c) 2002-2003 BSE project contributors 
 * (http://bse.sourceforge.net/)
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.  IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.StreamTokenizer;
import java.security.Security;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

/**
 * Command-line entry point for application.
 *
 * @author Bill Foote (bill.foote@sun.com)
 * @author Aleksi Peebles (aleksi.peebles@infocast.fi)
 * @version $Revision: 1.4 $ $Date: 2004/06/24 08:30:03 $
 */
public class BSE
{
    private static HashMap arguments = new HashMap();
    
    public static void main(String[] args)
    {
        System.out.println();
        System.out.println("Broadcast Signing Engine, version 0.3.2");
        System.out.println("Feedback to bse-users@lists.sourceforge.net");
        System.out.println();
        
        Security.addProvider(new BouncyCastleProvider());
        String commandString;
        if (args.length <= 0)
        {
            usage();
        }
        if (args[0].equals("-gui"))
        {
            if (args.length > 1)
            {
                usage();
                return;
            }
            System.out.println("Starting GUI...\n");
            new net.sf.bse.gui.BSEGUI().show();
            return;
        }
        else if (args[0].equals("-args"))
        {
            if (args.length != 2)
            {
                usage();
                return;
            }
            try
            {
                args = readArgs(args[1]);
            }
            catch (IOException e)
            {
                System.err.println("Error reading file \"" + args[1] + "\"");
                System.err.println(e.getMessage());
                System.err.println();
                System.exit(2);
            }            
        }
        processArgs(args);
        Command command = null;
        if (args[0].equals("root"))
        {
            command = new GenerateRootCertificate(arguments);
        } 
        else if (args[0].equals("request"))
        {
            command = new GenerateLeafRequest(arguments);
        } 
        else if (args[0].equals("sign"))
        {
            command = new SignLeafCertificate(arguments);
        } 
        else if (args[0].equals("xlet"))
        {
            command = new SignXlet(arguments);
        }
        
        if (command == null)
        {
            System.err.println();
            System.err.println(
                "The command \"" + args[0] + "\" is not recognized.");
            usage();
        } 
        else
        {
            validateArgs(command);
            try
            {
                command.run();
            } 
            catch (Exception ex)
            {
                System.err.println();
                System.err.println("Execution failed:");
                ex.printStackTrace(System.err);
                System.err.println();
                System.exit(1);
            }
            System.exit(0);
        }
    }
    
    private static String[] readArgs(String fileName) throws IOException
    {
        ArrayList result = new ArrayList();
        BufferedReader r = new BufferedReader(new FileReader(fileName));
        StreamTokenizer st = new StreamTokenizer(r);
        st.commentChar('#');
        st.ordinaryChars('0', '9');
        st.ordinaryChar('-');
        st.ordinaryChar('.');
        st.wordChars('0', '9');
        st.wordChars('!', '!');
        st.wordChars('$', '/');
        st.wordChars('-', '-');
        st.wordChars(':', '@');
        st.wordChars('.', '.');
        st.wordChars('[', '`');
        st.wordChars('{', '~');
        st.wordChars((char)128, (char)255);
        while (true)
        {
            int res = st.nextToken();
            if (res == StreamTokenizer.TT_EOF)
            {
                break;
            } 
            else if (
              res == StreamTokenizer.TT_WORD || res == '"' || res == '\'')
            {
                result.add(st.sval);
            }
        }
        r.close();
        return (String[])result.toArray(new String[result.size()]);
    }
    
    private static void processArgs(String[] args)
    {
        for (int i = 1; i < args.length;)
        {
            String key = args[i++];
            if (i >= args.length)
            {
                System.err.println();
                System.err.println("The attribute \"" + key + 
                    "\" has no associated value. Other arguments:");
                System.err.println("    command:  " + args[0]);
                
                for (Iterator it = arguments.keySet().iterator(); it.hasNext();)
                {
                    Object att = it.next();
                    System.err.println("    attribute:  " + att);
                    System.err.println("        value:  " + arguments.get(att));
                }
                System.err.println();
                System.exit(2);                
            } 
            else
            {
                String value = args[i++];
                arguments.put(key, value);
            }
        }
    }
    
    private static String getArg(String key)
    {
        return (String)arguments.get(key);
    }
    
    public static void validateArgs(Command command)
    {
        String[] required = command.getRequiredArgs();
        String[] optional = command.getOptionalArgs();
        HashMap allArgs = new HashMap();
        if (required != null)
        {
            for (int i = 0; i < required.length; i++)
            {
                allArgs.put(required[i], required[i]);
                if (getArg(required[i]) == null)
                {
                    System.err.println(
                        "Missing required argument \"" + required[i] + "\"\n");
                    command.usageMessage(System.err);
                    System.exit(1);
                }
            }
        }
        
        if (optional != null)
        {
            for (int i = 0; i < optional.length; i++)
            {
                allArgs.put(optional[i], optional[i]);
            }
        }        
        for (Iterator it = arguments.keySet().iterator(); it.hasNext();)
        {
            Object arg = it.next();
            if (! allArgs.containsKey(arg))
            {
                System.err.println();
                System.err.println("Unrecognized argument \"" + arg + "\"");
                command.usageMessage(System.err);
                System.exit(1);
            }
        }
         
    }
    
    private static void usage()
    {
        System.err.println(
        "Usage:  bse command arguments\n" +
        "   or:  bse -args argfile\n" +
        "   or:  bse -gui\n\n" +
        
        "Commands:\n\n" +
        
        "    root     Generate an MHP root certificate.\n" +
        "    request  Generate a request for an MHP leaf certificate.\n" +
        "    sign     Respond to a certificate request by signing an X509\n" +
        "             certificate.\n" +
        "    xlet     Sign an Xlet.\n\n" +
        
        "The arguments take the form of attribute/value pairs, like\n" +
        "\"file: /tmp/myCert\".  Used with -args, argfile must be a\n" +
        "text file, containing the arguments in the above format.\n" +
        "Quoted strings may appear in this file.\n"
        );
        
        System.exit(1);
    }       
}