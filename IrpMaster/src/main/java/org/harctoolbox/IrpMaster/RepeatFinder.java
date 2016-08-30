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

package org.harctoolbox.IrpMaster;

/**
 *
 */
public class RepeatFinder {

    private static double defaultMinRepeatLastGap = IrpUtils.defaultMinRepeatLastGap; // 20 milli seconds minimum for a repetition
    private static double defaultRelativeTolerance = IrpUtils.defaultRelativeTolerance;
    private static double defaultAbsoluteTolerance = IrpUtils.defaultAbsoluteTolerance;

    /**
     * @return the defaultMinRepeatLastGap
     */
    public static double getDefaultMinRepeatLastGap() {
        return defaultMinRepeatLastGap;
    }

    /**
     * @param aDefaultMinRepeatLastGap the defaultMinRepeatLastGap to set
     */
    public static void setDefaultMinRepeatLastGap(double aDefaultMinRepeatLastGap) {
        defaultMinRepeatLastGap = aDefaultMinRepeatLastGap;
    }

    /**
     * @return the defaultRelativeTolerance
     */
    public static double getDefaultRelativeTolerance() {
        return defaultRelativeTolerance;
    }

    /**
     * @param aDefaultRelativeTolerance the defaultRelativeTolerance to set
     */
    public static void setDefaultRelativeTolerance(double aDefaultRelativeTolerance) {
        defaultRelativeTolerance = aDefaultRelativeTolerance;
    }

    /**
     * @return the defaultabsoluteTolerance
     */
    public static double getDefaultAbsoluteTolerance() {
        return defaultAbsoluteTolerance;
    }

    /**
     * @param aDefaultAbsoluteTolerance the defaultAbsoluteTolerance to set
     */
    public static void setDefaultAbsoluteTolerance(double aDefaultAbsoluteTolerance) {
        defaultAbsoluteTolerance = aDefaultAbsoluteTolerance;
    }

    public static class RepeatFinderData {
        private int beginLength;
        private int repeatLength;
        private int numberRepeats;
        private int endingLength;
        private double lastGap;
        private double repeatsDuration;

        private RepeatFinderData() {
            this(0, 0, 0, 0);
        }

        public RepeatFinderData(int beginLength, int repeatLength, int numberRepeats, int endingLength) {
            if (beginLength % 2 != 0 || repeatLength % 2 != 0 || endingLength % 2 != 0)
                throw new IllegalArgumentException("Lengths and start must be even");
            this.beginLength = beginLength;
            this.repeatLength = repeatLength;
            this.numberRepeats = numberRepeats;
            this.endingLength = endingLength;
            this.lastGap = 0;
            this.repeatsDuration = 0;
        }

        @Override
        public String toString() {
            return "beginLength = " + beginLength
                    + "; repeatLength = " + repeatLength
                    + "; numberRepeats = " + numberRepeats
                    + "; endingLength = " + endingLength
                    + "; repeatsDuration = " + repeatsDuration;
        }

        /**
         * @return the repeatStart
         */
        public int getBeginLength() {
            return beginLength;
        }

        /**
         * @return the numberRepeats
         */
        public int getNumberRepeats() {
            return numberRepeats;
        }

        /**
         * @return the repeatLength
         */
        public int getRepeatLength() {
            return repeatLength;
        }

        public int getEndingLength() {
            return endingLength;
        }

        public IrSignal chopIrSequence(ModulatedIrSequence irSequence) {
            try {
                return numberRepeats > 1
                        ? irSequence.toIrSignal(beginLength, repeatLength, numberRepeats)
                        : // no repeat found, just do the trival
                        irSequence.toIrSignal();
            } catch (IncompatibleArgumentException ex) {
                throw new InternalError(); // cannot happen: repeatStart repeatLength have been checked to be even.
            }
        }
    }

    private double relativeTolerance;
    private double absoluteTolerance;
    private double minRepeatLastGap;
    private IrSequence irSequence;
    private RepeatFinderData repeatFinderData;

    public RepeatFinder(IrSequence irSequence, double absoluteTolerance, double relativeTolerance) {
        this.absoluteTolerance = absoluteTolerance;
        this.relativeTolerance = relativeTolerance;
        this.minRepeatLastGap = defaultMinRepeatLastGap;
        this.irSequence = irSequence;
        analyze();
    }

    public RepeatFinder(IrSequence irSequence) {
        this(irSequence, defaultAbsoluteTolerance, defaultRelativeTolerance);
    }

    public static IrSignal findRepeat(ModulatedIrSequence irSequence, double absoluteTolerance, double relativeTolerance) {
        RepeatFinder repeatFinder = new RepeatFinder(irSequence, absoluteTolerance, relativeTolerance);
        return repeatFinder.toIrSignal(irSequence);
    }

    public static IrSignal findRepeat(ModulatedIrSequence irSequence) {
        return findRepeat(irSequence, defaultAbsoluteTolerance, defaultRelativeTolerance);
    }

    public static IrSignal findRepeatClean(ModulatedIrSequence irSequence, double absoluteTolerance, double relativeTolerance) {
        RepeatFinder repeatFinder = new RepeatFinder(irSequence, absoluteTolerance, relativeTolerance);
        return repeatFinder.toIrSignalClean(irSequence);
    }

    public static IrSignal findRepeatClean(ModulatedIrSequence irSequence) {
        return findRepeatClean(irSequence, defaultAbsoluteTolerance, defaultRelativeTolerance);
    }

    private void analyze() {
        RepeatFinderData candidate = new RepeatFinderData();
        for (int length = irSequence.getNumberBursts() / 2; length >= 2; length--) {
            for (int beginning = 0; beginning < irSequence.getNumberBursts() - length; beginning++) {
                RepeatFinderData newCandidate = countRepeats(2*beginning, 2*length);
                if (newCandidate.numberRepeats > 1
                        && newCandidate.lastGap > minRepeatLastGap
                        && newCandidate.repeatsDuration > candidate.repeatsDuration - 0.1)
                    candidate = newCandidate;
            }
        }
        repeatFinderData = candidate;
    }

    private RepeatFinderData countRepeats(int beginning, int length) {
        RepeatFinderData result = new RepeatFinderData(beginning, length, 0, 0);
        result.lastGap = Math.abs(irSequence.get(beginning + length - 1));
        if (result.lastGap < minRepeatLastGap)
            return result; // will be rejected anyhow, save some computations
        for (int hits = 1;; hits++) {
            boolean hit = compareSubSequences(beginning, beginning + hits*length, length);
            if (!hit) {
                result.numberRepeats = hits;
                result.endingLength = irSequence.getLength() - beginning - hits*length;
                result.repeatsDuration = irSequence.getDuration(beginning, hits*length);
                return result;
            }
        }
    }

    private boolean compareSubSequences(int beginning, int compareStart, int length) {
        if (compareStart + length > irSequence.getLength())
            return false;

        return irSequence.isEqual(beginning, compareStart, length, absoluteTolerance, relativeTolerance, minRepeatLastGap);
    }

    /**
     * @return the relativeTolerance
     */
    public double getRelativeTolerance() {
        return relativeTolerance;
    }

    /**
     * @return the absoluteTolerance
     */
    public double getAbsoluteTolerance() {
        return absoluteTolerance;
    }

    /**
     * @return the irSequence
     */
    public IrSequence getIrSequence() {
        return irSequence;
    }

    /**
     * @param irSequence
     * @return the irSignal
     */
    public IrSignal toIrSignal(ModulatedIrSequence irSequence) {
        return repeatFinderData.chopIrSequence(irSequence);
    }

    public IrSignal toIrSignalClean(ModulatedIrSequence irSequence) {
        return repeatFinderData.chopIrSequence(Cleaner.clean(irSequence, (int) absoluteTolerance, relativeTolerance));
    }

    public RepeatFinderData getRepeatFinderData() {
        return repeatFinderData;
    }
}
