/*
Copyright (C) 2011 Bengt Martensson.

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

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;

/**
 * This class ...
 *
 * @author Bengt Martensson
 */
public class Utils {

    public static final int invalid = -1;

    public static String getHostname() {
        String hostname = System.getenv("HOSTNAME");
        if (hostname == null)
            hostname = System.getenv("HOSTNAME");
        if (hostname == null)
            hostname = System.getenv("COMPUTERNAME");

        return hostname;
    }

    /**
     * Returns the MAC address for the argument IP.
     * @param address IP address.
     * @return MAC address as string.
     */
    public static String getMacAddress(InetAddress address) {
        try {
            NetworkInterface ni = NetworkInterface.getByInetAddress(address);
            StringBuilder macAddress = new StringBuilder();
            if (ni != null) {
                byte[] mac = ni.getHardwareAddress();
                if (mac != null) {
                    for (int i = 0; i < mac.length; i++) {
                        macAddress.append(String.format("%02X%s", mac[i], ((i == mac.length - 1) ? "" : "-")));
                    }
                }
            }
            return macAddress.toString();
        } catch (SocketException ex) {
            return null;
        }
    }

    public static byte[] readBytes(InputStream inStream, int length) throws TimeoutException, IOException  {
        byte[] result = new byte[length];
        int index = 0;
        while (index < length) {
            int bytesRead = inStream.read(result, index, length - index);
            if (bytesRead == 0)
                throw new TimeoutException("Timeout in readBytes");
            index += bytesRead;
        }
        return result;
    }

    //public static String evaluateEscapes(String in) {
    //    return in.endsWith("\\r") ? in.substring(0, in.length() - 2) + "\r" : in;
    //}

    /**
     * Just for testing.
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        String hostname = getHostname();
        String macAddress = null;
        try {
            macAddress = getMacAddress(InetAddress.getByName(hostname));
        } catch (UnknownHostException ex) {
            System.err.println(ex.getMessage());
        }
        System.out.println(hostname + " " + macAddress);

        //System.out.println(evaluateEscapes("123\\r45\\n6"));
    }

}
