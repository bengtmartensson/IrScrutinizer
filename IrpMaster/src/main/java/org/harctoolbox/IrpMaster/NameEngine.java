/*
Copyright (C) 2011, 2012 Bengt Martensson.

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

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import org.antlr.runtime.ANTLRStringStream;
import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.RecognitionException;
import org.antlr.runtime.tree.CommonTree;

/**
 * Implementation of Definitions in Chapter 10 and Assignments in Chapter 11; these are not independent objects.
 *
 */

// TODO: There are probably too many accessing functions here.
// Clean up by eliminating and making private.

public class NameEngine implements Serializable {

    private static void usage(int code) {
        System.err.println("Usage:");
        System.err.println("\tNameEngine [<name>=<value>|{<name>=<expression>}]+");
        IrpUtils.exit(code);
    }

    /**
     * Just for testing purposes.
     *
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        if (args.length == 0)
            usage(IrpUtils.exitUsageError);
        Protocol prot = new Protocol(new GeneralSpec());
        try {
            prot.assign(args, 0);
        } catch (IncompatibleArgumentException ex) {
            System.err.println(ex.getMessage());
            usage(IrpUtils.exitFatalProgramFailure);
        }
        System.out.println(prot.nameEngineString());
    }

    private final transient Map<String, CommonTree> map;

    public NameEngine() {
        map = new HashMap<>(4);
    }

    public CommonTree get(String name) {
        Debug.debugNameEngine("NameEngine: " + name + (map.containsKey(name) ? (" = " + map.get(name).toStringTree()) : "-"));
        return map.get(name);
    }

    public void parseDefines(String str) throws ParseException {
        String s[] = str.replaceAll("[{}]", "").split(",");
        for (String item : s)
            define(item);

    }

    private void define(String str) throws ParseException {
        String[] s = str.split("=");
        define(s[0], s[1]);
    }

    private void define(String name, CommonTree t) {
        map.put(name.trim(), t);
    }

    private void define(CommonTree t /* DEFINITION */) {
        define(t.getChild(0).getText(), (CommonTree) t.getChild(1));
    }

    public void readDefinitions(CommonTree t /* DEFINITIONS */) {
        for (int i = 0; i < t.getChildCount(); i++)
            define((CommonTree)t.getChild(i));
    }

    /**
     * Invoke the parser on the supplied argument, and stuff the result into the name engine.
     *
     * @param str String to be parsed, like "{C = F*4 + D + 3}".
     */
    public void readDefinitions(String str) {
        IrpLexer lex = new IrpLexer(new ANTLRStringStream(str));
        CommonTokenStream tokens = new CommonTokenStream(lex);
        IrpParser parser = new IrpParser(tokens);
        IrpParser.definitions_return r;
        try {
            r = parser.definitions();
            CommonTree AST = r.getTree();
            //System.out.println(AST.toStringTree());
            readDefinitions(AST);
        } catch (RecognitionException ex) {
            System.err.println(ex.getMessage());
        }
    }

    // FIXME: should not enter anything if the bare_expression does not parse,
    public void define(String name, String bare_expression) throws ParseException {
        IrpLexer lex = new IrpLexer(new ANTLRStringStream(bare_expression));
        CommonTokenStream tokens = new CommonTokenStream(lex);
        IrpParser parser = new IrpParser(tokens);
        IrpParser.bare_expression_return r;
        try {
            r = parser.bare_expression();
            map.put(name.trim(), r.getTree());
        } catch (RecognitionException ex) {
            throw new ParseException(ex);
        }
    }

    public void assign(String name, long value) {
        define(name, IrpParser.newIntegerTree(value));
    }

    /**
     * Set names according to the content of the default values supplies in the first argument.
     *
     * @param parameterSpecs from where the default values are taken
     * @param initial If false, Parameters with memory (state variables) are not reset.
     */
    public void loadDefaults(ParameterSpecs parameterSpecs, boolean initial) {
        parameterSpecs.getParams().stream().filter((param) -> ((initial || ! param.hasMemory()) && param.getDefault() != null)).forEachOrdered((param) -> {
            //System.out.println(">>>>>" + param.getName());
            map.put(param.getName(), param.getDefault());
        });
    }

    public void loadActualParameters(Map<String, Long> ivs, ParameterSpecs paramSpecs) throws DomainViolationException {
        for (Entry<String, Long> kvp : ivs.entrySet()) {
            String name = kvp.getKey();
            // if no Parameter Specs, do not annoy the user; he has been warned already.
            if (!paramSpecs.isEmpty()) {
                ParameterSpec ps = paramSpecs.getParameterSpec(name);
                if (ps == null) {
                    UserComm.warning("Parameter `" + name + "' unknown in ParameterSpecs.");
                } else if (!ps.isOK(kvp.getValue())) {
                    throw new DomainViolationException("Parameter " + name + " = " + kvp.getValue() + " outside of allowed domain (" + ps.domainAsString() + ").");
                }
            }
            assign(name, kvp.getValue());
        }
    }

    public void checkAssignments(ParameterSpecs paramSpecs) throws UnassignedException {
        for (String name : paramSpecs.getNames()) {
            if (!map.containsKey(name)) {
                throw new UnassignedException("Parameter `" + name + "' has not been assigned.");
            }
        }
    }

    /**
     *
     * @param name Input name
     * @return StringTree of the value.
     * @throws UnassignedException
     */
    public String evaluate(String name) throws UnassignedException {
        if (map.containsKey(name))
            return map.get(name).toStringTree();
        else
            throw new UnassignedException("Name `" + name + "' not defined.");
    }

    public boolean containsKey(String name) {
        return map.containsKey(name);
    }

    public String tryEvaluate(String name) {
        String result = null;
        try {
            result = evaluate(name);
        } catch (UnassignedException ex) {
            System.err.println(ex.getMessage());
        }
        return result;
    }

    @Override
    public String toString() {
        StringBuilder str = new StringBuilder(64);
        map.keySet().forEach((name) -> {
            str.append(name).append("=").append(map.get(name).toStringTree()).append(",");
        });
        return "{" + (str.length() == 0 ? "" : str.substring(0, str.length()-1)) + "}";
    }

    /**
     * Creates consisting of parameter values that can be used as part of filenames etc.
     * Roughly, is a "pretty" variant of toString().
     *
     * @param equals String between name and value, often "=",
     * @param separator String between name-value pairs, often ",".
     * @return String
     */
    public String notationString(String equals, String separator) {
        StringBuilder str = new StringBuilder(64);
        map.keySet().stream().filter((name) -> (!name.startsWith("$") && !map.get(name).toStringTree().startsWith("("))).forEachOrdered((name) -> {
            str.append(name).append(equals).append(map.get(name).toStringTree()).append(separator);
        });
        return (str.length() == 0 ? "" : str.substring(0, str.length()-1));
    }

}
