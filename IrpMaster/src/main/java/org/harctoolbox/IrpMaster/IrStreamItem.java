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

/**
 * This class is an abstract superclass of the things that make up an IRStream (see "Directly known subclasses").
 *
 * @author Bengt Martensson
 */
public abstract class IrStreamItem {

    protected Protocol environment;
    protected int noAlternatives = 0;

    public abstract boolean isEmpty() throws IncompatibleArgumentException;

    public IrStreamItem(Protocol env) {
        environment = env;
        Debug.debugIrStreamItems(this.getClass().getSimpleName() + " constructed.");
    }

    public IrStreamItem() {
        Debug.debugIrStreamItems(this.getClass().getSimpleName() + " constructed.");
    }

    /**
     * To be overridden in Variation
     * @return noAlternatives
     */
    public int getNoAlternatives() {
        return noAlternatives;
    }
}
