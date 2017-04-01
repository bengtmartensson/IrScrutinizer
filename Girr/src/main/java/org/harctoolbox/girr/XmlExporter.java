/*
Copyright (C) 2013 Bengt Martensson.

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

package org.harctoolbox.girr;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.harctoolbox.IrpMaster.XmlUtils;
import org.w3c.dom.Comment;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.ProcessingInstruction;

/**
 * Utility class for XML export. Usage in other contexts not recommended.
 */
public final class XmlExporter {

    /**
     * Name space for the XML Schemas
     */
    static final String w3cSchemaNamespace = "http://www.w3.org/2001/XMLSchema-instance";

    /**
     * Name space for XLST (1.0)
     */
    static final String xsltNamespace = "http://www.w3.org/1999/XSL/Transform";

    /**
     * Namespace URI
     */
    public static final String girrNamespace = "http://www.harctoolbox.org/Girr";

    /**
     * Homepage URL.
     */
    public static final String girrHomePage = "http://www.harctoolbox.org/Girr.html";

    /**
     * URL for schema file supporting name spaces.
     */
    public static final String girrSchemaLocationURL = "http://www.harctoolbox.org/schemas/girr_ns.xsd";

    /**
     * URL for schema file, namespace-less version.
     */
    public static final String girrNoNamespaceSchemaLocationURL = "http://www.harctoolbox.org/schemas/girr.xsd";

    //public static final boolean useNamespaces = true;

    private static boolean debug = false;

    /**
     * Comment string pointing to Girr docu.
     */
    private static final String girrComment = "This file is in the Girr (General IR Remote) format, see http://www.harctoolbox.org/Girr.html";

    private static final String defaultCharsetName = "UTF-8";

    public static void setDebug(boolean dbg) {
        debug = dbg;
    }

    public static Document createDocument(Element root, String stylesheetType, String stylesheetUrl, boolean createSchemaLocation) {
        Document document = root.getOwnerDocument();

        if (stylesheetType != null && stylesheetUrl != null && ! stylesheetUrl.isEmpty()) {
            ProcessingInstruction pi = document.createProcessingInstruction("xml-stylesheet",
                    "type=\"text/" + stylesheetType + "\" href=\"" + stylesheetUrl + "\"");
            document.appendChild(pi);
        }

        // At least in some Java versions (https://bugs.openjdk.java.net/browse/JDK-7150637)
        // there is no line feed before and after the comment.
        // This is technically correct, but looks awful to the human reader.
        // AFAIK, there is no clean way to fix this.
        // Possibly works with some Java versions?
        Comment comment = document.createComment(girrComment);
        document.appendChild(comment);
        document.appendChild(root);
        root.setAttribute("girrVersion", RemoteSet.girrVersion);
        if (createSchemaLocation) {
            root.setAttribute("xmlns:xsi", XmlExporter.w3cSchemaNamespace);
            root.setAttribute("xmlns", girrNamespace);
            root.setAttribute("xsi:schemaLocation", girrNamespace + " " + girrSchemaLocationURL);
        }
        return document;
    }

    public static Document newDocument() {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setValidating(false);
        factory.setNamespaceAware(true);
        Document doc = null;
        try {
            DocumentBuilder builder = factory.newDocumentBuilder();
            doc = builder.newDocument();
        } catch (ParserConfigurationException e) {
        }
        return doc;
    }

    private final Document document;

    /**
     *
     * @param doc
     */
    public XmlExporter(Document doc) {
        this.document = doc;
    }
    public XmlExporter(Element root, String stylesheetType, String stylesheetUrl, boolean createSchemaLocation) {
        this(createDocument(root, stylesheetType, stylesheetUrl, createSchemaLocation));
    }

    public void printDOM(OutputStream ostr, Document stylesheet, Map<String, String>parameters,
            boolean binary, String charsetName) throws IOException, TransformerException {
        if (debug) {
            XmlUtils.printDOM(new File("girr.girr"), this.document);
            XmlUtils.printDOM(new File("stylesheet.xsl"), stylesheet);
        }
        try {
            TransformerFactory factory = TransformerFactory.newInstance();
            Transformer tr;
            if (stylesheet == null) {
                tr = factory.newTransformer();

                tr.setOutputProperty(OutputKeys.METHOD, "xml");
                tr.setOutputProperty(OutputKeys.ENCODING, charsetName);

            } else {
                if (parameters != null)
                    parameters.entrySet().stream().map((kvp) -> {
                        Element e = stylesheet.createElementNS(xsltNamespace, "xsl:param");
                        e.setAttribute("name", kvp.getKey());
                        e.setAttribute("select", kvp.getValue());
                    return e;
                }).forEachOrdered((e) -> {
                    stylesheet.getDocumentElement().insertBefore(e, stylesheet.getDocumentElement().getFirstChild());
                });
                NodeList nodeList = stylesheet.getDocumentElement().getElementsByTagNameNS(xsltNamespace, "output");
                if (nodeList.getLength() > 0) {
                    Element e = (Element) nodeList.item(0);
                    e.setAttribute("encoding", charsetName);
                }
                if (debug)
                    XmlUtils.printDOM(new File("stylesheet-params.xsl"), stylesheet);
                tr = factory.newTransformer(new DOMSource(stylesheet));
            }
            tr.setOutputProperty(OutputKeys.INDENT, "yes");
            tr.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
            if (binary) {
                DOMResult domResult = new DOMResult();
                tr.transform(new DOMSource(document), domResult);
                Document newDoc = (Document) domResult.getNode();
                if (debug)
                    XmlUtils.printDOM(new File("girr-binary.xml"), newDoc);
                NodeList byteElements = newDoc.getDocumentElement().getElementsByTagName("byte");
                for (int i = 0; i < byteElements.getLength(); i++) {
                    int val = Integer.parseInt(byteElements.item(i).getTextContent());
                    ostr.write(val);
                }
            } else
                tr.transform(new DOMSource(document), new StreamResult(ostr));
        } finally {
            if (parameters != null && stylesheet != null) {
                NodeList nl = stylesheet.getDocumentElement().getChildNodes();
                // Must remove children in backward order not to invalidate nl, #139.
                for (int i = nl.getLength() - 1; i >= 0; i--) {
                    Node n = nl.item(i);
                    if (n.getNodeType() != Node.ELEMENT_NODE)
                        continue;
                    Element e = (Element) n;
                    if (e.getLocalName().equals("param") && parameters.containsKey(e.getAttribute("name")))
                        stylesheet.getDocumentElement().removeChild(n);
                }
            }
        }
    }

    public void printDOM(OutputStream ostr, String charsetName) throws IOException, TransformerException {
        printDOM(ostr, null, null, false, charsetName);
    }

    public void printDOM(File file, String charsetName) throws IOException, TransformerException  {
        if (file == null)
            printDOM(System.out, charsetName);
        else {
            try (FileOutputStream stream = new FileOutputStream(file)) {
                printDOM(stream, charsetName);
            }
        }
    }

    public void printDOM(File file) throws FileNotFoundException, IOException, TransformerException {
        printDOM(file, defaultCharsetName);
    }
}
