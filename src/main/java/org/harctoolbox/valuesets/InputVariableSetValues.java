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
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.harctoolbox.ircore.InvalidArgumentException;
import org.harctoolbox.ircore.IrCoreUtils;
import org.harctoolbox.irp.NameUnassignedException;
import org.harctoolbox.irp.Protocol;

/**
 * This class produces, through its iterator, the "Cartesian product" of its VariableSetList's.
 * This class is immutable, except of course for the state of the iterator, complemented with a reset() function.
 *
 * The name of the class is awful.
 */

// The problem here is that the iterators are in an unusable state until you have called next() the first time.
// Therefore, by initialization I call next() for each variable, thus making values valid,
// and set virgin = true. The first call to next() will not call any iterators, but just flip virgin = false.

public class InputVariableSetValues implements Iterable<LinkedHashMap<String, Long>>{
    /**
     * Just for interactive testing
     * @param args
     */
    public static void main(String[] args) {
        int seed = (int) IrCoreUtils.INVALID;
        int arg_i = 0;
        if (args[arg_i].equals("-s")) {
            arg_i++;
            seed = Integer.parseInt(args[arg_i]);
            arg_i++;
        }
        RandomValueSet.initRng(seed);

        String[] s = new String[args.length - arg_i];
        System.arraycopy(args, arg_i, s, 0, args.length - arg_i);

        InputVariableSetValues ivsv = null;
        try {
            ivsv = new InputVariableSetValues(s, null);
        } catch (NameUnassignedException | InvalidArgumentException | ParseException ex) {
            Logger.getLogger(InputVariableSetValues.class.getName()).log(Level.SEVERE, null, ex);
        }

        System.out.println(ivsv);

        if (ivsv != null)
            for (LinkedHashMap<String, Long> hm : ivsv) {
                System.out.println(hm);//ivsv.currentAssignment);
        }
    }

    // I use LinkedHashMap instead of HashMap because the latter preserves the order of the keys.
    private LinkedHashMap<String, ValueSetList>map;

    // These hold the state of the iterator
    private LinkedHashMap<String, Iterator<Long>>iterators;
    private boolean virgin = true;

    // current state of the variables, as per the iterator.
    private LinkedHashMap<String, Long> currentAssignment;

    // Just used to resolve "*"
    private Protocol protocol;



    private InputVariableSetValues(Protocol protocol) {
        this.protocol = protocol;
        map = new LinkedHashMap<>(4);
        currentAssignment = new LinkedHashMap<>(4);
        iterators = new LinkedHashMap<>(4);
    }

    public InputVariableSetValues(String[] str, Protocol protocol) throws NameUnassignedException, InvalidArgumentException, ParseException {
        this(protocol);
        for (String s : str) {
            String[] kv = s.split("=");
            if (kv.length != 2)
                throw new IllegalArgumentException();
            assign(kv[0], kv[1]);
        }
    }

    public InputVariableSetValues(Map<String, String>input, boolean reverse, Protocol protocol) throws NameUnassignedException, InvalidArgumentException, ParseException {
        this(protocol);

        if (reverse) {
            String[] s = new String[input.size()];
            input.keySet().toArray(s);
            for (int i = 0; i < s.length; i++) {
                String key = s[s.length - i - 1];
                assign(key, input.get(key));
            }
        } else {
            for (Entry<String, String> kvp : input.entrySet())
                assign(kvp.getKey(), kvp.getValue());
        }
    }

    private boolean isFinished() {
        if (virgin)
            return false;
        boolean finished = true;
        for (String var : iterators.keySet())
            if (iterators.get(var).hasNext()) {
                finished = false;
                break;
            }
        return finished;
    }

    private void update() throws NoSuchElementException {
        if (isFinished())
            throw new NoSuchElementException();

        if (virgin) {
            virgin = false;
            return;
        }

        // Think of this a a number of cascaded counters:
        /// Starting with the "leftmost", increment each one as long as necessary,
        // i.e. as long as they have reached their max.
        for (String v : map.keySet()) {
            if (iterators.get(v).hasNext()) {
                // This has not reached its max, just increment and we are through.
                Long value = iterators.get(v).next();
                currentAssignment.put(v, value);
                break;
            } else {
                // Reset. We have a "carry", i.e. continue with following interators.
                map.get(v).reset();
                Long value = iterators.get(v).next();
                currentAssignment.put(v, value);
            }
        }
    }

    public Set<String> getKeys() {
        return map.keySet();
    }

    private void assign(String untrimmedName, String value) throws NameUnassignedException, InvalidArgumentException, ParseException {
        Long val; // = IrCoreUtils.invalid;
        String name = untrimmedName.trim();

        try {
            val = IrCoreUtils.parseLong(value, true);
            // Ignore negative values, equivalent of not assigning.
            if (val < 0 && val != IrCoreUtils.ALL && val != IrCoreUtils.SOME)
                return;
        } catch (NumberFormatException ex) {
            if (value == null || value.trim().isEmpty())
                throw new NameUnassignedException("Parameter " + name + " not assigned.");
            throw new InvalidArgumentException("Parameter " + name + ": unparsable value `" + value + "'.");
        }


        //ValueSetList vsl;
        if (protocol == null)
            throw new NameUnassignedException("protocol not assigned");

        ValueSetList vsl;
        try {
            vsl = (protocol.hasParameter(name))
                    ? new ValueSetList(protocol.getParameterMin(name), protocol.getParameterMax(name), value)
                    : new ValueSetList(null, null, value);
        } catch (NameUnassignedException ex) {
            throw new NameUnassignedException("Parameter " + name + " not found in parameter specs, cannot loop over it.");
        }

        map.put(name, vsl);
        Iterator<Long> it = vsl.iterator();

        currentAssignment.put(name, it.next()); // Unusable state before first next()
        iterators.put(name, it);

    }

    public void reset() {
        map.keySet().stream().map((var) -> {
            map.get(var).reset();
            return var;
        }).forEachOrdered((var) -> {
            currentAssignment.put(var, iterators.get(var).next());
        });
        virgin = true;
    }

    @Override
    public String toString() {
        return map.toString();
    }

    @Override
    public Iterator<LinkedHashMap<String, Long>> iterator() {
       return new Iterator<LinkedHashMap<String, Long>>() {
            @Override
            public boolean hasNext() {
                return !isFinished();
            }

            @Override
            public LinkedHashMap<String, Long> next() /*throws NoSuchElementException*/ {
                update(); // throws NoSuchElementException
                return currentAssignment;
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException("Not supported.");
            }
       };
    }
}
