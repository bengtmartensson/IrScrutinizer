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
import org.harctoolbox.IrpMaster.Protocol;
import org.harctoolbox.IrpMaster.UnassignedException;
import org.harctoolbox.IrpMaster.XmlUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * This class models the command in Girr. A command is essentially an IR signal,
 * given either by protocol/parameters or timing data, and a name.
 * <p>
 * Some protocols have toggles, a persistant variable that changes between invocations.
 * If a such a protocol is used, there are two cases
 * <ol>
 * <li>It the toggle parameter is explicitly specified, the signal is treated no different from other signals,
 * and no particular treatment of the toggle parameter occurs.
 * <li>If the toggle parameter is not explicitly specified, the CCF and the raw versions are computed for
 * all values of the toggle. They can be accessed by the version of the functions getCcf, toIrSignal, getIntro, getRepeat, getEnding
 * taking an argument, corresponding to the value of the toggle parameter.
 * </ol>
 *
 * <p>
 * The member functions of class may throw IrpMasterExceptions when they encounter
 * erroneous data. The other classes in the package may not; they should just
 * ignore individual unparseable commands.
 */
public class Command implements Serializable {



    /** Attribute name for toggle used in girr file */
    private final static String toggleAttributeName = "T";

    /** Name of the parameter containing the toggle in the IRP protocol. */
    private final static String toggleParameterName = "T";

    private static final String linefeed = System.getProperty("line.separator", "\n");

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
     * This describes which representation of a Command constitutes the master,
     * from which the other representations are derived.
     * Note that raw and ccf cannot have toggles.
     */
    public static enum MasterType {
        /** The raw representation is the master. Does not have multiple toggle values. */
        raw,

        /** The CCF (also called Pronto Hex) representation is the master. Does not have multiple toggle values. */
        ccf,

        /** The protocol/parameter version is the master. May have multiple CCF/raw representations if the protocol has a toggle. */
        parameters
    }
    private transient Protocol protocol;
    private static IrpMaster irpMaster;
    private MasterType masterType;
    private String notes;
    private String name;
    private String protocolName;
    private HashMap<String, Long> parameters;
    private int frequency;
    private double dutyCycle;
    private String[] intro;
    private String[] repeat;
    private String[] ending;
    private String[] ccf;
    private String comment;
    private HashMap<String, String> otherFormats;

    /**
     * Sets an global IrpMaster instance, which will be used in subsequent transformations from parameter format.
     * @param newIrpMaster IrpMaster instance
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
     * @return name of the protocol
     */
    public String getProtocol() {
        return protocolName;
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
     * Returns the first intro sequence. Equivalent to getIntro(0);
     * @return the intro
     */
    public String getIntro() {
        return getIntro(0);
    }

    /**
     *
     * @param T toggle value
     * @return intro sequence corresponding to T.
     */
    public String getIntro(int T) {
        barfIfInvalidToggle(T);
        return intro[T];
    }

    /**
     * Returns the first repeat sequence. Equivalent to getRepeat(0);
     * @return the repeat
     */
    public String getRepeat() {
        return getRepeat(0);
    }

    /**
     *
     * @param T toggle value
     * @return repeat sequence corresponding to T.
     */
    public String getRepeat(int T) {
        barfIfInvalidToggle(T);
        return repeat[T];
    }

    /**
     * Returns the first ending sequence. Equivalent to getEnding(0).
     * @return the ending
     */
    public String getEnding() {
        return getEnding(0);
    }

    /**
     *
     * @param T toggle value
     * @return ending sequence corresponding to T.
     */
    public String getEnding(int T) {
        barfIfInvalidToggle(T);
        return ending[T];
    }

    /**
     * Returns the  CCF (Pronto Hex) version of the first signal. Equivalent to getCcf(0).
     * @return the ccf
     */
    public String getCcf() {
        return getCcf(0);
    }

    /**
     *
     * @param T toggle value
     * @return ccf corresponding to T
     */
    public String getCcf(int T) {
        barfIfInvalidToggle(T);
        return ccf[T];
    }

    /**
     * Checks that the parameter T is a valid toggle value; throws an exception otherwise.
     * @param T toggle value
     * @throws IllegalArgumentException
     */
    private void barfIfInvalidToggle(int T) throws IllegalArgumentException {
        barfIfInvalidToggle(T, numberToggles());
    }

    private void barfIfInvalidToggle(int T, int noToggles) throws IllegalArgumentException {
        if (T < 0 || T >= noToggles)
            throw new IllegalArgumentException("Illegal value of T = " + T);
    }

    /**
     * @return Collection of the otherFormats
     */
    public Collection<String> getOtherFormats() {
        return otherFormats.keySet();
    }

    /**
     * Returns an "other" format, identified by its name.
     * @param name format name
     * @return test string of the format.
     */
    public String getFormat(String name) {
        return otherFormats != null ? otherFormats.get(name) : null;
    }

    /**
     * Returns the IrSignal of the Command.
     * @return IrSignal
     * @throws IrpMasterException
     */
    public IrSignal toIrSignal() throws IrpMasterException {
        return toIrSignal(0);
    }

    /**
     * Returns the IrSignal of the Command.
     * @param T toggle value
     * @return IrSignal corresponding to the Command.
     * @throws IrpMasterException
     */
    public IrSignal toIrSignal(int T) throws IrpMasterException {
        barfIfInvalidToggle(T);
        return
                masterType == MasterType.parameters ? new IrSignal(irpMaster, protocolName, parameters)
                : masterType == MasterType.raw ? new IrSignal(frequency, dutyCycle, intro[T], repeat[T], ending[T])
                : new IrSignal(ccf[T]);
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

    // Nice to have: a version that takes a user supplied format string as argument.
    /**
     *
     * @return A "pretty" textual representation of the protocol and the parameters.
     */
    public String nameProtocolParameterString() {
        StringBuilder str = new StringBuilder(name != null ? name : "<no_name>");
        if (protocolName == null) {
            str.append(": <no decode>");
        } else {
            str.append(": ").append(protocolName);
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
        return (parameters != null && !parameters.isEmpty()) ? protocolName + ", " + toPrintString(parameters)
                : ccf != null ? ccf[0]
                : "Raw signal";
    }

    /**
     *
     * @return Nicely formatted string the way the user would like to see it if truncated to "small" length.
     */
    public String toPrintString() throws IrpMasterException {
        StringBuilder str = new StringBuilder(name != null ? name : "<unnamed>");
        str.append(": ");
        if (parameters != null)
            str.append(protocolName).append(", ").append(toPrintString(parameters));
        else if (ccf != null)
            str.append(getCcf());
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
        boolean protocolOk = protocolName != null && ! protocolName.isEmpty();
        boolean parametersOk = parameters != null && ! parameters.isEmpty();
        boolean rawOk = (intro != null && ! intro[0].isEmpty()) || (repeat != null && ! repeat[0].isEmpty());
        boolean ccfOk = ccf != null && ! ccf[0].isEmpty();

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
    public Command(Element element, String inheritProtocol, HashMap<String, Long> inheritParameters) throws ParseException, IrpMasterException, IllegalArgumentException {
        this(element.getAttribute("master"), element.getAttribute("name"), element.getAttribute("comment")); // throws IllegalArgumentException
        protocolName = inheritProtocol;
        parameters = new HashMap<>();
        parameters.putAll(inheritParameters);
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
                    parameters.put(el.getAttribute("name"), IrpUtils.parseLong(el.getAttribute("value")));
                }
            }
            String Fstring = element.getAttribute("F");
            if (!Fstring.isEmpty())
                parameters.put("F", IrpUtils.parseLong(Fstring));
            nl = element.getElementsByTagName("raw");
            if (nl.getLength() > 0) {
                intro = new String[nl.getLength()];
                repeat = new String[nl.getLength()];
                ending = new String[nl.getLength()];
                for (int i = 0; i < nl.getLength(); i++) {
                    Element el = (Element) nl.item(i);
                    int T;
                    try {
                        T = Integer.parseInt(el.getAttribute(toggleAttributeName));
                    } catch (NumberFormatException ex) {
                        T = 0;
                    }
                    barfIfInvalidToggle(T, nl.getLength()); // throws IllegalArgumentException
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
                        intro[T] = parseSequence((Element) nodeList.item(0));
                    nodeList = el.getElementsByTagName("repeat");
                    if (nodeList.getLength() > 0)
                        repeat[T] = parseSequence((Element) nodeList.item(0));
                    nodeList = el.getElementsByTagName("ending");
                    if (nodeList.getLength() > 0)
                        ending[T] = parseSequence((Element) nodeList.item(0));
                }
            }
            nl = element.getElementsByTagName("ccf");
            if (nl.getLength() > 0) {
                ccf = new String[nl.getLength()];
                for (int i = 0; i < nl.getLength(); i++) {
                    Element el = (Element) nl.item(i);
                    int T;
                    try {
                        T = Integer.parseInt(el.getAttribute(toggleAttributeName));
                    } catch (NumberFormatException ex) {
                        T = 0;
                    }
                    barfIfInvalidToggle(T, nl.getLength()); // throws IllegalArgumentException
                    ccf[T] = el.getTextContent();
                }
            }
            nl = element.getElementsByTagName("format");
            for (int i = 0; i < nl.getLength(); i++) {
                Element el = (Element) nl.item(0);
                otherFormats.put(el.getAttribute("name"), el.getTextContent());
            }
        } catch (IllegalArgumentException ex) { // contains NumberFormatException
            throw new ParseException(ex.getClass().getSimpleName() + " " + ex.getMessage(), (int) IrpUtils.invalid);
        }
        sanityCheck();
    }

    /**
     * Construct a Command from an IrSignal, i.e.&nbsp;timing data.
     * @param name
     * @param comment
     * @param irSignal
     */
    public Command(String name, String comment, IrSignal irSignal) {
        this(name, comment, irSignal, false, false);
    }

    /**
     * Construct a Command from an IrSignal, i.e.&nbsp;timing data.
     *
     * @param name Name of command
     * @param comment textual comment
     * @param irSignal IrSignal
     * @param generateCcf generate the CCF form, if not already present.
     * @param decode invoke decoding.
     */
    public Command(String name, String comment, IrSignal irSignal, boolean generateCcf, boolean decode) {
        this(MasterType.raw, name, comment);
        generateRaw(irSignal);

        if (generateCcf)
            generateCcf(irSignal);
        if (decode) {
            generateDecode(irSignal);
        }
    }

    /**
     * Construct a Command from protocolName and parameters.
     *
     * @param name
     * @param comment
     * @param protocolName
     * @param parameters
     * @throws IrpMasterException
     */
    @SuppressWarnings("unchecked")
    public Command(String name, String comment, String protocolName, HashMap<String, Long> parameters) throws IrpMasterException {
        this(name, comment, protocolName, irpMaster.newProtocol(protocolName), parameters, false, false);
    }

    /**
     * Construct a Command from protocolName and parameters.
     *
     * @param name
     * @param comment
     * @param protocolName
     * @param parameters
     * @param irpMstr
     * @param generateRaw
     * @param generateCcf
     * @throws IrpMasterException
     */
    @SuppressWarnings("unchecked")
    public Command(String name, String comment, String protocolName, HashMap<String, Long> parameters,
            IrpMaster irpMstr, boolean generateRaw, boolean generateCcf) throws IrpMasterException {
        this(name, comment, protocolName, irpMstr.newProtocol(protocolName), parameters, generateRaw, generateCcf);
    }

    @SuppressWarnings("unchecked")
    private Command(String name, String comment, String protocolName, Protocol protocol, HashMap<String, Long> parameters,
            boolean generateRaw, boolean generateCcf) throws IrpMasterException {
        this(MasterType.parameters, name, comment);
        this.parameters = (HashMap<String, Long>) parameters.clone();
        this.protocolName = protocolName;
        this.protocol = protocol;
        sanityCheck();

        if (generateRaw || generateCcf)
            generateRawCcfAllT(parameters, generateRaw, generateCcf);
    }

    private void generateRawCcfAllT(HashMap<String, Long> parameters, boolean generateRaw, boolean generateCcf) throws IrpMasterException {
        if (numberToggles() == 1)
            generateRawCcf(parameters, generateRaw, generateCcf);
        else
            for (int T = 0; T < numberToggles(); T++)
                generateRawCcfForceT(parameters, T, generateRaw, generateCcf);
    }

    private void generateRawCcfForceT(HashMap<String, Long> parameter, int T, boolean generateRaw, boolean generateCcf) throws IrpMasterException {
        @SuppressWarnings("unchecked")
        HashMap<String, Long> params = (HashMap<String, Long>) parameters.clone();
        params.put(toggleParameterName, (long) T);
        generateRawCcf(params, T, generateRaw, generateCcf);
    }

    private void generateRawCcf(HashMap<String, Long> parameter, boolean generateRaw, boolean generateCcf) throws IrpMasterException {
        IrSignal irSignal = protocol.renderIrSignal(parameters);
        if (generateRaw)
            generateRaw(irSignal);
        if (generateCcf)
            generateCcf(irSignal);
    }

    private void generateRawCcf(HashMap<String, Long> parameters, int T, boolean generateRaw, boolean generateCcf) throws IrpMasterException {
        IrSignal irSignal = protocol.renderIrSignal(parameters);
        if (generateRaw)
            generateRaw(irSignal, T);
        if (generateCcf)
            generateCcf(irSignal, T);
    }

    private Command(MasterType masterType, String name, String comment) {
        this.masterType = masterType;
        this.name = name;
        this.comment = comment;
        frequency = (int) IrpUtils.invalid;
        dutyCycle = (double) IrpUtils.invalid;
        ccf = null;
        intro = null;
        repeat = null;
        ending = null;
    }

    private Command(String masterType, String name, String comment) throws IllegalArgumentException {
        this(MasterType.valueOf(masterType), name, comment);
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
        this(MasterType.ccf, name, comment);
        this.ccf = new String[1];
        this.ccf[0] = ccf;

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
                protocolName = decodes[0].getProtocol();
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
     * Tries to generate the parameter version of the signal (decoding the signals),
     * unless parameters already are present.
     * @throws IrpMasterException
     */
    public void checkForParameters() throws IrpMasterException {
        if (parameters == null || parameters.isEmpty())
            generateDecode(toIrSignal());
    }

    /**
     * Tries to generate the raw version of the signal, unless already present.
     * @throws IrpMasterException
     */
    public void checkForRaw() throws IrpMasterException {
        if ((intro != null) || (repeat != null) || (ending != null))
            return;

        if (masterType == MasterType.parameters)
            generateRawCcfAllT(parameters, true, false);
        else {
            IrSignal irSignal = new IrSignal(ccf[0]);
            generateRaw(irSignal);
        }
    }

    /**
     * Tries to generate the CCF (Pronto Hex) version of the signal, unless already present.
     * @throws IrpMasterException
     */
    public void checkForCcf() throws IrpMasterException {
        if (ccf != null)
            return;

        if (masterType == MasterType.parameters)
            generateRawCcfAllT(parameters, false, true);
        else {
            IrSignal irSignal = new IrSignal(frequency, dutyCycle, intro[0], repeat[0], ending[0]);
            generateCcf(irSignal);
        }
    }

    private int numberToggles() {
        try {
            return (masterType == MasterType.parameters
                    && !parameters.containsKey(toggleParameterName)
                    && protocol.hasParameter(toggleParameterName)
                    && protocol.hasParameterMemory(toggleParameterName)) ? ((int)protocol.getParameterMax(toggleParameterName)) + 1 : 1;
        } catch (UnassignedException ex) {
            // cannot happen
            assert (false);
            return 1;
        }
    }

    private void generateRaw(IrSignal irSignal) {
        generateRaw(irSignal,  0);
    }

    private void generateRaw(IrSignal irSignal, int T) throws IllegalArgumentException {
        barfIfInvalidToggle(T);
        frequency = (int) Math.round(irSignal.getFrequency());
        dutyCycle = irSignal.getDutyCycle();
        if (intro == null)
            intro = new String[numberToggles()];
        intro[T] = irSignal.getIntroSequence().toPrintString(true, false, " ", false);
        if (repeat == null)
            repeat = new String[numberToggles()];
        repeat[T] = irSignal.getRepeatSequence().toPrintString(true, false, " ", false);
        if (ending == null)
            ending = new String[numberToggles()];
        ending[T] = irSignal.getEndingSequence().toPrintString(true, false, " ", false);
    }

    private void generateCcf(IrSignal irSignal) {
        generateCcf(irSignal, 0);
    }

    private void generateCcf(IrSignal irSignal, int T) throws IllegalArgumentException {
        barfIfInvalidToggle(T);
        if (ccf == null)
            ccf = new String[numberToggles()];
        ccf[(int)T] = Pronto.toPrintString(irSignal);
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
     * @return XML Element with tag name "command".
     */
    public Element xmlExport(Document doc, String title, boolean fatRaw,
            boolean generateRaw, boolean generateCcf, boolean generateParameters) {
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
            try {
                checkForParameters();
                if (parameters != null) {
                    Element parametersEl = doc.createElement("parameters");
                    if (protocolName != null)
                        parametersEl.setAttribute("protocol", protocolName);
                    element.appendChild(parametersEl);
                    for (Entry<String, Long> parameter : parameters.entrySet()) {
                        Element parameterEl = doc.createElement("parameter");
                        parameterEl.setAttribute("name", parameter.getKey());
                        parameterEl.setAttribute("value", parameter.getValue().toString());
                        parametersEl.appendChild(parameterEl);
                    }
                }
            } catch (IrpMasterException ex) {
                element.appendChild(doc.createComment("Parameters requested but could not be generated."));
            }
        }
        if (generateRaw) {
            try {
                checkForRaw();
                if (intro != null || repeat != null || ending != null) {
                    for (int T = 0; T < numberToggles(); T++) {
                        Element rawEl = doc.createElement("raw");
                        rawEl.setAttribute("frequency", Integer.toString(frequency));
                        if (dutyCycle > 0)
                            rawEl.setAttribute("dutyCycle", Double.toString(dutyCycle));
                        if (numberToggles() > 1)
                            rawEl.setAttribute(toggleAttributeName, Integer.toString(T));
                        element.appendChild(rawEl);
                        processRaw(doc, rawEl, intro[T], "intro", fatRaw);
                        processRaw(doc, rawEl, repeat[T], "repeat", fatRaw);
                        processRaw(doc, rawEl, ending[T], "ending", fatRaw);
                    }
                }
            } catch (IrpMasterException ex) {
                element.appendChild(doc.createComment("Raw signal requested but could not be generated."));
            }
        }
        if (generateCcf) {
            try {
                checkForCcf();
                if (ccf != null) {
                    for (int T = 0; T < numberToggles(); T++) {
                        Element ccfEl = doc.createElement("ccf");
                        if (numberToggles() > 1)
                            ccfEl.setAttribute(toggleAttributeName, Integer.toString(T));
                        ccfEl.setTextContent(ccf[T]);
                        element.appendChild(ccfEl);
                    }
                }
            } catch (IrpMasterException ex) {
                element.appendChild(doc.createComment("Pronto Hex requested but could not be generated."));
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
        try {
            /*    String ccf = "0000 006C 0022 0002 015B 00AD 0016 0016 0016 0016 0016 0041 0016 0041 0016 0016 0016 0016 0016 0016 0016 0016 0016 0016 0016 0041 0016 0016 0016 0016 0016 0016 0016 0041 0016 0016 0016 0016 0016 0016 0016 0016 0016 0016 0016 0041 0016 0041 0016 0041 0016 0016 0016 0016 0016 0041 0016 0041 0016 0041 0016 0016 0016 0016 0016 0016 0016 0041 0016 0041 0016 06A4 015B 0057 0016 0E6C";
            try {
                Command irCommand = new Command("test", "A very cool signal", ccf, true, true);
                Document doc = XmlUtils.newDocument();
                Element el = irCommand.xmlExport(doc, "A very silly title", args.length > 1, true, true, true);
                doc.appendChild(el);
                XmlUtils.printDOM(new File("junk.xml"), doc);
             */

            HashMap<String, Long> parameters = new HashMap<>();
            parameters.put("F", 1L);
            parameters.put("D", 0L);
            Command irCommand = new Command("nombre", "komment", "RC5", parameters,
                    "/usr/local/share/irscrutinizer/IrpProtocols.ini", true, true);
            Document doc = XmlUtils.newDocument();
            Element el = irCommand.xmlExport(doc, "A very silly title", args.length > 1, true, true, true);
            doc.appendChild(el);
            XmlUtils.printDOM(new File("junk.xml"), doc);

        } catch (FileNotFoundException | IrpMasterException ex) {
            System.err.println(ex.getMessage());
        }
    }
}
