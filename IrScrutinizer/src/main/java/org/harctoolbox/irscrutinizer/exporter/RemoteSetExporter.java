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
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import javax.xml.transform.TransformerException;
import org.harctoolbox.IrpMaster.DecodeIR;
import org.harctoolbox.IrpMaster.IrpMasterException;
import org.harctoolbox.girr.Command;
import org.harctoolbox.girr.Remote;
import org.harctoolbox.girr.RemoteSet;
import org.harctoolbox.irscrutinizer.Version;

/**
 * This class does something interesting and useful. Or not...
 */
public abstract class RemoteSetExporter extends Exporter {

    protected String creatingUser;

    protected  RemoteSetExporter() {
        this(System.getProperty("user.name", "unknown"));
    }

    protected RemoteSetExporter(String creatingUser) {
        super();
        this.creatingUser = creatingUser;
    }

    public void export(RemoteSet remoteSet, String title, int repeatCount, boolean automaticFilenames,
            Component parent, File exportDir, String charsetName)
            throws IOException, IrpMasterException, TransformerException {
        export(remoteSet, title, repeatCount, exportFilename(automaticFilenames, parent, exportDir), charsetName);
    }

    public abstract void export(RemoteSet remoteSet, String title, int repeatCount, File saveFile, String charsetName) throws IOException, IrpMasterException, TransformerException;

    public void export(Remote remote, String title, String source, int repeatCount, File saveFile, String charsetName) throws IrpMasterException, IOException, TransformerException {
        RemoteSet remoteSet = new RemoteSet(creatingUser,
                source,
                Exporter.getDateString(), //java.lang.String creationDate,
                Version.appName, //java.lang.String tool,
                Version.version, //java.lang.String toolVersion,
                DecodeIR.appName, //java.lang.String tool2,
                DecodeIR.getVersion(), //java.lang.String tool2Version,
                null, //java.lang.String notes,
                remote);
        export(remoteSet, title, repeatCount, saveFile, charsetName);
    }

    public void export(Map<String, Command> commands, String source, String title,
            Remote.MetaData metaData,
            int repeatCount, File saveFile, String charsetName) throws IrpMasterException, IOException, TransformerException {
        Remote remote = new Remote(
                metaData,
                "Export from " + Version.appName, //                String comment,
                null, //                       String notes,
                commands,
                null, //     HashMap<String, HashMap<String, String>> applicationParameters,
                null, //String protocol,
                null //HashMap<String,Long>parameters
        );
        export(remote, title, source, repeatCount, saveFile, charsetName);
    }

    public File export(Map<String, Command> commands, String source, String title,
            Remote.MetaData metaData,
            int repeatCount, boolean automaticFilenames, Component parent, File exportDir, String charsetName)
            throws IrpMasterException, IOException, TransformerException {
        File file = exportFilename(automaticFilenames, parent, exportDir);
        if (file == null)
            return null;
        Remote remote = new Remote(
                metaData,
                "Export from " + Version.appName, //                String comment,
                null, //                       String notes,
                commands,
                null, //     HashMap<String, HashMap<String, String>> applicationParameters,
                null, //String protocol,
                null //HashMap<String,Long>parameters
                );
        export(remote, title, source, repeatCount, file, charsetName);
        return file;
    }

    public void export(Collection<Command> commands, String source, String title, int repeatCount,
            File saveFile, String charsetName) throws IOException, IrpMasterException, TransformerException {
        Map<String, Command> cmds = new HashMap<>(32);
        for (Command command : commands)
            cmds.put(command.getName(), command);

        export(cmds, source, title, new Remote.MetaData(), repeatCount, saveFile, charsetName);
    }

    public File export(Command command, String title, String source, int repeatCount,
            boolean automaticFilenames, Component parent, File exportDir, String charsetName)
            throws IOException, IrpMasterException, TransformerException {
        File file = exportFilename(automaticFilenames, parent, exportDir);
        if (file == null)
            return null;
        export(command, title, source, repeatCount, file, charsetName);
        return file;
    }

    public void export(Command command, String title, String source, int repeatCount, File saveFile, String charsetName)
            throws IrpMasterException, IOException, TransformerException {
        Map<String,Command> commands = new HashMap<>(1);
        commands.put(command.getName(), command);
        export(commands, title, source, new Remote.MetaData(Version.appName + "Export"), repeatCount, saveFile, charsetName);
    }

    public boolean supportsEmbeddedFormats() {
        return false;
    }

    public boolean considersRepetitions() {
        return false;
    }

    public boolean supportsMetaData() {
        return false;
    }
}
