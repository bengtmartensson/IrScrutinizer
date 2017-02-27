package org.harctoolbox.IrpMaster;

import static org.testng.Assert.fail;
import static org.testng.AssertJUnit.assertEquals;
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

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    public RepeatFinderNGTest() {
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
            // Note: lasts gap is too short, should find three repetitons anyhow.
            int[] arr = new int[] { 9008, 4516, 552, 552, 552, 552, 552, 552, 552, 552, 552, 552, 552, 552, 552, 552, 552, 1717, 552, 1717, 552, 1717, 552, 1717, 552, 1717, 552, 1717, 552, 1717, 552, 1717, 552, 1717, 552, 1717, 552, 1717, 552, 1717, 552, 1717, 552, 1717, 552, 552, 552, 552, 552, 552, 552, 552, 552, 552, 552, 552, 552, 552, 552, 552, 552, 1717, 552, 1717, 552, 1717, 552, 38902, 9008, 2289, 552, 31080, 9008, 2289, 552, 31080, 9008, 2289, 552, 21080 };
            ModulatedIrSequence modulatedIrSequence = new ModulatedIrSequence(arr, 38400f);
            RepeatFinder repeatFinder = new RepeatFinder(modulatedIrSequence);
            assertEquals(repeatFinder.getRepeatFinderData().getNumberRepeats(), 3);
        } catch (IrpMasterException ex) {
            fail();
        }
    }
}
