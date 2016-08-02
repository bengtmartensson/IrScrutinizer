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

import org.harctoolbox.IrpMaster.DecodeIR;
import org.harctoolbox.IrpMaster.ExchangeIR;
import org.harctoolbox.IrpMaster.IncompatibleArgumentException;
import org.harctoolbox.IrpMaster.IrSequence;
import org.harctoolbox.IrpMaster.IrSignal;
import org.harctoolbox.IrpMaster.IrpMasterException;
import org.harctoolbox.girr.Command;

/**
 * Note: Editing of the sequences is not implemented (yet).
 *
 */
public class RawIrSignal extends NamedIrSignal {
    private IrSignal irSignal;
    private String analyzerString;
    private DecodeIR.DecodedSignal[] decodes;

    private static boolean generateCcf = true;
    private static boolean decode = true;

    /**
     * @param aGenerateCcf the generateCcf to set
     */
    public static void setGenerateCcf(boolean aGenerateCcf) {
        generateCcf = aGenerateCcf;
    }

    /**
     * @param aDecode the decode to set
     */
    public static void setDecode(boolean aDecode) {
        decode = aDecode;
    }

    public RawIrSignal(IrSignal irSignal, String name, String comment, boolean invokeAnalyzer) {
        super(name, comment);
        setIrSignal(irSignal, invokeAnalyzer);
    }

    public RawIrSignal(Command command, boolean invokeAnalyzer) throws IrpMasterException {
        this(command.toIrSignal(), command.getName(), command.getComment(), invokeAnalyzer);
    }

    private void setIrSignal(IrSignal irSignal, boolean invokeAnalyzer) {
        this.irSignal = irSignal;
        decodes = DecodeIR.decode(irSignal);
        if (invokeAnalyzer  && irSignal.getIntroLength() > 0) // Analyzer misbehaves on zero length signals, be careful.
            analyzerString = ExchangeIR.newAnalyzer(irSignal).toString();
    }

    public Command toCommand() {
        Command command = new Command(getName(), getComment(), irSignal);
        return command;
    }

    public IrSignal getIrSignal() {
        return irSignal;
    }

    public DecodeIR.DecodedSignal getDecode(int i) {
        return decodes[i];
    }

    public String getDecodeString() {
        return DecodeIR.DecodedSignal.toPrintString(decodes, false);
    }

    public int getNoDecodes() {
        return decodes.length;
    }

    public String getAnalyzerString() {
        return analyzerString;
    }

    public void setFrequency(double newFrequency, boolean invokeAnalyzer) {
        setIrSignal(new IrSignal(newFrequency, irSignal.getDutyCycle(), irSignal.getIntroSequence(), irSignal.getRepeatSequence(), irSignal.getEndingSequence()),
                invokeAnalyzer);
    }

    public void setIntroSequence(String str, boolean invokeAnalyzer) throws IncompatibleArgumentException {
        setIrSignal(new IrSignal(irSignal.getFrequency(), irSignal.getDutyCycle(), new IrSequence(str), irSignal.getRepeatSequence(), irSignal.getEndingSequence()),
                invokeAnalyzer);
    }

    public void setRepeatSequence(String str, boolean invokeAnalyzer) throws IncompatibleArgumentException {
        setIrSignal(new IrSignal(irSignal.getFrequency(), irSignal.getDutyCycle(), irSignal.getIntroSequence(), new IrSequence(str), irSignal.getEndingSequence()),
                invokeAnalyzer);
    }

    public void setEndingSequence(String str, boolean invokeAnalyzer) throws IncompatibleArgumentException {
        setIrSignal(new IrSignal(irSignal.getFrequency(), irSignal.getDutyCycle(), irSignal.getIntroSequence(), irSignal.getRepeatSequence(), new IrSequence(str)),
                invokeAnalyzer);
    }

    @Override
    public String csvString(String separator) {
        StringBuilder str = new StringBuilder(super.csvString(separator));
        str.append(irSignal.getFrequency()).append(separator);
        str.append(irSignal.getIntroSequence().toPrintString(true)).append(separator);
        str.append(irSignal.getRepeatSequence().toPrintString(true)).append(separator);
        str.append(irSignal.getEndingSequence().toPrintString(true)).append(separator);
        str.append(DecodeIR.DecodedSignal.toPrintString(decodes, true));
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
            10, 40, 75, 75, 75, 75, 75, 75, 10, 75, 40, 10
        };
        private static final String[] columnNames = new String[] {
            "#", "Date", "Intro", "Repetition", "Ending", "Name", "Decode", "Analyze", "Ver.", "Comment", "Frequency", "C. IrSignal"
        };
        private static final Class<?>[] classes = new Class<?>[] {
            Integer.class, String.class, String.class, String.class, String.class, String.class, String.class, String.class, Boolean.class, String.class, Integer.class, RawIrSignal.class
        };

        public static final int posNumber = 0;
        public static final int posDate = 1;
        public static final int posIntro = 2;
        public static final int posRepetition = 3;
        public static final int posEnding = 4;
        public static final int posName = 5;
        public static final int posVerified = 8;
        public static final int posComment = 9;
        public static final int posFrequency = 10;
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
            return i > posEnding;
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
        public Object[] toObjectArray(NamedIrSignal signal) {
            if (!RawIrSignal.class.isInstance(signal))
                throw new IllegalArgumentException();
            return toObjectArray((RawIrSignal) signal);
        }

        public Object[] toObjectArray(RawIrSignal cir) {
            IrSignal irSignal = cir.getIrSignal();
            Object[] result = new Object[] {
                        cir.getNumeral(), cir.getDate(), irSignal.getIntroSequence().toPrintString(true),
                        irSignal.getRepeatSequence().toPrintString(true), irSignal.getEndingSequence().toPrintString(true),
                        cir.getName(), cir.getDecodeString(), cir.getAnalyzerString(), cir.getValidated(),
                        cir.getComment(), (int) irSignal.getFrequency(), cir, null
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
        public void fireTableCellUpdated(int row, int column) {
            boolean invokeAnalyzer = true; // ???
            try {
                RawIrSignal rawIrSignal = getCapturedIrSignal(row);
                switch (column) {
                    case CapturedIrSignalColumns.posIntro:
                        rawIrSignal.setIntroSequence((String) getValueAt(row, column), invokeAnalyzer);
                        break;
                    case CapturedIrSignalColumns.posRepetition:
                        rawIrSignal.setRepeatSequence((String) getValueAt(row, column), invokeAnalyzer);
                        break;
                    case CapturedIrSignalColumns.posEnding:
                        rawIrSignal.setEndingSequence((String) getValueAt(row, column), invokeAnalyzer);
                        break;
                    case CapturedIrSignalColumns.posVerified:
                        rawIrSignal.setValidated((Boolean) getValueAt(row, column));
                        break;
                    case CapturedIrSignalColumns.posName:
                        rawIrSignal.setName((String) getValueAt(row, column));
                        break;
                    case CapturedIrSignalColumns.posComment:
                        rawIrSignal.setComment((String) getValueAt(row, column));
                        break;
                    case CapturedIrSignalColumns.posFrequency:
                        rawIrSignal.setFrequency((Integer)getValueAt(row, column), invokeAnalyzer);
                    default:
                        break;
                }
            } catch (IncompatibleArgumentException | NumberFormatException ex) {
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
    }
}
