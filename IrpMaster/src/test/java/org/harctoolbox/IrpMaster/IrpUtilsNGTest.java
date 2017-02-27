/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.harctoolbox.IrpMaster;

import static org.testng.Assert.assertEquals;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 *
 * @author bengt
 */
public class IrpUtilsNGTest {

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    public IrpUtilsNGTest() {
    }

    @BeforeMethod
    public void setUpMethod() throws Exception {
    }

    @AfterMethod
    public void tearDownMethod() throws Exception {
    }

    /**
     * Test of isEqual method, of class IrpUtils.
     */
    @Test
    public void testIsEqual() {
        System.out.println("isEqual");
        assertEquals(IrpUtils.isEqual(0f, 0f, 0, 0), true);
        assertEquals(IrpUtils.isEqual(0.0001, 0f, 0, 0), false);
        assertEquals(IrpUtils.isEqual(0.0001, 0f, 0.0001, 0), true);
        assertEquals(IrpUtils.isEqual(0.0001, 0f, 0, 0.5), false);
        assertEquals(IrpUtils.isEqual(1f, 2f, 0, 0.5), true);
        assertEquals(IrpUtils.isEqual(1f, 2f, 1, 0), true);
        //assertEquals(IrpUtils.isEqual(0.0001, 0f, 0, 0.5), false);
    }
}
