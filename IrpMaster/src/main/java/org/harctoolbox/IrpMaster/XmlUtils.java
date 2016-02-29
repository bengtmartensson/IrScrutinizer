/*
Copyright (C) 2009-2013 Bengt Martensson.

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

package org.harctoolbox.IrpMaster;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.util.HashMap;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 * This class consists of a collection of useful static constants and functions.
 */
public class XmlUtils {

    private XmlUtils() {}

    public static Document openXmlFile(File file, Schema schema, boolean isNamespaceAware, boolean isXIncludeAware) throws IOException, SAXParseException, SAXException {
        final String fname = file.getCanonicalPath();
        DocumentBuilder builder = newDocumentBuilder(schema, isNamespaceAware, isXIncludeAware);
        Document docu = null;

        builder.setErrorHandler(new org.xml.sax.ErrorHandler() {
            @Override
            public void error(SAXParseException exception) throws SAXParseException {
                //System.err.println("Parse Error in file " + fname + ", line " + exception.getLineNumber() + ": " + exception.getMessage());
                throw new SAXParseException("Parse Error in file " + fname + ", line " + exception.getLineNumber() + ": " + exception.getMessage(), "", fname, exception.getLineNumber(), 0);
            }

            @Override
            public void fatalError(SAXParseException exception) throws SAXParseException {
                //System.err.println("Parse Fatal Error: " + exception.getMessage() + exception.getLineNumber());
                throw new SAXParseException("Parse Error in file " + fname + ", line " + exception.getLineNumber() + ": " + exception.getMessage(), "", fname, exception.getLineNumber(), 0);
            }

            @Override
            public void warning(SAXParseException exception) {
                System.err.println("Parse Warning: " + exception.getMessage() + exception.getLineNumber());
            }
        });

        docu = builder.parse(file);
        return docu;
    }

    public static Document openXmlFile(File file, File schemaFile, boolean isNamespaceAware, boolean isXIncludeAware) throws IOException, SAXParseException, SAXException {
        Schema schema = readSchemaFromFile(schemaFile);
        return openXmlFile(file, schema, isNamespaceAware, isXIncludeAware);
    }

    // NOTE: By silly reader, makes null as InputStream, producing silly error messages.
    public static Document openXmlReader(Reader reader, Schema schema, boolean isNamespaceAware, boolean isXIncludeAware) throws IOException, SAXParseException, SAXException {
        return openXmlStream((new InputSource(reader)).getByteStream(), schema, isNamespaceAware, isXIncludeAware);
    }

    public static Document openXmlStream(InputStream stream, Schema schema, boolean isNamespaceAware, boolean isXIncludeAware) throws IOException, SAXParseException, SAXException {
        DocumentBuilder builder = newDocumentBuilder(schema, isNamespaceAware, isXIncludeAware);
        Document docu = null;

        builder.setErrorHandler(new org.xml.sax.ErrorHandler() {
            @Override
            public void error(SAXParseException exception) throws SAXParseException {
                throw new SAXParseException("Parse Error in instream, line " + exception.getLineNumber() + ": " + exception.getMessage(), "", "instream", exception.getLineNumber(), 0);
            }

            @Override
            public void fatalError(SAXParseException exception) throws SAXParseException {
                throw new SAXParseException("Parse Error in instream, line " + exception.getLineNumber() + ": " + exception.getMessage(), "", "instream", exception.getLineNumber(), 0);
            }

            @Override
            public void warning(SAXParseException exception) {
                System.err.println("Parse Warning: " + exception.getMessage() + exception.getLineNumber());
            }
        });
        docu = builder.parse(stream);

        return docu;
    }

    private static DocumentBuilder newDocumentBuilder(Schema schema, boolean isNamespaceAware, boolean isXIncludeAware) throws SAXException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

        factory.setNamespaceAware(isNamespaceAware);
        factory.setXIncludeAware(isXIncludeAware);
        if (schema != null) {
            factory.setSchema(schema);
            factory.setValidating(false);
        }
        DocumentBuilder builder = null;
        try {
            builder = factory.newDocumentBuilder();
        } catch (ParserConfigurationException ex) {
        }
        return builder;
    }

    public static Document newDocument(boolean isNamespaceAware) {
        try {
            DocumentBuilder builder = newDocumentBuilder(null, isNamespaceAware, false);
            return builder.newDocument();
        } catch (SAXException ex) {
            // should never happen
            return null;
        }
    }

    public static Document newDocument() {
        return newDocument(false);
    }

    public static HashMap<String, Element> createIndex(Element root, String tagName, String idName) {
        HashMap<String, Element> index = new HashMap<>();
        NodeList nodes = root.getElementsByTagName(tagName);
        for (int i = 0; i < nodes.getLength(); i++) {
            Element el = (Element) nodes.item(i);
            String key = el.getAttribute(idName);
            if (!key.isEmpty())
                index.put(key, el);
        }
        return index;
    }

    public static void printDOM(OutputStream ostr, Document doc, String encoding) {
        try {
            Transformer tr = TransformerFactory.newInstance().newTransformer();
            if (encoding != null)
                tr.setOutputProperty(OutputKeys.ENCODING, encoding);
            tr.setOutputProperty(OutputKeys.INDENT, "yes");
            tr.setOutputProperty(OutputKeys.METHOD, "xml");
            tr.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
            tr.transform(new DOMSource(doc), new StreamResult(ostr));
        } catch (TransformerConfigurationException ex) {
            System.err.println(ex.getMessage());
        } catch (TransformerException ex) {
            System.err.println(ex.getMessage());
        }
    }

    public static void printDOM(File file, Document doc, String encoding)
            throws FileNotFoundException {
        printDOM(file != null ? new FileOutputStream(file) : System.out,
                doc, encoding);
        System.err.println("File " + file + " written.");
    }

    // Do not define a function printDOM(File, Document, String),
    // since it would been too error prone.

    public static void printDOM(File file, Document doc) throws FileNotFoundException {
        printDOM(file, doc, null);
    }

    private static Schema readSchemaFromFile(File schemaFile) throws SAXException {
        return (SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI)).newSchema(schemaFile);
    }

    public static HashMap<String, Element> buildIndex(Element element, String tagName, String idName) {
        HashMap<String, Element> index = new HashMap<>();
        NodeList nl = element.getElementsByTagName(tagName);
        for (int i = 0; i < nl.getLength(); i++) {
            Element el = (Element) nl.item(i);
            String id = el.getAttribute(idName);
            if (!id.isEmpty())
                index.put(id, el);
        }
        return index;
    }

    public static void main(String[] args) {
        try {
            Schema schema = args.length > 1 ? readSchemaFromFile(new File(args[1])) : null;
            Document doc = openXmlFile(new File(args[0]), schema, true, true);
            System.out.println(doc);
        } catch (IOException ex) {
            System.err.println(ex.getMessage());
        } catch (SAXParseException ex) {
            System.err.println(ex.getMessage());
        } catch (SAXException ex) {
            System.err.println(ex.getMessage());
        }
    }
}
