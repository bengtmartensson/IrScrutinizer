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

public class PrimaryIrStream extends PrimaryIrStreamItem {

    private ArrayList<PrimaryIrStreamItem> irStreamItems;
    private BitSpec bitSpec;
    private boolean hasAssignmentContent = false;

    public PrimaryIrStream(Protocol env) {
        super(env);
        irStreamItems = new ArrayList<>(16);
    }

    public PrimaryIrStream(Protocol env, boolean hasAssignmentContent) {
        this(env);
        this.hasAssignmentContent = hasAssignmentContent;
    }

    public PrimaryIrStream(Protocol env, ArrayList<PrimaryIrStreamItem>items, BitSpec bitSpec, int noAlternatives) {
        super(env);
        this.bitSpec = bitSpec;
        this.irStreamItems = items;
        this.noAlternatives = noAlternatives;
    }

    public PrimaryIrStream(Protocol env, PrimaryIrStream src, BitSpec bitSpec) {
        this(env, src, bitSpec, 0);
    }

    public PrimaryIrStream(Protocol env, PrimaryIrStream src, BitSpec bitSpec, int noAlternatives) {
        this(env, (src != null) ? src.irStreamItems : new ArrayList<PrimaryIrStreamItem>(16), bitSpec, noAlternatives);
    }

    @Override
    public String toString() {
        return irStreamItems.toString();
    }

    @Override
    public boolean isEmpty() {
        return irStreamItems.isEmpty() && ! hasAssignmentContent;
    }

    public void concatenate(PrimaryIrStream irStream) {
        irStreamItems.addAll(irStream.irStreamItems);
    }

    public void assignBitSpecs() {
        assignBitSpecs(null);
    }

    public void assignBitSpecs(BitSpec parentBitSpec) {
        if (bitSpec == null) {
            bitSpec = parentBitSpec;
            Debug.debugBitSpec("BitSpec " + bitSpec + "was assigned to " + this);
        } else {
            bitSpec.assignBitSpecs(parentBitSpec);
        }

        irStreamItems.stream().filter((item) -> (item.getClass().getSimpleName().equals("PrimaryIrStream"))).forEachOrdered((item) -> {
            ((PrimaryIrStream) item).assignBitSpecs(bitSpec);
        });
    }

    @Override
    public ArrayList<PrimitiveIrStreamItem> evaluate(BitSpec bitSpec) throws IncompatibleArgumentException, UnassignedException {
        BitSpec bs = (bitSpec == null || this.bitSpec != null) ? this.bitSpec : bitSpec;
        debugBegin();
        ArrayList<PrimitiveIrStreamItem> list = new ArrayList<>(16);
        BitStream bitStream = new BitStream(environment);
        for (PrimaryIrStreamItem item : irStreamItems)
            if (item.getClass().getSimpleName().equals("BitField"))
                bitStream.add((BitField) item);
            else {
                if (!bitStream.isEmpty()) {
                    list.addAll(bitStream.evaluate(bs));
                    bitStream = new BitStream(environment);
                }

                list.addAll(item.evaluate(bs));
            }
        if (!bitStream.isEmpty())
            list.addAll(bitStream.evaluate(bs));

        debugEnd(list);
        return list;
    }

    public IrSequence toIrSequence() throws IncompatibleArgumentException, UnassignedException {
        ArrayList<PrimitiveIrStreamItem> durationList = evaluate(null);
        if (durationList.size() % 2 == 1)
            durationList.add(new Duration(environment, 0.001, DurationType.gap)); // ????
        int size = durationList.size();
        ArrayList<Double> data = new ArrayList<>(size);
        double elapsed = 0;
        boolean seenPositive = false;

        for (PrimitiveIrStreamItem item : durationList) {
            try {
                if (!Class.forName("org.harctoolbox.IrpMaster.Duration").isInstance(item))
                    throw new RuntimeException("Programming error detected: This cannot happen");

            double duration = ((Duration) item).evaluate_sign(elapsed);
            if (duration > 0)
                seenPositive = true;

            if (seenPositive) {
                elapsed += Math.abs(duration);
                data.add(duration);
            }
            if (Class.forName("org.harctoolbox.IrpMaster.Extent").isInstance(item))
                elapsed = 0;
            } catch (ClassNotFoundException ex) {
                // This cannot happen
                assert false;
            }
        }
        return new IrSequence(data);
    }
}
