/*
Copyright (C) 2012, 2013 Bengt Martensson.

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

import java.awt.event.ActionListener;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.text.JTextComponent;

@SuppressWarnings("serial")
public class CopyPastePopupMenu extends JPopupMenu {

    private JMenuItem copyMenuItem;
    private JMenuItem pasteMenuItem;

    public CopyPastePopupMenu() {
        this(false);
    }

    public CopyPastePopupMenu(boolean paste) {
        super();
        copyMenuItem = new JMenuItem();

        copyMenuItem.setIcon(new javax.swing.ImageIcon(CopyPastePopupMenu.class.getResource("/icons/Crystal-Clear/22x22/actions/editcopy.png"))); // NOI18N
        copyMenuItem.setMnemonic('C');
        copyMenuItem.setText("Copy");
        copyMenuItem.setToolTipText("Copy content of window to clipboard (ignoring any selection).");
        copyMenuItem.addActionListener(new ActionListenerImpl());
        add(copyMenuItem);

        if (paste) {
            pasteMenuItem = new JMenuItem();
            pasteMenuItem.setIcon(new javax.swing.ImageIcon(CopyPastePopupMenu.class.getResource("/icons/Crystal-Clear/22x22/actions/editpaste.png"))); // NOI18N
            pasteMenuItem.setText("Paste");
            pasteMenuItem.setToolTipText("Paste from clipboard, replacing previous content.");
            pasteMenuItem.addActionListener(new ActionListenerImpl1());
            add(pasteMenuItem);
        }
    }

    private static class ActionListenerImpl implements ActionListener {

        ActionListenerImpl() {
        }

        @Override
        public void actionPerformed(java.awt.event.ActionEvent evt) {
            JMenuItem jmi = (JMenuItem)evt.getSource();
            JPopupMenu jpm = (JPopupMenu)jmi.getParent();
            JTextComponent jtf = (JTextComponent) jpm.getInvoker();
            (new CopyClipboardText(null)).toClipboard(jtf.getText());
        }
    }

    private static class ActionListenerImpl1 implements ActionListener {

        ActionListenerImpl1() {
        }

        @Override
        public void actionPerformed(java.awt.event.ActionEvent evt) {
            JMenuItem jmi = (JMenuItem) evt.getSource();
            JPopupMenu jpm = (JPopupMenu) jmi.getParent();
            JTextComponent jtf = (JTextComponent) jpm.getInvoker();
            jtf.setText((new CopyClipboardText(null)).fromClipboard());
        }
    }
}
