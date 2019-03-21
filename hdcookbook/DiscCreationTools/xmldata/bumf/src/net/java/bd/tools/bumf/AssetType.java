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

//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, vJAXB 2.1.3 in JDK 1.6 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2008.05.19 at 09:45:15 AM PDT 
//


package net.java.bd.tools.bumf;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for assetType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="assetType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="BUDAFile" type="{urn:BDA:bdmv;manifest}FileType"/>
 *       &lt;/sequence>
 *       &lt;attribute name="VPFilename" use="required" type="{urn:BDA:bdmv;manifest}FilenameType" />
 *       &lt;attribute name="digestAlgorithm" type="{urn:BDA:bdmv;manifest}digestAlgorithmType" default="SHA-1" />
 *       &lt;attribute name="digestValue" type="{http://www.w3.org/2001/XMLSchema}base64Binary" />
 *       &lt;attribute name="credentialID" type="{urn:BDA:bdmv;manifest}credentialIDType" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "assetType", propOrder = {
    "budaFile"
})
public class AssetType {

    @XmlElement(name = "BUDAFile", required = true)
    protected FileType budaFile;
    @XmlAttribute(name = "VPFilename", required = true)
    protected String vpFilename;
    @XmlAttribute
    protected DigestAlgorithmType digestAlgorithm;
    @XmlAttribute
    protected byte[] digestValue;
    @XmlAttribute
    protected String credentialID;

    /**
     * Gets the value of the budaFile property.
     * 
     * @return
     *     possible object is
     *     {@link FileType }
     *     
     */
    public FileType getBUDAFile() {
        return budaFile;
    }

    /**
     * Sets the value of the budaFile property.
     * 
     * @param value
     *     allowed object is
     *     {@link FileType }
     *     
     */
    public void setBUDAFile(FileType value) {
        this.budaFile = value;
    }

    /**
     * Gets the value of the vpFilename property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getVPFilename() {
        return vpFilename;
    }

    /**
     * Sets the value of the vpFilename property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setVPFilename(String value) {
        this.vpFilename = value;
    }

    /**
     * Gets the value of the digestAlgorithm property.
     * 
     * @return
     *     possible object is
     *     {@link DigestAlgorithmType }
     *     
     */
    public DigestAlgorithmType getDigestAlgorithm() {
        if (digestAlgorithm == null) {
            return DigestAlgorithmType.SHA_1;
        } else {
            return digestAlgorithm;
        }
    }

    /**
     * Sets the value of the digestAlgorithm property.
     * 
     * @param value
     *     allowed object is
     *     {@link DigestAlgorithmType }
     *     
     */
    public void setDigestAlgorithm(DigestAlgorithmType value) {
        this.digestAlgorithm = value;
    }

    /**
     * Gets the value of the digestValue property.
     * 
     * @return
     *     possible object is
     *     byte[]
     */
    public byte[] getDigestValue() {
        return digestValue;
    }

    /**
     * Sets the value of the digestValue property.
     * 
     * @param value
     *     allowed object is
     *     byte[]
     */
    public void setDigestValue(byte[] value) {
        this.digestValue = ((byte[]) value);
    }

    /**
     * Gets the value of the credentialID property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getCredentialID() {
        return credentialID;
    }

    /**
     * Sets the value of the credentialID property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setCredentialID(String value) {
        this.credentialID = value;
    }

}
