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

package org.harctoolbox.harchardware.ir;

import gnu.io.NoSuchPortException;
import gnu.io.PortInUseException;
import gnu.io.UnsupportedCommOperationException;
import java.io.IOException;
import org.harctoolbox.IrpMaster.IncompatibleArgumentException;
import org.harctoolbox.IrpMaster.IrSignal;
import org.harctoolbox.IrpMaster.IrpMasterException;
import org.harctoolbox.IrpMaster.ModulatedIrSequence;
import org.harctoolbox.harchardware.comm.LocalSerialPort;
import org.harctoolbox.harchardware.comm.LocalSerialPortBuffered;

/**
 * This class models a serial device that takes text commands from a serial port, like the Arduino.
 */
public class IrGenericSerial extends IrSerial<LocalSerialPortBuffered> implements IRawIrSender {

    private String command;
    private boolean useSigns;
    private String separator;
    private String lineEnding;
    private boolean raw;

    public IrGenericSerial(String portName, int baudRate, int dataSize, int stopBits, LocalSerialPort.Parity parity,
            LocalSerialPort.FlowControl flowControl, int timeout, boolean verbose)
            throws NoSuchPortException, PortInUseException, UnsupportedCommOperationException, IOException {
        super(LocalSerialPortBuffered.class, portName, baudRate, dataSize, stopBits, parity, flowControl, timeout, verbose);
    }

    /**
     * @param command the command to set
     */
    public void setCommand(String command) {
        this.command = command;
    }

    /**
     * @param useSigns the useSigns to set
     */
    public void setUseSigns(boolean useSigns) {
        this.useSigns = useSigns;
    }

    /**
     * @param separator the separator to set
     */
    public void setSeparator(String separator) {
        this.separator = separator;
    }

    /**
     * @param lineEnding the lineEnding to set
     */
    public void setLineEnding(String lineEnding) {
        this.lineEnding = lineEnding;
    }

    /**
     * @param raw the raw to set
     */
    public void setRaw(boolean raw) {
        this.raw = raw;
    }

    @Override
    public synchronized boolean sendIr(IrSignal irSignal, int count, Transmitter transmitter) throws NoSuchTransmitterException, IrpMasterException, IOException {
        String payload = formatString(irSignal, count);
        serialPort.sendString(payload);
        if (verbose)
            System.err.print(payload);
        return true;
    }


    private String formatString(IrSignal irSignal, int count) {
        if (irSignal == null)
            throw new IllegalArgumentException("irSignal cannot be null");
        StringBuilder str = new StringBuilder(command);
        str.append(" ");
        ModulatedIrSequence seq = irSignal.toModulatedIrSequence(count);
        if (raw) {
            str.append(seq.toPrintString(useSigns, !useSigns, separator));
        } else {
            try {
                IrSignal signal = new IrSignal(seq.getFrequency(), seq.getDutyCycle(), seq, null, null);
                str.append(signal.ccfString());
            } catch (IncompatibleArgumentException ex) {
                // should not happen, really
                throw new RuntimeException("Silly IrSignal/CCF");
            }
        }
        str.append(lineEnding);
        return str.toString();
    }

    @Override
    public void setDebug(int debug) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
