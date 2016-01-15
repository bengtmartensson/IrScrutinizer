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

package org.harctoolbox.irscrutinizer;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import org.harctoolbox.IrpMaster.IrpMasterException;
import org.harctoolbox.girr.Command;

/**
 *
 *
 */
public abstract class NamedIrSignal {
    private static int count = 0;

    private static synchronized int incrementCount() {
        return ++count;
    }

    protected static synchronized void decrementCount() {
        count--;
    }

    private final Date date;
    private boolean validated;
    private String name;
    private String comment;
    private final int numeral;

    public NamedIrSignal(String name, String comment) {
        numeral = incrementCount();
        date = new Date();
        this.name = name;
        this.comment = comment;
        validated = false;
    }

    public int getNumeral() {
        return numeral;
    }

    public String getDate() {
        return (new SimpleDateFormat("HH:mm:ss")).format(date);
    }

    public boolean getValidated() {
        return validated;
    }

    public void setValidated(boolean validated) {
        this.validated = validated;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String csvString(String separator) {
        StringBuilder str = new StringBuilder();
        str.append(numeral).append(separator);
        str.append(name).append(separator);
        str.append(validated ? "true" : "false");
        return str.toString();
    }

    public String toPrintString() {
        return csvString(", ");
    }

    /**
     *
     */
    protected static abstract class AbstractColumnFunction {
        private final String[] columnNames;
        private final int[] widths;
        private Class[] classes;
        private int toIgnore;

        AbstractColumnFunction(String[] columnNames,
                int[] widths,
                Class[] classes,
                int toIgnore) {
            this.columnNames = columnNames;
            this.widths = widths;
            if (widths.length != columnNames.length)
                throw new IllegalArgumentException();
            this.classes = classes;
            if (classes.length != columnNames.length)
                throw new IllegalArgumentException();
            this.toIgnore = toIgnore;
        }

        public abstract boolean isEditable(int i);

        public abstract Object[] toObjectArray(NamedIrSignal signal);

        public abstract int getPosName();

        public abstract int getPosComment();

        public abstract int getPosIrSignal();

        public abstract int getPosDate();

        public abstract int getPosNumber();

        public int noFields() {
            return columnNames.length - toIgnore;
        }

        public String name(int i) {
            return columnNames[i];
        }

        public int width(int i) {
            return widths[i];
        }

        public Class<?> clazz(int i) {
            return classes[i];
        }

        public Object[] headers() {
            Object[] result = new Object[columnNames.length];
            for (int i = 0; i < columnNames.length; i++)
                result[i] = (Object) columnNames[i];
            return result;
        }
    }

    public abstract static class LearnedIrSignalTableColumnModel extends DefaultTableColumnModel {
        private final AbstractColumnFunction columnFunc;

        public LearnedIrSignalTableColumnModel(AbstractColumnFunction icolumn) {
            super();
            this.columnFunc = icolumn;
            setup();
        }

        private void setup() {
            for (int i = 0; i < columnFunc.noFields(); i++) {
                TableColumn column = new TableColumn(i, columnFunc.width(i));
                column.setHeaderValue(columnFunc.name(i));
                column.setIdentifier(i);
                addColumn(column);
            }
        }

        /**
         * Remove selected column, if possible
         * @param i column number
         * @throws ArrayIndexOutOfBoundsException if no column #i exists
         */
        public void removeColumn(int i) {
            removeColumn(getColumn(i));
        }

        public void reset() {
            tableColumns.clear();
            setup();
        }

        protected void setup(String[] columnNames, int [] widths, int noFields) {
            for (int i = 0; i < noFields; i++) {
                TableColumn column = new TableColumn(i, widths[i]);
                column.setHeaderValue(columnNames[i]);
                addColumn(column);
            }
        }

        public void removeColumns(Collection<Integer> list) {
            for (int column = this.getColumnCount() - 1; column >= 0; column--) {
                TableColumn col = this.getColumn(column);
                if (list.contains(col.getModelIndex()))
                    removeColumn(col);
            }
        }
    }

    public abstract static class LearnedIrSignalTableModel extends DefaultTableModel {
        private final AbstractColumnFunction columnsFunc;
        private boolean scrollRequest = false;

        public abstract String getType();

        public abstract Command toCommand(int row) throws IrpMasterException;

        public HashMap<String, Command> getCommands(boolean forgiveSillySignals) throws IrpMasterException {
            HashMap<String, Command> commands = new LinkedHashMap<>(getRowCount() + 10);
            for (int row = 0; row < getRowCount(); row++) {
                try {
                    Command command = toCommand(row);
                    if (command != null)
                        commands.put(command.getName(), command);
                } catch (IrpMasterException ex) {
                    if (forgiveSillySignals) {
                        String commandName = (String) getValueAt(row, columnsFunc.getPosName());
                        String commandComment = (String) getValueAt(row, columnsFunc.getPosComment());
                        System.err.println("Warning: Signal named " + commandName + " ("
                                + commandComment + ") could not be rendered (" + ex.getMessage() + "); ignored.");
                    } else {
                        throw ex;
                    }
                }
            }
            return commands;
        }

        public synchronized void clearComment() {
            for (int row = 0; row < getRowCount(); row++) {
                NamedIrSignal nir = (NamedIrSignal) getValueAt(row, columnsFunc.getPosIrSignal());
                nir.setComment(null);
                setValueAt(null, row, columnsFunc.getPosComment());
            }
        }

        public ArrayList<String> getNonUniqueNames() {
            ArrayList<String> duplicates = new ArrayList<>();
            ArrayList<String> allNames = new ArrayList<>(getRowCount() + 10);
            for (int row = 0; row < getRowCount(); row++) {
                String name = (String) getValueAt(row, columnsFunc.getPosName());
                if (allNames.contains(name)) {
                    if (!duplicates.contains(name))
                        duplicates.add(name);
                } else
                    allNames.add(name);
            }
            return duplicates;
        }

        public ArrayList<Integer> getUnusedColumns() {
            ArrayList<Integer> list = new ArrayList<>();
            if (getRowCount() > 0) { // If the table is empty, do not consider any columns unused.
                for (int column = 0; column < getColumnCount(); column++) {
                    if (!isUsedColumn(column))
                        list.add(column);
                }
            }
            return list;
        }

        @Override
        public final Class<?> getColumnClass(int columnIndex) {
            return columnsFunc.clazz(columnIndex);
        }

        public final String columnName(int i) {
            //if (i > noFields - 1)
            //    throw new IndexOutOfBoundsException();
            return columnsFunc.name(i);
        }

        public boolean isUsedColumn(int column) {
            for (int row = 0; row < this.getRowCount(); row++) {
                Object thing = getValueAt(row, column);
                if (thing == null)
                    continue;
                if (getColumnClass(column) == Boolean.class ? (Boolean) getValueAt(row, column)
                        : getColumnClass(column) == String.class ? !((String) getValueAt(row, column)).isEmpty()
                        : getValueAt(row, column) != null)
                    return true;
            }
            return false;
        }

        public ArrayList<Integer> getUninterestingColumns() {
            ArrayList<Integer> list = new ArrayList<>();
            if (getRowCount() > 0) { // If the table is empty, do not consider any columns unused.
                for (int column = 0; column < getColumnCount(); column++) {
                    if (!isInterestingColumn(column))
                        list.add(column);
                }
            }
            return list;
        }

        public boolean isInterestingColumn(int column) {
            if (columnsFunc.getPosDate() == column || columnsFunc.getPosNumber() == column)
                return false;

            if (columnsFunc.getPosName() == column) // prevents from removing all columns
                return true;

            if (getRowCount() < 2)
                return true;

            Object firstThing = getValueAt(0, column);
            for (int row = 1; row < this.getRowCount(); row++) {
                Object thing = getValueAt(row, column);
                if (firstThing == null) {
                    if (thing != null)
                        return true;
                } else {
                    if (!firstThing.equals(thing))
                        return true;
                }
            }
            return false;
        }

        // Derived classes should define a public version of the function, taking only
        // an instance of the derived class as argument.
        protected synchronized void addSignal(NamedIrSignal cir) {
            addRow(columnsFunc.toObjectArray(cir));
            scrollRequest = true;
        }

        public synchronized boolean getAndResetScrollRequest() {
            boolean old = scrollRequest;
            scrollRequest = false;
            return old;
        }

        public boolean validRow(int row) {
            return row >= 0 && row < getRowCount();
        }

        @Override
        public final boolean isCellEditable(int rowIndex, int columnIndex) {
            return columnsFunc.isEditable(columnIndex);
        }

        protected LearnedIrSignalTableModel(AbstractColumnFunction columnFunc) {
            //super(columnFunc.dummyArray(), columnFunc.headers());
            super(columnFunc.headers(), 0);
            this.columnsFunc = columnFunc;
        }

        /**
         * For debugging purposes only.
         * @param modelRow
         * @return nicely formatted String.
         */
        public String toPrintString(int modelRow) {
            if (modelRow < 0)
                return null;
            StringBuilder str = new StringBuilder();
            for (int i = 0; i < this.columnsFunc.noFields(); i++) {
                Object thing = getValueAt(modelRow, i);
                str.append(" ").append(thing != null ? thing.toString() : "null");
            }
            return str.toString();
        }
    }
}
