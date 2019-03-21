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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.security.cert.Certificate;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

import org.bouncycastle.jce.provider.JDKX509CertificateFactory;
import org.bouncycastle.jce.provider.X509CertificateObject;

/**
 * Abstract class representing a command.
 *
 * @author Bill Foote (bill.foote@sun.com)
 * @version $Revision: 1.1 $ $Date: 2003/01/17 14:40:16 $
 */
public abstract class Command
{    
    private Map arguments;
    
    protected Command(Map arguments)
    {
        this.arguments = arguments;
    }
    
    /**
     * @return the given argument, or null if it's not there
     */
    protected String getArg(String key)
    {
        return (String)arguments.get(key);
    }
    
    protected Date getDateArg(String key)
    {
        DateFormat df = 
            DateFormat.getDateInstance(DateFormat.SHORT, Locale.FRANCE);
        try
        {
            df.setLenient(false);
            return df.parse(getArg(key));
        } 
        catch (ParseException ex)
        {
            System.err.println();
            System.err.println(
                "Cannot parse the date given for \"" + key + "\".");
            System.err.println(ex.toString());
            System.err.println();
            System.exit(1);
            return null;
        }
    }
    
    protected byte[] readBytesFromFile(String fn) throws IOException
    {
        FileInputStream fis = new FileInputStream(fn);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        int ch;
        while ((ch = fis.read()) != -1)
        {
            bos.write(ch);
        }
        return bos.toByteArray();
    }
    
    protected X509CertificateObject readX509(ByteArrayInputStream bis)
        throws Exception
    {
        JDKX509CertificateFactory cf = new JDKX509CertificateFactory();
        // I'm really supposed to use the generic API, but I ended up
        // going straight to the implementation as part of debugging.
        // It works now, so I don't really want to experiment with
        // switching back.
        Certificate cert = cf.engineGenerateCertificate(bis);
        return (X509CertificateObject) cert;
    }
    
    public abstract void usageMessage(PrintStream out);
    
    public abstract String[] getRequiredArgs();
    
    public abstract String[] getOptionalArgs();
    
    /**
     * Execute this command.
     */
    public abstract void run() throws Exception;    
}