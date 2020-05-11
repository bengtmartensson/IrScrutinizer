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

import java.awt.Event;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.table.DefaultTableModel;
import org.harctoolbox.girr.Command;
import org.harctoolbox.girr.GirrException;
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

    public void fixKeyMappings(JTable table) {
        addKey(table, "move up", KeyEvent.VK_UP, Event.CTRL_MASK, new AbstractAction("move up") {
            @Override
            public void actionPerformed(ActionEvent evt) {
                try {
                    tableMoveSelection(table, true);
                } catch (ErroneousSelectionException ex) {
                    guiUtils.error(ex);
                }
            }
        });

        addKey(table, "move down", KeyEvent.VK_DOWN, Event.CTRL_MASK, new AbstractAction("move down") {
            @Override
            public void actionPerformed(ActionEvent evt) {
                try {
                    tableMoveSelection(table, false);
                } catch (ErroneousSelectionException ex) {
                    guiUtils.error(ex);
                }
            }
        });

        addKey(table, "delete row", KeyEvent.VK_DELETE, Event.CTRL_MASK, new AbstractAction("delete row") {
            @Override
            public void actionPerformed(ActionEvent evt) {
                try {
                    deleteTableSelectedRows(table);
                } catch (ErroneousSelectionException ex) {
                    guiUtils.error(ex);
                }
            }
        });

        addKey(table, "delete all", KeyEvent.VK_DELETE, Event.CTRL_MASK | Event.SHIFT_MASK, new AbstractAction("delete all") {
            @Override
            public void actionPerformed(ActionEvent evt) {
                clearTableConfirm(table);
            }
        });
    }

    private void addKey(JTable table, String name, int key, int mask, Action action) {
        InputMap inputMap = table.getInputMap(JComponent.WHEN_FOCUSED);
        ActionMap actionMap = table.getActionMap();
        KeyStroke keyStroke = KeyStroke.getKeyStroke(key, mask);
        inputMap.put(keyStroke, name);
        actionMap.put(name, action);
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

    void barfIfNonconsecutiveSelection(JTable table) throws ErroneousSelectionException {
        int[] a = table.getSelectedRows();
        Arrays.sort(a); // not sure if this is needed
        if (a[a.length - 1] - a[0] != a.length - 1)
            throw new ErroneousSelectionException("Non-consecutive row selection");
    }

    // Requires the row sorter to be disabled
    public void tableMoveSelection(JTable table, boolean up) throws ErroneousSelectionException {
        barfIfNonconsecutiveSelection(table);
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

    void deleteTableSelectedRows(JTable table) throws ErroneousSelectionException {
        barfIfNoneSelected(table);
        int[] rows = table.getSelectedRows();
        for (int i = 0; i < rows.length; i++)
            rows[i] = table.convertRowIndexToModel(rows[i]);

        Arrays.sort(rows);

        DefaultTableModel tableModel = (DefaultTableModel) table.getModel();

        for (int i = rows.length - 1; i >= 0; i--)
            tableModel.removeRow(rows[i]);
    }

    void duplicateTableSelectedRow(JTable table) throws ErroneousSelectionException, GirrException {
        barfIfNotExactlyOneSelected(table);
        int selectedRow = table.getSelectedRow();
        int modelRow = table.convertRowIndexToModel(selectedRow);
        NamedIrSignal.LearnedIrSignalTableModel tableModel = (NamedIrSignal.LearnedIrSignalTableModel) table.getModel();
        tableModel.duplicate(modelRow);
    }

    void printTableSelectedRows(JTable table) {
        NamedIrSignal.LearnedIrSignalTableModel tableModel = (NamedIrSignal.LearnedIrSignalTableModel) table.getModel();
        int[] selected = table.getSelectedRows();
        for (int selectedRow : selected) {
            int modelRow = table.convertRowIndexToModel(selectedRow);
            String str = tableModel.toPrintString(modelRow);
            guiUtils.message(str);
        }
    }

    Command commandTableSelectedRow(JTable table) throws ErroneousSelectionException, GirrException {
        barfIfNotExactlyOneSelected(table);
        return commandTableSelected(table).values().iterator().next();
    }

    Map<String, Command> commandTableSelected(JTable table) throws GirrException {
        int[] selected = table.getSelectedRows();
        Map<String, Command> commands = new LinkedHashMap<>(selected.length);
        for (int selectedRow : selected) {
            int modelRow = table.convertRowIndexToModel(selectedRow);
            NamedIrSignal.LearnedIrSignalTableModel tableModel = (NamedIrSignal.LearnedIrSignalTableModel) table.getModel();
            Command command = tableModel.toCommand(modelRow);
            commands.put(command.getName(), command);
        }
        return commands;
    }

    void clearTableConfirm(JTable table) {
        if (guiUtils.confirm("Delete it all?")) {
            ((DefaultTableModel) table.getModel()).setRowCount(0);
            ((NamedIrSignal.LearnedIrSignalTableModel) table.getModel()).clearUnsavedChanges();
        }
    }
}
