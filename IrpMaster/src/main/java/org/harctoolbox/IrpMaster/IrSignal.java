/*
Copyright (C) 2011,2012,2014 Bengt Martensson.

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

import java.io.FileNotFoundException;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;

/**
 * This class models a rendered IR signals.
 * It consists of a frequency, a duty cycle, and an intro sequence,
 * a repeat sequence, and an ending sequence. Any of the latter three,
 * but not all, can be empty, but not null.
 * <p>
 * The "count" semantic: The "count" argument in functions like toModulatedIrSequece(int count) is interpreted like this:
 * If the intro sequence is null, then "count" copies or the repeat sequence are used, otherwise count-1.
 * It is believed that this interpretation is consistent with the heuristic meaning of 'sending a signal "count" times'.
 *
 * <p>
 * The "repetitions" semantic: "repetition" number of copies of the repeat sequence are used.
 *
 * <p>This class is immutable.
 *
 * @see IrSequence
 *
 */
public class IrSignal {

    /** Intro sequence, always sent once. Can be empty, but not null. */
    protected IrSequence introSequence;

    /** Repeat sequence, sent after the intro sequence if the signal is repeating. Can be empty, but  not null. */
    protected IrSequence repeatSequence;

    /** Ending sequence, sent at the end of transmission. Can be empty (but not null),
     * actually, most often is. */
    protected IrSequence endingSequence;

    /** "Table" for mapping Pass to intro, repeat, or ending sequence. */
    protected EnumMap<Pass, IrSequence>map;

    /** Modulation frequency in Hz. Use 0 for not modulated. */
    protected double frequency;

    /** Duty cycle of the modulation. Between 0 and 1. Use -1 for not assigned. */
    protected double dutyCycle = (double) IrpUtils.invalid;

    public double getFrequency() {
        return frequency;
    }

    public double getDutyCycle() {
        return dutyCycle;
    }

    /**
     * Returns length of Intro sequence, in number of flashes and gaps.
     * @see IrSequence
     * @return length of intro
     */
    public int getIntroLength() {
        return introSequence.getLength();
    }

    /**
     * Returns number of burst pairs in the intro sequence.
     * @see IrSequence
     * @return number of burst pairs in intro sequence
     */
    public int getIntroBursts() {
        return introSequence.getNumberBursts();
    }

    /**
     * Returns the data in the intro sequence, as a sequence of microsecond durations.
     * @param alternatingSigns
     * @see IrSequence
     * @return integer sequence of durations in microseconds, possibly with sign.
     */
    public int[] getIntroInts(boolean alternatingSigns) {
        return introSequence.toInts(alternatingSigns);
    }

    /**
     * Returns the data in the intro sequence, as a sequence of pulses in the used frequency.
     * @see IrSequence
     * @return integer array of pulses
     */
    public int[] getIntroPulses() {
        return introSequence.toPulses(frequency);
    }

    /**
     * Returns the i'th data in the intro sequence, as double.
     * @see IrSequence
     * @param i index
     * @return duration, possibly with sign.
     */
    public double getIntroDouble(int i) {
        return introSequence.get(i);
    }

    public int getRepeatLength() {
        return repeatSequence.getLength();
    }

    public int getRepeatBursts() {
        return repeatSequence.getNumberBursts();
    }

    public int[] getRepeatInts(boolean alternatingSigns) {
        return repeatSequence.toInts(alternatingSigns);
    }

    public int[] getRepeatPulses() {
        return repeatSequence.toPulses(frequency);
    }

    public double getRepeatDouble(int i) {
        return repeatSequence.get(i);
    }

    public int getEndingLength() {
        return endingSequence.getLength();
    }

    public int getEndingBursts() {
        return endingSequence.getNumberBursts();
    }

    public int[] getEndingInts(boolean alternatingSigns) {
        return endingSequence.toInts(alternatingSigns);
    }

    public int[] getEndingPulses() {
        return endingSequence.toPulses(frequency);
    }

    public double getEndingDouble(int i) {
        return endingSequence.get(i);
    }

    /**
     * Computes the duration in microseconds of the intro sequence,
     * one repeat sequence, plus the ending sequence.
     * @return duration in microseconds.
     */
    public double getDuration() {
        return introSequence.getDuration() + repeatSequence.getDuration() + endingSequence.getDuration();
    }

    /**
     * Computes the duration in microseconds of the intro sequence,
     * repetitions repeats of the repeat sequence, plus the ending sequence.
     * Uses the count semantic.
     * @param count Uses count semantic.
     * @return duration in microseconds.
     */
    public double getDuration(int count) {
        return introSequence.getDuration() + repeatsPerCountSemantic(count)*repeatSequence.getDuration() + endingSequence.getDuration();
    }

    public double getDouble(Pass pass, int i) {
        return map.get(pass).get(i);
    }

    public int getLength(Pass pass) {
        return map.get(pass).getLength();
    }

    @Override
    public String toString() {
        return "Freq=" + Math.round(frequency) + "Hz " + introSequence + repeatSequence + endingSequence;
    }

    /**
     * Generates nice string. Generate alternating signs according to the parameter.
     *
     * @param alternatingSigns if true generate alternating signs, otherwise remove signs.
     * @return nice string.
     */
    public String toString(boolean alternatingSigns) {
        return "Freq=" + Math.round(frequency) + "Hz " + introSequence.toString(alternatingSigns)
                + repeatSequence.toString(alternatingSigns) + endingSequence.toString(alternatingSigns);
    }

    /**
     * Analog to the IrSequence toPrintString.
     * @param alternatingSigns If true, generated signs will have alternating signs, ignoring original signs, otherwise signs are preserved.
     * @return Nicely formatted string.
     *
     * @see IrSequence
     */
    public String toPrintString(boolean alternatingSigns) {
        return introSequence.toPrintString(alternatingSigns) + "\n"
                + repeatSequence.toPrintString(alternatingSigns)
                + (endingSequence.getLength() > 0 ? "\n" + endingSequence.toPrintString(alternatingSigns) : "");
    }

    public String toPrintString() {
        return toPrintString(false);
    }

    // helper function for the next
    private void append(int offset, int[] result, IrSequence seq) {
        for (int i = 0; i < seq.getLength(); i++)
            result[i+offset] = seq.iget(i);
    }

    /**
     * Returns the number of repetitions according to the count semantics.
     * @param count
     * @return introSequence.isEmpty() ? count : count - 1
     */
    public int repeatsPerCountSemantic(int count) {
        return introSequence.isEmpty() ? count : count - 1;
    }

    /**
     * Returns an integer array of one intro sequence, repeat number of repeat sequence, followed by one ending sequence.
     * The sizes can be obtained with the get*Length()- or get*Bursts()-functions.
     * @param repetitions Number of times of to include the repeat sequence.
     * @return integer array as copy.
     */
    public int[] toIntArray(int repetitions) {
        int[] result = new int[introSequence.getLength() + repetitions*repeatSequence.getLength() + endingSequence.getLength()];
        append(0, result, introSequence);
        for (int i = 0; i < repetitions; i++)
            append(introSequence.getLength() + i*repeatSequence.getLength(), result, repeatSequence);
        append(introSequence.getLength() + repetitions*repeatSequence.getLength(), result, endingSequence);

        return result;
    }

    /**
     * Equivalent to toIntArray(1)
     * @return
     */
    public int[] toIntArray() {
        return toIntArray(1);
    }

    /**
     * Returns an integer array of one intro sequence, count or count-1 number of repeat sequence, dependent on if intro is empty or not, followed by one ending sequence.
     * The sizes can be obtained with the get*Length()- or get*Bursts()-functions.
     * @param count Number of times of the "signal" to send, according to the count semantic.
     * @return integer array as copy.
     */
    public int[] toIntArrayCount(int count) {
        return toIntArray(repeatsPerCountSemantic(count));
    }

    /**
     *
     * @return Emptyness of the signal.
     */
    public boolean isEmpty() {
        return introSequence.isEmpty() && repeatSequence.isEmpty() && endingSequence.isEmpty();
    }

    /**
     * Returns true if and only if the sequence contains durations of zero length.
     * @return existence of zero durations.
     */
    public boolean containsZeros() {
        return introSequence.containsZeros() || repeatSequence.containsZeros() || endingSequence.containsZeros();
    }

    /**
     * Replace all zero durations. Changes the signal in-place.
     * @param replacement Duration in micro seconds to replace zero durations with.
     */
    public void replaceZeros(double replacement) {
        introSequence.replaceZeros(replacement);
        repeatSequence.replaceZeros(replacement);
        endingSequence.replaceZeros(replacement);
    }

    /**
     * Replace all zero durations. Changes the signal in-place.
     * @param replacement Duration in pulses to replace zero durations with. If frequency == 0, interpret as microseconds instead.
     */
    public void replaceZeros(int replacement) {
        replaceZeros(frequency > 0 ?  replacement / frequency * IrpUtils.seconds2microseconds : (double) replacement);
    }

    /**
     * Returns max gap of intro- and repeat sequences.
     * @return
     */
    public double getGap() {
        return Math.max(introSequence.getGap(), repeatSequence.getGap());
    }

    /**
     * Constructs an IrSignal from its arguments.
     * @param frequency
     * @param dutyCycle
     * @param introSequence
     * @param repeatSequence
     * @param endingSequence
     */
    public IrSignal(double frequency, double dutyCycle, IrSequence introSequence, IrSequence repeatSequence, IrSequence endingSequence) {
        this.frequency = frequency;
        this.dutyCycle = dutyCycle;
        // If the given intro sequence is identical to the repeat sequence, reject it.
        this.introSequence = ((introSequence != null) && !introSequence.isEqual(repeatSequence)) ? introSequence : new IrSequence();
        this.repeatSequence = repeatSequence != null ? repeatSequence : new IrSequence();
        this.endingSequence = ((endingSequence != null) && !endingSequence.isEqual(repeatSequence)) ? endingSequence : new IrSequence();

        map = new EnumMap<Pass, IrSequence>(Pass.class);

        map.put(Pass.intro, introSequence);
        map.put(Pass.repeat, repeatSequence);
        map.put(Pass.ending, endingSequence);
    }

    /**
     * Constructs an IrSignal from its arguments.
     *
     * @param durations
     * @param noIntroBursts
     * @param noRepeatBursts
     * @param frequency
     */
    public IrSignal(int[] durations, int noIntroBursts, int noRepeatBursts, int frequency) {
        this(durations, noIntroBursts, noRepeatBursts, frequency, (double) IrpUtils.invalid);
    }

    /**
     * Constructs an IrSignal from its arguments.
     *
     * @param frequency
     * @param dutyCycle
     * @param introSequence
     * @param repeatSequence
     * @param endingSequence
     * @throws IncompatibleArgumentException
     */
    public IrSignal(double frequency, double dutyCycle, String introSequence, String repeatSequence,
            String endingSequence) throws IncompatibleArgumentException {
        this(frequency, dutyCycle, new IrSequence(introSequence), new IrSequence(repeatSequence),
                new IrSequence(endingSequence));
    }

    /**
     * Constructs an IrSignal from its arguments.
     *
     * The first 2*noIntroBursts durations belong to the Intro signal,
     * the next 2*noRepeatBursts to the repetition part, and the remaining to the ending sequence.
     *
     * @param durations Integer array of durations. Signs of the entries are ignored,
     * @param noIntroBursts Number of bursts (half the number of entries) belonging to the intro sequence.
     * @param noRepeatBursts Number of bursts (half the number of entries) belonging to the intro sequence.
     * @param frequency Modulation frequency in Hz.
     * @param dutyCycle Duty cycle of modulation pulse, between 0 and 1. Use -1 for not specified.
     */
    public IrSignal(int[] durations, int noIntroBursts, int noRepeatBursts, int frequency, double dutyCycle) {
        this((double) frequency, dutyCycle,
                new IrSequence(durations, 0, 2*noIntroBursts),
                new IrSequence(durations, 2*noIntroBursts, 2*noRepeatBursts),
                new IrSequence(durations, 2*(noIntroBursts+noRepeatBursts), durations.length - 2*(noIntroBursts + noRepeatBursts)));
    }

    /**
     * Constructs an IrSignal of zero length.
     */
    public IrSignal() {
        this(new int[0], 0, 0, (int) IrpUtils.defaultFrequency);
    }

    // Plunders the victim. Therefore private, othewise would violate immutability.
    private void copyFrom(IrSignal victim) throws IrpMasterException {
        dutyCycle = victim.dutyCycle;
        frequency = victim.frequency;
        introSequence = victim.introSequence;
        repeatSequence = victim.repeatSequence;
        endingSequence = victim.endingSequence;
        map = victim.map;
    }

    /**
     * Creates an IrSignal from a CCF string. Also some "short formats" of CCF are recognized.
     * @see Pronto
     *
     * @param ccf String supposed to represent a valid CCF signal.
     * @throws IrpMasterException Error in CCF
     */
    public IrSignal(String ccf) throws IrpMasterException {
        copyFrom(Pronto.ccfSignal(ccf));
    }

    /**
     * Creates an IrSignal from a CCF array. Also some "short formats" of CCF are recognized.
     * @see Pronto
     *
     * @param ccf Integer array supposed to represent a valid CCF signal.
     * @throws IrpMasterException Error in CCF
     */
    public IrSignal(int[] ccf) throws IrpMasterException {
        copyFrom(Pronto.ccfSignal(ccf));
    }

    /**
     * Creates an IrSignal from a CCF array. Also some "short formats" of CCF are recognized.
     * @param begin starting index
     * @see Pronto
     *
     * @param ccf String array supposed to represent a valid CCF signal.
     * @throws IrpMasterException Error in CCF
     */
    public IrSignal(String[] ccf, int begin) throws IrpMasterException {
        copyFrom(Pronto.ccfSignal(ccf, begin));
    }

    /**
     * Intended to construct an IrSignal from the args of a main-routine,
     * for example for exporting. It can be used to decode the non-option arguments.
     * Either the arguments are hexadecimal numbers to be interpreted as a CCF type signal,
     * or it is expected to be a protocol name followed by a number of parameters.
     * The parameters can be given either as name=value pairs, and/or as unnamed parameters,
     * whereas one parameter is taken to be F, two parameters are taken to be D and F,
     * three parameters D, S, and F, while four parameters are interpreted as D, S, F, and T.
     *
     * @see IrpMaster
     * @see Protocol
     *
     * @param protocolsIniPath Path to Protocols.ini of the IrpMaster. There is no default.
     * @param offset How many initial elements of the argument vector to ignore.
     * @param args String array, typically the arguments of main.
     *
     * @throws IrpMasterException
     * @throws FileNotFoundException
     * @throws UnassignedException
     */
    public IrSignal(String protocolsIniPath, int offset, String... args) throws IrpMasterException, FileNotFoundException, UnassignedException {
        if (args == null || args.length - offset < 1)
            throw new IncompatibleArgumentException("Too few arguments");

        if (args[offset].matches("^[0-9]{3}[0-9a-fA-F]$")) {
            // This is a CCF
            int[] ccf = new int[args.length - offset];
            for (int i = 0; i < args.length - offset; i++)
                ccf[i] = Integer.parseInt(args[i], 16);
            copyFrom(Pronto.ccfSignal(ccf));
        } else {
            int arg_i = offset;
            String protocolName = args[arg_i++];
            LinkedHashMap<String, Long> parameters = new LinkedHashMap<String, Long>();

            // Parse name = value assignments
            while (arg_i < args.length && !args[arg_i].isEmpty() && args[arg_i].contains("=")) {
                String[] kv = args[arg_i++].split("=");
                if (kv.length != 2)
                    throw new IncompatibleArgumentException("Parse error by " + args[arg_i - 1]);
                parameters.put(kv[0], IrpUtils.parseLong(kv[1], true));
            }

            // Arguments left are shorthand assignments.
            switch (args.length - arg_i) {
                case 0:
                    break;
                case 1:
                    parameters.put("F", IrpUtils.parseLong(args[arg_i++], true));
                    break;
                case 2:
                    parameters.put("D", IrpUtils.parseLong(args[arg_i++], true));
                    parameters.put("F", IrpUtils.parseLong(args[arg_i++], true));
                    break;
                case 3:
                    parameters.put("D", IrpUtils.parseLong(args[arg_i++], true));
                    parameters.put("S", IrpUtils.parseLong(args[arg_i++], true));
                    parameters.put("F", IrpUtils.parseLong(args[arg_i++], true));
                    break;
                case 4:
                    parameters.put("D", IrpUtils.parseLong(args[arg_i++], true));
                    parameters.put("S", IrpUtils.parseLong(args[arg_i++], true));
                    parameters.put("F", IrpUtils.parseLong(args[arg_i++], true));
                    parameters.put("T", IrpUtils.parseLong(args[arg_i++], true));
                    break;
                default:
                    throw new IncompatibleArgumentException("Too many parameters.");
                    //break;
            }
            if (parameters.isEmpty())
                throw new IncompatibleArgumentException("No parameters given.");

            IrpMaster irpMaster = new IrpMaster(protocolsIniPath);
            Protocol protocol = irpMaster.newProtocol(protocolName);
            IrSignal irSignal = protocol.renderIrSignal(parameters);
            copyFrom(irSignal);
        }
    }

    /**
     * Convenience version of the constructor with an IrpMaster instance.
     * Equivalent to IrSignal(new IrpMaster(protocolsIniPath), protocolName, parameters).
     *
     * @param protocolsIniPath Path to IrpProtocols.ini
     * @param protocolName name of protocol
     * @param parameters Dictionary of parameter values
     * @throws FileNotFoundException
     * @throws IrpMasterException
     */
    public IrSignal(String protocolsIniPath, String protocolName, HashMap<String, Long> parameters) throws FileNotFoundException, IrpMasterException {
        this(new IrpMaster(protocolsIniPath), protocolName, parameters);
    }

    /**
     * Constructs an IrSignal from its arguments.
     * @param irpMaster
     * @param protocolName name of protocol
     * @param parameters Dictionary of parameter values
     * @throws IrpMasterException
     */
    public IrSignal(IrpMaster irpMaster, String protocolName, HashMap<String, Long> parameters) throws IrpMasterException {
        Protocol protocol = irpMaster.newProtocol(protocolName);
        if (protocol == null)
            throw new IrpMasterException("Protocol \"" + protocolName + "\" is not known.");

        IrSignal irSignal = protocol.renderIrSignal(parameters);
        copyFrom(irSignal);
    }

    /**
     * Constructs an IrSignal from its arguments.
     * @param protocolsIniPath Path to IrpProtocols.ini
     * @param protocolName name of protocol
     * @param parameters String of parameter assignments like "D=12 F=34"
     * @throws FileNotFoundException
     * @throws IrpMasterException
     */
    public IrSignal(String protocolsIniPath, String protocolName, String parameters)
            throws FileNotFoundException, IrpMasterException {
        this(protocolsIniPath, protocolName, Protocol.parseParams(parameters));
    }

    /**
     * Returns a ModulatedIrSequence consisting of one intro sequence,
     * count or count-1 number of repeat sequence, dependent on if intro is empty or not, followed by one ending sequence.
     * @param count Number of times to send signal. Must be > 0.
     * @return ModulatedIrSequence.
     */
    public ModulatedIrSequence toModulatedIrSequence(int count) {
        return toModulatedIrSequence(true, this.repeatsPerCountSemantic(count), true);
    }

    /**
     * Returns a ModulatedIrSequence consisting of zero or one intro sequence,
     * repetition number of repeat sequence, and zero or one ending sequence.
     * @param intro inclusion of intro sequence?
     * @param repetitions number of repetitions (repeat semantic)
     * @param ending inclusion of ending sequence.
     * @return ModulatedIrSequence.
     */
    public ModulatedIrSequence toModulatedIrSequence(boolean intro, int repetitions, boolean ending) {
        IrSequence seq1 = intro ? introSequence : new IrSequence();
        IrSequence seq2 = seq1.append(repeatSequence, repetitions);
        return new ModulatedIrSequence(ending ? seq2.append(endingSequence) : seq2, frequency, dutyCycle);
    }

    /**
     * Returns an IrSignal consisting of count repetitions (count semantic) as the intro sequence,
     * while repeat and ending are empty.
     * @param count Number of times to send signal. Must be > 0.
     * @return IrSignal consisting of count repetitions (count semantic) as the intro sequence.
     */
    public IrSignal toOneShot(int count) {
        return new IrSignal(frequency, dutyCycle, toModulatedIrSequence(count), null, null);
    }

    /**
     *
     * @return the intro sequence, as ModulatedIrSequence
     */
    public ModulatedIrSequence getIntroSequence() {
        return new ModulatedIrSequence(introSequence, frequency, dutyCycle);
    }

    /**
     *
     * @return the repeat sequence, as ModulatedIrSequence
     */
    public ModulatedIrSequence getRepeatSequence() {
        return new ModulatedIrSequence(repeatSequence, frequency, dutyCycle);
    }

    /**
     *
     * @return the ending sequence, as ModulatedIrSequence
     */
    public ModulatedIrSequence getEndingSequence() {
        return new ModulatedIrSequence(endingSequence, frequency, dutyCycle);
    }

    /**
     * Computes the CCF form, if possible. Since a CCF does not have an ending sequence,
     * a nonempty ending sequence will be ignored.
     * @see Pronto
     * @return CCF as string.
     * @throws IncompatibleArgumentException
     */
    public String ccfString() throws IncompatibleArgumentException {
        return Pronto.toPrintString(this);
    }

    /**
     * Just for testing. Invokes the IrSignal(String ProtocolsIniPath, int offset, String[] args)
     * and tests the result.
     *
     * @param args
     */
    public static void main(String[] args) {
        if (args.length == 0) {
            int times[] = {
                -9024, -4512, -564, -1692, +564, -564, +564, -564, +564, -564, +564, -564, +564, -564,
                +564, -564, +564, -564, +564, -564, +564, -1692, +564, -564, +564, -564, +564, -564, +564, -564,
                +564, -564, +564, -564, +564, -1692, +564, -1692, +564, -564, +564, -564, +564, -564, +564, -564,
                +564, -564, +564, -564, +564, -564, +564, -564, +564, -1692, +564, -1692, +564, -1692, +564, -1692,
                +564, -1692, +564, -1692, +564, -43992,
                +9024, -2256, +564, -97572};
            try {
                IrSignal irSignal = new IrSignal(times, 34, 2, 38400);
                System.out.println(irSignal.ccfString());
                System.out.println(irSignal.toString(true));
                System.out.println(irSignal.toString(false));
                System.out.println(irSignal);
            } catch (IncompatibleArgumentException ex) {
                System.err.println(ex.getMessage());
            }
        } else {
            String protocolsIni = "data/IrpProtocols.ini";
            int arg_i = 0;
            if (args[arg_i].equals("-c")) {
                arg_i++;
                protocolsIni = args[arg_i++];
            }
            try {
                IrSignal irSignal = new IrSignal(protocolsIni, arg_i, args);
                System.out.println(irSignal);
                System.out.println(irSignal.ccfString());
                DecodeIR.invoke(irSignal);
            } catch (IrpMasterException ex) {
                System.err.println(ex.getMessage());
            } catch (FileNotFoundException ex) {
                System.err.println(ex.getMessage());
            }
        }
    }
}
