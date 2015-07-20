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

import java.io.Serializable;
import java.text.ParseException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
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
public class Remote implements Serializable {
    private static final long serialVersionUID = 1L;
    private String name;
    private String manufacturer;
    private String model;
    private String deviceClass;
    private String remoteName;
    private String comment;
    private String notes;
    private String protocol;
    private HashMap<String, Long>parameters;
    private HashMap<String, Command>commands;
    private HashMap<String, HashMap<String, String>> applicationParameters;

    /**
     * XML import function.
     *
     * @param element Element to read from.
     * @throws ParseException
     * @throws org.harctoolbox.IrpMaster.IrpMasterException
     */
    public Remote(Element element) throws ParseException, IrpMasterException {
        commands = new LinkedHashMap<String, Command>();
        applicationParameters = new LinkedHashMap<String, HashMap<String, String>>();

        name = element.getAttribute("name");
        manufacturer = element.getAttribute("manufacturer");
        model = element.getAttribute("model");
        deviceClass = element.getAttribute("deviceClass");
        remoteName = element.getAttribute("remoteName");
        comment = element.getAttribute("comment");
        NodeList nl = element.getElementsByTagName("applicationData");
        for (int i = 0; i < nl.getLength(); i++) {
            Element el = (Element) nl.item(i);
            NodeList nodeList = el.getElementsByTagName("appParameter");
            HashMap<String, String> map = new HashMap<String, String>();
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
     * @param name
     * @param manufacturer
     * @param model
     * @param deviceClass
     * @param remoteName
     * @param comment
     * @param notes
     * @param commands
     * @param applicationParameters
     * @param protocol
     * @param parameters
     */
    public Remote(String name, String manufacturer, String model, String deviceClass, String remoteName, String comment, String notes,
            HashMap<String, Command> commands, HashMap<String, HashMap<String, String>> applicationParameters,
            String protocol, HashMap<String,Long>parameters) {
        this.name = name;
        this.manufacturer = manufacturer;
        this.model = model;
        this.deviceClass = deviceClass;
        this.remoteName = remoteName;
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
     * @param name
     * @param manufacturer
     * @param model
     * @param deviceClass
     * @param remoteName
     * @param comment
     * @param notes
     * @param commands
     * @param applicationParameters
     */
    public Remote(String name, String manufacturer, String model, String deviceClass, String remoteName, String comment, String notes,
            HashMap<String, Command> commands, HashMap<String, HashMap<String, String>> applicationParameters) {
        this(name, manufacturer, model, deviceClass, remoteName, comment, notes,
            commands, applicationParameters, null, null);
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
        this(deviceName, //java.lang.String name,
                null, //java.lang.String manufacturer,
                null, //java.lang.String model,
                null, //java.lang.String deviceClass,
                null, //java.lang.String remoteName,
                null, //java.lang.String comment,
                null, //java.lang.String notes,
                Command.toHashMap(new Command(name, comment, irSignal)),
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
     * @throws IrpMasterException
     */
    public Element xmlExport(Document doc, boolean fatRaw,
            boolean generateRaw, boolean generateCcf, boolean generateParameters) throws IrpMasterException {
        Element element = doc.createElement("remote");
        element.setAttribute("name", name);
        if (manufacturer != null)
            element.setAttribute("manufacturer", manufacturer);
        if (model != null)
            element.setAttribute("model", model);
        if (deviceClass !=  null)
            element.setAttribute("deviceClass", deviceClass);
        if (remoteName != null)
            element.setAttribute("remoteName", remoteName);
        if (comment != null)
            element.setAttribute("comment", comment);
        if (notes != null) {
            Element notesEl = doc.createElement("notes");
            notesEl.setTextContent(notes);
            element.appendChild(notesEl);
        }
        if (applicationParameters != null) {
            for (Entry<String, HashMap<String, String>> kvp : applicationParameters.entrySet()) {
                Element appEl = doc.createElement("applicationData");
                appEl.setAttribute("application", kvp.getKey());
                element.appendChild(appEl);
                for (Entry<String, String>param : kvp.getValue().entrySet() ) {
                    Element paramEl = doc.createElement("appParameter");
                    paramEl.setAttribute("name", param.getKey());
                    paramEl.setAttribute("value", param.getValue());
                    appEl.appendChild(paramEl);
                }
            }
        }

        CommandSet commandSet = new CommandSet(null, null, commands, protocol, parameters);
        element.appendChild(commandSet.xmlExport(doc, fatRaw, generateRaw, generateCcf, generateParameters));

        //for (Command irCommand : commands.values()) {
        //    element.appendChild(irCommand.xmlExport(doc));
        //}
        return element;
    }

    /**
     * Applies the format argument to all Command's in the Remote.
     *
     * @param format
     * @param repeatCount
     * @throws IrpMasterException
     */
    public void addFormat(Command.CommandTextFormat format, int repeatCount) throws IrpMasterException {
        for (Command command : commands.values())
            command.addFormat(format, repeatCount);
    }

    /**
     * Returns true if and only if all contained commands has the protocol in the argument.
     * @param protocolName
     * @return
     */
    public boolean hasThisProtocol(String protocolName) {
        for (Command command : commands.values()) {
            if (!command.getProtocol().equalsIgnoreCase(protocolName))
                return false;
        }
        return true;
    }

    public static class compareNameCaseSensitive implements Comparator<Remote> {

        public int compare(Remote o1, Remote o2) {
            return o1.name.compareTo(o2.name);
        }
    }

    public static class compareNameCaseInsensitive implements Comparator<Remote> {

        public int compare(Remote o1, Remote o2) {
            return o1.name.compareToIgnoreCase(o2.name);
        }
    }

    public Command getCommand(String commandName) {
        return commands.get(commandName);
    }

    /**
     *
     * @return Name of the Remote.
     */
    public String getName() {
        return name;
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
    public HashMap<String, Command> getCommands() {
        return commands;
    }

    /**
     * @return the applicationParameters
     */
    public HashMap<String, HashMap<String, String>> getApplicationParameters() {
        return applicationParameters;
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

    /**
     * @return the notes
     */
    public String getNotes() {
        return notes;
    }
}
