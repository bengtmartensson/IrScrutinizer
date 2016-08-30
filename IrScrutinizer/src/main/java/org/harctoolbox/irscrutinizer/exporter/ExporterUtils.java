/*
Copyright (C) 2016 Bengt Martensson.

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

package org.harctoolbox.irscrutinizer.exporter;

/**
 * Static support functions intended to be called from XSLT transformations.
 */
public class ExporterUtils {
    private static final String formattingCode = "%02X";

    public static int reverse(int n, int bits) {
        int mask = (1 << bits) - 1;
        return Integer.reverse(n & mask) >>> (Integer.SIZE - bits);
    }

    public static String twoDigitHex(int n) {
        return String.format(formattingCode, n);
    }

    public static String twoDigitReverseHex(int n) {
        return twoDigitHex(Integer.reverse(n) >>> 24);
    }

    public static String rc5Data(int D, int F, int T) {
        int data =
                (~F & 0x40) << 6
                | (T & 1) << 11
                | (D & 0x1F) << 6
                | (F & 0x3F);
        return String.format("%X", data);
    }

    public static String sony12Data(int D, int F) {
        int data = reverse(F, 7) << 5;
        data |= reverse(D, 5);
        return Integer.toHexString(data);
    }

    public static String sony15Data(int D, int F) {
        int data = reverse(F, 7) << 8;
        data |= reverse(D, 8);
        return Integer.toHexString(data);
    }

    public static String sony20Data(int D, int S, int F) {
        int data =
                  reverse(F, 7) << 13
                | reverse(D, 5) << 8
                | reverse(S, 8);
        return Integer.toHexString(data);
    }

    private ExporterUtils() {
    }
}
