/*
 Copyright (C) 2014 Bengt Martensson.

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
import org.harctoolbox.harchardware.HarcHardwareException;
import org.harctoolbox.harchardware.misc.Ethers;
import wol.WakeUpUtil;
import wol.configuration.EthernetAddress;
import wol.configuration.IllegalEthernetAddressException;

/**
 * This class sends a wakeup package to a host, given by its MAC address or hostname (provided that it is present in the ethers data base).
 */

public class Wol {

    private static Ethers ethers = null;
    private EthernetAddress ethernetAddress;
    private boolean verbose = false;

    private synchronized void setupEthers() throws FileNotFoundException {
        if (ethers == null)
            ethers = new Ethers();
    }

    public Wol(String str) throws FileNotFoundException, HarcHardwareException {
        this(str, false);
    }

    public Wol(String str, boolean verbose) throws FileNotFoundException, HarcHardwareException {
        this.verbose = verbose;
        try {
            this.ethernetAddress = new EthernetAddress(str);
        } catch (IllegalEthernetAddressException ex) {
            setupEthers();
            String mac = ethers.getMac(str);
            if (mac == null)
                throw new HarcHardwareException("No MAC address for " + str + " found");
            try {
                this.ethernetAddress = new EthernetAddress(mac);
            } catch (IllegalEthernetAddressException ex1) {
                // Error in ethers, should not happen
                throw new RuntimeException(ex1);
            }
        }
    }

    public void wol() throws IOException {
        if (verbose)
            System.err.println("Sent WOL to " + ethernetAddress.toString());
        WakeUpUtil.wakeup(ethernetAddress);
    }

    public static void wol(String str) throws IOException, FileNotFoundException, FileNotFoundException, HarcHardwareException {
        (new Wol(str)).wol();
    }

    public static void wol(String str, boolean verbose) throws IOException, FileNotFoundException, FileNotFoundException, HarcHardwareException {
        (new Wol(str, verbose)).wol();
    }

    @Override
    public String toString() {
        return "wol " + ethernetAddress.toString();
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
        } catch (IOException ex) {
            System.err.println(ex.getMessage());
        } catch (HarcHardwareException ex) {
            System.err.println(ex.getMessage());
        }
    }
}
