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
public class CIdentifierFactoryNGTest {

    @BeforeClass
    public void setUpClass() throws Exception {
    }

    @AfterClass
    public void tearDownClass() throws Exception {
    }

    public CIdentifierFactoryNGTest() {
    }

    @BeforeMethod
    public void setUpMethod() throws Exception {
    }

    @AfterMethod
    public void tearDownMethod() throws Exception {
    }

    /**
     * Test of mkCIdentifier method, of class CIdentifierFactory.
     */
    @Test
    public void testMkCIdentifier() {
        System.out.println("mkCIdentifier");
        CIdentifierFactory instance = new CIdentifierFactory();
        assertEquals(instance.mkCIdentifier("foo bar", 7), "foo_bar");
        assertEquals(instance.mkCIdentifier("foo bar", 8), "foo_bar_1");
        assertEquals(instance.mkCIdentifier("foo bar", 9), "foo_bar_2");
        assertEquals(instance.mkCIdentifier("foo bar", 8), "foo_bar_1");
        assertEquals(instance.mkCIdentifier("0", 73), "_0");
    }

}
