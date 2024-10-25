/*
Copyright (C) 2013 Bengt Martensson.

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
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.text.ParseException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.antlr.v4.runtime.misc.ParseCancellationException;
import org.harctoolbox.girr.Command;
import org.harctoolbox.girr.GirrException;
import org.harctoolbox.ircore.InvalidArgumentException;
import org.harctoolbox.ircore.IrCoreUtils;
import org.harctoolbox.irp.Expression;
import org.harctoolbox.irp.NameEngine;
import org.harctoolbox.irp.NameUnassignedException;

/**
 * This class does something interesting and useful. Or not...
 *
 * Columns are numbered starting with 1.
 */
@SuppressWarnings("serial")
public class CsvParametrizedImporter extends CsvImporter {

    private static final String protocolFallback = "NEC1"; // FIXME: make configurable!!
    private static final String NONE = "none";

    public static Collection<Command> process(Reader reader,
            String separator, int nameColumn, boolean nameMultiColumn, String filename, boolean verbose, int base, int Fcolumn, int Dcolumn,
            int Scolumn, int protocolColumn, int miscParametersColumn) throws IOException {
        CsvParametrizedImporter csvImportParametrized = new CsvParametrizedImporter(
                separator, nameColumn, nameMultiColumn, verbose, base, Fcolumn, Dcolumn, Scolumn, protocolColumn, miscParametersColumn);
        csvImportParametrized.load(reader, filename);
        return csvImportParametrized.getCommands();
    }

    @SuppressWarnings("UseOfSystemOutOrSystemErr")
    public static void main(String[] args) {
        try {
            Reader r = new InputStreamReader(new FileInputStream(args[0]), Charset.forName("US-ASCII"));
            Collection<Command> signals = process(r,",", 1, false, args[0], true, 10, 3, -1, -1, -1, -1);
            signals.forEach((s) -> {
                System.out.println(s.toString());
            });
        } catch (IOException ex) {
            System.err.println(ex.getMessage());
        }
    }

    private int numberBase;
    private int fColumn;
    private int dColumn;
    private int sColumn;
    private int miscParametersColumn = 4;
    private int protocolColumn;

    public CsvParametrizedImporter(int separatorIndex, int nameColumn, boolean nameMultiColumn, boolean verbose, int base, int Fcolumn, int Dcolumn,
            int Scolumn, int protocolColumn, int miscParametersColumn) {
        this(CsvImporter.getSeparator(separatorIndex), nameColumn, nameMultiColumn, verbose, base, Fcolumn, Dcolumn,
                Scolumn, protocolColumn, miscParametersColumn);
    }

    public CsvParametrizedImporter(String separator, int nameColumn, boolean nameMultiColumn, boolean verbose, int numberBase, int Fcolumn, int Dcolumn,
            int Scolumn, int protocolColumn, int miscParametersColumn) {
        super(separator, nameColumn, nameMultiColumn);
        this.numberBase = numberBase;
        this.fColumn = Fcolumn;
        this.dColumn = Dcolumn;
        this.sColumn = Scolumn;
        this.protocolColumn = protocolColumn;
        this.miscParametersColumn = miscParametersColumn;
    }

    /**
     * @param protocolColumn the protocolColumn to set
     */
    public void setProtocolColumn(int protocolColumn) {
        this.protocolColumn = protocolColumn;
    }

    /**
     * @param numberBase the numberBase to set
     */
    public void setNumberBase(int numberBase) {
        this.numberBase = numberBase;
    }

    /**
     * @param fColumn the fColumn to set
     */
    public void setFColumn(int fColumn) {
        this.fColumn = fColumn;
    }

    /**
     * @param dColumn the dColumn to set
     */
    public void setDColumn(int dColumn) {
        this.dColumn = dColumn;
    }

    /**
     * @param sColumn the sColumn to set
     */
    public void setSColumn(int sColumn) {
        this.sColumn = sColumn;
    }


    @Override
    public void load(File file, String origin, String charsetName) throws FileNotFoundException, IOException, ParseException, InvalidArgumentException {
        try (FileInputStream stream = new FileInputStream(file)) {
            load(stream, origin, charsetName);
        }
    }

    @Override
    @SuppressWarnings("UseOfSystemOutOrSystemErr")
    public void load(Reader reader, String origin) throws IOException {
        prepareLoad(origin);
        boolean rejectNumbers = nameColumn < protocolColumn || nameColumn < dColumn
                || nameColumn < sColumn || nameColumn < fColumn;
        lineNo = 1;
        BufferedReader bufferedReader = new BufferedReader(reader);
        while (true) {
            String line = bufferedReader.readLine();
            if (line == null)
                break;
            String[] chunks = line.split(separator);
            try {
                Command command = scrutinizeParameters(chunks, "Line " + lineNo + ", " + origin, rejectNumbers);
                if (command != null)
                    addCommand(command);
            } catch (GirrException | NameUnassignedException | ParseCancellationException ex) {
                if (isVerbose())
                    System.err.println("Errors parsing line " + lineNo + ": \"" + line + "\": " + ex.getMessage());
            }
            lineNo++;
        }
        setupRemoteSet();
    }

    private Command scrutinizeParameters(String[] chunks, String sourceAsComment, boolean rejectNumbers) throws GirrException, NameUnassignedException {
        String[] nameArray = gobbleString(chunks, nameColumn, nameMultiColumn, numberBase, "-", rejectNumbers);
        if (nameArray == null || nameArray.length == 0)
            return null;
        int offset = nameArray.length - 1;
        String name = join(nameArray);
        Map<String, Long> parameters = new HashMap<>(3);
        long F = gobbleLong(chunks, fColumn, "F", nameColumn < fColumn ? offset : 0);
        if (F != invalid)
            parameters.put("F", F);
        long D = gobbleLong(chunks, dColumn, "D", nameColumn < dColumn ? offset : 0);
        if (D != invalid)
            parameters.put("D", D);
        long S = gobbleLong(chunks, sColumn, "S", nameColumn < sColumn ? offset : 0);
        if (S != invalid)
            parameters.put("S", S);
        NameEngine miscParameters = gobbleNameEngine(chunks, miscParametersColumn, nameColumn < miscParametersColumn ? offset : 0);
        if (miscParameters != null)
            for (Map.Entry<String, Expression> entry : miscParameters)
                    parameters.put(entry.getKey(), entry.getValue().toLong());

        String protocol = gobbleString(chunks, protocolColumn, protocolFallback, false, nameColumn < protocolColumn ? offset : 0);
        return new Command(uniqueName(name), sourceAsComment, protocol, parameters);
    }

    private NameEngine gobbleNameEngine(String[] chunks, int column, int offset) {
        if (column == 0)
            return null;
        int colIndex = column > 0 ? column - 1 + offset : chunks.length + column;

        if (colIndex < chunks.length) {
            String str = chunks[colIndex];
            return NameEngine.parseLoose(str);
        }
        return null;
    }

    @SuppressWarnings("UseOfSystemOutOrSystemErr")
    private long gobbleLong(String[] chunks, int column, String name, int offset) {
        if (column == 0)
            return IrCoreUtils.INVALID;
        int colIndex = column > 0 ? column - 1 + offset : chunks.length + column;

        if (colIndex < chunks.length) {
            try {
                String num = chunks[colIndex];
                return num.trim().equalsIgnoreCase(NONE) ? IrCoreUtils.INVALID
                        : IrCoreUtils.parseLong(num.trim(), numberBase);
            } catch (NumberFormatException ex) {
                if (isVerbose()) {
                    System.err.println("Errors parsing " + name + " (= " + chunks[colIndex] + ") in line " + lineNo);
                }
            }
        }
        return invalid;
    }
}
