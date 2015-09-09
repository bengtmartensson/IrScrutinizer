/*
 Copyright (C) 2013, 2014, 2015 Bengt Martensson.

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
import java.io.Serializable;
import java.text.ParseException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map.Entry;
import org.harctoolbox.IrpMaster.DecodeIR;
import org.harctoolbox.IrpMaster.IrSequence;
import org.harctoolbox.IrpMaster.IrSignal;
import org.harctoolbox.IrpMaster.IrpMaster;
import org.harctoolbox.IrpMaster.IrpMasterException;
import org.harctoolbox.IrpMaster.IrpUtils;
import org.harctoolbox.IrpMaster.ModulatedIrSequence;
import org.harctoolbox.IrpMaster.Pronto;
import org.harctoolbox.IrpMaster.XmlUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * This class models the command in Girr. A command is essentially an IR signal,
 * given either by protocol/parameters or timing data, and a name.
 *
 * The member functions of class may throw IrpMasterExceptions when they encounter
 * erroneous data. The other classes in the package may not; they should just
 * "ignore" individual unparseable commands.
 */
public class Command implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * An implementation of this interface describes a way to format an IrSignal to a text string.
     */
    public interface CommandTextFormat {
        /**
         *
         * @return Name of the format (not the signal).
         */
        public String getName();

        /**
         * Formats an IrSignal with repeatCount number of repetitions.
         * @param irSignal IrSignal to be formatted
         * @param repeatCount Number of repeat sequences to include.
         * @return string of formatted signal.
         */
        public String format(IrSignal irSignal, int repeatCount);
    }

    /**
     * This describes which representation of a Command constitutes the master.
     */
    public static enum MasterType {
        raw,
        ccf,
        parameters
    }
    private static IrpMaster irpMaster;
    private static final String linefeed = System.getProperty("line.separator", "\n");

    private MasterType masterType;
    private String notes;
    private String name;
    private String protocol;
    private HashMap<String, Long> parameters;
    private int frequency = (int) IrpUtils.invalid;
    private double dutyCycle = (double) IrpUtils.invalid;
    private String intro;
    private String repeat;
    private String ending;
    private String ccf;
    private String comment;
    private HashMap<String, String> otherFormats;

    /**
     * Sets an IrpMaster instance, which will be used in subsequent transformations from parameter format.
     * @param newIrpMaster
     */
    public static void setIrpMaster(IrpMaster newIrpMaster) {
        irpMaster = newIrpMaster;
    }

    /**
     * @return duty cycle, a number between 0 and 1.
     */
    public double getDutyCycle() {
        return dutyCycle;
    }

    /**
     * @return the comment
     */
    public String getComment() {
        return comment;
    }

    /**
     * @return the notes
     */
    public String getNotes() {
        return notes;
    }

    /**
     * @return the masterType
     */
    public MasterType getMasterType() {
        return masterType;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @return the protocol
     */
    public String getProtocol() {
        return protocol;
    }

    /**
     * @return the parameters
     */
    public HashMap<String, Long> getParameters() {
        return parameters;
    }

    /**
     * @return the frequency
     */
    public int getFrequency() {
        return frequency;
    }

    /**
     * @return the intro
     */
    public String getIntro() {
        return intro;
    }

    /**
     * @return the repeat
     */
    public String getRepeat() {
        return repeat;
    }

    /**
     * @return the ending
     */
    public String getEnding() {
        return ending;
    }

    /**
     * @return the ccf
     */
    public String getCcf() {
        return ccf;
    }

    /**
     * @return List of the otherFormats
     */
    public Collection<String> getOtherFormats() {
        return otherFormats.keySet();
    }

    /**
     * Returns an "other" format, identified by its name.
     * @param name
     * @return test string of the format.
     */
    public String getFormat(String name) {
        return otherFormats != null ? otherFormats.get(name) : null;
    }

    /**
     * Returns the IrSignal of the Command.
     * @return IrSignal corresponding to the Command.
     * @throws IrpMasterException
     */
    public IrSignal toIrSignal() throws IrpMasterException {
        return
                masterType == MasterType.parameters ? new IrSignal(irpMaster, protocol, parameters)
                : masterType == MasterType.raw ? new IrSignal(frequency, dutyCycle, intro, repeat, ending)
                : new IrSignal(ccf);
    }

    public ModulatedIrSequence toModulatedIrSequence(int noRepetitions) throws IrpMasterException {
        return toIrSignal().toModulatedIrSequence(true, noRepetitions, true);
    }

    public static ModulatedIrSequence appendAsSequence(Collection<Command>commands) throws IrpMasterException {
        double frequency = (double) IrpUtils.invalid;
        double dutyCycle = (double) IrpUtils.invalid;
        IrSequence seq = new IrSequence();
        for (Command c : commands) {
            if (frequency < 0) // take the first sensible frequency
                frequency = c.getFrequency();
            if (dutyCycle <= 0)
                dutyCycle = c.getDutyCycle();
            seq = seq.append(c.toModulatedIrSequence(1));
        }
        return new ModulatedIrSequence(seq, frequency, dutyCycle);
    }

    private static String toPrintString(HashMap<String,Long>map) {
        if (map == null || map.isEmpty())
            return "";
        StringBuilder str = new StringBuilder();
        for (Entry<String,Long>kvp : map.entrySet()) {
            str.append(kvp.getKey()).append("=").append(Long.toString(kvp.getValue())).append(" ");
        }
        return str.substring(0, str.length() - 1);
    }

    // This can be improved with a user supplied template...
    public String nameProtocolParameterString() {
        StringBuilder str = new StringBuilder(name != null ? name : "<no_name>");
        if (protocol == null) {
            str.append(": <no decode>");
        } else {
            str.append(": ").append(protocol);
            if (parameters.containsKey("D"))
                str.append(" Device: ").append(parameters.get("D"));
            if (parameters.containsKey("S"))
                str.append(".").append(parameters.get("S"));
            if (parameters.containsKey("F"))
                str.append(" Function: ").append(parameters.get("F"));
            for (Entry<String, Long> kvp : parameters.entrySet()) {
                String parName = kvp.getKey();
                if (parName.equals("F") || parName.equals("D") || parName.equals("F"))
                    continue;
                str.append(" ").append(parName).append("=").append(kvp.getValue());
            }
        }

        return str.toString();
    }

    /**
     *
     * @return Nicely formatted string the way the user would like to see it if truncated to "small" length.
     */
    public String prettyValueString() {
        return (parameters != null && !parameters.isEmpty()) ? protocol + ", " + toPrintString(parameters)
                : ccf != null ? this.ccf
                : "Raw signal";
    }

    public String toPrintString() throws IrpMasterException {
        StringBuilder str = new StringBuilder(name != null ? name : "<unnamed>");
        str.append(": ");
        if (parameters != null)
            str.append(protocol).append(", ").append(toPrintString(parameters));
        else if (ccf != null)
            str.append(ccf);
        else
            str.append(toIrSignal().toPrintString(true));

        return str.toString();
    }

    @Override
    public String toString() {
        try {
            return toPrintString();
        } catch (IrpMasterException ex) {
            return name + ": (<erroneous signal>)";
        }
    }

    private void sanityCheck() throws IrpMasterException {
        boolean protocolOk = protocol != null && ! protocol.isEmpty();
        boolean parametersOk = parameters != null && ! parameters.isEmpty();
        boolean rawOk = (intro != null && ! intro.isEmpty()) || (repeat != null && ! repeat.isEmpty());
        boolean ccfOk = ccf != null && ! ccf.isEmpty();

        if (masterType == null)
            masterType = (protocolOk && parametersOk) ? MasterType.parameters
                    : rawOk ? MasterType.raw
                    : ccfOk ? MasterType.ccf
                    : null;

        if (masterType == null)
            throw new IrpMasterException("Command " + name + ": No usable data or parameters.");

        if (masterType == MasterType.parameters && !protocolOk)
            throw new IrpMasterException("Command " + name + ": MasterType is parameters, but no protocol found.");
        if (masterType == MasterType.parameters && !parametersOk)
            throw new IrpMasterException("Command " + name + ": MasterType is parameters, but no parameters found.");
        if (masterType == MasterType.raw && !rawOk)
            throw new IrpMasterException("Command " + name + ": MasterType is raw, but both intro- and repeat-sequence empty.");
        if (masterType == MasterType.ccf && !ccfOk)
            throw new IrpMasterException("Command " + name + ": MasterType is ccf, but no ccf found.");
    }

    /**
     * This constructor is for importing from the Element as first argument.
     * @param element
     * @param inheritProtocol
     * @param inheritParameters
     * @throws ParseException
     * @throws IrpMasterException
     */
    public Command(Element element, String inheritProtocol, HashMap<String, Long> inheritParameters) throws ParseException, IrpMasterException {
        protocol = inheritProtocol;
        parameters = new HashMap<>();
        parameters.putAll(inheritParameters);
        name = element.getAttribute("name");
        comment = element.getAttribute("comment");
        try {
            masterType = MasterType.valueOf(element.getAttribute("master"));
        } catch (IllegalArgumentException ex) {
            masterType = null;
        }
        otherFormats = new HashMap<>();
        NodeList nl = element.getElementsByTagName("notes");
        if (nl.getLength() > 0)
            notes = ((Element) nl.item(0)).getTextContent();

        try {
            NodeList paramsNodeList = element.getElementsByTagName("parameters");
            if (paramsNodeList.getLength() > 0) {
                Element params = (Element) paramsNodeList.item(0);
                nl = params.getElementsByTagName("parameter");
                for (int i = 0; i < nl.getLength(); i++) {
                    Element el = (Element) nl.item(i);
                    parameters.put(el.getAttribute("name"), Long.parseLong(el.getAttribute("value")));
                }
            }
            String Fstring = element.getAttribute("F");
            if (!Fstring.isEmpty())
                parameters.put("F", Long.parseLong(Fstring));
            nl = element.getElementsByTagName("raw");
            if (nl.getLength() > 0) {
                Element el = (Element) nl.item(0);
                String freq = el.getAttribute("frequency");
                if (!freq.isEmpty())
                    frequency = Integer.parseInt(freq);
                String dc = el.getAttribute("dutyCycle");
                if (!dc.isEmpty()) {
                    dutyCycle = Double.parseDouble(dc);
                    if (dutyCycle <= 0 || dutyCycle >= 1)
                        throw new ParseException("Invalid dutyCycle: " + dutyCycle + "; must be between 0 and 1.", (int) IrpUtils.invalid);
                }
                NodeList nodeList = el.getElementsByTagName("intro");
                if (nodeList.getLength() > 0)
                    intro = parseSequence((Element) nodeList.item(0));
                nodeList = el.getElementsByTagName("repeat");
                if (nodeList.getLength() > 0)
                    repeat = parseSequence((Element) nodeList.item(0));
                nodeList = el.getElementsByTagName("ending");
                if (nodeList.getLength() > 0)
                    ending = parseSequence((Element) nodeList.item(0));
            }
            nl = element.getElementsByTagName("ccf");
            if (nl.getLength() > 0)
                ccf = ((Element) nl.item(0)).getTextContent();
            nl = element.getElementsByTagName("format");
            for (int i = 0; i < nl.getLength(); i++) {
                Element el = (Element) nl.item(0);
                otherFormats.put(el.getAttribute("name"), el.getTextContent());
            }
        } catch (NumberFormatException ex) {
            throw new ParseException("NumberFormatException " + ex.getMessage(), (int) IrpUtils.invalid);
        }
        sanityCheck();
    }

    public Command(String name, String comment, IrSignal irSignal) {
        this(name, comment, irSignal, false, false);
    }

    /**
     * Construct a Command from an IrSignal, i.e. timing data.
     *
     * @param name Name of command
     * @param comment textual comment
     * @param irSignal IrSignal
     * @param generateCcf generate the CCF form, if not already present.
     * @param decode invoke decoding.
     */
    public Command(String name, String comment, IrSignal irSignal, boolean generateCcf, boolean decode) {
        masterType = MasterType.raw;
        this.name = name;
        this.comment = comment;
        generateRaw(irSignal);

        if (generateCcf)
            generateCcf(irSignal);
        if (decode) {
            generateDecode(irSignal);
        }
    }

    /**
     * Construct a Command from protocol and parameters.
     *
     * @param name
     * @param comment
     * @param protocol
     * @param parameters
     * @throws IrpMasterException
     */
    @SuppressWarnings("unchecked")
    public Command(String name, String comment, String protocol, HashMap<String, Long> parameters) throws IrpMasterException {
        masterType = MasterType.parameters;
        this.name = name;
        this.comment = comment;
        this.parameters = (HashMap<String, Long>) parameters.clone();
        this.protocol = protocol;
        sanityCheck();
    }

    /**
     * Construct a Command from protocol and parameters.
     *
     * @param name
     * @param comment
     * @param protocol
     * @param parameters
     * @param irpMaster
     * @param generateRaw
     * @param generateCcf
     * @throws IrpMasterException
     */
    public Command(String name, String comment, String protocol, HashMap<String, Long> parameters, IrpMaster irpMaster, boolean generateRaw, boolean generateCcf) throws IrpMasterException {
        this(name, comment, protocol, parameters);

        if (!generateRaw && !generateCcf)
            return;

        if (irpMaster == null)
            throw new NullPointerException("IrpMaster is not setup.");

        IrSignal irSignal = new IrSignal(irpMaster, protocol, parameters);
        if (generateRaw)
            generateRaw(irSignal);
        if (generateCcf)
            generateCcf(irSignal);
    }

    /**
     * Convenience version of the constructor with IrpMaster.
     * @param name
     * @param comment
     * @param protocol
     * @param parameters
     * @param protocolIniPath
     * @param generateRaw
     * @param generateCcf
     * @throws FileNotFoundException
     * @throws IrpMasterException
     */
    public Command(String name, String comment, String protocol, HashMap<String, Long> parameters, String protocolIniPath, boolean generateRaw, boolean generateCcf) throws FileNotFoundException, IrpMasterException {
        this(name, comment, protocol, parameters, new IrpMaster(protocolIniPath), generateRaw, generateCcf);
    }

    /**
     * Construct a Command from CCF form.
     *
     * @param name
     * @param comment
     * @param ccf
     * @param generateRaw
     * @param decode
     * @throws IrpMasterException
     */
    public Command(String name, String comment, String ccf, boolean generateRaw, boolean decode) throws IrpMasterException {
        masterType = MasterType.ccf;
        this.name = name;
        this.ccf = ccf;
        this.comment = comment;

        if (!generateRaw && !decode)
            return;

        IrSignal irSignal = new IrSignal(ccf);
        if (decode)
            generateDecode(irSignal);
        if (generateRaw)
            generateRaw(irSignal);
        sanityCheck();
    }

    public static HashMap<String,Command> toHashMap(Command command) {
        HashMap<String,Command> result = new HashMap<>(1);
        result.put(command.getName(), command);
        return result;
    }

    private void generateDecode(IrSignal irSignal) {
        DecodeIR.DecodedSignal[] decodes = DecodeIR.decode(irSignal);

        if (decodes == null) {
            notes = "DecodeIR was attempted, but not found.";
        } else {
            if (decodes.length > 0 && !decodes[0].getProtocol().startsWith("Gap") && decodes[0].getParameters().size() > 0) {
                protocol = decodes[0].getProtocol();
                parameters = decodes[0].getParameters();
            }

            if (decodes.length == 0)
                notes = "DecodeIr was invoked, but found no decode.";
            else if (decodes.length > 1 || decodes[0].getProtocol().startsWith("Gap") || decodes[0].getParameters().isEmpty()) {
                notes = (decodes.length > 1)
                        ? "Several decodes from DecodeIr:"
                        : "Failed decodes from DecodeIr:";
                for (DecodeIR.DecodedSignal dec : decodes) {
                    notes = notes + linefeed + dec.toString();
                }
            }
        }
    }

    /**
     * Tries to decode the signals, unless parameters already are present.
     * @throws IrpMasterException
     */
    public void checkForParameters() throws IrpMasterException {
        if (parameters == null || parameters.isEmpty())
            generateDecode(toIrSignal());
    }

    public void checkForRaw() throws IrpMasterException {
        if ((intro != null) || (repeat != null) || (ending != null))
            return;

        IrSignal irSignal = masterType == MasterType.parameters
                ? new IrSignal(irpMaster, protocol, parameters)
                : new IrSignal(ccf);

        generateRaw(irSignal);
    }

    public void checkForCcf() throws IrpMasterException {
        if (ccf != null)
            return;

        IrSignal irSignal = masterType == MasterType.parameters
                ? new IrSignal(irpMaster, protocol, parameters)
                : new IrSignal(frequency, dutyCycle, intro, repeat, ending);
        generateCcf(irSignal);
    }

    private void generateRaw(IrSignal irSignal) {
        frequency = (int) Math.round(irSignal.getFrequency());
        dutyCycle = irSignal.getDutyCycle();
        intro = irSignal.getIntroSequence().toPrintString(true, false, " ", false);
        repeat = irSignal.getRepeatSequence().toPrintString(true, false, " ", false);
        ending = irSignal.getEndingSequence().toPrintString(true, false, " ", false);
    }

    private void generateCcf(IrSignal irSignal) {
        ccf = Pronto.toPrintString(irSignal);
    }

    public void addFormat(String name, String value) {
        if (otherFormats == null)
            otherFormats = new HashMap<>();
        otherFormats.put(name, value);
    }

    /**
     * Add an extra format to the Command.
     * @param format
     * @param repeatCount
     * @throws IrpMasterException
     */
    public void addFormat(CommandTextFormat format, int repeatCount) throws IrpMasterException {
        addFormat(format.getName(), format.format(toIrSignal(), repeatCount));
    }

    /**
     * XMLExport of the Command.
     *
     * @param doc
     * @param title
     * @param fatRaw
     * @param generateRaw
     * @param generateCcf
     * @param generateParameters
     * @return XML Element of gid "command".
     * @throws IrpMasterException
     */
    public Element xmlExport(Document doc, String title, boolean fatRaw,
            boolean generateRaw, boolean generateCcf, boolean generateParameters) throws IrpMasterException {
        Element element = doc.createElement("command");
        if (title != null)
            element.setAttribute("title", title);
        element.setAttribute("name", name);
        element.setAttribute("master", masterType.name());
        if (comment != null)
            element.setAttribute("comment", comment);
        if (notes != null) {
            Element notesEl = doc.createElement("notes");
            notesEl.setTextContent(notes);
            element.appendChild(notesEl);
        }
        if (generateParameters) {
            checkForParameters();
            if (parameters != null) {
                Element parametersEl = doc.createElement("parameters");
                if (protocol != null)
                    parametersEl.setAttribute("protocol", protocol);
                element.appendChild(parametersEl);
                for (Entry<String, Long> parameter : parameters.entrySet()) {
                    Element parameterEl = doc.createElement("parameter");
                    parameterEl.setAttribute("name", parameter.getKey());
                    parameterEl.setAttribute("value", parameter.getValue().toString());
                    parametersEl.appendChild(parameterEl);
                }
            }
        }
        if (generateRaw) {
            checkForRaw();
            if (intro != null || repeat != null || ending != null) {
                Element rawEl = doc.createElement("raw");
                rawEl.setAttribute("frequency", Integer.toString(frequency));
                if (dutyCycle > 0)
                    rawEl.setAttribute("dutyCycle", Double.toString(dutyCycle));
                element.appendChild(rawEl);
                processRaw(doc, rawEl, intro, "intro", fatRaw);
                processRaw(doc, rawEl, repeat, "repeat", fatRaw);
                processRaw(doc, rawEl, ending, "ending", fatRaw);
            }
        }
        if (generateCcf) {
            checkForCcf();
            if (ccf != null) {
                Element ccfEl = doc.createElement("ccf");
                ccfEl.setTextContent(ccf);
                element.appendChild(ccfEl);
            }
        }
        if (otherFormats != null) {
            for (Entry<String, String> format : otherFormats.entrySet()) {
                Element formatEl = doc.createElement("format");
                formatEl.setAttribute("name", format.getKey());
                formatEl.setTextContent(format.getValue());
                element.appendChild(formatEl);
            }
        }
        return element;
    }

    private static void processRaw(Document doc, Element element, String sequence, String tagName, boolean fatRaw) {
        Element el = xmlExport(doc, sequence, tagName, fatRaw);
        if (el != null)
            element.appendChild(el);
    }

    private static Element xmlExport(Document doc, String sequence, String tagName, boolean fatRaw) {
        if (sequence == null || sequence.isEmpty())
            return null;

        Element el = doc.createElement(tagName);
        if (fatRaw)
            insertFatElements(doc, el, sequence);
        else
            el.setTextContent(sequence);

        return el;
    }

    private static void insertFatElements(Document doc, Element el, String sequence) {
        String[] durations = sequence.split(" ");
        for (int i = 0; i < durations.length; i++) {
            String duration = durations[i].replaceAll("[\\+-]", "");
            Element e = doc.createElement(i % 2 == 0 ? "flash" : "gap");
            e.setTextContent(duration);
            el.appendChild(e);
        }
    }

    private static String parseSequence(Element element) {
        if (element.getElementsByTagName("flash").getLength() > 0) {
            StringBuilder str = new StringBuilder();
            NodeList nl = element.getChildNodes();
            for (int i = 0; i < nl.getLength(); i++) {
                if (nl.item(i).getNodeType() != Node.ELEMENT_NODE)
                    continue;
                Element el = (Element) nl.item(i);
                switch (el.getTagName()) {
                    case "flash":
                        str.append(" +").append(el.getTextContent());
                        break;
                    case "gap":
                        str.append(" -").append(el.getTextContent());
                        break;
                    default:
                        throw new RuntimeException("Invalid tag name");
                }
            }
            return str.substring(1);
        } else
            return element.getTextContent();
    }

    /**
     * Just for testing, not for deployment.
     * @param args
     */
    public static void main(String[] args) {
        String ccf = "0000 006C 0022 0002 015B 00AD 0016 0016 0016 0016 0016 0041 0016 0041 0016 0016 0016 0016 0016 0016 0016 0016 0016 0016 0016 0041 0016 0016 0016 0016 0016 0016 0016 0041 0016 0016 0016 0016 0016 0016 0016 0016 0016 0016 0016 0041 0016 0041 0016 0041 0016 0016 0016 0016 0016 0041 0016 0041 0016 0041 0016 0016 0016 0016 0016 0016 0016 0041 0016 0041 0016 06A4 015B 0057 0016 0E6C";
        try {
            Command irCommand = new Command("test", "A very cool signal", ccf, true, true);
            Document doc = XmlUtils.newDocument();
            Element el = irCommand.xmlExport(doc, "A very silly title", args.length > 1, true, true, true);
            doc.appendChild(el);
            XmlUtils.printDOM(new File("junk.xml"), doc);
        } catch (FileNotFoundException | IrpMasterException ex) {
            System.err.println(ex.getMessage());
        }
    }
}
