/*
Copyright (C) 2011, 2012, 2014 Bengt Martensson.

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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;

/**
 * This class models an IR Sequence, which is a sequence of pulse pairs, often called "bursts".
 *
 * <p>Intrinsically, it "is" an array of doubles, each representing a gap
 * (for odd indices) or flash (for even indices).
 * The duration of each flash or gap is the absolute value of the entry in micro seconds.
 * Negative values are thus accepted, and preserved, but the sign ignored.
 * An application program is free to interpret the sign as it wishes.
 * The sign can therefore be viewed as "application specific information."
 *
 * <p>The length of the IrSequence (as reported by getLength()) is therefore always even.
 * To get the "length in bursts", divide by 2.
 *
 * <p>The class is (almost) immutable: it can only be constructed and then read from.
 * Also note that there is no modulation frequency herein.
 *
 * @see ModulatedIrSequence
 *
 */
public class IrSequence implements Cloneable, Serializable {
    private static final long serialVersionUID = 1L;
    /**
     * Duration data, possibly with signs, which are ignored (by this class).
     */
    protected double[] data;
    private static final double epsilon = 0.001;
    private static final int dummySpaceDuration = 50; // should not translate to 0000 in Pronto

    /**
     * Returns the i'th value, a duration in micro seconds.
     * If i is even, it is a flash (light on), if i is odd, a gal (light off).
     * @param i index
     * @return duration in microseconds, possibly with sign,
     */
    public double get(int i) {
        return data[i];
    }

    /**
     * Returns the i'th value, the duration in micro seconds, rounded to integer.
     * @param i index
     * @return duration in microseconds, possibly with sign.
     */
    public int iget(int i) {
        return (int) Math.round(Math.abs(data[i]));
    }

    /**
     * Returns an array of integers of durations.
     * This is a copy of the original data, so it might be manipulated without affecting the original instance.
     * @return integer array of durations in micro seconds, all positive.
     */
    public int[] toInts() {
        return toInts(false);
    }

    /**
     * Returns an array of integers of durations.
     * This is a copy of the original data, so it might be manipulated without affecting the original instance.
     * @param alternatingSigns if true, all the durations with odd index are negative, otherwise, all are positive.
     * @return integer array of durations in micro seconds.
     */
    public int[] toInts(boolean alternatingSigns) {
        int[] array = new int[data.length];
        for (int i = 0; i < data.length; i++) {
            int duration = (int) Math.round(Math.abs(data[i]));
            array[i] = (alternatingSigns && (i % 2 != 0)) ? -duration : duration;
        }

        return array;
    }

    /**
     * Returns an array of doubles of durations.
     * This is a copy of the original data, so it might be manipulated without affecting the original instance.
     * @return double array of durations in micro seconds.
     */
    public double[] toDoubles() {
        return data.clone();
    }

    /**
     * For the frequency given as argument, computes an array of durations in number of periods in the given frequency.
     * @param frequency Frequency in Hz.
     * @return integer array of durations in periods of frequency.
     */
    public int[] toPulses(double frequency) {
        int[] array = new int[data.length];
        for (int i = 0; i < data.length; i++)
            array[i] = (int) Math.round(Math.abs(frequency*data[i]/1000000.0));

        return array;
    }

    /**
     * Constructs an empty IrSequence,
     */
    public IrSequence() {
        data = new double[0];
    }

    /**
     * Constructs an IrSequence from the parameter data.
     * @param data Array of durations. Is copied.
     * @throws IncompatibleArgumentException If data is not of even length.
     */
    public IrSequence(double[] data) throws IncompatibleArgumentException {
        if (data.length % 2 != 0)
            throw new IncompatibleArgumentException("IrSequence has odd length = " + data.length);
        this.data = data.clone();
    }

    /**
     * Constructs an IrSequence from the parameter data.
     * @param idata Array of durations. Data is copied.
     * @throws IncompatibleArgumentException If data is not of even length.
     */
    public IrSequence(int[] idata) throws IncompatibleArgumentException {
        this(idata, false);
    }

    /**
     * Constructs an IrSequence from the parameter data.
     * @param idata Array of durations. Data is copied.
     * @param acceptOdd If odd length: if true, force length even by ignoring the last, otherwise throw exception.
     * @throws IncompatibleArgumentException If data is not of even length.
     */
    public IrSequence(int[] idata, boolean acceptOdd) throws IncompatibleArgumentException {
        int length = idata.length;
        if (length % 2 != 0) {
            if (acceptOdd)
                length--;
            else
                throw new IncompatibleArgumentException("IrSequence has odd length = " + idata.length);
        }

        data = new double[length];
        for (int i = 0; i < length; i++) {
            data[i] = (double) idata[i];
        }
    }

    /**
     * Constructs an IrSequence from the parameter data.
     * This version does not require flashes and gaps to be interleaved (signs alternating).
     * @param str String of durations, possibly using signed numbers.
     * @param fixOddSequences it true, odd sequences (ending with space) are silently fixed.
     * @throws IncompatibleArgumentException If last duration is not a gap, and fixOddSequences false.
     */
    public IrSequence(String str, boolean fixOddSequences) throws IncompatibleArgumentException {
        if (str == null || str.trim().isEmpty()) {
            data = new double[0];
        } else {
            String[] strings = str.trim().split("[\\s,;]+");
            if (strings.length == 1)
                // Instead, try to break at "+" and "-" chars, preserving these
                strings = str.trim().split("(?=\\+)|(?=-)");
            boolean hasAlternatingSigns = false;
            for (String string : strings) {
                if (string.startsWith("-")) {
                    hasAlternatingSigns = true;
                    break;
                }
            }

            int[] tmplist = new int[strings.length + 1];
            int index = -1;
            for (String string : strings) {
                if (string.isEmpty())
                    continue; // be on the safe side
                int x = Integer.parseInt(string.replaceFirst("\\+", ""));
                if (index == -1 && x < 0)
                    continue; // Ignore silly leading gaps

                if (hasAlternatingSigns && index >= 0 && sameSign(tmplist[index], x))
                    tmplist[index] += x;
                else
                    tmplist[++index] = x;
            }
            if (index % 2 == 0) {
                if (fixOddSequences)
                    tmplist[++index] = dummySpaceDuration;
                else
                    throw new IncompatibleArgumentException("IrSequence ends with a space");
            }
            data = new double[index+1];
            for (int i = 0; i < index+1; i++)
                data[i] = (double) tmplist[i];
        }
    }

    /**
     * Constructs an IrSequence from the parameter data.
     * This version does not require flashes and gaps to be interleaved (signs alternating).
     * @param str String of durations, possibly using signed numbers.
     * @throws IncompatibleArgumentException If last duration is not a gap.
     */
    public IrSequence(String str) throws IncompatibleArgumentException {
        this(str, false);
    }

    private boolean sameSign(int x, int y) {
        return (x < 0) == (y < 0);
    }

    /**
     * Constructs an IrSequence from the parameter data.
     * @param idata Data
     * @param offset First index to be used
     * @param length Length of used subset of the idata array.
     */
    // @throws RuntimeException If data is contradictory or erroneous.
    public IrSequence(int[] idata, int offset, int length) /*throws IncompatibleArgumentException*/ {
        if (length % 2 != 0)
            throw new RuntimeException("IrSequence has odd length = " + length);
        if (offset >= idata.length && length != 0)
            throw new RuntimeException("IrSequence: offset beyond end.");
        if (offset + length > idata.length)
            throw new RuntimeException("IrSequence: length too large.");

        data = new double[length];
        for (int i = 0; i < length; i++)
            data[i] = (double) idata[i+offset];
    }

    /**
     * Constructs an IrSequence from the parameter data.
     * The argument is supposed to use positive sign for flashes, and negative sign for gaps.
     * Leading gaps are discarded, while meaningless.
     * Consecutive gaps (flashes) are combined into one gap (flash).
     * @param list List of durations as Double, containing signs.
     * @throws IncompatibleArgumentException If data ens with a flash, not a gap.
     */
    public IrSequence(ArrayList<Double>list) throws IncompatibleArgumentException {
        ArrayList<Double> normalized = normalize(list, true);
        if (normalized.size() % 2 != 0)
            throw new IncompatibleArgumentException("IrSequence cannot end with a flash.");
        data = new double[normalized.size()];
        for (int i = 0; i < normalized.size(); i++)
            data[i] = normalized.get(i);
    }

    /**
     * Constructs an IrSequence from input parameter.
     * @param irStream
     * @throws IncompatibleArgumentException
     * @throws UnassignedException
     */
    public IrSequence(PrimaryIrStream irStream) throws IncompatibleArgumentException, UnassignedException {
        this(irStream.toIrSequence());
    }

    /**
     * Constructs an IrSequence from the parameter data, by cloning.
     * @param src Original
     */
    public IrSequence(IrSequence src) {
        data = src.data.clone();
    }

    /**
     * Concatenates the elements in the argument.
     * @param sequences
     * @return new IrSequence
     */
    public static IrSequence concatenate(Collection<IrSequence> sequences) {
        IrSequence s = new IrSequence();
        for (IrSequence seq : sequences) {
            s = s.append(seq);
        }
        return s;
    }

    /**
     * Returns an IrSequence consisting of this sequence, with repetitions
     * copies of the first argument appended.
     * @param tail IrSequence to be appended.
     * @param repetitions Number of copies to append.
     * @return new IrSequence
     */
    public IrSequence append(IrSequence tail, int repetitions) {
        double[] newData = new double[data.length + repetitions*tail.data.length];
        System.arraycopy(data, 0, newData, 0, data.length);
        for (int r = 0; r < repetitions; r++)
            System.arraycopy(tail.data, 0, newData, data.length + r*tail.data.length, tail.data.length);
        IrSequence irSequence = new IrSequence();
        irSequence.data = newData;
        return irSequence;
    }

     /**
     * Returns an IrSequence consisting of this sequence, with one
     * copy of the argument appended.
     * @param tail IrSequence to be appended.
     * @return new IrSequence
     */
    public IrSequence append(IrSequence tail) {
        return append(tail, 1);
    }

    /**
     * Appends a delay to the end of the IrSequence. Original is left untouched.
     * @param delay microseconds of silence to be appended to the IrSequence.
     * @return Copy of object with additional delay at end.
     * @throws IncompatibleArgumentException
     */
    public IrSequence append(double delay) throws IncompatibleArgumentException {
        if (data.length == 0)
            throw new IncompatibleArgumentException("IrSequence is empty");
        IrSequence irSequence = new IrSequence(this);
        irSequence.data[data.length-1] = -(Math.abs(data[data.length-1]) + Math.abs(delay));
        return irSequence;
    }

    /**
     * Creates and returns a copy of this object.
     * @return A copy of this IrSequence.
     * @throws java.lang.CloneNotSupportedException
     */
    @Override
    public IrSequence clone() throws CloneNotSupportedException {
        super.clone();
        return new IrSequence(this);
    }

    /**
     * Chops a IrSequence in parts. Every gap of length >= threshold cause a cut.
     * @param threshold minimal gap in microseconds to cause a cut.
     * @return Array of IrSequences
     */
    public IrSequence[] chop(double threshold) {
        ArrayList<IrSequence> arrayList = new ArrayList<IrSequence>();
        int beg = 0;
        for (int i = 1; i < data.length; i += 2) {
            if (data[i] >= threshold || i == data.length - 1) {
                double[] arr = new double[i - beg + 1];
                System.arraycopy(data, beg, arr, 0, i - beg + 1);
                try {
                    arrayList.add(new IrSequence(arr));
                } catch (IncompatibleArgumentException ex) {
                    assert(false);
                }
                beg = i + 1;
            }
        }
        return arrayList.toArray(new IrSequence[arrayList.size()]);
    }

    /**
     * Adds an amount to all flashes.
     *
     * @param amount Amount to add in microsecods.
     * @return New instance
     */
    public IrSequence addToFlashes(double amount) {
        IrSequence clone;
        try {
            clone = clone();
        } catch (CloneNotSupportedException ex) {
            return null;
        }
        for (int i = 0; i < data.length; i += 2) {
            clone.data[i] += clone.data[i] > 0 ? amount : -amount;
        }
        return clone;
    }

    /**
     * Adds an amount to all flashes, and subtract it from all gaps. For
     * generating test data for decoders etc.
     *
     * @param amount Amount (positive or negative) to add in microseconds.
     * @return new instance
     */
    public IrSequence addToGaps(double amount) {
        IrSequence clone;
        try {
            clone = clone();
        } catch (CloneNotSupportedException ex) {
            return null;
        }
        for (int i = 1; i < data.length; i += 2) {
            clone.data[i] += clone.data[i] > 0 ? amount : -amount;
        }
        return clone;
    }

    /**
     * Adds an amount to all flashes, and subtract it from all gaps. For
     * generating test data for decoders etc.
     *
     * @param amount Amount (positive or negative) to add in microseconds.
     * @return new instance
     */
    public IrSequence flashExcess(double amount) {
        return addToFlashes(amount).addToGaps(-amount);
    }

    /**
     * Adds a random number in the interval [-max, max) to each flash, and
     * subtract it from the next gap. For generating test data for decoders etc.
     *
     * @param max max amount to add/subtract, in microseconds.
     * @return new instance
     */
    public IrSequence noisify(double max) {
        IrSequence clone;
        try {
            clone = clone();
        } catch (CloneNotSupportedException ex) {
            return null;
        }
        for (int i = 0; i < data.length; i += 2) {
            double t = max * (2 * Math.random() - 1);
            clone.data[i] += t;
            clone.data[i + 1] -= t;
        }
        return clone;
    }

    /**
     * Compares two IrSequences for equality.
     * @param irSequence to be compared against this.
     * @return equality
     */
    public boolean isEqual(IrSequence irSequence) {
        if (irSequence == null || (data.length != irSequence.data.length))
            return false;

        for (int i = 0; i < data.length; i++)
            if (Math.abs(data[i] - irSequence.data[i]) > epsilon)
                return false;

        return true;
    }

    /**
     * Compares two IrSequences for equality.
     *
     * @param irSequence to be compared against this.
     * @param tolerance tolerance threshold in microseconds.
     * @return equality within tolerance.
     */
    public boolean isEqual(IrSequence irSequence, double tolerance) {
        if (irSequence == null || (data.length != irSequence.data.length))
            return false;

        for (int i = 0; i < data.length; i++)
            if (Math.abs(data[i] - irSequence.data[i]) > tolerance)
                return false;

        return true;
    }

    private static ArrayList<Double> normalize(ArrayList<Double> list, boolean nukeLeadingZeros) {
        if (list == null || list.isEmpty())
            return list;

        // Nuke leading gaps
        while (nukeLeadingZeros && list.size() > 1 && list.get(0) <= 0)
            list.remove(0);

        for (int i = 0; i < list.size(); i++) {
            while (i + 1 < list.size() && equalSign(list.get(i), list.get(i+1))) {
                double val = list.get(i) + list.get(i+1);
                list.remove(i);
                list.remove(i);
                list.add(i, val);
            }
        }
        return list;
    }

    private static boolean equalSign(double x, double y) {
        return x <= 0 && y <= 0 || x >= 0 && y >= 0;
    }

    /**
     * Returns the number of gaps and flashes. Always even.
     * Divide by 2 to get number of bursts.
     * @return number of gaps/flashes.
     */
    public int getLength() {
        return data.length;
    }

    /**
     * Return last entry, of -1 of empty.
     * @return
     */
    public double getGap() {
        return data.length > 0 ? Math.abs(data[data.length - 1]) : -1.0;
    }

    /**
     *
     * @return emptyness of the sequence.
     */
    public boolean isEmpty() {
        return data.length == 0;
    }

    /**
     * Returns true if and only if the sequence contains durations of zero length.
     * @return existence of zero durations.
     */
    public boolean containsZeros() {
        for (double t : data)
            if (Math.abs(t) < epsilon)
                return true;
        return false;
    }

    /**
     * Replace all zero durations. Changes the signal in-place.
     * @param replacement Duration in micro seconds to replace zero durations with.
     * @return if the signal was changed.
     */
    public boolean replaceZeros(double replacement) {
        boolean wasChanged = false;
        for (int i = 0; i < data.length; i++)
            if (Math.abs(data[i]) < epsilon) {
                data[i] = replacement;
                wasChanged = true;
            }
        return wasChanged;
    }

    /**
     * Returns the number of bursts, half of the length.
     * @return number bursts.
     */
    public int getNumberBursts() {
        return data.length/2;
    }

    /**
     * Computes the total duration of the IR sequence modeled.
     *
     * @return Length of the IR sequence in microseconds.
     */
    public double getDuration() {
        return IrpUtils.l1Norm(data);
    }

    /**
     * Formats IR signal as sequence of durations, with alternating signs, ignoring all signs, or by preserving signs.
     * @param alternatingSigns if true, generate alternating signs (ignoring original signs).
     * @param noSigns remove all signs.
     * @param separator
     * @return Printable string.
     */
    public String toPrintString(boolean alternatingSigns, boolean noSigns, String separator) {
        StringBuilder s = new StringBuilder();
        if (alternatingSigns) {
            for (int i = 0; i < data.length; i++) {
                int x = (int) Math.abs(Math.round(data[i]));
                s.append(String.format((i % 2 == 0) ? (i > 0 ? (separator + "+%d") : "+%d") : (separator + "-%d"), x));
            }
        } else if (noSigns) {
            for (int i = 0; i < data.length; i++) {
                int x = (int) Math.abs(Math.round(data[i]));
                s.append(String.format((i % 2 == 0) ? (i > 0 ? (separator + "%d") : "%d") : (separator + "%d"), x));
            }
        } else {
            for (int i = 0; i < data.length; i++) {
                s.append(String.format((i > 0 ? (separator + "%d") : "%d"), (int) Math.round(data[i])));
            }
        }
        return s.toString();
    }

    /**
     * Formats IR signal as sequence of durations, with alternating signs or by preserving signs.
     * @param alternatingSigns if true, generate alternating signs (ignoring original signs), otherwise preserve signs.
     * @param noSigns
     * @return Printable string.
     */
    public String toPrintString(boolean alternatingSigns, boolean noSigns) {
        return toPrintString(alternatingSigns, noSigns, " ");
    }

    /**
     * Formats IR signal as sequence of durations, with alternating signs or by preserving signs.
     * @param alternatingSigns if true, generate alternating signs (ignoring original signs), otherwise preserve signs.
     * @return Printable string.
     */
    public String toPrintString(boolean alternatingSigns) {
        return toPrintString(alternatingSigns, false);
    }

    /**
     * Formats IR signal as sequence of durations, by preserving signs.
     * @return Printable string.
     */
    public String toPrintString() {
        return toPrintString(false, false, " ");
    }

    /**
     * Generates a pretty string representing the object. Signs in the data are preserved.
     * @return nice string.
     */
    @Override
    public String toString() {
        if (data.length == 0)
            return "[]";
        StringBuilder result = new StringBuilder();
        result.append("[").append(Math.round(data[0]));
        for (int i = 1; i < data.length; i++)
            result.append(",").append(Math.round(data[i]));

        return result.append("]").toString();
    }

    /**
     * Generates a pretty string representing the object. If argument true, generate string with alternating signs,
     * otherwise remove signs. To preserve signs, use toString() instead.
     * @param alternatingSigns if true, generate alternating signs, otherwise remove signs.
     * @return nice string.
     */
    public String toString(boolean alternatingSigns) {
        if (data.length == 0)
            return "[]";
        StringBuilder result = new StringBuilder();
        result.append("[").append(Math.round(Math.abs(data[0])));
        for (int i = 1; i < data.length; i++)
            result.append(",").append((alternatingSigns && (i % 2 != 0) ? "-" : "")).append(Math.round(Math.abs(data[i])));

        return result.append("]").toString();
    }

    /**
     * Just for testing.
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        try {
            IrSignal irSignal = new IrSignal("0000 006C 0022 0002 015B 00AD 0016 0016 0016 0016 0016 0041 0016 0041 0016 0016 0016 0016 0016 0016 0016 0016 0016 0016 0016 0041 0016 0016 0016 0016 0016 0016 0016 0041 0016 0016 0016 0016 0016 0016 0016 0016 0016 0016 0016 0041 0016 0041 0016 0041 0016 0016 0016 0016 0016 0041 0016 0041 0016 0041 0016 0016 0016 0016 0016 0016 0016 0041 0016 0041 0016 06A4 015B 0057 0016 0E6C");
            IrSequence irSequence = irSignal.toModulatedIrSequence(3).noisify(10);
            IrSequence[] seqs = irSequence.chop(25000);
            for (IrSequence irs : seqs)
                System.out.println(irs);
        } catch (IrpMasterException ex) {
            System.err.println(ex);
        }
        double[] d = {1, 2, 3, -4};
        String raw = " +1266 -426 +1266 -426 +422 -1270 +1266 -426 +1266 -426 +422 -1270 +422 -1270 +422 -1270 +422 -1270 +422 -1270 +422 -1270 +1266 -7096 +1266 -426 +1266 -426 +422 -1270 +1266 -426 +1266 -426 +422 -1270 +422 -1270 +422 -1270 +422 -1270 +422 -1270  +422 -1270 +1266 -7096   ";
        try {
            IrSequence irs = new IrSequence(d);
            System.out.println(irs);
            irs = new IrSequence(raw);
            System.out.println(irs);
            System.out.println(new IrSequence());
        } catch (IncompatibleArgumentException e) {
        }
    }
}
