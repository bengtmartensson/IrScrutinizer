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
 * This class implements Extents as per Chapter 4.
 *
 */
public class Extent extends Duration {
    
    public Extent(Protocol env, double time, String unit) {
        super(env, time, unit, DurationType.extent);
    }
    
    @Override
    public double evaluate(double elapsed) throws IncompatibleArgumentException{
        double time = super.evaluate(0.0) - elapsed;
        if (time < 0)
            throw new IncompatibleArgumentException("Argument of extent smaller than actual duration.");
        return super.evaluate(0.0) - elapsed;
    }
    
    @Override
    public double evaluate_sign(double elapsed) throws IncompatibleArgumentException {
        return -evaluate(elapsed);
    }
}
