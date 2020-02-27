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
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import org.harctoolbox.girr.Command;
import org.harctoolbox.girr.GirrException;
import org.harctoolbox.ircore.IrCoreException;
import org.harctoolbox.ircore.IrCoreUtils;
import org.harctoolbox.ircore.IrSignal;
import org.harctoolbox.ircore.ModulatedIrSequence;
import org.harctoolbox.irp.Decoder;
import org.harctoolbox.irp.IrpException;

/**
 *
 *
 */
public class ParametrizedIrSignal extends NamedIrSignal {
    private static boolean generateRaw = true;
    private static boolean generateCcf = true;
    private static Decoder decoder = null;
    private static Decoder.DecoderParameters decoderParameters = null;

    /**
     * @param aGenerateRaw the generateRaw to set
     */
    public static void setGenerateRaw(boolean aGenerateRaw) {
        generateRaw = aGenerateRaw;
    }

    /**
     * @param aGenerateCcf the generateCcf to set
     */
    public static void setGenerateCcf(boolean aGenerateCcf) {
        generateCcf = aGenerateCcf;
    }

    /**
     *
     * @param aDecoder
     */
    public static void setDecoder(Decoder aDecoder) {
        decoder = aDecoder;
    }

    public static void setDecoderParameters(Decoder.DecoderParameters params) {
        decoderParameters = params;
    }

    private static String formatMiscParams(Map<String, Long> params) {
        if (params == null)
            return "";
        StringBuilder str = new StringBuilder(16);
        params.entrySet().forEach((kvp) -> {
            String key = kvp.getKey();
            if (!(key.equals("D") || key.equals("S") || key.equals("F") || key.equals("T"))) {
                if (str.length() > 0)
                    str.append(" ");
                str.append(key).append("=").append(kvp.getValue());
            }
        });
        return str.toString();
    }

    private Map<String, Long>parameters;
    private String protocolName;

    public ParametrizedIrSignal(Command command) throws IrpException, IrCoreException {
        super(command.getName(), command.getComment());
        this.protocolName = command.getProtocolName();
        this.parameters = command.getParameters();
        if (parameters == null)
            parameters = new HashMap<>(3);
    }

    public ParametrizedIrSignal(String protocolName, Map<String, Long>parameters, String name, String comment) {
        super(name, comment);
        this.parameters = parameters;
        this.protocolName = protocolName;
    }

    public ParametrizedIrSignal(ParametrizedIrSignal old) {
        this(old.getProtocol(), old.parameters, old.getName(), old.getComment());
    }

    public ParametrizedIrSignal(String protocolName, long device, long subdevice, long function, String name, String comment) {
        super(name, comment);
        parameters = new HashMap<>(3);
        setParameter("F", function);
        setParameter("D", device);
        setParameter("S", subdevice);
        this.protocolName = protocolName;
    }

    public ParametrizedIrSignal(String protocolName, long device, long function, String name, String comment) {
        this(protocolName, device, IrCoreUtils.INVALID, function, name, comment);
    }

    public ParametrizedIrSignal(Decoder.Decode decode, String name, String comment) {
        this(decode.getName(), decode.getMap(), name, comment);
    }

    public ParametrizedIrSignal(IrSignal irSignal, boolean ignoreT) throws NoDecodeException {
        this(irSignal, "", "", ignoreT);
    }

    public ParametrizedIrSignal(ModulatedIrSequence irSequence, boolean ignoreT) throws NoDecodeException {
        this(irSequence, "", "", ignoreT);
    }

    public ParametrizedIrSignal(IrSignal irSignal, String name, String comment, boolean ignoreT) throws NoDecodeException {
        this(decoder.decodeIrSignal(irSignal, decoderParameters), name, comment, ignoreT);
    }

    public ParametrizedIrSignal(ModulatedIrSequence irSequence, String name, String comment, boolean ignoreT) throws NoDecodeException {
        this(new Decoder.SimpleDecodesSet(decoder.decode(irSequence, decoderParameters)), name, comment, ignoreT);
    }

    public ParametrizedIrSignal(Decoder.SimpleDecodesSet decodes, String name, String comment, boolean ignoreT) throws NoDecodeException {
        super(name, comment);
        if (decodes.isEmpty()) {
            decrementCount();
            throw new NoDecodeException();
        }
        Decoder.Decode decode = decodes.first();
        protocolName = decode.getName();
        parameters = decode.getMap();
        if (ignoreT && parameters.containsKey("T"))
            parameters.remove("T");
    }

    ParametrizedIrSignal() {
        this(null, new HashMap<>(0), null, null);
    }

    public void digestMiscParameters(String payload) {
        if (payload == null || payload.trim().isEmpty())
            return;

        for (String name : parameters.keySet().toArray(new String[parameters.size()])) {
            if (!(name.equals("D") || name.equals("S") || name.equals("F") || name.equals("T")))
                parameters.remove(name);
        }
        String[] chunks = payload.split("\\s+");
        for (String chunk : chunks) {
            String[] kvp = chunk.split("=");
            if (kvp.length == 2) { // Not perfect, should barf to the user.
                String name = kvp[0];
                if (name.equals("D") || name.equals("S") || name.equals("F") || name.equals("T"))
                    continue;
                long value = IrCoreUtils.parseLong(kvp[1]);

                setParameter(name, value);
            }
        }
    }

    public String getProtocol() {
        return protocolName;
    }

    public long getParameter(String param) {
        Long val = parameters.get(param);
        return val != null ? val : IrCoreUtils.INVALID;
    }

    private void setParameter(String name, Object object) {
        setParameter(name, object != null ? (Integer) object : IrCoreUtils.INVALID);
    }

    public final void setParameter(String name, long value) {
        if (value == IrCoreUtils.INVALID)
            parameters.remove(name);
        else
            parameters.put(name, value);
    }

    public String formatMiscParameters() {
        return formatMiscParams(parameters);
    }

    // TODO: @Override public String csvString(String separator) {

    @Override
    public String toPrintString() {
        return csvString(", ") + ", " + protocolName + ", " + parameters.toString();
    }


    public Command toCommand() throws GirrException {
        return new Command(getName(), getComment(), protocolName, parameters);
    }

    private String dummyName() {
        return DefaultSignalNameFormatter.formatName(protocolName, parameters);
    }

    private static class ParameterIrSignalColumns extends NamedIrSignal.AbstractColumnFunction /*implements IColumn*/ {

        private static final int[] widths = {
            //0  1   2   3   4   5   6   7    8    9   10  11
            15, 60, 60, 25, 25, 25, 25, 100, 25, 100, 200, 10
        };
        private static final String[] columnNames = new String[]{
            // 0      1        2        3    4    5    6       7            8         9         10       11
            "#", "Date", "Protocol", "D", "S", "F", "T", "Misc. params", "Ver.", "Name", "Comment", "Signal"
        };
        /*private static final Object[] dummyArray = new Object[]{
            //    0                 1       2        3    4      5    6     7     8       9        10     11
            //    0                 1       2        3    4      5    6     7     8       9        10     11
            Integer.valueOf(0), "0:00", "Protocol", null, null, null, null, "", false, "Name", "Comment", new ParametrizedIrSignal("xxx", new HashMap<String, Long>(), null, null)
        };*/
        private static final Class<?>[] classes = new Class<?>[]{
            //  0              1             2            3              4              5
            //  0              1             2            3              4              5
            Integer.class, String.class, String.class, Integer.class, Integer.class, Integer.class,
            //  6              7             8               9             10               11
            Integer.class, String.class, Boolean.class, String.class, String.class, ParametrizedIrSignal.class
        };

        public final static int toIgnore = 1; // Never show the last entry to user, the ParametrizedIrSignal

        public static final int posNumber = 0;
        public static final int posDate = 1;
        public final static int posProtocol = 2;
        public final static int posD = 3;
        public final static int posS = 4;
        public final static int posF = 5;
        public final static int posT = 6;
        public final static int posMiscParameters = 7;
        public final static int posVerified = 8;
        public final static int posName = 9;
        public final static int posComment = 10;
        public final static int posParameterIrSignal = columnNames.length - 1;

        ParameterIrSignalColumns() {
            super(columnNames, widths, classes, /*dummyArray,*/ toIgnore);
        }

        @Override
        public boolean isImportant(int column) {
            return super.isImportant(column)
                    || column == posProtocol
                    || column == posD
                    || column == posF;
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
        public int getPosDate() {
            return posDate;
        }

        @Override
        public int getPosNumber() {
            return posNumber;
        }

        @Override
        public int getPosIrSignal() {
            return posParameterIrSignal;
        }

        @Override
        public int getPosVerified() {
            return posVerified;
        }

        @Override
        public boolean isEditable(int i) {
            return i != posNumber && i != posDate;
        }

        @Override
        public Object[] toObjectArray(NamedIrSignal signal) {
            if (!ParametrizedIrSignal.class.isInstance(signal))
                throw new IllegalArgumentException();
            return toObjectArray((ParametrizedIrSignal) signal);
        }

        public Object[] toObjectArray(ParametrizedIrSignal signal) {
            //if (signal == null || signal.parameters == null)
            //    return new Object[0];
            //HashMap<String, Long>params = signal.parameters;
            return new Object[] {
                        //       0                   1                  2                    3
                        signal.getNumeral(), signal.getDate(), signal.protocolName, safeGet(signal.parameters, "D"),
                        //       4                                 5                                 6
                        safeGet(signal.parameters, "S"), safeGet(signal.parameters, "F"), safeGet(signal.parameters, "T"),
                        //            7                           8
                        formatMiscParams(signal.parameters), signal.getValidated(),
                        //       9             10                11
                        signal.getName(), signal.getComment(), signal
                    };
        }

        private Integer safeGet(Map<String, Long>map, String key) {
            return map == null ? null
                    : map.get(key) == null ? null : map.get(key).intValue();
        }
    }

    public static class ParameterIrSignalTableColumnModel extends NamedIrSignal.LearnedIrSignalTableColumnModel {
        public ParameterIrSignalTableColumnModel() {
            super(new ParameterIrSignalColumns());
        }
    }

    public static class ParameterIrSignalTableModel extends NamedIrSignal.LearnedIrSignalTableModel {
        public ParameterIrSignalTableModel() {
            super(new ParameterIrSignalColumns());
        }

        public ParametrizedIrSignal getParameterIrSignal(int row) {
            return validRow(row)
                    ? (ParametrizedIrSignal) getValueAt(row, ParameterIrSignalColumns.posParameterIrSignal)
                    : null;
        }

        public void addSignal(ParametrizedIrSignal signal) {
            super.addSignal(signal);
        }

        @Override
        void duplicate(int modelRow) {
            ParametrizedIrSignal pir = new ParametrizedIrSignal(getParameterIrSignal(modelRow));
            addSignal(pir);
        }

        public ArrayList<Long> listF(Command reference) throws IrpException, IrCoreException, GirrException {
            ArrayList<Long> list = new ArrayList<>(16);
            @SuppressWarnings("unchecked")
            Map<String, Long> params = new HashMap<>(reference.getParameters());
            params.remove("F");
            for (int row =  0; row < getRowCount(); row++) {
                Command cmd = toCommand(row);
                if (!reference.getProtocolName().equalsIgnoreCase(cmd.getProtocolName()))
                    continue;

                boolean eq = true;
                for (Entry<String, Long> kvp : params.entrySet()) {
                    if (cmd.getParameters().get(kvp.getKey()) != kvp.getValue().longValue()) {
                        eq = false;
                        break;
                    }
                }
                if (!eq)
                    continue;

                Long F = cmd.getParameters().get("F");
                list.add(F);
            }
            return list;
        }

        public void setProtocol(String newProtocol) {
            for (int row =  0; row < getRowCount(); row++) {
                //ParametrizedIrSignal pir = getParameterIrSignal(row);
                setValueAt(newProtocol, row, ParameterIrSignalColumns.posProtocol);
            }
            fireTableDataChanged();
        }

        public void setMiscParameters(String value) {
            for (int row =  0; row < getRowCount(); row++)
                setValueAt(value, row, ParameterIrSignalColumns.posMiscParameters);
            fireTableDataChanged();
        }

        public void addDummyNames() {
            for (int row = 0; row < getRowCount(); row++) {
                ParametrizedIrSignal irSignal = getParameterIrSignal(row);
                if (irSignal.getName().isEmpty()) {
                    String newName = irSignal.dummyName();
                    setValueAt(newName, row, ParameterIrSignalColumns.posName);
                }
            }
            fireTableDataChanged();
        }

        private int colPos(String name) {
            return name.equals("D") ? ParameterIrSignalColumns.posD
                    : name.equals("S") ? ParameterIrSignalColumns.posS
                    : name.equals("F") ? ParameterIrSignalColumns.posF
                    : name.equals("T") ? ParameterIrSignalColumns.posT
                    : -1;
        }

        public void setParameter(int colPos, long value) {
            if (colPos < 0)
                return;

            for (int row = 0; row < getRowCount(); row++)
                setValueAt(value != IrCoreUtils.INVALID ? (int) value : null, row, colPos);

            fireTableDataChanged();
        }

        public void setParameter(String name, long value) {
            setParameter(colPos(name), value);
        }

        public void unsetParameter(int colPos) {
            if (colPos < 0)
                return;

            for (int row = 0; row < getRowCount(); row++)
                setValueAt(null, row, colPos);

            fireTableDataChanged();
        }

        public void unsetParameter(String name) {
            unsetParameter(colPos(name));
        }

        @Override
        public Command toCommand(int row) throws GirrException {
            ParametrizedIrSignal pir = getParameterIrSignal(row);
            return pir.toCommand();
        }

        @Override
        public String toPrintString(int row) {
            ParametrizedIrSignal pir = getParameterIrSignal(row);
            return super.toPrintString(row) + ": " + (pir != null ? pir.toPrintString() : "null");
        }

        @Override
        public void fireTableCellUpdated(int row, int column) {
            //System.err.println("************" + row + "-" + column);
            ParametrizedIrSignal pir = getParameterIrSignal(row);
            this.unsavedChanges = true;
            switch (column) {
                case ParameterIrSignalColumns.posProtocol:
                    pir.protocolName = (String) getValueAt(row, column);
                    break;
                case ParameterIrSignalColumns.posD:
                    pir.setParameter("D", getValueAt(row, column));
                    break;
                case ParameterIrSignalColumns.posS:
                    pir.setParameter("S", getValueAt(row, column));
                    break;
                case ParameterIrSignalColumns.posF:
                    pir.setParameter("F", getValueAt(row, column));
                    break;
                case ParameterIrSignalColumns.posT:
                    pir.setParameter("T", getValueAt(row, column));
                    break;
                case ParameterIrSignalColumns.posMiscParameters:
                    pir.digestMiscParameters((String) getValueAt(row, column));
                    break;
                case ParameterIrSignalColumns.posVerified:
                    pir.setValidated((Boolean)getValueAt(row, column));
                    break;
                case ParameterIrSignalColumns.posName:
                    pir.setName((String)getValueAt(row, column));
                    break;
                case ParameterIrSignalColumns.posComment:
                    pir.setComment((String)getValueAt(row, column));
                    break;
                default:
                    break;
            }
        }

        @Override
        public String getType() {
            return "parametrized";
        }

        public void deleteDefaultedSignals() {
            for (int row = getRowCount() - 1; row >= 0; row--) {
                ParametrizedIrSignal pir = getParameterIrSignal(row);
                String defaultName = DefaultSignalNameFormatter.formatName(pir.protocolName, pir.parameters);
                if (pir.getName().equalsIgnoreCase(defaultName) && pir.getComment().isEmpty() && ! pir.getValidated())
                    removeRow(row);
            }
            fireTableDataChanged();
        }
    }
}
