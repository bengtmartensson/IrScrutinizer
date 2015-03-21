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

import org.antlr.runtime.ANTLRStringStream;
import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.RecognitionException;
import org.antlr.runtime.tree.CommonTree;

/**
 * This class ...
 *
 * @author Bengt Martensson
 */
public class ParameterSpec {

    private String name;
    private long min;
    private long max;
    private CommonTree deflt;
    private boolean memory = false;

    @Override
    public String toString() {
        return name + (memory ? "@" : "") + ":" + min + ".." + max + (deflt != null ? ("=" + deflt.toStringTree()) : "");
    }

    public ParameterSpec(String name, int min, int max, boolean memory, int deflt) {
        this(name, min, max, memory, IrpParser.newIntegerTree(deflt));
    }

    public ParameterSpec(String name, int min, int max) {
        this(name, min, max, false);
    }

    public ParameterSpec(String name, int min, int max, boolean memory, CommonTree deflt) {
        this.name = name;
        this.min = min;
        this.max = max;
        this.memory = memory;
        this.deflt = deflt;
    }

    public ParameterSpec(String name, int min, int max, boolean memory) {
        this(name, min, max, memory, (CommonTree) null);
    }

    public ParameterSpec(String name, int min, int max, boolean memory, String bare_expression) throws ParseException {
        this.name = name;
        this.min = min;
        this.max = max;
        this.memory = memory;
        IrpLexer lex = new IrpLexer(new ANTLRStringStream(bare_expression));
        CommonTokenStream tokens = new CommonTokenStream(lex);
        IrpParser parser = new IrpParser(tokens);
        IrpParser.bare_expression_return r;
        try {
            r = parser.bare_expression();
            CommonTree ct = (CommonTree) r.getTree();
            deflt = ct;
        } catch (RecognitionException ex) {
            throw new ParseException(ex);
        }
    }

    public ParameterSpec(CommonTree t) {
        load(t);
    }

    public ParameterSpec(String parameter_spec) throws ParseException {
        IrpLexer lex = new IrpLexer(new ANTLRStringStream(parameter_spec));
        CommonTokenStream tokens = new CommonTokenStream(lex);
        IrpParser parser = new IrpParser(tokens);
        IrpParser.parameter_spec_return r;
        try {
            r = parser.parameter_spec();
            CommonTree ct = (CommonTree) r.getTree();
            load(ct);
        } catch (RecognitionException ex) {
            throw new ParseException(ex);
        }
    }

    private void load(CommonTree t) {
        memory = t.getText().equals("PARAMETER_SPEC_MEMORY");
        name = t.getChild(0).getText();
        min = Long.parseLong(t.getChild(1).getText());
        max = Long.parseLong(t.getChild(2).getText());
        deflt = t.getChildCount() >= 4 ? (CommonTree) t.getChild(3) : null;
    }

    public boolean isOK(long x) {
        return min <= x && x <= max;
    }

    public String domainAsString() {
        return min + ".." + max;
    }

    public String getName() {
        return name;
    }

    public CommonTree getDefault() {
        return deflt;
    }

    public long getMin() {
        return min;
    }

    public long getMax() {
        return max;
    }

    public boolean hasMemory() {
        return memory;
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        ParameterSpec dev = null;
        ParameterSpec toggle = null;
        ParameterSpec func = null;
        try {
            dev = new ParameterSpec("d", 0, 255, false, "255-s");
            toggle = new ParameterSpec("t", 0, 1, true, 0);
            func = new ParameterSpec("F", 0, 1, false, 0);
            System.out.println(new ParameterSpec("Fx", 0, 1, false, 0));
            System.out.println(new ParameterSpec("Fx", 0, 1, false));
            System.out.println(new ParameterSpec("Fx", 0, 1));
            System.out.println(new ParameterSpec("D:0..31"));
            System.out.println(new ParameterSpec("D@:0..31=42"));
            System.out.println(new ParameterSpec("D:0..31=42*3+33"));
            System.out.println(dev);
            System.out.println(toggle);
            System.out.println(func);
            System.out.println(dev.isOK(-1));
            System.out.println(dev.isOK(0));
            System.out.println(dev.isOK(255));
            System.out.println(dev.isOK(256));
        } catch (ParseException ex) {
            System.out.println(ex.getMessage());
        }
    }
}
