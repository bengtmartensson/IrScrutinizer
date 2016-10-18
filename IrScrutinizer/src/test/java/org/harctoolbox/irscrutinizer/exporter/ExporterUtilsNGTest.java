package org.harctoolbox.irscrutinizer.exporter;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.fail;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 *
 */
public class ExporterUtilsNGTest {


    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    public ExporterUtilsNGTest() {
    }

    @BeforeMethod
    public void setUpMethod() throws Exception {
    }

    @AfterMethod
    public void tearDownMethod() throws Exception {
    }

    /**
     * Test of twoDigitHex method, of class ExporterUtils.
     */
    @Test
    public void testTwoDigitHex() {
        System.out.println("twoDigitHex");
        assertEquals(ExporterUtils.twoDigitHex(0), "00");
        assertEquals(ExporterUtils.twoDigitHex(16), "10");
        assertEquals(ExporterUtils.twoDigitHex(130), "82");
        assertEquals(ExporterUtils.twoDigitHex(255), "FF");
    }

    /**
     * Test of twoDigitReverseHex method, of class ExporterUtils.
     */
    @Test
    public void testTwoDigitReverseHex() {
        System.out.println("twoDigitReverseHex");
        assertEquals(ExporterUtils.twoDigitReverseHex(0), "00");
        assertEquals(ExporterUtils.twoDigitReverseHex(16), "08");
        assertEquals(ExporterUtils.twoDigitReverseHex(130), "41");
        assertEquals(ExporterUtils.twoDigitReverseHex(255), "FF");
    }

    /**
     * Test of rc5Data method, of class ExporterUtils.
     */
    @Test
    public void testRc5Data() {
        System.out.println("rc5Data");
        int D = 0;
        int F = 64;
        int T = 0;
        String expResult =                            "0000000000000000";
        String result = ExporterUtils.rc5Data(D, F, T);
        assertEquals(result, expResult);
        assertEquals(ExporterUtils.rc5Data(0, 0, 0),  "0000000000001000");
        assertEquals(ExporterUtils.rc5Data(0, 0, 1),  "0000000000001800");
        assertEquals(ExporterUtils.rc5Data(0, 1, 0),  "0000000000001001");
        assertEquals(ExporterUtils.rc5Data(16, 0, 0), "0000000000001400");
    }

    /**
     * Test of reverse method, of class ExporterUtils.
     */
    @Test
    public void testReverse() {
        System.out.println("reverse");
        assertEquals(ExporterUtils.reverse(1, 1), 1);
        assertEquals(ExporterUtils.reverse(1, 2), 2);
        assertEquals(ExporterUtils.reverse(1, 8), 128);
        assertEquals(ExporterUtils.reverse(1, 16), 0x8000);
    }

    /**
     * Test of sony12Data method, of class ExporterUtils.
     */
    @Test
    public void testSony12Data() {
        System.out.println("sony12Data");
        assertEquals(ExporterUtils.sony12Data(3, 4), String.format(ExporterUtils.longFormattingCode, (16 << 5) + 24));
    }

    /**
     * Test of sony15Data method, of class ExporterUtils.
     */
    @Test
    public void testSony15Data() {
        System.out.println("sony15Data");
        assertEquals(ExporterUtils.sony15Data(3, 4), String.format(ExporterUtils.longFormattingCode, (16 << 8) + 0xC0));
    }

    /**
     * Test of sony20Data method, of class ExporterUtils.
     */
    @Test
    public void testSony20Data() {
        System.out.println("sony20Data");
        assertEquals(ExporterUtils.sony20Data(3, 5, 4), String.format(ExporterUtils.longFormattingCode, (0x10 << 13) + (24 << 8) + 0xA0));
    }

    /**
     * Test of sixteenDigitHex method, of class ExporterUtils.
     */
    @Test
    public void testSixteenDigitHex() {
        System.out.println("sixteenDigitHex");
        long n = 0xDEADBEEFDEADBEEFL;
        String expResult = "DEADBEEFDEADBEEF";
        String result = ExporterUtils.sixteenDigitHex(n);
        assertEquals(result, expResult);
    }

    /**
     * Test of processBitFields method, of class ExporterUtils.
     */
    @Test
    public void testProcessBitFields() {
        System.out.println("processBitFields");
        String expResult = "000000000000DEAD";
        String result = ExporterUtils.processBitFields(false, false, 0xDE, 8, 0, false, false, 0xADFF, 8, 8);
        assertEquals(result, expResult);

    }

    /**
     * Test of processFiniteBitField method, of class ExporterUtils.
     */
    @Test
    public void testProcessFiniteBitFieldLong() {
        System.out.println("processFiniteBitFieldLong");
        int D = 244;
        boolean complement = false;
        boolean reverse = false;
        int data = 244;
        int length = 6;
        int chop = 0;
        long expResult = 52L;
        long result = ExporterUtils.processFiniteBitFieldLong(complement, reverse, data, length, chop);
        assertEquals(result, expResult);
        result = ExporterUtils.processFiniteBitFieldLong(complement, reverse, data, length, 2);
        assertEquals(result, 61L);
    }

    /**
     * Test of reverse method, of class ExporterUtils.
     */
    @Test
    public void testReverse_int_int() {
        System.out.println("reverse");
        int n = 73;
        int bits = 7;
        int expResult = 73;
        int result = ExporterUtils.reverse(n, bits);
        assertEquals(result, expResult);
    }

    /**
     * Test of reverse method, of class ExporterUtils.
     */
    @Test
    public void testReverse_long_int() {
        System.out.println("reverse");
        long n = 0xDEADBEEFEDL;
        long bits = 40L;
        long expResult = 790131225979L;
        long result = ExporterUtils.reverse(n, bits);
        assertEquals(result, expResult);
    }
}
