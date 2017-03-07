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
    public static final String longFormattingCode = "%016X";

    public static String sixteenDigitHex(long n) {
        return String.format(longFormattingCode, n);
    }

    private static String processBitFields(long... args) {
        return sixteenDigitHex(processBitFieldsLong(args));
    }

    static long processBitFieldsLong(long... args) {
        if (args.length % 2 != 0)
            throw new ArithmeticException("Number of argument was " + args.length + "; must be a multiple of 2");

        int numberBitFields = args.length / 2;
        long result = 0L;
        for (int i = 0; i < numberBitFields; i++) {
            long length = args[2*i+1];
            long bf = args[2*i];
            result <<= length;
            result |= bf;
        }
        return result;
    }

    static long processFiniteBitFieldLong(boolean complement, boolean reverse, int data, int length, int chop) {
        long payload = data;
        if (complement)
            payload = ~payload;
        if (reverse)
            payload = reverse(payload, length);
        payload >>>= chop;
        return payload & mkMask(length);
    }

    public static String processBitFields(boolean complement, boolean reverse, int data, int length, int chop) {
        long bf = processFiniteBitFieldLong(complement, reverse, data, length, chop);
        return processBitFields(bf, length);
    }

    public static String processBitFields(boolean complement0, boolean reverse0, int data0, int length0, int chop0,
            boolean complement1, boolean reverse1, int data1, int length1, int chop1) {
        long bf0 = processFiniteBitFieldLong(complement0, reverse0, data0, length0, chop0);
        long bf1 = processFiniteBitFieldLong(complement1, reverse1, data1, length1, chop1);
        return processBitFields(bf0, length0, bf1, length1);
    }

    public static String processBitFields(boolean complement0, boolean reverse0, int data0, int length0, int chop0,
            boolean complement1, boolean reverse1, int data1, int length1, int chop1,
            boolean complement2, boolean reverse2, int data2, int length2, int chop2) {
        long bf0 = processFiniteBitFieldLong(complement0, reverse0, data0, length0, chop0);
        long bf1 = processFiniteBitFieldLong(complement1, reverse1, data1, length1, chop1);
        long bf2 = processFiniteBitFieldLong(complement2, reverse2, data2, length2, chop2);
        return processBitFields(bf0, length0, bf1, length1, bf2, length2);
    }

    public static String processBitFields(boolean complement0, boolean reverse0, int data0, int length0, int chop0,
            boolean complement1, boolean reverse1, int data1, int length1, int chop1,
            boolean complement2, boolean reverse2, int data2, int length2, int chop2,
            boolean complement3, boolean reverse3, int data3, int length3, int chop3) {
        long bf0 = processFiniteBitFieldLong(complement0, reverse0, data0, length0, chop0);
        long bf1 = processFiniteBitFieldLong(complement1, reverse1, data1, length1, chop1);
        long bf2 = processFiniteBitFieldLong(complement2, reverse2, data2, length2, chop2);
        long bf3 = processFiniteBitFieldLong(complement3, reverse3, data3, length3, chop3);
        return processBitFields(bf0, length0, bf1, length1, bf2, length2, bf3, length3);
    }

    public static String processBitFields(boolean complement0, boolean reverse0, int data0, int length0, int chop0,
            boolean complement1, boolean reverse1, int data1, int length1, int chop1,
            boolean complement2, boolean reverse2, int data2, int length2, int chop2,
            boolean complement3, boolean reverse3, int data3, int length3, int chop3,
            boolean complement4, boolean reverse4, int data4, int length4, int chop4) {
        long bf0 = processFiniteBitFieldLong(complement0, reverse0, data0, length0, chop0);
        long bf1 = processFiniteBitFieldLong(complement1, reverse1, data1, length1, chop1);
        long bf2 = processFiniteBitFieldLong(complement2, reverse2, data2, length2, chop2);
        long bf3 = processFiniteBitFieldLong(complement3, reverse3, data3, length3, chop3);
        long bf4 = processFiniteBitFieldLong(complement4, reverse4, data4, length4, chop4);
        return processBitFields(bf0, length0, bf1, length1, bf2, length2, bf3, length3, bf4, length4);
    }

    public static String processBitFields(boolean complement0, boolean reverse0, int data0, int length0, int chop0,
            boolean complement1, boolean reverse1, int data1, int length1, int chop1,
            boolean complement2, boolean reverse2, int data2, int length2, int chop2,
            boolean complement3, boolean reverse3, int data3, int length3, int chop3,
            boolean complement4, boolean reverse4, int data4, int length4, int chop4,
            boolean complement5, boolean reverse5, int data5, int length5, int chop5) {
        long bf0 = processFiniteBitFieldLong(complement0, reverse0, data0, length0, chop0);
        long bf1 = processFiniteBitFieldLong(complement1, reverse1, data1, length1, chop1);
        long bf2 = processFiniteBitFieldLong(complement2, reverse2, data2, length2, chop2);
        long bf3 = processFiniteBitFieldLong(complement3, reverse3, data3, length3, chop3);
        long bf4 = processFiniteBitFieldLong(complement4, reverse4, data4, length4, chop4);
        long bf5 = processFiniteBitFieldLong(complement5, reverse5, data5, length5, chop5);
        return processBitFields(bf0, length0, bf1, length1, bf2, length2, bf3, length3, bf4, length4, bf5, length5);
    }

    public static String processBitFields(
            boolean complement0, boolean reverse0, int data0, int length0, int chop0,
            boolean complement1, boolean reverse1, int data1, int length1, int chop1,
            boolean complement2, boolean reverse2, int data2, int length2, int chop2,
            boolean complement3, boolean reverse3, int data3, int length3, int chop3,
            boolean complement4, boolean reverse4, int data4, int length4, int chop4,
            boolean complement5, boolean reverse5, int data5, int length5, int chop5,
            boolean complement6, boolean reverse6, int data6, int length6, int chop6) {
        long bf0 = processFiniteBitFieldLong(complement0, reverse0, data0, length0, chop0);
        long bf1 = processFiniteBitFieldLong(complement1, reverse1, data1, length1, chop1);
        long bf2 = processFiniteBitFieldLong(complement2, reverse2, data2, length2, chop2);
        long bf3 = processFiniteBitFieldLong(complement3, reverse3, data3, length3, chop3);
        long bf4 = processFiniteBitFieldLong(complement4, reverse4, data4, length4, chop4);
        long bf5 = processFiniteBitFieldLong(complement5, reverse5, data5, length5, chop5);
        long bf6 = processFiniteBitFieldLong(complement6, reverse6, data6, length6, chop6);

        return processBitFields(bf0, length0, bf1, length1, bf2, length2, bf3, length3, bf4, length4,
                bf5, length5, bf6, length6);
    }

    public static String processBitFields(
            boolean complement0, boolean reverse0, int data0, int length0, int chop0,
            boolean complement1, boolean reverse1, int data1, int length1, int chop1,
            boolean complement2, boolean reverse2, int data2, int length2, int chop2,
            boolean complement3, boolean reverse3, int data3, int length3, int chop3,
            boolean complement4, boolean reverse4, int data4, int length4, int chop4,
            boolean complement5, boolean reverse5, int data5, int length5, int chop5,
            boolean complement6, boolean reverse6, int data6, int length6, int chop6,
            boolean complement7, boolean reverse7, int data7, int length7, int chop7) {
        long bf0 = processFiniteBitFieldLong(complement0, reverse0, data0, length0, chop0);
        long bf1 = processFiniteBitFieldLong(complement1, reverse1, data1, length1, chop1);
        long bf2 = processFiniteBitFieldLong(complement2, reverse2, data2, length2, chop2);
        long bf3 = processFiniteBitFieldLong(complement3, reverse3, data3, length3, chop3);
        long bf4 = processFiniteBitFieldLong(complement4, reverse4, data4, length4, chop4);
        long bf5 = processFiniteBitFieldLong(complement5, reverse5, data5, length5, chop5);
        long bf6 = processFiniteBitFieldLong(complement6, reverse6, data6, length6, chop6);
        long bf7 = processFiniteBitFieldLong(complement7, reverse7, data7, length7, chop7);

        return processBitFields(bf0, length0, bf1, length1, bf2, length2, bf3, length3, bf4, length4,
                bf5, length5, bf6, length6, bf7, length7);
    }

    public static String processBitFields(
            boolean complement0, boolean reverse0, int data0, int length0, int chop0,
            boolean complement1, boolean reverse1, int data1, int length1, int chop1,
            boolean complement2, boolean reverse2, int data2, int length2, int chop2,
            boolean complement3, boolean reverse3, int data3, int length3, int chop3,
            boolean complement4, boolean reverse4, int data4, int length4, int chop4,
            boolean complement5, boolean reverse5, int data5, int length5, int chop5,
            boolean complement6, boolean reverse6, int data6, int length6, int chop6,
            boolean complement7, boolean reverse7, int data7, int length7, int chop7,
            boolean complement8, boolean reverse8, int data8, int length8, int chop8) {
        long bf0 = processFiniteBitFieldLong(complement0, reverse0, data0, length0, chop0);
        long bf1 = processFiniteBitFieldLong(complement1, reverse1, data1, length1, chop1);
        long bf2 = processFiniteBitFieldLong(complement2, reverse2, data2, length2, chop2);
        long bf3 = processFiniteBitFieldLong(complement3, reverse3, data3, length3, chop3);
        long bf4 = processFiniteBitFieldLong(complement4, reverse4, data4, length4, chop4);
        long bf5 = processFiniteBitFieldLong(complement5, reverse5, data5, length5, chop5);
        long bf6 = processFiniteBitFieldLong(complement6, reverse6, data6, length6, chop6);
        long bf7 = processFiniteBitFieldLong(complement7, reverse7, data7, length7, chop7);
        long bf8 = processFiniteBitFieldLong(complement8, reverse8, data8, length8, chop8);

        return processBitFields(bf0, length0, bf1, length1, bf2, length2, bf3, length3, bf4, length4,
                bf5, length5, bf6, length6, bf7, length7, bf8, length8);
    }

    public static String processBitFields(
            boolean complement0, boolean reverse0, int data0, int length0, int chop0,
            boolean complement1, boolean reverse1, int data1, int length1, int chop1,
            boolean complement2, boolean reverse2, int data2, int length2, int chop2,
            boolean complement3, boolean reverse3, int data3, int length3, int chop3,
            boolean complement4, boolean reverse4, int data4, int length4, int chop4,
            boolean complement5, boolean reverse5, int data5, int length5, int chop5,
            boolean complement6, boolean reverse6, int data6, int length6, int chop6,
            boolean complement7, boolean reverse7, int data7, int length7, int chop7,
            boolean complement8, boolean reverse8, int data8, int length8, int chop8,
            boolean complement9, boolean reverse9, int data9, int length9, int chop9) {
        long bf0 = processFiniteBitFieldLong(complement0, reverse0, data0, length0, chop0);
        long bf1 = processFiniteBitFieldLong(complement1, reverse1, data1, length1, chop1);
        long bf2 = processFiniteBitFieldLong(complement2, reverse2, data2, length2, chop2);
        long bf3 = processFiniteBitFieldLong(complement3, reverse3, data3, length3, chop3);
        long bf4 = processFiniteBitFieldLong(complement4, reverse4, data4, length4, chop4);
        long bf5 = processFiniteBitFieldLong(complement5, reverse5, data5, length5, chop5);
        long bf6 = processFiniteBitFieldLong(complement6, reverse6, data6, length6, chop6);
        long bf7 = processFiniteBitFieldLong(complement7, reverse7, data7, length7, chop7);
        long bf8 = processFiniteBitFieldLong(complement8, reverse8, data8, length8, chop8);
        long bf9 = processFiniteBitFieldLong(complement9, reverse9, data9, length9, chop9);

        return processBitFields(bf0, length0, bf1, length1, bf2, length2, bf3, length3, bf4, length4,
                                bf5, length5, bf6, length6, bf7, length7, bf8, length8, bf9, length9);
    }

    public static String processBitFields(
            boolean complement0, boolean reverse0, int data0, int length0, int chop0,
            boolean complement1, boolean reverse1, int data1, int length1, int chop1,
            boolean complement2, boolean reverse2, int data2, int length2, int chop2,
            boolean complement3, boolean reverse3, int data3, int length3, int chop3,
            boolean complement4, boolean reverse4, int data4, int length4, int chop4,
            boolean complement5, boolean reverse5, int data5, int length5, int chop5,
            boolean complement6, boolean reverse6, int data6, int length6, int chop6,
            boolean complement7, boolean reverse7, int data7, int length7, int chop7,
            boolean complement8, boolean reverse8, int data8, int length8, int chop8,
            boolean complement9, boolean reverse9, int data9, int length9, int chop9,
            boolean complement10, boolean reverse10, int data10, int length10, int chop10) {
        long bf0 = processFiniteBitFieldLong(complement0, reverse0, data0, length0, chop0);
        long bf1 = processFiniteBitFieldLong(complement1, reverse1, data1, length1, chop1);
        long bf2 = processFiniteBitFieldLong(complement2, reverse2, data2, length2, chop2);
        long bf3 = processFiniteBitFieldLong(complement3, reverse3, data3, length3, chop3);
        long bf4 = processFiniteBitFieldLong(complement4, reverse4, data4, length4, chop4);
        long bf5 = processFiniteBitFieldLong(complement5, reverse5, data5, length5, chop5);
        long bf6 = processFiniteBitFieldLong(complement6, reverse6, data6, length6, chop6);
        long bf7 = processFiniteBitFieldLong(complement7, reverse7, data7, length7, chop7);
        long bf8 = processFiniteBitFieldLong(complement8, reverse8, data8, length8, chop8);
        long bf9 = processFiniteBitFieldLong(complement9, reverse9, data9, length9, chop9);
        long bf10 = processFiniteBitFieldLong(complement10, reverse10, data10, length10, chop10);
        return processBitFields(bf0, length0, bf1, length1, bf2, length2, bf3, length3, bf4, length4,
                                bf5, length5, bf6, length6, bf7, length7, bf8, length8, bf9, length9, bf10, length10);
    }

    public static String processBitFields(
            boolean complement0, boolean reverse0, int data0, int length0, int chop0,
            boolean complement1, boolean reverse1, int data1, int length1, int chop1,
            boolean complement2, boolean reverse2, int data2, int length2, int chop2,
            boolean complement3, boolean reverse3, int data3, int length3, int chop3,
            boolean complement4, boolean reverse4, int data4, int length4, int chop4,
            boolean complement5, boolean reverse5, int data5, int length5, int chop5,
            boolean complement6, boolean reverse6, int data6, int length6, int chop6,
            boolean complement7, boolean reverse7, int data7, int length7, int chop7,
            boolean complement8, boolean reverse8, int data8, int length8, int chop8,
            boolean complement9, boolean reverse9, int data9, int length9, int chop9,
            boolean complement10, boolean reverse10, int data10, int length10, int chop10,
            boolean complement11, boolean reverse11, int data11, int length11, int chop11) {
        long bf0 = processFiniteBitFieldLong(complement0, reverse0, data0, length0, chop0);
        long bf1 = processFiniteBitFieldLong(complement1, reverse1, data1, length1, chop1);
        long bf2 = processFiniteBitFieldLong(complement2, reverse2, data2, length2, chop2);
        long bf3 = processFiniteBitFieldLong(complement3, reverse3, data3, length3, chop3);
        long bf4 = processFiniteBitFieldLong(complement4, reverse4, data4, length4, chop4);
        long bf5 = processFiniteBitFieldLong(complement5, reverse5, data5, length5, chop5);
        long bf6 = processFiniteBitFieldLong(complement6, reverse6, data6, length6, chop6);
        long bf7 = processFiniteBitFieldLong(complement7, reverse7, data7, length7, chop7);
        long bf8 = processFiniteBitFieldLong(complement8, reverse8, data8, length8, chop8);
        long bf9 = processFiniteBitFieldLong(complement9, reverse9, data9, length9, chop9);
        long bf10 = processFiniteBitFieldLong(complement10, reverse10, data10, length10, chop10);
        long bf11 = processFiniteBitFieldLong(complement11, reverse11, data11, length11, chop11);
        return processBitFields(bf0, length0, bf1, length1, bf2, length2, bf3, length3, bf4, length4,
                                bf5, length5, bf6, length6, bf7, length7, bf8, length8, bf9, length9,
                                bf10, length10, bf11, length11);
    }

    public static String processBitFields(
            boolean complement0, boolean reverse0, int data0, int length0, int chop0,
            boolean complement1, boolean reverse1, int data1, int length1, int chop1,
            boolean complement2, boolean reverse2, int data2, int length2, int chop2,
            boolean complement3, boolean reverse3, int data3, int length3, int chop3,
            boolean complement4, boolean reverse4, int data4, int length4, int chop4,
            boolean complement5, boolean reverse5, int data5, int length5, int chop5,
            boolean complement6, boolean reverse6, int data6, int length6, int chop6,
            boolean complement7, boolean reverse7, int data7, int length7, int chop7,
            boolean complement8, boolean reverse8, int data8, int length8, int chop8,
            boolean complement9, boolean reverse9, int data9, int length9, int chop9,
            boolean complement10, boolean reverse10, int data10, int length10, int chop10,
            boolean complement11, boolean reverse11, int data11, int length11, int chop11,
            boolean complement12, boolean reverse12, int data12, int length12, int chop12) {
        long bf0 = processFiniteBitFieldLong(complement0, reverse0, data0, length0, chop0);
        long bf1 = processFiniteBitFieldLong(complement1, reverse1, data1, length1, chop1);
        long bf2 = processFiniteBitFieldLong(complement2, reverse2, data2, length2, chop2);
        long bf3 = processFiniteBitFieldLong(complement3, reverse3, data3, length3, chop3);
        long bf4 = processFiniteBitFieldLong(complement4, reverse4, data4, length4, chop4);
        long bf5 = processFiniteBitFieldLong(complement5, reverse5, data5, length5, chop5);
        long bf6 = processFiniteBitFieldLong(complement6, reverse6, data6, length6, chop6);
        long bf7 = processFiniteBitFieldLong(complement7, reverse7, data7, length7, chop7);
        long bf8 = processFiniteBitFieldLong(complement8, reverse8, data8, length8, chop8);
        long bf9 = processFiniteBitFieldLong(complement9, reverse9, data9, length9, chop9);
        long bf10 = processFiniteBitFieldLong(complement10, reverse10, data10, length10, chop10);
        long bf11 = processFiniteBitFieldLong(complement11, reverse11, data11, length11, chop11);
        long bf12 = processFiniteBitFieldLong(complement12, reverse12, data12, length12, chop12);
        return processBitFields(bf0, length0, bf1, length1, bf2, length2, bf3, length3, bf4, length4,
                                bf5, length5, bf6, length6, bf7, length7, bf8, length8, bf9, length9,
                                bf10, length10, bf11, length11, bf12, length12);
    }

    public static int reverse(int n, int bits) {
        int mask = (1 << bits) - 1;
        return Integer.reverse(n & mask) >>> (Integer.SIZE - bits);
    }

    public static long reverse(long n, long bits) {
        long mask = mkMask(bits);
        return Long.reverse(n & mask) >>> (Long.SIZE - bits);
    }

    public static int xor(int x, int y) {
        return x ^ y;
    }

    public static long xor(long x, long y) {
        return x ^ y;
    }

    public static int or(int x, int y) {
        return x | y;
    }

    public static long or(long x, long y) {
        return x | y;
    }

    public static int and(int x, int y) {
        return x & y;
    }

    public static long and(long x, long y) {
        return x & y;
    }

    private static long mkMask(long length) {
        return (1L << length) - 1L;
    }

    public static String twoDigitHex(int n) {
        return String.format(formattingCode, n);
    }

    public static String twoDigitReverseHex(int n) {
        return twoDigitHex(Integer.reverse(n) >>> 24);
    }

    public static String rc5Data(int D, int F, int T) {
        return processBitFields(true, false, F, 1, 6,
                false, false, T, 1, 0,
                false, false, D, 5, 0,
                false, false, F, 6, 0);
    }

    public static String sony12Data(int D, int F) {
        return processBitFields(false, true, F, 7, 0,
                false, true, D, 5, 0);
    }

    public static String sony15Data(int D, int F) {
        return processBitFields(false, true, F, 7, 0,
                false, true, D, 8, 0);
    }

    public static String sony20Data(int D, int S, int F) {
        return processBitFields(false, true, F, 7, 0,
                false, true, D, 5, 0,
                false, true, S, 8, 0);
    }

    private ExporterUtils() {
    }
}
