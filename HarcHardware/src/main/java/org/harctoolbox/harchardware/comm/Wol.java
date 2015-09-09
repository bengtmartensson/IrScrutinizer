/*
 Copyright (C) 2014, 2015 Bengt Martensson.

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

package org.harctoolbox.harchardware.comm;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Arrays;
import org.harctoolbox.harchardware.HarcHardwareException;
import org.harctoolbox.harchardware.misc.Ethers;

/**
 * This class sends a wakeup package to a host, given by its MAC address or hostname (provided that it is present in the ethers data base).
 */
public class Wol {

    private static Ethers ethers = null;
    private EthernetAddress ethernetAddress;
    private boolean verbose = false;
    public final static String defaultIP = "255.255.255.255";
    public final static int defaultPort = 9;
    private InetAddress ip;
    private final int port;

    private synchronized void setupEthers() throws FileNotFoundException {
        if (ethers == null)
            ethers = new Ethers();
    }

    public Wol(String str) throws FileNotFoundException, HarcHardwareException {
        this(str, false, null, defaultPort);
    }

    public Wol(String str, boolean verbose) throws FileNotFoundException, HarcHardwareException {
        this(str, verbose, null, defaultPort);
    }

    public Wol(String str, boolean verbose, InetAddress ip) throws FileNotFoundException, HarcHardwareException {
        this(str, verbose, ip, defaultPort);
    }

    /**
     * Constructor for Wol.
     * @param str Either ethernet address or a host name found in the ethers data base
     * @param verbose verbose execution
     * @param ip IP address to send to, should be a broadcast address (255.255.255.255)
     * @param port port to send to, normally 9
     * @throws FileNotFoundException ethers data base file was not found.
     * @throws HarcHardwareException
     */
    public Wol(String str, boolean verbose, InetAddress ip, int port) throws FileNotFoundException, HarcHardwareException {
        this.verbose = verbose;
        this.ip = ip;
        this.port = port;
        try {
            if (ip == null)
                this.ip = InetAddress.getByName(defaultIP);
        } catch (UnknownHostException ex) {
            // cannot happen
        }
        try {
            this.ethernetAddress = new EthernetAddress(str);
        } catch (InvalidEthernetAddressException ex) {
            // the argument did not parse as Ethernet address, try as hostname
            setupEthers();
            String mac = ethers.getMac(str);
            if (mac == null)
                throw new HarcHardwareException("No MAC address for " + str + " found");
            try {
                this.ethernetAddress = new EthernetAddress(mac);
            } catch (InvalidEthernetAddressException ex1) {
                // error in ethers, unprobable but not impossible
                throw new HarcHardwareException("Invalid Ethernet address for " + str + " found (" + mac + ")");
            }
        }
    }

    public static void wol(String str) throws IOException, FileNotFoundException, FileNotFoundException, HarcHardwareException {
        (new Wol(str)).wol();
    }

    public static void wol(String str, boolean verbose) throws IOException, FileNotFoundException, FileNotFoundException, HarcHardwareException {
        (new Wol(str, verbose)).wol();
    }

    public static void wol(String str, boolean verbose, InetAddress ip) throws IOException, FileNotFoundException, FileNotFoundException, HarcHardwareException {
        (new Wol(str, verbose, ip)).wol();
    }

    public static void wol(String str, boolean verbose, InetAddress ip, int port) throws IOException, FileNotFoundException, FileNotFoundException, HarcHardwareException {
        (new Wol(str, verbose, ip, port)).wol();
    }

    @Override
    public String toString() {
        return "wol " + ethernetAddress.toString();
    }

    private byte[] createWakeupFrame() {
        byte[] ethernetAddressBytes = ethernetAddress.toBytes();
        byte[] wakeupFrame = new byte[6 + 16 * ethernetAddressBytes.length];
        Arrays.fill(wakeupFrame, 0, 6, (byte) 0xFF);
        for (int j = 6; j < wakeupFrame.length; j += ethernetAddressBytes.length)
            System.arraycopy(ethernetAddressBytes, 0, wakeupFrame, j, ethernetAddressBytes.length);

        return wakeupFrame;
    }

    /**
     * Wakes up the machines with provided Ethernet addresses. The magic
     * sequences are sent to the given host and port.
     *
     * @throws java.net.SocketException
     * @throws IOException if an I/O error occurs
     */
    public void wol() throws SocketException, IOException {
        if (verbose)
            System.err.println("Sent WOL to " + ethernetAddress.toString());
        try (DatagramSocket socket = new DatagramSocket()) {
            byte[] wakeupFrame = createWakeupFrame();
            DatagramPacket packet = new DatagramPacket(wakeupFrame, wakeupFrame.length, ip, port);
            socket.send(packet);
        }
    }

    public static void main(String[] args) {
        try {
            int arg_i = 0;
            if (args[arg_i].equals("-f")) {
                arg_i++;
                String ethersPath = args[arg_i++];
                Ethers.setPathname(ethersPath);
            }
            String thing = args[arg_i];
            wol(thing, true);
        } catch (IOException | HarcHardwareException ex) {
            System.err.println(ex.getMessage());
        }
    }
}
