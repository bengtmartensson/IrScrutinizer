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

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;

@SuppressWarnings("UseOfSystemOutOrSystemErr")
public class CopyClipboardText implements ClipboardOwner {

    public static String getSelection() {
        return (new CopyClipboardText(null)).fromSystemSelection();
    }

    private GuiUtils guiUtils = null;

    public CopyClipboardText(GuiUtils guiUtils) {
        this.guiUtils = guiUtils;
    }

    @Override
    public void lostOwnership(Clipboard c, Transferable t) {
    }

    public void toClipboard(String str) {
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(str), this);
    }

    public String fromClipboard() {
        try {
            return (String) Toolkit.getDefaultToolkit().getSystemClipboard().getContents(this).getTransferData(DataFlavor.stringFlavor);
        } catch (UnsupportedFlavorException | IOException ex) {
            if (guiUtils != null)
                guiUtils.error(ex);
            else
                System.err.println(ex);
        }
        return null;
    }

    public String fromSystemSelection() {
        Clipboard selection = Toolkit.getDefaultToolkit().getSystemSelection();
        if (selection == null)
            return null;
        try {
            return (String) selection.getContents(this).getTransferData(DataFlavor.stringFlavor);
        } catch (UnsupportedFlavorException | IOException ex) {
            if (guiUtils != null)
                guiUtils.error(ex);
            else
                System.err.println(ex);
        }
        return null;
    }
}
