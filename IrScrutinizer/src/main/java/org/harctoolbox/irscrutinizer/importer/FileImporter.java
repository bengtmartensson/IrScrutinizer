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

package org.harctoolbox.irscrutinizer.importer;

import java.awt.Component;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.text.ParseException;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import javax.swing.JFileChooser;
import org.harctoolbox.IrpMaster.IrpMasterException;
import org.harctoolbox.IrpMaster.IrpUtils;
import org.harctoolbox.guicomponents.SelectFile;
import org.harctoolbox.irscrutinizer.Version;

/**
 * This class extends the Importer with file load functions.
 */
public abstract class FileImporter extends Importer {

    protected FileImporter() {
        super();
    }

    public abstract void load(File file, String origin) throws IOException, ParseException;

    public final void load(File file) throws IOException, ParseException {
        load(file, file.getPath());
    }

    public boolean loadFileSelector(Component component, String title, String defaultDir) throws IOException, ParseException, IrpMasterException {
        File file = SelectFile.selectFile(component, title, defaultDir, false, false,
            canImportDirectories() ? JFileChooser.FILES_AND_DIRECTORIES : JFileChooser.FILES_ONLY, getFileExtensions());
        if (file == null)
            return false;
        load(file);
        return true;
    }

    protected void dumbLoad(Reader reader, String origin) throws FileNotFoundException, IOException, ParseException {
        FileOutputStream out = null;
        try {
            File file = File.createTempFile(Version.appName + origin, null);
            out = new FileOutputStream(file);
            while (true) {
                int c = reader.read(); // not written to be efficient...
                if (c == -1)
                    break;
                out.write(c);
            }
            load(file);
        } finally {
            if (out != null)
                out.close();
        }
    }

    // Reason there is no version returning an InputStream: Such an InputStream would
    // close when the zip file is closed; i.e. the zip file has to be left open,
    // and the caller has to take responsibility for somehow closing the zip file.
    // A reasonably clean solution would be to have a protected class for the zip file
    // -- just not worth it.
    public void possiblyZipLoad(File file) throws ParseException, IOException {
        if (file.getName().toLowerCase(IrpUtils.dumbLocale).endsWith(".zip")) {
            String extension = getFileExtensions()[0][1];
            File tmpFile = unzipFirstMatch(file, extension);
            boolean success;
            // I do not understand when File.delete throws an exception,
            // and when it just returns false.
            // Better to be on the safe side...
            try {
                load(tmpFile, file.getPath());
            } finally {

                success = tmpFile.delete();
            }
            if (!success)
                throw new IOException("Deletion of temporary file " + file.getAbsolutePath() + " failed.");
        } else
            load(file);
    }

    private File unzipFirstMatch(File file, String extension) throws IOException {
        ZipFile zip = new ZipFile(file, ZipFile.OPEN_READ);
        String payload = null;
        ZipEntry entry = null;
        int index = 0;
        for (Enumeration<? extends ZipEntry> enumeration = zip.entries();
                payload == null && enumeration.hasMoreElements();) {
            entry = enumeration.nextElement();
            if (extension == null
                    || (index == 0 && !enumeration.hasMoreElements()) // first and only element
                    || entry.getName().toLowerCase(IrpUtils.dumbLocale).endsWith(extension))
                payload = entry.getName();
            index++;
        }
        if (payload == null) {
            zip.close();
            throw new IOException(
                    (extension == null
                    ? "No content"
                    : ("No file with extension ." + extension))
                    + " in " + file.getCanonicalPath());
        }

        InputStream in = null;
        FileOutputStream out = null;
        File outFile = null;
        try {
            in = zip.getInputStream(entry);
            outFile = File.createTempFile(Version.appName + origin, null);
            outFile.deleteOnExit();
            out = new FileOutputStream(outFile);
            byte[] buf = new byte[8192];
            while (true) {
                int len = in.read(buf);
                if (len == -1)
                    break;
                out.write(buf, 0, len);
            }
        } finally {
            try {
                zip.close();
            } finally {
                if (out != null)
                    out.close();
                if (in != null)
                    in.close();
            }
        }
        return outFile;
    }

    public boolean canImportDirectories() {
        return false;
    }

    public abstract String[][] getFileExtensions();

    public String[][] getFileExtensions(boolean zip) {
        String[][] exts = getFileExtensions();
        if (!zip)
            return exts;

        String[][] extensions = new String[exts.length + 1][2];
        System.arraycopy(exts, 0, extensions, 0, exts.length);
        extensions[exts.length][0] = "Zip files (*.zip)";
        extensions[exts.length][1] = "zip";
        return extensions;
    }
}
