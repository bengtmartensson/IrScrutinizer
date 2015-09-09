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

import com.hifiremote.exchangeir.Analyzer;
import com.hifiremote.exchangeir.RepeatFinder;
import com.hifiremote.exchangeir.UeiLearned;
import com.hifiremote.exchangeir.UeiLearnedImporter;

/**
 * This class interfaces to the ExchangeIR library. It consists entirely of of static functions,
 * using the IrpMaster classes IrSignal, IrSequence as arguments or results.
 *
 */
public class ExchangeIR {
    /** The errlimit parameter in ExchangeIR. Amounts to allowed difference im microseconds. */
    public static final int errLimit = 25;

    /** The number basis for Analyzer outputs. Quite ugly...*/
    private static int analyzerBasis = 16;

    public static void setAnalyzerBasis(int basis) {
        analyzerBasis = basis;
    }

    /** Number of characters in the hexadecimal representation of UEI learned signals. */
    public static final int ueiLearnedCharsInDigit = 2;
    //private static boolean invokeRepeatFinder;

    /** Instantiating this class is meaningless. */
    private ExchangeIR() {}

    /**
     * Returns a new Analyzer, constructed from an IrSignal.
     *
     * @param irSignal input IrSignal
     * @return Analyzer
     */
    public static Analyzer newAnalyzer(IrSignal irSignal) {
        return new Analyzer(irSignal.toIntArray(),
                     irSignal.getIntroBursts(), irSignal.getRepeatBursts(), irSignal.getEndingBursts(),
                     1, (int) irSignal.getFrequency(), errLimit, analyzerBasis);
    }

    /**
     * Returns a new Analyzer, constructed from an IrSequence and a frequency.
     *
     * @param irSequence
     * @param frequency
     * @return Analyzer, null if irSequence has zero length.
     */

    public static Analyzer newAnalyzer(IrSequence irSequence, double frequency) {
        return irSequence.isEmpty() ? null : new Analyzer(irSequence.toInts(true), (int) frequency, errLimit, analyzerBasis);
    }

    /**
     * Returns a new Analyzer, constructed from a ModulatedIrSequene.
     * @param irSequence
     * @return Analyzer
     */
    public static Analyzer newAnalyzer(ModulatedIrSequence irSequence) {
        return newAnalyzer(irSequence, irSequence.getFrequency());
    }

    /**
     * Constructs a RepeatFinder from an IrSequence.
     *
     * @param irSequence
     * @return RepeatFinder
     */
    public static RepeatFinder newRepeatFinder(IrSequence irSequence) {
        return new RepeatFinder(irSequence.toInts(true), errLimit);
    }

    /**
     * Constructs a UEI learned signal from an IrSignal.
     *
     * @param irSignal
     * @return UEI learned signal
     */
    public static UeiLearned newUeiLearned(IrSignal irSignal) {
        return new UeiLearned(irSignal.toIntArray(), irSignal.getIntroBursts(),
                irSignal.getRepeatBursts(), irSignal.getEndingBursts(), 1, (int) irSignal.getFrequency(), errLimit);
    }

    /**
     * Parses a string containing an UEI learned signal into an IrSignal.
     *
     * @param str
     * @return IrSignal
     */
    public static IrSignal parseUeiLearned(String str) {
        UeiLearnedImporter importer = new UeiLearnedImporter(str);
        return parseUeiLearned(importer);
    }

    /**
     * Parses a string containing an UEI learned signal into an IrSignal.
     *
     * @param array Integer array representing signal.
     * @return IrSignal
     */
    public static IrSignal parseUeiLearned(int[] array) {
        UeiLearnedImporter importer = new UeiLearnedImporter(array);
        return parseUeiLearned(importer);
    }

    private static IrSignal parseUeiLearned(UeiLearnedImporter importer) {
        int[] data = importer.getSignal();
        IrSignal irSignal = new IrSignal(importer.getFrequency(), IrpUtils.invalid,
                new IrSequence(data, 0, 2*importer.getNoIntroBursts()),
                new IrSequence(data, 2*importer.getNoIntroBursts(), 2*importer.getNoRepeatBursts()),
                new IrSequence(data, 2*(importer.getNoIntroBursts() + importer.getNoRepeatBursts()), 2*importer.getNoEndingBursts()));
        return irSignal;
    }

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
     * @param fallbackFrequency Modulation frequency to use, if it cannot be inferred from the first parameter.
     * @param invokeRepeatFinder
     * @param invokeAnalyzer
     * @return IrSignal, or null on failure.
     * @throws ParseException
     * @throws IncompatibleArgumentException
     * @throws UnassignedException
     * @throws DomainViolationException
     * @throws InvalidRepeatException
     */
    public static IrSignal interpretString(String str, double fallbackFrequency, boolean invokeRepeatFinder, boolean invokeAnalyzer) throws ParseException, IncompatibleArgumentException, UnassignedException, DomainViolationException, InvalidRepeatException {
        try {
            if (str.trim().startsWith("[")) {
                String[] codes = str.trim().substring(1).split("[\\[\\]]+");
                return new IrSignal(fallbackFrequency, IrpUtils.invalid, codes[0],
                        codes.length > 1 ? codes[1] : null, codes.length > 2 ? codes[2] : null);
            }

            try {
                int[] array = Pronto.parseString(str);
                return array != null ? Pronto.ccfSignal(array) : parseUeiLearned(str);
            } catch (IllegalArgumentException ex) {
                return interpretRawString(str, fallbackFrequency, invokeRepeatFinder, invokeAnalyzer);
            }
        } catch (NumberFormatException ex) {
            throw new ParseException("Could not interpret string " + str + " (" + ex.getMessage() + ")");
        }
    }

    private static IrSignal interpretRawString(String str, double fallbackFrequency, boolean invokeRepeatFinder, boolean invokeAnalyzer) throws ParseException, IncompatibleArgumentException {
        try {
            String[] codes = str.split("[\n\r]+");
            if (codes.length > 1)
                return new IrSignal(fallbackFrequency, IrpUtils.invalid, codes[0], codes[1], codes.length > 2 ? codes[2] : null);

            IrSequence irSequence = new IrSequence(str, true);
            return interpretIrSequence(irSequence, invokeRepeatFinder, invokeAnalyzer);
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
     * @param invokeAnalyzer
     * @return IrSignal
     * @throws IncompatibleArgumentException
     * @throws UnassignedException
     * @throws DomainViolationException
     * @throws InvalidRepeatException
     * @throws ParseException
     */
    public static IrSignal interpretString(String str, boolean invokeRepeatFinder, boolean invokeAnalyzer) throws IncompatibleArgumentException, UnassignedException, DomainViolationException, InvalidRepeatException, ParseException {
        return interpretString(str, IrpUtils.defaultFrequency, invokeRepeatFinder, invokeAnalyzer);
    }

    public static IrSignal interpretString(String str, boolean invokeRepeatFinder) throws IncompatibleArgumentException, UnassignedException, DomainViolationException, InvalidRepeatException, ParseException {
        return interpretString(str, invokeRepeatFinder, true);
    }

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
    public static IrSignal interpretString(String str) throws IncompatibleArgumentException, UnassignedException, DomainViolationException, InvalidRepeatException, ParseException {
        return interpretString(str, IrpUtils.defaultFrequency, true, true);
    }

    /**
     * If invokeRepeatFinder is true, tries to identify intro, repeat, and ending applying a RepeatFinder.
     * If not, the sequence is used as intro on the returned signal.
     * In this case, if invokeAnalyzer is true, an analyzer is first used to clean the signal.
     * @param irSequence
     * @param frequency
     * @param invokeRepeatFinder If the repeat finder is invoked. This alse uses the analyzer.
     * @param invokeAnalyzer If the analyzer is invoked for cleaning the signals.
     * @return IrSignal signal constructed according to rules above.
     */
    public static IrSignal interpretIrSequence(IrSequence irSequence, double frequency, boolean invokeRepeatFinder, boolean invokeAnalyzer) {
        // The analyzer misbehaves for zero length arguments, so treat this case separately.
        if (irSequence == null || irSequence.getLength() == 0)
            return new IrSignal();

        if (invokeRepeatFinder) {
            RepeatFinder repeatFinder = newRepeatFinder(irSequence);
            Analyzer analyzer = new Analyzer(irSequence.toInts(true), repeatFinder.getNoIntroBursts(),
                    repeatFinder.getNoRepeatBursts(), repeatFinder.getNoEndingBursts(), repeatFinder.getNoRepeats(),
                    frequency >= 0 ? (int) frequency : (int) IrpUtils.defaultFrequency, errLimit, analyzerBasis);
            return new IrSignal(analyzer.getCleansedSignal(), repeatFinder.getNoIntroBursts(),
                    repeatFinder.getNoRepeatBursts(), frequency >= 0 ? (int) frequency : (int) IrpUtils.defaultFrequency);
        } else if (invokeAnalyzer) {
            Analyzer analyzer = new Analyzer(irSequence.toInts(true), irSequence.getNumberBursts(),
                0, 0, 0,
                frequency >= 0 ? (int) frequency : (int) IrpUtils.defaultFrequency, errLimit, analyzerBasis);
            return new IrSignal(analyzer.getCleansedSignal(), irSequence.getNumberBursts(), 0,
                    frequency >= 0 ? (int) frequency : (int) IrpUtils.defaultFrequency);
        } else {
            // Just turn the sequence into an IR signal with just an intro.
            return new IrSignal(frequency >= 0 ? frequency : IrpUtils.defaultFrequency, IrpUtils.invalid, irSequence, null, null);
        }
    }

    public static IrSignal interpretIrSequence(IrSequence irSequence, double frequency, boolean invokeRepeatFinder) {
        return interpretIrSequence(irSequence, frequency, invokeRepeatFinder, true);
    }

    /**
     * By applying a RepeatFinder, tries to identify intro, repeat, and ending;
     * thus constructing an IrSignal.
     * If the first argument happens to be of the subclass ModulatedIrSequence,
     * the information of the modulation frequency is taken into account,
     * otherwise a default value of the modulation frequency is used.
     * @param irSequence
     * @param invokeRepeatFinder
     * @param invokeAnalyzer
     * @return IrSignal
     *
     */
    public static IrSignal interpretIrSequence(IrSequence irSequence, boolean invokeRepeatFinder, boolean invokeAnalyzer) {
        return interpretIrSequence(irSequence,
                irSequence instanceof ModulatedIrSequence ? ((ModulatedIrSequence)irSequence).frequency : IrpUtils.defaultFrequency,
                invokeRepeatFinder, invokeAnalyzer);
    }

    public static IrSignal interpretIrSequence(IrSequence irSequence, boolean invokeRepeatFinder) {
        return interpretIrSequence(irSequence, invokeRepeatFinder, true);
    }

    /**
     * By applying a RepeatFinder, tries to identify intro, repeat, and ending;
     * thus constructing an IrSignal.
     * @param data
     * @param frequency
     * @param invokeRepeatFinder
     * @param invokeAnalyzer
     * @return IrSignal
     */
    public static IrSignal interpretIrSequence(int[] data, double frequency, boolean invokeRepeatFinder, boolean invokeAnalyzer) {
        try {
            return interpretIrSequence(new IrSequence(data), frequency, invokeRepeatFinder, invokeAnalyzer);
        } catch (IncompatibleArgumentException ex) {
            return null;
        }
    }

    /**
     * By applying a RepeatFinder, tries to identify intro, repeat, and ending;
     * thus constructing an IrSignal.
     * @param data
     * @param invokeRepeatFinder
     * @param invokeAnalyzer
     * @return IrSignal
     */
    public static IrSignal interpretIrSequence(int[] data, boolean invokeRepeatFinder, boolean invokeAnalyzer) {
        return interpretIrSequence(data, IrpUtils.defaultFrequency, invokeRepeatFinder, invokeAnalyzer);
    }

    public static IrSignal interpretIrSequence(int[] data, boolean invokeRepeatFinder) {
        return interpretIrSequence(data, invokeRepeatFinder, true);
    }

    /**
     * The main routine is mainly for testing and demonstration.
     *
     * @param args UEI learned signal.
     */
    public static void main(String[] args) {
        StringBuilder str = new StringBuilder();
        for (String arg : args) {
            if (str.length() > 0)
                str.append(" ");
            str.append(arg);
        }
        IrSignal irSignal = parseUeiLearned(str.toString());
        System.out.println(irSignal);
        System.out.println(newAnalyzer(irSignal));
        ModulatedIrSequence seq = irSignal.toModulatedIrSequence(7);
        RepeatFinder repeatFinder = newRepeatFinder(seq);
        System.out.println(repeatFinder);
        System.out.println(newAnalyzer(seq));
        DecodeIR.invoke(irSignal);
    }
}
