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
package org.harctoolbox.IrpMaster;

import java.util.ArrayList;

public class BitStream extends PrimitiveIrStreamItem {

    private int length;
    private long data[];

    public BitStream(Protocol env) {
        super(env);
        data = new long[1];
        data[0] = 0L;
        length = 0;
    }

    @Override
    public String toString() {
        if (data.length == 1)
            return "BitStream, length = " + length + ", data = " + data[0] + " = " + Long.toBinaryString(data[0]);
        else {
            StringBuilder dataString = new StringBuilder();
            dataString.append("[ ");
            StringBuilder binString = new StringBuilder();
            for (int i = data.length - 1; i >= 0; i--) {
                dataString.append(Long.toString(data[i])).append(" ");
                binString.append(Long.toBinaryString(data[i])).append(" ");
            }
            return "BitStream, length = " + length + ", data = " + dataString + "] = " + binString;
        }
    }

    public void add(BitField bitField) throws IncompatibleArgumentException {
        add(bitField, environment.getBitDirection());
    }

    public void add(BitField bitField, BitDirection bitDirection) throws IncompatibleArgumentException {
        if (bitField.isInfinite())
            throw new IncompatibleArgumentException("Infinite bitfields cannot be converted to bitstreams.");

        long newData = this.environment.getBitDirection() == BitDirection.msb
                    ? bitField.toLong()
                    : IrpUtils.reverse(bitField.toLong(), bitField.getWidth());
        length += bitField.getWidth();

        if (length > Long.SIZE) {
            // "abnormal" case
            if (longsNeeded(length) > data.length) {
                // need to extend
                long[] newdata = new long[data.length + 1];
                System.arraycopy(data, 0, newdata, 0, data.length);
                newdata[data.length] = 0L;
                data = newdata;
            }
            for (int i = data.length - 1; i > 0; i--) {
                long x = data[i] << bitField.getWidth() | getLeftmostBits(data[i-1], bitField.getWidth());
                data[i] = x;
            }
        }
        data[0] = data[0] << bitField.getWidth() | newData;
    }

    private long getLeftmostBits(long x, int n) {
        return x >> (Long.SIZE - n) & ((1L << n) - 1L);
    }

    // really ceal(n/Long.Size)
    private int longsNeeded(int n) {
        return n/Long.SIZE + ((n % Long.SIZE == 0) ? 0 : 1);
    }

    /** Extracts bit n*chunksize .. (n+1)*chunksize-1 */
    private int getChunkNo(int n, int chunksize) {
        if (n < 0 || (length > 0 && (n+1)*chunksize-1 >= length))
            throw new IndexOutOfBoundsException("Illegal bit " + n + " in getChunkNo");
        // If a chunk goes over the data[] limits, this has to be implemented extra.
        // I have more interesting thing to do :-)
        if (((n+1)*chunksize-1)/Long.SIZE != (n*chunksize)/Long.SIZE)
            throw new RuntimeException("Case not implemented");
        int chunk = (int)(data[n*chunksize/Long.SIZE] >> n*chunksize) & ((1 << chunksize)- 1);
        return chunk;
    }

    @Override
    public ArrayList<PrimitiveIrStreamItem> evaluate(BitSpec bitSpec) throws UnassignedException, IncompatibleArgumentException {
        debugBegin();
        if (bitSpec == null)
            throw new UnassignedException("BitStream " + toString() + " has no associated BitSpec, cannot compute IRStream");
        ArrayList<PrimitiveIrStreamItem> list = new ArrayList<>();
        if (length % bitSpec.getChunkSize() != 0)
            throw new IncompatibleArgumentException("chunksize (= " + bitSpec.getChunkSize() + ") does not divide bitstream length (= " + length + ").");

        int noChunks = length/bitSpec.getChunkSize();
        for (int n = 0; n < noChunks; n++) {
            int chunkNo = noChunks - n - 1;
            PrimaryIrStream irs = bitSpec.getBitIrsteam(getChunkNo(chunkNo, bitSpec.getChunkSize()));
            ArrayList<PrimitiveIrStreamItem> items = irs.evaluate(null);
            list.addAll(items);
        }
        Debug.debugBitStream(toString());
        debugEnd(list);
        return list;
    }

    @Override
    public boolean isEmpty() {
        return length == 0;
    }
}
