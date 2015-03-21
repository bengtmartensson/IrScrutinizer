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

import java.awt.Component;
import java.io.File;
import java.util.HashMap;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;

/**
 * This class packs a number of file selectors.
 *
 */
public class SelectFile {

    private static final HashMap<String, String> filechooserdirs = new HashMap<String, String>();

    /**
     * Version of the file selector with exactly one file extension.
     *
     * @param parent
     * @param title
     * @param save
     * @param defaultdir
     * @param showHiddenFiles
     * @param extension
     * @param fileTypeDesc
     * @return Selected File, or null.
     */
    public static File selectFile(Component parent, String title, String defaultdir, boolean save, boolean showHiddenFiles, String fileTypeDesc, String extension) {
        return selectFile(parent, title, defaultdir, save, showHiddenFiles, JFileChooser.FILES_ONLY, new String[]{fileTypeDesc, extension});
    }

    /**
     * Encapsulates a file selector. The finally selected direcory will be remembered, and used as initial direcory for subsequent invocations with the same title.
     *
     * @param parent Component, to which the popup will be positioned.
     * @param title Title of the popup. Also identifies the file selector.
     * @param save True iff the file is to be written.
     * @param defaultdir Default direcory if not stored in the class' static memory.
     * @param showHiddenFiles If true show also "hidden files".
     * @param mode the type of files to be displayed: JFileChooser.FILES_ONLY, JFileChooser.DIRECTORIES_ONLY, JFileChooser.FILES_AND_DIRECTORIES.
     * @param filetypes Variable number of file extensions, as pair of strings.
     * @return Selected File, or null.
     */
    public static File selectFile(Component parent, String title, String defaultdir, boolean save, boolean showHiddenFiles, int mode, String[]... filetypes) {
        String startdir = filechooserdirs.containsKey(title) ? filechooserdirs.get(title) : defaultdir;
        JFileChooser chooser = new JFileChooser(startdir);
        chooser.setFileHidingEnabled(!showHiddenFiles);
        chooser.setDialogTitle(title);
        chooser.setFileSelectionMode(mode);
        if (filetypes != null && filetypes.length > 0) {
            chooser.setFileFilter(new FileNameExtensionFilter(filetypes[0][0], cdr(filetypes[0])));
            for (int i = 1; i < filetypes.length; i++) {
                chooser.addChoosableFileFilter(new FileNameExtensionFilter(filetypes[i][0], cdr(filetypes[i])));
            }
        }
        int result = save ? chooser.showSaveDialog(parent) : chooser.showOpenDialog(parent);

        if (result == JFileChooser.APPROVE_OPTION) {
            filechooserdirs.put(title, chooser.getSelectedFile().getAbsoluteFile().getParent());
            return chooser.getSelectedFile();
        } else {
            return null;
        }
    }

    private static String[] cdr(String[] in) {
        String[] result = new String[in.length - 1];
        System.arraycopy(in, 1, result, 0, result.length);
        return result;
    }

    private SelectFile() {
    }
}
