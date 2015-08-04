/*
Copyright (C) 2013, 2014 Bengt Martensson.

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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.Reader;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import org.harctoolbox.IrpMaster.IrSignal;
import org.harctoolbox.IrpMaster.IrpMasterException;
import org.harctoolbox.IrpMaster.IrpUtils;
import org.harctoolbox.girr.Command;
import org.harctoolbox.irscrutinizer.Utils;

/**
 * This class does something interesting and useful. Or not...
 *
 * Columns are numbered starting with 1.
 */
public class CsvRawImporter extends CsvImporter {
    private static final long serialVersionUID = 1L;
    private int codeColumn;
    private boolean includeTail;

    /**
     * @param codeColumn the codeColumn to set
     */
    public void setCodeColumn(int codeColumn) {
        this.codeColumn = codeColumn;
    }

    /**
     * @param includeTail the includeTail to set
     */
    public void setIncludeTail(boolean includeTail) {
        this.includeTail = includeTail;
    }

    public CsvRawImporter(int separatorIndex, int nameColumn, boolean nameMultiColumn, int codeColumn, boolean includeTail) {
        this(CsvImporter.getSeparator(separatorIndex), nameColumn, nameMultiColumn, codeColumn, includeTail);
    }

    public CsvRawImporter(String separator, int nameColumn, boolean nameMultiColumn, int codeColumn, boolean includeTail) {
        super(separator, nameColumn, nameMultiColumn);
        this.separator = separator;
        this.nameColumn = nameColumn;
        this.codeColumn = codeColumn;
        this.includeTail = includeTail;
    }

    private CsvRawImporter() { // ???
        this(0, 0, false, 1, false);
    }

    @Override
    public void load(File file, String origin) throws IOException, ParseException {
        try (FileInputStream stream = new FileInputStream(file)) {
            load(stream, origin);
        }
    }

    private String[] csvSplit(String line, String separator) {
        StringBuilder str = new StringBuilder(line.trim());
        ArrayList<String> chunks = new ArrayList<>();

        while (str.length() > 0) {
            if (str.length() >= separator.length() && separator.equals(str.substring(0, separator.length())))
                str.delete(0, separator.length());
            while (str.length() > 0 && Character.isWhitespace(str.charAt(0))) {
                str.deleteCharAt(0);
            }
            String chunk;
            if (str.length() > 0 && str.charAt(0) == '"') {
                int n = str.indexOf("\"", 1);
                chunk = str.substring(1, n);
                str.delete(0, n+1);
            } else {
                int n = str.indexOf(separator);
                if (n == -1) {
                    chunk = str.toString();
                    str.setLength(0);
                } else {
                    chunk = str.substring(0, n);
                    str.delete(0, n);
                }
            }
            chunks.add(chunk.trim());
        }
        return chunks.toArray(new String[chunks.size()]);
    }

    @Override
    public void load(Reader reader, String origin) throws IOException {
        prepareLoad(origin);

        BufferedReader bufferedReader = new BufferedReader(reader);
        lineNo = 1;
        for (String line = bufferedReader.readLine(); line != null; line = bufferedReader.readLine()) {
            String[] chunks = csvSplit(line, separator);
            Command signal = scrutinizeRaw(chunks);
            if (signal != null) {
                addCommand(signal);
            }
            lineNo++;
        }
        setupRemoteSet();
    }

    private Command scrutinizeRaw(String[] chunks) {
        String[] nameArray = gobbleString(chunks, nameColumn, nameMultiColumn, 16, "-");
        String name = join(nameArray);
        if (name.isEmpty())
            return null;
        int offset = nameArray.length - 1;
        String code = gobbleString(chunks, codeColumn, null, includeTail, offset);
        if (code == null || code.isEmpty()) {
            if (isVerbose())
                System.err.println("Empty code in line " + lineNo);
            return null;
        }
        IrSignal irSignal = null;
        try {
            irSignal = Utils.interpretString(code, IrpUtils.defaultFrequency, isInvokeRepeatFinder(), isInvokeAnalyzer());
        } catch (IrpMasterException ex) {
            if (isVerbose())
                System.err.println("Error parsing code in line " + lineNo + " (" + ex.getMessage() + ")");
            return null;
        }
        return irSignal != null ?
                new Command(uniqueName(name), "Line " + lineNo + ", " + origin, irSignal, isGenerateCcf(), isInvokeDecodeIr())
                : null;
    }

    public static Collection<Command> process(File file, String separator, int nameColumn, boolean nameMultiColumn, int codeColumn, boolean includeTail,
            boolean invokeAnalyzer, boolean invokeRepeatFinder, boolean verbose) throws IOException, ParseException, IrpMasterException {
        CsvRawImporter csvImportRaw = new CsvRawImporter(separator, nameColumn, nameMultiColumn, codeColumn, includeTail);
        csvImportRaw.load(file);
        return csvImportRaw.getCommands();
    }

    public static void main(String[] args) {
        try {
            Collection<Command> signals = process(new File(args[0]), " ", 3, false, 6, true, true, true, true);
            for (Command signal : signals)
                System.out.println(signal.toPrintString());
        } catch (IOException | IrpMasterException | ParseException ex) {
            System.err.println(ex.getMessage());
        }
    }
}
