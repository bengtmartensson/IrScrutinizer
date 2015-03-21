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

import javax.swing.JTextField;

public class StatusLine extends JTextField {
    private static final long serialVersionUID = 1L;

    private final CopyPastePopupMenu copyPastePopupMenu;

    public StatusLine() {
        super();
        copyPastePopupMenu = new CopyPastePopupMenu(false);
        setEditable(false);
        setToolTipText("Status line");
        setText(null);
        addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mousePressed(java.awt.event.MouseEvent evt) {
                genericCopyMenu(evt);
            }
            @Override
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                genericCopyMenu(evt);
            }
        });
    }

    private void genericCopyMenu(java.awt.event.MouseEvent evt) {
        if (evt.isPopupTrigger())
           copyPastePopupMenu.show(evt.getComponent(), evt.getX(), evt.getY());
    }

    public void setStatus(String message) {
        this.setText(message);
    }

}
