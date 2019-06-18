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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Reader;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.harctoolbox.girr.Command;
import org.harctoolbox.girr.GirrException;
import org.harctoolbox.ircore.InvalidArgumentException;
import org.harctoolbox.ircore.IrCoreException;
import org.harctoolbox.ircore.IrSignal;
import org.harctoolbox.irp.IrpException;
import org.harctoolbox.irscrutinizer.InterpretString;

/**
 * This class does something interesting and useful. Or not...
 *
 * Columns are numbered starting with 1.
 */
public class CsvRawImporter extends CsvImporter {

    public static Collection<Command> process(File file, String separator, int nameColumn, boolean nameMultiColumn, int codeColumn, boolean includeTail,
            boolean invokeAnalyzer, boolean invokeRepeatFinder, boolean verbose, String charsetName) throws IOException, ParseException, FileNotFoundException, InvalidArgumentException {
        CsvRawImporter csvImportRaw = new CsvRawImporter(separator, nameColumn, nameMultiColumn, codeColumn, includeTail);
        csvImportRaw.load(file, charsetName);
        return csvImportRaw.getCommands();
    }

    public static void main(String[] args) {
        try {
            Collection<Command> signals = process(new File(args[0]), " ", 3, false, 6, true, true, true, true, "WINDOWS-1252");
            for (Command signal : signals)
                System.out.println(signal.toPrintString());
        } catch (IrpException | GirrException | IrCoreException | IOException | ParseException ex) {
            Logger.getLogger(CsvRawImporter.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private int codeColumn;
    private boolean includeTail;


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

    @Override
    public void load(File file, String origin, String charsetName) throws IOException, ParseException, InvalidArgumentException {
        try (FileInputStream stream = new FileInputStream(file)) {
            load(stream, origin, charsetName);
        }
    }

    // This is probably quite inefficient. Better would be to copy
    // String.split and make the necessary fixes to it.
    private String[] csvSplit(String line, String separator) {
        StringBuilder str = new StringBuilder(line.trim());
        ArrayList<String> chunks = new ArrayList<>(16);
        Pattern pattern = Pattern.compile(separator);

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
                Matcher matcher = pattern.matcher(str);
                boolean success = matcher.find();
                if (success) {
                    chunk = str.substring(0, matcher.start());
                    str.delete(0, matcher.end());
                } else {
                    chunk = str.toString();
                    str.setLength(0);
                }
            }
            chunks.add(chunk.trim());
        }
        return chunks.toArray(new String[chunks.size()]);
    }

    @Override
    public void load(Reader reader, String origin) throws IOException {
        prepareLoad(origin);
        boolean rejectNumbers = nameColumn < codeColumn;
        BufferedReader bufferedReader = new BufferedReader(reader);
        lineNo = 1;
        for (String line = bufferedReader.readLine(); line != null; line = bufferedReader.readLine()) {
            String[] chunks = csvSplit(line, separator);
            Command signal = scrutinizeRaw(chunks, rejectNumbers);
            if (signal != null) {
                addCommand(signal);
            }
            lineNo++;
        }
        setupRemoteSet();
    }

    private Command scrutinizeRaw(String[] chunks, boolean rejectNumbers) {
        String[] nameArray = gobbleString(chunks, nameColumn, nameMultiColumn, 16, "-", rejectNumbers);
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
        IrSignal irSignal;
        try {
            irSignal = InterpretString.interpretString(code, getFallbackFrequency(), getDummyGap(),
                    isInvokeRepeatFinder(), isInvokeCleaner(), getAbsoluteTolerance(), getRelativeTolerance(), getMinRepeatLastGap());
        } catch (InvalidArgumentException ex) {
            if (isVerbose())
                System.err.println("Error parsing code in line " + lineNo + " (" + ex.getMessage() + ")");
            return null;
        }
        return irSignal != null ?
                new Command(uniqueName(name), "Line " + lineNo + ", " + origin, irSignal)
                : null;
    }
}
