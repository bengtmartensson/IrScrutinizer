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

package org.harctoolbox.guicomponents;

import java.awt.Desktop;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import org.harctoolbox.IrpMaster.IrpUtils;

/**
 * This class is a general purpose console.
 */
public class Console extends javax.swing.JScrollPane {
    public interface IErrorFunction extends Serializable {
        void err(Exception ex, String str);

        void err(String str);
    }

    private static class SimpleErrorFunction implements IErrorFunction {
        PrintStream consolePrintStream;

        SimpleErrorFunction(PrintStream consolePrintStream) {
            this.consolePrintStream = consolePrintStream;
        }

        @Override
        public void err(Exception ex, String message) {
            err(ex.getMessage() + ". " + message);
        }

        @Override
        public void err(String str) {
            consolePrintStream.println(str);
        }
    }

    /**
     * Sets a used defined error reporting function.
     * @param errorFunction
     */
    public void setErrorFunction(IErrorFunction errorFunction) {
        this.errorFunction = errorFunction;
    }

    /**
     * Clears the console.
     */
    public void clear() {
        consoleTextArea.setText(null);
    }

    /**
     * Saves the console content as text to the file given as argument.
     * @param file
     * @throws FileNotFoundException
     */
    public void save(File file) throws FileNotFoundException {
        PrintStream ps = null;
        try {
            ps = new PrintStream(new FileOutputStream(file), true, IrpUtils.dumbCharsetName);
            //if (consoleTextArea != null)
            ps.println(consoleTextArea.getText());
            ps.close();
        } catch (UnsupportedEncodingException ex) {
            assert false; // cannot happen
        }
    }

    /**
     * Returns all text in the console.
     * @return Content as String.
     */
    public String getText() {
        return consoleTextArea.getText();
    }

    /**
     * Returns the selected text of the console.
     * @return Selection as String.
     */
    public String getSelectedText() {
        return consoleTextArea.getSelectedText();
    }

    /**
     * Copy the selected text in the console to the clipboard.
     */
    public void copySelectionToClipboard() {
        (new CopyClipboardText()).toClipboard(getSelectedText());
    }

    /**
     * Copy the console as text to the clipboard.
     */
    public void copyToClipboard() {
        (new CopyClipboardText()).toClipboard(getText());
    }

    /**
     * Sets the system's stdout to this console.
     */
    public void setStdOut() {
        System.setOut(consolePrintStream);
    }

    /**
     * Sets the system's stderr to this console.
     */
    public void setStdErr() {
        System.setErr(consolePrintStream);
    }

    /**
     * Returns the print stream. Do a System.setErr(...getPrintStream()) to redirect stderr.
     * @return
     */
    //public PrintStream getPrintStream() {
    //    return consolePrintStream;
    //}

    /**
     * Prints on the console.
     * @param str
     */
    public void println(String str) {
        consolePrintStream.println(str);
    }

    // From Real Gagnon
    private class FilteredStream extends FilterOutputStream {

        FilteredStream(OutputStream aStream) {
            super(aStream);
        }

        @Override
        public void write(byte b[]) throws IOException {
            String aString = new String(b, "US-ASCII");
            consoleTextArea.append(aString);
        }

        @Override
        public void write(byte b[], int off, int len) throws IOException {
            String aString = new String(b, off, len, "US-ASCII");
            consoleTextArea.append(aString);
            consoleTextArea.setCaretPosition(consoleTextArea.getDocument().getLength());
        }
    }

    private static class CopyClipboardText implements ClipboardOwner {

        @Override
        public void lostOwnership(Clipboard c, Transferable t) {
        }

        public void toClipboard(String str) {
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(str), this);
        }

        //public String fromClipboard() throws UnsupportedFlavorException, IOException {
        //    return (String) Toolkit.getDefaultToolkit().getSystemClipboard().getContents(this).getTransferData(DataFlavor.stringFlavor);
        //}
    }

    private IErrorFunction errorFunction = null;

    private final javax.swing.JMenuItem consoleClearMenuItem;
    private final javax.swing.JMenuItem consoleCopyMenuItem;
    private final javax.swing.JMenuItem consoleCopySelectionMenuItem;
    private final javax.swing.JMenuItem consolePrintMenuItem;
    private final javax.swing.JPopupMenu consolePopupMenu;
    private final javax.swing.JMenuItem consoleSaveMenuItem;
    private final javax.swing.JTextArea consoleTextArea;

    private transient PrintStream consolePrintStream = null;

    private void consoleClearMenuItemActionPerformed(java.awt.event.ActionEvent evt) {
        clear();
    }

    private void consoleTextAreaMousePressed(java.awt.event.MouseEvent evt) {
        if (evt.isPopupTrigger())
           consolePopupMenu.show(evt.getComponent(), evt.getX(), evt.getY());
    }

    private void consoleTextAreaMouseReleased(java.awt.event.MouseEvent evt) {
        consoleTextAreaMousePressed(evt);
    }

    private void consoleCopyMenuItemActionPerformed(java.awt.event.ActionEvent evt) {
        copyToClipboard();
    }

    private void consoleCopySelectionMenuItemActionPerformed(java.awt.event.ActionEvent evt) {
        this.copySelectionToClipboard();
    }

    private void consoletextSaveMenuItemActionPerformed(java.awt.event.ActionEvent evt) {
        try {
            File file = SelectFile.selectFile(this, "Save console text as...", null, true, false, "Text file", "txt");
            if (file != null)
                save(file);
        } catch (FileNotFoundException ex) {
            errorFunction.err(ex, "File not found");
        }
    }

    private void consoletextPrintMenuItemActionPerformed(java.awt.event.ActionEvent evt) {
        File file;
        try {
            file = File.createTempFile("console", ".txt");
        } catch (IOException ex) {
            errorFunction.err(ex, "");
            return;
        }
        try (PrintStream pos = new PrintStream(new FileOutputStream(file), true, "US-ASCII")) {
            pos.println(getText());
        } catch (IOException ex) {
            errorFunction.err(ex, "");
            return;
        }

        try {
            Desktop.getDesktop().print(file);
            file.deleteOnExit();
        } catch (UnsupportedOperationException ex) {
            errorFunction.err("Desktop does not support printing. Print the file "
                    + file.getAbsolutePath() + " manually.");
        } catch (IOException ex) {
            errorFunction.err(ex, "");
        }
    }

    public Console() {
        errorFunction = new SimpleErrorFunction(consolePrintStream);

        consolePopupMenu = new javax.swing.JPopupMenu();
        consoleClearMenuItem = new javax.swing.JMenuItem();
        javax.swing.JPopupMenu.Separator jSeparator5 = new javax.swing.JPopupMenu.Separator();
        consoleCopySelectionMenuItem = new javax.swing.JMenuItem();
        consoleCopyMenuItem = new javax.swing.JMenuItem();
        javax.swing.JPopupMenu.Separator jSeparator8 = new javax.swing.JPopupMenu.Separator();
        consolePrintMenuItem = new javax.swing.JMenuItem();
        javax.swing.JPopupMenu.Separator jSeparator9 = new javax.swing.JPopupMenu.Separator();
        consoleSaveMenuItem = new javax.swing.JMenuItem();

        consoleClearMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_DELETE, 0));
        consoleClearMenuItem.setIcon(new javax.swing.ImageIcon(Console.class.getResource("/icons/Crystal-Clear/22x22/actions/eraser.png"))); // NOI18N
        consoleClearMenuItem.setText("Clear");
        consoleClearMenuItem.setToolTipText("Discard the content of the console window.");
        consoleClearMenuItem.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                consoleClearMenuItemActionPerformed(evt);
            }
        });
        consolePopupMenu.add(consoleClearMenuItem);
        consolePopupMenu.add(jSeparator5);

        consoleCopySelectionMenuItem.setIcon(new javax.swing.ImageIcon(Console.class.getResource("/icons/Crystal-Clear/22x22/actions/editcopy.png"))); // NOI18N
        consoleCopySelectionMenuItem.setText("Copy selection");
        consoleCopySelectionMenuItem.setToolTipText("Copy currently selected text to the clipboard.");
        consoleCopySelectionMenuItem.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                consoleCopySelectionMenuItemActionPerformed(evt);
            }
        });
        consolePopupMenu.add(consoleCopySelectionMenuItem);

        consoleCopyMenuItem.setText("Copy all");
        consoleCopyMenuItem.setToolTipText("Copy the content of the console to the clipboard.");
        consoleCopyMenuItem.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                consoleCopyMenuItemActionPerformed(evt);
            }
        });
        consolePopupMenu.add(consoleCopyMenuItem);
        consolePopupMenu.add(jSeparator8);

        consoleSaveMenuItem.setIcon(new javax.swing.ImageIcon(Console.class.getResource("/icons/Crystal-Clear/22x22/actions/filesaveas.png"))); // NOI18N
        consoleSaveMenuItem.setText("Save...");
        consoleSaveMenuItem.setToolTipText("Save the content of the console to a text file.");
        consoleSaveMenuItem.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                consoletextSaveMenuItemActionPerformed(evt);
            }
        });
        consolePopupMenu.add(consoleSaveMenuItem);
        consolePopupMenu.add(jSeparator9);

        consolePrintMenuItem.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/Crystal-Clear/22x22/apps/printer.png"))); // NOI18N
        consolePrintMenuItem.setText("Print...");
        consolePrintMenuItem.setToolTipText("Print the content of the console to a printer.");
        consolePrintMenuItem.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                consoletextPrintMenuItemActionPerformed(evt);
            }
        });
        consolePopupMenu.add(consolePrintMenuItem);

        consoleTextArea = new javax.swing.JTextArea();
        consoleTextArea.setEditable(false);
        consoleTextArea.setColumns(20);
        consoleTextArea.setLineWrap(true);
        consoleTextArea.setRows(5);
        consoleTextArea.setToolTipText("This is the console, where errors and messages go. Press right mouse button for menu.");
        consoleTextArea.setWrapStyleWord(true);
        consoleTextArea.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        consoleTextArea.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mousePressed(java.awt.event.MouseEvent evt) {
                consoleTextAreaMousePressed(evt);
            }

            @Override
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                consoleTextAreaMouseReleased(evt);
            }
        });
        super.setViewportView(consoleTextArea);

        try {
            consolePrintStream = new PrintStream(new FilteredStream(new ByteArrayOutputStream()),
                    false, "US-ASCII");
        } catch (UnsupportedEncodingException ex) {
            assert false;
        }
    }
}
