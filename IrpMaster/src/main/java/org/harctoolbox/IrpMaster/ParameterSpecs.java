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

import java.util.Collection;
import java.util.HashMap;
import java.util.Set;
import org.antlr.runtime.ANTLRStringStream;
import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.RecognitionException;
import org.antlr.runtime.tree.CommonTree;

/**
 * This class ...
 *
 * @author Bengt Martensson
 */
public class ParameterSpecs {

    private HashMap<String, ParameterSpec>map;

    public boolean isEmpty() {
        return map.isEmpty();
    }

    public Set<String> getNames() {
        return map.keySet();
    }

    public Collection<ParameterSpec> getParams() {
        return map.values();
    }

    public ParameterSpec getParameterSpec(String name) {
        return map.get(name);
    }

    public ParameterSpecs() {
        map = new HashMap<>();
    }

    public ParameterSpecs(String parameter_specs) throws ParseException {
        this();
        IrpLexer lex = new IrpLexer(new ANTLRStringStream(parameter_specs));
        CommonTokenStream tokens = new CommonTokenStream(lex);
        IrpParser parser = new IrpParser(tokens);
        IrpParser.parameter_specs_return r;
        try {
            r = parser.parameter_specs();
            CommonTree ct = (CommonTree) r.getTree();
            load(ct);
        } catch (RecognitionException ex) {
            throw new ParseException(ex);
        }
    }

    public ParameterSpecs(CommonTree t) {
        this();
        load(t);
    }

    private void load(CommonTree t) {
        for (int i = 0; i < t.getChildCount(); i++) {
            ParameterSpec ps = new ParameterSpec((CommonTree)t.getChild(i));
            map.put(ps.getName(), ps);
        }
    }

    @Override
    public String toString() {
        StringBuilder str = new StringBuilder();
        for (ParameterSpec ps : map.values())
            str.append(ps.toString()).append(",");

        if (str.length() > 0)
            str.deleteCharAt(str.length()-1);
        return "[" + str.toString() + "]";
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        try {
            System.out.println(new ParameterSpecs("[T@:0..1=0,D:0..31,F:0..128,S:0..255=D-255]"));
        } catch (ParseException ex) {
            System.err.println(ex.getMessage());
        }
    }
}
