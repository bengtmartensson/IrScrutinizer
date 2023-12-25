/*
Copyright (C) 2016 Bengt Martensson.

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

import java.awt.HeadlessException;
import javax.swing.ImageIcon;
import javax.swing.JOptionPane;
import org.harctoolbox.irp.IrpUtils;

/**
 * This class is intended for a final error message.
 */
public class StandalonePopupAnnoyer {

    private static boolean errorContinue(String message) {
        Object[] options = {"Continue", "Exit"};
        int ans = JOptionPane.showOptionDialog(null, message.replaceAll("\\\\n", "\n"), "Error",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.ERROR_MESSAGE,
                new ImageIcon(GuiUtils.class.getResource("/icons/Crystal-Clear/48x48/apps/error.png")),
                options, // the titles of buttons
                options[1]); //default button title
        return ans == 0;
    }

    @SuppressWarnings("UseOfSystemOutOrSystemErr")
    public static void main(String[] args) {
        try {
            boolean result = errorContinue(args[0]);
            System.exit(result ? IrpUtils.EXIT_SUCCESS : IrpUtils.EXIT_USAGE_ERROR);
        } catch (HeadlessException ex) {
            System.err.println(args[0]);
            System.err.println();
            System.err.println("This program does not run in headless mode.");
            System.exit(IrpUtils.EXIT_USAGE_ERROR);
        }
    }
}
