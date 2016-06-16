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
package org.harctoolbox.harchardware.ir;

import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class LircTransmitter extends Transmitter {

    public static final int NOMASK = -1;
    private int[] transmitters;

    public LircTransmitter() {
        transmitters = null;
    }

    public LircTransmitter(int[] transmitters) {
        this.transmitters = transmitters;
    }

    public LircTransmitter(int xmitter) throws NoSuchTransmitterException {
        if (xmitter < 0)
            throw new NoSuchTransmitterException("Invalid transmitter: " + xmitter);

        transmitters = new int[]{xmitter};
    }

    public LircTransmitter(boolean[] ports) {
        this(parseBoolean(ports));
    }

    private static List<Integer> parseBoolean(boolean[] ports) {
        List<Integer> prts = new ArrayList<>();
        for (int i = 0; i < ports.length; i++)
            if (ports[i])
                prts.add(i + 1);
        return prts;
    }

    public LircTransmitter(String str) {
        this(parseString(str));
    }

    private static List<Integer> parseString(String str) {
        if (str.equals("default"))
            return null;
        String[] pieces = str.split("\\D+");
        List<Integer> result = new ArrayList<>();
        for (String s : pieces)
            result.add(Integer.parseInt(s));
        return result;
    }

    public LircTransmitter(List<Integer> ports) {
        if (ports == null || ports.isEmpty())
            transmitters = null;
        else {
            transmitters = new int[ports.size()];
            for (int i = 0; i < ports.size(); i++)
                transmitters[i] = ports.get(i);
        }
    }

    @Override
    public String toString() {
        if (isTrivial())
            return "";

        StringBuilder s = new StringBuilder();
        for (int i = 0; i < transmitters.length; i++) {
            if (i > 0)
                s.append(" ");
            s.append(transmitters[i]);
        }

        return s.toString();
    }

    public int toMask() {
        if (transmitters == null)
            return NOMASK;

        int mask = 0;
        for (int i = 0; i < transmitters.length; i++)
            mask |= (1 << i);
        return mask;
    }

    public boolean isTrivial() {
        return transmitters == null || transmitters.length == 0;
    }
}
