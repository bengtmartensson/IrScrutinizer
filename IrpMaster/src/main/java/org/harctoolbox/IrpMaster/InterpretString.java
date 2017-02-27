/*
Copyright (C) 2012,2013,2014,2016 Bengt Martensson.

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

public class InterpretString {

    /**
     * Tries to interpret the string argument as one of our known formats, and return an IrSignal.
     * If the string starts with "[", interpret it as raw data, already split in intro-,
     * repeat-, and ending sequences.
     * If not try to interpret as Pronto Hex, or UEI learned.
     * If not successful, it is assumed to be in raw format.
     * If it contains more than on line, assume
     * that the caller has split it into intro, repeat, and ending sequences already.
     * Otherwise invoke ExchangeIR to split it into constituents.
     *
     * @param str String to be interpreted.
     * @param frequency Modulation frequency to use, if it cannot be inferred from the first parameter.
     * @param invokeRepeatFinder
     * @param invokeCleaner
     * @param absouluteTolerance
     * @param relativeTolerance
     * @return IrSignal, or null on failure.
     * @throws ParseException
     * @throws IncompatibleArgumentException
     * @throws UnassignedException
     * @throws DomainViolationException
     * @throws InvalidRepeatException
     */
    public static IrSignal interpretString(String str, double frequency, boolean invokeRepeatFinder, boolean invokeCleaner,
            double absouluteTolerance, double relativeTolerance) throws ParseException, IncompatibleArgumentException, UnassignedException, DomainViolationException, InvalidRepeatException {
        try {
            // Already in irSequences?
            if (str.trim().startsWith("[")) {
                String[] codes = str.trim().substring(1).split("[\\[\\]]+");
                return new IrSignal(frequency, IrpUtils.invalid, codes[0],
                        codes.length > 1 ? codes[1] : null, codes.length > 2 ? codes[2] : null);
            }

            // First try decoding as Pronto
            try {
                int[] array = Pronto.parseString(str);
                return array != null ? Pronto.ccfSignal(array) : UeiLearnedSignal.parseUeiLearned(str);
            } catch (IllegalArgumentException | IncompatibleArgumentException ex) {
                return interpretRawString(str, frequency, invokeRepeatFinder, invokeCleaner, absouluteTolerance, relativeTolerance);
            }
        } catch (NumberFormatException ex) {
            throw new ParseException("Could not interpret string " + str + " (" + ex.getMessage() + ")");
        }
    }

    public static IrSignal interpretString(String str, double frequency, boolean invokeRepeatFinder, boolean invokeCleaner)
            throws ParseException, IncompatibleArgumentException, UnassignedException, DomainViolationException, InvalidRepeatException {
        return interpretString(str, frequency, invokeRepeatFinder, invokeCleaner,
                IrpUtils.defaultAbsoluteTolerance, IrpUtils.defaultRelativeTolerance);
    }

    private static IrSignal interpretRawString(String str, double frequency, boolean invokeRepeatFinder, boolean invokeCleaner,
            double absoluteTolerance, double relativeTolerance) throws ParseException, IncompatibleArgumentException {
        try {
            // IRremote writes spaces after + and -, sigh...
            String fixedString = str.replaceAll("\\+\\s+", "+").replaceAll("-\\s+", "-");
            String[] codes = fixedString.split("[\n\r]+");
            if (codes.length > 1 && codes.length <= 3) // already decomposed in sequences?
                return new IrSignal(frequency, IrpUtils.invalid, codes[0], codes[1], codes.length > 2 ? codes[2] : null);

            IrSequence irSequence = new IrSequence(fixedString, true);
            ModulatedIrSequence modulatedIrSequence = new ModulatedIrSequence(irSequence, frequency, IrpUtils.unknownDutyCycle);
            return interpretIrSequence(modulatedIrSequence, invokeRepeatFinder, invokeCleaner, absoluteTolerance, relativeTolerance);
        } catch (NumberFormatException ex) {
            throw new ParseException("Could not interpret string " + str + " (" + ex.getMessage() + ")");
        }
    }

    /**
     * If invokeRepeatFinder is true, tries to identify intro, repeat, and ending applying a RepeatFinder.
     * If not, the sequence is used as intro on the returned signal.
     * In this case, if invokeCleaner is true, an analyzer is first used to clean the signal.
     * @param modulatedIrSequence
     * @param invokeRepeatFinder If the repeat finder is invoked. This also uses the analyzer.
     * @param absoluteTolerance
     * @param relativeTolerance
     * @param invokeCleaner If the analyzer is invoked for cleaning the signals.
     * @return IrSignal signal constructed according to rules above.
     */
    public static IrSignal interpretIrSequence(ModulatedIrSequence modulatedIrSequence, boolean invokeRepeatFinder, boolean invokeCleaner,
            double absoluteTolerance, double relativeTolerance) {
        // The analyzer misbehaves for zero length arguments, so treat this case separately.
        //if (irSequence == null || irSequence.getLength() == 0)
        //    return new IrSignal();

        ModulatedIrSequence cleaned = invokeCleaner ? Cleaner.clean(modulatedIrSequence, (int) RepeatFinder.getDefaultAbsoluteTolerance(),
                RepeatFinder.getDefaultRelativeTolerance()) : modulatedIrSequence;
        if (invokeRepeatFinder) {
            RepeatFinder repeatFinder = new RepeatFinder(modulatedIrSequence, absoluteTolerance, relativeTolerance);
            return repeatFinder.toIrSignal(cleaned);
        } else
            return cleaned.toIrSignal();
    }

    public static IrSignal interpretIrSequence(ModulatedIrSequence modulatedIrSequence, boolean invokeRepeatFinder, boolean invokeCleaner) {
        return interpretIrSequence(modulatedIrSequence, invokeRepeatFinder, invokeCleaner,
                IrpUtils.defaultAbsoluteTolerance, IrpUtils.defaultRelativeTolerance);
    }

    /**
     * By applying a RepeatFinder, tries to identify intro, repeat, and ending;
     * thus constructing an IrSignal.
     * @param data
     * @param frequency
     * @param invokeRepeatFinder
     * @param invokeCleaner
     * @param absoluteTolerance
     * @param relativeTolerance
     * @return IrSignal
     */
    public static IrSignal interpretIrSequence(int[] data, double frequency, boolean invokeRepeatFinder, boolean invokeCleaner,
            double absoluteTolerance, double relativeTolerance) {
        try {
            return interpretIrSequence(new ModulatedIrSequence(data, frequency, IrpUtils.unknownDutyCycle),
                    invokeRepeatFinder, invokeCleaner, absoluteTolerance, relativeTolerance);
        } catch (IncompatibleArgumentException ex) {
            return null;
        }
    }

    /**
     * By applying a RepeatFinder, tries to identify intro, repeat, and ending;
     * thus constructing an IrSignal.
     * @param data
     * @param frequency
     * @param invokeRepeatFinder
     * @param invokeCleaner
     * @return IrSignal
     */
    public static IrSignal interpretIrSequence(int[] data, double frequency, boolean invokeRepeatFinder, boolean invokeCleaner) {
        try {
            return interpretIrSequence(new ModulatedIrSequence(data, frequency, IrpUtils.unknownDutyCycle),
                    invokeRepeatFinder, invokeCleaner);
        } catch (IncompatibleArgumentException ex) {
            return null;
        }
    }
    /** Instantiating this class is meaningless. */
    private InterpretString() {}
}
