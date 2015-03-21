/*
Copyright (C) 2009-2012 Bengt Martensson.

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

/**
 * Type of toggle in an IR signal.
 */

public enum ToggleType {

    /**
     * Generate the toggle code with toggle = 0.
     */
    toggle0,
    /**
     * Generate the toggle code with toggle = 1.
     */
    toggle1,

    /**
     * Don't care
     */
    dontCare;

    public static ToggleType flip(ToggleType t) {
        return t == toggle0 ? toggle1 : toggle0;
    }

    public static int toInt(ToggleType t) {
        return t == dontCare ? -1 : t.ordinal();// == toggle1 ? 1 : 0;
    }

    public static ToggleType parse(String t) {
        return t.equals("0") ? ToggleType.toggle0 : t.equals("1") ? ToggleType.toggle1 : ToggleType.dontCare;
    }

    public static String toString(ToggleType toggle) {
        return toggle == ToggleType.toggle0 ? "0"
                : toggle == ToggleType.toggle1 ? "1" : "-";
    }
}
