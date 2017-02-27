/*
Copyright (C) 2011,2012 Bengt Martensson.

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
 * This class corresponds to Chapter 9.
 * An expression is evaluated during execution time with the current name bindings.
 *
 * @author Bengt Martensson
 */
public class Expression {

    private static boolean debug;

    public static long evaluate(Protocol env, String str) throws UnassignedException, DomainViolationException {
        IrpLexer lex = new IrpLexer(new ANTLRStringStream(str));
        CommonTokenStream tokens = new CommonTokenStream(lex);
        IrpParser parser = new IrpParser(tokens);
        IrpParser.expression_return r;
        try {
            r = parser.expression();
            CommonTree AST = r.getTree();
            if (debug)
                System.out.println(AST.toStringTree());
            return evaluate(env, AST);
        } catch (RecognitionException ex) {
            System.err.println(ex.getMessage());
        }
        return 0L;
    }

    public static long evaluate(Protocol env, CommonTree AST) throws UnassignedException, DomainViolationException {
        return ASTTraverser.expression(env, AST);
    }

    private static void usage(int code) {
        System.out.println("Usage:");
        System.out.println("\tExpression [-d]? <expression> [<name>=<value>|{<name>=<expression>}]*");
        System.exit(code);
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        if (args.length == 0)
            usage(IrpUtils.exitUsageError);
        int arg_i = 0;
        if (args[0].equals("-d")) {
            debug = true;
            arg_i++;
        }

        Protocol prot = new Protocol(new GeneralSpec());
        try {
            String expression = args[arg_i].trim();
            prot.assign(args, arg_i+1);
            if ((expression.charAt(0) != '(') || (expression.charAt(expression.length()-1) != ')'))
                expression = '(' + expression + ')';
            System.out.println(evaluate(prot, expression));
        } catch (IncompatibleArgumentException ex) {
            System.err.println(ex.getMessage());
            usage(IrpUtils.exitUsageError);
        } catch (ArrayIndexOutOfBoundsException ex) {
            usage(IrpUtils.exitUsageError);
        } catch (UnassignedException | DomainViolationException ex) {
            System.err.println(ex.getMessage());
        }
    }
}
