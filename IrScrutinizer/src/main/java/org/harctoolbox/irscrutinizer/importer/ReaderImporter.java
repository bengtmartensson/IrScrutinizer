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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.text.ParseException;
import org.harctoolbox.ircore.InvalidArgumentException;

/**
 * This class extends the Importer with file/reader load functions.
 */
public abstract class ReaderImporter extends FileImporter {

    protected ReaderImporter() {
        super();
    }

    public abstract void load(Reader reader, String origin) throws IOException, FileNotFoundException, ParseException, InvalidArgumentException;

    @Override
    public void load(File file, String origin, String charsetName) throws IOException, ParseException, InvalidArgumentException {
        try (InputStream inputStream = new FileInputStream(file)) {
            load(inputStream, origin, charsetName);
        }
    }

    public void load(String charsetName) throws IOException, ParseException, InvalidArgumentException {
        load(System.in, "<STDIN>", charsetName);
    }

    public void load(InputStream inputStream, String origin, String charsetName) throws ParseException, IOException, InvalidArgumentException {
        try (Reader reader = new InputStreamReader(inputStream, charsetName)) {
            load(reader, origin);
        }
    }

    private void loadURL(String urlOrFilename, String charsetName) throws MalformedURLException, IOException, ParseException, InvalidArgumentException {
        URL url = new URL(urlOrFilename);
        URLConnection urlConnection = url.openConnection();
        try (InputStream inputStream = urlConnection.getInputStream()) {
            load(inputStream, urlOrFilename, charsetName);
        }
    }

    public void load(String payload, String origin, String charsetName) throws IOException, ParseException, InvalidArgumentException {
        load(new ByteArrayInputStream(payload.getBytes(charsetName)), origin, charsetName);
    }

    public void load(String urlOrFilename, boolean zip, String charsetName) throws IOException, ParseException, InvalidArgumentException {
        if (urlOrFilename == null || urlOrFilename.isEmpty())
            throw new IOException("Empty file name/URL");

        try {
           loadURL(urlOrFilename, charsetName);
        } catch (MalformedURLException ex) {
            if (zip)
                possiblyZipLoad(new File(urlOrFilename), charsetName);
            else
                load(new File(urlOrFilename), charsetName);
        }
    }

    public void load(String urlOrFilename, String charsetName) throws IOException, ParseException, InvalidArgumentException {
        load(urlOrFilename, false, charsetName);
    }
}
