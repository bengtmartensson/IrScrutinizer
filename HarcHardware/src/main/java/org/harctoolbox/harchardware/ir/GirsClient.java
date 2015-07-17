/*
Copyright (C) 2015 Bengt Martensson.

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
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import org.harctoolbox.IrpMaster.DecodeIR;
import org.harctoolbox.IrpMaster.IncompatibleArgumentException;
import org.harctoolbox.IrpMaster.IrSequence;
import org.harctoolbox.IrpMaster.IrSignal;
import org.harctoolbox.IrpMaster.IrpMasterException;
import org.harctoolbox.IrpMaster.IrpUtils;
import org.harctoolbox.IrpMaster.ModulatedIrSequence;
import org.harctoolbox.harchardware.HarcHardwareException;
import org.harctoolbox.harchardware.IHarcHardware;
import org.harctoolbox.harchardware.IStringCommand;
import org.harctoolbox.harchardware.comm.LocalSerialPort;
import org.harctoolbox.harchardware.comm.LocalSerialPortBuffered;

/**
 *
 */
public class GirsClient  implements IHarcHardware, IReceive, IRawIrSender, IRawIrSenderRepeat, IRemoteCommandIrSender, IIrSenderStop, ITransmitter, ICapture, IStringCommand {
    private String version;
    private List<String> modules;
    private LocalSerialPortBuffered hardware; // FIXME
    private boolean verbosity;
    private int debug;
    private boolean useReceiveForCapture;
    private String lineEnding;
    private int beginTimeout;
    private int maxCaptureLength;
    private int endingTimeout;
    private int serialTimeout = 10000;
    private int fallbackFrequency = (int) IrpUtils.defaultFrequency;
    private boolean stopRequested = false;
    private boolean pendingCapture = false;

    private final static int defaultBeginTimeout = 5000;
    private final static int defaultMiddleTimeout = 1000;
    private final static int defaultEndingTimeout = 500;
    private final static int defaultSerialTimeout = 10000;
    private final static String sendCommand = "send";
    private final static String captureCommand = "analyze";
    private final static String receiveCommand = "receive";
    private final static String versionCommand = "version";
    private final static String modulesCommand = "modules";
    private final static String resetCommand = "Reset"; // FIXME
    private final static String ledCommand = "led";
    private final static String lcdCommand = "LCD"; // FIXME
    private final static String okString = "OK";
    private final static String timeoutString = ".";
    private final static String separator = " ";
    public final static int defaultBaudRate = 115200;
    private final static int dataSize = 8;
    private final static int stopBits = 1;
    private final static LocalSerialPort.Parity parity = LocalSerialPort.Parity.NONE;
    private final static LocalSerialPort.FlowControl defaultFlowControl = LocalSerialPort.FlowControl.NONE;


    public GirsClient(LocalSerialPortBuffered hardware) throws HarcHardwareException, IOException {
        this.debug = 0;
        this.hardware = hardware;
        this.useReceiveForCapture = false;
    }

    public void close() throws IOException {
        hardware.close();
    }

    public String getVersion() throws IOException {
        return version;
    }

    public void setVerbosity(boolean verbosity) {
        this.verbosity = verbosity;
    }

    public void setDebug(int debug) {
        this.debug = debug;
    }

    @Deprecated
    public void setTimeout(int timeout) throws IOException {
        setBeginTimeout(timeout);
    }

    @Override
    public void setTimeout(int beginTimeout, int maxCaptureLength, int endingTimeout) throws IOException {
        setBeginTimeout(beginTimeout);
        setMaxCaptureLength(maxCaptureLength);
        setEndingTimeout(endingTimeout);
    }

    /**
     * @return the beginTimeout
     */
    public int getBeginTimeout() {
        return beginTimeout;
    }

    /**
     * @param beginTimeout the beginTimeout to set
     */
    public void setBeginTimeout(int beginTimeout) {
        this.beginTimeout = beginTimeout;
    }

    /**
     * @return the maxCaptureLength
     */
    public int getMaxCaptureLength() {
        return maxCaptureLength;
    }

    /**
     * @param maxCaptureLength the maxCaptureLength to set
     */
    public void setMaxCaptureLength(int maxCaptureLength) {
        this.maxCaptureLength = maxCaptureLength;
    }

    /**
     * @return the endingTimeout
     */
    public int getEndingTimeout() {
        return endingTimeout;
    }

    /**
     * @param endingTimeout the endingTimeout to set
     */
    public void setEndingTimeout(int endingTimeout) {
        this.endingTimeout = endingTimeout;
    }

    /**
     * @param lineEnding the lineEnding to set
     */
    public void setLineEnding(String lineEnding) {
        this.lineEnding = lineEnding;
    }

    public boolean isValid() {
        return hardware.isValid() && version != null && modules != null && modules.contains("base");
    }

    public List<String> getModules() {
        return modules;
    }

    private boolean checkModule(String module) {
        return modules.contains(module.toLowerCase(Locale.US));
    }

    /**
     * @param fallbackFrequency the fallbackFrequency to set
     */
    public void setFallbackFrequency(int fallbackFrequency) {
        this.fallbackFrequency = fallbackFrequency;
    }

    @Override
    public synchronized boolean sendIr(IrSignal irSignal, int count, Transmitter transmitter) throws NoSuchTransmitterException, IrpMasterException, IOException {
        String payload = formatSendString(irSignal, count);
        hardware.sendString(payload + lineEnding);
        if (verbosity)
            System.err.println(payload);
        String response = hardware.readString();
        return response != null && response.trim().equals(okString);
    }

    public GirsClient(String portName) throws NoSuchPortException, PortInUseException, UnsupportedCommOperationException, IOException, HarcHardwareException {
        this(portName, defaultBaudRate, defaultBeginTimeout, defaultMiddleTimeout, defaultEndingTimeout, false);
    }

    public GirsClient(String portName, int baudRate, boolean verbose) throws NoSuchPortException, PortInUseException, UnsupportedCommOperationException, IOException, HarcHardwareException {
        this(portName, baudRate, defaultBeginTimeout, defaultMiddleTimeout, defaultEndingTimeout, verbose);
    }

    public GirsClient(String portName, int baudRate, int beginTimeout, boolean verbose) throws NoSuchPortException, PortInUseException, UnsupportedCommOperationException, IOException, HarcHardwareException {
        this(portName, baudRate, beginTimeout, defaultMiddleTimeout, defaultEndingTimeout, verbose);
    }

    public GirsClient(String portName, int beginTimeout, int middleTimeout, int endingTimeout, boolean verbose) throws NoSuchPortException, PortInUseException, UnsupportedCommOperationException, IOException, HarcHardwareException {
        this(portName, defaultBaudRate, beginTimeout, defaultMiddleTimeout, defaultEndingTimeout, verbose);
    }

    public GirsClient(String portName, int baudRate, int beginTimeout, int middleTimeout, int endingTimeout, boolean verbose)
            throws NoSuchPortException, PortInUseException, UnsupportedCommOperationException, IOException, HarcHardwareException {
        this(new LocalSerialPortBuffered(portName, baudRate, dataSize, stopBits,
                parity, defaultFlowControl, defaultSerialTimeout, verbose));
        //super(LocalSerialPortBuffered.class, portName, baudRate, dataSize, stopBits, parity, defaultFlowControl, serialTimeout, verbose);
    }

    public void open() throws IOException, HarcHardwareException {
        hardware.open();
        hardware.waitFor(okString, lineEnding, /*delay*/ 100, /* tries = */ 10);
        hardware.sendString(versionCommand + lineEnding);
        version = hardware.readString(true).trim();
        if (verbosity)
            System.err.println(versionCommand + " returned '" + version + "'.");
        hardware.sendString(modulesCommand + lineEnding);
        String line = hardware.readString(true);
        if (verbosity)
            System.err.println(versionCommand + " returned '" + version + "'.");
        if (line != null)
            modules = Arrays.asList(line.toLowerCase(Locale.US).split("\\s+"));
    }

    private StringBuilder join(IrSequence irSequence, String separator) {
        if (irSequence == null || irSequence.isEmpty())
            return new StringBuilder();

        StringBuilder str = new StringBuilder();
        for (int i = 0; i < irSequence.getLength(); i++)
            str.append(separator).append(Integer.toString(irSequence.iget(i)));
        return str;
    }

    private String formatSendString(IrSignal irSignal, int count) {
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
    public ModulatedIrSequence capture() throws IOException, HarcHardwareException {
        if (stopRequested) // ???
            return null;
        if (!isValid())
            throw new HarcHardwareException("Port not initialized");
        if (!pendingCapture) {
            hardware.sendString((useReceiveForCapture ? receiveCommand : captureCommand) + lineEnding);
            pendingCapture = true;
        }
        ModulatedIrSequence seq = null;
        try {
            //open();
            String str = hardware.readString(true);
            pendingCapture = false;
            if (str == null || str.length() == 0 || str.startsWith("null"))
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

    public void reset() {
        hardware.dropDTR(100);
    }

    @Override
    public void sendString(String cmd) throws IOException {
        hardware.sendString(cmd);
    }

    @Override
    public String readString() throws IOException {
        return hardware.readString();
    }

    public String readString(boolean wait) throws IOException {
        return hardware.readString(wait);
    }

    @Override
    public boolean ready() throws IOException {
        return hardware.ready();
    }

    public void flushInput() throws IOException {
        hardware.flushInput();
    }

    public Transmitter getTransmitter() {
        throw new UnsupportedOperationException("Not supported yet."); // TODO
    }

    public boolean stopIr(Transmitter transmitter) throws NoSuchTransmitterException, IOException {
        throw new UnsupportedOperationException("Not supported yet."); // TODO
    }

    public Transmitter getTransmitter(String connector) throws NoSuchTransmitterException {
        throw new UnsupportedOperationException("Not supported yet."); // TODO
    }

    public String[] getTransmitterNames() {
        throw new UnsupportedOperationException("Not supported yet."); // TODO
    }

    public boolean sendIrRepeat(IrSignal irSignal, Transmitter transmitter) throws NoSuchTransmitterException, IOException, IrpMasterException {
        throw new UnsupportedOperationException("Not supported yet."); // TODO
    }

    public String[] getRemotes() throws IOException {
        throw new UnsupportedOperationException("Not supported yet."); // TODO (later)
    }

    public String[] getCommands(String remote) throws IOException {
        throw new UnsupportedOperationException("Not supported yet."); // TODO (later)
    }

    public boolean sendIrCommand(String remote, String command, int count, Transmitter transmitter) throws IOException, NoSuchTransmitterException {
        throw new UnsupportedOperationException("Not supported yet."); //TODO (later)
    }

    public boolean sendIrCommandRepeat(String remote, String command, Transmitter transmitter) throws IOException, NoSuchTransmitterException {
        throw new UnsupportedOperationException("Not supported yet."); // TODO (later)
    }

    public void setLed(int led, boolean state) throws IOException {
        hardware.sendString(ledCommand + separator + led + separator + (state ? "1" : "0"));
    }

    public void setLed(int led, int flashTime) {
        // TODO
    }

    public void setLcd(String message) {
        // TODO
    }

    public void setLcd(String message, int x, int y) {
        // TODO
    }

    public void setLcdBacklight(boolean state) {
        // TODO
    }

    public void setLcdBacklight(int flashTime) {
        // TODO
    }

    /**
     * Just for testing.
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        String portName = "/dev/arduino";
        GirsClient w = null;
        boolean verbose = true;
        try {
            w = new GirsClient(portName, 10000, verbose);
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
        System.exit(IrpUtils.exitSuccess);
    }
}
