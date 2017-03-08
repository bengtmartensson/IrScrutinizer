/*
Copyright (C) 2017 Bengt Martensson.

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

package org.harctoolbox.harchardware.ir;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.Reader;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.harctoolbox.IrpMaster.IncompatibleArgumentException;
import org.harctoolbox.IrpMaster.IrSequence;
import org.harctoolbox.IrpMaster.IrpUtils;

/**
 * This class contains a parser for mode2 files.
 */

public class Mode2Parser {
    private final static Logger logger = Logger.getLogger(Mode2Parser.class.getName());
    /**
     * Added at the end of IR sequences that would otherwise end with a flash.
     */
    public static final int DUMMYGAP = 50000;
    public static final int DEFAULTTHRESHOLD = 100000; // 100ms

    public static void main(String[] args) {
        try {
            Mode2Parser parser = args.length < 2 ? new Mode2Parser(System.in, false, Integer.parseInt(args[0]))
                    : new Mode2Parser(new File(args[0]), false, Integer.parseInt(args[1]));
            List<IrSequence> seqs = parser.readIrSequencesUntilEOF();
            int i = 0;
            for (IrSequence part : seqs)
                System.out.println("signal_" + i++ + ":" + new IrSequence(part).toPrintString(true)); // Easy to parse for IrScrutinizer
        } catch (IOException ex) {
            Logger.getLogger(Mode2Parser.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private final LineNumberReader reader;
    private int threshold;
    private boolean verbose;
    private boolean valid;

    public Mode2Parser(Reader reader, boolean verbose, int threshold) {
        this.reader = new LineNumberReader(reader);
        this.threshold = threshold;
        this.verbose = verbose;
        this.valid = true;
    }

    public Mode2Parser(File file, boolean verbose, int threshold) throws FileNotFoundException {
        this(new InputStreamReader(new FileInputStream(file), IrpUtils.dumbCharset), verbose, threshold);
    }

    public Mode2Parser(InputStream stream, boolean verbose, int threshold) {
        this(new InputStreamReader(stream, IrpUtils.dumbCharset), verbose, threshold);
    }

    public boolean isValid() {
        return valid;
    }

    public void close() throws IOException {
        if (reader != null) {
            synchronized (reader) {
                reader.close();
            }
            valid = false;
        }
    }

    public List<IrSequence> readIrSequencesUntilEOF() throws IOException {
        List<IrSequence> result = new ArrayList<>(8);
        while (true) {
            IrSequence irSequence;
            try {
                irSequence = readIrSequence();
            } catch (ParseException ex) {
                continue;
            }
            if (irSequence == null)
                break;
            result.add(irSequence);
        }
        return result;
    }

    public IrSequence readIrSequence() throws ParseException, IOException {
        if (!valid)
            return null;

        List<Integer> list = new ArrayList<>(1024);
        String line;
        loop:
        while (true) {
            synchronized (reader) {
                line = reader.readLine();
            }
            if (verbose)
                System.err.println(line);
            if (line == null) {
                valid = false;
                break;
            }
            line = line.trim();
            if (line.isEmpty() || line.startsWith("#"))
                continue;

            // Quirk in mode2, see https://sourceforge.net/p/lirc/tickets/272/
            if (line.startsWith("Using "))
                continue;

            String[] parts = line.split("\\s+");
            try {
                int value;
                switch (parts[0]) {
                    case "space":
                        if (list.isEmpty())
                            continue;

                        value = Integer.parseInt(parts[1]);
                        if (list.size() % 2 != 0)
                            list.add(value);
                        else
                            list.set(list.size() - 1, list.get(list.size() - 1) + value);
                        if (value >= threshold)
                            break loop;
                        break;
                    case "pulse":
                        value = Integer.parseInt(parts[1]);
                        if (list.size() % 2 == 0)
                            list.add(value);
                        else
                            list.set(list.size() - 1, list.get(list.size() - 1) + value);
                        break;
                    default:
                        throw new ParseException("Unknown keyword: " + parts[0], reader.getLineNumber());
                }
            } catch (NumberFormatException ex) {
                throw new ParseException(ex.getMessage(), reader.getLineNumber());
            }
        }
        if (list.size() % 2 != 0)
            list.add(DUMMYGAP);

        double[] data = new double[list.size()];
        for (int i = 0; i < data.length; i++)
            data[i] = list.get(i);
        try {
            return new IrSequence(data);
        } catch (IncompatibleArgumentException ex) {
            assert false;
            return null;
        }
    }

    /**
     * @param threshold the threshold to set
     */
    public void setThreshold(int threshold) {
        this.threshold = threshold;
    }

    /**
     * @param verbose the verbose to set
     */
    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }
}
