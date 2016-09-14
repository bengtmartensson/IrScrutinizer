/*
Copyright (C) 2015, 2016 Bengt Martensson.

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

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.harctoolbox.IrpMaster.DecodeIR;
import org.harctoolbox.IrpMaster.IncompatibleArgumentException;
import org.harctoolbox.IrpMaster.IrSequence;
import org.harctoolbox.IrpMaster.IrSignal;
import org.harctoolbox.IrpMaster.IrpMasterException;
import org.harctoolbox.IrpMaster.IrpUtils;
import org.harctoolbox.IrpMaster.ModulatedIrSequence;
import org.harctoolbox.harchardware.HarcHardwareException;
import org.harctoolbox.harchardware.ICommandLineDevice;
import org.harctoolbox.harchardware.IHarcHardware;
import org.harctoolbox.harchardware.comm.LocalSerialPort;
import org.harctoolbox.harchardware.comm.LocalSerialPortBuffered;
import org.harctoolbox.harchardware.comm.TcpSocketPort;

/**
 *
 * @param <T>
 */
public class GirsClient<T extends ICommandLineDevice & IHarcHardware>  implements IHarcHardware, IReceive, IRawIrSender, IRawIrSenderRepeat, IRemoteCommandIrSender, IIrSenderStop, ITransmitter, ICapture, ICommandLineDevice {

    private final static String defaultLineEnding = "\r";
    private final static String sendCommand = "send";
    private final static String captureCommand = "analyze";
    private final static String receiveCommand = "receive";
    private final static String versionCommand = "version";
    private final static String modulesCommand = "modules";
    private final static String resetCommand = "reset";
    private final static String ledCommand = "led";
    private final static String lcdCommand = "lcd";
    private final static String okString = "OK";
    private final static String timeoutString = ".";
    private final static String separator = " ";

    /**
     * Just for testing.
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        //testGirsSerial("/dev/arduino", 115200, true);
        testGirsTcp("arduino", 33333, true);
    }

    private static void testGirsTcp(String ip, int portnumber, boolean verbose) {
        GirsClient<TcpSocketPort> gc = null;
        try {
            TcpSocketPort tcp = new TcpSocketPort(ip, portnumber, verbose, TcpSocketPort.ConnectionMode.keepAlive);
            gc = new GirsClient<>(tcp);
            testGirs(gc);
        } catch (HarcHardwareException | IOException ex) {
            Logger.getLogger(GirsClient.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            if (gc != null)
                try {
                    gc.close();
                } catch (IOException ex) {
                }
        }
    }

//    private static void testGirsSerial(String portName, int baud, boolean verbose) {
//        GirsClient<LocalSerialPortBuffered> w = null;
//        try {
//            w = new GirsClient<>(new LocalSerialPortBuffered(portName, 115200, verbose));
//            testGirs(w);
//        } catch (IOException ex) {
//            System.err.println("exception: " + ex.toString() + ex.getMessage());
//            //ex.printStackTrace();
//        } catch (NoSuchPortException ex) {
//            System.err.println("No such port: " + portName);
//        } catch (HarcHardwareException | UnsupportedCommOperationException ex) {
//            System.err.println(ex.getMessage());
//        } catch (PortInUseException ex) {
//            System.err.println("Port " + portName + " in use.");
//        } finally {
//            if (w != null)
//                try {
//                    w.close();
//                } catch (IOException ex) {
//                }
//        }
//    }

    private static void testGirs(GirsClient<?> gc) {
        try {
            gc.open();
            System.out.println(gc.getVersion());
            if (gc.hasModule("lcd"))
                gc.setLcd("Now send an IR signal");
            //ModulatedIrSequence seq = gc.capture();
            IrSequence irSequence = gc.receive();
            if (irSequence == null) {
                System.err.println("No input");
                gc.close();
                System.exit(1);
            }
            ModulatedIrSequence seq = new ModulatedIrSequence(irSequence, IrpUtils.defaultFrequency);
            System.out.println(seq);
            DecodeIR.invoke(seq);
            try {
                Thread.sleep(2000);
            } catch (InterruptedException ex) {
                Logger.getLogger(GirsClient.class.getName()).log(Level.SEVERE, null, ex);
            }
            gc.close();
        } catch (IOException | HarcHardwareException | IrpMasterException ex) {
            Logger.getLogger(GirsClient.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private String version;
    private List<String> modules;
    private final T hardware;
    private boolean verbose;
    private int debug;
    private boolean useReceiveForCapture;
    private String lineEnding;
    private int beginTimeout;
    private int maxCaptureLength;
    private int endingTimeout;
    private int fallbackFrequency = (int) IrpUtils.defaultFrequency;
    private boolean stopRequested = false;
    private boolean pendingCapture = false;

    public GirsClient(T hardware) throws HarcHardwareException, IOException {
        this.lineEnding = defaultLineEnding;
        this.verbose = false;
        this.hardware = hardware;
        this.useReceiveForCapture = false;
    }

    public void setUseReceiveForCapture(boolean val) {
        this.useReceiveForCapture = val;
    }

    @Override
    public void close() throws IOException {
        hardware.close();
    }

    @Override
    public String getVersion() throws IOException {
        return version;
    }

    @Override
    public void setVerbosity(boolean verbose) {
        hardware.setVerbosity(verbose);
        this.verbose = verbose;
    }

    @Override
    public void setDebug(int debug) {
        this.debug = debug;
    }

    @Deprecated
    @Override
    public void setTimeout(int timeout) throws IOException {
        setBeginTimeout(timeout);
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
    @Override
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
    @Override
    public void setCaptureMaxSize(int maxCaptureLength) {
        this.maxCaptureLength = maxCaptureLength;
    }

    /**
     * @return the endingTimeout
     */
    public int getEndTimeout() {
        return endingTimeout;
    }

    /**
     * @param endingTimeout the endingTimeout to set
     */
    @Override
    public void setEndTimeout(int endingTimeout) {
        this.endingTimeout = endingTimeout;
    }

    /**
     * @param lineEnding the lineEnding to set
     */
    public void setLineEnding(String lineEnding) {
        this.lineEnding = lineEnding;
    }

    @Override
    public boolean isValid() {
        return hardware.isValid() && version != null && modules != null && modules.contains("base");
    }

    public List<String> getModules() {
        return modules;
    }

    public boolean hasModule(String module) {
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
        if (verbose)
            System.err.println(payload);
        String response = hardware.readString(true);
        return response != null && response.trim().equals(okString);
    }

    @Override
    public void open() throws IOException, HarcHardwareException {
        hardware.open();
        waitFor(okString, lineEnding, /*delay*/ 100, /* tries = */ 3);
        hardware.sendString(versionCommand + lineEnding);
        version = hardware.readString(true).trim();
        if (verbose)
            System.err.println(versionCommand + " returned '" + version + "'.");
        hardware.sendString(modulesCommand + lineEnding);
        String line = hardware.readString(true);
        if (verbose)
            System.err.println(versionCommand + " returned '" + version + "'.");
        if (line != null)
            modules = Arrays.asList(line.toLowerCase(Locale.US).split("\\s+"));
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
                String junk = readString(false);
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
    public ModulatedIrSequence capture() throws IOException, HarcHardwareException, IrpMasterException {
        return useReceiveForCapture ? mockModulatedIrSequence() : realCapture();
    }

    private ModulatedIrSequence mockModulatedIrSequence() throws HarcHardwareException, IOException, IrpMasterException {
        IrSequence irSequence = receive();
        return irSequence == null ? null : new ModulatedIrSequence(irSequence, fallbackFrequency);
    }

    private ModulatedIrSequence realCapture() throws HarcHardwareException, IOException {
        if (stopRequested) // ???
            return null;
        if (!isValid())
            throw new HarcHardwareException("Port not initialized");
        if (!pendingCapture) {
            hardware.sendString(captureCommand + lineEnding);
            pendingCapture = true;
        }
        ModulatedIrSequence seq = null;
        try {
            //open();
            String str = hardware.readString(true);
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
        } catch (SocketTimeoutException ex) {
            return null;
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
        if (!pendingCapture) {
            hardware.sendString(receiveCommand + lineEnding);
            pendingCapture = true;
        }

        IrSequence seq = null;
        try {
            //open();
            String str = hardware.readString(true);
            pendingCapture = false;
            if (str == null || str.length() == 0 || str.startsWith("null") || str.startsWith(timeoutString))
                return null;

            str = str.trim();

            //double frequency = fallbackFrequency;
            seq = new IrSequence(str);
        } catch (SocketTimeoutException ex) {
            return null;
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

    public void reset() throws IOException {
        hardware.sendString(resetCommand);
        // ???
        if (hardware instanceof LocalSerialPortBuffered)
            ((LocalSerialPort) hardware).dropDTR(100);
    }

    @Override
    public void sendString(String cmd) throws IOException {
        hardware.sendString(cmd);
    }

    @Override
    public String readString() throws IOException {
        return hardware.readString();
    }

    @Override
    public String readString(boolean wait) throws IOException {
        return hardware.readString(wait);
    }

    @Override
    public boolean ready() throws IOException {
        return hardware.ready();
    }

    @Override
    public void flushInput() throws IOException {
        hardware.flushInput();
    }

    @Override
    public Transmitter getTransmitter() {
        return null; // TODO
    }

    @Override
    public boolean stopIr(Transmitter transmitter) throws NoSuchTransmitterException, IOException {
        throw new UnsupportedOperationException("Not supported yet."); // TODO
    }

    @Override
    public Transmitter getTransmitter(String connector) throws NoSuchTransmitterException {
        throw new UnsupportedOperationException("Not supported yet."); // TODO
    }

    @Override
    public String[] getTransmitterNames() {
        throw new UnsupportedOperationException("Not supported yet."); // TODO
    }

    @Override
    public boolean sendIrRepeat(IrSignal irSignal, Transmitter transmitter) throws NoSuchTransmitterException, IOException, IrpMasterException {
        throw new UnsupportedOperationException("Not supported yet."); // TODO
    }

    @Override
    public String[] getRemotes() throws IOException {
        throw new UnsupportedOperationException("Not supported yet."); // TODO (later)
    }

    @Override
    public String[] getCommands(String remote) throws IOException {
        throw new UnsupportedOperationException("Not supported yet."); // TODO (later)
    }

    @Override
    public boolean sendIrCommand(String remote, String command, int count, Transmitter transmitter) throws IOException, NoSuchTransmitterException {
        throw new UnsupportedOperationException("Not supported yet."); //TODO (later)
    }

    @Override
    public boolean sendIrCommandRepeat(String remote, String command, Transmitter transmitter) throws IOException, NoSuchTransmitterException {
        throw new UnsupportedOperationException("Not supported yet."); // TODO (later)
    }

    public void setLed(int led, boolean state) throws IOException, HarcHardwareException {
        sendStringWaitOk(ledCommand + separator + led + separator + (state ? "1" : "0"));
    }

    public void setLed(int led, int flashTime) {
        // TODO
    }

    public void setLcd(String message) throws IOException, HarcHardwareException {
        sendStringWaitOk(lcdCommand + " " + message);
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

    private void sendStringWaitOk(String line) throws IOException, HarcHardwareException {
        hardware.sendString(line + lineEnding);
        String answer = readString(true);
        if (answer == null)
            throw new HarcHardwareException("No \"" + okString + "\" received.");
        answer = answer.trim();
        if (!answer.startsWith(okString))
            throw new HarcHardwareException("No \"" + okString + "\" received, instead \"" + answer + "\".");
    }
}
