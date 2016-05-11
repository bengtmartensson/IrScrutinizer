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

/**
 * This class interfaces to the ExchangeIR library. It consists entirely of of static functions,
 * using the IrpMaster classes IrSignal, IrSequence as arguments or results.
 *
 */
public class ExchangeIR {
    /** The errlimit parameter in ExchangeIR. Amounts to allowed difference im microseconds. */
    private static final int errLimit = 25;

    /** The number basis for Analyzer outputs. Quite ugly...*/
    private static int analyzerBasis = 16;

    public static void setAnalyzerBasis(int basis) {
        analyzerBasis = basis;
    }

    /** Number of characters in the hexadecimal representation of UEI learned signals. */
    public static final int ueiLearnedCharsInDigit = 2;

    /** Instantiating this class is meaningless. */
    private ExchangeIR() {
    }

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
}
