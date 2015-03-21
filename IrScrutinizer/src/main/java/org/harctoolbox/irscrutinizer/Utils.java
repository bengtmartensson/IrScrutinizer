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

import java.io.File;
import java.util.HashMap;
import org.harctoolbox.IrpMaster.DomainViolationException;
import org.harctoolbox.IrpMaster.ExchangeIR;
import org.harctoolbox.IrpMaster.IncompatibleArgumentException;
import org.harctoolbox.IrpMaster.InvalidRepeatException;
import org.harctoolbox.IrpMaster.IrSignal;
import org.harctoolbox.IrpMaster.IrpUtils;
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
     * @param fallbackFrequency
     * @param invokeRepeatFinder
     * @param invokeAnalyzer
     * @return Generated IrSignal, or null if failed.
     * @throws ParseException
     * @throws IncompatibleArgumentException
     * @throws UnassignedException
     * @throws DomainViolationException
     * @throws InvalidRepeatException
     */
    public static IrSignal interpretString(String string, double fallbackFrequency, boolean invokeRepeatFinder, boolean invokeAnalyzer) throws ParseException, IncompatibleArgumentException, UnassignedException, DomainViolationException, InvalidRepeatException {
        return string.startsWith(GlobalCache.sendIrPrefix) ? GlobalCache.parse(string)
                : ExchangeIR.interpretString(string, fallbackFrequency, invokeRepeatFinder, invokeAnalyzer);
    }

    private Utils() {
    }

    public static final String girrHomepageUrl = "http://www.harctoolbox.org/Girr.html";

    public static final String linefeed = System.getProperty("line.separator", "\n");
    public static final double seconds2microseconds = 1000000f;

    public static String basename(String s) {
        StringBuilder sb = new StringBuilder(s);
        int n = sb.lastIndexOf(File.separator);
        if (n != -1)
            sb.delete(0, n+1);
        n = sb.lastIndexOf(".");
        if (n != -1)
            sb.delete(n, sb.length());
        return sb.toString();
    }

    public static String addExtensionIfNotPresent(String filename, String extension) {
        return filename + ((extension != null && !hasExtension(filename)) ? ('.' + extension) : "");
    }

    private static boolean hasExtension(String filename) {
        int lastSeparator = filename.lastIndexOf(File.separator);
        int lastPeriod = filename.lastIndexOf('.');
        return lastPeriod > lastSeparator;
    }

    public static int numberbaseIndex2numberbase(int index) {
        return index == 0 ? 2
                : index == 1 ? 8
                : index == 2 ? 10
                : 16;
    }

    public static HashMap<String,Long> mkParameters(long D, long S, long F) {
        HashMap<String, Long> result = new HashMap<String, Long>(3);
        if (D != IrpUtils.invalid)
            result.put("D", D);
        if (S != IrpUtils.invalid)
            result.put("S", S);
        if (F != IrpUtils.invalid)
            result.put("F", F);

        return result;
    }
}
