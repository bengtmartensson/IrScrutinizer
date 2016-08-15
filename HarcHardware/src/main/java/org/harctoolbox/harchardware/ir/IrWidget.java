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

import gnu.io.CommPort;
import gnu.io.CommPortIdentifier;
import gnu.io.NoSuchPortException;
import gnu.io.PortInUseException;
import gnu.io.RXTXPort;
import gnu.io.SerialPort;
import gnu.io.UnsupportedCommOperationException;
import java.io.IOException;
import java.io.InputStream;
import org.harctoolbox.IrpMaster.DecodeIR;
import org.harctoolbox.IrpMaster.IncompatibleArgumentException;
import org.harctoolbox.IrpMaster.IrSequence;
import org.harctoolbox.IrpMaster.IrpUtils;
import org.harctoolbox.IrpMaster.ModulatedIrSequence;
import org.harctoolbox.harchardware.HarcHardwareException;
import org.harctoolbox.harchardware.IHarcHardware;

/**
 * This class implements support for Kevin Timmerman's Ir Widget.
 * It uses the RXTX library, which encapsulates all system dependencies.
 * Although it duplicates some functionality found in Kevin's program IrScope (file widget.cpp),
 * it is not derived.
 *
 * <a href="http://www.compendiumarcana.com/irwidget/">Original web page</a>.
 */

// Presently, only the "irwidgetPulse" (case 0 in widget.cpp) is to be considered as implemented and tested.
public class IrWidget implements IHarcHardware, ICapture {

    /** Number of micro seconds in a count msPerTick. */
    public static final int msPerTick = 100;
    public static final String defaultPortName = "/dev/ttyUSB0";

    private static final int baudRate = 115200;
    private static final int mask = 0x3F;
    private static final Modes defaultMode = Modes.irwidgetPulse;
    private static final int shortDelay = 20;
    private static final int longDelay = 200;

    // I hate this "nobody needs or understands unsigned" by Gosling...
    private static int toIntAsUnsigned(byte b) {
        return b >= 0 ? b : b + 256;
    }

    /**
     * For testing purposes only.
     *
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        try (IrWidget w = new IrWidget()) {
            w.open();
            ModulatedIrSequence seq = w.capture();
            System.out.println(seq);
            if (seq == null) {
                System.err.println("No input");
            } else {
                DecodeIR.invoke(seq);
            }
        } catch (IOException ex) {
            System.err.println("exception: " + ex.toString() + ex.getMessage());
        } catch (HarcHardwareException ex) {
            System.err.println(ex.getMessage());
        }
    }

    private CommPortIdentifier portIdentifier;
    private CommPort commPort;
    private Modes mode;
    private String portName;
    private int debug;
    private byte[] data;
    private int[] times;
    private int dataLength;
    private double frequency;
    private boolean stopRequested;
    private boolean verbose;
    private int beginTimeout;
    private int captureMaxSize;
    private int endTimeout;

     /**
     * Constructs new IrWidget with default port name and timeouts.
     *
     */
    public IrWidget() {
        this(defaultPortName, false, 0);
    }

    /**
     * Constructs new IrWidget with default timeouts.
     *
     * @param portName Name of serial port to use. Typically something like COM7: (Windows) or /dev/ttyUSB0.
     * @param verbose
     * @param debug debug code
     */
    public IrWidget(String portName, boolean verbose, int debug) {
        this(portName, defaultMode, defaultBeginTimeout, defaultCaptureMaxSize, defaultEndTimeout, verbose, debug);
    }

    /**
     * Constructs new IrWidget.
     * @param portName Name of serial port to use. Typically something like COM7: (Windows) or /dev/ttyUSB0.
     * @param verbose
     * @param startTimeout
     * @param runTimeout
     * @param endTimeout
     */
    public IrWidget(String portName, int startTimeout, int runTimeout, int endTimeout, boolean verbose) {
        this(portName, defaultMode, startTimeout, runTimeout, endTimeout, verbose, 0);
    }

    /**
     * Constructs new IrWidget.
     * @param portName Name of serial port to use. Typically something like COM7: (Windows) or /dev/ttyUSB0.
     * @param mode Hardware mode.
     * @param verbose
     * @param debug debug code
     * @param startTimeout
     * @param runTimeout
     * @param endTimeout
     */
    private IrWidget(String portName, Modes mode, int beginTimeout, int captureMaxSize, int endTimeout, boolean verbose, int debug) {
        this.mode = mode;
        this.portName = portName;
        this.debug = debug;
        this.verbose = verbose;
        this.beginTimeout = beginTimeout;
        this.captureMaxSize = captureMaxSize;
        this.endTimeout = endTimeout;
    }
    @Override
    public void setDebug(int debug) {
    }

    @Override
    public void open() throws HarcHardwareException, IOException {
        try {
            portIdentifier = CommPortIdentifier.getPortIdentifier(portName);
        } catch (NoSuchPortException ex) {
            throw new HarcHardwareException(ex);
        }
        try {
            commPort = portIdentifier.open(getClass().getName(), 2000);
        } catch (PortInUseException ex) {
            throw new HarcHardwareException(ex);
        }

        if (!(commPort instanceof gnu.io.SerialPort))
            throw new RuntimeException("Internal error: " + portName + " not a serial port");
        RXTXPort serialPort = (RXTXPort) commPort;
        try {
            serialPort.setSerialPortParams(baudRate, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);
        } catch (UnsupportedCommOperationException ex) {
            throw new HarcHardwareException(ex);
        }
        serialPort.setFlowControlMode(SerialPort.FLOWCONTROL_NONE);

        serialPort.disableReceiveThreshold();
        //serial.disableReceiveFraming();
        //serialPort.enableReceiveTimeout(5000);
        //serial.setLowLatency();
        serialPort.disableReceiveFraming();
        //serialPort.clearCommInput();
        //setMode(mode);
        //inputStream = commPort.getInputStream();
    }

    @Override
    public void close() {
        portIdentifier = null;
        //unsetMode();
        if (commPort != null)
            commPort.close();
        commPort = null;
    }

    /**
     *
     * @param timeout
     */
    @Override
    public void setTimeout(int timeout) {
        setBeginTimeout(timeout);
    }

    @Override
    public void setBeginTimeout(int timeout) {
        this.beginTimeout = timeout;
    }

    @Override
    public void setCaptureMaxSize(int maxCaptureMaxSize) {
        this.captureMaxSize = maxCaptureMaxSize;
    }

    @Override
    public void setEndTimeout(int endTimeout) {
        this.endTimeout = endTimeout;
    }

    /**
     * Captures a signal using the given timeout values, and returns it as a ModulatedIrSequence.
     *
     * @return ModulatedIrSequence
     * @throws IOException
     */
    @Override
    public ModulatedIrSequence capture() throws IOException {
        int bytesRead = 0;
        ModulatedIrSequence seq = null;
        RXTXPort serialPort = (RXTXPort) commPort;

        setMode(mode);
        try {
            serialPort.clearCommInput();
        } catch (UnsupportedCommOperationException ex) {
            // This is bad...
            throw new RuntimeException(ex);
        }
        InputStream inputStream = serialPort.getInputStream();
        int toRead = (int) Math.round(captureMaxSize * IrpUtils.milliseconds2microseconds / msPerTick);
        data = new byte[toRead];

        byte last = -1;
        long startTime = System.currentTimeMillis();
        long lastEvent = startTime;
        stopRequested = false;
        while (bytesRead < toRead && !stopRequested) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException ex) {
            }
            if (inputStream.available() == 0) {
                if (bytesRead == 0) {
                    if (beginTimeout > 0 && System.currentTimeMillis() - startTime >= beginTimeout)
                        break;
                } else {
                    if (endTimeout > 0 && System.currentTimeMillis() - lastEvent >= endTimeout)
                        break;
                }
            } else {
                int x = inputStream.read(data, bytesRead, toRead - bytesRead);
                bytesRead += x;
                int i = 0;
                while (i < x && data[bytesRead - x + i] == last) {
                    i++;
                }

                if (i == x) {
                    // nothing interesting happened
                    if (System.currentTimeMillis() - lastEvent >= endTimeout)
                        break;
                } else {
                    // something happened
                    lastEvent = System.currentTimeMillis();
                }

                last = data[bytesRead - 1];

                if (debug > 10) {
                    System.out.print(x + "\t" + bytesRead);
                    for (i = 0; i < x; i++) {
                        byte num = data[bytesRead - x + i];
                        System.out.print("\t" + num);
                    }
                    System.out.println();
                }
            }
        }
        boolean success = compute(bytesRead);
        try {
            seq = success ? new ModulatedIrSequence(new IrSequence(times, true), frequency, -1.0) : null;
        } catch (IncompatibleArgumentException ex) {
            System.err.println("Internal error: " + ex.getMessage());
        }

        unsetMode();
        try {
            inputStream.close();
        } catch (IOException ex) {
        }

        return seq;
    }

    /**
     * The IrWidget does not support versions.
     * @return null
     */
    @Override
    public String getVersion() {
        return null;
    }

    @Override
    public void setVerbosity(boolean verbose) {
        this.verbose = verbose;
    }

    @Override
    public boolean isValid() {
        return portIdentifier != null;
    }

    /**
     * Stops ongoing capture.
     * @return true
     */
    @Override
    public boolean stopCapture() {
        if (debug > 0)
            System.err.println("captureStop called");
        stopRequested = true;
        return true;
    }

    private boolean compute(int noBytes) {
        if (noBytes == 0)
            return false;
        return mode.pulse() ? computePulses(noBytes) : computeTimes(noBytes);
    }

    private boolean computeTimes(int noBytes) {
        int offset = 2;
        dataLength = noBytes - offset;
        times = new int[dataLength/2];
        for (int i = 0; i < dataLength/2; i++) {
            int t = toIntAsUnsigned(data[2*i+offset]) | (toIntAsUnsigned(data[2*i+1+offset]) << 8);
            t = ((t&0x8000) != 0) ? (t&0x7FFF) : -t;
            times[i] = t*16;
        }
        return true;
    }

    private boolean computePulses(int noBytes) {
        // replace the existing, incremental data by actual count in the intervals
        for (int i = 0; i < noBytes-1; i++)
            data[i] = (byte)(mask & (data[i+1] - data[i]));
        dataLength = noBytes - 1;

        // Compute frequency
        int periods = 0;
        int bins = 0;
        int pulses = 1;
        int gaps = 0;

        for (int i = 1; i < dataLength; i++) {
            if (i < dataLength - 1 && data[i] > 0 && data[i-1] > 0 && data[i+1] > 0) {
                periods += data[i];
                bins++;
            }
            if (data[i] > 0 && data[i-1] == 0)
                pulses++;
            if (data[i] == 0 && data[i-1] > 0)
                gaps++;
        }

        if (bins == 0)
            return false;

        if (debug > 0)
            System.out.println("IrWidget read pulses = " + pulses + ", gaps = " + gaps);
        frequency = periods/(bins * msPerTick * IrpUtils.microseconds2seconds);

        times = new int[pulses + gaps];
        int index = 0;
        int currentCount = 0;
        int currentGap = 0;
        boolean previousState = false;
        boolean currentState = false;
        for (int i = 0; i < dataLength; i++) {
            currentState = data[i] > 0;
            if (currentState == previousState) {
                if (currentState)
                    currentCount += data[i];
                else
                    currentGap += msPerTick;
            } else {
                if (currentState) { // starting flash
                    currentGap += gapDuration(data[i]);
                    if (index > 0)
                        times[index++] = -currentGap;
                    currentCount = data[i];
                    currentGap = 0;
                } else { // starting gap
                    times[index++] = pulseDuration(currentCount);
                    currentGap = gapDuration(data[i-1]) + msPerTick;
                    currentCount = 0;
                }
            }
            previousState = currentState;
        }
        times[index++] = currentState ? pulseDuration(currentCount) : -currentGap;
        if (debug > 0)
            System.out.println(index + " " + pulses + " " + gaps);
        if (debug > 0)
            System.out.println(index + " " + pulses + " " + gaps);
        return true;
    }

    private int pulseDuration(int pulses) {
        int x = (int) Math.round(pulses/frequency * IrpUtils.seconds2microseconds);
        return x;
    }

    private int gapDuration(int pulses) {
        return msPerTick - pulseDuration(pulses);
    }


    private void setMode(Modes mode) {
        try {
            RXTXPort serial = (RXTXPort) commPort;
            serial.setDTR(false);
            serial.setRTS(false);
            Thread.sleep(shortDelay); // ???
            switch (mode) {
                case irwidgetPulse:            // Kevin's case 0,2
                    serial.setDTR(true);
                    Thread.sleep(longDelay);
                    serial.setRTS(true);
                    break;
                case miniPovPulse:            // Kevin's case 1
                    serial.setRTS(true);
                    Thread.sleep(longDelay);
                    serial.setDTR(true);
                    break;
                case irwidgetTime:            // Kevin's case 3
                    serial.setRTS(true);
                    serial.setDTR(true);
                    break;
                case miniPovTime:            // Kevin's case 4
                    serial.setDTR(true);
                    serial.setRTS(true);
                    break;
                default:
                    // This cannot happen
                    assert (false);
            }
        } catch (InterruptedException ex) {
            System.err.println("Interrupted; likely programming error.");
        }
    }

    private void unsetMode() {
        RXTXPort serial = (RXTXPort) commPort;
        serial.setDTR(false);
        serial.setRTS(false);
    }

    /**
     * Different hardware and different operating modes supported (more-or-less) by the software.
     * Presently, only irwidgetPulse is to be considered as tested.
     */
    private enum Modes {
        irwidgetPulse,
        irwidgetTime,
        miniPovPulse,
        miniPovTime;

        public static boolean pulse(Modes m) {
            return (m == irwidgetPulse) || (m == miniPovPulse);
        }

        public boolean pulse() {
            return (this == irwidgetPulse) || (this == miniPovPulse);
        }

        public static Modes lanidro(int i) {
            int j = 0;
            for (Modes m : values()) {
                if (i == j)
                    return m;
                j++;
            }
            return null;
        }

        @Override
        public String toString() {
            return this == irwidgetPulse ? "IrWidget count"
                    : this == irwidgetTime ? "IrWidget time"
                    : this == miniPovPulse ? "MiniPOV count"
                    : this == miniPovTime ? "MiniPOV time"
                    : "?";
        }

        public static String toString(Modes m) {
            return m.toString();
        }
    }
}
