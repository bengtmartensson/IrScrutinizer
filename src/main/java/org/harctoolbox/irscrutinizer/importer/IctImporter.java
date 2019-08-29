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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Serializable;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.harctoolbox.girr.Command;
import org.harctoolbox.ircore.InvalidArgumentException;
import org.harctoolbox.ircore.IrCoreException;
import org.harctoolbox.ircore.IrCoreUtils;
import org.harctoolbox.ircore.IrSignal;
import org.harctoolbox.ircore.ModulatedIrSequence;
import org.harctoolbox.irp.IrpException;
import org.harctoolbox.irscrutinizer.InterpretString;

/**
 * This class allows for the creation of rendered IR signals in the ICT Format, used by the IRScope.
 *
 */
public class IctImporter extends RemoteSetImporter implements IReaderImporter, Serializable {

    private static final int invalid = -1;
    private static final int lengthInsertedGap = 100000;
    private static final int IRSCOPE_ENDING_GAP = -500000;
    private static final String IRSCOPE_ENDING_STRING = Integer.toString(IRSCOPE_ENDING_GAP);

    public static Collection<Command> importer(File file, String charsetName) throws IOException, ParseException, InvalidArgumentException {
        IctImporter imp = new IctImporter();
        imp.load(file, file.getCanonicalPath(), charsetName);
        return imp.getCommands();
    }

    public static Collection<Command> importer(BufferedReader reader, String orig) throws IOException, InvalidArgumentException {
        IctImporter imp = new IctImporter();
        imp.load(reader, orig);
        return imp.getCommands();
    }

    public static void main(String[] args) {
        for (String s : args) {
            try {
                Collection<Command> cmds = parse(s);
                for (Command cmd : cmds) {
                    System.out.println(cmd.getName());
                    System.out.println(cmd.toIrSignal());
                    System.out.println();
                }
            } catch (IOException | IrCoreException | IrpException ex) {
                Logger.getLogger(IctImporter.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    private static Collection<Command> parse(String s) throws FileNotFoundException, IOException, InvalidArgumentException {
        Collection<Command> cmds;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(IrCoreUtils.getInputSteam(s), IrCoreUtils.DEFAULT_CHARSET))) {
            cmds = importer(reader, s);
        }
        return cmds;
    }

    private int lineNumber;
    private int anonymousNumber;
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
    public void load(Reader reader, String origin) throws IOException, InvalidArgumentException {
        prepareLoad(origin);
        hasComplainedAboutMissingFrequency = false;
        BufferedReader bufferedReader = new BufferedReader(reader);
        anonymousNumber = 0;
        lineNumber = 0;
        noSamples = 0;
        String name = "unnamed_" + anonymousNumber;
        ArrayList<Integer> data = new ArrayList<>(64);
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
            } else if (chunks[0].equals(IRSCOPE_ENDING_STRING)) {
                data.add(IRSCOPE_ENDING_GAP);
                noSamples++;
                processSignal(data, name, origin);
                data.clear();
                anonymousNumber++;
                name = "unnamed_" + anonymousNumber;
            } else if (chunks[0].equals("note")) {
                if (!data.isEmpty()) {
                    processSignal(data, name, origin);
                    data.clear();
                }
                chunks = line.split("=");
                if (chunks.length >= 2) {
                    name = chunks[1];
                    anonymousNumber--;
                }
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
        if (noSamples != sampleCount) {
            if (sampleCount == -1)
                System.err.println("Warning: sample_count missing (" + noSamples + " samples found)");
            else
                System.err.println("Warning: sample_count erroneous (expected " + sampleCount + ", found " + noSamples + ")");
        }
        setupRemoteSet();
    }

    private void processSignal(ArrayList<Integer> data, String name, String origin) throws InvalidArgumentException {
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
            frequency = (int) ModulatedIrSequence.DEFAULT_FREQUENCY;
            System.err.println("Warning: carrier_frequency missing, assuming " + frequency);
        }
        ModulatedIrSequence seq = new ModulatedIrSequence(dataArray, (double) frequency);
        IrSignal irSignal = InterpretString.interpretIrSequence(seq, isInvokeRepeatFinder(), isInvokeCleaner(), getAbsoluteTolerance(), getRelativeTolerance());
        Command command = new Command(uniqueName(name), origin == null ? "ICT import" : ("ICT import from " + origin), irSignal);
        addCommand(command);
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
