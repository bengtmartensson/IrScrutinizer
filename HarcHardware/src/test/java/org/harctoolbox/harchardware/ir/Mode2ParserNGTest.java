package org.harctoolbox.harchardware.ir;

import java.io.IOException;
import java.io.StringReader;
import java.text.ParseException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.harctoolbox.IrpMaster.IrSequence;
import static org.testng.Assert.*;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class Mode2ParserNGTest {

    private static final String testString = "# comment\n\n"
            + "Using driver default on device auto\n"
            + "   space 1   \n\n"
            + "  pulse 2  \n"
            + "space 3\n"
            + "pulse 4\n"
            + "pulse 5\n"
            + "pulse 6\n"
            + "space 7\n"
            + "space 1000000\n"
            + "space 8\n"
            + "Donald J. Trump\n"
            + "pulse 0x99\n"
            + "space 8\n"
            + "pulse 9\n"
            + "space 10\n"
            + "pulse 11\n";


    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    public Mode2ParserNGTest() {
    }

    @BeforeMethod
    public void setUpMethod() throws Exception {
    }

    @AfterMethod
    public void tearDownMethod() throws Exception {
    }

    /**
     * Test of readIrSequencesUntilEOF method, of class Mode2Parser.
     * @throws java.io.IOException
     */
    @Test
    public void testReadIrSequencesUntilEOF() throws IOException {
        System.out.println("readIrSequencesUntilEOF");
        StringReader reader = new StringReader(testString);
        Mode2Parser instance = new Mode2Parser(reader, false, Mode2Parser.DEFAULTTHRESHOLD);
        List result;
        result = instance.readIrSequencesUntilEOF();
        assertEquals(result.size(), 2);
    }

    /**
     * Test of readIrSequence method, of class Mode2Parser.
     * @throws java.io.IOException
     */
    @Test
    @SuppressWarnings("UnusedAssignment")
    public void testReadIrSequence() throws IOException {
        System.out.println("readIrSequence");
        StringReader reader = new StringReader(testString);
        Mode2Parser instance = new Mode2Parser(reader, false, Mode2Parser.DEFAULTTHRESHOLD);
        IrSequence result;
        try {
            result = instance.readIrSequence();
            String expected = "+2 -3 +15 -1000007";
            assertEquals(result.toPrintString(true), expected);
        } catch (ParseException ex) {
            fail();
        }

        try {
            result = instance.readIrSequence();
            fail();
        } catch (ParseException ex) {
        }

        try {
            result = instance.readIrSequence();
            fail();
        } catch (ParseException ex) {
            assertEquals(ex.getMessage(), "For input string: \"0x99\"");
        }
        try {
            result = instance.readIrSequence();
            assertEquals(result.toPrintString(true), "+9 -10 +11 -50000");
        } catch (ParseException ex) {
            Logger.getLogger(Mode2ParserNGTest.class.getName()).log(Level.SEVERE, null, ex);
        }
        try {
            result = instance.readIrSequence();
            assertNull(result);
        } catch (ParseException ex) {
            fail();
        }
    }
}
