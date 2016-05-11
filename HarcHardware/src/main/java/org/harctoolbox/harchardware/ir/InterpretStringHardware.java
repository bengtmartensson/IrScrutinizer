/*
Copyright (C) 2013 Bengt Martensson.

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

package org.harctoolbox.harchardware.ir;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import java.util.ArrayList;
import org.harctoolbox.IrpMaster.DomainViolationException;
import org.harctoolbox.IrpMaster.IncompatibleArgumentException;
import org.harctoolbox.IrpMaster.InterpretString;
import org.harctoolbox.IrpMaster.InvalidRepeatException;
import org.harctoolbox.IrpMaster.IrSignal;
import org.harctoolbox.IrpMaster.IrpUtils;
import org.harctoolbox.IrpMaster.ParseException;
import org.harctoolbox.IrpMaster.UnassignedException;

/**
 * The static function interpretString herein "extends" the static function
 * org.harctoolbox.IrpMaster.InterpretString.interpretString in that it understands also some
 * hardware specific formats, presently GlobalCache sendir format.
 *
 * It should not be merged into the latter, since IrpMaster must not depend on HarcHardware.
 */
public class InterpretStringHardware {

    private static CommandLineArgs commandLineArgs;
    private static JCommander argumentParser;
    // Maintainer note:
    // Do not add a version with default fallback frequency.
    // That would make the user ignore a problem that should not be just ignored.

    /**
     * Smarter version of InterpretString.interpretString.
     *
     * @param string
     * @param frequency
     * @param invokeRepeatFinder
     * @param invokeCleaner
     * @param absoluteTolerance
     * @param relativeTolerance
     * @return Generated IrSignal, or null if failed.
     * @throws org.harctoolbox.IrpMaster.ParseException
     * @throws org.harctoolbox.IrpMaster.IncompatibleArgumentException
     * @throws org.harctoolbox.IrpMaster.UnassignedException
     * @throws org.harctoolbox.IrpMaster.DomainViolationException
     * @throws org.harctoolbox.IrpMaster.InvalidRepeatException
     */
    public static IrSignal interpretString(String string, double frequency, boolean invokeRepeatFinder, boolean invokeCleaner,
            double absoluteTolerance, double relativeTolerance)
            throws ParseException, IncompatibleArgumentException, UnassignedException, DomainViolationException, InvalidRepeatException {
        return string.startsWith(GlobalCache.sendIrPrefix)
                ? GlobalCache.parse(string)
                : InterpretString.interpretString(string, frequency, invokeRepeatFinder, invokeCleaner,
                        absoluteTolerance, relativeTolerance);
    }

    public static IrSignal interpretString(String string, double frequency, boolean invokeRepeatFinder, boolean invokeCleaner)
            throws ParseException, IncompatibleArgumentException, UnassignedException, DomainViolationException, InvalidRepeatException {
        return string.startsWith(GlobalCache.sendIrPrefix)
                ? GlobalCache.parse(string)
                : InterpretString.interpretString(string, frequency, invokeRepeatFinder, invokeCleaner);
    }

    private InterpretStringHardware() {
    }

    private final static class CommandLineArgs {
        private final static int defaultTimeout = 2000;

        @Parameter(names = {"-h", "-?", "--clean"}, description = "Invoke help")
        private boolean helpRequested = false;

        @Parameter(names = {"-c", "--clean"}, description = "Invoke cleaner")
        private boolean invokeCleaner = false;

        @Parameter(names = {"-r", "--repeatfinder"}, description = "Invoke repeatfinder")
        private boolean invokeRepeatFinder = false;

        @Parameter(names = {"-f", "--frequency"}, description = "Modulation frequency")
        private double frequency = IrpUtils.defaultFrequency;

        @Parameter(names = {"-a", "--absolutetolearance"}, description = "Absoulte Tolerance")
        private double absouteTolerance = IrpUtils.defaultAbsoluteTolerance;

        @Parameter(names = {"-r", "--relativetolearance"}, description = "Relative Tolerance")
        private double relativeTolerance = IrpUtils.defaultRelativeTolerance;

        @Parameter(description = "[arguments]")
        private ArrayList<String> arguments = new ArrayList<>();
    }

    public static void main(String[] args) {

        commandLineArgs = new CommandLineArgs();
        argumentParser = new JCommander(commandLineArgs);
        argumentParser.setProgramName("HarcHardware");

        try {
            argumentParser.parse(args);
        } catch (ParameterException ex) {
            System.err.println(ex.getMessage());
            argumentParser.usage();
            System.exit(IrpUtils.exitUsageError);
        }

        if (commandLineArgs.helpRequested) {
            argumentParser.usage();
            System.exit(IrpUtils.exitSuccess);
        }

        String payload = IrpUtils.join(commandLineArgs.arguments, " ");

        IrSignal irSignal;
        try {
            irSignal = interpretString(payload, commandLineArgs.frequency,
                    commandLineArgs.invokeRepeatFinder, commandLineArgs.invokeCleaner,
                    commandLineArgs.absouteTolerance, commandLineArgs.relativeTolerance);
            System.out.println(irSignal);
        } catch (ParseException | IncompatibleArgumentException | UnassignedException | DomainViolationException | InvalidRepeatException ex) {
            System.err.println(ex);
            System.exit(IrpUtils.exitFatalProgramFailure);
        }
    }
}
