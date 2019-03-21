
import com.hdcookbook.grin.util.BitStreamIO;
import com.hdcookbook.grin.util.Debug;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.PrintStream;

/**
 * A quick test to exercise com.hdcookbook.grin.util.BitStreamIO.
 * Runs on desktop JDK.
 **/

public class Test  {

    boolean isFirst;
    boolean isPretty;
    int randomFlags;
    int fiveBits;
    String someName;


    public Test(boolean isFirst, boolean isPretty, int randomFlags, 
                int fiveBits, String someName)
    {
        this.isFirst = isFirst;
        this.isPretty = isPretty;
        this.randomFlags = randomFlags;
        this.fiveBits = fiveBits;
        this.someName = someName;
    }

    public Test() {
    }

    public String toString() {
        return "Test( isFirst=" + isFirst + ", isPretty=" + isPretty
                + ", randomFlags=0x" + toHex(randomFlags, 1) 
                + ", fiveBits=" + fiveBits
                + ", someName=\"" + someName + "\" )";
    }

    public boolean equals(Object other) {
        if (!(other instanceof Test)) {
            return false;
        }
        Test ot = (Test) other;
        return isFirst == ot.isFirst && isPretty == ot.isPretty
                && randomFlags == ot.randomFlags 
                && fiveBits == ot.fiveBits
                && someName.equals(ot.someName);
    }

    public void read(DataInputStream dis) throws IOException {
        BitStreamIO bio = new BitStreamIO();
        isFirst = (bio.readBits(dis, 1)) != 0;
        isPretty = (bio.readBits(dis, 1)) != 0;
        randomFlags = bio.readBits(dis, 3);
        bio.readBits(dis, 8);
        fiveBits=bio.readBits(dis, 5);  // Straddles a byte
        bio.readBits(dis, 6);
        if (Debug.ASSERT) {
            bio.assertByteAligned(1);
        }
        byte[] buf = new byte[bio.readBits(dis, 8)];
        dis.read(buf);
        someName = new String(buf, "UTF-8");
    }

    public void write(DataOutputStream dos) throws IOException {
        BitStreamIO bio = new BitStreamIO();
        bio.writeBits(dos, 1, (isFirst ? 1 : 0));
        bio.writeBits(dos, 1, (isPretty ? 1 : 0));
        bio.writeBits(dos, 3, randomFlags);
        bio.writeBits(dos, 8, 0);
        bio.writeBits(dos, 5, fiveBits);
        bio.writeBits(dos, 6, 0);
        if (Debug.ASSERT) {
            bio.assertByteAligned(1);
        }
        byte[] buf = someName.getBytes("UTF-8");
        bio.writeBits(dos, 8, buf.length);
        dos.write(buf);
    }
    

    public void readLong(DataInputStream dis) throws IOException {
        BitStreamIO bio = new BitStreamIO();
        isFirst = (bio.readBitsLong(dis, 1)) != 0;
        isPretty = (bio.readBitsLong(dis, 1)) != 0;
        randomFlags = (int) bio.readBitsLong(dis, 3);
        bio.readBitsLong(dis, 8);
        fiveBits = (int) bio.readBitsLong(dis, 5);
        bio.readBitsLong(dis, 6);
        if (Debug.ASSERT) {
            bio.assertByteAligned(1);
        }
        byte[] buf = new byte[(int) bio.readBitsLong(dis, 8)];
        dis.read(buf);
        someName = new String(buf, "UTF-8");
    }

    public void writeLong(DataOutputStream dos) throws IOException {
        BitStreamIO bio = new BitStreamIO();
        bio.writeBitsLong(dos, 1, (isFirst ? 1 : 0));
        bio.writeBitsLong(dos, 1, (isPretty ? 1 : 0));
        bio.writeBitsLong(dos, 3, randomFlags);
        bio.writeBitsLong(dos, 8, 0);
        bio.writeBitsLong(dos, 5, fiveBits);
        bio.writeBitsLong(dos, 6, 0);
        if (Debug.ASSERT) {
            bio.assertByteAligned(1);
        }
        byte[] buf = someName.getBytes("UTF-8");
        bio.writeBitsLong(dos, 8, buf.length);
        dos.write(buf);
    }
    

    private static String hexDigits = "0123456789abcdef";

    public static void dump(InputStream in, PrintStream out,
                            String indent) 
                throws IOException
    {
        int ch = 0;
        int count = 0;
        String line = "";
        for(;;) {
            if (ch != -1) {
                ch = in.read();
            }
            int m = count % 16;
            if (m == 0) {
                if (ch == -1) {
                    break;
                }
                System.out.print(indent + toHex(count, 8) + ":  ");
            }
            if (m == 8) {
                System.out.print(" ");
            }
            if (ch == -1) {
                System.out.print("  ");
            } else {
                System.out.print(toHex(ch, 2));
                if (ch >= 32 && ch < 127) {
                    line += ((char) ch);
                } else {
                    line += ".";
                }
            }
            if (m == 15)  {
                System.out.println("   " + line);
                line = "";
            } else {
                System.out.print(" ");
            }
            count++;
        }
    }

    private static String toHex(int b, int digits) {
        if (digits <= 0) {
            throw new IllegalArgumentException();
        }
        String result = "";
        while (digits > 0 || b > 0) {
            result = hexDigits.charAt(b % 16) + result;
            b = b / 16;
            digits--;
        }
        return result;
    }


    public void test() throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(bos);
        write(dos);
        dos.flush();
        byte[] buf = bos.toByteArray();
        dos.close();

        ByteArrayInputStream bis = new ByteArrayInputStream(buf);
        DataInputStream dis = new DataInputStream(bis);
        Test other = new Test();
        other.read(dis);
        dis.close();

        boolean pass = other.equals(this);
        if (pass) {
            System.out.println("PASSED:  " + this);
        } else {
            System.out.println("FAILED:  " + this);
            System.out.println(" other:  " + other);
        }
        bis = new ByteArrayInputStream(buf);
        dump(bis, System.out, "\t");
        System.out.println();

        if (pass) {
            testLong();
        } else {
            throw new IOException("Test failed on " + this);
        }
    }

    public void testLong() throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(bos);
        writeLong(dos);
        dos.flush();
        byte[] buf = bos.toByteArray();
        dos.close();

        ByteArrayInputStream bis = new ByteArrayInputStream(buf);
        DataInputStream dis = new DataInputStream(bis);
        Test other = new Test();
        other.readLong(dis);
        dis.close();

        boolean pass = other.equals(this);
        if (pass) {
        } else {
            System.out.println("FAILED LONG:  " + this);
            System.out.println("      other:  " + other);
            bis = new ByteArrayInputStream(buf);
            dump(bis, System.out, "\t");
            System.out.println();
            throw new IOException("Test failed on " + this);
        }
    }


    public static void main(String[] args) {
        try {
            (new Test(true, true, 0, 18, "hello 1")).test();
            (new Test(true, false, 7, 17, "hello 2")).test();
            (new Test(false, true, 3, 16, "hello 3")).test();
            (new Test(true, true, 1, 15, "hello 4")).test();
            (new Test(true, true, 2, 14, "hello 5")).test();
            (new Test(true, true, 3, 13, "hello 6")).test();
            (new Test(true, true, 4, 12, "hello 7")).test();
            (new Test(true, true, 5, 11, "hello 8")).test();
            (new Test(true, true, 6, 10, "hello 9")).test();
            (new Test(true, true, 7, 9, "hello 10")).test();
            (new Test(false, false, 0, 8, "hello 11")).test();
            (new Test(false, false, 1, 7, "hello 12")).test();
            (new Test(false, false, 2, 6, "hello 13")).test();
            (new Test(false, false, 3, 5, "hello 14")).test();
            (new Test(false, false, 4, 4, "hello 15")).test();
            (new Test(false, false, 5, 3, "hello 16")).test();
            (new Test(false, false, 6, 2, "hello 17")).test();
            (new Test(false, false, 7, 1, "hello 18")).test();
        } catch (Exception ex) {
            ex.printStackTrace();
            System.exit(1);
        }
        System.exit(0);
    }
}
