/*
Copyright (C) 2009-2012 Bengt Martensson.

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

package org.harctoolbox.harchardware.beacon;

import java.io.IOException;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import org.harctoolbox.IrpMaster.IrpUtils;
import org.harctoolbox.harchardware.Utils;


public class AmxBeacon implements Serializable {
    public  final static String broadcastIp = "239.255.250.250";
    public  final static int broadcastPort = 9131;
    public  final static String beaconPreamble = "AMXB";
    private final static int beaconPeriod = 30 * 1000;

    private final String payload;

    public AmxBeacon(String payload) {
        this.payload = payload;
    }

    private void sendCommandUdp() throws UnknownHostException, IOException {
        //boolean success = false;

        DatagramSocket sock = null;
        try {
            sock = new DatagramSocket();
        } catch (SocketException ex) {
            System.err.println(ex);
            return;
        }
        try {
            //sock.setSoTimeout(timeout);
            InetAddress addr = InetAddress.getByName(broadcastIp);
            byte[] buf = null;
            try {
                buf = payload.getBytes(IrpUtils.dumbCharsetName);

                DatagramPacket dp = new DatagramPacket(buf, buf.length, addr, broadcastPort);
                sock.send(dp);
            } catch (UnsupportedEncodingException ex) {
                assert false;
            }
            //success = true;
        } finally {
            sock.close();
        }
    }

    private static String pack(String key, String value) {
        return "<" + key + "=" + value + ">";
    }

    private static String createPayload(String uuid, String utility, String make, String model,
            String revision, String configName, String configUrl) {
        return beaconPreamble
                + pack("-UUID", uuid)
                + pack("-SDKClass", utility)
                + pack("-Make", make)
                + pack("-Model", model)
                + pack("-Revision", revision)
                + pack("Config-Name", configName)
                + pack("Config-URL", configUrl);
    }

    private static class BeaconThread extends Thread {

        AmxBeacon beacon;

        BeaconThread(AmxBeacon beacon) {
            this.beacon = beacon;
        }

        @Override
        public void run() {
            while (true) {
                try {
                    beacon.sendCommandUdp();
                    Thread.sleep(beaconPeriod);
                } catch (IOException | InterruptedException ex) {
                    System.err.println(ex.getMessage());
                }
            }
        }
    }

    public static void main(String[] args) {
        try {
            String hostname = Utils.getHostname();
            String macAddress = Utils.getMacAddress(InetAddress.getByName(hostname));

            AmxBeacon amx = new AmxBeacon(createPayload(hostname + "@" + macAddress, "HarcToolbox", "zzz", "0000", "0.0.0", "xyz", "http://localhost"));
            BeaconThread thread = new BeaconThread(amx);
            thread.start();
        } catch (UnknownHostException ex) {
            System.err.println(ex.getMessage());
        }
    }
}
