/*
Copyright (C) 2011, 2012, 2015 Bengt Martensson.

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

import java.util.HashMap;

/**
 * This class allows for the creation of integer arrays
 * and strings containing Pronto (CCF) form of the signal. There are also some static
 * members that can possibly be used in other contexts. Immutable.
 */
public class Pronto {
    /** Number of characters in the hexadecimal digits of Pronto strings. */
    public final static int charsInDigit = 4;

    /** Constant used for computing the frequency code from the frequency */
    public final static double prontoConstant = 0.241246;
    private final static double dummyFrequency = 100000.0/prontoConstant;

    /** Format code used to format integers in the Pronto Hex. */
    public final static String formattingCode = "%04X";

    private final static int learnedCode = 0x0000;
    private final static int learnedZeroFrequencyCode = 0x0100;
    private final static int rc5Code = 0x5000;
    private final static int rc5xCode = 0x5001;
    private final static int rc6Code = 0x6000;
    private final static int nec1Code = 0x900a;
    private final static int rc5Frequency = 0x0073;
    private final static int rc5xFrequency = 0x0073;
    private final static int rc6Frequency = 0x0073;
    private final static int nec1Frequency = 0x006C;

    private final static String rc5Irp  = "{36k,msb,889}<1,-1|-1,1>((1:1,~F:1:6,T:1,D:5,F:6,^114m)+,T=1-T)[T@:0..1=0,D:0..31,F:0..127]";
    private final static String rc5xIrp = "{36k,msb,889}<1,-1|-1,1>(1:1,~S:1:6,T:1,D:5,-4,S:6,F:6,^114m,T=1-T)+ [D:0..31,S:0..127,F:0..63,T@:0..1=0]";
    private final static String rc6Irp  = "{36k,444,msb}<-1,1|1,-1>(6,-2,1:1,0:3,<-2,2|2,-2>(T:1),D:8,F:8,^107m,T=1-T)+ [D:0..255,F:0..255,T@:0..1=0]";
    private final static String nec1Irp = "{38.4k,564}<1,-1|1,-3>(16,-8,D:8,S:8,F:8,~F:8,1,-78,(16,-4,1,-173)*) [D:0..255,S:0..255=255-D,F:0..255]";

    private IrSignal irSignal;

    /**
     * Constructor from IrSignal.
     * @param irSignal
     * @throws IncompatibleArgumentException
     */
    public Pronto(IrSignal irSignal) throws IncompatibleArgumentException {
        this.irSignal = irSignal;
        if (irSignal.getEndingLength() != 0) {
            UserComm.warning("When computing the Pronto representation, a (non-empty) ending sequence was ignored");
        }
    }

    /**
     * Formats an integer like seen in CCF strings, in printf-ish, using "%04X".
     * @param n Integer to be formatted.
     * @return Formatted string
     */
    public final static String formatInteger(int n) {
        return String.format(formattingCode, n);
    }

    /**
     * Returns frequency code from frequency in Hz (the second number in the CCF).
     *
     * @param f Frequency in Hz.
     * @return code for the frequency.
     */
    public final static int getProntoCode(double f) {
        return (int) Math.round(1000000.0 / ((f>0 ? f : dummyFrequency) * prontoConstant));
    }

    /**
     * Computes the carrier frequency in Hz.
     * @param code Pronto frequency code
     * @return Frequency in Hz.
     */
    public final static double getFrequency(int code) {
        return code == 0
                ? IrpUtils.invalid // Invalid value
                : 1000000.0 / ((double) code * prontoConstant);
    }

    /**
     * Computes the carrier frequency in Hz.
     * @return Frequency in Hz.
     */
    public final double getFrequency() {
        return irSignal.frequency;
    }

    /**
     * Computes pulse time.
     * @param code Pronto frequency code
     * @return Duration of one pulse of the carrier in microseconds
     */
    public final static double getPulseTime(int code) { // in microseconds
        return code == 0
                ? IrpUtils.invalid // Invalid value
                : code * prontoConstant;
    }

    // TODO: fix for f=0,
    /**
     * Computes number of cycles of the carrier the first argument will require.
     *
     * @param us duration in microseconds
     * @param frequency
     * @return number of pulses
     */
    public final static int pulses(double us, double frequency) {
        return (int)Math.round(Math.abs(us) * (frequency > 0 ? frequency : dummyFrequency)/1000000.0);
    }

    /**
     * Computes a ccf array, without header.
     * @param frequency
     * @param sequence
     * @return CCF array
     */
    public final static int[] toArray(double frequency, double[] sequence) /*throws RuntimeException*/ {
        if (sequence.length % 2 == 1)
            throw new RuntimeException("IR Sequence must be of even length.");

        int[] data = new int[sequence.length];
        for (int i = 0; i < sequence.length; i++)
            data[i] = pulses(sequence[i], frequency);

        return data;
    }

    private static double[] usArray(int frequencyCode, int[] ccfArray, int beg, int end) {
        double pulseTime = getPulseTime(frequencyCode);
        double[] data = new double[end - beg];
        for (int i = beg; i < end; i++)
            data[i-beg] = pulseTime*ccfArray[i];

        return data;
    }

    /**
     * Creates a new IrSignals by interpreting its argument as CCF signal.
     * @param ccf CCF signal
     * @return  IrSignal
     * @throws ParseException
     * @throws IncompatibleArgumentException
     * @throws UnassignedException
     * @throws DomainViolationException
     * @throws InvalidRepeatException
     */
    public final static IrSignal ccfSignal(int[] ccf) throws ParseException, IncompatibleArgumentException, UnassignedException, DomainViolationException, InvalidRepeatException {
        if (ccf.length < 4)
            throw new IncompatibleArgumentException("CCF is invalid since less than 4 numbers long.");
        if (ccf.length % 2 != 0)
            throw new IncompatibleArgumentException("CCF is invalid since it has an odd number ("
                    + ccf.length + ") of durations.");
        int index = 0;
        int type = ccf[index++];
        int frequencyCode = ccf[index++];
        int introLength = ccf[index++];
        int repeatLength = ccf[index++];
        if (index + 2*(introLength+repeatLength) != ccf.length)
            throw new IncompatibleArgumentException("Inconsistent length in CCF (claimed "
                    + (introLength + repeatLength) + " pairs, was " + (ccf.length - 4)/2 + " pairs).");
        IrSignal irSignal = null;
        String irp = null;
        int dev = (int) IrpUtils.invalid;
        int subdev = (int) IrpUtils.invalid;
        int cmd = (int) IrpUtils.invalid;
        int pass = (int) IrpUtils.all;

        switch (type) {
            case learnedCode: // 0x0000
            case learnedZeroFrequencyCode: // 0x0100
                double[] intro = usArray(frequencyCode, ccf, index, index + 2*introLength);
                double[] repeat = usArray(frequencyCode, ccf, index + 2*introLength, ccf.length);
                IrSequence introSequence = new IrSequence(intro);
                IrSequence repeatSequence = new IrSequence(repeat);
                irSignal = new IrSignal(type == learnedCode ? getFrequency(frequencyCode) : 0,
                        IrpUtils.invalid, introSequence, repeatSequence, null);
                break;

            case rc5Code: // 0x5000:
                if (repeatLength != 1)
                    throw new IncompatibleArgumentException("wrong repeat length");
                irp = rc5Irp;
                pass = 1;
                dev = ccf[index++];
                cmd = ccf[index++];
                break;

            case rc5xCode: // 0x5001:
                if (repeatLength != 2)
                    throw new IncompatibleArgumentException("wrong repeat length");
                irp = rc5xIrp;
                pass = 1;
                dev = ccf[index++];
                subdev = ccf[index++];
                cmd = ccf[index++];
                break;

            case rc6Code: // 0x6000:
                if (repeatLength != 1)
                    throw new IncompatibleArgumentException("wrong repeat length");
                irp = rc6Irp;
                pass = 1;
                dev = ccf[index++];
                cmd = ccf[index++];
                break;

            case nec1Code: // 0x900a:
                if (repeatLength != 1)
                    throw new IncompatibleArgumentException("wrong repeat length");
                irp = nec1Irp;
		dev = ccf[index] >> 8;
                subdev = ccf[index++] & 0xff;
		cmd = ccf[index] >> 8;
		int cmd_chk = 0xff - (ccf[index++] & 0xff);
		if (cmd != cmd_chk)
		    throw new IncompatibleArgumentException("checksum erroneous");
                break;

            default:
                throw new IncompatibleArgumentException("CCF type 0x" + Integer.toHexString(type) + " not supported");
        }

        if (irSignal == null) {
            HashMap<String, Long> parameters = new HashMap<>();
            parameters.put("D", (long) dev);
            if (subdev != (int) IrpUtils.invalid)
                parameters.put("S", (long) subdev);
            parameters.put("F", (long) cmd);
            if (pass == 1)
                parameters.put("T", 0L);

            Protocol protocol = new Protocol(null, irp, null);
            irSignal = protocol.renderIrSignal(parameters, pass);
        }
        return irSignal;
    }

    /**
     * Creates a new IrSignals by interpreting its argument as CCF string.
     * @param ccfString CCF signal
     * @return IrSignal
     * @throws IncompatibleArgumentException
     */
    public final static IrSignal ccfSignal(String ccfString) throws IrpMasterException {
        int[] ccf;
        try {
            ccf = parseString(ccfString);
        } catch (NumberFormatException ex) {
            throw new IrpMasterException("Non-parseable CCF string: " + ccfString);
        }
        if (ccf == null)
            throw new IrpMasterException("Invalid CCF string: " + ccfString);

        return ccfSignal(ccf);
    }

    /**
     * Creates a new IrSignals by interpreting its argument as CCF string.
     * @param array Strings representing hexadecimal numbers
     * @param begin Starting index
     * @return  IrSignal
     * @throws IrpMasterException
     */
    public final static IrSignal ccfSignal(String[] array, int begin) throws IrpMasterException {
        int[] ccf;
        try {
            ccf = parseStringArray(array, begin);
        } catch (NumberFormatException ex) {
            throw new IrpMasterException("Non-parseable CCF strings");
        }
        if (ccf == null)
            throw new IrpMasterException("Invalid CCF strings");

        return ccfSignal(ccf);
    }

    /**
     * Tries to parse the string as argument.
     * Can be used to test "Proto-ness" of an unknown string.
     *
     * @param ccfString Input string, to be parsed/tested.
     * @return Integer array of numbers if successful, null if unsuccessful.
     * @throws NumberFormatException
     */
    public final static int[] parseString(String ccfString) throws NumberFormatException {
        String[] array = ccfString.trim().split("\\s+");
        return parseStringArray(array, 0);
    }

    /**
     * Tries to parse the strings as argument.
     * Can be used to test "Proto-ness" of an unknown array of strings.
     *
     * @param array Input strings, to be parsed/tested.
     * @param begin Starting index
     * @return Integer array of numbers if successful, null if unsuccessful.
     */
    public final static int[] parseStringArray(String[] array, int begin) {
        int[] ccf = new int[array.length];

        for (int i = begin; i < array.length; i++) {
            if (array[i].length() != charsInDigit || array[i].charAt(0) == '+' || array[i].charAt(0) == '-')
                return null;
            try {
                ccf[i] = Integer.parseInt(array[i], 16);
            } catch (NumberFormatException ex) {
                return null;
            }
        }
        return ccf;
    }

    /**
     * CCF array of initial sequence
     * @return CCF array
     */
    public final int[] initArray() {
        return toArray(irSignal.frequency, irSignal.introSequence.data);
    }

    /**
     * CCF array of repeat sequence
     * @return CCF array
     */
    public final int[] repeatArray() {
        return toArray(irSignal.frequency, irSignal.repeatSequence.data);
    }

    /**
     * CCF array of complete signal, i.e. the CCF string before formatting
     * @param frequency
     * @param intro
     * @param repetition
     * @return CCF array
     */
    public final static int[] toArray(double frequency, double[] intro, double[] repetition) /*throws RuntimeException*/ {
        if (intro.length % 2 == 1 || repetition.length % 2 == 1)
            throw new RuntimeException("IR Sequences must be of even length.");

        int[] data = new int[4 + intro.length + repetition.length];
        int index = 0;
        data[index++] = frequency > 0 ? learnedCode : learnedZeroFrequencyCode;
        data[index++] = getProntoCode(frequency);
        data[index++] = intro.length/2;
        data[index++] = repetition.length/2;
        for (int i = 0; i < intro.length; i++)
            data[index++] = pulses(intro[i], frequency);

        for (int i = 0; i < repetition.length; i++)
            data[index++] = pulses(repetition[i], frequency);

        return data;
    }

    /**
     * CCF array of complete signal, i.e. the CCF string before formatting
     * @return CCF array
     */
    public final int[] toArray() {
        if (irSignal.getIntroLength() % 2 != 0 || irSignal.getRepeatLength() % 2 != 0)
            // Probably forgot normalize() if I get here.
            throw new RuntimeException("IR Sequences must be of even length.");

        int[] data = new int[4 + irSignal.getIntroLength() + irSignal.getRepeatLength()];
        int index = 0;
        data[index++] = irSignal.getFrequency() > 0 ? learnedCode : learnedZeroFrequencyCode;
        data[index++] = getProntoCode(irSignal.getFrequency());
        data[index++] = irSignal.getIntroLength()/2;
        data[index++] = irSignal.getRepeatLength()/2;
        for (int i = 0; i < irSignal.getIntroLength(); i++)
            data[index++] = pulses(irSignal.getIntroDouble(i), irSignal.getFrequency());

        for (int i = 0; i < irSignal.getRepeatLength(); i++)
            data[index++] = pulses(irSignal.getRepeatDouble(i), irSignal.getFrequency());

        return data;
    }

    /**
     * Computes the ("long", raw) CCF string
     * @return CCF string
     */
    public String toPrintString() {
        return toPrintString(toArray());
    }

    /**
     * Formats a CCF as string.
     * @param array CCF in form of an integer array.
     * @return CCF string.
     */
    public static String toPrintString(int[] array) {
        String s = "";
        for (int i = 0; i < array.length; i++)
            s += String.format((i > 0 ? " " : "") + formattingCode, array[i]);
        return s;
    }

    /**
     * Computes the ("long", raw) CCF string
     *
     * @param irSignal
     * @return CCF string
     */
    public static String toPrintString(IrSignal irSignal) {
        String str = null;
        try {
            str = (new Pronto(irSignal)).toPrintString();
        } catch (IncompatibleArgumentException ex) {
            // cannot happen since irSignal is already checked.
        }
        return str;
    }

    /**
     * The string version of shortCCF(...).
     *
     * @param protocolName
     * @param device
     * @param subdevice
     * @param command
     * @return CCF as string, or null on failure.
     * @throws IncompatibleArgumentException
     */
    public final static String shortCCFString(String protocolName, int device, int subdevice, int command)
            throws IncompatibleArgumentException {
        int[] ccf = shortCCF(protocolName, device, subdevice, command);
        return ccf == null ? null : toPrintString(ccf);
    }

    /**
     * Computes the "short" Pronto form of some signals, given by protocol number and parameter values.
     *
     * @param protocolName Name of protcol, presently "rc5", "rc5x", "rc6", and "nec1".
     * @param device
     * @param subdevice
     * @param command
     * @return integer array of short CCF, or null om failure.
     * @throws IncompatibleArgumentException for paramters outside of its allowed domain.
     */
    public final static int[] shortCCF(String protocolName, int device, int subdevice, int command) throws IncompatibleArgumentException {
        int index = 0;
        if (protocolName.equalsIgnoreCase("rc5")) {
            if (device > 31 || subdevice != (int) IrpUtils.invalid || command > 127)
                throw new IncompatibleArgumentException("Invalid parameters");

            int[] result = new int[6];
            result[index++] = rc5Code;
            result[index++] = rc5Frequency;
            result[index++] = 0;
            result[index++] = 1;
            result[index++] = device;
            result[index++] = command;

            return result;
        } else if (protocolName.equalsIgnoreCase("rc5x")) {
            if (device > 31 || subdevice > 127 || subdevice < 0 || command > 63)
                throw new IncompatibleArgumentException("Invalid parameters");

            int[] result = new int[8];
            result[index++] = rc5xCode;
            result[index++] = rc5xFrequency;
            result[index++] = 0;
            result[index++] = 2;
            result[index++] = device;
            result[index++] = subdevice;
            result[index++] = command;
            result[index++] = 0;

            return result;
        } else if (protocolName.equalsIgnoreCase("rc6")) {
            if (device > 255 || subdevice != (int) IrpUtils.invalid || command > 255)
                throw new IncompatibleArgumentException("Invalid parameters");

            int[] result = new int[6];
            result[index++] = rc6Code;
            result[index++] = rc6Frequency;
            result[index++] = 0;
            result[index++] = 1;
            result[index++] = device;
            result[index++] = command;

            return result;
        } else if (protocolName.equalsIgnoreCase("nec1")) {
            if (device > 255 || subdevice > 255 || command > 255)
                throw new IncompatibleArgumentException("Invalid parameters");

            int[] result = new int[6];
            result[index++] = nec1Code;
            result[index++] = nec1Frequency;
            result[index++] = 0;
            result[index++] = 1;
            result[index++] = (device << 8) + (subdevice != (int) IrpUtils.invalid ? subdevice : (0xff - device));
            result[index++] = (command << 8) + (0xff - command);

            return result;
        } else {
            return null;
        }
    }

    public static void main(String[] args) {
        try {
            IrSignal irSignal = null;

            if (args.length == 1) {
                irSignal = ccfSignal(args[0]);
            } else if (args[0].equals("-s")) {
                int index = 1;
                String protocol = args[index++];
                int device = Integer.parseInt(args[index++]);
                int subdevice = Integer.parseInt(args[index++]);
                int command = Integer.parseInt(args[index++]);
                System.out.println(shortCCFString(protocol, device, subdevice, command));
                System.exit(IrpUtils.exitSuccess);
            } else {
                int[] ccf = new int[args.length];
                for (int i = 0; i < args.length; i++) {
                    ccf[i] = Integer.parseInt(args[i], 16);
                }
                irSignal = ccfSignal(ccf);
            }
            System.out.println(irSignal);
        } catch (IrpMasterException ex) {
            System.err.println(ex.getMessage());
        }
    }
}
