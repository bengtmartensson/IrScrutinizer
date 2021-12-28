/*
Copyright (C) 2013 Bengt Martensson.

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

package org.harctoolbox.irscrutinizer;

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;
import org.harctoolbox.analyze.Analyzer;
import org.harctoolbox.analyze.NoDecoderMatchException;
import org.harctoolbox.girr.Command;
import org.harctoolbox.ircore.InvalidArgumentException;
import org.harctoolbox.ircore.IrCoreException;
import org.harctoolbox.ircore.IrCoreUtils;
import org.harctoolbox.ircore.IrSequence;
import org.harctoolbox.ircore.IrSignal;
import org.harctoolbox.ircore.ModulatedIrSequence;
import org.harctoolbox.ircore.OddSequenceLengthException;
import org.harctoolbox.irp.BitDirection;
import org.harctoolbox.irp.Decoder;
import org.harctoolbox.irp.ElementaryDecode;
import org.harctoolbox.irp.IrpException;
import org.harctoolbox.irp.Protocol;
import org.harctoolbox.irscrutinizer.exporter.NameUniquefier;

/**
 * Note: Editing of the sequences is not implemented (yet).
 *
 */
class RawIrSignal extends NamedIrSignal {

    private static Decoder decoder = null;

    // Preferences
    //private static boolean generateCcf = true;
    private static boolean invokeDecoder = true;
    private static boolean invokeAnalyzer = true;
    private static Decoder.DecoderParameters decoderParameters = null;

    // These are parameters for the analyzer. TODO: Should probably be more dynamic.
    private static int analyzerRadix = 16;
    private static Double absoluteTolerance = IrCoreUtils.DEFAULT_ABSOLUTE_TOLERANCE;
    private static Double relativeTolerance = IrCoreUtils.DEFAULT_RELATIVE_TOLERANCE;
    private static String timeBaseString = null;
    private static BitDirection bitDirection = BitDirection.msb;
    private static boolean useExtents = true;
    private static List<Integer> parameterWidths = new ArrayList<>(0);
    private static boolean invert= false;

    /**
     * @param aDecoder the decoder to set
     */
    public static void setDecoder(Decoder aDecoder) {
        decoder = aDecoder;
    }

    public static void setDecoderParameters(Decoder.DecoderParameters params) {
        decoderParameters = params;
    }

    /**
     * @param aInvokeDecoder the invokeDecoder to set
     */
    public static void setInvokeDecoder(boolean aInvokeDecoder) {
        invokeDecoder = aInvokeDecoder;
    }
    /**
     * @param aInvokeAnalyzer the invokeAnalyzer to set
     */
    public static void setInvokeAnalyzer(boolean aInvokeAnalyzer) {
        invokeAnalyzer = aInvokeAnalyzer;
    }

    /**
     * @param aAnalyzerRadix the analyzerRadix to set
     */
    public static void setAnalyzerRadix(int aAnalyzerRadix) {
        analyzerRadix = aAnalyzerRadix;
    }
    /**
     * @param aAbsoluteTolerance the absoluteTolerance to set
     */
    public static void setAbsoluteTolerance(Double aAbsoluteTolerance) {
        absoluteTolerance = aAbsoluteTolerance;
    }
    /**
     * @param aRelativeTolerance the relativeTolerance to set
     */
    public static void setRelativeTolerance(Double aRelativeTolerance) {
        relativeTolerance = aRelativeTolerance;
    }
    /**
     * @param aTimeBaseString the timeBaseString to set
     */
    public static void setTimeBaseString(String aTimeBaseString) {
        timeBaseString = aTimeBaseString;
    }
    /**
     * @param aBitDirection the bitDirection to set
     */
    public static void setBitDirection(BitDirection aBitDirection) {
        bitDirection = aBitDirection;
    }
    /**
     * @param aUseExtents the useExtents to set
     */
    public static void setUseExtents(boolean aUseExtents) {
        useExtents = aUseExtents;
    }
    /**
     * @param aParameterWidths the parameterWidths to set
     */
    @SuppressWarnings("AssignmentToCollectionOrArrayFieldFromParameter")
    public static void setParameterWidths(List<Integer> aParameterWidths) {
        parameterWidths = aParameterWidths;
    }
    /**
     * @param aInvert the invert to set
     */
    public static void setInvert(boolean aInvert) {
        invert = aInvert;
    }

    private IrSignal irSignal = null;
    private String analyzerString = null;
    private Decoder.AbstractDecodesCollection<? extends ElementaryDecode> decodes = null; // null: no decoding attempted; isEmpty: tried decoding, but no decode found

    RawIrSignal(IrSignal irSignal, String name, String comment) {
        super(name, comment);
        setIrSignal(irSignal);
    }

    RawIrSignal(RawIrSignal old) {
        this(old.getIrSignal(), old.getName(), old.getComment());
    }

    RawIrSignal(ModulatedIrSequence irSequence, String name, String comment) {
        super(name, comment);
        setIrSignal(irSequence);
    }

    RawIrSignal(Command command) throws IrpException, IrCoreException {
        this(command.toIrSignal(), command.getName(), command.getComment());
    }

    RawIrSignal() {
        this(new IrSignal(), "", "");
    }

    private void setIrSignal(IrSignal irSignal, Decoder.AbstractDecodesCollection<? extends ElementaryDecode> decodes) {
        this.irSignal = irSignal;
        this.decodes = decodes;
        if (invokeAnalyzer) {
            try {
                Analyzer analyzer = new Analyzer(irSignal, absoluteTolerance, relativeTolerance);
                Analyzer.AnalyzerParams analyzerParams = new Analyzer.AnalyzerParams(irSignal.getFrequency(), timeBaseString, bitDirection, useExtents, parameterWidths, invert);
                List<Protocol> list = analyzer.searchBestProtocol(analyzerParams);
                if (!list.isEmpty() && list.get(0) != null)
                    analyzerString = list.get(0).toIrpString(analyzerRadix);
            } catch (NoDecoderMatchException | InvalidArgumentException ex) {
                analyzerString = null;
            }
        }
    }

    private void setIrSignal(IrSignal irSignal) {
        setIrSignal(irSignal, invokeDecoder ? decoder.decodeLoose(irSignal, decoderParameters) : null);
    }

    private void setIrSignal(ModulatedIrSequence irSequence) {
        setIrSignal(new IrSignal(irSequence), invokeDecoder ?  new Decoder.SimpleDecodesSet(decoder.decode(irSequence, decoderParameters)) : null);
    }

    public Command toCommand() {
        Command command = new Command(getName(), getComment(), irSignal);
        return command;
    }

    public IrSignal getIrSignal() {
        return irSignal;
    }

    public String getDecodeString() {
        if (decodes == null)
            return "";

        StringJoiner stringJoiner = new StringJoiner("; ");
        decodes.forEach((dec) -> {
            stringJoiner.add(dec.toString());
        });
        return stringJoiner.toString();
    }

    public int getNoDecodes() {
        return decodes.size();
    }

    public String getAnalyzerString() {
        return analyzerString;
    }

    public void setFrequency(double newFrequency) {
        IrSignal sig = new IrSignal(irSignal.getIntroSequence(), irSignal.getRepeatSequence(), irSignal.getEndingSequence(), newFrequency, irSignal.getDutyCycle());
        setIrSignal(sig);
    }

    public void setIntroSequence(String str) {
        try {
            IrSignal sig = new IrSignal(new IrSequence(str), irSignal.getRepeatSequence(), irSignal.getEndingSequence(), irSignal.getFrequency(), irSignal.getDutyCycle());
            setIrSignal(sig);
        } catch (OddSequenceLengthException | NumberFormatException ex) {
            // TODO
        }
    }

    public void setRepeatSequence(String str) {
        try {
            IrSignal sig = new IrSignal(irSignal.getIntroSequence(), new IrSequence(str), irSignal.getEndingSequence(), irSignal.getFrequency(), irSignal.getDutyCycle());
            setIrSignal(sig);
        } catch (OddSequenceLengthException | NumberFormatException ex) {
            // TODO
        }
    }

    public void setEndingSequence(String str) {
        try {
            IrSignal sig = new IrSignal(irSignal.getIntroSequence(), irSignal.getRepeatSequence(), new IrSequence(str), irSignal.getFrequency(), irSignal.getDutyCycle());
            setIrSignal(sig);
        } catch (OddSequenceLengthException ex) {
            // TODO
        }
    }

    @Override
    public String csvString(String separator) {
        StringBuilder str = new StringBuilder(super.csvString(separator));
        str.append(irSignal.getFrequency()).append(separator);
        str.append(irSignal.getIntroSequence().toString(true)).append(separator);
        str.append(irSignal.getRepeatSequence().toString(true)).append(separator);
        str.append(irSignal.getEndingSequence().toString(true)).append(separator);
//        str.append(DecodeIR.DecodedSignal.toPrintString(decodes, true));
        str.append(separator);
        str.append(analyzerString).append(separator);
        return str.toString();
    }

    @Override
    public String toPrintString() {
        return csvString(", ");
    }

    private static class CapturedIrSignalColumns extends NamedIrSignal.AbstractColumnFunction {

        private static final int[] widths = {
            10, 40, 75, 75, 75, 75, 75, 75, 75, 40, 10
        };
        private static final String[] columnNames = new String[] {
            "#", "Timestamp", "Intro", "Repetition", "Ending", "Name", "Decode", "Analyze", "Comment", "Frequency", "C. IrSignal"
        };
        private static final Class<?>[] classes = new Class<?>[] {
            Integer.class, String.class, String.class, String.class, String.class, String.class, String.class, String.class, String.class, Integer.class, RawIrSignal.class
        };

        public static final int posNumber = 0;
        public static final int posDate = 1;
        public static final int posIntro = 2;
        public static final int posRepetition = 3;
        public static final int posEnding = 4;
        public static final int posName = 5;
        public static final int posDecode = 6;
        public static final int posAnalyze = 7;
        public static final int posComment = 8;
        public static final int posFrequency = 9;
        public static final int posCapturedIrSignal = columnNames.length - 1;

        CapturedIrSignalColumns() {
            super(columnNames, widths, classes, /*dummyArray,*/ 1);
        }

        @Override
        public int getPosName() {
            return posName;
        }

        @Override
        public int getPosComment() {
            return posComment;
        }

        @Override
        public int getPosIrSignal() {
            return posCapturedIrSignal;
        }

        @Override
        public boolean isEditable(int i) {
            return i > posDate && i != posDecode && i != posAnalyze;
        }

        @Override
        public int getPosDate() {
            return posDate;
        }

        @Override
        public int getPosNumber() {
            return posNumber;
        }

        @Override
        public boolean uninterestingIfAllEqual(int column) {
            return super.uninterestingIfAllEqual(column) || column == posFrequency;
        }

        @Override
        public Object[] toObjectArray(NamedIrSignal signal) {
            if (!RawIrSignal.class.isInstance(signal))
                throw new IllegalArgumentException();
            return toObjectArray((RawIrSignal) signal);
        }

        public Object[] toObjectArray(RawIrSignal cir) {
            IrSignal irSignal = cir.getIrSignal();
            Object[] result = new Object[]{
                cir.getNumeral(),
                cir.getDate(),
                ((IrSequence) irSignal.getIntroSequence()).toString(true, ","),
                ((IrSequence) irSignal.getRepeatSequence()).toString(true, ","),
                ((IrSequence) irSignal.getEndingSequence()).toString(true, ","),
                cir.getName(),
                cir.getDecodeString(),
                cir.getAnalyzerString(),
                cir.getComment(),
                Math.round(irSignal.getFrequency()),
                cir, // Analyze
                null
            };
            assert(result.length == columnNames.length);
            return result;
        }
    }

    public static class RawTableColumnModel extends NamedIrSignal.LearnedIrSignalTableColumnModel {
        public RawTableColumnModel() {
            super(new CapturedIrSignalColumns());
        }
    }

    public static class RawTableModel extends NamedIrSignal.LearnedIrSignalTableModel {
        public RawTableModel() {
            super(new CapturedIrSignalColumns());
        }

        public RawIrSignal getCapturedIrSignal(int row) {
            return validRow(row)
                    ? (RawIrSignal) getValueAt(row, CapturedIrSignalColumns.posCapturedIrSignal)
                    : null;
        }

        @Override
        public Command toCommand(int row) {
            RawIrSignal rir = getCapturedIrSignal(row);
            return rir.toCommand();
        }

        @Override
        void duplicate(int modelRow) {
            RawIrSignal raw = new RawIrSignal(getCapturedIrSignal(modelRow));
            addSignal(raw);
        }

        @Override
        public void fireTableCellUpdated(int row, int column) {
            try {
                RawIrSignal rawIrSignal = getCapturedIrSignal(row);
                switch (column) {
                    case CapturedIrSignalColumns.posIntro:
                        rawIrSignal.setIntroSequence((String) getValueAt(row, column));
                        break;
                    case CapturedIrSignalColumns.posRepetition:
                        rawIrSignal.setRepeatSequence((String) getValueAt(row, column));
                        break;
                    case CapturedIrSignalColumns.posEnding:
                        rawIrSignal.setEndingSequence((String) getValueAt(row, column));
                        break;
                    case CapturedIrSignalColumns.posName:
                        rawIrSignal.setName((String) getValueAt(row, column));
                        break;
                    case CapturedIrSignalColumns.posComment:
                        rawIrSignal.setComment((String) getValueAt(row, column));
                        break;
                    case CapturedIrSignalColumns.posFrequency:
                        rawIrSignal.setFrequency((Integer)getValueAt(row, column));
                        break;
                    default:
                        return; // !!
                }
                setValueAt(rawIrSignal.getDecodeString(), row, CapturedIrSignalColumns.posDecode);
                setValueAt(rawIrSignal.getAnalyzerString(), row, CapturedIrSignalColumns.posAnalyze);
            } catch (NumberFormatException ex) {
                System.err.println(ex.getMessage()); // FIXME; (good for now)
            }
        }

        @Override
        public String toPrintString(int row) {
            RawIrSignal cir = getCapturedIrSignal(row);
            return super.toPrintString(row) + ": " + (cir != null ? cir.toPrintString() : "null");
        }

        @Override
        public String getType() {
            return "raw";
        }

        public void chopSignals(Iterable<Integer> rows, double chopSize) throws IrpException, IrCoreException {
            NameUniquefier uniquefier = new NameUniquefier(this.getUniqueNames(), "_");

            for (Integer row : rows) {
                Command command = toCommand(row);
                IrSignal irSignal = command.toIrSignal();
                ModulatedIrSequence irSequence = irSignal.toModulatedIrSequence();
                Double frequency = irSignal.getFrequency();
                String stem = command.getName();
                List<IrSequence> fragments = irSequence.chop(IrCoreUtils.milliseconds2microseconds(chopSize));
                for (IrSequence fragment : fragments) {
                    IrSignal miniSignal = new IrSignal(fragment, frequency, null);
                    String newName = uniquefier.uniquefy(stem);
                    RawIrSignal chopped = new RawIrSignal(miniSignal, newName, "Chopped from " + stem);
                    this.addSignal(chopped);
                }
            }
            fireTableDataChanged();
        }
    }
}
