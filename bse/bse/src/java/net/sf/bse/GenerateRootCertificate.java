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
import java.math.BigInteger;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.Hashtable;
import java.util.Map;

import org.bouncycastle.asn1.DERConstructedSequence;
import org.bouncycastle.asn1.DERIA5String;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.asn1.x509.X509Extensions;
import org.bouncycastle.asn1.x509.X509Name;
import org.bouncycastle.jce.X509KeyUsage;
import org.bouncycastle.jce.X509V3CertificateGenerator;

/**
 * Command to generate an MHP root certificate.
 *
 * @author Bill Foote (bill.foote@sun.com)
 * @author Aleksi Peebles (aleksi.peebles@infocast.fi)
 * @version $Revision: 1.1 $ $Date: 2003/01/17 14:40:19 $
 */
public class GenerateRootCertificate extends Command
{    
    public GenerateRootCertificate(Map args)
    {
        super(args);
    }
    
    public void usageMessage(PrintStream out)
    {
        out.println(
"Command:  root\n\n" +

"    Generates an MHP root certificate\n\n" +

"    Arguments:\n\n" +

"        name:        Subject commonName of root CA\n" +
"        country:     Subject countryName of root CA\n" +
"        email:       Subject e-mail address of root CA\n" +
"        validFrom:   Date cert is valid from, in dd/mm/yyyy format\n" +
"        validUntil:  Date cert is valid until, in dd/mm/yyyy format\n" +
"        serial:      Serial number of certificate\n" +
"        strength:    Length of key in bits\n" +
"        file:        Where to store the results\n");
    }
    
    public String[] getRequiredArgs()
    {
        return new String[] { "name:", "country:", "email:", "validFrom:", 
            "validUntil:", "serial:", "strength:", "file:" };
    }
    
    public String[] getOptionalArgs()
    {
        return null;
    }
    
    public void run() throws Exception
    {
        System.out.println("Generating root certificate.");
        
        // Do a bit of argument checking before time-consuming operations...
        getDateArg("validFrom:");
        getDateArg("validUntil:");
        
        KeyPairGenerator kpGen = KeyPairGenerator.getInstance("RSA", "BC");

        // key bit length, 4096 is max guaranteed by MHP
        int strength = Integer.parseInt(getArg("strength:"));
        
        kpGen.initialize(strength, new SecureRandom());
        
        System.out.println("Generating key pair. This may take a few minutes.");
        KeyPair pair = kpGen.genKeyPair();
        
        X509V3CertificateGenerator cg = new X509V3CertificateGenerator();
        cg.reset();

        X509Name subject = null;
        Hashtable ht = new Hashtable();
        ht.put(X509Name.C, getArg("country:"));
        ht.put(X509Name.CN, getArg("name:"));
        subject = new X509Name(ht);

        cg.setIssuerDN(subject); // Root CA: issuer == subject
        cg.setSubjectDN(subject);
        cg.setNotBefore(getDateArg("validFrom:"));
        cg.setNotAfter(getDateArg("validUntil:"));
        cg.setPublicKey(pair.getPublic());
        cg.setSerialNumber(new BigInteger(getArg("serial:")));
        cg.setSignatureAlgorithm("MD5WITHRSA"); // SHA1WITHRSA is OK, too
        
        cg.addExtension(X509Extensions.KeyUsage.getId(), true,
            new X509KeyUsage(
            X509KeyUsage.keyCertSign)); // leaf KeyUsage is different
        
        GeneralName gn = new GeneralName(new DERIA5String(getArg("email:")), 1);
        DERConstructedSequence seq = new DERConstructedSequence();
        seq.addObject(gn);
        GeneralNames san = new GeneralNames(seq);
        cg.addExtension(
            X509Extensions.SubjectAlternativeName.getId(), false, san);
        
        cg.addExtension(
            X509Extensions.IssuerAlternativeName.getId(), false, san);
        
        X509Certificate cert = 
            cg.generateX509Certificate(pair.getPrivate(), "BC");
        
        // Now, write private key
        Key key = pair.getPrivate();
        String fn = getArg("file:") + "_private.pkcs";
        System.out.println("Writing root private key in "
            + key.getFormat() + " format to " + fn + ".");
        FileOutputStream str = new FileOutputStream(fn);
        str.write(key.getEncoded());
        str.close();

        // Then public certificate
        fn = getArg("file:") + "_public.x509.crt";
        System.out.println("Writing root cert to " + fn + ".");
        str = new FileOutputStream(fn);
        str.write(cert.getEncoded());
        str.close();

        System.out.println("Done!");
        System.out.println();
    }
}