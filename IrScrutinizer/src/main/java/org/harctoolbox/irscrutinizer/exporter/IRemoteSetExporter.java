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
import java.util.Collection;
import java.util.HashMap;
import org.harctoolbox.IrpMaster.IrpMasterException;
import org.harctoolbox.girr.Command;
import org.harctoolbox.girr.Remote;
import org.harctoolbox.girr.RemoteSet;

/**
 *
 */
public interface IRemoteSetExporter extends ICommandExporter {
    public void export(RemoteSet remoteSet, String title, int count, boolean automaticFilenames,
            Component parent, File exportDir, String charsetName)
            throws FileNotFoundException, IOException, IrpMasterException;

    public abstract void export(RemoteSet remoteSet, String title, int count, File saveFile, String charsetName) throws FileNotFoundException, IOException, IrpMasterException;

    public void export(Remote remote, String title, String source, int count, File saveFile, String charsetName) throws FileNotFoundException, IrpMasterException, IOException;

    public void export(HashMap<String, Command> commands, String source, String title,
            Remote.MetaData metaData,
            int count, File saveFile, String charsetName) throws FileNotFoundException, IrpMasterException, IOException;

    public File export(HashMap<String, Command> commands, String source, String title,
            Remote.MetaData metaData, int count, boolean automaticFilenames, Component parent, File exportDir, String charsetName)
            throws FileNotFoundException, IrpMasterException, IOException;

    public void export(Collection<Command> commands, String source, String title, int count, File saveFile, String charsetName)
            throws FileNotFoundException, IOException, IrpMasterException;

    public boolean supportsEmbeddedFormats();
}
