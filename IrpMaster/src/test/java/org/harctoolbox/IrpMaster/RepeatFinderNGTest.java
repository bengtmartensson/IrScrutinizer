package org.harctoolbox.IrpMaster;

import static junit.framework.Assert.assertEquals;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 *
 * @author bengt
 */
public class RepeatFinderNGTest {

    public RepeatFinderNGTest() {
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
     * Test of findRepeat method, of class RepeatFinder.
     */
    @Test
    public void testFindRepeat_ModulatedIrSequence() {
        System.out.println("findRepeat");
        IrSignal irSignal;
        try {
            irSignal = Pronto.ccfSignal("0000 006C 0022 0002 015B 00AD 0016 0016 0016 0016 0016 0041 0016 0041 0016 0016 0016 0016 0016 0016 0016 0016 0016 0016 0016 0041 0016 0016 0016 0016 0016 0016 0016 0041 0016 0016 0016 0016 0016 0016 0016 0016 0016 0016 0016 0041 0016 0041 0016 0041 0016 0016 0016 0016 0016 0041 0016 0041 0016 0041 0016 0016 0016 0016 0016 0016 0016 0041 0016 0041 0016 06A4 015B 0057 0016 0E6C");
            ModulatedIrSequence irSequence = irSignal.toModulatedIrSequence(5);
            ModulatedIrSequence junk = new ModulatedIrSequence(new int[] { 1, 2, 3, 4}, irSignal.getFrequency(), irSignal.getDutyCycle());
            IrSignal reference = new IrSignal(irSignal.getFrequency(), irSignal.getDutyCycle(), irSignal.getIntroSequence(), irSignal.getRepeatSequence(), junk);
            irSequence = irSequence.append(junk);
            IrSignal rep = RepeatFinder.findRepeat(irSequence);
            assertEquals(reference.isEqual(rep, 1f, 0.01, 1f), true);
        } catch (IrpMasterException ex) {
            assert(false);
        }
    }
}
