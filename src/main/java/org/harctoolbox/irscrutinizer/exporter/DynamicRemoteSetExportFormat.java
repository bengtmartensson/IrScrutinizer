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
import java.io.IOException;
import java.io.OutputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.validation.Schema;
import org.harctoolbox.girr.Command;
import org.harctoolbox.girr.RemoteSet;
import org.harctoolbox.guicomponents.GuiUtils;
import org.harctoolbox.ircore.IrCoreUtils;
import org.harctoolbox.xml.XmlUtils;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * This class is a RemoteSetExporter that dynamically takes its content from the contents of a configuration file,
 * or a directory of such.
 */
public class DynamicRemoteSetExportFormat extends RemoteSetExporter {

    public final static String EXPORTFORMAT_NAMESPACE = "http://www.harctoolbox.org/exportformats";

    static Map<String, IExporterFactory> parseExportFormats(GuiUtils guiUtils, File file) throws ParserConfigurationException, SAXException, IOException {
        return parseExportFormats(guiUtils, file, null);
    }

    static Map<String, IExporterFactory> parseExportFormats(GuiUtils guiUtils, File file, Schema schema) throws ParserConfigurationException, SAXException, IOException {
        if (! file.exists())
            throw new FileNotFoundException(file + " does not exist.");

        return file.isDirectory()
                ? parseExportFormatsDirectory(guiUtils, file, schema)
                : parseExportFormatsFile(guiUtils, file, schema);
    }

    @SuppressWarnings("UseOfSystemOutOrSystemErr")
    static Map<String, IExporterFactory> parseExportFormatsDirectory(GuiUtils guiUtils, File file, Schema schema) throws ParserConfigurationException, SAXException, IOException {
        Map<String, IExporterFactory> result = new HashMap<>(32);
        File[] files = file.listFiles((File dir, String name) -> name.endsWith(".xml"));
        for (File f : files) {
            try {
                Map<String, IExporterFactory> map = parseExportFormats(guiUtils, f, schema); // allow hierarchies
                map.entrySet().forEach((kvp) -> {
                    putWithCheck(guiUtils, result, kvp.getKey(), kvp.getValue());
                });
            } catch (ParserConfigurationException | SAXException | IOException ex) {
                String message = "Export formats file \"" + f.getPath() + "\" could not be read, ignoring it. " + ex.getLocalizedMessage();
                if (guiUtils != null)
                    guiUtils.warning(message);
                else
                    System.err.println(message);
            }
        }
        return result;
    }

    private static Map<String, IExporterFactory> parseExportFormatsFile(GuiUtils guiUtils, File file, Schema schema) throws ParserConfigurationException, SAXException, IOException {
        Document doc = XmlUtils.openXmlFile(file, schema, true, true);

        Map<String, IExporterFactory> result = new HashMap<>(32);
        NodeList nl = doc.getElementsByTagNameNS(EXPORTFORMAT_NAMESPACE, "exportformat");
        String documentURI = doc.getDocumentURI();
        for (int i = 0; i < nl.getLength(); i++) {
            final Element el = (Element) nl.item(i);
            final Exporter ef = (el.getAttribute("multiSignal").equals("true"))
                    ? new DynamicRemoteSetExportFormat(el, documentURI)
                    : new DynamicCommandExportFormat(el, documentURI);

            putWithCheck(guiUtils, result, ef.getName(), () -> ef);
        }
        return result;
    }

    static DocumentFragment extractDocumentation(Element el) {
        NodeList nodeList = el.getElementsByTagNameNS(EXPORTFORMAT_NAMESPACE, "documentation");
        DocumentFragment doc = nodeList.getLength() > 0 ? nodeListToDocumentFragment(nodeList, true) : null;
        return doc;
    }

    private static DocumentFragment nodeToDocumentFragment(Node node, boolean preserve) {
        return nodeListToDocumentFragment(node.getChildNodes(), preserve);
    }

    private static DocumentFragment nodeListToDocumentFragment(NodeList childNodes, boolean preserveSpace) {
        Document doc = XmlUtils.newDocument(true);
        DocumentFragment fragment = doc.createDocumentFragment();
        for (int i = 0; i < childNodes.getLength(); i++) {
            Node node = childNodes.item(i);
            if (preserveSpace)
                fragment.appendChild(doc.importNode(node, true));
            else {
                switch (node.getNodeType()) {
                    case Node.TEXT_NODE:
                        if (preserveSpace || !node.getTextContent().matches(IrCoreUtils.WHITESPACE))
                            fragment.appendChild(doc.createTextNode(node.getTextContent()));
                        break;
                    case Node.COMMENT_NODE:
                        break;
                    default:
                        Node importedNode = doc.importNode(node, false);
                        fragment.appendChild(importedNode);
                        importedNode.appendChild(doc.importNode(nodeToDocumentFragment(node, preserveSpace), true));
                        break;
                }
            }
        }
        return fragment;
    }

    private static void putWithCheck(GuiUtils guiUtils, Map<String, IExporterFactory> result, String formatName, IExporterFactory iExporterFactory) {
        if (result.containsKey(formatName))
            guiUtils.warning("Export format \"" + formatName + "\" present more than once; keeping the last.");
        result.put(formatName, iExporterFactory);
    }

    private final String formatName;
    private final String extension;
    private final boolean simpleSequence;
    private final boolean binary;
    private final boolean metadata;
    private final Document xslt;
    private final DocumentFragment documentation;
    private final boolean executable;

    private DynamicRemoteSetExportFormat(Element el, String documentURI) {
        super();
        this.executable = Boolean.parseBoolean(el.getAttribute("executable"));
        this.documentation = extractDocumentation(el);
        this.formatName = el.getAttribute("name");
        this.extension = el.getAttribute("extension");
        this.simpleSequence = Boolean.parseBoolean(el.getAttribute("simpleSequence"));
        this.binary = Boolean.parseBoolean(el.getAttribute("binary"));
        this.metadata = Boolean.parseBoolean(el.getAttribute("metadata"));

        xslt = XmlUtils.newDocument(true);
        xslt.setDocumentURI(documentURI);
        Node stylesheet = el.getElementsByTagNameNS("http://www.w3.org/1999/XSL/Transform", "stylesheet").item(0);
        xslt.appendChild(xslt.importNode(stylesheet, true));
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
    public String getName() {
        return formatName;
    }

    @Override
    public String getPreferredFileExtension() {
        return extension;
    }

    @Override
    public boolean supportsMetaData() {
        return metadata;
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

    @Override
    public void export(RemoteSet remoteSet, String title, File saveFile, String encoding) throws IOException, TransformerException {
        export(remoteSet, title, saveFile.getCanonicalPath(), encoding);
    }

    private void export(RemoteSet remoteSet, String title, String fileName, String encoding) throws IOException, TransformerException {
        boolean oldInheritStatus = Command.isUseInheritanceForXml();
        Command.setUseInheritanceForXml(false);
        Document document = remoteSet.toDocument(title,
                true, //fatRaw,
                true, //generateRaw,
                true, //generateCcf,
                true //generateParameters)
        );
        Command.setUseInheritanceForXml(oldInheritStatus);
        export(document, fileName, encoding);
    }

    void export(Document document, String fileName, String wantedEncoding) throws IOException, TransformerException {
        String encoding = (getEncoding() == null || getEncoding().isEmpty()) ? wantedEncoding : getEncoding();
        try (OutputStream out = IrCoreUtils.getPrintStream(fileName, encoding)) {
            XmlUtils.printDOM(out, document, encoding, xslt, standardParameter(encoding), binary);
        }
    }

    private Map<String, String> standardParameter(String encoding) {
        Map<String, String> parameters = new HashMap<>(8);
        parameters.put("encoding", "'" + encoding + "'");
        parameters.put("creatingUser", "'" + getCreatingUser() + "'");
        parameters.put("creatingTool", "'" + org.harctoolbox.irscrutinizer.Version.versionString + "'");
        parameters.put("creatingDate", "'" + (new Date()).toString() + "'");
        return parameters;
    }
}
