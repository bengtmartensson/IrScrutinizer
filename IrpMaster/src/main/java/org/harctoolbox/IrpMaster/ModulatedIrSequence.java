/*
Copyright (C) 2012, 2014 Bengt Martensson.

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

import java.util.Collection;

/**
 * A ModulatedIrSequence is an IrSequence with the additional properties of a modulation frequency and a duty cycle.
 * The name is slightly misleading since the modulation frequency can be 0; it just needs to be present.
 */
public class ModulatedIrSequence extends IrSequence {
    private static final long serialVersionUID = 1L;

    private static final double allowedFrequencyDeviation = 0.05;
    private static final double zeroModulationLimit = 0.000001;

    /** Modulation frequency in Hz. Use 0 for no modulation, use -1 for no information. */
    protected double frequency = 0;

    /** Duty cycle of the modulation, a number between 0 and 1. Use -1 for unassigned.*/
    protected double dutyCycle = (double) IrpUtils.invalid;

    /**
     *
     * @return modulation frequency in Hz.
     */
    public double getFrequency() {
        return frequency;
    }

    /**
     *
     * @return Duty cycle.
     */
    public double getDutyCycle() {
        return dutyCycle;
    }

    @Override
    public String toString() {
        return "{" + Integer.toString((int)Math.round(frequency)) + "," + super.toString() + "}";
    }

    private ModulatedIrSequence() {
    }

    /**
     * Constructs a ModulatedIrSequence from its arguments.
     *
     * @param irSequence irSequence to be copied from
     * @param frequency
     * @param dutyCycle
     */
    public ModulatedIrSequence(IrSequence irSequence, double frequency, double dutyCycle) {
        data = irSequence.data;
        this.frequency = frequency;
        this.dutyCycle = dutyCycle;
    }

    /**
     * Constructs a ModulatedIrSequence from its arguments.
     *
     * @param durations
     * @param frequency
     * @param dutyCycle
     * @throws IncompatibleArgumentException
     */
    public ModulatedIrSequence(int[] durations, double frequency, double dutyCycle) throws IncompatibleArgumentException {
        this(new IrSequence(durations), frequency, dutyCycle);
    }

    /**
     * Constructs a ModulatedIrSequence from its arguments.
     *
     * @param durations
     * @param frequency
     * @throws IncompatibleArgumentException if duration has odd length.
     */
    public ModulatedIrSequence(int[] durations, double frequency) throws IncompatibleArgumentException {
        this(new IrSequence(durations), frequency, (double) IrpUtils.invalid);
    }

    /**
     * Concatenates the IrSequences in the argument to a new sequence.
     * Frequency and duty cycle are set to the average between minimum and maximum values by the components, if it makes sense.
     * @param seqs One or more ModulatedIrSequences
     */
    public ModulatedIrSequence(ModulatedIrSequence... seqs) {
        int cumulatedLength = 0;
        double minf = Double.MAX_VALUE;
        double maxf = Double.MIN_VALUE;
        double mindc = Double.MAX_VALUE;
        double maxdc = Double.MIN_VALUE;
        for (ModulatedIrSequence seq : seqs) {
            minf = Math.min(minf, seq.frequency);
            maxf = Math.max(maxf, seq.frequency);
            mindc = Math.min(mindc, seq.frequency);
            maxdc = Math.max(maxdc, seq.frequency);
            cumulatedLength += seq.getLength();
        }

        dutyCycle = mindc > 0 ? (mindc + maxdc)/2 : (double) IrpUtils.invalid;
        frequency = minf > 0 ? (minf + maxf)/2 : 0;
        data = new double[cumulatedLength];
        int beginIndex = 0;
        for (ModulatedIrSequence seq : seqs) {
            System.arraycopy(seq.data, 0, data, beginIndex, seq.data.length);
            beginIndex += seq.data.length;
        }
    }

    /**
     * Formats IR signal as sequence of durations, with alternating signs, ignoring all signs, or by preserving signs.
     * @param alternatingSigns if true, generate alternating signs (ignoring original signs).
     * @param noSigns remove all signs.
     * @param separator
     * @return Printable string.
     */
    @Override
    public String toPrintString(boolean alternatingSigns, boolean noSigns, String separator) {
        return toPrintString(alternatingSigns, noSigns, separator, true);
    }
    
    /**
     * Formats IR signal as sequence of durations, with alternating signs, ignoring all signs, or by preserving signs.
     * @param alternatingSigns if true, generate alternating signs (ignoring original signs).
     * @param noSigns remove all signs.
     * @param separator
     * @param includeFrequency If true, include frequency information
     * @return Printable string.
     */
    public String toPrintString(boolean alternatingSigns, boolean noSigns, String separator, boolean includeFrequency) {
        return ((isEmpty() || !includeFrequency) ?  "" : ("f=" + Long.toString(Math.round(frequency))  + separator))
                + super.toPrintString(alternatingSigns, noSigns, separator);
    }

    /**
     * Formats IR signal as sequence of durations, with alternating signs or by preserving signs.
     * @param alternatingSigns if true, generate alternating signs (ignoring original signs), otherwise preserve signs.
     * @param noSigns
     * @return Printable string.
     */
    @Override
    public String toPrintString(boolean alternatingSigns, boolean noSigns) {
        return toPrintString(alternatingSigns, noSigns, " ");
    }

    /**
     * Formats IR signal as sequence of durations, with alternating signs or by preserving signs.
     * @param alternatingSigns if true, generate alternating signs (ignoring original signs), otherwise preserve signs.
     * @return Printable string.
     */
    @Override
    public String toPrintString(boolean alternatingSigns) {
        return toPrintString(alternatingSigns, false);
    }

    /**
     * Formats IR signal as sequence of durations, by preserving signs.
     * @return Printable string.
     */
    @Override
    public String toPrintString() {
        return toPrintString(false, false, " ");
    }

    /**
     * Makes the current sequence into an IrSignal by considering the sequence as an intro sequence.
     * @return IrSignal
     */
    public IrSignal toIrSignal() {
        return new IrSignal(frequency, dutyCycle, this, new IrSequence(), new IrSequence());
    }

    /**
     *
     * @return true if and only iff the modulation frequency is zero (in numerical sense).
     */
    public boolean isZeroModulated() {
        return frequency < zeroModulationLimit;
    }

    /**
     * Appends a delay to the end of the ModulatedIrSequence. Original is left untouched.
     * @param delay microseconds of silence to be appended to the IrSequence.
     * @return Copy of object with additional delay at end.
     * @throws IncompatibleArgumentException
     */
    @Override
    public ModulatedIrSequence append(double delay) throws IncompatibleArgumentException {
        IrSequence irSequence = ((IrSequence) this).append(delay);
        return new ModulatedIrSequence(irSequence, frequency, dutyCycle);
    }

    public ModulatedIrSequence append(ModulatedIrSequence tail) throws IncompatibleArgumentException {
        if (isZeroModulated() ? (! tail.isZeroModulated())
            : (Math.abs(frequency - tail.getFrequency())/frequency > allowedFrequencyDeviation))
            throw new IncompatibleArgumentException("concationation not possible; modulation frequencies differ");
        IrSequence irSequence = ((IrSequence) this).append(tail);
        return new ModulatedIrSequence(irSequence, frequency, dutyCycle);
    }

    public static ModulatedIrSequence concatenate(Collection<IrSequence> sequences, double frequency, double dutyCycle) {
        return new ModulatedIrSequence(IrSequence.concatenate(sequences), frequency, dutyCycle);
    }

    @Override
    public ModulatedIrSequence[] chop(double amount) {
        IrSequence[] irSequences = super.chop(amount);
        ModulatedIrSequence[] mods = new ModulatedIrSequence[irSequences.length];
        for (int i = 0; i < irSequences.length; i++)
            mods[i] = new ModulatedIrSequence(irSequences[i], frequency, dutyCycle);
        return mods;
    }
}
