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
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import org.harctoolbox.IrpMaster.IrpMasterException;
import org.harctoolbox.IrpMaster.IrpUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * A CommandSet is a set of Command's with the same protocol, but different parameter values.
 */
public class CommandSet {

    private String notes;
    private String protocol;
    private final String name;
    private final Map<String, Long> parameters;
    private final Map<String, Command> commands;

    /**
     * Imports a CommandSet from an Element.
     *
     * @param element
     * @throws ParseException
     */
    CommandSet(Element element) throws ParseException {
        name = element.getAttribute("name");
        protocol = null;
        commands = new LinkedHashMap<>(4);
        parameters = new LinkedHashMap<>(4);
        NodeList nl = element.getElementsByTagName("notes");
        if (nl.getLength() > 0)
            notes = nl.item(0).getTextContent();
        // Cannot use getElementsByTagName("parameters") because it will find
        // the parameters of the child commands, which is not what we want.
        nl = element.getChildNodes();
        for (int nodeNr = 0; nodeNr < nl.getLength(); nodeNr++) {
            if (nl.item(nodeNr).getNodeType() != Node.ELEMENT_NODE)
                continue;
            Element el = (Element) nl.item(nodeNr);
            if (!el.getTagName().equals("parameters"))
                continue;
            String newProtocol = el.getAttribute("protocol");
            if (!newProtocol.isEmpty())
                protocol = newProtocol;
            NodeList paramList = el.getElementsByTagName("parameter");
            for (int i = 0; i < paramList.getLength(); i++) {
                Element e = (Element) paramList.item(i);
                try {
                    parameters.put(e.getAttribute("name"), Command.parseParameter(e.getAttribute("value")));
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
    CommandSet(String name, String notes, Map<String, Command> commands, String protocol, Map<String, Long>parameters) {
        this.name = name != null ? name : "commandSet";
        this.notes = notes;
        this.commands = commands;
        this.protocol = protocol;
        this.parameters = parameters;
    }
    @SuppressWarnings("ReturnOfCollectionOrArrayField")
    public Map<String, Command> getCommands() {
        return commands;
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
        Element element = doc.createElementNS(XmlExporter.girrNamespace, "commandSet");
        element.setAttribute("name", name);
        if (notes != null) {
            Element notesEl = doc.createElementNS(XmlExporter.girrNamespace, "notes");
            notesEl.setTextContent(notes);
            element.appendChild(notesEl);
        }
        if (parameters != null && generateParameters) {
            Element parametersEl = doc.createElementNS(XmlExporter.girrNamespace, "parameters");
            parametersEl.setAttribute("protocol", protocol.toLowerCase(Locale.US));
            element.appendChild(parametersEl);
            parameters.entrySet().stream().map((parameter) -> {
                Element parameterEl = doc.createElementNS(XmlExporter.girrNamespace, "parameter");
                parameterEl.setAttribute("name", parameter.getKey());
                parameterEl.setAttribute("value", parameter.getValue().toString());
                return parameterEl;
            }).forEachOrdered((parameterEl) -> {
                parametersEl.appendChild(parameterEl);
            });
        }
        if (commands != null) {
            commands.values().forEach((command) -> {
                element.appendChild(command.xmlExport(doc, null, fatRaw,
                        generateRaw, generateCcf, generateParameters));
            });
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
        commands.values().forEach((command) -> {
            try {
                command.addFormat(format, repeatCount);
            } catch (IrpMasterException ex) {
                // TODO: invoke logger
            }
        });
    }
}
