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

import java.util.ArrayList;
import org.antlr.runtime.ANTLRStringStream;
import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.RecognitionException;
import org.antlr.runtime.tree.CommonTree;

/**
 * This class implements Durations in Chapter 3 and 4.
 * Depends on its Protocol (GeneralSpec and NameEngine), except for this, it is immutable.
 */

public class Duration extends PrimitiveIrStreamItem {

    private DurationType durationType;

    private double us = IrpUtils.invalid;
    private double time_periods = IrpUtils.invalid;
    private double time_units = IrpUtils.invalid;

    public static Duration newDuration(Protocol env, double time, String unit, DurationType durationType) {
        return durationType == DurationType.extent ? new Extent(env, time, unit) : new Duration(env, time, unit, durationType);
    }

    public Duration(Protocol env, double time, DurationType dt) {
        this(env, time, "u", dt);
    }

    public Duration(Protocol env, double time, String unit, DurationType dt) {
        super(env);
        //if (time == 0)
        //    UserComm.warning("Duration of 0 detected. This is normally not sensible.");
        durationType = dt;
        if (unit == null || unit.isEmpty())
            time_units = time;
        else if (unit.equals("p"))
            time_periods = time;
        else if (unit.equals("m"))
            us = 1000 * time;
        else if (unit.equals("u"))
            us = time;
    }

    public double evaluate_sign(double elapsed) throws ArithmeticException, IncompatibleArgumentException {
        return (durationType == DurationType.flash) ? evaluate(elapsed) : -evaluate(elapsed);
    }

    public double evaluate(double elapsed) throws ArithmeticException, IncompatibleArgumentException {
        if (time_periods != IrpUtils.invalid) {
            if (environment.getFrequency() > 0) {
                return 1000000.0*time_periods/environment.getFrequency();
            } else {
                throw new ArithmeticException("Units in p and frequency == 0 do not go together.");
            }
        } else if (time_units != IrpUtils.invalid) {
            if (environment.getUnit() > 0) {
                return time_units * environment.getUnit();
            } else {
                throw new ArithmeticException("Relative units and unit == 0 do not go together.");
            }
        } else {
            return us;
        }
    }

    /** Returns a new Duration instance by invoking the parser on the second argument.
     *
     * @param env Protocol, containing GeneralSpec and NameEngine
     * @param str String to be parsed
     * @return newly constructed Duration instance.
     */
    public static Duration newDuration(Protocol env, String str) {
        IrpLexer lex = new IrpLexer(new ANTLRStringStream(str));
        CommonTokenStream tokens = new CommonTokenStream(lex);
        IrpParser parser = new IrpParser(tokens);
        IrpParser.duration_return r;
        try {
            r = parser.duration();
            CommonTree AST = (CommonTree) r.getTree();
            return ASTTraverser.duration(env, AST);
        } catch (RecognitionException | UnassignedException | DomainViolationException ex) {
            System.err.println(ex.getMessage());
        }
        return null;
    }

    public DurationType getDurationType() {
        return durationType;
    }

    @Override
    public ArrayList<PrimitiveIrStreamItem> evaluate(BitSpec bitSpec) {
        debugBegin();
        ArrayList<PrimitiveIrStreamItem> list = new ArrayList<PrimitiveIrStreamItem>(1);
        list.add(this);
        return list;
    }

    @Override
    public String toString() {
        return durationType + ":" + (us != IrpUtils.invalid ? (us + "u") : time_periods != IrpUtils.invalid  ? (time_periods + "p") : (this.time_units + "u"));
    }

    private static void test(Protocol protocol, String str) throws ArithmeticException, IncompatibleArgumentException {
        Duration d = newDuration(protocol, str);
        System.out.println(d + "\t" + Math.round(d.evaluate_sign(0.0)));
    }

    private static void test(String str) throws ArithmeticException, IncompatibleArgumentException {
        test(new Protocol(), str);
    }

    private static void test(String gs, String str) throws ArithmeticException, IncompatibleArgumentException {
        test(new Protocol(new GeneralSpec(gs)), str);
    }

    private static void usage(int code) {
        System.out.println("Usage:");
        System.out.println("\tDuration [<generalSpec>] <duration> [<variable>=<value>]*");
        System.exit(code);
    }

    public static void main(String[] args) {
        try {
            switch (args.length) {
                case 0:
                    usage(IrpUtils.exitUsageError);
                    break;
                case 1:
                    test(args[0]);
                    break;
                case 2:
                    test(args[0], args[1]);
                    break;
                default:
                    Protocol prot = new Protocol(new GeneralSpec(args[0]));
                    try {
                        prot.assign(args, 2);
                    } catch (IncompatibleArgumentException ex) {
                        System.err.println(ex.getMessage());
                        usage(IrpUtils.exitFatalProgramFailure);
                    }
                    test(prot, args[1]);
            }
        } catch (ArithmeticException | IncompatibleArgumentException ex) {
            System.err.println(ex.getMessage());
        }
    }

    @Override
    public boolean isEmpty() throws IncompatibleArgumentException {
        return evaluate(0.0) == 0;
    }
}
