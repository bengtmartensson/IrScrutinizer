/*
Copyright (C) 2013,2014 Bengt Martensson.

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
package org.harctoolbox.irscrutinizer.importer;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.Serializable;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import org.harctoolbox.IrpMaster.ExchangeIR;
import org.harctoolbox.IrpMaster.IrSignal;
import org.harctoolbox.IrpMaster.IrpMasterException;
import org.harctoolbox.IrpMaster.IrpUtils;
import org.harctoolbox.girr.Command;

/**
 * This class allows for the creation of rendered IR signals in the ICT Format, used by the IRScope.
 *
 */
public class IctImporter extends RemoteSetImporter implements IReaderImporter, Serializable {

    private static int invalid = -1;
    private static final int lengthInsertedGap = 100000;
    private static final long serialVersionUID = 1L;
    private int lineNumber;
    private int frequency = invalid;
    private int sampleCount = invalid;
    private int noSamples;
    private boolean hasComplainedAboutMissingFrequency;

    /**
     * Parses an input file into an ModulatedIrSequence.
     * Note that the ICT file may contain several IR signals in the usual sense.
     * The are returned as one long IrSequence consisting of all the data concatenated.
     *
     */
    public IctImporter() {
    }

    @Override
    @SuppressWarnings("empty-statement")
    public void load(Reader reader, String origin) throws IOException {
        prepareLoad(origin);
        hasComplainedAboutMissingFrequency = false;
        BufferedReader bufferedReader = new BufferedReader(reader);
        lineNumber = 0;
        noSamples = 0;
        String name = "unnamed";
        ArrayList<Integer> data = new ArrayList<>();
        while (true) {
            String line = bufferedReader.readLine();
            if (line == null)
                break;
            lineNumber++;
            String[] chunks = line.split("[ ,=]");
            if (chunks[0].equals("carrier_frequency"))
                frequency = Integer.parseInt(chunks[1]);
            else if (chunks[0].equals("sample_count"))
                sampleCount = Integer.parseInt(chunks[1]);
            else if (chunks[0].startsWith("+")) {
                data.add(Integer.parseInt(chunks[0].substring(1)));
                noSamples++;
            } else if (chunks[0].equals("pulse")) {
                data.add(Integer.parseInt(chunks[1]));
                noSamples++;
            } else if (chunks[0].equals("note")) {
                processSignal(data, name, origin);
                data.clear();
                name = chunks[1];
            } else if (chunks[0].equals("irscope"))
                ;
            else if (chunks[0].startsWith("-")) {
                data.add(Integer.parseInt(chunks[0].substring(1)));
                noSamples++;
            } else if (chunks[0].equals("space"))
                if (data.isEmpty())
                    ; // Ignore leading gaps
                else {
                    data.add(Integer.parseInt(chunks[1]));
                    noSamples++;
                }
            else if (chunks[0].startsWith("#"))
                ; // Comment, ignore
            else
                System.err.println("Warning: Ignored line: " + lineNumber);
        }
        processSignal(data, name, origin);
        if (noSamples != sampleCount)
            System.err.println("Warning: sample_count erroneous or missing (expected " + noSamples + " was " + sampleCount + ")");
        setupRemoteSet();
    }

    private void processSignal(ArrayList<Integer> data, String name, String origin) {
        if (data.isEmpty())
            return;

        if (data.size() % 2 == 1) {
            System.err.println("Warning: Last sample was pulse, appending a " + lengthInsertedGap + " microsecond gap");
            data.add(lengthInsertedGap);
        }
        int[] dataArray = new int[data.size()];
        for (int i = 0; i < data.size(); i++) {
            dataArray[i] = data.get(i);
        }

        if (frequency < 0 && hasComplainedAboutMissingFrequency) {
            hasComplainedAboutMissingFrequency = true;
            frequency = (int) IrpUtils.defaultFrequency;
            System.err.println("Warning: carrier_frequency missing, assuming " + (int) IrpUtils.defaultFrequency);
        }
        IrSignal irSignal = ExchangeIR.interpretIrSequence(dataArray, (double) frequency, isInvokeRepeatFinder(), isInvokeAnalyzer());
        Command command = new Command(uniqueName(name), origin == null ? "ICT import" : ("ICT import from " + origin), irSignal, isGenerateCcf(), isInvokeDecodeIr());
        addCommand(command);
    }

    public static Collection<Command> importer(File file) throws IOException, ParseException, IrpMasterException {
        IctImporter imp = new IctImporter();
        imp.load(file);
        return imp.getCommands();
    }

    public static Collection<Command> importer(BufferedReader reader) throws IOException {
        IctImporter imp = new IctImporter();
        imp.load(reader, null);
        return imp.getCommands();
    }

    @Override
    public String[][] getFileExtensions() {
        return new String[][] { new String[]{ "IrScope files (*.ict)", "ict"}};
    }

    @Override
    public String getFormatName() {
        return "IrScope ict";
    }
}
