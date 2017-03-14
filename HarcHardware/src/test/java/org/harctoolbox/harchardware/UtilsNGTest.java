/*
Copyright (C) 2017 Bengt Martensson.

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; either version 3 of the License, or (at
your option) any later version.

This program is distributed in the hope that it will be useful, but
WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
General Public License for more details.

You should have received a copy of the GNU General Public License along with
this program. If not, see http://www.gnu.org/licenses/.
*/

package org.harctoolbox.harchardware;

import static org.testng.Assert.assertEquals;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class UtilsNGTest {

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    public UtilsNGTest() {
    }

    @BeforeMethod
    public void setUpMethod() throws Exception {
    }

    @AfterMethod
    public void tearDownMethod() throws Exception {
    }

    /**
     * Test of getHostname method, of class Utils.
     */
    @Test(enabled = false)
    public void testGetHostname() {
        System.out.println("getHostname");
        String expResult = "epsilon";
        String result = Utils.getHostname();
        assertEquals(result, expResult);
    }

    /**
     * Test of escapeCommandLine method, of class Utils.
     */
    @Test
    public void testEscapeCommandLine() {
        System.out.println("escapeCommandLine");
        String cmd = "bla\rblurbb\r\n";
        String expResult = "bla\\rblurbb\\r\\n";
        String result = Utils.escapeCommandLine(cmd);
        assertEquals(result, expResult);
    }
}
