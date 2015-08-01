/*
Copyright (C) 2015 Bengt Martensson.

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

/**
 * This class defines an exception to be thrown when invalid Ethernet addresses are encountered.
 */
public class EthernetAddress {
    public final static int noBytes = 6;
    public final static String separator = ":";
    private byte[] data;

    public EthernetAddress(byte[] data) throws InvalidEthernetAddressException {
        if (data.length != noBytes)
            throw new InvalidEthernetAddressException();
        this.data = data;
    }

    public EthernetAddress(String str) throws InvalidEthernetAddressException {
        this(parse(str));
    }

    private static byte[] parse(String str) throws InvalidEthernetAddressException {
        try {
            byte[] addr = new byte[noBytes];
            if (str.length() == 2 * noBytes) {
                for (int i = 0; i < noBytes; i++)
                    addr[i] = (byte) Integer.parseInt(str.substring(2 * i, 2 * (i + 1)), 16);
            } else {
                String[] chunks = str.split(separator);
                if (chunks.length != noBytes)
                    throw new InvalidEthernetAddressException(str);

                int i = 0;
                for (String s : chunks)
                    addr[i++] = (byte) Integer.parseInt(s, 16);
            }
            return addr;
        } catch (NumberFormatException ex) {
            throw new InvalidEthernetAddressException(str);
        }
    }

    public byte[] toBytes() {
        byte[] answer = new byte[noBytes];
        System.arraycopy(data, noBytes, answer, 0, noBytes);
        return answer;
    }

    // I hate this...
    private int byte2uint(byte b) {
        return b >= 0 ? (int) b : b + 256;
    }

    @Override
    public String toString() {
        StringBuilder str = new StringBuilder();
        for (int i = 0; i < noBytes; i++) {
            str.append(Integer.toHexString(byte2uint(data[i])));
            if (i < noBytes - 1)
                str.append(separator);
        }
        return str.toString();
    }
}
