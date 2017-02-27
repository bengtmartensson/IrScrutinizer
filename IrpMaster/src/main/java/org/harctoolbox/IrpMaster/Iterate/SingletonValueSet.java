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

/**
 * This class bundles input arguments together.
 *
 * Giving a parameter a negative value is ignored, i.e. equivalent to not making any assignment at all.
 * Thus, for example using the four parameter main method, the user can assign T without assigning S.
 *
 * @author Bengt Martensson
 */
public class SingletonValueSet extends ValueSet {

    /**
     *
     * @param value
     */
    public SingletonValueSet(long value) {
        super(value);
    }

    @Override
    public String toString() {
        return "{" + min + "}";
    }

    @Override
    public Iterator<Long> iterator() {
        return new Iterator<Long>() {

            @Override
            public Long next() {
                if (!hasNext())
                    throw new NoSuchElementException();
                current = min;
                return current;
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException("Not supported.");
            }

            @Override
            public boolean hasNext() {
                return current == IrpUtils.invalid;
            }
        };
    }
}
