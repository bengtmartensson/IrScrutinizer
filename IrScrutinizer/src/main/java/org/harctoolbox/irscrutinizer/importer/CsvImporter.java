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

import java.io.Serializable;
import java.util.ArrayList;
import org.harctoolbox.IrpMaster.IrpUtils;

/**
 * This class does something interesting and useful. Or not...
 *
 * Columns are numbered starting with 1.
 */
public abstract class CsvImporter extends RemoteSetImporter implements IReaderImporter,Serializable {
    protected final static int invalid = (int) IrpUtils.invalid;
    private static final long serialVersionUID = 1L;

    protected String separator;
    protected int nameColumn;
    protected boolean nameMultiColumn;

    protected int lineNo;

    private final static String[][] separators = new String[][] {
        new String[] { ", (comma)", "," },
        new String[] { "; (semicolon)", ";" },
        new String[] { ": (colon)", ":" },
        new String[] { "TAB", "\t" },
        new String[] { "WHITESPACE", "\\s+" }
    };

    /**
     * @param separator the separator to set
     */
    public void setSeparator(String separator) {
        this.separator = separator;
    }

    public void setSeparatorIndex(int separatorIndex) {
        this.separator = getSeparator(separatorIndex);
    }

    /**
     * @param nameColumn the nameColumn to set
     */
    public void setNameColumn(int nameColumn) {
        this.nameColumn = nameColumn;
    }

    /**
     * @return the nameMultiColumn
     */
    public boolean isNameMultiColumn() {
        return nameMultiColumn;
    }

    /**
     * @param nameMultiColumn the nameMultiColumn to set
     */
    public void setNameMultiColumn(boolean nameMultiColumn) {
        this.nameMultiColumn = nameMultiColumn;
    }

    public static String[] separatorsArray() {
        String[] result = new String[separators.length];
        for (int i = 0; i < separators.length; i++)
            result[i] = separators[i][0];
        return result;
    }

    public static String getSeparator(int index){
        return separators[index][1];
    }

    public CsvImporter(String separator, int nameColumn, boolean nameMultiColumn) {
        super();
        this.nameMultiColumn = nameMultiColumn;
        this.separator = separator;
        this.nameColumn = nameColumn;
    }

    protected static String gobbleString(String[] chunks, int column, String aPriori, boolean includeTail, int offset) {
        if (column <= 0)
            return aPriori;
        if (column > chunks.length)
            return null;

        StringBuilder str = new StringBuilder(chunks[column-1+offset]);
        if (str.length() > 0 && str.charAt(str.length()-1) == '"')
            str.deleteCharAt(str.length()-1);
        if (str.length() > 0 && str.charAt(0) == '"')
            str.deleteCharAt(0);

        if (includeTail)
            for (int c = column+offset; c < chunks.length; c++)
                str.append(" ").append(chunks[c]);

        return str.toString();
    }

    protected static String[] gobbleString(String[] chunks, int column, boolean nameMultiColumn, int basis, String aPriori) {
        if (column <= 0)
            return new String[]{ aPriori };
        if (column > chunks.length)
            return new String[0];

        String str = chunks[column-1];
        if (str.startsWith("\"") && str.endsWith("\""))
            str = str.substring(1, str.length()-1);

        if (!nameMultiColumn)
            return new String[]{ str };

        ArrayList<String> arrayList = new ArrayList<>();
        arrayList.add(str);
        for (int index = column; index < chunks.length; index++) {
            boolean isNumber = false;
            String chunk = chunks[index];
            if (basis == 16 && chunk.startsWith("0x"))
                chunk = chunk.substring(2);
            try {
                Integer.parseInt(chunk, basis);
                isNumber = true;
            } catch (NumberFormatException ex) {
            }
            if (isNumber)
                break;
            else
                arrayList.add(chunk);
        }
        return arrayList.toArray(new String[arrayList.size()]);
    }

    protected static String join(String[] arr) {
        if (arr == null)
            return "";
        StringBuilder str = new StringBuilder();
        for (String s : arr) {
            if (str.length() > 0)
                str.append(" ");

            str.append(s);
        }
        return str.toString();
    }

    @Override
    public String[][] getFileExtensions() {
        return new String[][]{ new String[]{ "Text files (*.txt *.text)", "txt", "text" }, { "CVS files (*.csv *.txt *.tsv)", "csv", "txt", "tsv"}};
    }

    @Override
    public boolean canImportDirectories() {
        return false;
    }

    @Override
    public String getFormatName() {
        return "Text (csv)";
    }
}
