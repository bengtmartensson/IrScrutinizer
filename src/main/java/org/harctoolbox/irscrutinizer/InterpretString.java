/*
 * Copyright (C) 2018 Bengt Martensson
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.harctoolbox.irscrutinizer;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.harctoolbox.analyze.Cleaner;
import org.harctoolbox.analyze.CleanerParser;
import org.harctoolbox.analyze.RepeatFinder;
import org.harctoolbox.analyze.RepeatFinderParser;
import org.harctoolbox.harchardware.ir.BroadlinkBase64Parser;
import org.harctoolbox.harchardware.ir.BroadlinkHexParser;
import org.harctoolbox.harchardware.ir.GlobalCacheParser;
import org.harctoolbox.ircore.InvalidArgumentException;
import org.harctoolbox.ircore.IrSignal;
import org.harctoolbox.ircore.IrSignalParser;
import org.harctoolbox.ircore.ModulatedIrSequence;
import org.harctoolbox.ircore.MultiParser;
import org.harctoolbox.irp.ShortProntoParser;

public class InterpretString {

    private final static Logger logger = Logger.getLogger(InterpretString.class.getName());

    /**
     * If invokeRepeatFinder is true, tries to identify intro, repeat, and
     * ending applying a RepeatFinder. If not, the sequence is used as intro on
     * the returned signal. In this case, if invokeCleaner is true, an analyzer
     * is first used to clean the signal.
     *
     * @param irSequence
     * @param invokeRepeatFinder If the repeat finder is invoked. This also uses
     * the analyzer.
     * @param absoluteTolerance
     * @param relativeTolerance
     * @param invokeCleaner If the analyzer is invoked for cleaning the signals.
     * @return IrSignal signal constructed according to rules above.
     * @throws org.harctoolbox.ircore.InvalidArgumentException
     */
    public static IrSignal interpretIrSequence(ModulatedIrSequence irSequence, boolean invokeRepeatFinder, boolean invokeCleaner,
            Double absoluteTolerance, Double relativeTolerance) throws InvalidArgumentException {
        if (invokeRepeatFinder) {
            RepeatFinder repeatFinder = new RepeatFinder(irSequence, absoluteTolerance, relativeTolerance);
            return invokeCleaner ? repeatFinder.toIrSignalClean(irSequence) : repeatFinder.toIrSignal(irSequence);
        } else {
            ModulatedIrSequence sequence = invokeCleaner ? Cleaner.clean(irSequence, absoluteTolerance, relativeTolerance) : irSequence;
            return sequence.toIrSignal();
        }
    }

    public static IrSignal interpretString(String line, Double fallbackFrequency, Double dummyGap, boolean invokeRepeatFinder, boolean invokeCleaner, Double absoluteTolerance, Double relativeTolerance, Double minRepeatLastGap) throws InvalidArgumentException {
        List<IrSignalParser> parsers = MultiParser.ircoreParsersList(line);
        parsers.add(0, new GlobalCacheParser(line));
        parsers.add(1, new ShortProntoParser(line));
        parsers.add(2, new BroadlinkBase64Parser(line));
        parsers.add(3, new BroadlinkHexParser(line));

        // If it goes wrong with InvalidArgumentException, try again without repeat finder and cleaner.
        try {
            if (invokeRepeatFinder) {
                RepeatFinderParser parser = new RepeatFinderParser(parsers, line, absoluteTolerance, relativeTolerance, minRepeatLastGap);
                return invokeCleaner ? parser.toIrSignalClean(fallbackFrequency, dummyGap) : parser.toIrSignal(fallbackFrequency, dummyGap);
            } else if (invokeCleaner) {
                CleanerParser parser = new CleanerParser(parsers, line, absoluteTolerance, relativeTolerance);
                return parser.toIrSignal(fallbackFrequency, dummyGap);
            }
        } catch (InvalidArgumentException ex) {
            logger.log(Level.WARNING, "{0}", ex.getMessage());
        }

        MultiParser parser = new MultiParser(parsers, line);
        return parser.toIrSignal(fallbackFrequency, dummyGap);
    }

    private InterpretString() {
    }
}
