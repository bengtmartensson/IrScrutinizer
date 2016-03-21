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
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.harctoolbox.IrpMaster.IrpMasterException;
import org.harctoolbox.IrpMaster.IrpUtils;
import org.harctoolbox.IrpMaster.XmlUtils;
import org.harctoolbox.girr.RemoteSet;
import org.harctoolbox.girr.XmlExporter;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 *
 */
public class DynamicRemoteSetExportFormat extends RemoteSetExporter implements IRemoteSetExporter {

    private final Document xslt;

    public Document getXslt() {
        return xslt;
    }

    static String[][] mkExtensions(String formatName, String extension) {
        return new String[][] { new String[] { formatName + " files (*." + extension + ")", extension } };
    }

    private static String parseDocumentation(Element el) {
        NodeList nl = el.getElementsByTagName("documentation");
        return nl.getLength() > 0 ? ((Element)nl.item(0)).getTextContent() : null;
    }

    private DynamicRemoteSetExportFormat(String nm, String ext, String documentation, URL url, List<Option> options,
            boolean simpleSequence, boolean binary, Document xslt) {
        super(nm, mkExtensions(nm, ext), ext, documentation, url, options, simpleSequence, binary);
        //if (!options.isEmpty() || !documentation.isEmpty() || url != null)
        //    panel = new JPanel();
        this.xslt = xslt;
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
            String nm = el.getAttribute("name");
            String ext = el.getAttribute("extension");
            String documentation = parseDocumentation(el);
            URL url = IrpUtils.newURL(el.getAttribute("url"));
            List<Option> opts = Option.parseOptions(el);
            boolean seq = Boolean.parseBoolean(el.getAttribute("simpleSequence"));
            boolean bin = Boolean.parseBoolean(el.getAttribute("binary"));
            NodeList nodeList = el.getElementsByTagName("xsl:stylesheet");
            Document xslt;
            if (nodeList.getLength() > 0) {
                xslt = XmlUtils.newDocument();
                xslt.appendChild(xslt.importNode(nodeList.item(0), true));
            } else
                xslt = null;

            final ICommandExporter ef = (el.getAttribute("multiSignal").equals("true"))
                    ? new DynamicRemoteSetExportFormat(nm, ext, documentation, url, opts, seq, bin, xslt)
                    : new DynamicCommandExportFormat(nm, ext, documentation, url, opts, seq, bin, xslt);

            result.put(ef.getName(), new IExporterFactory() {

                @Override
                public ICommandExporter newExporter() {
                    return ef;
                }

                //@Override
                //public JPanel getPanel() {
                //    return ef.getPanel();
                //}

                @Override
                public String getName() {
                    return ef.getName();
                }
            });
        }
        return result;
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
            xmlExporter.printDOM(out, xslt, parameters, isBinary(), charsetName);
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
