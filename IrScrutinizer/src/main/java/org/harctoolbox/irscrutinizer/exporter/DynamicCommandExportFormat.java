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

import java.io.*;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import org.harctoolbox.IrpMaster.IrpMasterException;
import org.harctoolbox.girr.RemoteSet;
import org.harctoolbox.girr.XmlExporter;
import org.w3c.dom.Document;

/**
 *
 */
public class DynamicCommandExportFormat extends RemoteSetExporter implements ICommandExporter {
    private final Document xslt;

    DynamicCommandExportFormat(String nm, String ext, String documentation, URL url, List<Option>options,
            boolean simpleSequence, boolean binary, Document stylesheet) {
        super(nm, DynamicRemoteSetExportFormat.mkExtensions(nm, ext), ext, documentation, url, options, simpleSequence, binary);
        xslt = stylesheet;
    }

    @Override
    public void export(RemoteSet remoteSet, String title, int noRepeats, File saveFile, String charsetName)
            throws IrpMasterException, FileNotFoundException, IOException {

        Document document = remoteSet.xmlExportDocument(title,
                null,
                null,
                true, //fatRaw,
                false, // createSchemaLocation,
                true, //generateRaw,
                true, //generateCcf,
                true //generateParameters)
                );
        XmlExporter xmlExporter = new XmlExporter(document);
        try (OutputStream out = new FileOutputStream(saveFile)) {
            HashMap<String, String> parameters = new HashMap<>(1);
            parameters.put("noRepeats", Integer.toString(noRepeats));
            xmlExporter.printDOM(out, xslt, parameters, isBinary(), charsetName);
        }
    }
}
