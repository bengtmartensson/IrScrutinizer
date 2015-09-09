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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map.Entry;
import org.harctoolbox.IrpMaster.IrpMasterException;
import org.harctoolbox.IrpMaster.IrpUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * A CommandSet is a set of Command's with the same protocol, but different parameter values.
 */
public class CommandSet {

    private String notes;
    private String protocol;
    private final String name;
    private final HashMap<String, Long> parameters;
    private final HashMap<String, Command> commands;

    public HashMap<String, Command> getCommands() {
        return commands;
    }

    /**
     * Imports a CommandSet from an Element.
     *
     * @param element
     * @throws ParseException
     */
    CommandSet(Element element) throws ParseException {
        name = element.getAttribute("name");
        protocol = null;
        commands = new LinkedHashMap<>();
        parameters = new LinkedHashMap<>();
        NodeList nl = element.getElementsByTagName("notes");
        if (nl.getLength() > 0)
            notes = ((Element) nl.item(0)).getTextContent();
        nl = element.getElementsByTagName("parameters");
        if (nl.getLength() > 0) {
            Element el = (Element) nl.item(0);
            String newProtocol = el.getAttribute("protocol");
            if (!newProtocol.isEmpty())
                protocol = newProtocol;
            NodeList paramList = el.getElementsByTagName("parameter");
            for (int i = 0; i < paramList.getLength(); i++) {
                Element e = (Element) paramList.item(i);
                try {
                    parameters.put(e.getAttribute("name"), Long.parseLong(e.getAttribute("value")));
                } catch (NumberFormatException ex) {
                    throw new ParseException("NumberFormatException " + ex.getMessage(), (int) IrpUtils.invalid);
                }
            }
        }

        nl = element.getElementsByTagName("command");
        for (int i = 0; i < nl.getLength(); i++) {
            Command irCommand;
            try {
                irCommand = new Command((Element) nl.item(i), protocol, parameters);
                commands.put(irCommand.getName(), irCommand);
            } catch (IrpMasterException ex) {
                // Ignore erroneous commands, continue parsing
                // TODO: invoke logger
            }

        }
    }

    /**
     * Constructs a CommandSet from its argument.
     *
     * @param name
     * @param notes
     * @param commands
     * @param protocol
     * @param parameters
     */
    CommandSet(String name, String notes, HashMap<String, Command> commands, String protocol, HashMap<String, Long>parameters) {
        this.name = name != null ? name : "commandSet";
        this.notes = notes;
        this.commands = commands;
        this.protocol = protocol;
        this.parameters = parameters;
    }

    /**
     * Exports the CommandSet to a Document.
     *
     * @param doc
     * @param fatRaw
     * @param generateRaw
     * @param generateCcf
     * @param generateParameters
     * @return newly constructed element, belonging to the doc Document.
     */
    public Element xmlExport(Document doc, boolean fatRaw,
            boolean generateRaw, boolean generateCcf, boolean generateParameters) {
        Element element = doc.createElement("commandSet");
        element.setAttribute("name", name);
        if (notes != null) {
            Element notesEl = doc.createElement("notes");
            notesEl.setTextContent(notes);
            element.appendChild(notesEl);
        }
        if (parameters != null && generateParameters) {
            Element parametersEl = doc.createElement("parameters");
            parametersEl.setAttribute("protocol", protocol);
            element.appendChild(parametersEl);
            for (Entry<String, Long> parameter : parameters.entrySet()) {
                Element parameterEl = doc.createElement("parameter");
                parameterEl.setAttribute("name", parameter.getKey());
                parameterEl.setAttribute("value", parameter.getValue().toString());
                parametersEl.appendChild(parameterEl);
            }
        }
        if (commands != null) {
            for (Command command : commands.values()) {
                try {
                    element.appendChild(command.xmlExport(doc, null, fatRaw,
                            generateRaw, generateCcf, generateParameters));
                } catch (IrpMasterException ex) {
                    element.appendChild(doc.createComment("Could not export command: " + command.toString()));
                    // TODO: invoke logger
                }
            }
        }
        return element;
    }

    /**
     * Applies the format argument to all Command's in the CommandSet.
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
}
