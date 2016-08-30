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
import org.harctoolbox.harchardware.ICommandLineDevice;
import org.harctoolbox.harchardware.comm.LocalSerialPort;
import org.harctoolbox.harchardware.comm.LocalSerialPortBuffered;

/**
 * This class supports sending and capturing from an Arduino connected through a (virtual) serial port.
 * For sending, the sketch scrutinize_sender should be running, for receiving the sketch scrutinize_receiver.
 */
public class Arduino extends IrSerial<LocalSerialPortBuffered> implements IRawIrSender, ICapture, IReceive, ICommandLineDevice {

    //private int beginTimeout;
    //private int middleTimeout;
    //private int endingTimeout;
    //private LocalSerialPortBuffered localSerialPort = null;
    private static final String sendCommand = "send";
    private static final String captureCommand = "analyze";
    private static final String versionCommand = "version";
    public static final String defaultPortName = "/dev/ttyACM0";
    public static final String okString = "OK";
    public static final String timeoutString = ".";
    private static final String separator = " ";
    //private String portName;
    public static final int defaultBaudRate = 115200;
    private static final int dataSize = 8;
    private static final int stopBits = 1;
    private static final LocalSerialPort.Parity parity = LocalSerialPort.Parity.NONE;
    private static final LocalSerialPort.FlowControl defaultFlowControl = LocalSerialPort.FlowControl.NONE;
    private final static int serialTimeout = 12345;
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
        } catch (HarcHardwareException | UnsupportedCommOperationException ex) {
            System.err.println(ex.getMessage());
        } catch (PortInUseException ex) {
            System.err.println("Port " + portName + " in use.");
        } finally {
            if (w != null)
                try {
                    w.close();
                } catch (IOException ex) {
                }
        }
        System.exit(0);
    }
    private String lineEnding = "\r";
    private double fallbackFrequency = 38000;
    private boolean stopRequested = false;
    private String versionString = "n/a";
    private boolean pendingCapture = false;


    public Arduino() throws NoSuchPortException, PortInUseException, UnsupportedCommOperationException, IOException {
        this(defaultPortName, defaultBaudRate, defaultBeginTimeout, defaultCaptureMaxSize, defaultEndTimeout, false);
    }

    public Arduino(String portName) throws NoSuchPortException, PortInUseException, UnsupportedCommOperationException, IOException {
        this(portName, defaultBaudRate, defaultBeginTimeout, defaultCaptureMaxSize, defaultEndTimeout, false);
    }

    public Arduino(String portName, int baudRate) throws NoSuchPortException, PortInUseException, UnsupportedCommOperationException, IOException {
        this(portName, baudRate, defaultBeginTimeout, defaultCaptureMaxSize, defaultEndTimeout, false);
    }

    public Arduino(String portName, int baudRate, boolean verbose) throws NoSuchPortException, PortInUseException, UnsupportedCommOperationException, IOException {
        this(portName, baudRate, defaultBeginTimeout, defaultCaptureMaxSize, defaultEndTimeout, verbose);
    }

    public Arduino(String portName, int baudRate, int beginTimeout, boolean verbose) throws NoSuchPortException, PortInUseException, UnsupportedCommOperationException, IOException {
        this(portName, baudRate, beginTimeout, defaultCaptureMaxSize, defaultEndTimeout, verbose);
    }

    public Arduino(String portName, int beginTimeout, int captureMaxSize, int endingTimeout, boolean verbose) throws NoSuchPortException, PortInUseException, UnsupportedCommOperationException, IOException {
        this(portName, defaultBaudRate, beginTimeout, captureMaxSize, defaultEndTimeout, verbose);
    }

    public Arduino(String portName, int baudRate, int beginTimeout, int captureMaxSize, int endingTimeout, boolean verbose)
            throws NoSuchPortException, PortInUseException, UnsupportedCommOperationException, IOException {
        super(LocalSerialPortBuffered.class, portName, baudRate, dataSize, stopBits, parity, defaultFlowControl, serialTimeout, verbose);
    }
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
        String response = serialPort.readString(true);
        return response != null && response.trim().equals(okString);
    }

    @Override
    public void open() throws IOException, HarcHardwareException {
        super.open();
        waitFor(okString, lineEnding, /*delay*/ 100, /* tries = */ 10);
        serialPort.sendString(versionCommand + lineEnding);
        versionString = serialPort.readString(true).trim();
        if (verbose)
            System.err.println(versionCommand + " returned '" + versionString + "'.");
    }

    public void waitFor(String goal, String areUThere, int delay, int tries) throws IOException, HarcHardwareException {
        try {
            Thread.sleep(delay);
        } catch (InterruptedException ex) {
            // nothing
        }
        flushIn();
        for (int i = 0; i < tries; i++) {
            sendString(areUThere);
            String answer = readString(true);
            if (answer == null)
                continue;
            answer = answer.trim();
            if (answer.startsWith(goal)) {// success!
                flushIn();
                return;
            }
            if (delay > 0)
                try {
                    Thread.sleep(delay);
                } catch (InterruptedException ex) {
                    break;
                }
        }
        // Failure if we get here.
        throw new HarcHardwareException("Hardware not responding");
    }

    private void flushIn() /*throws IOException*/ {
        try {
            while (true) {
                String junk = readString();
                if (junk == null)
                    break;
                if (verbose)
                    System.err.println("LocalSerialPortBuffered.flushIn: junked '" + junk + "'.");
            }
        } catch (IOException ex) {
            // This bizarre code actually both seems to work, and be needed (at least using my Mega2560),
            // the culprit is probably rxtx.
             if (verbose)
                    System.err.println("IOException in LocalSerialPortBuffered.flushIn ignored: " + ex.getMessage());
        }
    }

    private StringBuilder join(IrSequence irSequence, String separator) {
        if (irSequence == null || irSequence.isEmpty())
            return new StringBuilder(0);

        StringBuilder str = new StringBuilder(128);
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
    public void setBeginTimeout(int beginTimeout) throws IOException {
    }

    @Override
    public void setCaptureMaxSize(int captureMaxSize) {
    }

    @Override
    public void setEndTimeout(int endTimeout) {
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
                if (indx < 0)
                    return null;
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
        if (stopRequested) // ???
            return null;
        if (!isValid())
            throw new HarcHardwareException("Port not initialized");
        //if (!pendingCapture) {
        //    serialPort.sendString(captureCommand + lineEnding);
        //    pendingCapture = true;
        //}
        IrSequence seq = null;
        try {
            //open();
            String str = serialPort.readString(true);
            //pendingCapture = false;
            if (str == null || str.length() == 0 || str.startsWith("null") || str.startsWith(timeoutString))
                return null;

            str = str.trim();

            //double frequency = fallbackFrequency;
            seq = new IrSequence(str);
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
    public boolean stopReceive() {
        throw new UnsupportedOperationException("Not supported yet.");// TODO
    }

    public void reset() {
        serialPort.dropDTR(100);
    }


    @Override
    public void sendString(String cmd) throws IOException {
        serialPort.sendString(cmd);
    }

    @Override
    public String readString() throws IOException {
        return serialPort.readString();
    }

    @Override
    public String readString(boolean wait) throws IOException {
        return serialPort.readString(wait);
    }

    @Override
    public boolean ready() throws IOException {
        return serialPort.ready();
    }

    @Override
    public void setDebug(int debug) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void flushInput() throws IOException {
        serialPort.flushInput();
    }
}
