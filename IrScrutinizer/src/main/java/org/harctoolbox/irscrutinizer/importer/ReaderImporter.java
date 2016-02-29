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

package org.harctoolbox.irscrutinizer.importer;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.text.ParseException;
import org.harctoolbox.IrpMaster.IrpMasterException;

/**
 * This class extends the Importer with file/reader load functions.
 */
public abstract class ReaderImporter extends FileImporter {

    protected ReaderImporter() {
        super();
    }

    public abstract void load(Reader reader, String origin) throws IOException, ParseException;

    @Override
    public void load(File file, String origin, String charsetName) throws IOException, ParseException {
        load(new InputStreamReader(new FileInputStream(file), charsetName), origin);
    }

    public void load(String charsetName) throws IOException, ParseException, IrpMasterException {
        load(System.in, "<STDIN>", charsetName);
    }

    public void load(InputStream inputStream, String origin, String charsetName) throws IOException, ParseException {
        load(new InputStreamReader(inputStream, charsetName), origin);
    }

    public void load(String payload, String origin, String charsetName) throws IOException, ParseException, IrpMasterException {
        //load(new StringReader(payload), origin);
        load(new ByteArrayInputStream(payload.getBytes(charsetName)), origin, charsetName);
    }

    public void load(String urlOrFilename, boolean zip, String charsetName) throws IOException, ParseException {
        if (urlOrFilename == null || urlOrFilename.isEmpty())
            throw new IOException("Empty file name/URL");
        try {
            URL url = new URL(urlOrFilename);
            URLConnection urlConnection = url.openConnection();
            InputStream inputStream = urlConnection.getInputStream();
            load(inputStream, urlOrFilename, charsetName);
        } catch (MalformedURLException ex) {
            if (zip)
                possiblyZipLoad(new File(urlOrFilename), charsetName);
            else
                load(new File(urlOrFilename), charsetName);
        }
    }

    public void load(String urlOrFilename, String charsetName) throws IOException, ParseException, IrpMasterException {
        load(urlOrFilename, false, charsetName);
    }

    //public final void possiblyZipLoad(String urlOrFilename) throws IOException, ParseException {
    //    load(urlOrFilename, true);
    //}

    /*public boolean loadFileSelector(Component component, String title, String defaultDir) throws IOException, ParseException {
        File file = SelectFile.selectFile(component, title, defaultDir, false, false,
            canImportDirectories() ? JFileChooser.FILES_AND_DIRECTORIES : JFileChooser.FILES_ONLY, getFileExtensions());
        if (file == null)
            return false;
        load(file);
        return true;
    }

    protected void sillyLoad(Reader reader, String origin) throws FileNotFoundException, IOException, ParseException {
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

    public boolean canImportDirectories() {
        return false;
    }

    public abstract String[][] getFileExtensions();*/
}
