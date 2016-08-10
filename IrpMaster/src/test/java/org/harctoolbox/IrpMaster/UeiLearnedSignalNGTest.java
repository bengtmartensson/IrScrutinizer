package org.harctoolbox.IrpMaster;

import com.hifiremote.exchangeir.UeiLearned;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.fail;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class UeiLearnedSignalNGTest {

    public UeiLearnedSignalNGTest() {
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

    private static final String necCcf = "0000 006C 0022 0002 015C 00AD 0016 0016 0016 0016 0016 0041 0016 0041 0016 0016 0016 0016 0016 0016 0016 0016 0016 0016 0016 0041 0016 0016 0016 0016 0016 0016 0016 0041 0016 0016 0016 0016 0016 0016 0016 0016 0016 0016 0016 0041 0016 0041 0016 0041 0016 0016 0016 0016 0016 0041 0016 0041 0016 0041 0016 0016 0016 0016 0016 0016 0016 0041 0016 0041 0016 06A8 015C 0057 0016 0E74";
    private static final String necUei = "00 00 2F 00 D0 06 11 A8 08 CD 01 1E 01 1E 01 1E 03 4F 01 1E 56 82 11 A8 04 6D 01 1E BB E0 22 01 12 21 11 11 21 11 21 11 11 22 21 12 22 11 12 23 82 45";
    private static final int[] array = new int[] {
        0x00, 0x00, 0x2F, 0x00, 0xD0, 0x06, 0x11, 0xA8, 0x08, 0xCD, 0x01, 0x1E, 0x01, 0x1E, 0x01, 0x1E, 0x03, 0x4F, 0x01, 0x1E, 0x56, 0x82, 0x11, 0xA8, 0x04, 0x6D, 0x01, 0x1E, 0xBB, 0xE0, 0x22, 0x01, 0x12, 0x21, 0x11, 0x11, 0x21, 0x11, 0x21, 0x11, 0x11, 0x22, 0x21, 0x12, 0x22, 0x11, 0x12, 0x23, 0x82, 0x45
    };

    /**
     * Test of newUeiLearned method, of class UeiLearnedSignal.
     */
    @Test
    public void testNewUeiLearned() {
        try {
            System.out.println("newUeiLearned");
            IrSignal ref = new IrSignal(necCcf);
            UeiLearned uei = UeiLearnedSignal.newUeiLearned(ref);
            String ueiString = uei.toString();
            IrSignal result = UeiLearnedSignal.parseUeiLearned(ueiString);
            assertEquals(result.isEqual(ref), true);
        } catch (IrpMasterException ex) {
            fail();
        }
    }

    /**
     * Test of parseUeiLearned method, of class UeiLearnedSignal.
     */
    @Test
    public void testParseUeiLearned_String() {
        System.out.println("parseUeiLearned");
        IrSignal result = UeiLearnedSignal.parseUeiLearned(necUei);
        try {
            IrSignal ref = new IrSignal(necCcf);
            assertEquals(result.isEqual(ref), true);
        } catch (IrpMasterException ex) {
            fail();
        }
    }

    /**
     * Test of parseUeiLearned method, of class UeiLearnedSignal.
     */
    @Test
    public void testParseUeiLearned_intArr() {
        System.out.println("parseUeiLearned");
        IrSignal result = UeiLearnedSignal.parseUeiLearned(array);
        try {
            IrSignal ref = new IrSignal(necCcf);
            assertEquals(result.isEqual(ref), true);
        } catch (IrpMasterException ex) {
            assert (false);
        }
    }
}
