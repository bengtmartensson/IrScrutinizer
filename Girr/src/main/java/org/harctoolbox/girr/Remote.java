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

import java.text.ParseException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import org.harctoolbox.IrpMaster.IrSignal;
import org.harctoolbox.IrpMaster.IrpMasterException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * This class describes a remote in Girr.
 * A Remote is essentially an abstraction of a hand-held "clicker" for controlling one device.
 * It has a name for identification, and a number of comment-like text fields. Most importantly,
 * it has a dictionary of Commands, indexed by their names.
 */
public class Remote {

    private static Map<String,Command> commandToMap(Command command) {
        Map<String,Command> result = new HashMap<>(1);
        result.put(command.getName(), command);
        return result;
    }

    private MetaData metaData;
    private String comment;
    private String notes;
    private String protocol;
    private Map<String, Long>parameters;
    private Map<String, Command>commands;
    private Map<String, Map<String, String>> applicationParameters;

    /**
     * XML import function.
     *
     * @param element Element to read from.
     * @throws ParseException
     */
    public Remote(Element element) throws ParseException {
        metaData = new MetaData(element.getAttribute("name"),
                element.getAttribute("displayName"),
                element.getAttribute("manufacturer"),
                element.getAttribute("model"),
                element.getAttribute("deviceClass"),
                element.getAttribute("remoteName"));
        commands = new LinkedHashMap<>(32);
        applicationParameters = new LinkedHashMap<>(4);
        comment = element.getAttribute("comment");
        NodeList nl = element.getElementsByTagName("applicationData");
        for (int i = 0; i < nl.getLength(); i++) {
            Element el = (Element) nl.item(i);
            NodeList nodeList = el.getElementsByTagName("appParameter");
            Map<String, String> map = new HashMap<>(32);
            for (int index = 0; index < nodeList.getLength(); index++) {
                Element par = (Element) nodeList.item(index);
                map.put(par.getAttribute("name"), par.getAttribute("value"));
            }
            applicationParameters.put(el.getAttribute("application"), map);
        }

        nl = element.getElementsByTagName("commandSet");
        for (int i = 0; i < nl.getLength(); i++) {
            CommandSet commandSet = new CommandSet((Element) nl.item(i));
            commands.putAll(commandSet.getCommands());
        }
    }

    /**
     * Construct a Remote from its arguments, general case.
     *
     * @param metaData
     * @param comment
     * @param notes
     * @param commands
     * @param applicationParameters
     * @param protocol
     * @param parameters
     */
    public Remote(MetaData metaData, String comment, String notes,
            Map<String, Command> commands, Map<String, Map<String, String>> applicationParameters,
            String protocol, Map<String,Long>parameters) {
        this.metaData = metaData;
        /*this.name = name;
        this.manufacturer = manufacturer;
        this.model = model;
        this.deviceClass = deviceClass;
        this.remoteName = remoteName;*/
        this.comment = comment;
        this.notes = notes;
        this.commands = commands;
        this.applicationParameters = applicationParameters;
        this.protocol = protocol;
        this.parameters = parameters;
    }

    /**
     * Convenience version of the general constructor, with default values.
     *
     * @param metaData
     * @param comment
     * @param notes
     * @param commands
     * @param applicationParameters
     */
    public Remote(MetaData metaData, String comment, String notes,
            Map<String, Command> commands, Map<String, Map<String, String>> applicationParameters) {
        this(metaData, comment, notes, commands, applicationParameters, null, null);
    }

    /**
     * This constructor constructs a Remote from one single IrSignal.
     *
     * @param irSignal
     * @param name Name of command to be constructed.
     * @param comment Comment for command to be constructed.
     * @param deviceName
     */
    public Remote(IrSignal irSignal, String name, String comment, String deviceName) {
        this(new MetaData(deviceName),
                null, // comment,
                null, // notes,
                commandToMap(new Command(name, comment, irSignal)),
                null);
    }


    /**
     * XML export function.
     *
     * @param doc
     * @param fatRaw
     * @param generateRaw
     * @param generateCcf
     * @param generateParameters
     * @return XML Element of gid "remote",
     */
    public Element xmlExport(Document doc, boolean fatRaw,
            boolean generateRaw, boolean generateCcf, boolean generateParameters) {
        Element element = doc.createElementNS(XmlExporter.girrNamespace, "remote");
        element.setAttribute("name", metaData.name);
        if (metaData.displayName != null)
            element.setAttribute("displayName", metaData.displayName);
        if (metaData.manufacturer != null)
            element.setAttribute("manufacturer", metaData.manufacturer);
        if (metaData.model != null)
            element.setAttribute("model", metaData.model);
        if (metaData.deviceClass !=  null)
            element.setAttribute("deviceClass", metaData.deviceClass);
        if (metaData.remoteName != null)
            element.setAttribute("remoteName", metaData.remoteName);
        if (comment != null)
            element.setAttribute("comment", comment);
        if (notes != null) {
            Element notesEl = doc.createElementNS(XmlExporter.girrNamespace, "notes");
            notesEl.setTextContent(notes);
            element.appendChild(notesEl);
        }
        if (applicationParameters != null) {
            for (Entry<String, Map<String, String>> kvp : applicationParameters.entrySet()) {
                Element appEl = doc.createElementNS(XmlExporter.girrNamespace, "applicationData");
                appEl.setAttribute("application", kvp.getKey());
                element.appendChild(appEl);
                for (Entry<String, String>param : kvp.getValue().entrySet() ) {
                    Element paramEl = doc.createElementNS(XmlExporter.girrNamespace, "appParameter");
                    paramEl.setAttribute("name", param.getKey());
                    paramEl.setAttribute("value", param.getValue());
                    appEl.appendChild(paramEl);
                }
            }
        }

        CommandSet commandSet = new CommandSet(null, null, commands, protocol, parameters);
        element.appendChild(commandSet.xmlExport(doc, fatRaw, generateRaw, generateCcf, generateParameters));

        return element;
    }

    /**
     * Applies the format argument to all Command's in the Remote.
     *
     * @param format
     * @param repeatCount
     */
    public void addFormat(Command.CommandTextFormat format, int repeatCount) {
        for (Command command : commands.values())
            try {
                command.addFormat(format, repeatCount);
            } catch (IrpMasterException ex) {
                // TODO: invoke logger
            }
    }

    /**
     * Returns true if and only if all contained commands has the protocol in the argument.
     * @param protocolName
     * @return
     * @throws org.harctoolbox.IrpMaster.IrpMasterException
     */
    public boolean hasThisProtocol(String protocolName) throws IrpMasterException {
        for (Command command : commands.values()) {
            String prtcl = command.getProtocolName();
            if (prtcl == null || !prtcl.equalsIgnoreCase(protocolName))
                return false;
        }
        return true;
    }


    public Command getCommand(String commandName) {
        return commands.get(commandName);
    }

    /**
     *
     * @return the metaData
     */
    public MetaData getMetaData() {
        return metaData;
    }

    /**
     *
     * @return Name of the Remote.
     */
    public String getName() {
        return metaData.name;
    }

    /**
     *
     * @return displayName of the Remote.
     */
    public String getDisplayName() {
        return metaData.displayName;
    }

    /**
     * @return the comment
     */
    public String getComment() {
        return comment;
    }

    /**
     * @return the commands
     */
    @SuppressWarnings("ReturnOfCollectionOrArrayField")
    public Map<String, Command> getCommands() {
        return commands;
    }

    /**
     * @return the applicationParameters
     */
    @SuppressWarnings("ReturnOfCollectionOrArrayField")
    public Map<String, Map<String, String>> getApplicationParameters() {
        return applicationParameters;
    }

    /**
     * @return the manufacturer
     */
    public String getManufacturer() {
        return metaData.manufacturer;
    }

    /**
     * @return the model
     */
    public String getModel() {
        return metaData.model;
    }

    /**
     * @return the deviceClass
     */
    public String getDeviceClass() {
        return metaData.deviceClass;
    }

    /**
     * @return the remoteName
     */
    public String getRemoteName() {
        return metaData.remoteName;
    }

    /**
     * @return the notes
     */
    public String getNotes() {
        return notes;
    }

    public static class MetaData {
        private String name;
        private String displayName;
        private String manufacturer;
        private String model;
        private String deviceClass;
        private String remoteName;

        public MetaData() {
            this.name = null;
            this.displayName = null;
            this.manufacturer = null;
            this.model = null;
            this.deviceClass = null;
            this.remoteName = null;
        }

        public MetaData(String name) {
            this();
            this.name = name;
        }

        public MetaData(String name, String displayName, String manufacturer, String model,
                String deviceClass, String remoteName) {
            this.name = name;
            this.displayName = displayName;
            this.manufacturer = manufacturer;
            this.model = model;
            this.deviceClass = deviceClass;
            this.remoteName = remoteName;
        }

        /**
         * @return the name
         */
        public String getName() {
            return name;
        }

        /**
         * @return the displayName
         */
        public String getDisplayName() {
            return displayName;
        }

        /**
         * @return the manufacturer
         */
        public String getManufacturer() {
            return manufacturer;
        }

        /**
         * @return the model
         */
        public String getModel() {
            return model;
        }

        /**
         * @return the deviceClass
         */
        public String getDeviceClass() {
            return deviceClass;
        }

        /**
         * @return the remoteName
         */
        public String getRemoteName() {
            return remoteName;
        }
    }
    public static class CompareNameCaseSensitive implements Comparator<Remote> {
        @Override
        public int compare(Remote o1, Remote o2) {
            return o1.metaData.name.compareTo(o2.metaData.name);
        }
    }
    public static class CompareNameCaseInsensitive implements Comparator<Remote> {
        @Override
        public int compare(Remote o1, Remote o2) {
            return o1.metaData.name.compareToIgnoreCase(o2.metaData.name);
        }
    }
}
