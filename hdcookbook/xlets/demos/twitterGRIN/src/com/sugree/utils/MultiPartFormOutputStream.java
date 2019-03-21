package com.sugree.utils;

import java.io.OutputStream;
import java.io.DataOutputStream;
 
/**
 * <code>MultiPartFormOutputStream</code> is used to write 
 * "multipart/form-data" to a <code>java.net.URLConnection</code> for 
 * POSTing.  This is primarily for file uploading to HTTP servers.  
 * 
 * @since  JDK1.3
 */
public class MultiPartFormOutputStream {
        /**
         * The line end characters.  
         */
        private static final String NEWLINE = "\r\n";
 
        /**
         * The boundary prefix.  
         */
        private static final String PREFIX = "--";
 
        /**
         * The output stream to write to.  
         */
        private DataOutputStream out = null;
 
        /**
         * The multipart boundary string.  
         */
        private String boundary = null;
 
        /**
         * Creates a new <code>MultiPartFormOutputStream</code> object using 
         * the specified output stream and boundary.  The boundary is required 
         * to be created before using this method, as described in the 
         * description for the <code>getContentType(String)</code> method.  
         * The boundary is only checked for <code>null</code> or empty string, 
         * but it is recommended to be at least 6 characters.  (Or use the 
         * static createBoundary() method to create one.)
         * 
         * @param  os        the output stream
         * @param  boundary  the boundary
         * @see  #createBoundary()
         * @see  #getContentType(String)
         */
        public MultiPartFormOutputStream(OutputStream os, String boundary) {
                if(os == null) {
                        throw new IllegalArgumentException("Output stream is required.");
                }
                if(boundary == null || boundary.length() == 0) {
                        throw new IllegalArgumentException("Boundary stream is required.");
                }
                this.out = new DataOutputStream(os);
                this.boundary = boundary;
        }
 
        /**
         * Writes an boolean field value.  
         * 
         * @param  name   the field name (required)
         * @param  value  the field value
         * @throws  java.io.IOException  on input/output errors
         */
        public void writeField(String name, boolean value) 
                        throws java.io.IOException {
                writeField(name, new Boolean(value).toString());
        }
 
        /**
         * Writes an double field value.  
         * 
         * @param  name   the field name (required)
         * @param  value  the field value
         * @throws  java.io.IOException  on input/output errors
         */
//#ifdef polish.cldc1.1
        public void writeField(String name, double value) 
                        throws java.io.IOException {
                writeField(name, Double.toString(value));
        }
//#endif
 
        /**
         * Writes an float field value.  
         * 
         * @param  name   the field name (required)
         * @param  value  the field value
         * @throws  java.io.IOException  on input/output errors
         */
//#ifdef polish.cldc1.1
        public void writeField(String name, float value) 
                        throws java.io.IOException {
                writeField(name, Float.toString(value));
        }
//#endif
 
        /**
         * Writes an long field value.  
         * 
         * @param  name   the field name (required)
         * @param  value  the field value
         * @throws  java.io.IOException  on input/output errors
         */
        public void writeField(String name, long value) 
                        throws java.io.IOException {
                writeField(name, Long.toString(value));
        }
 
        /**
         * Writes an int field value.  
         * 
         * @param  name   the field name (required)
         * @param  value  the field value
         * @throws  java.io.IOException  on input/output errors
         */
        public void writeField(String name, int value) 
                        throws java.io.IOException {
                writeField(name, Integer.toString(value));
        }
 
        /**
         * Writes an char field value.  
         * 
         * @param  name   the field name (required)
         * @param  value  the field value
         * @throws  java.io.IOException  on input/output errors
         */
        public void writeField(String name, char value) 
                        throws java.io.IOException {
                writeField(name, new Character(value).toString());
        }
 
        /**
         * Writes an string field value.  If the value is null, an empty string 
         * is sent ("").  
         * 
         * @param  name   the field name (required)
         * @param  value  the field value
         * @throws  java.io.IOException  on input/output errors
         */
        public void writeField(String name, String value) 
                        throws java.io.IOException {
                if(name == null) {
                        throw new IllegalArgumentException("Name cannot be null or empty.");
                }
                if(value == null) {
                        value = "";
                }
                /*
                --boundary\r\n
                Content-Disposition: form-data; name="<fieldName>"\r\n
                \r\n
                <value>\r\n
                */
                // write boundary
                writeString(PREFIX);
                writeString(boundary);
                writeString(NEWLINE);
                // write content header
                writeString("Content-Disposition: form-data; name=\"" + name + "\"");
                writeString(NEWLINE);
                writeString("Content-Length: " + value.length());
                writeString(NEWLINE);
                writeString(NEWLINE);
                // write content
                writeString(value);
                writeString(NEWLINE);
                out.flush();
        }
 
        /**
         * Writes the given bytes.  The bytes are assumed to be the contents 
         * of a file, and will be sent as such.  If the data is null, a 
         * <code>java.lang.IllegalArgumentException</code> will be thrown.  
         * 
         * @param  name      the field name
         * @param  mimeType  the file content type (optional, recommended)
         * @param  fileName  the file name (required)
         * @param  data      the file data
         * @throws  java.io.IOException  on input/output errors
         */
        public void writeFile(String name, String mimeType, 
                        String fileName, byte[] data) 
                        throws java.io.IOException {
                if(data == null) {
                        throw new IllegalArgumentException("Data cannot be null.");
                }
                if(fileName == null || fileName.length() == 0) {
                        throw new IllegalArgumentException("File name cannot be null or empty.");
                }
                /*
                --boundary\r\n
                Content-Disposition: form-data; name="<fieldName>"; filename="<filename>"\r\n
                Content-Type: <mime-type>\r\n
                \r\n
                <file-data>\r\n
                */
                // write boundary
                writeString(PREFIX);
                writeString(boundary);
                writeString(NEWLINE);
                // write content header
                writeString("Content-Disposition: form-data; name=\"" + name + 
                        "\"; filename=\"" + fileName + "\"");
                writeString(NEWLINE);
                if(mimeType != null) {
                        writeString("Content-Type: " + mimeType);
                        writeString(NEWLINE);
                }
                writeString("Content-Length: " + data.length);
                writeString(NEWLINE);
                writeString(NEWLINE);
                // write content
                out.write(data, 0, data.length);
                writeString(NEWLINE);
                out.flush();
        }

        public void writeString(String s) throws java.io.IOException {
                byte[] b = s.getBytes();
                out.write(b, 0, b.length);
        }
 
        /**
         * Flushes the stream.  Actually, this method does nothing, as the only 
         * write methods are highly specialized and automatically flush.  
         * 
         * @throws  java.io.IOException  on input/output errors
         */
        public void flush() throws java.io.IOException {
                // out.flush();
        }
 
        /**
         * Closes the stream.  <br />
         * <br />
         * <b>NOTE:</b> This method <b>MUST</b> be called to finalize the 
         * multipart stream.
         * 
         * @throws  java.io.IOException  on input/output errors
         */
        public void close() throws java.io.IOException {
                // write final boundary
                writeString(PREFIX);
                writeString(boundary);
                writeString(PREFIX);
                writeString(NEWLINE);
                out.flush();
                out.close();
        }
 
        /**
         * Gets the multipart boundary string being used by this stream.  
         * 
         * @return  the boundary
         */
        public String getBoundary() {
                return this.boundary;
        }
 
        /**
         * Creates a multipart boundary string by concatenating 20 hyphens (-) 
         * and the hexadecimal (base-16) representation of the current time in 
         * milliseconds.  
         * 
         * @return  a multipart boundary string
         * @see  #getContentType(String)
         */
        public static String createBoundary() {
                return "--------------------" + 
                        Long.toString(System.currentTimeMillis(), 16);
        }
 
        /**
         * Gets the content type string suitable for the 
         * <code>java.net.URLConnection</code> which includes the multipart 
         * boundary string.  <br />
         * <br />
         * This method is static because, due to the nature of the 
         * <code>java.net.URLConnection</code> class, once the output stream 
         * for the connection is acquired, it's too late to set the content 
         * type (or any other request parameter).  So one has to create a 
         * multipart boundary string first before using this class, such as 
         * with the <code>createBoundary()</code> method.  
         * 
         * @param  boundary  the boundary string
         * @return  the content type string
         * @see  #createBoundary()
         */
        public static String getContentType(String boundary) {
                return "multipart/form-data; charset=UTF-8; boundary=" + boundary;
        }
}
