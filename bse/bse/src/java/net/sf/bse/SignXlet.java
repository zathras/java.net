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

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Map;
import java.util.StringTokenizer;

import org.bouncycastle.asn1.DERBitString;
import org.bouncycastle.asn1.DERConstructedSequence;
import org.bouncycastle.asn1.DERInteger;
import org.bouncycastle.asn1.DERObjectIdentifier;
import org.bouncycastle.asn1.DEROutputStream;
import org.bouncycastle.asn1.DERTaggedObject;
import org.bouncycastle.asn1.x509.AuthorityKeyIdentifier;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.X509Name;
import org.bouncycastle.jce.provider.X509CertificateObject;

/**
 * Command to sign an Xlet.
 *
 * @author Bill Foote (bill.foote@sun.com)
 * @author Aleksi Peebles (aleksi.peebles@infocast.fi)
 * @version $Revision: 1.3 $ $Date: 2004/05/06 09:55:18 $
 */
public class SignXlet extends Command
{
    public SignXlet(Map args)
    {
        super(args);
    }
    
    public void usageMessage(PrintStream out)
    {
        out.println(
"Command:  xlet\n\n" +

"    Signs an MHP Xlet\n\n" +

"    Arguments:\n\n" +

"        certs:  Full names of all certificate files in the certificate\n" +
"                chain, separated by the OS path separator.\n" +
"                The file names must be in the correct ascending order:\n" +
"                signing certificate first and root certificate last.\n" +
"        key:    Full name of file containing signing private key\n" +
"        src:    Base directory to copy Xlet files from\n" +
"        dest:   Destination. If this is equal to src, the files will be\n" +
"                added/modified in this directory. Otherwise,\n" +
"                a directory with this name will be created and\n" +
"                if the directory already exists and the optional rm\n" +
"                argument is not set to \"true\" the command will fail.\n\n" +

"    Plus, optionally:\n\n" +

"        files:  Full names of all files to be signed, separated by the\n" +
"                OS path separator. All other files will not be signed.\n" +
"                If this argument is left out all files will be signed.\n" +
"        rm:     If set to \"true\", the dest directory will be deleted\n" +
"                in the case that it already exists. If set to \"false\"\n" +
"                (or anything else) or left out, the command will fail if\n" +
"                the dest directory already exists.\n");

    }
    
    public String[] getRequiredArgs()
    {
        return new String[] { "certs:", "key:", "src:", "dest:" };
    }
    
    public String[] getOptionalArgs()
    {
        return new String[] { "files:", "rm:" };
    }    
    
    private X509CertificateObject readCert(String file) throws Exception
    {
        System.out.println("Reading certificate from " + file);
        ByteArrayInputStream bis = 
            new ByteArrayInputStream(readBytesFromFile(file));
        return readX509(bis);
    }
    
    private PrivateKey readPrivateKey(String file) throws Exception
    {
        System.out.println("Reading private key from " + file);
        byte[] encoded = readBytesFromFile(file);
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(encoded);
        KeyFactory fact = KeyFactory.getInstance("RSA", "BC");
        PrivateKey key = fact.generatePrivate(spec);
        return key;
    }
        
    private void rmMinusR(File f)
    {
        if (f.isFile())
        {
            f.delete();
        } 
        else if (f.isDirectory())
        {
            String[] contents = f.list();
            for (int i = 0; i < contents.length; i++)
            {
                rmMinusR(new File(f, contents[i]));
            }
            f.delete();
        }
    }

    /**
     * Copies the contents of the <code>src</code> directory to the 
     * <code>dest</code> directory and creates a hashfile in all copied 
     * directories. A separate digest is calculated for every file listed in 
     * <code>signFiles</code> (see MHP 1.0.2 spec 12.4.1.4).
     */
    private byte[] multiDigestCopyAndHash(String path, File src, File dest, 
        Hashtable signFiles) throws Exception
    {
        if (!src.isDirectory())
        {
            throw new IOException(src + " isn't a readable directory");
        }
        if (dest != null && !dest.mkdirs())
        {
            throw new IOException("Couldn't create directory " + dest);
        }
        
        // Start constructing hashfile. See MHP 1.0.2 spec 12.4.1.1.
        
        ByteArrayOutputStream hashFile = new ByteArrayOutputStream();
        
        String[] contents = src.list();
        
        // digest_count, 16 bit uimsbf
        // Is the same as file count.
        hashFile.write(contents.length >> 8);
        hashFile.write(contents.length & 0xff);
        
        for (int i = 0; i < contents.length; i++)
        {                        
            File srcF = new File(src, contents[i]);
            File destF = dest == null ? null : new File(dest, contents[i]);
            
            System.out.println("Calculating digest for file " + srcF);
            
            int digestType = 0; // 0 = Non authenticated
            if (signFiles.get(buildPath(path, contents[i])) != null ||
                signFiles.size() == 0 || srcF.isDirectory())
            {
                digestType = 1; // 1 = MD5, 8 bit uimsbf
            }
            hashFile.write(digestType);

            // name_count, 16 bit uimsbf
            // just one file in digest
            hashFile.write(0);
            hashFile.write(1);
            
            if (contents[i].length() > 0xff)
            {
                throw new IOException("Filename too long:  " + contents[i]);
            }
            // name_length
            hashFile.write(contents[i].length());
            
            for (int j = 0; j < contents[i].length(); j++)
            {
                // name_byte
                hashFile.write(contents[i].charAt(j));
            }            
            
            // Copy file to destination directory and calculate digest
            
            MessageDigest md5 = MessageDigest.getInstance("MD5", "BC");
            md5.reset();

            if (srcF.isDirectory())
            {
                byte[] ba = multiDigestCopyAndHash(
                    buildPath(path, contents[i]), srcF, destF, signFiles);
                md5.update(ba);
                hashFile.write(md5.digest());
            } 
            else if (srcF.isFile())
            {
                byte[] ba = readBytesFromFile(srcF.getAbsolutePath());
                if (dest != null) {
                    FileOutputStream fos = new FileOutputStream(destF);
                    fos.write(ba);
                    fos.close();
                }
                if (digestType > 0)
                {
                    md5.update(ba);
                    hashFile.write(md5.digest());
                }
            } 
            else
            {
                System.err.println("Ignoring " + srcF);
            }
        }
                                                
        hashFile.close();
                
        byte[] hashContents = hashFile.toByteArray();
        
        File hashF = new File(dest == null ? src : dest, "dvb.hashfile");
        FileOutputStream fos = new FileOutputStream(hashF);
        fos.write(hashContents);
        fos.close();
        return hashContents;
    }    

    private String buildPath(String path, String file) {
      if (path == null || path.length() == 0) {
        return file;
      }
      return path + File.separator + file;
    }

    private void writeCertificate(OutputStream os, X509CertificateObject cert)
        throws Exception
    {
        byte[] encoded = cert.getEncoded();
        
        // Length, 24 bit uimsbf
        os.write(encoded.length >> 16);
        os.write((encoded.length >> 8) & 0xff);
        os.write(encoded.length & 0xff);
        
        os.write(encoded);
    }
    
    public void run() throws Exception
    {
        Hashtable signFiles = new Hashtable();
        if (getArg("files:") != null)
        {
            StringTokenizer st = new StringTokenizer(getArg("files:"), 
                File.pathSeparator);
            while (st.hasMoreTokens())
            {
                // The digest algorithm value isn't used at the moment, 
                // but maybe in future versions...
                signFiles.put(st.nextToken(), "MD5");
            }
        }
        
        System.out.println("Signing Xlet " + getArg("src:"));
        
        File src = new File(getArg("src:"));
        File dest = new File(getArg("dest:"));
        if (dest.getAbsolutePath().equals(src.getAbsolutePath())) {
          dest = null;
        }
        
        if (dest != null && dest.exists())
        {
            if (getArg("rm:") != null && 
                getArg("rm:").equalsIgnoreCase("true"))
            {
                System.out.println("Removing " + dest);
                rmMinusR(dest);
            }
            else
            {
                System.out.println(
                    "Command failed: destination directory already exists.\n");
                return;
            }
        }
        
        // Certificate file. See MHP 1.0.2 spec 12.4.3.
        
        System.out.println("Generating the certificate file");                

        ArrayList certFiles = new ArrayList();
        StringTokenizer st = 
            new StringTokenizer(getArg("certs:"), File.pathSeparator);
        while (st.hasMoreTokens())
        {
            certFiles.add(st.nextToken());
        }
        
        File certFile = new File(src, "dvb.certificates.1");
        OutputStream os = 
            new BufferedOutputStream(new FileOutputStream(certFile));

        // certificate_count, 16 bit uimsbf
        os.write(certFiles.size() >> 8);
        os.write(certFiles.size() & 0xff);

        X509CertificateObject leafCert = readCert((String)certFiles.get(0));
        writeCertificate(os, leafCert);
        for (int i = 1; i < certFiles.size(); i++)
        {
            writeCertificate(os, readCert((String)certFiles.get(i)));
        }

        os.close();
        
        System.out.println("Computing hashes and copying tree");
        
        byte[] hashFile = multiDigestCopyAndHash(null, src, dest, signFiles);
        
        System.out.println("Generating signature file");

        // Begin constructing signature sequence.
        // See MHP 1.0.2 spec 12.4.2.

        PrivateKey key = readPrivateKey(getArg("key:"));
        Signature engine = Signature.getInstance("MD5WITHRSA", "BC");        
        engine.initSign(key);
        engine.update(hashFile);
        byte[] signature = engine.sign();
        DERConstructedSequence sigSeq = new DERConstructedSequence();

        // Begin constructing certificateIdentifier
        // (AuthorityKeyIdentifier) sequence

        DERConstructedSequence ci = new DERConstructedSequence();

        // keyIdentifier left out from sequence, not required in MHP

        // authorityCertIssuer
        X509Name nm = (X509Name)leafCert.getIssuerDN();
        GeneralName gn = new GeneralName(nm);
        ci.addObject(new DERTaggedObject(1, gn));

        // authorityCertSerialNumber
        DERInteger ser = new DERInteger(leafCert.getSerialNumber());
        ci.addObject(new DERTaggedObject(2, ser));

        // cerfiticateIdentifier sequence ready, add to signature sequence
        AuthorityKeyIdentifier aki = new AuthorityKeyIdentifier(ci);
        sigSeq.addObject(aki);

        // Add hashSignatureAlgorithm to signature sequence
        sigSeq.addObject(new DERObjectIdentifier("1.2.840.113549.2.5"));

        // Add signatureValue to signature sequence
        sigSeq.addObject(new DERBitString(signature));

        // Signature sequence ready, write to file
        File f = new File(dest == null ? src : dest, "dvb.signaturefile.1");
        os = new BufferedOutputStream(new FileOutputStream(f));
        DEROutputStream dos = new DEROutputStream(os);
        dos.writeObject(sigSeq);
        dos.close();
        
        // Delete the generated certificate file from the source directory
        if (dest != null && !certFile.delete()) {
          System.err.println(
            "Could not delete certificate file in source directory: "
              + certFile.getName());
        } 
        
        System.out.println();
        System.out.println("Done!");
        System.out.println();
    }
}