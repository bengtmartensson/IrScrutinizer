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
import org.harctoolbox.IrpMaster.IrpMasterException;

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
     * @throws org.harctoolbox.IrpMaster.IrpMasterException
     */
    public void load(Reader reader, String origin) throws IOException, ParseException, IrpMasterException;

    /**
     * Loads from stdin.
     * @throws IOException
     * @throws ParseException
     * @throws org.harctoolbox.IrpMaster.IrpMasterException
     */
    public void load() throws IOException, ParseException, IrpMasterException;

    public void load(InputStream inputStream, String origin) throws IOException, ParseException, IrpMasterException;

    /**
     * Load from the string given as the first argument.
     *
     * @param payload
     * @param origin
     * @throws IOException
     * @throws ParseException
     * @throws org.harctoolbox.IrpMaster.IrpMasterException
     */
    public void load(String payload, String origin) throws IOException, ParseException, IrpMasterException;

    /**
     * If the argument can be parsed as an URL string, load from its content.
     * Otherwise, consider it as a file name, and load its content.
     * @param urlOrFilename
     * @throws IOException
     * @throws ParseException
     * @throws org.harctoolbox.IrpMaster.IrpMasterException
     */
    public void load(String urlOrFilename) throws IOException, ParseException, IrpMasterException;

    public void load(String urlOrFilename, boolean zip) throws IOException, ParseException;
}
