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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import javax.swing.JFileChooser;
import org.harctoolbox.guicomponents.SelectFile;
import org.harctoolbox.ircore.IrCoreUtils;

/**
 * This class is a common base class of all the exporters.
 */
public abstract class Exporter {

    private static File lastSaveFile = null;
    private static final String defaultDateFormatString = "yyyy-MM-dd_HH:mm:ss";
    private static final String defaultDateFormatFileString = "yyyy-MM-dd_HH-mm-ss";
    private static String dateFormatString = defaultDateFormatString;
    private static String dateFormatFileString = defaultDateFormatFileString;

    /**
     * @param aDateFormatString the dateFormatString to set
     */
    public static void setDateFormatString(String aDateFormatString) {
        dateFormatString = aDateFormatString;
    }

    /**
     * @param aDateFormatFileString the dateFormatFileString to set
     */
    public static void setDateFormatFileString(String aDateFormatFileString) {
        dateFormatFileString = aDateFormatFileString;
    }

    private static void checkExportDir(File exportDir) throws IOException {
        if (!exportDir.exists()) {
            boolean success = exportDir.mkdirs();
            if (!success)
                throw new IOException("Export directory " + exportDir + " does not exist, attempt to create failed.");
        }
        if (!exportDir.isDirectory() || !exportDir.canWrite())
            throw new IOException("Export directory `" + exportDir + "' is not a writable directory.");
    }
    protected static String getDateString() {
        return (new SimpleDateFormat(dateFormatString)).format(new Date());
    }

    public synchronized static File getLastSaveFileOrCopy() throws IOException {
        if (lastSaveFile == null || !lastSaveFile.getName().endsWith(".girr"))
            return lastSaveFile;

        Path newPath = Paths.get(lastSaveFile.getCanonicalPath() + ".txt");
        Files.copy(Paths.get(lastSaveFile.getCanonicalPath()), newPath, REPLACE_EXISTING);
        return newPath.toFile();
    }

    private synchronized static void setLastSaveFile(File theLastSaveFile) {
        lastSaveFile = theLastSaveFile;
    }

    private final boolean executable;
    private final String encoding;

    protected Exporter(boolean executable, String encoding) {
        this.executable = executable;
        this.encoding = encoding;
    }

    protected void possiblyMakeExecutable(File file) {
        if (executable)
            file.setExecutable(true, false);
    }

    public String getEncoding() {
        return encoding;
    }

    public String getDateFormatString() {
        return dateFormatString;
    }

    public abstract String[][] getFileExtensions();

    // Dummy
    public abstract String getPreferredFileExtension();


    public abstract String getFormatName();

    private File selectExportFile(Component parent, File exportDir) {
        File answer = SelectFile.selectFile(parent, "Select file for " + getFormatName() + " export.",
                exportDir.getPath(), true, false, JFileChooser.FILES_ONLY, getFileExtensions());
        if (answer != null && getPreferredFileExtension() != null && ! getPreferredFileExtension().isEmpty())
            answer = new File(IrCoreUtils.addExtensionIfNotPresent(answer.getPath(), getPreferredFileExtension()));
        return answer;
    }

    private File automaticFilename(File exportDir) throws IOException {
        checkExportDir(exportDir);
        String cleanedFormatName = getFormatName().toLowerCase(Locale.US).replaceAll("[^a-z0-9_\\-\\.]", "_");
        String name = cleanedFormatName + "_" + (new SimpleDateFormat(dateFormatFileString)).format(new Date());
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
}
