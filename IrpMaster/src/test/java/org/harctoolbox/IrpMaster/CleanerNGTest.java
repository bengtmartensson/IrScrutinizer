package org.harctoolbox.IrpMaster;

import static org.testng.AssertJUnit.assertEquals;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class CleanerNGTest {

    public CleanerNGTest() {
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
     * Test of clean method, of class Cleaner.
     */
    @Test
    public void testClean_IrSequence() {
        System.out.println("clean");
        IrSignal irSignal;
        try {
            irSignal = Pronto.ccfSignal("0000 006C 0022 0002 015B 00AD 0016 0016 0016 0016 0016 0041 0016 0041 0016 0016 0016 0016 0016 0016 0016 0016 0016 0016 0016 0041 0016 0016 0016 0016 0016 0016 0016 0041 0016 0016 0016 0016 0016 0016 0016 0016 0016 0016 0016 0041 0016 0041 0016 0041 0016 0016 0016 0016 0016 0041 0016 0041 0016 0041 0016 0016 0016 0016 0016 0016 0016 0041 0016 0041 0016 06A4 015B 0057 0016 0E6C");
            IrSequence irSequence = irSignal.toModulatedIrSequence(5);
            IrSequence noisy = irSequence.noisify(60);
            IrSequence cleaned = Cleaner.clean(noisy);
            boolean result = irSequence.isEqual(cleaned);
            assertEquals(result, true);
        } catch (IrpMasterException ex) {
            assert (false);
        }
    }
}
