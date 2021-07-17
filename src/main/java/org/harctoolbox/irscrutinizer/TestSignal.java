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
package org.harctoolbox.irscrutinizer;

import org.harctoolbox.ircore.InvalidArgumentException;
import org.harctoolbox.ircore.IrSignal;
import org.harctoolbox.ircore.Pronto;

public class TestSignal {

    private final static String TEST_SIGNAL_PRONTO_HEX = // NEC1 12.34 56
            "0000 006C 0022 0002 015B 00AD 0016 0016 0016 0016 0016 0041 0016 0041 "
            + "0016 0016 0016 0016 0016 0016 0016 0016 0016 0016 0016 0041 0016 0016 "
            + "0016 0016 0016 0016 0016 0041 0016 0016 0016 0016 0016 0016 0016 0016 "
            + "0016 0016 0016 0041 0016 0041 0016 0041 0016 0016 0016 0016 0016 0041 "
            + "0016 0041 0016 0041 0016 0016 0016 0016 0016 0016 0016 0041 0016 0041 "
            + "0016 06A4 015B 0057 0016 0E6C";

    public final static IrSignal testSignal;

    static {
        try {
            testSignal = Pronto.parse(TEST_SIGNAL_PRONTO_HEX);
        } catch (Pronto.NonProntoFormatException | InvalidArgumentException ex) {
            throw new IllegalArgumentException();
        }
    }

    private TestSignal() {
    }
}