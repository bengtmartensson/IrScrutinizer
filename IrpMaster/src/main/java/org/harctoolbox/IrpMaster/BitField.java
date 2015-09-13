/*
Copyright (C) 2011, 2012, 2013 Bengt Martensson.

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
 * This class implements Bitfields as described in Chapter 5, except for that it does not
 * know how to transform itself into a bitstream -- that is what the class BitStream does.
 * Accordingly, it does not know bit directions.
 *
 * @see BitStream
 */
public class BitField extends PrimaryIrStreamItem {

    /**
     * Max length of a BitField in this implementation.
     */
    public static final int maxWidth = Long.SIZE - 1; // = 63

    private boolean complement;
    boolean reverse;
    boolean infinite;
    long data;
    int width = maxWidth;
    int skip = 0;
    long value;

    //private long evaluatePrimaryItem(String s, long deflt) {
    //    return (s == null || s.isEmpty()) ? deflt : Long.parseLong(s);
    //}

    public BitField(Protocol p, boolean complement, boolean reverse, boolean infinite, long data, long width, long skip) throws DomainViolationException {
        super(p);
        if (width > maxWidth)
            throw new DomainViolationException("Max width of bitfields (= " + maxWidth + ") exceeded.");
        if (width < 0)
            throw new DomainViolationException("Width of bitfield must be nonnegative.");
        if (skip > maxWidth)
            throw new DomainViolationException("Max skip value in bitfields (= " + maxWidth + ") exceeded.");
        if (skip < 0)
            throw new DomainViolationException("Skip value of bitfield must be nonnegative.");

        this.complement = complement;
        this.reverse = reverse;
        this.infinite = infinite;
        this.data = data;
        this.width = infinite ? maxWidth : (int) width;
        this.skip = (int) skip;
        Debug.debugBitFields("new Bitfield: " + toString());
        compute();
    }

    /**
     * Mainly for debugging and testing
     *
     * @param env
     * @param str
     * @param debug
     * @return BitField
     */
    public static BitField newBitfield(Protocol env, String str, boolean debug) {
        IrpLexer lex = new IrpLexer(new ANTLRStringStream(str));
        CommonTokenStream tokens = new CommonTokenStream(lex);
        IrpParser parser = new IrpParser(tokens);
        IrpParser.bitfield_return r;
        try {
            r = parser.bitfield();
            CommonTree AST = (CommonTree) r.getTree();
            if (debug)
                System.out.println(AST.toStringTree());
            return ASTTraverser.bitfield(env, AST);
        } catch (RecognitionException | UnassignedException | DomainViolationException ex) {
            System.err.println(ex.getMessage());
        }
        return null;
    }

    @Override
    public final String toString() {
        return (complement ? "~" : "") + data + ":" + (reverse ? "-" : "") + (infinite ? "" : width) + ":" + skip;
    }

    private void compute() {
        long x = data >> skip;
        if (complement)
            x = ~x;
        x &= ((1L << width) - 1L);
        if (reverse) {
            x = IrpUtils.reverse(x, width);
        }
        value = x;
        //lsb_value = environment.getBitDirection() == BitDirection.msb ? IrpUtils.reverse(value, width) : value;
    }

    public String evaluateAsString() {
        String padding = value >= 0
                ? "0000000000000000000000000000000000000000000000000000000000000000"
                : "1111111111111111111111111111111111111111111111111111111111111111";
        String s = Long.toBinaryString(value);
        if (s.length() > width)
            s = s.substring(s.length() - width);
        else if (s.length() < width)
            s = padding.substring(0, width - s.length()) + s;
        return s;
    }

    public long toLong() {
        return value;
    }

    public long evaluate() {
        return value;
    }

    public int getWidth() {
        return width;
    }

    public boolean isInfinite() {
        return infinite;
    }

    private static void usage(int code) {
        System.out.println("Usage:");
        System.out.println("\tBitfield [-d]? <bitfield> [<name>=<value>|{<name>=<value>}]*");
        System.exit(code);
    }

    public static void main(String[] args) {
        boolean debug = false;
        if (args.length == 0)
            usage(IrpUtils.exitUsageError);
        int arg_i = 0;
        if (args[0].equals("-d")) {
            debug = true;
            arg_i++;
        }
        Protocol prot = new Protocol(new GeneralSpec());
        String bitfield = null;
        try {
            bitfield = args[arg_i].trim();
            prot.assign(args, arg_i+1);
        } catch (IncompatibleArgumentException ex) {
            System.err.println(ex.getMessage());
            usage(IrpUtils.exitFatalProgramFailure);
        } catch (ArrayIndexOutOfBoundsException ex) {
            usage(IrpUtils.exitUsageError);
        }
        BitField bf = newBitfield(prot, bitfield, debug);
        if (bf != null)
            System.out.println(bf.toString() + "\tint=" + bf.toLong() + "\tstring=" + bf.evaluateAsString());
    }

    @Override
    public boolean isEmpty() {
        return width == 0;
    }

    @Override
    public ArrayList<PrimitiveIrStreamItem> evaluate(BitSpec bitSpec) throws IncompatibleArgumentException, UnassignedException {
        throw new UnsupportedOperationException("This cannot happen.");
    }
}
