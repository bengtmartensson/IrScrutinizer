/*
 * Copyright (C) 2019 Bengt Martensson
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.harctoolbox.guicomponents;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JTextArea;

/**
 * This class extends JTextArea with an undo function.
 */
public class UndoableJTextArea extends JTextArea {

    private final static Logger logger = Logger.getLogger(UndoableJTextArea.class.getName());

    private final StringStack history;

    /**
     * Constructs a new TextArea.
     */
    public UndoableJTextArea() {
        super();
        history = new StringStack();
    }

    /**
     * Constructs a new empty TextArea with the specified number of rows and
     * columns.
     * @param rows
     * @param columns
     */
    public UndoableJTextArea(int rows, int columns) {
        super(rows, columns);
        history = new StringStack();
    }

    /**
     * Constructs a new TextArea with the specified text displayed.
     * @param text
     */
    public UndoableJTextArea(String text) {
        history = new StringStack(text);
    }

    /**
     * Constructs a new TextArea with the specified text displayed.
     *
     * @param text
     * @param rows
     * @param columns
     */
    public UndoableJTextArea(String text, int rows, int columns) {
        super(text, rows, columns);
        history = new StringStack(text);
    }

    public void clearHistory() {
        history.clear();
    }

    public int getHistorySize() {
        return history.size();
    }

    @Override
    public void setText(String t) {
        String present = getText();
        history.saveIfDifferent(present);
        if (!present.equals(t)) {
            super.setText(t);
            history.save(t);
        }
    }

    public void undo() throws UndoHistoryEmptyException {
        String old;
        do {
            old = history.pull();
        } while (old.equals(getText()));
        super.setText(old);
    }

    private static final class StringStack implements Serializable {

        private static final int INITIAL_CAPACITY = 8;
        private final List<String> list;

        StringStack() {
            list = new ArrayList<>(INITIAL_CAPACITY);
        }

        StringStack(String text) {
            this();
            push(text);
        }

        void push(String t) {
            list.add(t);
        }

        String pull() throws UndoHistoryEmptyException {
            if (list.isEmpty())
                throw new UndoHistoryEmptyException();
            String result = list.get(list.size() - 1);
            list.remove(list.size() - 1);
            logger.log(Level.FINE, "Removed: {0}", result);
            return result;
        }

        int size() {
            return list.size();
        }

        void clear() {
            list.clear();
        }

        public boolean isEmpty() {
            return list.isEmpty();
        }

        private void saveIfDifferent(String text) {
            if (list.isEmpty() || ! last().equals(text))
                save(text);
        }

        private void save(String t) {
            if (t == null || t.isEmpty())
                logger.log(Level.FINE, "Refused to added null/empty entry");
            else {
                list.add(t);
                logger.log(Level.FINE, "Added: {0}", t);
            }
        }

        private String last() {
            return list.get(list.size() - 1);
        }
    }

    public static class UndoHistoryEmptyException extends Exception {

        public UndoHistoryEmptyException() {
            super("No undo information");
        }
    }
}
