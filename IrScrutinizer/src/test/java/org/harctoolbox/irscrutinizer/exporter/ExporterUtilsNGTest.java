package org.harctoolbox.irscrutinizer.exporter;

import static org.testng.Assert.assertEquals;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 *
 */
public class ExporterUtilsNGTest {

    public ExporterUtilsNGTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
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
        String expResult = "0";
        String result = ExporterUtils.rc5Data(D, F, T);
        assertEquals(result, expResult);
        assertEquals(ExporterUtils.rc5Data(0, 0, 0),  "1000");
        assertEquals(ExporterUtils.rc5Data(0, 0, 1),  "1800");
        assertEquals(ExporterUtils.rc5Data(0, 1, 0),  "1001");
        assertEquals(ExporterUtils.rc5Data(16, 0, 0), "1400");
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
        assertEquals(ExporterUtils.sony12Data(3, 4), Integer.toHexString((16 << 5) + 24));
    }

    /**
     * Test of sony15Data method, of class ExporterUtils.
     */
    @Test
    public void testSony15Data() {
        System.out.println("sony15Data");
        assertEquals(ExporterUtils.sony15Data(3, 4), Integer.toHexString((16 << 8) + 0xC0));
    }

    /**
     * Test of sony20Data method, of class ExporterUtils.
     */
    @Test
    public void testSony20Data() {
        System.out.println("sony20Data");
        int D = 0;
        int S = 0;
        int F = 0;
        String expResult = "";
        String result = ExporterUtils.sony20Data(D, S, F);
        assertEquals(ExporterUtils.sony20Data(3, 5, 4), Integer.toHexString((0x10 << 13) + (24 << 8) + 0xA0));
    }
}
