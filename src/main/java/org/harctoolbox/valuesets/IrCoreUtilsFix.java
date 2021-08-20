/*
 * Copyright (C) 2021 Bengt Martensson
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
package org.harctoolbox.valuesets;

import org.harctoolbox.ircore.IrCoreUtils;
import static org.harctoolbox.ircore.IrCoreUtils.ALL;
import static org.harctoolbox.ircore.IrCoreUtils.SOME;

class IrCoreUtilsFix {

    /**
     * Fixed version of IrCore.parseLong(...)
     * Parses integers of base 2 (prefix "0b"  or "%", 8 (leading 0), 10, or 16 (prefix "0x).
     * If argument special is true, allows intervals 123..456 or 123:456 by ignoring upper part.
     * and translates `*' to the constant "all" = (-2) and `#' to "some" (= -3).
     * If no prefix, use the default given as second argument.
     *
     * @param str String to be parsed
     * @param special If the special stuff should be interpreted ('*', '+', intervals).
     * @param defaultRadix
     * @return long integer.
     */
    static long parseLong(String str, boolean special, int defaultRadix) /*throws NumberFormatException*/ {
        if (special && (str.startsWith("#") || str.contains(",")))
            return SOME;

        String s = special ? str.replaceAll("[:.\\+<#].*$", "").trim() : str;
        if (special && (s.equals("*") || s.equals("'*'")))
            return ALL; // Just to help Windows' victims, who cannot otherwise pass a *.
        //s.equals("#") ? some :
        return IrCoreUtils.parseWithPrefix(s, defaultRadix);
    }

    static long parseLong(String value, boolean special) {
        return parseLong(value, special, 10);
    }

    private IrCoreUtilsFix() {
    }
}