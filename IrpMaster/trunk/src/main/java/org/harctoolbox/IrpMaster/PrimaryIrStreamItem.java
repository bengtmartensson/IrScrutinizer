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

public abstract class PrimaryIrStreamItem extends IrStreamItem {
    protected PrimaryIrStreamItem(Protocol env) {
        super(env);
    }

    /**
     * Processes the Item's internal IRStreamItems, ideally just to Durations.
     *
     * @param bitSpec
     * @return ArrayList&lt;PrimitiveIrStreamItem&gt;
     * @throws IncompatibleArgumentException
     * @throws UnassignedException
     */
    public abstract ArrayList<PrimitiveIrStreamItem> evaluate(BitSpec bitSpec) throws IncompatibleArgumentException, UnassignedException;

    protected void debugBegin() {
        if (Debug.getInstance().debugOn(Debug.Item.Evaluate))
           Debug.debugEvaluate("Entering " + this.getClass().getSimpleName() + ": " + toString());
    }

    protected void debugEnd(Object o) {
        if (Debug.getInstance().debugOn(Debug.Item.Evaluate))
            Debug.debugEvaluate("Leaving " + this.getClass().getSimpleName() + ": " + o.toString());
    }
}
