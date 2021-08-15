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
import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
import org.harctoolbox.girr.Command;
import org.harctoolbox.girr.RemoteSet;
import org.harctoolbox.xml.XmlUtils;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;

/**
 * This class does something interesting and useful. Or not...
 */
public class GirrExporter extends RemoteSetExporter {

    private final static DocumentFragment documentation = parseToDocumentFragment("<div>Girr documentation <b>not</b> yet <a href=\"http://www.harctoolbox.org\">written</a></div>");

    private final boolean fatRaw;
    private final boolean generateRaw;
    private final boolean generateCcf;
    private final boolean generateParameters;
    private final Command.CommandTextFormat[] extraFormats;

    public GirrExporter(boolean fatRaw, boolean generateRaw, boolean generateCcf,
            boolean generateParameters, Command.CommandTextFormat... extraFormats) {
        super();
        this.fatRaw = fatRaw;
        this.generateRaw = generateRaw;
        this.generateCcf = generateCcf;
        this.generateParameters = generateParameters;
        this.extraFormats = extraFormats;
    }

    @Override
    public void export(RemoteSet remoteSet, String title, int count, File file, String charsetName) throws FileNotFoundException, UnsupportedEncodingException  {
        for (Command.CommandTextFormat formatter : extraFormats)
            remoteSet.addFormat(formatter, count);
        Document document = remoteSet.toDocument(title, fatRaw, generateParameters, generateCcf, generateRaw);
        XmlUtils.printDOM(file, document, charsetName, null);
    }

    @Override
    public String[][] getFileExtensions() {
        return new String[][] { new String[]{ "Girr files (*.girr)", "girr"}, new String[]{ "XML files", "xml"}};
    }

    @Override
    public String getPreferredFileExtension() {
        return "girr";
    }

    @Override
    public String getFormatName() {
        return "Girr";
    }

    @Override
    public boolean supportsEmbeddedFormats() {
        return true;
    }

    @Override
    public boolean supportsMetaData() {
        return true;
    }

    @Override
    public DocumentFragment getDocumentation() {
        return documentation;
    }

    @Override
    protected boolean isExecutable() {
        return false;
    }
}
