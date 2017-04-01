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

import java.io.Serializable;
import org.antlr.runtime.ANTLRStringStream;
import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.RecognitionException;
import org.antlr.runtime.tree.CommonTree;

/**
 * This class implements GeneralSpec as given in Chapter 2 of Dixon: "Specification of IRP Notation", second draft.
 * This class is immutable; can only be build by the constructor, and not altered.
 *
 */
public class GeneralSpec implements Serializable {

    public final static double defaultDutyCycle = IrpUtils.invalid;
    public final static BitDirection defaultBitDirection = BitDirection.lsb;
    public final static double defaultUnit = 1;
    private static CommonTree toAST(String str) {
        IrpLexer lex = new IrpLexer(new ANTLRStringStream(str));
        CommonTokenStream tokens = new CommonTokenStream(lex);
        IrpParser parser = new IrpParser(tokens);
        IrpParser.generalspec_return r;
        CommonTree AST = null;
        try {
            r = parser.generalspec();
            AST = r.getTree();
        } catch (RecognitionException ex) {
            System.err.println(ex.getMessage());
        }
        return AST;
    }

    private static GeneralSpec newGeneralSpec(CommonTree AST) {
        GeneralSpec generalSpec = null;
        try {
            generalSpec = ASTTraverser.generalspec(AST);
        } catch (UnassignedException ex) {
            assert false; //this cannot happen
        }
        return generalSpec;
    }

    private static void test(String str) {
        GeneralSpec gs = new GeneralSpec(str);
        System.out.println(toAST(str).toStringTree());
        System.out.println(gs);
    }

    /**
     * Just for testing and debugging.
     *
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        if (args.length > 0) {
            test(args[0]);
        } else {
            //test("{0k,,10p}"); // Thows error
            //test("{ }"); // Seem to trigger bug in ANTLR
            test("{38.4k,564}");
            test("{564,38.4k}");
            test("{msb, 889u}");
            test("{42%, 10p,msb,40k}");
            test("{msb ,40k , 33.33333% ,10p }");
            test("{msb, 123u, 100k, 10p, 1000k}");
        }
    }

    /** Carrier frequency in Hz */
    private double frequency = IrpUtils.defaultFrequency;

    /** Duty cycle in percent. IrpUtils.invalid (-1) is defined to denote "don't care". */
    private double dutyCycle = defaultDutyCycle;


    /** BitDirection */
    private BitDirection bitDirection = defaultBitDirection;


    /** Timing unit in us */
    private double unit = 1;


    /** Timing unit in pulses, if and only if given by the user.*/
    private double unit_pulses = IrpUtils.invalid;


    /**
     * This constructor is intended for debugging and testing only.
     *
     * @param bitDirection
     * @param unit
     * @param unit_pulses
     * @param frequency
     * @param dutyCycle
     */
    public GeneralSpec(BitDirection bitDirection, double unit, double unit_pulses, double frequency, double dutyCycle) {
        this.bitDirection = bitDirection;
        this.unit = unit;
        this.unit_pulses = unit_pulses;
        this.frequency = frequency;
        this.dutyCycle = dutyCycle;
        updateUnit();
    }

    private GeneralSpec(GeneralSpec src) {
        this.bitDirection = src.bitDirection;
        this.unit = src.unit;
        this.unit_pulses = src.unit_pulses;
        this.frequency = src.frequency;
        this.dutyCycle = src.dutyCycle;
        updateUnit();
    }

    /** This constructor is intended for debugging and testing only */
    public GeneralSpec() {
        this(defaultBitDirection, defaultUnit, -1, IrpUtils.defaultFrequency, defaultDutyCycle);
    }


    public GeneralSpec(CommonTree tree) {
        this(newGeneralSpec(tree));
    }

    public GeneralSpec(String str) {
        this(newGeneralSpec(toAST(str)));
    }
    //private CommonTree AST;

    @Override
    public String toString() {
        return "Frequency = " + frequency + "Hz, unit = " + unit + "us, " + bitDirection + (dutyCycle > 0 ? (", Duty cycle = " + dutyCycle + "%.") : ", Duty cycle: -.");
    }
    private void updateUnit() {
        if (unit_pulses != IrpUtils.invalid) {
            if (frequency == 0)
                throw new ArithmeticException("Units in p and frequency == 0 do not go together.");
            unit = unit_pulses*(1000000.0/frequency);
        }
    }

    BitDirection getBitDirection() {
        return bitDirection;
    }

    public double getFrequency() {
        return frequency;
    }

    public double getUnit() {
        return unit;
    }

    public double getDutyCycle() {
        return dutyCycle;
    }

}
