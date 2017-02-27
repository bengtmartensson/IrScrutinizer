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
import org.harctoolbox.IrpMaster.IrpUtils;

public class ShiftValueSet extends ValueSet {

    private final long max;
    private final int shift;

    public ShiftValueSet(long min, long max, int shift) {
        super(min);
        this.max = max;
        this.shift = shift;
    }

    @Override
    public String toString() {
        return "{"
                + formatThing("min", (int) min)
                + formatThing(", max", (int) max)
                + formatThing(", shift", shift)
                + "}";
    }

    @Override
    public Iterator<Long> iterator() {
        return new Iterator<Long>() {

            @Override
            public Long next() {
                if (!hasNext())
                    throw new NoSuchElementException();

                if (current == IrpUtils.invalid)
                    current = min;
                else
                    current <<= getShift();

                return current;
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException("Not supported.");
            }

            @Override
            public boolean hasNext() {
                return current == IrpUtils.invalid || current << getShift() <= getMax();
            }
        };
    }

    /**
     * @return the max
     */
    public long getMax() {
        return max;
    }

    /**
     * @return the shift
     */
    public int getShift() {
        return shift;
    }
}
