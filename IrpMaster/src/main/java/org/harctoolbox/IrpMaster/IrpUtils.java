/*
Copyright (C) 2011, 2012, 2013 Bengt Martensson.

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
package org.harctoolbox.IrpMaster;

import java.io.*;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/**
 * This class is a collection of useful utilities as static functions and constants.
 */
public class IrpUtils {

    public final static String defaultConfigfile = "data" + File.separator + "IrpProtocols.ini";

    public final static String dumbCharsetName = "US-ASCII";
    public final static Charset dumbCharset = Charset.forName(dumbCharsetName);
    public final static Locale dumbLocale = Locale.US;

    public final static long invalid = -1L;
    public final static long all = -2L;
    public final static long some = -3L;

    // Symbolic names for exit statii
    public final static int exitSuccess             = 0;
    public final static int exitUsageError          = 1;
    public final static int exitSemanticUsageError  = 2;
    public final static int exitFatalProgramFailure = 3;
    public final static int exitInternalFailure     = 4;
    public final static int exitConfigReadError     = 5;
    public final static int exitConfigWriteError    = 6;
    public final static int exitIoError             = 7;
    public final static int exitXmlError            = 8;
    public final static int exitDynamicLinkError    = 9;
    public final static int exitThisCannotHappen    = 10;
    public final static int exitRestart             = 99; // An invoking script is supposed to restart the program

    public final static String jp1WikiUrl = "http://www.hifi-remote.com/wiki/index.php?title=Main_Page";
    public final static String irpNotationUrl = "http://www.hifi-remote.com/wiki/index.php?title=IRP_Notation";
    public final static String decodeIrUrl = "http://www.hifi-remote.com/wiki/index.php?title=DecodeIR";

    /**
     * Use if no information at all available.
     */
    public final static double defaultFrequency = 38000f;
    public final static double unknownDutyCycle = -1f;

    public final static double microseconds2seconds = 1E-6;
    public final static double seconds2microseconds = 1E6;
    public final static double milliseconds2seconds = 1E-3;
    public final static double seconds2milliseconds = 1E3;
    public final static double milliseconds2microseconds = 1E3;

    /**
     * Default absolute tolerance in micro seconds.
     */
    public static final double defaultAbsoluteTolerance = 60;

    /**
     * Default relative tolerance as a number between 0 and 1.
     */
    public static final double defaultRelativeTolerance = 0.2;

    /**
     * Default absolute tolerance for frequency comparison.
     */
    public static final double defaultFrequencyTolerance = 500;

    /**
     * Default min repeat last gap, in milli seconds.
     */
    public static final double defaultMinRepeatLastGap = 20000f; // 20 milli seconds minimum for a repetition

    /**
     * Joins the Strings in the second argument, starting at the first argument, separating them with the third argument.
     *
     * @param beg Index of first argument to consider,
     * @param s Array of strings to be joined
     * @param separator
     * @return String
     */

    public static String join(int beg, String[] s, String separator) {
        if (s == null || s.length <= beg)
            return "";

        StringBuilder res = new StringBuilder(10 * s.length);
        res.append(s[beg]);
        for (int i = beg+1; i < s.length; i++)
            res.append(separator).append(s[i]);

        return res.toString();
    }

    // For Java >= 1.8, this can/should be replaced by String.join.
    public static String join(Iterable<String> payload, String separator) {
        StringBuilder str = new StringBuilder(128);
        for (String s : payload) {
            if (str.length() > 0)
                str.append(separator);
            str.append(s);
        }
        return str.toString();
    }

    public static String join(String[] s, String separator) {
        return join(0, s, separator);
    }

    public static String join(String[] s, char separator) {
        return join(s, Character.toString(separator));
    }

    public static String stringArray(int[] array) {
        if (array == null)
            return null;
        if (array.length == 0)
            return "[]";

        StringBuilder result = new StringBuilder(10 * array.length);
        result.append("[").append(array[0]);
        for (int i = 1; i < array.length; i++)
            result.append(", ").append(array[i]);

        return result.append("]").toString();
    }

    public static double l1Norm(Double[] sequence) {
        double sum = 0;
        for (Double d : sequence)
            sum += Math.abs(d);
        return sum;
    }

    public static double l1Norm(double[] sequence) {
        return l1Norm(sequence, 0, sequence.length);
    }

    public static double l1Norm(double[] sequence, int beg, int length) {
        double sum = 0;
        for (int i = beg; i < beg + length; i++)
            sum += Math.abs(sequence[i]);
        return sum;
    }

    public static boolean isInstance(Object o, String classname) {
        try {
            return Class.forName("IrpMaster." + classname).isInstance(o);
        } catch (ClassNotFoundException ex) {
            System.err.println(ex.getMessage());
        }
        return false;
    }

    public static String spaces(int length) {
        byte[] buf = new byte[length];
        for (int i = 0; i < length; i++)
            buf[i] = 0x20;
        return new String(buf, IrpUtils.dumbCharset);
    }

    /**
     * Either opens a file (optionally for appending (if beginning with +)) or returns stdout.
     *
     * @param filename Either - for stdout, or a file name, or null. If starting with +, the file is opened in append mode, after removing the +-character.
     * @return Open PrintStream
     * @throws FileNotFoundException if FileOutputStream does
     */
    public static PrintStream getPrintSteam(String filename) throws FileNotFoundException {
        if (filename == null)
            return null;

        String realFilename = filename.startsWith("+") ? filename.substring(1) : filename;
        try {
            return filename.equals("-")
                    ? System.out
                    : new PrintStream(new FileOutputStream(realFilename, filename.startsWith("+")), false, dumbCharsetName);
        } catch (UnsupportedEncodingException ex) {
            assert false;
            return null;
        }
    }

    /**
     * Either opens an input file or returns stdin.
     *
     * @param filename
     * @return Open InputStream
     * @throws FileNotFoundException
     */
    public static InputStream getInputSteam(String filename) throws FileNotFoundException {
        return filename.equals("-") ? System.in : new FileInputStream(filename);
    }

    /**
     * Tests for approximate equality.
     *
     * @param x first argument
     * @param y second argument
     * @param absoluteTolerance
     * @param relativeTolerance
     * @return true if either absolute or relative requirement is satisfied.
     */
    public static boolean isEqual(double x, double y, double absoluteTolerance, double relativeTolerance) {
        double absDiff = Math.abs(x - y);
        boolean absoluteOk = absDiff <= absoluteTolerance;
        double max = Math.max(Math.abs(x), Math.abs(y));
        boolean relativeOk = max > 0 && absDiff/max <= relativeTolerance;
        return absoluteOk || relativeOk;
    }

    /**
     * Tests for approximate equality.
     *
     * @param x first argument
     * @param y second argument
     * @param absoluteTolerance
     * @param relativeTolerance
     * @return true if either absolute or relative requirement is satisfied.
     */
    public static boolean isEqual(int x, int y, int absoluteTolerance, double relativeTolerance) {
        int absDiff = Math.abs(x - y);
        boolean absoluteOk = absDiff <= absoluteTolerance;
        int max = Math.max(Math.abs(x), Math.abs(y));
        boolean relativeOk = max > 0 && absDiff/(double)max <= relativeTolerance;
        return absoluteOk || relativeOk;
    }

    /**
     * The power function for long arguments.
     *
     * @param x long
     * @param y long, non-negative
     * @return x raised to the y'th power
     *
     * @throws ArithmeticException
     */
    public static long power(long x, long y) {
        if (y < 0)
            throw new ArithmeticException("power to a negative integer is not sensible here.");
        long r = 1;
        for (long i = 0; i < y; i++)
            r *= x;
        return r;
    }

    /**
     * Reverses the bits, living in a width-bit wide world.
     *
     * @param x
     * @param width
     * @return bitreversed
     */

    public static long reverse(long x, int width) {
        long y = Long.reverse(x);
        if (width > 0)
            y >>>= Long.SIZE - width;
        return y;
    }

    /**
     * Parses integers of base 2 (prefix "0b"  or "%", 8 (leading 0), 10, or 16 (prefix "0x).
     * If argument special is true, allows intervals 123..456 or 123:456 by ignoring upper part.
     * and translates `*' to the constant "all" = (-2) and `#' to "some" (= -3).
     *
     * @param str String to be parsed
     * @param special If the special stuff should be interpreted ('*', '+', intervals).
     * @return long integer.
     */
    public static long parseLong(String str, boolean special) /*throws NumberFormatException*/ {
        if (special && (str.startsWith("#") || str.contains(",")))
            return some;

        String s = special ? str.replaceAll("[:.\\+<#].*$", "").trim() : str;
        if (special && (s.equals("*") || s.equals("'*'")))
            return all; // Just to help Windows' victims, who cannot otherwise pass a *.
        //s.equals("#") ? some :
        return s.startsWith("0x") ? Long.parseLong(s.substring(2), 16) :
               s.startsWith("0b") ? Long.parseLong(s.substring(2), 2) :
               s.startsWith("%") ? Long.parseLong(s.substring(1), 2) :
               s.equals("0") ? 0L :
               s.startsWith("0") ? Long.parseLong(s.substring(1), 8) :
               Long.parseLong(s);
    }

    /**
     * Parses integers of base 2 (prefix "0b"  or "%", 8 (leading 0), 10, or 16 (prefix "0x).
     *
     * @param str String to be parsed
     * @return long integer.
     */
    public static long parseLong(String str) {
        return parseLong(str,false);
    }

    public static long parseUpper(String str) {
        String[] s = str.split("\\.\\.");
        if (s.length == 1)
            s = str.split(":");

        return (s.length == 2) ? parseLong(s[1], false) : invalid;
    }

    public static long variableGet(Map<String, Long> map, String name) {
        return map.containsKey(name) ? map.get(name) :  invalid;
    }

    /**
     * Produces a header in the spirit of Makehex. Follows the convention of variable ordering:
     * D, (S), F, (T), then the rest alphabetically ordered,
     *
     * @param params tests- irpmaster?HashMap&lt;String, Long&gt; of input parameters.
     * @return Nicely formatted header (String)
     */
    public static String variableHeader(Map<String, Long> params) {
        TreeMap<String, Long> map = new TreeMap(params);
        map.remove("D");
        map.remove("F");
        map.remove("S");
        map.remove("T");

        String result = formatVariable(params, "D", "Device Code: ", "")
                + formatVariable(params, "S", ".", "")
                + " "
                + formatVariable(params, "F", "Function: ", "")
                + formatVariable(params, "T", ", Toggle: ", "");

        result = map.keySet().stream().map((var) -> formatVariable(params, var, ", " + var + "=", "")).reduce(result, String::concat);

        return result;
    }

    private static String formatVariable(Map<String, Long>map, String name, String prefix, String postfix) {
        if (!map.containsKey(name))
            return "";

        return prefix + map.get(name) + postfix;
    }

    public static String basename(String s) {
        StringBuilder sb = new StringBuilder(s);
        int n = sb.lastIndexOf(File.separator);
        if (n != -1)
            sb.delete(0, n+1);
        n = sb.lastIndexOf(".");
        if (n != -1)
            sb.delete(n, sb.length());
        return sb.toString();
    }

    public static String addExtensionIfNotPresent(String filename, String extension) {
        return filename + ((extension != null && !hasExtension(filename)) ? ('.' + extension) : "");
    }

    private static boolean hasExtension(String filename) {
        int lastSeparator = filename.lastIndexOf(File.separator);
        int lastPeriod = filename.lastIndexOf('.');
        return lastPeriod > lastSeparator;
    }

    public static Map<String,Long> mkParameters(long D, long S, long F) {
        Map<String, Long> result = new HashMap<>(3);
        if (D != IrpUtils.invalid)
            result.put("D", D);
        if (S != IrpUtils.invalid)
            result.put("S", S);
        if (F != IrpUtils.invalid)
            result.put("F", F);

        return result;
    }

    @SuppressWarnings("ValueOfIncrementOrDecrementUsed")
    public static void main(String[] args) {
        for (String arg : args)
            System.out.println(arg);

        int arg_i = 0;
        String outFile = args[arg_i++];
        String configFilename = args[arg_i++];
        String preamble = args[arg_i++];
        String protocolName = args[arg_i++];
        String device = args[arg_i++];
        String OBC = args[arg_i++];

        int status = IrpMaster.makeHex(outFile, true, configFilename, preamble, protocolName, device, OBC);
        System.out.println(status);
    }

    public static void exit(int exitcode) {
        System.exit(exitcode);
    }
}
