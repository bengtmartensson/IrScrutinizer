/*
Copyright (C) 2012,2013,2014 Bengt Martensson.

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

    /** Instantiating this class is meaningless. */
    private InterpretString() {}

    /**
     * Constructs a RepeatFinder from an IrSequence.
     *
     * @param irSequence
     * @return RepeatFinder
     */
//    public static RepeatFinder newRepeatFinder(IrSequence irSequence) { // TODO remove
//        //return new RepeatFinder(irSequence.toInts(true), errLimit);
//        return new RepeatFinder(irSequence);
//    }

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
     * @return IrSignal, or null on failure.
     * @throws ParseException
     * @throws IncompatibleArgumentException
     * @throws UnassignedException
     * @throws DomainViolationException
     * @throws InvalidRepeatException
     */
    public static IrSignal interpretString(String str, double frequency, boolean invokeRepeatFinder, boolean invokeCleaner) throws ParseException, IncompatibleArgumentException, UnassignedException, DomainViolationException, InvalidRepeatException {
        try {
            if (str.trim().startsWith("[")) {
                String[] codes = str.trim().substring(1).split("[\\[\\]]+");
                return new IrSignal(frequency, IrpUtils.invalid, codes[0],
                        codes.length > 1 ? codes[1] : null, codes.length > 2 ? codes[2] : null);
            }

            try {
                int[] array = Pronto.parseString(str);
                return array != null ? Pronto.ccfSignal(array) : UeiLearnedSignal.parseUeiLearned(str);
            } catch (IllegalArgumentException | IncompatibleArgumentException ex) {
                return interpretRawString(str, frequency, invokeRepeatFinder, invokeCleaner);
            }
        } catch (NumberFormatException ex) {
            throw new ParseException("Could not interpret string " + str + " (" + ex.getMessage() + ")");
        }
    }

    private static IrSignal interpretRawString(String str, double frequency, boolean invokeRepeatFinder, boolean invokeCleaner) throws ParseException, IncompatibleArgumentException {
        try {
            String[] codes = str.split("[\n\r]+");
            if (codes.length > 1)
                return new IrSignal(frequency, IrpUtils.invalid, codes[0], codes[1], codes.length > 2 ? codes[2] : null);

            IrSequence irSequence = new IrSequence(str, true);
            ModulatedIrSequence modulatedIrSequence = new ModulatedIrSequence(irSequence, frequency, (double) IrpUtils.unknownDutyCycle);
            return interpretIrSequence(modulatedIrSequence, invokeRepeatFinder, invokeCleaner);
        } catch (NumberFormatException ex) {
            throw new ParseException("Could not interpret string " + str + " (" + ex.getMessage() + ")");
        }
    }

    /**
     * Tries to interpret the string argument as one of our known formats, and return an IrSignal.
     * If the string starts with "+", take it as raw. Then, if it contains more than one line, assume
     * that the caller has split it into intro, repeat, and ending sequences already.
     * Otherwise invoke ExchangeIR to split it into constituents. If it does not start
     * with "+", try to interpret as Pronto CCF and UEI learned.
     *
     * @param str String to be interpreted.
     * @param invokeRepeatFinder
     * @param invokeCleaner
     * @return IrSignal
     * @throws IncompatibleArgumentException
     * @throws UnassignedException
     * @throws DomainViolationException
     * @throws InvalidRepeatException
     * @throws ParseException
     */
    public static IrSignal interpretString(String str, boolean invokeRepeatFinder, boolean invokeCleaner) throws IncompatibleArgumentException, UnassignedException, DomainViolationException, InvalidRepeatException, ParseException {
        return interpretString(str, IrpUtils.defaultFrequency, invokeRepeatFinder, invokeCleaner);
    }

    //public static IrSignal interpretString(String str, boolean invokeRepeatFinder) throws IncompatibleArgumentException, UnassignedException, DomainViolationException, InvalidRepeatException, ParseException {
    //    return interpretString(str, invokeRepeatFinder, true);
    //}

    /**
     * Tries to interpret the string argument as one of our known formats, and return an IrSignal.
     * If the string starts with "+", take it as raw. Then, if it contains more than one line, assume
     * that the caller has split it into intro, repeat, and ending sequences already.
     * Otherwise invoke ExchangeIR to split it into constituents. If it does not start
     * with "+", try to interpret as Pronto CCF and UEI learned.
     *
     * @param str String to be interpreted.
     * @return IrSignal
     * @throws IncompatibleArgumentException
     * @throws UnassignedException
     * @throws DomainViolationException
     * @throws InvalidRepeatException
     * @throws ParseException
     */
    //public static IrSignal interpretString(String str) throws IncompatibleArgumentException, UnassignedException, DomainViolationException, InvalidRepeatException, ParseException {
    //    return interpretString(str, IrpUtils.defaultFrequency, true, true);
    //}

    /**
     * If invokeRepeatFinder is true, tries to identify intro, repeat, and ending applying a RepeatFinder.
     * If not, the sequence is used as intro on the returned signal.
     * In this case, if invokeCleaner is true, an analyzer is first used to clean the signal.
     * @param irSequence
     * @param frequency
     * @param invokeRepeatFinder If the repeat finder is invoked. This alse uses the analyzer.
     * @param invokeCleaner If the analyzer is invoked for cleaning the signals.
     * @return IrSignal signal constructed according to rules above.
     */
    public static IrSignal interpretIrSequence(IrSequence irSequence, double frequency,
            boolean invokeRepeatFinder, boolean invokeCleaner) {
        ModulatedIrSequence modulatedIrSequence = new ModulatedIrSequence(irSequence, frequency, (double) IrpUtils.invalid);
        return interpretIrSequence(modulatedIrSequence, invokeRepeatFinder, invokeCleaner);
    }

    public static IrSignal interpretIrSequence(ModulatedIrSequence modulatedIrSequence, boolean invokeRepeatFinder, boolean invokeCleaner) {
        // The analyzer misbehaves for zero length arguments, so treat this case separately.
        //if (irSequence == null || irSequence.getLength() == 0)
        //    return new IrSignal();

        return invokeRepeatFinder
                ? (invokeCleaner ? RepeatFinder.findRepeatClean(modulatedIrSequence) : RepeatFinder.findRepeat(modulatedIrSequence))
                : invokeCleaner ? Cleaner.clean(modulatedIrSequence).toIrSignal()
                : modulatedIrSequence.toIrSignal();
    }

    //public static IrSignal interpretIrSequence(IrSequence irSequence, double frequency, boolean invokeRepeatFinder) {
    //    return interpretIrSequence(irSequence, frequency, invokeRepeatFinder, true);
    //}

    /**
     * By applying a RepeatFinder, tries to identify intro, repeat, and ending;
     * thus constructing an IrSignal.
     * If the first argument happens to be of the subclass ModulatedIrSequence,
     * the information of the modulation frequency is taken into account,
     * otherwise a default value of the modulation frequency is used.
     * @param irSequence
     * @param invokeRepeatFinder
     * @param invokeCleaner
     * @return IrSignal
     *
     */
    public static IrSignal interpretIrSequence(IrSequence irSequence, boolean invokeRepeatFinder, boolean invokeCleaner) {
        return interpretIrSequence(irSequence,
                irSequence instanceof ModulatedIrSequence ? ((ModulatedIrSequence)irSequence).frequency : IrpUtils.defaultFrequency,
                invokeRepeatFinder, invokeCleaner);
    }

    //public static IrSignal interpretIrSequence(IrSequence irSequence, boolean invokeRepeatFinder) {
    //    return interpretIrSequence(irSequence, invokeRepeatFinder, true);
    //}

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
            return interpretIrSequence(new IrSequence(data), frequency, invokeRepeatFinder, invokeCleaner);
        } catch (IncompatibleArgumentException ex) {
            return null;
        }
    }

    /**
     * By applying a RepeatFinder, tries to identify intro, repeat, and ending;
     * thus constructing an IrSignal.
     * @param data
     * @param invokeRepeatFinder
     * @param invokeCleaner
     * @return IrSignal
     */
    //public static IrSignal interpretIrSequence(int[] data, boolean invokeRepeatFinder, boolean invokeCleaner) {
    //    return interpretIrSequence(data, IrpUtils.defaultFrequency, invokeRepeatFinder, invokeCleaner);
    //}

    //public static IrSignal interpretIrSequence(int[] data, boolean invokeRepeatFinder) {
    //    return interpretIrSequence(data, invokeRepeatFinder, true);
    //}

}
