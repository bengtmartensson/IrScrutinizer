/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.harctoolbox.harchardware.ir;

import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;

/**
 *
 * @author bengt
 */
public class DevLircNGTest {

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    public DevLircNGTest() {
//        try {
//            instance = new DevLirc();
//            assertFalse(instance.isValid());
//            instance.open();
//            assertTrue(instance.isValid());
//        } catch (HarcHardwareException ex) {
//            fail();
//        }
    }

    @BeforeMethod
    public void setUpMethod() throws Exception {
    }

    @AfterMethod
    public void tearDownMethod() throws Exception {
    }

//    /**
//     * Test of canSend method, of class DevLirc.
//     */
//    @Test
//    public void testCanSend() {
//        System.out.println("canSend");
//        boolean result = instance.canSend();
//        assertTrue(result);
//    }
//
//    /**
//     * Test of canReceive method, of class DevLirc.
//     */
//    @Test
//    public void testCanReceive() {
//        System.out.println("canReceive");
//        boolean result = instance.canReceive();
//        assertTrue(result);
//    }
//
//    /**
//     * Test of getNumberTransmitters method, of class DevLirc.
//     */
//    @Test
//    public void testGetNumberTransmitters() {
//        System.out.println("getNumberTransmitters");
//        int expResult = 0;
//        int result = instance.getNumberTransmitters();
//        assertEquals(result, expResult);
//    }
//
//    /**
//     * Test of canSetCarrier method, of class DevLirc.
//     */
//    @Test
//    public void testCanSetCarrier() {
//        System.out.println("canSetCarrier");
//        boolean expResult = true;
//        boolean result = instance.canSetCarrier();
//        assertEquals(result, expResult);
//    }
//
//    /**
//     * Test of canSetTransmitter method, of class DevLirc.
//     */
//    @Test
//    public void testCanSetTransmitter() {
//        System.out.println("canSetTransmitter");
//        boolean expResult = true;
//        boolean result = instance.canSetTransmitter();
//        assertEquals(result, expResult);
//    }
//
//    /**
//     * Test of sendIr method, of class DevLirc.
//     */
//    @Test
//    public void testSendIr_3args_1() throws Exception {
//        System.out.println("sendIr");
//        int[] intro = new int[]{
//            9024, 4512, 564, 564, 564, 1692, 564, 564, 564, 1692, 564, 1692, 564, 1692, 564, 1692, 564, 564, 564, 1692, 564, 564, 564, 1692, 564, 564, 564, 564, 564, 564, 564, 564, 564, 1692, 564, 1692, 564, 1692, 564, 564, 564, 1692, 564, 1692, 564, 564, 564, 564, 564, 564, 564, 564, 564, 564, 564, 1692, 564, 564, 564, 564, 564, 1692, 564, 1692, 564, 1692, 564, 39756
//        };
//        int[] repeat = new int[]{9024, 2256, 564, 96156};
//
//
//        IrSignal irSignal = new IrSignal(38400f, -1f, new IrSequence(intro), new IrSequence(repeat), null);
//        int count = 30;
//        Transmitter transmitter = instance.getTransmitter();
//        boolean expResult = true;
//        boolean result = instance.sendIr(irSignal, count, transmitter);
//        assertEquals(result, expResult);
//    }
//
//    /**
//     * Test of receive method, of class DevLirc.
//     */
//    @Test
//    public void testReceive() throws Exception {
//        System.out.println("receive");
//        System.err.println(">>>>>>>>>>>>>>>>> Now shoot an IR signal");
//        IrSequence result = instance.receive();
//        System.out.println(result);
//    }
//
//    /**
//     * Test of getVersion method, of class DevLirc.
//     */
//    @Test
//    public void testGetVersion() throws Exception {
//        System.out.println("getVersion");
//        String expResult = "LircDevice 0.1.0";
//        String result = instance.getVersion();
//        assertEquals(result, expResult);
//    }
//
//     /**
//     * Test of getTransmitter method, of class DevLirc.
//     */
//    @Test
//    public void testGetTransmitter_0args() {
//        System.out.println("getTransmitter");
//        Transmitter expResult = null;
//        Transmitter result = instance.getTransmitter();
//     }
}
