/*
Copyright (C) 2013, 2014 Bengt Martensson.

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

package org.harctoolbox.irscrutinizer.exporter;

import java.awt.Component;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import javax.swing.JFileChooser;
import org.harctoolbox.guicomponents.SelectFile;
import org.harctoolbox.irscrutinizer.Utils;

/**
 * This class is a common base class of all the exporters.
 */
public abstract class Exporter {
    /**
     * @param aDateFormatString the dateFormatString to set
     */
    public static void setDateFormatString(String aDateFormatString) {
        dateFormatString = aDateFormatString;
    }

    public String getDateFormatString() {
        return dateFormatString;
    }

    /**
     * @param aDateFormatFileString the dateFormatFileString to set
     */
    public static void setDateFormatFileString(String aDateFormatFileString) {
        dateFormatFileString = aDateFormatFileString;
    }

    private static File lastSaveFile = null;
    private static final String defaultDateFormatString = "yyyy-MM-dd_HH:mm:ss";
    private static final String defaultDateFormatFileString = "yyyy-MM-dd_HH-mm-ss";
    private static String dateFormatString = defaultDateFormatString;
    private static String dateFormatFileString = defaultDateFormatFileString;

    protected Exporter() {
    }

    public abstract String[][] getFileExtensions();

    // Dummy
    public abstract String getPreferredFileExtension();

    private static void checkExportDir(File exportDir) throws IOException {
        if (!exportDir.exists()) {
            boolean success = exportDir.mkdirs();
            if (!success)
                throw new IOException("Export directory " + exportDir + " does not exist, attempt to create failed.");
        }
        if (!exportDir.isDirectory() || !exportDir.canWrite())
            throw new IOException("Export directory `" + exportDir + "' is not a writable directory.");
    }

    public abstract String getFormatName();

    private File selectExportFile(Component parent, File exportDir) {
        File answer = SelectFile.selectFile(parent, "Select file for " + getFormatName() + " export.",
                exportDir.getPath(), true, false, JFileChooser.FILES_ONLY, getFileExtensions());
        if (answer != null && getPreferredFileExtension() != null && ! getPreferredFileExtension().isEmpty())
            answer = new File(Utils.addExtensionIfNotPresent(answer.getPath(), getPreferredFileExtension()));
        return answer;
    }

    private File automaticFilename(File exportDir) throws IOException {
        checkExportDir(exportDir);
        String name = getFormatName().toLowerCase(Locale.US) + "_" + (new SimpleDateFormat(dateFormatFileString)).format(new Date());
        if (getPreferredFileExtension() != null)
            name += "." + getPreferredFileExtension();
        return new File(exportDir, name);
    }

    public File exportFilename(boolean automatic, Component parent, File exportDir) throws IOException {
        File file =  automatic ? automaticFilename(exportDir) : selectExportFile(parent, exportDir);
        if (file != null)
            setLastSaveFile(file);
        return file;
    }

    protected static String getDateString() {
        return (new SimpleDateFormat(dateFormatString)).format(new Date());
    }

    public synchronized static File getLastSaveFile() {
        return lastSaveFile;
    }

    private synchronized static void setLastSaveFile(File theLastSaveFile) {
        lastSaveFile = theLastSaveFile;
    }
}
