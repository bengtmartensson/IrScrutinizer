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

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.text.ParseException;
import org.harctoolbox.ircore.InvalidArgumentException;

/**
 * This class models reading from either a file or a stream (Reader), but not from a data base.
 */
public interface IReaderImporter extends IFileImporter {

    /**
     *
     * @param reader
     * @param origin
     * @throws IOException Generic IO error.
     * @throws ParseException Generic parse error.
     * @throws org.harctoolbox.ircore.InvalidArgumentException
     */
    public void load(Reader reader, String origin) throws IOException, ParseException, InvalidArgumentException;

    /**
     * Loads from stdin.
     * @param charsetName
     * @throws IOException
     * @throws ParseException
     * @throws org.harctoolbox.ircore.InvalidArgumentException
     */
    public void load(String charsetName) throws IOException, ParseException, InvalidArgumentException;

    /**
     *
     * @param inputStream
     * @param origin
     * @param charsetName
     * @throws IOException
     * @throws ParseException
     * @throws org.harctoolbox.ircore.InvalidArgumentException
     */
    public void load(InputStream inputStream, String origin, String charsetName) throws IOException, ParseException, InvalidArgumentException;

    /**
     * Load from the string given as the first argument.
     *
     * @param payload
     * @param origin
     * @param charsetName
     * @throws IOException
     * @throws ParseException
     * @throws org.harctoolbox.ircore.InvalidArgumentException
     */
    public void load(String payload, String origin, String charsetName) throws IOException, ParseException, InvalidArgumentException;

    /**
     * If the argument can be parsed as an URL string, load from its content.
     * Otherwise, consider it as a file name, and load its content.
     * @param urlOrFilename
     * @param charsetName
     * @throws IOException
     * @throws ParseException
     * @throws org.harctoolbox.ircore.InvalidArgumentException
     */
    public void load(String urlOrFilename, String charsetName) throws IOException, ParseException, InvalidArgumentException;

    public void load(String urlOrFilename, boolean zip, String charsetName) throws IOException, ParseException, InvalidArgumentException;
}
