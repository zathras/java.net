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
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Collection;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.bouncycastle.asn1.DERConstructedSequence;
import org.bouncycastle.asn1.DERIA5String;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.asn1.x509.X509Extensions;
import org.bouncycastle.asn1.x509.X509Name;
import org.bouncycastle.jce.PKCS10CertificationRequest;
import org.bouncycastle.jce.X509KeyUsage;
import org.bouncycastle.jce.X509V3CertificateGenerator;
import org.bouncycastle.jce.provider.X509CertificateObject;

/**
 * Command to sign a leaf certificate.
 *
 * @author Bill Foote (bill.foote@sun.com)
 * @author Aleksi Peebles (aleksi.peebles@infocast.fi)
 * @version $Revision: 1.3 $ $Date: 2004/05/06 10:00:34 $
 */
//TODO better rename class to SignCertificate as non-leaf certs are now supported?
public class SignLeafCertificate extends Command
{    
    public SignLeafCertificate(Map args)
    {
        super(args);
    }
    
    public void usageMessage(PrintStream out)
    {
        out.println(
"Command:  sign\n\n" +

"    Signs an MHP certificate\n\n" +

"    Arguments:\n\n" +

"        csrFile:     Certificate signing request file\n" +
"        certFile:    File containing signer's X509 certificate\n" +
"        keyFile:     File containing signer's private key\n" +
"        validFrom:   Date cert to be valid from, in dd/mm/yyyy format\n" +
"        validUntil:  Date cert to be valid until, in dd/mm/yyyy format\n" +
"        file:        Where to store the results\n\n" +

"    Optional arguments:\n\n" +
"        leaf:        Whether the certificate is a leaf (true, default)\n" +
"                     or not (false)\n");
    }
    
    public String[] getRequiredArgs()
    {
        return new String[] { "csrFile:", "certFile:", "keyFile:", 
            "validFrom:", "validUntil:", "file:" };
    }
    
    public String[] getOptionalArgs()
    {
      return new String[] { "leaf:"};
    }
        
    private X509CertificateObject readIssuerCert() throws Exception
    {
        String fn = getArg("certFile:");
        System.out.println("Reading issuer cert from " + fn + ".");
        ByteArrayInputStream bis = 
            new ByteArrayInputStream(readBytesFromFile(fn));
        return readX509(bis);
    }
         
    private PrivateKey readIssuerKey() throws Exception
    {
        String fn = getArg("keyFile:");
        System.out.println("Reading issuer key from " + fn + ".");
        byte[] encoded = readBytesFromFile(fn);
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(encoded);
        KeyFactory fact = KeyFactory.getInstance("RSA", "BC");
        PrivateKey key = fact.generatePrivate(spec);
        return key;
    }
    
    public void run() throws Exception
    {
        String token = getArg("leaf:");
        boolean isLeaf = token == null || token.equals("true"); 
        System.out.println(
          "Signing " + (isLeaf ? "leaf " : "") + "certificate request.");
        
        // Do a bit of argument checking before time-consuming operations...
        getDateArg("validFrom:");
        getDateArg("validUntil:");
        
        X509CertificateObject issuerCert = readIssuerCert();
        PrivateKey issuerKey = readIssuerKey();
        
        // Read request file        
        PKCS10CertificationRequest csr = new PKCS10CertificationRequest(
            readBytesFromFile(getArg("csrFile:")));        
        
        // Is there a better way to get these from csr?
        String subject = 
            csr.getCertificationRequestInfo().getSubject().toString();
        String c = null, cn = null, o = null, e = null;
        StringTokenizer st = new StringTokenizer(subject, ",");
        while (st.hasMoreTokens())
        {
            token = st.nextToken();
            if (token.startsWith("C="))
            {
                c = token.substring(2);
            }
            else if (token.startsWith("CN="))
            {
                cn = token.substring(3);
            }
            else if (token.startsWith("O="))
            {
                o = token.substring(2);
            }
            else if (token.startsWith("E="))
            {
                e = token.substring(2);
            }
        }
        
        // Generate leaf certificate
        X509V3CertificateGenerator cg = new X509V3CertificateGenerator();
        cg.reset();
        cg.setIssuerDN((X509Name)issuerCert.getSubjectDN());
        Hashtable ht = new Hashtable();                
        ht.put(X509Name.C, c);
        ht.put(X509Name.CN, cn);
        ht.put(X509Name.O, o);
        cg.setSubjectDN(new X509Name(ht));
        cg.setNotBefore(getDateArg("validFrom:"));
        cg.setNotAfter(getDateArg("validUntil:"));
        cg.setPublicKey(csr.getPublicKey());
        cg.setSerialNumber(BigInteger.valueOf(1));
        cg.setSignatureAlgorithm("MD5WITHRSA"); // SHA1WITHRSA is OK, too
        
        cg.addExtension(X509Extensions.KeyUsage.getId(), true,
        new X509KeyUsage(
        isLeaf ? X509KeyUsage.digitalSignature : X509KeyUsage.keyCertSign));
        
        GeneralName gn = new GeneralName(new DERIA5String(e), 1);
        DERConstructedSequence seq = new DERConstructedSequence();
        seq.addObject(gn);
        cg.addExtension(X509Extensions.SubjectAlternativeName.getId(), false,
            new GeneralNames(seq));
        
        Collection issuerNames = issuerCert.getSubjectAlternativeNames();
        if (issuerNames != null) {
            Iterator iter = issuerNames.iterator();
            while (iter.hasNext()) {
                List generalNames = (List) iter.next();
                //find the first rfc822Name (email address)
                if (((Integer) generalNames.get(0)).intValue() == 1) {
                    Object name = generalNames.get(1);
                    DERIA5String derString;
                    if (name instanceof String) {
                        derString = new DERIA5String((String) name);
                    } else {
                        derString = new DERIA5String((byte[]) name);
                    }
                    seq = new DERConstructedSequence();
                    seq.addObject(new GeneralName(derString, 1));
                    cg.addExtension(
                        X509Extensions.IssuerAlternativeName.getId(),
                        false, new GeneralNames(seq));
                    break; //while (iter.hasNext())
                }
            }
        }
        
        X509Certificate cert = cg.generateX509Certificate(issuerKey, "BC");

        // Now, write leaf certificate
        String fn = getArg("file:") + "_public.x509.crt";
        System.out.println("Writing cert to " + fn + ".");
        FileOutputStream str = new FileOutputStream(fn);
        str.write(cert.getEncoded());
        str.close();

        System.out.println();
        System.out.println("Done!");
        System.out.println();
    }
}