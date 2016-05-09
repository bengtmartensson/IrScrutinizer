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

package org.harctoolbox.irscrutinizer;

import org.harctoolbox.IrpMaster.DomainViolationException;
import org.harctoolbox.IrpMaster.IncompatibleArgumentException;
import org.harctoolbox.IrpMaster.InterpretString;
import org.harctoolbox.IrpMaster.InvalidRepeatException;
import org.harctoolbox.IrpMaster.IrSignal;
import org.harctoolbox.IrpMaster.ParseException;
import org.harctoolbox.IrpMaster.UnassignedException;
import org.harctoolbox.harchardware.ir.GlobalCache;

/**
 * This class does something interesting and useful. Or not...
 */
public class Utils {

    // Maintainer note 1:
    // This should NOT go into IrpMaster.ExchangeIR since that package must not depend on HarcHardware.

    // Maintainer note 2:
    // Do not add a version with default fallback frequency.
    // That would make the user ignore a problem that should not be just ignored.

    /**
     * Smarter version of ExchangeIR.interpretString.
     *
     * @param string
     * @param frequency
     * @param invokeRepeatFinder
     * @param invokeAnalyzer
     * @return Generated IrSignal, or null if failed.
     * @throws ParseException
     * @throws IncompatibleArgumentException
     * @throws UnassignedException
     * @throws DomainViolationException
     * @throws InvalidRepeatException
     */
    public static IrSignal interpretString(String string, double frequency, boolean invokeRepeatFinder, boolean invokeAnalyzer)
            throws ParseException, IncompatibleArgumentException, UnassignedException, DomainViolationException, InvalidRepeatException {
        return string.startsWith(GlobalCache.sendIrPrefix)
                ? GlobalCache.parse(string)
                : InterpretString.interpretString(string, frequency, invokeRepeatFinder, invokeAnalyzer);
    }

    private Utils() {
    }
}
