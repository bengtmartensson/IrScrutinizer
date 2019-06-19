/*
Copyright (C) 2016 Bengt Martensson.

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

package org.harctoolbox.irscrutinizer.exporter;

import java.util.HashMap;

/**
 * This class is a helper for C exporters. It makes unique C identifiers from "arbitrary"
 * command names.
 */
public class CIdentifierFactory {

    private final HashMap<Integer, String> table;

    public CIdentifierFactory() {
        table = new HashMap<>(8);
    }

    public String mkCIdentifier(String s, int position) {
        if (table.containsKey(position))
            return table.get(position);

        String str = s.replaceAll("[^0-9A-Za-z_]", "_");
        if (str.matches("^[0-9].*"))
            str = "_" + str;
        int n = 1;
        String candidate = str;
        while (table.containsValue(candidate)) {
            candidate = str + "_" + n;
            n++;
        }
        table.put(position, candidate);
        return candidate;
    }
}
