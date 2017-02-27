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

import com.hifiremote.exchangeir.UeiLearned;
import com.hifiremote.exchangeir.UeiLearnedImporter;

/**
 * This class interfaces to the ExchangeIR library. It consists entirely of of
 * static functions, using the IrpMaster classes IrSignal, IrSequence as
 * arguments or results.
 *
 */
public class UeiLearnedSignal {

    /**
     * The errlimit parameter in ExchangeIR. Amounts to allowed difference im
     * microseconds.
     */
    private static final int errLimit = 25;

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
        IrSignal irSignal = new IrSignal(importer.getFrequency(), IrpUtils.unknownDutyCycle,
                new IrSequence(data, 0, 2 * importer.getNoIntroBursts()),
                new IrSequence(data, 2 * importer.getNoIntroBursts(), 2 * importer.getNoRepeatBursts()),
                new IrSequence(data, 2 * (importer.getNoIntroBursts() + importer.getNoRepeatBursts()), 2 * importer.getNoEndingBursts()));
        return irSignal;
    }

    /**
     * The main routine is mainly for testing and demonstration.
     *
     * @param args UEI learned signal.
     */
    public static void main(String[] args) {
        StringBuilder str = new StringBuilder(3 * args.length);
        for (String arg : args) {
            if (str.length() > 0)
                str.append(" ");
            str.append(arg);
        }
        IrSignal irSignal = parseUeiLearned(str.toString());
        System.out.println(irSignal);
        //ModulatedIrSequence seq = irSignal.toModulatedIrSequence(7);
        //RepeatFinder repeatFinder = newRepeatFinder(seq);
        //System.out.println(repeatFinder);
        DecodeIR.invoke(irSignal);
    }

    /**
     * Instantiating this class is meaningless.
     */
    private UeiLearnedSignal() {
    }
}
