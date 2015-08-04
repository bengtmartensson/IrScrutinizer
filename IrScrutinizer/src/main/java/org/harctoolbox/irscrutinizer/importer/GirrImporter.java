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

package org.harctoolbox.irscrutinizer.importer;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import javax.xml.XMLConstants;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import org.harctoolbox.IrpMaster.XmlUtils;
import org.harctoolbox.girr.RemoteSet;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 * This class imports Girr files. Only Girr files having remotes as root element are presently supported.
 *
 */
public class GirrImporter extends RemoteSetImporter implements IReaderImporter, Serializable {
    public static final String homeUrl = "http://www.harctoolbox.org/girr";
    private static final long serialVersionUID = 1L;

    private transient Schema schema;
    private transient URL url;
    private boolean validate;

    /**
     * @return the schema
     */
    public Schema getSchema() {
        return schema;
    }

    /**
     * @param url the url to set
     */

    public void setUrl(URL url) {
        this.url = url;
    }

    public void setValidate(boolean validate) {
        this.validate = validate;
    }

    public GirrImporter(boolean validate, URL url) {
        super();
        schema = null;
        this.url = url;
        this.validate = validate;
    }

    private void loadSchema() throws SAXException {
        if (validate && schema == null && url != null)
            schema = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI).newSchema(url);
    }

    private void load(Document doc, String origin) throws ParseException, MalformedURLException, SAXException {
        if (!doc.getDocumentElement().getTagName().equals("remotes")) {
            throw new UnsupportedOperationException("Import of Girr files with other root elements as remotes not yet implemented.");
        }
        //this.lircRemotes = lircRemotes;
        prepareLoad(origin);
        remoteSet = new RemoteSet(doc);
        setupCommands();
    }

    @Override
    public void load(InputStream inputStream, String origin) throws IOException, ParseException {
        try {
            loadSchema();
            load(XmlUtils.openXmlStream(inputStream, validate ? schema : null, false, false), origin);
        } catch (SAXParseException ex) {
            throw new ParseException(ex.getMessage(), ex.getLineNumber());
        } catch (SAXException ex) {
            throw new IOException(ex.getMessage());
        }
    }

    @Override
    public void load(Reader reader, String origin) throws IOException, ParseException {
        try {
            loadSchema();
            load(XmlUtils.openXmlReader(reader, validate ? schema : null, false, false), origin);
        } catch (SAXParseException ex) {
            throw new ParseException(ex.getMessage(), ex.getLineNumber());
        } catch (SAXException ex) {
            throw new IOException(ex.getMessage());
        }
    }

    @Override
    public void load(File file, String origin) throws IOException, ParseException {
        try {
            loadSchema();
            load(XmlUtils.openXmlFile(file, validate ? schema : null, false, false), origin);
        } catch (SAXParseException ex) {
            throw new ParseException(ex.getMessage(), ex.getLineNumber());
        } catch (SAXException ex) {
            throw new IOException(ex.getMessage());
        }
    }

    @Override
    public boolean canImportDirectories() {
        return false;
    }

    @Override
    public String[][] getFileExtensions() {
        return new String[][]{new String[]{"Girr files (*.girr)", "girr" }, new String[]{"XML files (*.xml)", "xml" }};
    }

    @Override
    public String getFormatName() {
        return "Girr";
    }
}
