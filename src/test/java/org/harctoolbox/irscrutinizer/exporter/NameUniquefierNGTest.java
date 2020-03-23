/*
 * Copyright (C) 2020 bengt
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.harctoolbox.irscrutinizer.exporter;

import java.util.Arrays;
import java.util.List;
import static org.testng.Assert.*;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class NameUniquefierNGTest {
    private static final String[] nonUniqueNames = {
        "foo",
        "foo",
        "foo",
        "bar",
        "foo#2",
        "foo#2",
        "foo",
        "foo",
        "bar"
    };

    private static final String[] uniqueNames = {
        "foo",
        "foo#1",
        "foo#2",
        "bar",
        "foo#3",
        "foo#4",
        "foo#5",
        "foo#6",
        "bar#1"
    };

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    public NameUniquefierNGTest() {
    }

    @BeforeMethod
    public void setUpMethod() throws Exception {
    }

    @AfterMethod
    public void tearDownMethod() throws Exception {
    }

    /**
     * Test of mkUnique method, of class NameUniquefier.
     */
    @Test
    public void testMkUnique() {
        System.out.println("mkUnique");
        List<String> names = Arrays.asList(nonUniqueNames);
        List<String> expResult = Arrays.asList(uniqueNames);
        List<String> result = NameUniquefier.mkUnique(names);
        assertEquals(result, expResult);
    }
}
