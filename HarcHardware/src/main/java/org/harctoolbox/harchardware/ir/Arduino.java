/*
Copyright (C) 2013, 2014 Bengt Martensson.

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
import org.harctoolbox.IrpMaster.DecodeIR;
import org.harctoolbox.IrpMaster.IncompatibleArgumentException;
import org.harctoolbox.IrpMaster.IrSequence;
import org.harctoolbox.IrpMaster.IrSignal;
import org.harctoolbox.IrpMaster.IrpMasterException;
import org.harctoolbox.IrpMaster.ModulatedIrSequence;
import org.harctoolbox.harchardware.HarcHardwareException;
import org.harctoolbox.harchardware.IStringCommand;
import org.harctoolbox.harchardware.comm.LocalSerialPort;
import org.harctoolbox.harchardware.comm.LocalSerialPortBuffered;

/**
 * This class supports sending and capturing from an Arduino connected through a (virtual) serial port.
 * For sending, the sketch scrutinize_sender should be running, for receiving the sketch scrutinize_receiver.
 */
public class Arduino extends IrSerial<LocalSerialPortBuffered> implements IRawIrSender, ICapture, IReceive, IStringCommand {

    //private int beginTimeout;
    //private int middleTimeout;
    //private int endingTimeout;
    private final static int defaultBeginTimeout = 5000;
    private final static int defaultMiddleTimeout = 1000;
    private final static int defaultEndingTimeout = 500;
    //private LocalSerialPortBuffered localSerialPort = null;
    private static final String sendCommand = "send";
    private static final String captureCommand = "analyze";
    private static final String versionCommand = "version";
    public static final String defaultPortName = "/dev/ttyACM0";
    public static final String okString = "OK";
    public static final String timeoutString = ".";
    private String lineEnding = "\r";
    private static final String separator = " ";
    //private String portName;
    public static final int defaultBaudRate = 115200;
    private static final int dataSize = 8;
    private static final int stopBits = 1;
    private double fallbackFrequency = 38000;
    private static final LocalSerialPort.Parity parity = LocalSerialPort.Parity.NONE;
    private static final LocalSerialPort.FlowControl defaultFlowControl = LocalSerialPort.FlowControl.NONE;
    private boolean stopRequested = false;
    private String versionString = "n/a";
    private final static int serialTimeout = 12345;
    private boolean pendingCapture = false;

    /**
     * @param lineEnding the lineEnding to set
     */
    public void setLineEnding(String lineEnding) {
        this.lineEnding = lineEnding;
    }

    /**
     * @param fallbackFrequency the fallbackFrequency to set
     */
    public void setFallbackFrequency(double fallbackFrequency) {
        this.fallbackFrequency = fallbackFrequency;
    }

    @Override
    public synchronized boolean sendIr(IrSignal irSignal, int count, Transmitter transmitter) throws NoSuchTransmitterException, IrpMasterException, IOException {
        String payload = formatString(irSignal, count);
        serialPort.sendString(payload + lineEnding);
        if (verbose)
            System.err.println(payload);
        String response = serialPort.readString();
        return response != null && response.trim().equals(okString);
    }

    public Arduino() throws NoSuchPortException, PortInUseException, UnsupportedCommOperationException, IOException {
        this(defaultPortName, defaultBaudRate, defaultBeginTimeout, defaultMiddleTimeout, defaultEndingTimeout, false);
    }

    public Arduino(String portName) throws NoSuchPortException, PortInUseException, UnsupportedCommOperationException, IOException {
        this(portName, defaultBaudRate, defaultBeginTimeout, defaultMiddleTimeout, defaultEndingTimeout, false);
    }

    public Arduino(String portName, int baudRate, boolean verbose) throws NoSuchPortException, PortInUseException, UnsupportedCommOperationException, IOException {
        this(portName, baudRate, defaultBeginTimeout, defaultMiddleTimeout, defaultEndingTimeout, verbose);
    }

    public Arduino(String portName, int baudRate, int beginTimeout, boolean verbose) throws NoSuchPortException, PortInUseException, UnsupportedCommOperationException, IOException {
        this(portName, baudRate, beginTimeout, defaultMiddleTimeout, defaultEndingTimeout, verbose);
    }

    public Arduino(String portName, int beginTimeout, int middleTimeout, int endingTimeout, boolean verbose) throws NoSuchPortException, PortInUseException, UnsupportedCommOperationException, IOException {
        this(portName, defaultBaudRate, beginTimeout, defaultMiddleTimeout, defaultEndingTimeout, verbose);
    }

    public Arduino(String portName, int baudRate, int beginTimeout, int middleTimeout, int endingTimeout, boolean verbose)
            throws NoSuchPortException, PortInUseException, UnsupportedCommOperationException, IOException {
        super(LocalSerialPortBuffered.class, portName, baudRate, dataSize, stopBits, parity, defaultFlowControl, serialTimeout, verbose);
    }

    @Override
    public void open() throws IOException, HarcHardwareException {
        super.open();
        serialPort.waitFor(okString, lineEnding, /*delay*/ 100, /* tries = */ 10);
        serialPort.sendString(versionCommand + lineEnding);
        versionString = serialPort.readString(true).trim();
        if (verbose)
            System.err.println(versionCommand + " returned '" + versionString + "'.");
    }

    private StringBuilder join(IrSequence irSequence, String separator) {
        if (irSequence == null || irSequence.isEmpty())
            return new StringBuilder();

        StringBuilder str = new StringBuilder();
        for (int i = 0; i < irSequence.getLength(); i++)
            str.append(separator).append(Integer.toString(irSequence.iget(i)));
        return str;
    }

    private String formatString(IrSignal irSignal, int count) {
        if (irSignal == null)
            throw new IllegalArgumentException("irSignal cannot be null");
        StringBuilder str = new StringBuilder(sendCommand);
        str.append(separator).append(Integer.toString(count));
        str.append(separator).append(Integer.toString((int) irSignal.getFrequency()));
        str.append(separator).append(Integer.toString(2 * irSignal.getIntroBursts()));
        str.append(separator).append(Integer.toString(2 * irSignal.getRepeatBursts()));
        str.append(separator).append(Integer.toString(2 * irSignal.getEndingBursts()));

        str.append(join(irSignal.getIntroSequence(), separator));
        str.append(join(irSignal.getRepeatSequence(), separator));
        str.append(join(irSignal.getEndingSequence(), separator));

        return str.toString();
    }

    @Override
    public String getVersion() /*throws IOException*/ {
        return versionString;
    }

    @Override
    public ModulatedIrSequence capture() throws IOException, HarcHardwareException {
        if (stopRequested) // ???
            return null;
        if (!isValid())
            throw new HarcHardwareException("Port not initialized");
        if (!pendingCapture) {
            serialPort.sendString(captureCommand + lineEnding);
            pendingCapture = true;
        }
        ModulatedIrSequence seq = null;
        try {
            //open();
            String str = serialPort.readString(true);
            pendingCapture = false;
            if (str == null || str.length() == 0 || str.startsWith("null") || str.startsWith(timeoutString))
                return null;

            str = str.trim();

            double frequency = fallbackFrequency;
            if (str.startsWith("f=")) {
                int indx = str.indexOf(' ');
                frequency = Integer.parseInt(str.substring(2, indx));
                str = str.substring(indx + 1);
            }
            seq = new ModulatedIrSequence(new IrSequence(str), frequency, -1.0);
        } catch (IOException ex) {
            //close();
            if (ex.getMessage().equals("Underlying input stream returned zero bytes")) //RXTX timeout
                return null;
            throw ex;
        } catch (IncompatibleArgumentException ex) {
            //close();
            throw new HarcHardwareException(ex);
        }
        //close();
        return seq;
    }

    @Override
    public boolean stopCapture() {
        stopRequested = true;
        return true;
    }

    @Override
    public IrSequence receive() throws HarcHardwareException, IOException, IrpMasterException {
        throw new UnsupportedOperationException("Not supported yet.");// TODO
    }

    @Override
    public boolean stopReceive() {
        throw new UnsupportedOperationException("Not supported yet.");// TODO
    }

    @Override
    public void setTimeout(int beginTimeout, int middleTimeout, int endingTimeout) throws IOException {
        setTimeout(beginTimeout);
        //this.middleTimeout = middleTimeout;
        //this.endingTimeout = endingTimeout;
    }

    public void reset() {
        serialPort.dropDTR(100);
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        String portName = defaultPortName;
        Arduino w = null;
        boolean verbose = true;
        try {
            w = new Arduino(portName, 10000, verbose);
            w.open();
            System.out.println(w.getVersion());
            ModulatedIrSequence seq = w.capture();
            if (seq == null) {
                System.err.println("No input");
                w.close();
                System.exit(1);
            }
            System.out.println(seq);
            DecodeIR.invoke(seq);
        } catch (IOException ex) {
            System.err.println("exception: " + ex.toString() + ex.getMessage());
            //ex.printStackTrace();
        } catch (NoSuchPortException ex) {
            System.err.println("No such port: " + portName);
        } catch (HarcHardwareException ex) {
            System.err.println(ex.getMessage());
        } catch (PortInUseException ex) {
            System.err.println("Port " + portName + " in use.");
        } catch (UnsupportedCommOperationException ex) {
            System.err.println(ex.getMessage());
        } finally {
            if (w != null)
                try {
                    w.close();
                } catch (IOException ex) {
                }
        }
        System.exit(0);
    }

    @Override
    public void sendString(String cmd) throws IOException {
        serialPort.sendString(cmd);
    }

    @Override
    public String readString() throws IOException {
        return serialPort.readString();
    }

    public String readString(boolean wait) throws IOException {
        return serialPort.readString(wait);
    }

    @Override
    public boolean ready() throws IOException {
        return serialPort.ready();
    }
}
