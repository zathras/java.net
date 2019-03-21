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

import java.io.FileOutputStream;
import java.io.PrintStream;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.util.Map;

import org.bouncycastle.asn1.DERConstructedSequence;
import org.bouncycastle.asn1.DERSet;
import org.bouncycastle.asn1.DERUTF8String;
import org.bouncycastle.asn1.x509.X509Name;
import org.bouncycastle.jce.PKCS10CertificationRequest;

/**
 * Command to generate a request for an MHP leaf certificate.
 *
 * @author Bill Foote (bill.foote@sun.com)
 * @author Aleksi Peebles (aleksi.peebles@infocast.fi)
 * @version $Revision: 1.3 $ $Date: 2004/05/06 09:51:15 $
 */
public class GenerateLeafRequest extends Command
{    
    public GenerateLeafRequest(Map args)
    {
        super(args);
    }
    
    public void usageMessage(PrintStream out)
    {
        out.println(
"Command:  request\n\n" +

"    Generates a request for an MHP leaf certificate\n\n" + 

"    Arguments:\n\n" +

"        name:        Subject commonName of leaf (not including org id)\n" +
"        country:     Subject countryName of leaf\n" +
"        email:       Subject e-mail address of leaf\n" +
"        strength:    Length of key in bits\n" +
"        file:        Where to store the results.\n\n" +

"    Plus, optionally:\n\n" +

"        org:         Subject organisation specific text followed by a\n" +
"                     dot and the organisation ID as eight hex digits\n" +
"                     with leading zeroes\n" +
"        validFrom:   Date cert to be valid from, in dd/mm/yyyy format\n" +
"        validUntil:  Date cert to be valid until, in dd/mm/yyyy format\n");
    }
    
    public String[] getRequiredArgs()
    {
        return new String[] { "name:", "country:", "email:", "strength:", 
            "file:" };
    }

    public String[] getOptionalArgs()
    {
        return new String[] { "org:", "validFrom:", "validUntil:" };
    }

    public void run() throws Exception
    {
        System.out.println("Generating leaf certificate request.");
        
        // Do a bit of argument checking before time-consuming operations...
        if (getArg("validFrom:") != null)
        {
            getDateArg("validFrom:");
        }
        if (getArg("validUntil:") != null)
        {
            getDateArg("validUntil:");
        }
        
        KeyPairGenerator kpGen = KeyPairGenerator.getInstance("RSA");
        
        // key bit length, 4096 is max guaranteed by MHP
        int strength = Integer.parseInt(getArg("strength:"));        
        
        kpGen.initialize(strength, new SecureRandom());
        
        System.out.println("Generating key pair. This may take a few minutes.");
        KeyPair pair = kpGen.genKeyPair();
        
        String privateFile = getArg("file:") + "_private.pkcs";
        String derCsrFile = getArg("file:") + ".der.csr";
        
        // Now, write private key
        Key key = pair.getPrivate();
        System.out.println("Writing root private key in "
            + key.getFormat() + " format to "
            + privateFile + ".");
        FileOutputStream str = new FileOutputStream(privateFile);
        str.write(key.getEncoded());
        str.close();              
        
        System.out.println("Writing request in DER encoded format to " + 
            derCsrFile + ".");
        
        DERConstructedSequence seq = new DERConstructedSequence();
        DERConstructedSequence p = new DERConstructedSequence();
        p.addObject(X509Name.CN);
        p.addObject(new DERUTF8String(getArg("name:")));
        seq.addObject(new DERSet(p));
        p = new DERConstructedSequence();
        p.addObject(X509Name.C);
        p.addObject(new DERUTF8String(getArg("country:")));
        seq.addObject(new DERSet(p));
        p = new DERConstructedSequence();
        p.addObject(X509Name.O);
        p.addObject(new DERUTF8String(getArg("org:")));
        seq.addObject(new DERSet(p));
        p = new DERConstructedSequence();
        p.addObject(X509Name.EmailAddress);
        p.addObject(new DERUTF8String(getArg("email:")));
        seq.addObject(new DERSet(p));
        X509Name subject = new X509Name(seq);
        
        PKCS10CertificationRequest req = 
            new PKCS10CertificationRequest("MD5WITHRSA", subject, 
            pair.getPublic(), null, pair.getPrivate());
        FileOutputStream fos = new FileOutputStream(derCsrFile);
        fos.write(req.getEncoded());
        fos.close();
        
        System.out.println("Done!");
        System.out.println();
        System.out.println("    Please send " + derCsrFile);
        System.out.println("    to your certificate authority.");
        System.out.println("    Keep " + privateFile);
        System.out.println("    in a safe place.");
        System.out.println();
    }
}