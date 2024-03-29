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
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import javax.xml.transform.TransformerException;
import org.harctoolbox.girr.Command;
import org.harctoolbox.ircore.IrCoreUtils;
import org.harctoolbox.xml.XmlUtils;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * This class does something interesting and useful. Or not...
 */
public class DynamicCommandExportFormat extends CommandExporter {

    private final String name;
    private final String extension;
    private final boolean simpleSequence;
    private final boolean binary;
    private final Document xslt;
    private final boolean executable;
    private final DocumentFragment documentation;

    public DynamicCommandExportFormat(Element el, String documentURI) {
        super();
        executable = Boolean.parseBoolean(el.getAttribute("executable"));
        documentation = DynamicRemoteSetExportFormat.extractDocumentation(el);
        this.name = el.getAttribute("name");
        this.extension = el.getAttribute("extension");
        this.simpleSequence = Boolean.parseBoolean(el.getAttribute("simpleSequence"));
        this.binary = Boolean.parseBoolean(el.getAttribute("binary"));
        xslt = XmlUtils.newDocument(true);
        xslt.setDocumentURI(documentURI);
        Node stylesheet = el.getElementsByTagName("xsl:stylesheet").item(0);
        xslt.appendChild(xslt.importNode(stylesheet, true));
    }

    @Override
    public boolean considersRepetitions() {
        return this.simpleSequence;
    }

    @Override
    public String[][] getFileExtensions() {
        return new String[][] { new String[] { name + " files (*." + extension + ")", extension } };
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getPreferredFileExtension() {
        return extension;
    }

    @Override
    public DocumentFragment getDocumentation() {
        return documentation;
    }

   @Override
    protected void possiblyMakeExecutable(File file) {
        if (executable)
            file.setExecutable(true, false);
    }

    //  FIXME
    @Override
    public void export(Command command, String source, String title, int repeatCount, File exportFile, String charsetName) throws IOException, TransformerException {
        Document document = command.toDocument(title, true, true, true, true);
        export(document, exportFile.getCanonicalPath(), charsetName, repeatCount);
    }

    private void export(Document document, String fileName, String charsetName, Map<String, String> parameters) throws IOException, TransformerException {
        try (OutputStream out = IrCoreUtils.getPrintStream(fileName, charsetName)) {
            XmlUtils.printDOM(out, document, charsetName, xslt, parameters, binary);
        }
    }

    void export(Document document, String fileName, String charsetName, int noRepeats) throws IOException, TransformerException {
        Map<String, String> parameters = new HashMap<>(1);
        parameters.put("noRepeats", Integer.toString(noRepeats));
        export(document, fileName, charsetName, parameters);
    }
}
