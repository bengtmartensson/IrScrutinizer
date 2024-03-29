/*
Copyright (C) 2011, 2013 Bengt Martensson.

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

package org.harctoolbox.valuesets;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.harctoolbox.ircore.IrCoreUtils;
import org.harctoolbox.irp.NameUnassignedException;

/**
 * This class bundles input arguments together.
 *
 * Giving a parameter a negative value is ignored, i.e. equivalent to not making any assignment at all.
 * Thus, for example using the four parameter main method, the user can assign T without assigning S.
 *
 * @author Bengt Martensson
 */

public class ValueSetList implements Iterable<Long> {
    /** For testing purposes only
     * @param args
     */
    @SuppressWarnings("UseOfSystemOutOrSystemErr")
    public static void main(String[] args) {
        int seed = (int) IrCoreUtils.INVALID;
        int arg_i = 0;
        if (args[arg_i].equals("-s")) {
            arg_i++;
            seed = Integer.parseInt(args[arg_i]);
            arg_i++;
        }
        RandomValueSet.initRng(seed);

        ValueSetList vsl = null;
        try {
            vsl = new ValueSetList(0L, 255L, args[arg_i]);
        } catch (NameUnassignedException | ParseException ex) {
            Logger.getLogger(ValueSetList.class.getName()).log(Level.SEVERE, null, ex);
        }

        System.out.println(vsl);

        if (vsl == null)
            return;

        for (Long l : vsl) {
            System.out.println(l);
        }
        System.out.println("----------------");
        vsl.reset();
        for (Long l : vsl) {
            System.out.println(l);
        }
    }

    private final ArrayList<ValueSet> valueSets = new ArrayList<>(4);
    private int currentSetIndex = (int) IrCoreUtils.INVALID;
    private Iterator<Long> setIterator = null;


    public ValueSetList(Long min, Long max, String str) throws ParseException, NameUnassignedException {
        String[] q = str.split(",");
        for (String q1 : q) {
            valueSets.add(ValueSet.newValueSet(min, max, q1));
        }
    }

    public void reset() {
        currentSetIndex = (int) IrCoreUtils.INVALID;
        setIterator = null;
        valueSets.forEach((valueSet) -> {
            valueSet.reset();
        });

    }

    @Override
    public Iterator<Long> iterator() {
        return new Iterator<Long>() {

            @Override
            public boolean hasNext() {
                return !valueSets.isEmpty()
                        && (currentSetIndex == IrCoreUtils.INVALID
                        || setIterator.hasNext()
                        || currentSetIndex < valueSets.size() - 1);
            }

            @Override
            public Long next() {
                if (!hasNext())
                    throw new NoSuchElementException();
                if (currentSetIndex == IrCoreUtils.INVALID)
                    currentSetIndex = 0;

                if (setIterator != null && !setIterator.hasNext()) {
                    currentSetIndex++;
                    setIterator = null;
                }

                if (setIterator == null)
                    setIterator = valueSets.get(currentSetIndex).iterator();

                return setIterator.next();
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException("Not supported.");
            }
        };
    }

    @Override
    public String toString() {
        return valueSets.toString();
    }

}
