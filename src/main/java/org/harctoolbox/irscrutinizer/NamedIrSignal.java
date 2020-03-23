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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import org.harctoolbox.girr.Command;
import org.harctoolbox.girr.GirrException;
import org.harctoolbox.guicomponents.GuiUtils;
import org.harctoolbox.ircore.IrCoreException;
import org.harctoolbox.ircore.IrSignal;
import org.harctoolbox.irp.IrpException;
import org.harctoolbox.irscrutinizer.exporter.NameUniquefier;

/**
 *
 *
 */
public abstract class NamedIrSignal {
    private static int count = 0;

    private static synchronized int incrementCount() {
        count++;
        return count;
    }

    protected static synchronized void decrementCount() {
        count--;
    }

    private final Date date;
    private boolean validated;
    private String name; // non-null
    private String comment; // can be null
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
        StringBuilder str = new StringBuilder(128);
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

        public abstract int getPosVerified();

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
            System.arraycopy(columnNames, 0, result, 0, columnNames.length);
            return result;
        }

        public boolean isUnimportant(int column) {
            return column == getPosDate() || column == getPosNumber();
        }

        public boolean isImportant(int column) {
            return column == getPosName();
        }

        public boolean uninterestingIfAllEqual(int column) {
            return column == getPosComment() || column == getPosVerified();
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
        protected boolean unsavedChanges;

        protected LearnedIrSignalTableModel(AbstractColumnFunction columnFunc) {
            //super(columnFunc.dummyArray(), columnFunc.headers());
            super(columnFunc.headers(), 0);
            this.unsavedChanges = false;
            this.columnsFunc = columnFunc;
        }

        public abstract String getType();

        public abstract Command toCommand(int row) throws GirrException;

        public Map<String, Command> getCommands() {
            Map<String, Command> commands = new LinkedHashMap<>(getRowCount());
            for (int row = 0; row < getRowCount(); row++) {
                try {
                    Command command = toCommand(row);
                    commands.put(command.getName(), command);
                } catch (GirrException ex) {
                        String commandName = (String) getValueAt(row, columnsFunc.getPosName());
                        String commandComment = (String) getValueAt(row, columnsFunc.getPosComment());
                        System.err.println("Warning: Signal named " + commandName + " ("
                                + commandComment + ") could not be rendered (" + ex.getMessage() + "); ignored.");
                }
            }
            return commands;
        }

        public boolean sanityCheck(GuiUtils guiUtils) {
            Map<String, Command> commands = getCommands();
            return sanityCheck(commands);
        }


        public Map<String, Command> getCommandsWithSanityCheck(GuiUtils guiUtils) {
            Map<String, Command> commands = getCommands();
            if (commands.isEmpty()) {
                guiUtils.error("No signals present, aborting export.");
                return null;
            }
            boolean status = sanityCheck(commands);
            return status || guiUtils.confirm("Some signals in export erroneous. Continue anyhow?") ? commands : null;
        }

        private boolean checkNonUniqueNames() {
            List<String> duplicateNames = getNonUniqueNames();
            if (!duplicateNames.isEmpty()) {
                StringBuilder str = new StringBuilder("The following names are non-unique: ");
                str.append(String.join(", ", duplicateNames));
                str.append(".\n").append("Only one signal per name will be preserved in the export.");
                System.err.println(str);
                return false;
            }
            return true;
        }

        private boolean sanityCheck(Map<String, Command> commands) {
            if (commands.isEmpty()) {
                System.err.println("No signals present.");
                return false;
            }
            boolean success = true;
            for (Command command : commands.values()) {
                success = checkCommandSanity(command) && success;
            }

            return checkNonUniqueNames() && success;
        }

        // likely to be overridden
        protected boolean checkName(String name) {
            if (name == null || name.isEmpty()) {
                System.err.println("Command with empty name.");
                return false;
            }
            return true;
        }

        protected boolean checkCommandSanity(Command command) {
            if (!checkName(command.getName()))
                return false;

            try {
                IrSignal irSignal = command.toIrSignal();
                return irSignal != null;
            } catch (IrpException | IrCoreException ex) {
                System.err.println(ex.getMessage());
                return false;
            }
        }

        public synchronized void clearComment() {
            for (int row = 0; row < getRowCount(); row++) {
                NamedIrSignal nir = (NamedIrSignal) getValueAt(row, columnsFunc.getPosIrSignal());
                nir.setComment(null);
                setValueAt(null, row, columnsFunc.getPosComment());
            }
            unsavedChanges = true;
        }

        public ArrayList<String> getNonUniqueNames() {
            ArrayList<String> duplicates = new ArrayList<>(32);
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

        public void uniquifyNames(String separator) {
            NameUniquefier uniquefier = new NameUniquefier(separator);
            for (int row = 0; row < getRowCount(); row++) {
                String oldName = (String) getValueAt(row, columnsFunc.getPosName());
                String newName = uniquefier.uniquefy(oldName);//oldName.replaceFirst(from, to);
                if (!oldName.equals(newName))
                    setValueAt(newName, row, columnsFunc.getPosName());
            }
            fireTableDataChanged();
        }

        public ArrayList<Integer> getUnusedColumns() {
            ArrayList<Integer> list = new ArrayList<>(16);
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

        public boolean isAllEqualColumn(int column) {
            Object firstThing = getValueAt(0, column);
            for (int row = 1; row < this.getRowCount(); row++) {
                Object thing = getValueAt(row, column);
                if (firstThing == null) {
                    if (thing != null)
                        return false;
                } else {
                    if (!firstThing.equals(thing))
                        return false;
                }
            }
            return true;
        }

        public ArrayList<Integer> getUninterestingColumns() {
            ArrayList<Integer> list = new ArrayList<>(16);
            if (getRowCount() > 0) { // If the table is empty, do not consider any columns unused.
                for (int column = 0; column < getColumnCount(); column++) {
                    if (!isInterestingColumn(column))
                        list.add(column);
                }
            }
            return list;
        }

        public boolean isInterestingColumn(int column) {
            if (columnsFunc.isUnimportant(column))
                return false;

            if (columnsFunc.isImportant(column))
                return true;

            if (!isUsedColumn(column))
                return false;

            if (getRowCount() < 2)
                return true;

            return ! (columnsFunc.uninterestingIfAllEqual(column) && isAllEqualColumn(column));
        }

        public void namesTransform(String from, String to) {
            for (int row = 0; row < getRowCount(); row++) {
                String oldName = (String) getValueAt(row, columnsFunc.getPosName());
                String newName = oldName.replaceFirst(from, to);
                setValueAt(newName, row, columnsFunc.getPosName());
            }
            fireTableDataChanged();
        }

        // Derived classes should define a public version of the function, taking only
        // an instance of the derived class as argument.
        protected synchronized void addSignal(NamedIrSignal cir) {
            addRow(columnsFunc.toObjectArray(cir));
            scrollRequest = true;
            unsavedChanges = true;
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

        /**
         * For debugging purposes only.
         * @param modelRow
         * @return nicely formatted String.
         */
        public String toPrintString(int modelRow) {
            if (modelRow < 0)
                return null;
            StringBuilder str = new StringBuilder(64);
            for (int i = 0; i < this.columnsFunc.noFields(); i++) {
                Object thing = getValueAt(modelRow, i);
                str.append(" ").append(thing != null ? thing.toString() : "null");
            }
            return str.toString();
        }

        void clearUnsavedChanges() {
            unsavedChanges = false;
        }

        boolean hasUnsavedChanges() {
            return unsavedChanges;
        }

        abstract void duplicate(int modelRow);
    }
}
