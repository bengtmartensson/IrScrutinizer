/*
Copyright (C) 2011 Bengt Martensson.

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
package org.harctoolbox.IrpMaster.Iterate;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Random;
import org.harctoolbox.IrpMaster.IrpUtils;

/**
 * This class ...
 */

public class RandomValueSet extends ValueSet {

    static Random rng;
    public static void initRng(int seed) {
        rng = seed > 0 ? new Random(seed) : new Random();
    }

    public static void initRng() {
        initRng((int) IrpUtils.invalid);
    }

    long max;
    int index; // How many time next() has been called.
    int noRandoms;
    public RandomValueSet(long min, long max, int noRandoms) {
        super(min);

        this.max = max;
        this.noRandoms = noRandoms;
        index = 0;
    }

    @Override
    public String toString() {
        return "{"
                + formatThing("min", (int) min)
                + formatThing(", max", (int) max)
                + formatThing(", noRandoms", noRandoms)
                + "}";
    }


    @Override
    public void reset() {
        index = 0;
    }

    @Override
    public Iterator<Long> iterator() {
        return new Iterator<Long>() {

            @Override
            public Long next() {
                if (!hasNext())
                    throw new NoSuchElementException();
                if (rng == null)
                    throw new RuntimeException("Must call RandomValueSet.initRng() before using RandomValueSet");

                index++;
                current = rng.nextInt((int) max + 1);

                return current;
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException("Not supported.");
            }

            @Override
            public boolean hasNext() {
                return index < noRandoms;
            }
        };
    }
}
