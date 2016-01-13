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
import java.io.FileNotFoundException;
import java.io.IOException;
import org.harctoolbox.IrpMaster.IrpMasterException;
import org.harctoolbox.girr.Command;

/**
 * This class does something interesting and useful. Or not...
 */
public abstract class CommandExporter extends Exporter {

    protected CommandExporter() {
        super();
    }

    public abstract void export(Command command, String source, String title, int repeatCount, File exportFile, String charsetName)
            throws IrpMasterException, FileNotFoundException;

    public File export(Command command, String source, String title, int repeatCount,
            boolean automaticFilenames, Component parent, File exportDir, String charsetName) throws IrpMasterException, IOException {
        File file = exportFilename(automaticFilenames, parent, exportDir);
        export(command, source, title, repeatCount, file, charsetName);
        return file;
    }

    public boolean considersRepetitions() {
        return false;
    }
}
