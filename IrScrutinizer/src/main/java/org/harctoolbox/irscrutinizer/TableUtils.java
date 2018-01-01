/*
Copyright (C) 2018 Bengt Martensson.

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

import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import org.harctoolbox.IrpMaster.IrpMasterException;
import org.harctoolbox.girr.Command;
import org.harctoolbox.guicomponents.ErroneousSelectionException;
import org.harctoolbox.guicomponents.GuiUtils;

/**
 *
 */
public class TableUtils {

    private final GuiUtils guiUtils;

    TableUtils(GuiUtils guiUtils) {
        this.guiUtils = guiUtils;
    }

    private void barfIfManySelected(JTable table) throws ErroneousSelectionException {
        if (table.getSelectedRowCount() > 1)
            throw new ErroneousSelectionException("Only one row may be selected");
    }

    void barfIfNoneSelected(JTable table) throws ErroneousSelectionException {
        if (table.getSelectedRow() == -1)
            throw new ErroneousSelectionException("No row selected");
    }

    void barfIfNotExactlyOneSelected(JTable table) throws ErroneousSelectionException {
        barfIfManySelected(table);
        barfIfNoneSelected(table);
    }

    // Requires the row sorter to be disabled
    public void tableMoveSelection(JTable table, boolean up) {
        int row = table.getSelectedRow();
        int lastRow = row + table.getSelectedRowCount() - 1;

        if (row < 0) {
            guiUtils.error("No signal selected");
            return;
        }
        DefaultTableModel tableModel = (DefaultTableModel) table.getModel();
        if (up) {
            if (row == 0) {
                guiUtils.error("Cannot move up");
                return;
            }
        } else { // down
            if (lastRow >= tableModel.getRowCount() - 1) {
                guiUtils.error("Cannot move down");
                return;
            }
        }

        if (up) {
            tableModel.moveRow(row, lastRow, row - 1);
            table.addRowSelectionInterval(row - 1, row - 1);
            table.removeRowSelectionInterval(lastRow, lastRow);
        } else {
            tableModel.moveRow(row, lastRow, row + 1);
            table.addRowSelectionInterval(lastRow + 1, lastRow + 1);
            table.removeRowSelectionInterval(row, row);
        }
    }

    // If using sorter and deleting several rows, need to compute the to-be-removed model-indexes,
    // sort them, and remove them in descending order. I presently do not care enough...
    void deleteTableSelectedRows(JTable table) throws ErroneousSelectionException {
        barfIfNoneSelected(table);
        if (table.getRowSorter() != null && table.getSelectedRowCount() > 1) {
            guiUtils.error("Deleting several rows with enabled row sorter not yet implemented");
            return;
        }
        int row = table.getSelectedRow();

        DefaultTableModel tableModel = (DefaultTableModel) table.getModel();

        for (int i = table.getSelectedRowCount(); i > 0; i--)
            tableModel.removeRow(table.convertRowIndexToModel(row + i - 1));
    }

    void printTableSelectedRow(JTable table) throws ErroneousSelectionException {
        barfIfNotExactlyOneSelected(table);
        int modelRow = table.convertRowIndexToModel(table.getSelectedRow());
        NamedIrSignal.LearnedIrSignalTableModel tableModel = (NamedIrSignal.LearnedIrSignalTableModel) table.getModel();
        String str = tableModel.toPrintString(modelRow);
        guiUtils.message(str);
    }

    Command commandTableSelectedRow(JTable table) throws IrpMasterException, ErroneousSelectionException {
        barfIfNotExactlyOneSelected(table);
        int selectedRow = table.getSelectedRow();
        int modelRow = table.convertRowIndexToModel(selectedRow);
        NamedIrSignal.LearnedIrSignalTableModel tableModel = (NamedIrSignal.LearnedIrSignalTableModel) table.getModel();
        Command command = tableModel.toCommand(modelRow);
        return command;
    }

    void clearTableConfirm(JTable table) {
        if (guiUtils.confirm("Delete it all?")) {
            ((DefaultTableModel) table.getModel()).setRowCount(0);
            ((NamedIrSignal.LearnedIrSignalTableModel) table.getModel()).clearUnsavedChanges();
        }
    }
}
