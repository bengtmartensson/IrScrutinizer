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
 * This class implements BitSpecs, as described in Chapter 7.
 *
 */
public class BitSpec extends IrStreamItem {
    
    // Number of bits encoded
    private int chunkSize;
    
    private PrimaryIrStream[] bitCodes;
    
    // Computes the upper integer part of the 2-logarithm of the integer n.
    // Treat n = 1 differently, since coding on a one-letter alphaber is ... special.
    private static int computeNoBits(int n) {
        if (n == 1)
            return 1;
        int x = n-1;
        int m;
        for (m = 0; x != 0; m++)
            x >>= 1;
        return m;
    }
    
    public BitSpec(Protocol env, PrimaryIrStream[] s) {
        super(env);
        bitCodes = s;
        chunkSize = computeNoBits(s.length);
    }
    
    public BitSpec(Protocol env, ArrayList<PrimaryIrStream> list) {
        this(env, list.toArray(new PrimaryIrStream[list.size()]));
    }
    
    public PrimaryIrStream getBitIrsteam(int index) throws IncompatibleArgumentException {
        if (index >= bitCodes.length)
            throw new IncompatibleArgumentException("Cannot encode " + index + " with current bitspec.");
        return this.bitCodes[index];
    }
    
    public void assignBitSpecs(BitSpec bitSpec) {
        for (PrimaryIrStream pis : bitCodes) {
            pis.assignBitSpecs(bitSpec);
        }
    }
    
    @Override
    public String toString() {
        if (bitCodes == null || bitCodes.length == 0)
            return "<null>";

        StringBuilder s = new StringBuilder();
        s.append("<").append(bitCodes[0]);
        for (int i = 1; i < bitCodes.length; i++) {
            //s += (i > 0 ? "; " : "") + "bitCodes[" + i + "]=" + bitCodes[i];
            s.append("|").append(bitCodes[i]);
        }
        return s.append(">").toString();
    }
    
    public int getChunkSize() {
        return chunkSize;
    }

    @Override
    public boolean isEmpty() {
        return bitCodes.length == 0;
    }
}
