/*
Copyright (C) 2011, 2013 Bengt Martensson.

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
import org.antlr.runtime.tree.CommonTree;

/**
 * This class traverses the AST (abstract syntax tree) built by the ANTLR parser.
 *
 * @author Bengt Martensson
 */
public class ASTTraverser {
    private final static int indentDepth = 3;

    private static String indent(int level) {
        return IrpUtils.spaces((level-1)*indentDepth);
    }

    // Some static convenience functions
    public static long expression(Protocol env, CommonTree tree) throws UnassignedException, DomainViolationException {
        ASTTraverser ast = new ASTTraverser(env);
        return ast.expression(tree, 1);
    }

    public static PrimaryIrStream bitspec_irstream(int pass, boolean considerRepeatMin, Protocol env, CommonTree tree) throws UnassignedException, InvalidRepeatException, DomainViolationException, IncompatibleArgumentException {
        ASTTraverser ast = new ASTTraverser(pass, considerRepeatMin, env);
        return ast.bitspec_irstream(tree, 1, false, null);
    }

    public static Duration duration(Protocol env, CommonTree tree) throws UnassignedException, DomainViolationException {
        ASTTraverser ast = new ASTTraverser(env);
        return ast.duration(tree, 1, true);
    }

    public static BitField bitfield(Protocol env, CommonTree tree) throws UnassignedException, DomainViolationException {
        ASTTraverser ast = new ASTTraverser(env);
        return ast.bitfield(tree, 1, true);
    }

    public static GeneralSpec generalspec(CommonTree tree) throws UnassignedException {
        ASTTraverser ast = new ASTTraverser(null);
        return ast.generalspec(tree, 1);
    }

    private Protocol env;
    private int state;
    private int pass;
    //private boolean skip; // Like in Execution model
    //private boolean currentStateRepeating;
    //private boolean hasVariations;
    private boolean considerRepeatMins;

    public ASTTraverser(int pass, boolean considerRepeatMins, Protocol env) {
        this.env = env;
        this.pass = pass;
        state = 0;
        this.considerRepeatMins = considerRepeatMins;
    }

    public ASTTraverser(Protocol env) {
        this(0, false, env);
    }

    public GeneralSpec generalspec(CommonTree tree, int level) {
        double frequency = IrpUtils.defaultFrequency;
        double dutyCycle = GeneralSpec.defaultDutyCycle;
        BitDirection bitDirection = GeneralSpec.defaultBitDirection;
        double unit = GeneralSpec.defaultUnit;
        double unit_pulses = IrpUtils.invalid;
        for (int i = 0; i < tree.getChildCount(); i++) {
            CommonTree child = (CommonTree) tree.getChild(i);
            if (child.getText().equals("FREQUENCY")) {
                if (child.getChild(0).getText().equals("FLOAT")) {
                    CommonTree val = (CommonTree) child.getChild(0);
                    frequency = 1000*Double.parseDouble(val.getChild(0).getText() + "." + val.getChild(1).getText());
                } else
                    frequency = 1000*Integer.parseInt(child.getChild(0).getText());
            } else if (child.getText().equals("DUTYCYCLE")) {
                if (child.getChild(0).getText().equals("FLOAT")) {
                    CommonTree val = (CommonTree) child.getChild(0);
                    dutyCycle = Double.parseDouble(val.getChild(0).getText() + "." + val.getChild(1).getText());
                } else
                    dutyCycle = Integer.parseInt(child.getChild(0).getText());
            } else if (child.getText().equals("UNIT")) {
                double number;
                if (child.getChild(0).getText().equals("FLOAT")) {
                    CommonTree val = (CommonTree) child.getChild(0);
                    number = Double.parseDouble(val.getChild(0).getText() + "." + val.getChild(1).getText());
                } else
                    number = Integer.parseInt(child.getChild(0).getText());

                if (child.getChildCount() == 1 || child.getChild(1).getText().equals("u"))
                    unit = number;
                else if (child.getChild(1).getText().equals("p"))
                    unit_pulses = number;
                else
                     throw new IllegalArgumentException("invalid postfix in unit.");
            } else if (child.getText().equals("BITDIRECTION")) {
                bitDirection = BitDirection.valueOf(child.getChild(0).getText());
            } else {
                throw new IllegalArgumentException("Parse error in GeneralSpec");
            }
        }
        return new GeneralSpec(bitDirection, unit, unit_pulses, frequency, dutyCycle);
    }

    public Duration duration(CommonTree tree, int level, boolean forceOk) throws UnassignedException, DomainViolationException {
        if (!passOk(forceOk, level))
            return null;

        nodeBegin(tree, level);
        String type = tree.getText();
        double time = name_or_number((CommonTree) tree.getChild(0), level+1);
        String unit = tree.getChildCount() == 2 ? ((CommonTree) tree.getChild(1)).getText() : "";
        Duration d = Duration.newDuration(env, time, unit, DurationType.valueOf(type.toLowerCase(IrpUtils.dumbLocale)));
        nodeEnd(tree, level, d);
        return d;
    }

    public BitField bitfield(CommonTree tree, int level, boolean forceOk) throws UnassignedException, DomainViolationException {
        if (!passOk(forceOk, level))
            return null;

        nodeBegin(tree, level);
        boolean complement = false;
        boolean reverse = false;
        boolean infinite = tree.getText().equals("INFINITE_BITFIELD");
        int offset = 0;
        if (((CommonTree)tree.getChild(offset)).getText().equals("~")) {
            complement = true;
            offset++;
        }
        if (((CommonTree)tree.getChild(offset)).getText().equals("-")
                && ((CommonTree)tree.getChild(offset)).getChildCount() == 0) {
            reverse = true;
            offset++;
        }
        long data = expression((CommonTree)tree.getChild(offset++), level+1);
        long width = infinite ? BitField.maxWidth : expression((CommonTree)tree.getChild(offset++), level+1);
        long skip = offset < tree.getChildCount() ? expression((CommonTree)tree.getChild(offset), level+1) : 0;
        BitField bitField = new BitField(env, complement, reverse, infinite, data, width, skip);
        nodeEnd(tree, level, bitField);
        return bitField;
    }

    public PrimaryIrStream irstream(CommonTree tree, int level, boolean forceOk, RepeatMarker parentRepeat) throws UnassignedException, InvalidRepeatException, DomainViolationException, IncompatibleArgumentException {
        nodeBegin(tree, level);

        PrimaryIrStream bareIrStream = null;// = BareIrStream.expression((CommonTree) tree.getChild(0), level+1);
        RepeatMarker repeatMarker = null;
        repeatMarker = tree.getChildCount() > 1
                ? repeatmarker((CommonTree) tree.getChild(1), level+1, null)
                : new RepeatMarker();
        if (parentRepeat != null && parentRepeat.isInfinite() && repeatMarker.isInfinite())
            throw new InvalidRepeatException("Hierachical repeats not implemented");
        int noAlternatives = 0;
        PrimaryIrStream irStream = null;

        if (repeatMarker.min >= 1) {
            bareIrStream = bare_irstream((CommonTree) tree.getChild(0), level + 1, forceOk, Pass.intro, repeatMarker);
            irStream = bareIrStream != null ? new PrimaryIrStream(env, bareIrStream, null, 0) : null;
            noAlternatives = bareIrStream.getNoAlternatives();
            if (repeatMarker.isInfinite() && ! this.considerRepeatMins)
                irStream = null; // Does not undo assignments...
            if (repeatMarker.max < 2 && noAlternatives > 1)
                UserComm.warning("Variations inside of IrSequence with repeat max < 2. Second and third alternative are ignored.");
            else if (noAlternatives > 0) {
                // We have an IrStream with Variation
                state++;
                env.assign("$state", state);
                if (repeatMarker.max < 3 && noAlternatives == 3) {
                    UserComm.warning("3-part Variations inside of IrSequence with repeat max < 3. Second alternative is ignored.");
                } else {
                    bareIrStream = bare_irstream((CommonTree) tree.getChild(0), level + 1, forceOk, Pass.repeat, repeatMarker);
                    if (irStream == null)
                        irStream = bareIrStream != null ? new PrimaryIrStream(env, bareIrStream, null, 0) : null;
                    else
                        irStream.concatenate(bareIrStream);
                }

                if (noAlternatives == 3) {
                    state++;
                    env.assign("$state", state);
                    bareIrStream = bare_irstream((CommonTree) tree.getChild(0), level + 1, forceOk, Pass.ending, repeatMarker);
                    if (irStream == null)
                        irStream = bareIrStream != null ? new PrimaryIrStream(env, bareIrStream, null, 0) : null;
                    else
                        irStream.concatenate(bareIrStream);
                }

            } else if (considerRepeatMins || !repeatMarker.isInfinite()) {
                // No Variation in this IrStream, traverse to reach repeatMarker.min
                for (int i = 0; i < repeatMarker.min - 1; i++) {
                    bareIrStream = bare_irstream((CommonTree) tree.getChild(0), level + 1, forceOk, Pass.intro, repeatMarker);
                    if (irStream == null)
                        irStream = bareIrStream != null ? new PrimaryIrStream(env, bareIrStream, null, 0) : null;
                    else
                        irStream.concatenate(bareIrStream);
                }
            }
        //}
        }

        if (noAlternatives == 0) {
            if (repeatMarker.isInfinite()) {
                state++;
                env.assign("$state", state);
                //currentStateRepeating = true;
            }
            if (state == pass && repeatMarker.isInfinite()) {
                bareIrStream = bare_irstream((CommonTree) tree.getChild(0), level + 1, forceOk, Pass.intro, repeatMarker);
                if (state == pass)
                    irStream = bareIrStream != null ? new PrimaryIrStream(env, bareIrStream, null, 0) : null;
                if (bareIrStream.getNoAlternatives() > 0)
                    throw new InvalidRepeatException("Invalid repeat: Variations enclosed in ( ... )* are not supported.");
            }

            if (repeatMarker.isInfinite()) {
                // I have just completed a repeating IrStream,
                // increment state, no matter if I delivered it or not,

                state++;
                env.assign("$state", state);
                //currentStateRepeating = false;
                Debug.debugASTParser("AST traverse state changed to " + state + ".");
            }
        }
        nodeEnd(tree, level, irStream);
        return irStream;
    }

    // Node type IRSTREAM, with children
    public PrimaryIrStream bare_irstream(CommonTree tree, int level, boolean forceOk, Pass variationAlternative, RepeatMarker repeatMarker) throws UnassignedException, InvalidRepeatException, DomainViolationException, IncompatibleArgumentException {
        nodeBegin(tree, level);
        // Intro pass
        int noAlternatives = 0;
        PrimaryIrStream bareIrStream = null;
        //skip = false; // local instead?
        ArrayList<PrimaryIrStreamItem> list = new ArrayList<>();
        for (int i = 0; i < tree.getChildCount(); i++) {
            PrimaryIrStreamItem irStreamItem = irstream_item((CommonTree) tree.getChild(i), level + 1, variationAlternative, repeatMarker, forceOk);
            if (irStreamItem != null && irStreamItem.getNoAlternatives() > 0)
                noAlternatives = irStreamItem.getNoAlternatives();
            if (irStreamItem != null && !irStreamItem.isEmpty())
                list.add(irStreamItem);
            if (noAlternatives > 0 && irStreamItem.isEmpty())
                // Empty alternative
                //skip = true;

                break;
        }
        bareIrStream = new PrimaryIrStream(env, list, null, noAlternatives);
        nodeEnd(tree, level, bareIrStream);

        return bareIrStream;
    }

    public PrimaryIrStream bitspec_irstream(CommonTree tree, int level, boolean forceOk, RepeatMarker repeatMarker) throws UnassignedException, InvalidRepeatException, DomainViolationException, IncompatibleArgumentException {
        BitSpec bitSpec = null;

        if (level == 1) {
            env.assign("$state", state);
            env.assign("$pass", pass);
        }
        nodeBegin(tree, level);
        bitSpec = bitspec((CommonTree) tree.getChild(0), level + 1, true, repeatMarker);

        PrimaryIrStream irStreamWithoutBitSpec = irstream((CommonTree) tree.getChild(1), level + 1, forceOk, repeatMarker);
        PrimaryIrStream irStream = new PrimaryIrStream(env, irStreamWithoutBitSpec, bitSpec);
        nodeEnd(tree, level, irStream);
        if (level == 1)
            env.assign("$final_state", state);
        return irStream;
    }

    public PrimaryIrStreamItem irstream_item(CommonTree tree, int level, Pass variationAlternative, RepeatMarker repeatMarker, boolean forceOk) throws UnassignedException, InvalidRepeatException, DomainViolationException, IncompatibleArgumentException {
        nodeIntermediate(tree, level, "IrStreamItem");
        String type = tree.getText();
        if (type.equals("FLASH") || type.equals("GAP") || type.equals("EXTENT"))
            return duration(tree, level, forceOk);
        else if (type.equals("BITFIELD") || type.equals("INFINITE_BITFIELD"))
            return bitfield(tree, level, forceOk);
        else if (type.equals("IRSTREAM"))
            return irstream(tree, level, forceOk, repeatMarker);
        else if (type.equals("BITSPEC_IRSTREAM"))
            return bitspec_irstream(tree, level, forceOk, repeatMarker);
        else if (type.equals("ASSIGNMENT"))
            return assignment(tree, level, forceOk, repeatMarker);
        else if (type.equals("VARIATION"))
            return variation(tree, level, forceOk, variationAlternative, repeatMarker);
        else
            throw new RuntimeException("Something I did not think about, " + type + ", occured.");
    }

    public BitSpec bitspec(CommonTree tree, int level, boolean forceOk, RepeatMarker repeatMarker) throws UnassignedException, InvalidRepeatException, DomainViolationException, IncompatibleArgumentException {
        nodeBegin(tree, level);
        ArrayList<PrimaryIrStream> list = new ArrayList<>();
        for (int i = 0; i < tree.getChildCount(); i++)
            list.add(bare_irstream((CommonTree)tree.getChild(i), level+1, forceOk, Pass.intro, repeatMarker));
        BitSpec b = new BitSpec(env, list);
        nodeEnd(tree, level, b);
        return b;
    }

    public RepeatMarker repeatmarker(CommonTree tree, int level, RepeatMarker dummy) {
        nodeBegin(tree, level);
        RepeatMarker repeatMarker = tree.getChildCount() == 2
                ? new RepeatMarker(Integer.parseInt(((CommonTree)tree.getChild(0)).getText()), ((CommonTree)tree.getChild(1)).getText().charAt(0))
                : new RepeatMarker(((CommonTree)tree.getChild(0)).getText());
        nodeEnd(tree, level, repeatMarker);
        return repeatMarker;
    }

    // FIXME: If this function encounters an unknown token (for example when extending the input grammar)
    // it will report that that name is not found in the name engine. Fix.
    public long expression(CommonTree tree, int level) throws UnassignedException, DomainViolationException {
        nodeBegin(tree, level);
        String type = tree.getText();
        long result =
                  type.equals("**") ? IrpUtils.power(expression((CommonTree) tree.getChild(0), level+1), expression((CommonTree) tree.getChild(1), level+1))
                : type.equals("%")  ? expression((CommonTree) tree.getChild(0), level+1) % expression((CommonTree) tree.getChild(1), level+1)
                : type.equals("/")  ? expression((CommonTree) tree.getChild(0), level+1) / expression((CommonTree) tree.getChild(1), level+1)
                : type.equals("*")  ? expression((CommonTree) tree.getChild(0), level+1) * expression((CommonTree) tree.getChild(1), level+1)
                : type.equals("-")  ? expression((CommonTree) tree.getChild(0), level+1) - expression((CommonTree) tree.getChild(1), level+1)
                : type.equals("+")  ? expression((CommonTree) tree.getChild(0), level+1) + expression((CommonTree) tree.getChild(1), level+1)
                : type.equals("&")  ? expression((CommonTree) tree.getChild(0), level+1) & expression((CommonTree) tree.getChild(1), level+1)
                : type.equals("^")  ? expression((CommonTree) tree.getChild(0), level+1) ^ expression((CommonTree) tree.getChild(1), level+1)
                : type.equals("|")  ? expression((CommonTree) tree.getChild(0), level+1) | expression((CommonTree) tree.getChild(1), level+1)
                : type.equals("UMINUS")   ? -expression((CommonTree) tree.getChild(0), level+1)
                : type.equals("BITCOUNT") ? (long) Long.bitCount(expression((CommonTree) tree.getChild(0), level+1))
                : type.equals("BITFIELD") ? bitfield(tree, level+1, true).toLong()
                : type.equals("INFINITE_BITFIELD") ? bitfield(tree, level+1, true).toLong()
                : type.matches("[0-9]+")  ? Long.parseLong(type)
                : env.evaluateName(type);
        nodeEnd(tree, level, result);
        return result;
    }

    public PrimaryIrStream assignment(CommonTree tree, int level, boolean forceOk, RepeatMarker repeatMarker) throws UnassignedException, DomainViolationException {
        if (!passOk(forceOk, level))
            return null;

        nodeBegin(tree, level);
        String name = ((CommonTree)tree.getChild(0)).getText();
        long num = expression((CommonTree) tree.getChild(1), level+1);
        env.assign(name, num);
        nodeEnd(tree, level, num);
        return new PrimaryIrStream(env, true);
    }

    public PrimaryIrStream variation(CommonTree tree, int level, boolean forceOk, Pass variationAlternative, RepeatMarker repeatMarker) throws UnassignedException, InvalidRepeatException, DomainViolationException, IncompatibleArgumentException {
        nodeBegin(tree, level);
        int childNo = variationAlternative.toInt();
        PrimaryIrStream bareIrStream = tree.getChildCount() > childNo ? bare_irstream((CommonTree) tree.getChild(childNo), level + 1, forceOk, Pass.intro, null) : null;
        PrimaryIrStream irStream = new PrimaryIrStream(env, bareIrStream, null, tree.getChildCount());
        nodeEnd(tree, level, irStream);
        return irStream;
    }

    public double float_number(CommonTree tree, int level) {
        nodeBegin(tree, level);
        double num = Double.parseDouble(tree.getChild(0).getText() + "." + tree.getChild(1).getText());
        nodeEnd(tree, level, num);
        return num;
    }

    public double number_with_decimals(CommonTree tree, int level) {
        nodeBegin(tree, level);
        String label = tree.getText();
        double num = label.matches("[0-9]+") ? (double) Integer.parseInt(label) : float_number(tree, level);
        nodeEnd(tree, level, num);
        return num;
    }

    public double name_or_number(CommonTree tree, int level) throws UnassignedException, DomainViolationException {
        nodeBegin(tree, level);
        String label = tree.getText();
        double num = label.equals("FLOAT") ? float_number(tree, level) : label.matches("[0-9\\.]+") ? number_with_decimals(tree, level) : env.evaluateName(label);
        nodeEnd(tree, level, num);
        return num;
    }

    private boolean passOk(boolean forceOk, int level) {
        boolean ok = forceOk || pass == state;
        Debug.debugASTParser(indent(level) + (forceOk ? "Forced OK" : ("Pass: " + pass + (ok ? "==" : "!=") + state + (ok ? ", proceeding..." : ". Traversing interrupted."))));
        return ok;
    }


    private void nodeBegin(CommonTree tree, int level) {
        if (Debug.getInstance().debugOn(Debug.Item.ASTParser))
            Debug.debugASTParser(indent(level) + "AST node " + tree.getText() + " entered, having " + tree.getChildCount() + " child(ren). State = " + state);
    }

    private void nodeEnd(CommonTree tree, int level, Object object) {
        if (Debug.getInstance().debugOn(Debug.Item.ASTParser))
            Debug.debugASTParser(indent(level) + "AST node " + tree.getText() + " left, producing " + (object == null ? "null" : (object.getClass().getSimpleName() + ": " +  object.toString())) + ". State = " + state);
    }

    private void nodeIntermediate(CommonTree tree, int level, String type) {
        if (Debug.getInstance().debugOn(Debug.Item.ASTParser))
            Debug.debugASTParser(indent(level) + "AST intermediate node (" + type + ") " + tree.getText() + " entered, having " + tree.getChildCount() + " child(ren).");
    }

}
