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

package org.harctoolbox.IrpMaster;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/**
 *
 */
public class Cleaner {
    public static IrSequence clean(IrSequence irSequence, int absoluteTolerance, double relativeTolerance) {
        Cleaner cleaner = new Cleaner(irSequence, absoluteTolerance, relativeTolerance);
        return cleaner.toIrSequence();
    }

    public static ModulatedIrSequence clean(ModulatedIrSequence irSequence, int absoluteTolerance, double relativeTolerance) {
        Cleaner cleaner = new Cleaner(irSequence, absoluteTolerance, relativeTolerance);
        return new ModulatedIrSequence(cleaner.toIrSequence(), irSequence.getFrequency(), irSequence.getDutyCycle());
    }

    private int rawData[];
    private List<Integer> dumbTimingsTable;
    private List<Integer> timingsTable;
    private HashMap<Integer, Integer> histogram;
    private int cookedData[];
    private int[] sorted;
    private HashMap<Integer, Integer> lookDownTable;

    private Cleaner(IrSequence irSequence, int absoluteTolerance, double relativeTolerance) {
        rawData = irSequence.toInts();
        createHistogram();
        createDumbTimingsTable(absoluteTolerance, relativeTolerance);
        improveTimingsTable(absoluteTolerance, relativeTolerance);
        createCookedData();
    }

    private Cleaner() {
    }

    private void createHistogram() {
        histogram = new HashMap<>(16);
        for (int d : rawData) {
            int old = histogram.containsKey(d) ? histogram.get(d) : 0;
            histogram.put(d, old + 1);
        }
    }

    private void createDumbTimingsTable(int absoluteTolerance, double relativeTolerance) {
        dumbTimingsTable = new ArrayList<>(16);
        sorted = rawData.clone();
        Arrays.sort(sorted);
        int last = Integer.MIN_VALUE;
        for (int d : sorted) {
            if (!IrpUtils.isEqual(d, last, absoluteTolerance, relativeTolerance)) {
                dumbTimingsTable.add(d);
                last = d;
            }
        }
    }

    private void improveTimingsTable(int absoluteTolerance, double relativeTolerance) {
        lookDownTable = new HashMap<>(16);
        timingsTable = new ArrayList<>(16);
        int indexInSortedTimings = 0;
        for (int timingsIndex = 0; timingsIndex < dumbTimingsTable.size(); timingsIndex++) {
            int dumbTiming = dumbTimingsTable.get(timingsIndex);
            int sum = 0;
            int terms = 0;
            int lastDuration = -1;
            while (indexInSortedTimings < sorted.length
                    && IrpUtils.isEqual(dumbTiming, sorted[indexInSortedTimings], absoluteTolerance, relativeTolerance)) {
                int duration = sorted[indexInSortedTimings];
                indexInSortedTimings++;
                if (duration == lastDuration)
                    continue;
                lastDuration = duration;
                int noHits = histogram.get(duration);
                sum += noHits * duration;
                terms += noHits;
                lookDownTable.put(duration, timingsIndex);
            }
            int average = (int) Math.round(sum/(double)terms);
            timingsTable.add(average);
        }
    }

    private void createCookedData() {
        cookedData = new int[rawData.length];
        for (int i = 0; i < rawData.length; i++)
            cookedData[i] = lookDownTable.get(rawData[i]);
    }

    private int[] toDurations() {
        int[] data = new int[rawData.length];
        for (int i = 0; i < rawData.length; i++)
            data[i] = timingsTable.get(cookedData[i]);
        return data;
    }

    private IrSequence toIrSequence() {
        try {
            return new IrSequence(toDurations());
        } catch (IncompatibleArgumentException ex) {
            throw new InternalError();
        }
    }

}
