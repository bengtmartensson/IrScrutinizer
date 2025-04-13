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
import java.net.URL;
import java.text.ParseException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.XMLConstants;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import org.harctoolbox.girr.Command;
import org.harctoolbox.girr.CommandSet;
import org.harctoolbox.girr.GirrException;
import org.harctoolbox.girr.Remote;
import org.harctoolbox.girr.RemoteSet;
import org.harctoolbox.girr.XmlStatic;
import org.harctoolbox.ircore.InvalidArgumentException;
import org.harctoolbox.ircore.IrCoreUtils;
import org.harctoolbox.irp.IrpDatabase;
import org.harctoolbox.irscrutinizer.GuiMain;
import org.harctoolbox.xml.XmlUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 * This class imports Girr files. Only Girr files having remotes as root element are presently supported.
 *
 */
public class GirrImporter extends RemoteSetImporter implements IReaderImporter {
    public static final String homeUrl = "https://www.harctoolbox.org/girr";
    private static final Logger logger = Logger.getLogger(GirrImporter.class.getName());

    private transient Schema schema;
    private URL url;
    private boolean validate;
    private final GuiMain guiMain;

    public GirrImporter(boolean validate, URL url, GuiMain guiMain) {
        super();
        schema = null;
        this.url = url;
        this.validate = validate;
        this.guiMain = guiMain;
    }

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


    private void loadSchema() throws SAXException {
        if (validate && schema == null && url != null)
            schema = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI).newSchema(url);
    }

    private void load(Document doc, String origin) throws ParseException, GirrException {
        prepareLoad(origin);
        remoteSet = null;
        loadIncremental(doc, origin);
        accumulateProtocols(origin);
        setupCommands();
    }

    private void loadIncremental(Document doc, String origin) throws ParseException, GirrException {
        Element root = doc.getDocumentElement();
        RemoteSet rs;
        Remote remote;
        CommandSet commandSet;
        Command command;
        switch (root.getLocalName()) {
            case XmlStatic.REMOTES_ELEMENT_NAME:
                rs = new RemoteSet(root, origin);
                break;
            case XmlStatic.REMOTE_ELEMENT_NAME:
                remote = new Remote(root, origin);
                rs = new RemoteSet(remote);
                break;
            case XmlStatic.COMMANDSET_ELEMENT_NAME:
                commandSet = new CommandSet(root);
                remote = new Remote(commandSet);
                remote.setName(origin);
                remote.setNotes("Imported from " + origin);
                rs = new RemoteSet(remote);
                break;
            case XmlStatic.COMMAND_ELEMENT_NAME:
                command = new Command(root);
                commandSet = new CommandSet(command);
                remote = new Remote(commandSet);
                remote.setName(origin);
                remote.setNotes("Imported from " + origin);
                rs = new RemoteSet(remote);
                break;
            default:
                throw new GirrException("Invalid root element \"" + root.getTagName() + "\" in " + origin);
        }

        if (remoteSet == null || remoteSet.isEmpty())
            remoteSet = rs;
        else
            remoteSet.append(rs);
    }

    /**
     *
     * @param inputStream
     * @param origin
     * @param charsetName ignored; taken from input file encoding.
     * @throws IOException
     * @throws ParseException
     */
    @Override
    public void load(InputStream inputStream, String origin, String charsetName /* ignored */) throws IOException, ParseException {
        try {
            loadSchema();
            Document thing = XmlUtils.openXmlStream(inputStream, validate ? schema : null, true, true);
            load(thing, origin);
        } catch (SAXParseException ex) {
            throw new ParseException(ex.getMessage(), ex.getLineNumber());
        } catch (GirrException | SAXException ex) {
            throw new IOException(ex.getMessage());
        }
    }

    public void load(InputStream inputStream, String origin) throws IOException, ParseException {
        load(inputStream, origin, null);
    }


    @Override
    public void load(Reader reader, String origin) throws IOException, ParseException {
        try {
            loadSchema();
            load(XmlUtils.openXmlReader(reader, validate ? schema : null, true, true), origin);
        } catch (SAXParseException ex) {
            throw new ParseException(ex.getMessage(), ex.getLineNumber());
        } catch (SAXException | GirrException ex) {
            throw new IOException(ex.getMessage());
        }
    }

    /**
     *
     * @param file
     * @param origin
     * @param charsetName ignored, instead taken from file encoding field.
     * @throws IOException
     */
    @Override
    public void load(File file, String origin, String charsetName /* ignored */) throws IOException {
        try {
            loadSchema();
        } catch (SAXException ex) {
            throw new IOException(ex.getMessage());
        }
        prepareLoad(origin);
        remoteSet = null;
        loadRecursive(file, origin);
        accumulateProtocols(origin);
        setupCommands();
    }

    private void loadRecursive(File fileOrDirectory, String origin) throws IOException {
        if (fileOrDirectory.isDirectory()) {
            File[] files = fileOrDirectory.listFiles();
            for (File file : files) {
                if (!ignored(file.getName()))
                    loadRecursive(file, file.getCanonicalPath());
            }
        } else {
            try {
                loadIncremental(XmlUtils.openXmlFile(fileOrDirectory, validate ? schema : null, true, true), origin);
            } catch (ParseException | SAXException | GirrException ex) {
                logger.log(Level.WARNING, "{0} in file {1}", new Object[]{ex.getMessage(), origin});
            }
        }
    }

    @Override
    public boolean canImportDirectories() {
        return true;
    }

    @Override
    public String[][] getFileExtensions() {
        return new String[][]{new String[]{"Girr files (*.girr)", "girr" }, new String[]{"XML files (*.xml)", "xml" }};
    }

    @Override
    public String getFormatName() {
        return "Girr";
    }

    private void accumulateProtocols(String origin) {
        IrpDatabase newProtocols = remoteSet.getIrpDatabase();
        guiMain.patchProtocols(newProtocols);
    }
}
