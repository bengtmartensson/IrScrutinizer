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

    public static class RepeatFinderData {

        private int repeatStart;
        private int repeatLength;
        private int numberRepeats;
        private double repeatsDuration;

        private RepeatFinderData() {
            this(0, 0, 0);
        }

        public RepeatFinderData(int repeatStart, int repeatLength, int numberRepeats) {
            if (repeatStart % 2 != 0 || repeatLength % 2 != 0)
                throw new IllegalArgumentException("Lengths and start must be even");
            this.repeatStart = repeatStart;
            this.repeatLength = repeatLength;
            this.numberRepeats = numberRepeats;
            this.repeatsDuration = 0;
        }

        @Override
        public String toString() {
            return "repeatStart = " + repeatStart
                    + "; repeatLength = " + repeatLength
                    + "; numberRepeats = " + numberRepeats
                    + "; repeatsDuration = " + repeatsDuration;
        }

        /**
         * @return the repeatStart
         */
        public int getRepeatStart() {
            return repeatStart;
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

        public IrSignal chopIrSequence(ModulatedIrSequence irSequence) {
            try {
                return numberRepeats > 1
                        ? irSequence.toIrSignal(repeatStart, repeatLength, numberRepeats)
                        : // no repeat found, just do the trival
                        irSequence.toIrSignal();
            } catch (IncompatibleArgumentException ex) {
                assert(false); // cannot happen: repeatStart repeatLength have been checked to be even.
                return null;
            }
        }
    }

    private double relativeTolerance;
    private double absoluteTolerance;
    private ModulatedIrSequence irSequence;
    private RepeatFinderData repeatFinderData;

    public RepeatFinder(ModulatedIrSequence irSequence, double absoluteTolerance, double relativeTolerance) {
        this.absoluteTolerance = absoluteTolerance;
        this.relativeTolerance = relativeTolerance;
        this.irSequence = irSequence;
        analyze();
    }

    public RepeatFinder(ModulatedIrSequence irSequence) {
        this(irSequence, IrpUtils.defaultAbsoluteTolerance, IrpUtils.defaultRelativeTolerance);
    }

    public static IrSignal findRepeat(ModulatedIrSequence irSequence, double absoluteTolerance, double relativeTolerance) {
        RepeatFinder repeatFinder = new RepeatFinder(irSequence, absoluteTolerance, relativeTolerance);
        return repeatFinder.getIrSignal();
    }

    public static IrSignal findRepeat(ModulatedIrSequence irSequence) {
        return findRepeat(irSequence, IrpUtils.defaultAbsoluteTolerance, IrpUtils.defaultRelativeTolerance);
    }

    private void analyze() {
        RepeatFinderData candidate = new RepeatFinderData();
        for (int length = irSequence.getNumberBursts() / 2; length >= 2; length--) {
            for (int beginning = 0; beginning < irSequence.getNumberBursts() - length; beginning++) {
                RepeatFinderData newCandidate = countRepeats(2*beginning, 2*length);
                if (newCandidate.numberRepeats > 1 && newCandidate.repeatsDuration > candidate.repeatsDuration - 0.1)
                    candidate = newCandidate;
            }
        }
        repeatFinderData = candidate;
    }

    private RepeatFinderData countRepeats(int beginning, int length) {
        RepeatFinderData result = new RepeatFinderData(beginning, length, 0);
        for (int hits = 1;; hits++) {
            boolean hit = compareSubSequences(beginning, beginning + hits*length, length);
            if (!hit) {
                result.numberRepeats = hits;
                result.repeatsDuration = irSequence.getDuration(beginning, hits*length);
                return result;
            }
        }
    }

    private boolean compareSubSequences(int beginning, int compareStart, int length) {
        if (compareStart + length > irSequence.getLength())
            return false;

        return irSequence.isEqual(beginning, compareStart, length, absoluteTolerance, relativeTolerance);
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
     * @return the irSignal
     */
    public IrSignal getIrSignal() {
        return repeatFinderData.chopIrSequence(irSequence);
    }

    public RepeatFinderData getRepeatFinderData() {
        return repeatFinderData;
    }
}
