/*
Copyright (C) 2011 Bengt Martensson.

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

import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.antlr.runtime.ANTLRStringStream;
import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.RecognitionException;
import org.antlr.runtime.tree.CommonTree;
import org.antlr.runtime.tree.DOTTreeGenerator;
import org.antlr.stringtemplate.StringTemplate;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * This class implements the Protocol, per Chapter 1.6--1.7.
 *
 * There are too many public functions in the API...
 *
 */
public class Protocol {

    private String name;
    private String documentation;
    private String irpString;
    private CommonTree AST;
    private GeneralSpec generalSpec;
    private NameEngine nameEngine;
    private ParameterSpecs parameterSpecs;
    private CommonTree topBitspecIrsteam;
    private CommonTokenStream tokens;

    // True the first time render is called, then false -- to be able to initialize.
    private boolean virgin = true;

    private int count = 0;

    private Document doc = null;
    private Element root = null;
    private Element currentElement = null;

    public long evaluateName(String name) throws UnassignedException, DomainViolationException {
        long result;
        Debug.debugNameEngine("evaluateName(" + name + ") called");
        CommonTree tree = nameEngine.get(name);
        if (tree == null)
            throw new UnassignedException("Name `" + name + "' not assigned.");
        try {
            result = ASTTraverser.expression(this, tree);
            Debug.debugExpressions("finished evaluating `" + name + "' = " + result + " without exceptions");
        } catch (StackOverflowError ex) {
            throw new UnassignedException("Name `" + name + "' appears to be recursively defined; stack overflow catched.");
        }
        return result;
    }

    public long evaluateName(String name, long dflt) {
        try {
            return evaluateName(name);
        } catch (UnassignedException | DomainViolationException ex) {
            return dflt;
        }
    }

    public long tryEvaluateName(String name) {
        try {
            return evaluateName(name);
        } catch (UnassignedException ex) {
            System.err.println("Variablename " + name + " not currently assigned.");
            return 0;
        } catch (DomainViolationException ex) {
            System.err.println(ex.getMessage());
            return 0;
        }
    }

    public void assign(String name, long value) {
        nameEngine.assign(name, value);
    }

    public void assign(String str) throws IncompatibleArgumentException {
        String s = str.trim();
        if (s.startsWith("{"))
            nameEngine.readDefinitions(s);
        else {
            String[] kw = s.split("=");
            if (kw.length != 2)
                throw new IncompatibleArgumentException("Invalid assignment: " + s);

            assign(kw[0], IrpUtils.parseLong(kw[1], false));
        }
    }

    public void assign(String[] args, int skip) throws IncompatibleArgumentException {
        for (int i = skip; i < args.length; i++)
            assign(args[i]);
    }

    public BitDirection getBitDirection() {
        return generalSpec.getBitDirection();
    }

    public double getFrequency() {
        return generalSpec.getFrequency();
    }

    public double getUnit() {
        return generalSpec.getUnit();
    }

    public double getDutyCycle() {
        return generalSpec.getDutyCycle();
    }

    public long getParameterMin(String name) throws UnassignedException {
        ParameterSpec ps = parameterSpecs.getParameterSpec(name);
        if (ps == null)
            throw new UnassignedException("Parameter " + name + " not assigned.");

        return ps.getMin();
    }

    public long getParameterMax(String name) throws UnassignedException {
        ParameterSpec ps = parameterSpecs.getParameterSpec(name);
        if (ps == null)
            throw new UnassignedException("Parameter " + name + " not assigned.");

        return ps.getMax();
    }

    public boolean hasParameter(String name) {
        return parameterSpecs.getNames().contains(name);
    }

    /**
     * Returns a set of the names of all parameters present in the protocol.
     * @return a Set of the names of all parameters present in the protocol.
     */
    public Set<String> getParameterNames() {
        return parameterSpecs.getNames();
    }

    /**
     * Checks if the named parameter has memory.
     * @param name Name of the parameters.
     * @return existence of memory for the parameter given as argument-
     * @throws UnassignedException If there is no parameters with the name given as parameter.
     */
    public boolean hasParameterMemory(String name) throws UnassignedException {
        ParameterSpec parameterSpec = parameterSpecs.getParameterSpec(name);
        if (parameterSpec == null)
            throw new UnassignedException("Parameter " + name + " not assigned.");
        return parameterSpec.hasMemory();
    }

    /**
     * Does this protocol have parameters other than the standard ones (F, D, S, T)?
     * @return existence of other parameters.
     */
    public boolean hasAdvancedParameters() {
        for (String param : parameterSpecs.getNames())
            if (!(param.equals("F") || param.equals("D") || param.equals("S") || param.equals("T")))
                return true;

        return false;
    }

    public String getIrp() {
        return irpString;
    }

    /**
     *
     * @param name
     * @param actualParameters
     * @return Default value of parameter in first argument, taking variable assignment in second input into account.
     * @throws UnassignedException
     * @throws DomainViolationException
     */
    public long getParameterDefault(String name, Map<String, Long> actualParameters) throws UnassignedException, DomainViolationException {
        ParameterSpec ps = parameterSpecs.getParameterSpec(name);
        if (ps == null)
            throw new UnassignedException("Parameter " + name + " not assigned.");

        CommonTree t = ps.getDefault();
        if (t == null)
            return IrpUtils.invalid;

        //CommonTreeNodeStream nodes = new CommonTreeNodeStream(t);
        Protocol newProtocol = new Protocol();
        try {
            newProtocol.nameEngine.loadActualParameters(actualParameters, parameterSpecs);
        } catch (DomainViolationException ex) {
            System.err.println(ex.getMessage());
        }
        return ASTTraverser.expression(newProtocol, t);
    }

    /**
     * Checks if the named parameter exists and have a default.
     * @param name
     * @return true if the parameter exists and have a default.
     */
    public boolean hasParameterDefault(String name) {
        ParameterSpec ps = parameterSpecs.getParameterSpec(name);
        return ps != null && ps.getDefault() != null;
    }

    /**
     *
     * @param generalSpec
     */
    public Protocol(GeneralSpec generalSpec) {
        if (generalSpec == null)
            throw new RuntimeException("empty generalSpec");
        this.generalSpec = generalSpec;
        this.nameEngine = new NameEngine();
    }

    /** Just for testing and debugging */
    public Protocol() {
        this(new GeneralSpec());
    }

    public Protocol(String name, String irpString, String documentation) throws UnassignedException, ParseException {
        this.name = name;
        this.documentation = documentation;
        this.irpString = irpString;
        this.nameEngine = new NameEngine();
        IrpLexer lex = new IrpLexer(new ANTLRStringStream(irpString));
        tokens = new CommonTokenStream(lex);
        IrpParser parser = new IrpParser(tokens);
        IrpParser.protocol_return r;
        try {
            r = parser.protocol();
        } catch (RecognitionException ex) {
            throw new ParseException(ex);
        }
        AST = r.getTree();

        for (int i = 0; i < AST.getChildCount(); i++) {
            CommonTree ch = (CommonTree) AST.getChild(i);
            switch (ch.getText()) {
                case "GENERALSPEC":
                    generalSpec = new GeneralSpec(ch);
                    break;
                case "PARAMETER_SPECS":
                    parameterSpecs = new ParameterSpecs(ch);
                    break;
                case "BITSPEC_IRSTREAM":
                    topBitspecIrsteam = ch;
                    break;
                case "DEFINITIONS":
                    // nothing to do
                    break;
                default:
                    throw new RuntimeException("This cannot happen");
            }
        }
        if (parameterSpecs == null) {
            UserComm.warning("Parameter specs are missing from protocol. Runtime errors due to unassigned variables are possile. Also silent truncation of parameters can occur. Further messages on parameters will be suppressed.");
            parameterSpecs = new ParameterSpecs();
        }
        if (generalSpec == null) {
            throw new UnassignedException("GeneralSpec missing from protocol");
        }

        Debug.debugIrpParser("GeneralSpec: " + generalSpec);
        Debug.debugIrpParser("nameEngine: " + nameEngine);
        Debug.debugIrpParser("parameterSpec: " + parameterSpecs);
    }

    /**
     * Debugging and testing purposes only
     *
     * @return NameEngine as String.
     */
    public String nameEngineString() {
        return nameEngine.toString();
    }

    /**
     * Creates consisting of parameter values that can be used as part of filenames etc.
     * @param equals String between name and value, often "=",
     * @param separator String between name-value pairs, often ",".
     * @return String
     */
    public String notationString(String equals, String separator) {
        return nameEngine.notationString(equals, separator);
    }

    @Override
    public String toString() {
        return name + ": " + AST.toStringTree();
    }

    public String toDOT() {
        //DOTTreeGenerator gen = new DOTTreeGenerator();
        StringTemplate st = (new DOTTreeGenerator()).toDOT(AST);
        return st.toString();
    }

    public void setupDOM() {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setValidating(false);
        factory.setNamespaceAware(false);
        try {
            DocumentBuilder builder = factory.newDocumentBuilder();
            doc = builder.newDocument();
        } catch (ParserConfigurationException ex) {
            System.err.println(ex.getMessage());
        }

        root = doc.createElement("protocol");
        root.setAttribute("name", name);
        doc.appendChild(root);
        root.setAttribute("frequency", Long.toString(Math.round(generalSpec.getFrequency())));
        if (generalSpec.getDutyCycle() > 0)
            root.setAttribute("dutycycle", Double.toString(generalSpec.getDutyCycle()));
    }

    public void addSignal(Map<String, Long> actualParameters) {
        Element el = doc.createElement("signal");
        for (Entry<String, Long> entry : actualParameters.entrySet())
            el.setAttribute(entry.getKey(), Long.toString(entry.getValue()));

        root.appendChild(el);
        currentElement = el;

    }

    public void addXmlNode(String gid, String content) {
        Element el = doc.createElement(gid);
        el.setTextContent(content);
        currentElement.appendChild(el);
    }

    public void addRawSignalRepresentation(IrSignal irSignal) {
        Element raw_el = doc.createElement("raw");
        currentElement.appendChild(raw_el);
        insertXMLNode(raw_el, irSignal, Pass.intro);
        insertXMLNode(raw_el, irSignal, Pass.repeat);
        insertXMLNode(raw_el, irSignal, Pass.ending);
    }

    private void insertXMLNode(Element parent, IrSignal irSignal, Pass pass) {
        if (irSignal.getLength(pass) > 0) {
            Element el = doc.createElement(pass.name());
            parent.appendChild(el);
            for (int i = 0; i < irSignal.getLength(pass); i++) {
                double time = irSignal.getDouble(pass, i);
                Element duration = doc.createElement(time > 0 ? "flash" : "gap");
                duration.setTextContent(Long.toString(Math.round(Math.abs(time))));
                el.appendChild(duration);
            }
        }
    }

    public void printDOM(OutputStream ostream) {
        (new XmlExport(doc)).printDOM(ostream, null);
    }

    public void printDOM(OutputStream ostream, Document stylesheet) {
        (new XmlExport(doc)).printDOM(ostream, stylesheet);
    }

    public Document toDOM() {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setValidating(false);
        factory.setNamespaceAware(false);
        Document doc = null;
        try {
            DocumentBuilder builder = factory.newDocumentBuilder();
            doc = builder.newDocument();
        } catch (ParserConfigurationException ex) {
            System.err.println(ex.getMessage());
            return null;
        }

        Element root = doc.createElement("PROTOCOL");
        root.setAttribute("name", name);
        doc.appendChild(root);
        if (documentation != null) {
            Element docu = doc.createElement("DOCUMENTATION");
            docu.appendChild(doc.createCDATASection(documentation));
            root.appendChild(docu);
        }
        CommonTree t = AST;
        Element parent = root;
        parseTree(doc, t, parent);
        return doc;
    }

    // Traverse the CommonTree, thus populating the DOM tree as first argument.
    private void parseTree(Document doc, CommonTree t, Element parent) {
        parseTree(doc, t, parent, 0);
    }

    private void parseTree(Document doc, CommonTree t, Element parent, int ignore /* = 0 */) {
        for (int i = ignore; i < t.getChildCount(); i++) {
            CommonTree child = (CommonTree) t.getChild(i);
            String label = child.getText();
            if ("+-*/%?".contains(label) || label.equals("**"))
                label = "OPERATOR";
            //System.out.println(label);
            Element e = null;
            boolean isInteger = false;
            try {
                Integer.parseInt(label);
                isInteger = true;
            } catch (NumberFormatException ex) {
                isInteger = false;
            }

            if (isInteger) {
                e = doc.createElement("INT");
                e.setAttribute("value", label);
                parent.appendChild(e);
            } else if (label.equals("FREQUENCY") || label.equals("DUTYCYCLE") || label.equals("FLASH") || label.equals("GAP") || label.equals("EXTENT") || label.equals("UNIT")) {
                e = doc.createElement(label);
                parent.appendChild(e);

                if (child.getChild(0).getText().equals("FLOAT")) {
                    CommonTree val = (CommonTree) child.getChild(0);
                    e.setAttribute("value", val.getChild(0).getText() + "." + val.getChild(1).getText());
                } else
                    e.setAttribute("value", child.getChild(0).getText());
                if (child.getChildCount() >= 2)
                    e.setAttribute("unit", child.getChild(1).getText());
            } else if (label.equals("PARAMETER_SPEC") || label.equals("PARAMETER_SPEC_MEMORY")) {
                e = doc.createElement(label);
                parent.appendChild(e);
                e.setAttribute("name", child.getChild(0).getText());
                e.setAttribute("min", child.getChild(1).getText());
                e.setAttribute("max", child.getChild(2).getText());
                if (child.getChildCount() >= 4)
                    parseTree(doc, child, e, 3);

            } else if (label.equals("ASSIGNMENT")) {
                e = doc.createElement(label);
                parent.appendChild(e);
                e.setAttribute("name", child.getChild(0).getText());
                parseTree(doc, child, e, 1);

            } else if (label.equals("BITDIRECTION")) {
                e = doc.createElement(label);
                parent.appendChild(e);
                e.setAttribute("dir", child.getChild(0).getText());
            } else if (label.equals("REPEAT_MARKER")) {
                e = doc.createElement(label);
                parent.appendChild(e);
                e.setAttribute("type", child.getChild(0).getText());
            } else if (label.equals("OPERATOR")) {
                e = doc.createElement(label);
                e.setAttribute("type", child.getText());
                parent.appendChild(e);
                parseTree(doc, child, e);
            } else if (label.equals("PARAMETER_SPECS") || label.equals("GENERALSPEC") || label.equals("DEFINITIONS") || label.equals("DEFINITION") || label.equals("IRSTREAM") || label.equals("BARE_IRSTREAM") || label.equals("COMPLEMENT") || label.equals("BITFIELD") || label.equals("BITSPEC_IRSTREAM")|| label.equals("BITSPEC")) {
                e = doc.createElement(label);
                parent.appendChild(e);
                parseTree(doc, child, e);
            } else {
                e = doc.createElement("NAME");
                e.setAttribute("label", label);
                parent.appendChild(e);
                parseTree(doc, child, e);
            }
        }
    }

    public void interactiveRender(UserComm userComm, Map actualVars) {
        int passNo = 0;
        boolean initial = true;
        boolean done = false;
        boolean finalState = false;

        userComm.printMsg(irpString);
        while (! done) {
            try {
                PrimaryIrStream irStream = process(actualVars, passNo, /*considerRepeatMin*/ true, initial);
                initial = false;
                finalState = false;
                userComm.printMsg(irStream.toString());
                userComm.printMsg(nameEngine.toString());
                if (passNo == this.evaluateName("$final_state", IrpUtils.invalid)) {
                    userComm.printMsg("Final state reached");
                    finalState = true;
                }
                String line = userComm.getLine("Enter one of `arsiq' for advance, repeat, start, initialize, quit >");
                switch (line.charAt(0)) {
                    case 'a':
                        passNo = finalState ? 0 : passNo + 1;
                        break;
                    case 'i':
                        passNo = 0;
                        initial = true;
                        break;
                    case 'q':
                        done = true;
                        break;
                    case 'r':
                        break;
                    case 's':
                        passNo = 0;
                        break;
                    default:
                        userComm.errorMsg("Unknown command: " + line);
                        done = true;
                        break;
                }
                //done = true;
             } catch (IOException | IrpMasterException ex) {
                userComm.errorMsg(ex.getMessage());
                done = true;
             }
        }
    }

    public PrimaryIrStream process(Map<String, Long> actualVars, int passNo, boolean considerRepeatMin) throws DomainViolationException, UnassignedException, IncompatibleArgumentException, InvalidRepeatException {
        return process(actualVars, passNo, considerRepeatMin, virgin);
    }

    public PrimaryIrStream process(Map<String, Long> actualVars, Pass pass, boolean considerRepeatMin) throws DomainViolationException, UnassignedException, IncompatibleArgumentException, InvalidRepeatException {
        return process(actualVars, pass.toInt(), considerRepeatMin, virgin);
    }

    public PrimaryIrStream process(Map<String, Long> actualVars, int passNo, boolean considerRepeatMin, boolean initial) throws DomainViolationException, UnassignedException, IncompatibleArgumentException, InvalidRepeatException {
        // Load Definitions
        for (int i = 0; i < AST.getChildCount(); i++) {
            CommonTree ch = (CommonTree) AST.getChild(i);
            if (ch.getText().equals("DEFINITIONS"))
                nameEngine.readDefinitions(ch);
        }
        Debug.debugNameEngine(nameEngine.toString());

        // Defaults
        Debug.debugParameters(parameterSpecs.toString());

        if (initial)
            count = 0;

        nameEngine.assign("$count", count);

        nameEngine.loadDefaults(parameterSpecs, initial);
        Debug.debugNameEngine(nameEngine.toString());

        // Actual values
        Debug.debugParameters(actualVars.toString());
        // Read actual parameters into the name engine, thowing DomainViolationException if appropriate.
        nameEngine.loadActualParameters(actualVars, parameterSpecs);
        Debug.debugNameEngine(nameEngine.toString());

        // Check that parameters have values (throwing UnassignedException if appropriate).
        nameEngine.checkAssignments(parameterSpecs);

        // Now do the real work
        PrimaryIrStream irStream = ASTTraverser.bitspec_irstream(passNo, considerRepeatMin, this, topBitspecIrsteam);
        irStream.assignBitSpecs();
        count++;
        nameEngine.assign("$count", count);
         // = 8
        Debug.debugASTParser("finished parsing AST without exceptions.");
        // = 16
        Debug.debugNameEngine(nameEngine.toString());
        // 4096
        Debug.debugIrStreams("ProcessAST: " + irStream);

        virgin = false;
        return irStream;
    }

    public IrSequence render(Map<String, Long>actualVars, int pass, boolean considerRepeatMins, boolean initialize) throws IncompatibleArgumentException, UnassignedException, DomainViolationException, InvalidRepeatException {
        PrimaryIrStream irStream = process(actualVars, pass, considerRepeatMins, initialize);
        return new IrSequence(irStream);
    }

    public IrSequence render(Map<String, Long>actualVars, Pass pass, boolean considerRepeatMins, boolean initialize) throws IncompatibleArgumentException, UnassignedException, DomainViolationException, InvalidRepeatException {
        PrimaryIrStream irStream = process(actualVars, pass.toInt(), considerRepeatMins, initialize);
        return new IrSequence(irStream);
    }

    public IrSignal renderIrSignal(Map<String, Long>actualVars, int pass, boolean considerRepeatMins) throws DomainViolationException, UnassignedException, IncompatibleArgumentException, InvalidRepeatException {
        virgin = true;
        IrSequence intro  = (pass == Pass.intro.toInt()  || pass == IrpUtils.all) ? render(actualVars, Pass.intro,  considerRepeatMins,  true) : null; //TODO: what is correct?
        IrSequence repeat = (pass == Pass.repeat.toInt() || pass == IrpUtils.all) ? render(actualVars, Pass.repeat, false, false) : null;
        IrSequence ending = (pass == Pass.ending.toInt() || pass == IrpUtils.all) ? render(actualVars, Pass.ending, false, false) : null;
        return new IrSignal(getFrequency(), getDutyCycle(), intro, repeat, ending);
    }

    public IrSignal renderIrSignal(Map<String, Long>actualVars, int pass) throws DomainViolationException, UnassignedException, IncompatibleArgumentException, InvalidRepeatException {
        return renderIrSignal(actualVars, pass, true);
    }

    public IrSignal renderIrSignal(Map<String, Long>actualVars) throws DomainViolationException, UnassignedException, IncompatibleArgumentException, InvalidRepeatException {
        return renderIrSignal(actualVars, (int) IrpUtils.all);
    }

    public IrSignal renderIrSignal(Map<String, Long>actualVars, boolean considerRepeatMins) throws DomainViolationException, UnassignedException, IncompatibleArgumentException, InvalidRepeatException {
        return renderIrSignal(actualVars, (int) IrpUtils.all, considerRepeatMins);
    }

    private static void assignIfValid(Map<String, Long> actualVars, String name, long value) {
        if (value != IrpUtils.invalid)
            actualVars.put(name, value);
    }
    public IrSignal renderIrSignal(long device, long subdevice, long function) throws DomainViolationException, UnassignedException, IncompatibleArgumentException, InvalidRepeatException {
        return renderIrSignal(device, subdevice, function, -1L);
    }

    public IrSignal renderIrSignal(long device, long subdevice, long function, long toggle) throws DomainViolationException, UnassignedException, IncompatibleArgumentException, InvalidRepeatException {
        Map<String, Long> actualVars = new HashMap<>(3);
        assignIfValid(actualVars, "D", device);
        assignIfValid(actualVars, "S", subdevice);
        assignIfValid(actualVars, "F", function);
        assignIfValid(actualVars, "T", toggle);
        return renderIrSignal(actualVars);
    }

    public IrSignal renderIrSignal(int device, int subdevice, int function) throws DomainViolationException, UnassignedException, IncompatibleArgumentException, InvalidRepeatException {
        return renderIrSignal((long) device, (long) subdevice, (long) function);
    }

    public IrSequence tryRender(Map<String, Long> ivs, int pass, boolean considerRepeatMins, boolean initialize) {
        boolean success = false;
        IrSequence irSequence = null;

        try {
            irSequence = render(ivs, pass, considerRepeatMins, initialize);
        } catch (IrpMasterException ex) {
            System.err.println(ex.getMessage());
        }
        return irSequence;
    }

    public IrSequence tryRender(Map<String, Long> ivs, int pass, boolean considerRepeatMins) {
        return tryRender(ivs, pass, considerRepeatMins, this.virgin);
    }

     /**
     * Returns a parameter Map&lt;String, Long&gt; suitable for using as argument to renderIRSignal by evaluating the arguments.
     * @param additionalParams String of assignments like a=12 b=34 c=56
     * @return tests- irpmaster?Map&lt;String, Long&gt; for using as argument to renderIrSignal
     */
    public static Map<String, Long> parseParams(String additionalParams) {
        Map<String, Long> params = new HashMap<>();
        String[] arr = additionalParams.split("[,=\\s;]+");
        //for (int i = 0; i < arr.length; i++)
        //    System.out.println(arr[i]);
        for (int i = 0; i < arr.length/2; i++)
            params.put(arr[2*i], IrpUtils.parseLong(arr[2*i+1], false));

        return params;
    }

    /**
     * Returns a parameter tests- irpmaster?Map&lt;String, Long&gt; suitable for using as argument to renderIrSignal by evaluating the arguments.
     * The four first parameters overwrite the parameters in the additional parameters, if in conflict.
     * @param D device number. Use -1 for not assigned.
     * @param S subdevice number. Use -1 for not assigned.
     * @param F function number (obc, command number). Use -1 for not assigned.
     * @param T toggle. Use -1 for not assigned.
     * @param additionalParams String of assignments like a=12 b=34 c=56
     * @return Map&lt;String, Long&gt; for using as argument to renderIrSignal
     */
    public static Map<String, Long> parseParams(int D, int S, int F, int T, String additionalParams) {
        Map<String, Long> params = parseParams(additionalParams);
        assignIfValid(params, "D", (long) D);
        assignIfValid(params, "S", (long) S);
        assignIfValid(params, "F", (long) F);
        assignIfValid(params, "T", (long) T);
        return params;
    }

    /**
     * "Smart" method for decoding parameters. If the first argument contains the character "=",
     * the arguments are assume to be assignments using the "=". Otherwise,
     *
     * <ul>
     * <li>one argument is supposed to be F
     * <li>two argumente are supposed to be D and F
     * <li>three arguments are supposed to be D, S, and F
     * <li>four arguments are supposed to be D, S, F, and T.
     * </ul>
     *
     * @param args String array of parameters
     * @param skip Number of elements in the args to skip
     * @return parameter Map&lt;String, Long&gt; suitable for rendering signals
     * @throws IncompatibleArgumentException
     */
    public static Map<String, Long> parseParams(String[] args, int skip) throws IncompatibleArgumentException {
        return args[skip].contains("=")
                ? parseNamedProtocolArgs(args, skip)
                : parsePositionalProtocolArgs(args, skip);
    }

    private static Map<String, Long> parseNamedProtocolArgs(String[] args, int skip) throws IncompatibleArgumentException {
        Map<String, Long> params = new HashMap<>();
            for (int i = skip; i < args.length; i++) {
                String[] str = args[i].split("=");
                if (str.length != 2)
                    throw new IncompatibleArgumentException("`" + args[i] + "' is not a parameter assignment");
                String name = str[0].trim();
                long value = Long.parseLong(str[1]);
                params.put(name, value);
            }
            return params;
    }

    private static Map<String, Long> parsePositionalProtocolArgs(String[] args, int skip) throws IncompatibleArgumentException {
        Map<String, Long> params = new LinkedHashMap<>();
        int index = skip;
        switch (args.length - skip) {
            case 4:
                params.put("D", Long.parseLong(args[index++]));
                params.put("S", Long.parseLong(args[index++]));
                params.put("F", Long.parseLong(args[index++]));
                params.put("T", Long.parseLong(args[index]));
                break;
            case 3:
                params.put("D", Long.parseLong(args[index++]));
                params.put("S", Long.parseLong(args[index++]));
                params.put("F", Long.parseLong(args[index]));
                break;
            case 2:
                params.put("D", Long.parseLong(args[index++]));
                params.put("F", Long.parseLong(args[index]));
                break;
            case 1:
                params.put("F", Long.parseLong(args[index]));
                break;
            case 0:
                break;
            default:
                throw new IncompatibleArgumentException("Too many parameters");
        }
        return params;
    }

    // There is no need for a main routine here, the one in IrpMaster is universal enough.
}
