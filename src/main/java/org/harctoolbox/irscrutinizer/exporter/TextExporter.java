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

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import org.harctoolbox.girr.Command;
import org.harctoolbox.girr.CommandSet;
import org.harctoolbox.girr.GirrException;
import org.harctoolbox.girr.Remote;
import org.harctoolbox.girr.RemoteSet;
import org.harctoolbox.ircore.IrCoreException;
import org.harctoolbox.irp.IrpException;
import org.harctoolbox.xml.XmlUtils;
import org.w3c.dom.DocumentFragment;

/**
 * This class does something interesting and useful. Or not...
 */
public class TextExporter extends RemoteSetExporter {
    private static final DocumentFragment documentation = XmlUtils.stringToDocumentFragment("TextExporter documentation not yet written.");

    private final boolean generateRaw;
    private final boolean generateCcf;
    private final boolean generateParameters;
    private final Command.CommandTextFormat[] extraFormatters;

    public TextExporter(boolean generateRaw, boolean generateCcf,
            boolean generateParameters, Command.CommandTextFormat... extraFormatters) {
        super();
        this.generateRaw = generateRaw;
        this.generateCcf = generateCcf;
        this.generateParameters = generateParameters;
        this.extraFormatters = extraFormatters;
    }

    @Override
    public String[][] getFileExtensions() {
        return new String[][]{ new String[] { "Text files (*.txt *.text)", "txt", "text" } };
    }

    @Override
    public String getName() {
        return "Text";
    }

    @Override
    public String getPreferredFileExtension() {
        return "txt";
    }

    @Override
    public DocumentFragment getDocumentation() {
        return documentation;
    }

    @Override
    public boolean supportsEmbeddedFormats() {
        return true;
    }

    @Override
    public void export(RemoteSet remoteSet, String title, File file, String charsetName)
            throws IOException, GirrException, IrCoreException, IrpException {
        if (file != null)
            try (PrintStream printStream = new PrintStream(file, charsetName)) {
                for (Remote remote : remoteSet)
                    for (CommandSet commandSet : remote)
                        for (Command command : commandSet)
                            printStream.println(formatCommand(command, 1));
            }
    }

    private String formatCommand(Command command, int count) throws GirrException, IrpException, IrCoreException {
        StringBuilder str = new StringBuilder(128);
        String linefeed = System.getProperty("line.separator", "\n");
        if (generateParameters) {
            command.checkForParameters();
            str.append(command.nameProtocolParameterString());
        } else
            str.append(command.getName());
        str.append(linefeed);

        if (generateCcf) {
            str.append(command.getProntoHex()).append(linefeed);
        }
        if (generateRaw) {
            str.append(command.getIntro()).append(linefeed);
            str.append(command.getRepeat()).append(linefeed);
            if (command.getEnding() != null && !command.getEnding().isEmpty())
                str.append(command.getEnding()).append(linefeed);
        }
        for (Command.CommandTextFormat formatter : extraFormatters) {
            command.addFormat(formatter, count);
            str.append(command.getFormat(formatter.getName())).append(linefeed);
        }
        return str.toString();
    }
}
