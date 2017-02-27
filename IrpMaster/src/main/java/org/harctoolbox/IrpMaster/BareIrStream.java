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
package org.harctoolbox.IrpMaster;

import java.util.ArrayList;

/**
 * This class implements Irstream as of Chapter 6.
 *
 * @author Bengt Martensson
 */
public class BareIrStream extends IrStreamItem {

    protected ArrayList<IrStreamItem> irStreamItems = null;
    //protected BitSpec bitSpec;
    private int noAlternatives = 0;

    public BareIrStream(Protocol env) {
        this(env, null, null, 0);
    }

    public BareIrStream(Protocol env, ArrayList<IrStreamItem>items, BitSpec bitSpec, int noAlternatives) {
        super(env);
        this.irStreamItems = items;
        this.noAlternatives = noAlternatives;
        //this.bitSpec = bitSpec;
    }

    //@Override
    boolean stringOk(String s) {
        return s.startsWith("(");
    }

    public void concatenate(BareIrStream bareIrStream) {
        irStreamItems.addAll(bareIrStream.irStreamItems);
    }

    @Override
    public boolean isEmpty() {
        return irStreamItems.isEmpty();
    }

    @Override
    public int getNoAlternatives() {
        return noAlternatives;
    }
    /*
    private static ArrayList<PrimaryIrStreamItem> toPrimaryIrStreamItems(Protocol environment, ArrayList<IrStreamItem> irstreamItems) {
        ArrayList<PrimaryIrStreamItem> primaryItems = new ArrayList<PrimaryIrStreamItem>();
        for (IrStreamItem item : irstreamItems) {
            BitStream bitStream = null;
            String type = item.getClass().getSimpleName();
            if (type.equals("Bitfield")) {
                if (bitStream == null)
                    bitStream = new BitStream(environment);

                bitStream.add((BitField)item, environment.getBitDirection());
            } else if (type.equals("Duration") || type.equals("Extent") || type.equals("IRStream")) {
                if (bitStream != null) {
                    primaryItems.add(bitStream);
                    bitStream = null;
                }
                primaryItems.add((PrimaryIrStreamItem)item);
            } else {
                throw new RuntimeException("This-cannot-happen-item found: " + type);
                //assert false;
            }
            if (bitStream != null) {
                    primaryItems.add(bitStream);
                    bitStream = null;
            }
        }
        return primaryItems;
    }*/

    @Override
    public String toString() {
        return irStreamItems.toString();
    }

    /*private static ArrayList<Double> normalize(ArrayList<Double> list, boolean nukeLeadingZeros) {
        if (list == null || list.isEmpty())
            return list;

        // Nuke leading gaps
        while (nukeLeadingZeros && list.size() > 1 && list.get(0) <= 0)
            list.remove(0);

        for (int i = 0; i < list.size(); i++) {
            while (i + 1 < list.size() && equalSign(list.get(i), list.get(i+1))) {
                double val = list.get(i) + list.get(i+1);
                list.remove(i);
                list.remove(i);
                list.add(i, val);
            }
        }
        return list;
    }*/
}
