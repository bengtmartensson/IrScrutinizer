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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.harctoolbox.IrpMaster.IrpMasterException;
import org.harctoolbox.IrpMaster.XmlUtils;
import org.harctoolbox.girr.RemoteSet;
import org.harctoolbox.girr.XmlExporter;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 *
 */
public class DynamicRemoteSetExportFormat extends RemoteSetExporter implements IRemoteSetExporter {

    private final String formatName;
    private final String extension;
    private final boolean simpleSequence;
    private final boolean binary;
    private final Document xslt;

    public Document getXslt() { return xslt; }

    private DynamicRemoteSetExportFormat(Element el) {
        super();
        this.formatName = el.getAttribute("name");
        this.extension = el.getAttribute("extension");
        this.simpleSequence = Boolean.parseBoolean(el.getAttribute("simpleSequence"));
        this.binary = Boolean.parseBoolean(el.getAttribute("binary"));

        xslt = XmlUtils.newDocument();
        Node stylesheet = el.getElementsByTagName("xsl:stylesheet").item(0);
        xslt.appendChild(xslt.importNode(stylesheet, true));
    }

    public static HashMap<String, IExporterFactory> parseExportFormats(File file) throws ParserConfigurationException, SAXException, IOException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setValidating(false);
        factory.setNamespaceAware(true);
        factory.setXIncludeAware(true);
        Document doc = null;
        DocumentBuilder builder = factory.newDocumentBuilder();
        doc = builder.parse(file);

        HashMap<String, IExporterFactory> result = new HashMap<>();
        NodeList nl = doc.getElementsByTagName("exportformat");
        for (int i = 0; i < nl.getLength(); i++) {
            final Element el = (Element) nl.item(i);
            final ICommandExporter ef = (el.getAttribute("multiSignal").equals("true"))
                    ? new DynamicRemoteSetExportFormat(el)
                    : new DynamicCommandExportFormat(el);

            result.put(ef.getFormatName(), new IExporterFactory() {

                @Override
                public ICommandExporter newExporter() {
                    return ef;
                }
            });
        }
        return result;
    }

    @Override
    public boolean considersRepetitions() {
        return this.simpleSequence;
    }

    @Override
    public String[][] getFileExtensions() {
        return new String[][] { new String[] { formatName + " files (*." + extension + ")", extension } };
    }

    @Override
    public String getFormatName() {
        return formatName;
    }

    @Override
    public String getPreferredFileExtension() {
        return extension;
    }

    @Override
    public void export(RemoteSet remoteSet, String title, int count, File saveFile, String charsetName) throws FileNotFoundException, IOException, IrpMasterException {
        Document document = remoteSet.xmlExportDocument(title,
                null,
                null,
                true, //fatRaw,
                false, // createSchemaLocation,
                true, //generateRaw,
                true, //generateCcf,
                true //generateParameters)
        );
        export(document, saveFile, charsetName);
    }

    public void export(Document document, File saveFile, String charsetName) throws FileNotFoundException, IOException, IrpMasterException {
        XmlExporter xmlExporter = new XmlExporter(document);
        try (OutputStream out = new FileOutputStream(saveFile)) {
            HashMap<String, String> parameters = new HashMap<>(1);
            xmlExporter.printDOM(out, xslt, parameters, binary, charsetName);
        }
    }

    public static void main(String[] args) {
        if (args.length != 3) {
            System.err.println("Usage:\n\tDynamicRemoteSetExportFormat exportformats.xml formatname girrfile\n");
            System.exit(1);
        }
        File configFile = new File(args[0]);
        String formatName = args[1];
        File girrFile = new File(args[2]);

        try {
            HashMap<String, IExporterFactory> exportFormats = parseExportFormats(configFile);
            //Schema schema = (SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI)).newSchema(new URL("http://www.harctoolbox.org/schemas/exportformats.xsd"));
            if (!exportFormats.containsKey(formatName)) {
                System.err.println("No such export format ``" + formatName + "''");
                System.exit(2);
            }
            IExporterFactory format = exportFormats.get(formatName);

            DynamicRemoteSetExportFormat exporter = (DynamicRemoteSetExportFormat) format.newExporter();
            File outFile = new File(args[2].replaceAll("\\.girr$", "." + exporter.getPreferredFileExtension()));
            Document doc = XmlUtils.openXmlFile(girrFile);
            exporter.export(doc, outFile, "ISO-8859-1");
            System.err.println("Created " + outFile);
        } catch (ParserConfigurationException | SAXException | IOException | IrpMasterException ex) {
            System.err.println(ex + ": " + ex.getMessage());
        }
    }
}
