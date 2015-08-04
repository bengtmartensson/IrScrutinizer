/*
Copyright (C) 2013, 2015 Bengt Martensson.

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
import java.io.IOException;
import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import org.harctoolbox.IrpMaster.DecodeIR;
import org.harctoolbox.IrpMaster.IrSignal;
import org.harctoolbox.IrpMaster.IrpMasterException;
import org.harctoolbox.IrpMaster.Version;
import org.harctoolbox.IrpMaster.XmlUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * This class models a collection of Remotes, indexed by their names.
 */
public class RemoteSet implements Serializable {

    /**
     * String of the form major.<!-- -->minor identifying the protocol version
     * (not to be confused with the version of an implementation).
     */
    public final static String girrVersion = "1.0";

    private static final long serialVersionUID = 1L;
    private static String dateFormatString = "yyyy-MM-dd_HH:mm:ss";

    /**
     * @param aDateFormatString the dateFormatString to set, default "yyyy-MM-dd_HH:mm:ss";
     */
    public static void setDateFormatString(String aDateFormatString) {
        dateFormatString = aDateFormatString;
    }

    private String creatingUser;
    private String source;
    private String creationDate;
    private String tool;
    private String toolVersion;
    private String tool2;
    private String tool2Version;
    private String notes;
    private HashMap<String, Remote> remotes;

    /**
     * This constructor is used to import an XML document.
     * @param doc W3C Document
     * @throws ParseException
     */
    public RemoteSet(Document doc) throws ParseException {
        remotes = new LinkedHashMap<>();

        Element root = doc.getDocumentElement();
        NodeList nl = root.getElementsByTagName("adminData");
        if (nl.getLength() > 0) {
            Element adminData = (Element) nl.item(0);
            NodeList nodeList = adminData.getElementsByTagName("notes");
            if (nodeList.getLength() > 0)
                notes = ((Element) nodeList.item(0)).getTextContent();

            nodeList = adminData.getElementsByTagName("creationData");
            if (nodeList.getLength() > 0) {
                Element creationdata = (Element) nodeList.item(0);
                creatingUser = creationdata.getAttribute("creatingUser");
                source = creationdata.getAttribute("source");
                creationDate = creationdata.getAttribute("creationDate");
                tool = creationdata.getAttribute("tool");
                toolVersion = creationdata.getAttribute("toolVersion");
                tool2 = creationdata.getAttribute("tool2");
                tool2Version = creationdata.getAttribute("tool2Version");
            }
        }
        nl = root.getElementsByTagName("remote");
        for (int i = 0; i < nl.getLength(); i++) {
            Remote remote = new Remote((Element) nl.item(i));
            remotes.put(remote.getName(), remote);
        }
    }

    /* *
     * This constructor is used to import an XML document.
     * @param file
     * @throws SAXException
     * @throws IOException
     * @throws ParseException
     */
    //public RemoteSet(File file) throws SAXException, IOException, ParseException {
    //    this(XmlUtils.openXmlFile(file, (File) null, false, false));
    //}

    /**
     * This constructor sets up a RemoteSet from a given HashMap of Remotes, so that it can later be used through
     * the xmlExport or xmlExportDocument to generate an XML export.
     * @param creatingUser Comment field for the creating user, if wanted.
     * @param source Comment field describing the origin of the data; e.g. name of human author or creating program.
     * @param creationDate Date of creation, as text string.
     * @param tool Name of creating tool.
     * @param toolVersion Version of creating tool.
     * @param tool2 Name of secondary tppl, if applicable.
     * @param tool2Version Version of secondary tool.
     * @param notes Textual notes.
     * @param remotes HashMap of remotes.
     */
    public RemoteSet(String creatingUser,
            String source,
            String creationDate,
            String tool,
            String toolVersion,
            String tool2,
            String tool2Version,
            String notes,
            HashMap<String, Remote> remotes) {
        this.creatingUser = creatingUser;
        this.source = source;
        this.creationDate = creationDate != null ? creationDate : (new SimpleDateFormat(dateFormatString)).format(new Date()) ;
        this.tool = tool;
        this.toolVersion = toolVersion;
        this.tool2 = tool2;
        this.tool2Version = tool2Version;
        this.notes = notes;
        this.remotes = remotes;
    }

    /**
     * This constructor sets up a RemoteSet from one single Remote.
     *
     * @param creatingUser
     * @param source
     * @param creationDate
     * @param tool
     * @param toolVersion
     * @param tool2
     * @param tool2Version
     * @param notes
     * @param remote
     */
    public RemoteSet(String creatingUser,
            String source,
            String creationDate,
            String tool,
            String toolVersion,
            String tool2,
            String tool2Version,
            String notes,
            Remote remote) {
        this(creatingUser,
                source,
                creationDate,
                tool,
                toolVersion,
                tool2,
                tool2Version,
                notes,
                new HashMap<String, Remote>(1));
        remotes.put(remote.getName(), remote);
    }

    /**
     * Convenience version of the one-remote constructor.
     * @param creatingUser
     * @param source
     * @param remote
     */
    public RemoteSet(String creatingUser, String source, Remote remote) {
        this(creatingUser,
                source,
                null,
                Version.appName,
                Version.versionString,
                DecodeIR.appName,
                DecodeIR.getVersion(),
                null,
                remote);
    }

    /**
     * Convenience version of the many-remote constructor.
     * @param creatingUser
     * @param source
     * @param remotes
     */
    public RemoteSet(String creatingUser, String source, HashMap<String,Remote> remotes) {
        this(creatingUser,
                source,
                null,
                Version.appName,
                Version.versionString,
                DecodeIR.appName,
                DecodeIR.getVersion(),
                null,
                remotes);
    }

    /**
     * This constructor creates a RemoteSet from a single IrSignal.
     *
     * @param source
     * @param creatingUser
     * @param irSignal
     * @param name
     * @param comment
     * @param deviceName
     */
    public RemoteSet(String source, String creatingUser, IrSignal irSignal, String name,
            String comment, String deviceName) {
        this(creatingUser, source, new Remote(irSignal, name, comment, deviceName));
    }

    /**
     * Generates an W3C Element from a RemoteList.
     * @param doc
     * @param title
     * @param fatRaw
     * @param createSchemaLocation
     * @param generateRaw
     * @param generateCcf
     * @param generateParameters
     * @return Element describing the RemoteSet
     */
    public Element xmlExport(Document doc, String title, boolean fatRaw, boolean createSchemaLocation,
            boolean generateRaw, boolean generateCcf, boolean generateParameters) {
        Element element = doc.createElement("remotes");
        element.setAttribute("girrVersion", girrVersion);
        if (createSchemaLocation) {
            element.setAttribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
            element.setAttribute("xsi:noNamespaceSchemaLocation", "girr.xsd");
        }
        if (title != null)
            element.setAttribute("title", title);

        Element adminDataEl = doc.createElement("adminData");
        element.appendChild(adminDataEl);
        Element creationEl = doc.createElement("creationData");
        adminDataEl.appendChild(creationEl);
        if (creatingUser != null)
            creationEl.setAttribute("creatingUser", creatingUser);
        if (source != null)
            creationEl.setAttribute("source", source);
        if (creationDate != null)
            creationEl.setAttribute("creationDate", creationDate);
        if (tool != null)
            creationEl.setAttribute("tool", tool);
        if (toolVersion != null)
            creationEl.setAttribute("toolVersion", toolVersion);
        if (tool2 != null)
            creationEl.setAttribute("tool2", tool2);
        if (tool2Version != null)
            creationEl.setAttribute("tool2Version", tool2Version);
        if (notes != null) {
            Element notesEl = doc.createElement("notes");
            notesEl.setTextContent(notes);
            adminDataEl.appendChild(notesEl);
        }

        for (Remote remote : remotes.values()) {
            element.appendChild(remote.xmlExport(doc, fatRaw, generateRaw, generateCcf, generateParameters));
        }
        return element;
    }

    /**
     * Generates an XML Document from a RemoteSet.
     * @param title Textual title of document.
     * @param stylesheetType Type of stylesheet, normally "css" or "xsl".
     * @param fatRaw For the raw form, generate elements for each flash and gap, otherwise a long PCDATA text string of durations will be generated.
     * @param stylesheetUrl URL of stylesheet to be linked in a processing instruction.
     * @param createSchemaLocation if schema location attributes (for validation) should be included.
     * @param generateRaw If true, the raw form will be generated.
     * @param generateCcf If true, the CCF ("Pronto hex") form will be generated.
     * @param generateParameters If true, the protocol/parameter description will be generated.
     * @return XmlExporter
     */
    public Document xmlExportDocument(String title, String stylesheetType, String stylesheetUrl,
            boolean fatRaw, boolean createSchemaLocation,
            boolean generateRaw, boolean generateCcf, boolean generateParameters) {
        Element root = xmlExport(XmlExporter.newDocument(), title, fatRaw, createSchemaLocation,
            generateRaw, generateCcf, generateParameters);
        return XmlExporter.createDocument(root, stylesheetType, stylesheetUrl, createSchemaLocation);
    }

    /**
     * Applies the format argument to all Command's in the CommandSet.
     * @param format
     * @param repeatCount
     * @throws IrpMasterException
     */
    public void addFormat(Command.CommandTextFormat format, int repeatCount) throws IrpMasterException {
        for (Remote remote : remotes.values())
            remote.addFormat(format, repeatCount);
    }

    /**
     * Generates a list of the commands in all contained remotes.
     * It may contain non-unique names.
     * @return ArrayList of the commands.
     */
    public ArrayList<Command> getAllCommands() {
        ArrayList<Command> allCommands = new ArrayList<>();
        for (Remote remote : remotes.values())
            allCommands.addAll(remote.getCommands().values());
        return allCommands;
    }

    /**
     * @return the creatingUser
     */
    public String getCreatingUser() {
        return creatingUser;
    }

    /**
     * @return the source
     */
    public String getSource() {
        return source;
    }

    /**
     * @return the creationDate
     */
    public String getCreationDate() {
        return creationDate;
    }

    /**
     * @return the tool
     */
    public String getTool() {
        return tool;
    }

    /**
     * @return the toolVersion
     */
    public String getToolVersion() {
        return toolVersion;
    }

    /**
     * @return the tool2
     */
    public String getTool2() {
        return tool2;
    }

    /**
     * @return the tool2Version
     */
    public String getTool2Version() {
        return tool2Version;
    }

    /**
     * @return the notes
     */
    public String getNotes() {
        return notes;
    }

    /**
     * @return Collection of the contained remotes.
     */
    public Collection<Remote> getRemotes() {
        return remotes.values();
    }

    /**
     * Returns a particular remote.
     * @param name
     * @return Remote with the corresponding name, or null if not found.
     */
    public Remote getRemote(String name) {
        return remotes.get(name);
    }

    /**
     * For testing only, not deployment.
     * @param args
     */
    public static void main(String[] args) {
        try {
            Document doc = XmlUtils.openXmlFile(new File(args[0]), new File(args[1]), true, false);
            RemoteSet remoteList = new RemoteSet(doc);

            Document newdoc = remoteList.xmlExportDocument("This is a silly title",
                    "xsl", "simplehtml.xsl", true, false, true, true, true);
            XmlExporter exporter = new XmlExporter(newdoc);
            exporter.printDOM(new File("junk.xml"));
        } catch (IOException | ParseException | SAXException ex) {
            System.err.println(ex.getMessage());
        }
    }
}
