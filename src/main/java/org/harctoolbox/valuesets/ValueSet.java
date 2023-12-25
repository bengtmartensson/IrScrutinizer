/*
Copyright (C) 2011,2013 Bengt Martensson.

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
import java.util.logging.Level;
import java.util.logging.Logger;
import org.harctoolbox.ircore.IrCoreUtils;
import org.harctoolbox.irp.NameUnassignedException;

/**
 * A value set is either an single value or a way
 * of generating repetitions. These are implemented in subclasses.
 *
 */
public abstract class ValueSet implements Iterable<Long> {

    protected static String formatThing(String prefix, int variable) {
        return (variable == IrCoreUtils.INVALID) ? "" : (prefix + "=" + variable);
    }

    /**
     * Factory method for parsing strings of iterations.
     *
     * @param protocolMin Protocol min value, or null if unknown,
     * @param protocolMax Protocol max value, or null if unknown.
     * @param s String to be parsed describing the iteration.
     * @return ValueSet
     * @throws NameUnassignedException if protocolMin == null or protocolMax == null and iteration uses # or *.
     * @throws ParseException unparseable s argument.
     */
    public static ValueSet newValueSet(Long protocolMin, Long protocolMax, String s) throws ParseException, NameUnassignedException {
        if (s.contains("<<")) {
            String[] q = s.split("<<");
            long min = IrCoreUtilsFix.parseLong(q[0], true);
            if (min == 0)
                throw new IllegalArgumentException("Shifting 0 in " + s + " senseless.");
            long max = IrCoreUtils.parseUpper(q[0]);
            int shift = (int) IrCoreUtilsFix.parseLong(q[1], true);
            return new ShiftValueSet(min, max, shift);
        } else if (s.contains("++")) {
            String[] q = s.split("\\+\\+");
            if (q.length != 2)
                throw new ParseException("Could not parse value set " + s, 0);
            long min = IrCoreUtilsFix.parseLong(q[0], true);
            long max = IrCoreUtils.parseUpper(q[0]);
            int increment = (int) IrCoreUtilsFix.parseLong(q[1], false);
            return new IterationValueSet(min, max, increment);
        } else if (s.contains("#")) {
            if (protocolMin == null || protocolMax == null)
                throw new NameUnassignedException("min or max not assigned");

            String[] q = s.split("#");
            long min = q[0].trim().isEmpty() ? protocolMin : IrCoreUtilsFix.parseLong(q[0], true);
            long max = q[0].trim().isEmpty() ? protocolMax : IrCoreUtils.parseUpper(q[0]);
            int noRandoms = (int) IrCoreUtilsFix.parseLong(q[1], true);
            return new RandomValueSet(min, max, noRandoms);
        } else if (s.startsWith("*")) {
            if (protocolMin == null || protocolMax == null)
                throw new NameUnassignedException("min or max not assigned");

            return new IterationValueSet(protocolMin, protocolMax, 1);
        } else {
            long min = IrCoreUtilsFix.parseLong(s, true);
            long max = IrCoreUtils.parseUpper(s);
            return (max == IrCoreUtils.INVALID) ? new SingletonValueSet(min) : new IterationValueSet(min, max, 1);
        }
    }

    /**
     * Just for testing purposes.
     *
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

        ValueSet vs = null;
        try {
            vs = newValueSet(0L, 255L, args[arg_i]);
        } catch (NameUnassignedException | ParseException ex) {
            Logger.getLogger(ValueSet.class.getName()).log(Level.SEVERE, null, ex);
        }
        System.out.println(vs);
        System.out.println();

        if (vs != null) {
            for (Long l : vs) {
                System.out.println(l);
            }
        }
    }

    protected long min;
    protected long current; // Memory for the iterator

    protected ValueSet(long min) {
        this.min = min;
        current = IrCoreUtils.INVALID;
    }

    /**
     * Resets the iterator to virgin state.
     */
    public void reset() {
        current = IrCoreUtils.INVALID;
    }

}
