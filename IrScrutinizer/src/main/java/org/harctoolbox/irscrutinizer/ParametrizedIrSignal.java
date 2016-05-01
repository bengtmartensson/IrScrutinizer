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
import java.util.Map.Entry;
import org.harctoolbox.IrpMaster.DecodeIR;
import org.harctoolbox.IrpMaster.DecodeIR.DecodeIrException;
import org.harctoolbox.IrpMaster.IrSignal;
import org.harctoolbox.IrpMaster.IrpMaster;
import org.harctoolbox.IrpMaster.IrpMasterException;
import org.harctoolbox.IrpMaster.IrpUtils;
import org.harctoolbox.girr.Command;

/**
 *
 *
 */
public class ParametrizedIrSignal extends NamedIrSignal {
    private static boolean generateRaw = true;
    private static boolean generateCcf = true;
    private static IrpMaster irpMaster = null;


    private HashMap<String, Long>parameters;
    private String protocolName;

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
     * @param aIrpMaster
     */
    public static void setIrpMaster(IrpMaster aIrpMaster) {
        irpMaster = aIrpMaster;
    }

    public ParametrizedIrSignal(Command command) throws IrpMasterException {
        super(command.getName(), command.getComment());
        this.protocolName = command.getProtocol();
        this.parameters = command.getParameters();
    }

    public ParametrizedIrSignal(String protocolName, HashMap<String, Long>parameters, String name, String comment) {
        super(name, comment);
        this.parameters = parameters;
        this.protocolName = protocolName;
    }

    public ParametrizedIrSignal(String protocolName, long device, long subdevice, long function, String name, String comment) {
        super(name, comment);
        parameters = new HashMap<>();
        setParameter("F", function);
        setParameter("D", device);
        setParameter("S", subdevice);
        this.protocolName = protocolName;
    }

    public ParametrizedIrSignal(String protocolName, long device, long function, String name, String comment) {
        this(protocolName, device, IrpUtils.invalid, function, name, comment);
    }

    public ParametrizedIrSignal(DecodeIR.DecodedSignal decode, String name, String comment) {
        this(decode.getProtocol(), decode.getParameters(), name, comment);
    }

    public ParametrizedIrSignal(IrSignal irSignal, String name, String comment, boolean ignoreT) throws DecodeIrException {
        super(name, comment);
        DecodeIR.DecodedSignal[] decodes = DecodeIR.decode(irSignal);
        if (decodes.length == 0) {
            decrementCount();
            throw new DecodeIR.DecodeIrException("No decode");
        }
        DecodeIR.DecodedSignal decode = decodes[0];
        if (decode.getProtocol().substring(0, 3).equalsIgnoreCase("gap")) {
            decrementCount();
            throw new DecodeIR.DecodeIrException("No sensible decode");
        }
        protocolName = decode.getProtocol();
        parameters = decode.getParameters();
        if (ignoreT && parameters.containsKey("T"))
            parameters.remove("T");
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
            String name = kvp[0];
            long value = Long.parseLong(kvp[1]);
            if (name.equals("D") || name.equals("S") || name.equals("F") || name.equals("T"))
                continue;

            setParameter(name, value);
        }
    }

    public void nukeHex() {
        parameters.remove("hex");
    }

    public String getProtocol() {
        return protocolName;
    }

    public long getParameter(String param) {
        Long val = parameters.get(param);
        return val != null ? val : IrpUtils.invalid;
    }

    private void setParameter(String name, Object object) {
        setParameter(name, object != null ? (long) (Integer) object : IrpUtils.invalid);
    }

    public final void setParameter(String name, long value) {
        if (value == IrpUtils.invalid)
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

    private static String formatMiscParams(HashMap<String, Long> params) {
        if (params == null)
            return "";
        StringBuilder str = new StringBuilder();
        for (Entry<String, Long> kvp : params.entrySet()) {
            String key = kvp.getKey();
            if (!(key.equals("D") || key.equals("S") || key.equals("F") || key.equals("T"))) {
                if (str.length() > 0)
                    str.append(" ");
                str.append(key).append("=").append(kvp.getValue());
            }
        }
        return str.toString();
    }

    public Command toCommand() throws IrpMasterException {
        if (protocolName == null || protocolName.isEmpty())
            throw new IrpMasterException("Protocol name is empty");
        if (parameters == null || parameters.isEmpty())
            throw new IrpMasterException("Parameters missing");

        // Strip out parameter named "hex" before sending to IrpMaster
        // (not used by any current protocols, just causes noisy warnings).
        @SuppressWarnings("unchecked")
        HashMap<String,Long> localParameters = (HashMap<String,Long>) parameters.clone();
        localParameters.remove("hex");
        Command command = new Command(getName(), getComment(), protocolName, localParameters);
        return command;
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

        ParameterIrSignalColumns() {
            super(columnNames, widths, classes, /*dummyArray,*/ toIgnore);
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

        private Integer safeGet(HashMap<String, Long>map, String key) {
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
            super.addSignal((NamedIrSignal) signal);
        }

        public void setFToHex() {
            for (int row =  0; row < getRowCount(); row++) {
                Long hex = getParameterIrSignal(row).getParameter("hex");
                if (/*hex != null &&*/ hex != IrpUtils.invalid)
                    setValueAt(hex.intValue(), row, ParameterIrSignalColumns.posF);
            }
            fireTableDataChanged();
        }

        public ArrayList<Long> listF(Command reference) throws IrpMasterException {
            ArrayList<Long> list = new ArrayList<>();
            @SuppressWarnings("unchecked")
            HashMap<String, Long> params = (HashMap<String, Long>) reference.getParameters().clone();
            params.remove("F");
            for (int row =  0; row < getRowCount(); row++) {
                Command cmd = toCommand(row);
                if (!reference.getProtocol().equalsIgnoreCase(cmd.getProtocol()))
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
                setValueAt(value != IrpUtils.invalid ? (int) value : null, row, colPos);

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

        public void nukeHex() {
            for (int row =  0; row < getRowCount(); row++) {
                ParametrizedIrSignal pir = getParameterIrSignal(row);
                pir.nukeHex();
                setValueAt(pir.formatMiscParameters(), row, ParameterIrSignalColumns.posMiscParameters);

                //setValueAt((int)value, row, colPos);
            }
        }

        @Override
        public Command toCommand(int row) throws IrpMasterException {
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
    }
}
