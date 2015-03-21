/*
Copyright (C) 2011, 2012, 2013 Bengt Martensson.

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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

/**
 * This class allows for the creation of rendered IR signals in the ICT Format, used by the IRScope.
 *
 */
public class ICT {
    private static String lineEnding = System.getProperty("line.separator");
    private static final double seconds2microseconds = 1000000.0;

    private ICT() {
    }

    private static String numbersString(int[] array, double frequency) {
        StringBuilder s = new StringBuilder();
        if (array.length > 0) {
            for (int i = 0; i < array.length; i++) {
                int x = Math.abs(array[i]);
                s.append((i % 2 == 0)
                        ? ("+" + x + "," + (int) Math.round(x*frequency/seconds2microseconds))
                        : -x);
                if (i < array.length - 1)
                    s.append(lineEnding);
            }
        }
        return s.toString();
    }

    public static String ictString(ModulatedIrSequence seq) {
        String head = String.format(
                "irscope 0%scarrier_frequency %d%ssample_count %d%s",
                lineEnding, (int) Math.round(seq.getFrequency()),
                lineEnding, seq.getLength(), lineEnding);
        return head + numbersString(seq.toInts(), seq.getFrequency());
    }

    /**
     * Parses an input file into an ModulatedIrSequence.
     * Note that the ICT file may contain several IR signals in the usual sense.
     * The are returned as one long IrSequence consisting of all the data concatenated.
     *
     * @param reader
     * @return IrSignal consisting of all data in the input concatenated.
     * @throws IOException
     * @throws IncompatibleArgumentException
     */
    public static ModulatedIrSequence parse(BufferedReader reader) throws IOException, IncompatibleArgumentException {
        //int[] data = null;
        ArrayList<Integer> data = new ArrayList<Integer>();
        int index = 0;
        int frequency = -1;
        int sample_count = -1;
        while (true) {
            String line = reader.readLine();
            if (line == null)
                break;
            String[] args = line.split("[ ,=]");
            if (args[0].equals("carrier_frequency"))
                frequency = Integer.parseInt(args[1]);
            else if (args[0].equals("sample_count"))
                sample_count = Integer.parseInt(args[1]);
                //data = new int[Integer.parseInt(args[1])];
            else if (args[0].startsWith("+"))
                data.add(Integer.parseInt(args[0].substring(1)));
                //data[index++] = Integer.parseInt(args[0].substring(1));
            else if (args[0].equals("pulse"))
                data.add(Integer.parseInt(args[1]));
            else if (args[0].equals("note") || args[0].equals("irscope"))
                ;
            else if (args[0].startsWith("-"))
                //data[index++] = (int) (((double) Integer.parseInt(args[0].substring(1)) * frequency) / 1000000.0 + 0.5);
                //data[index++] = Integer.parseInt(args[0].substring(1));
                data.add(Integer.parseInt(args[0].substring(1)));
            else if (args[0].equals("space"))
                if (data.isEmpty())
                    ; // Ignore leading gaps
                else
                    data.add(Integer.parseInt(args[1]));
            else if (args[0].startsWith("#"))
                ; // Comment, ignore
            else
                System.err.println("Warning: Ignored line: " + line);
        }
        if (data.size() % 2 == 1) {
            System.err.println("Warning: Last sample was pulse, appending a 100000 microsecond gap");
            data.add(100000);
        }
        int [] dataArray = new int[data.size()];
        for (int i = 0; i < data.size(); i++)
            dataArray[i] = data.get(i);

        if (data.size() != sample_count)
            System.err.println("Warning: sample_count erroneous or missing");
        if (frequency < 0) {
            frequency = (int) IrpUtils.defaultFrequency;
            System.err.println("Warning: carrier_frequency missing, assuming " + (int) IrpUtils.defaultFrequency);
        }
        return new ModulatedIrSequence(new IrSequence(dataArray), (double) frequency, (double) IrpUtils.invalid);
    }

    public static ModulatedIrSequence parse(File file) throws FileNotFoundException, IOException, IncompatibleArgumentException {
        return parse(new BufferedReader(new InputStreamReader(new FileInputStream(file), IrpUtils.dumbCharset)));
    }

    public static void main(String[] args) {
        try {
            ModulatedIrSequence seq = parse(new File(args[0]));
            IrSignal ip = seq.toIrSignal();
            System.out.println(ip.ccfString());
            for (DecodeIR.DecodedSignal ds : DecodeIR.decode(ip))
                System.out.println(ds);
        } catch (IncompatibleArgumentException ex) {
            System.err.println(ex.getMessage());
        } catch (FileNotFoundException ex) {
            System.err.println(ex.getMessage());
        } catch (IOException ex) {
            System.err.println(ex.getMessage());
        }
    }
}
