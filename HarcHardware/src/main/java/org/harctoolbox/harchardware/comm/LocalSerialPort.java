/*
Copyright (C) 2012,2013,2014 Bengt Martensson.

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

package org.harctoolbox.harchardware.comm;

import gnu.io.CommPort;
import gnu.io.CommPortIdentifier;
import gnu.io.NoSuchPortException;
import gnu.io.PortInUseException;
import gnu.io.RXTXPort;
import gnu.io.SerialPort;
import gnu.io.UnsupportedCommOperationException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import org.harctoolbox.harchardware.HarcHardwareException;
import org.harctoolbox.harchardware.IHarcHardware;

public abstract class LocalSerialPort implements IHarcHardware {

    public final static String defaultPort = "/dev/ttyS0";
    private final static int msToWaitForPort = 100;

    public enum Parity {
        NONE,  // 0
        ODD,   // 1
        EVEN,  // 2
        MARK,  // 3
        SPACE; // 4
    }

    public enum FlowControl {
        NONE,        // 0
        RTSCTS_IN,   // 1
        RTSCTS_OUT,  // 2
        RTSCTS,      // 3
        XONXOFF_IN,  // 4
        dummy2,
        dummy3,
        dummy4,
        XONXOFF_OUT, // 8
        dummy5,
        dummy6,
        dummy7,
        XONXOFF;     // 12
    }

    protected InputStream inStream;
    protected OutputStream outStream;
    private CommPort commPort;
    private final String portName;
    private String actualPortName;
    private final int baud;
    private final int length;
    private final int stopBits;
    private final Parity parity;
    private final FlowControl flowControl;
    private int timeout;
    private static ArrayList<String> cachedPortNames = null;
    private final static int maxtries = 3;
    protected boolean verbose;
    protected int debug;

    public LocalSerialPort(String portName, int baud, int length, int stopBits, Parity parity, FlowControl flowControl, int timeout) throws NoSuchPortException, PortInUseException, UnsupportedCommOperationException, IOException {
        this.debug = 0;
        this.verbose = false;
        this.portName = portName;
        this.actualPortName = null;
        this.baud = baud;
        this.length = length;
        this.stopBits = stopBits;
        this.parity = parity;
        this.flowControl = flowControl;
        this.timeout = timeout;
    }

    private void lowLevelOpen() throws NoSuchPortException, PortInUseException {
        CommPortIdentifier portIdentifier = CommPortIdentifier.getPortIdentifier(actualPortName);
        commPort = portIdentifier.open(this.getClass().getName(), msToWaitForPort);
    }

    private static String nextPortName(String portName) {
        try {
            final String pattern = "(.*)(\\D+)(\\d+)(:?)";
            String dig = portName.replaceFirst(pattern, "$3");
            int n = Integer.parseInt(dig);
            return portName.replaceFirst(pattern, "$1$2" + Integer.toString(n+1) + "$4");
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Override
    public void setVerbosity(boolean verbosity) {
        this.verbose = verbosity;
    }

    @Override
    public void setDebug(int debug) {
        this.debug = debug;
    }

    @Override
    public void open() throws HarcHardwareException, IOException {
        open(false);
    }

    /**
     * This version of open tries, if its argument is true, to open the "following" device if opening of the requested device fails.
     * Up to maxtries attempts are made, then a HarcHardwareException is thrown on failure.
     *
     * @param iterate
     * @throws HarcHardwareException
     * @throws IOException
     */
    public void open(boolean iterate) throws HarcHardwareException, IOException {
        boolean success = false;
        // Resolve symbolic links (udev!!)
        actualPortName = (new File(portName)).getCanonicalPath();
        int tries = 0;
        do {
            try {
                lowLevelOpen();
                success = true;
            } catch (NoSuchPortException ex) {
                if (!iterate)
                    throw new HarcHardwareException("No such port: " + portName);
                actualPortName = nextPortName(actualPortName);
                if (actualPortName == null)
                    throw new HarcHardwareException("No such port: " + portName);
            } catch (PortInUseException ex) {
                if (!iterate)
                    throw new HarcHardwareException(ex);
                actualPortName = nextPortName(actualPortName);
            }
            tries++;
        } while (!success && tries < maxtries);

        if (!success)
            throw new HarcHardwareException("Could not open LocalSerialPort " + portName + (iterate ? (" -- " + actualPortName): ""));

        if (commPort instanceof gnu.io.SerialPort) {
            SerialPort serialPort = (SerialPort) commPort;
            try {
                serialPort.setSerialPortParams(baud, length, stopBits, parity.ordinal());
            } catch (UnsupportedCommOperationException ex) {
                throw new HarcHardwareException(ex);
            }

            inStream = serialPort.getInputStream();
            outStream = serialPort.getOutputStream();
        }
        if (commPort instanceof gnu.io.RXTXPort) {
            ((RXTXPort) commPort).setFlowControlMode(flowControl.ordinal());
        }
        setTimeout();
    }

    public LocalSerialPort(String portName, int baud) throws NoSuchPortException, PortInUseException, UnsupportedCommOperationException, IOException {
        this(portName, baud, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, Parity.NONE, FlowControl.NONE, 0);
        this.debug = 0;
        this.verbose = false;
    }

    public LocalSerialPort(String portName) throws NoSuchPortException, PortInUseException, UnsupportedCommOperationException, IOException {
        this(portName, 9600, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, Parity.NONE, FlowControl.NONE, 0);
        this.debug = 0;
        this.verbose = false;
    }

    public LocalSerialPort(int portNumber) throws IOException, NoSuchPortException, PortInUseException, UnsupportedCommOperationException {
        this(getSerialPortName(portNumber), 9600, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, Parity.NONE, FlowControl.NONE, 0);
        this.debug = 0;
        this.verbose = false;
    }

    public static String getSerialPortName(int portNumber) throws IOException, NoSuchPortException {
        @SuppressWarnings("unchecked")
        Enumeration<CommPortIdentifier> portEnum = CommPortIdentifier.getPortIdentifiers();
        int nr = 0;
        while (portEnum.hasMoreElements()) {
            CommPortIdentifier portIdentifier = portEnum.nextElement();
            if (portIdentifier.getPortType() == CommPortIdentifier.PORT_SERIAL)
                nr++;

            if (nr == portNumber)
                return portIdentifier.getName();
        }
        throw new NoSuchPortException();
    }

    /**
     * Returns all serial port names found in the system.
     * @param useCached
     * @return ArrayList&lt;String&gt;
     * @throws IOException
     */
    @SuppressWarnings("unchecked")
    public static ArrayList<String> getSerialPortNames(boolean useCached) throws IOException {
        if (useCached && cachedPortNames != null)
            return cachedPortNames;

        Enumeration<CommPortIdentifier> portEnum = null;
        try {
            portEnum = CommPortIdentifier.getPortIdentifiers();
        } catch (UnsatisfiedLinkError ex) {
            throw new IOException(ex.getMessage());
        }
        ArrayList<String> names = new ArrayList<String>();
        while (portEnum.hasMoreElements()) {
            CommPortIdentifier portIdentifier = portEnum.nextElement();
            if (portIdentifier.getPortType() == CommPortIdentifier.PORT_SERIAL)
                names.add(portIdentifier.getName());
        }
        cachedPortNames = names;
        return names;
    }

    public void flushInput() throws IOException {
        while (inStream.available() > 0)
            inStream.read();
    }

    @Override
    public boolean isValid() {
        return commPort != null;
    }

    @Override
    public void setTimeout(int timeout) throws IOException {
        this.timeout = timeout;
        setTimeout();
    }

    private void setTimeout() throws IOException {
        if (timeout > 0)
            try {
                commPort.enableReceiveTimeout(timeout);
            } catch (UnsupportedCommOperationException ex) {
                throw new IOException("timeout not supported");
            }
        else
            commPort.disableReceiveTimeout();
    }

    @Override
    public String getVersion() {
        return "n/a";
    }

    /**
     * Returns the actual port name being used. May differ from one requested in
     * the constructor if the device was opened with open(true).
     * @return Actual port name used.
     */
    public String getActualPortName() {
        return actualPortName;
    }

    /**
     * Returns the nominal port name being used. May differ from one requested in
     * the constructor if the device was opened with open(true), or if it is symlink
     * (for example created by udev).
     * @return port name used.
     */
    public String getPortName() {
        return portName;
    }

    @Override
    public void close() {
        if (!isValid())
            return;
        try {
            inStream.close();
            outStream.close();
            commPort.close();
        } catch (IOException ex) {
            //throw new HarcHardwareException(ex);
        } finally {
            commPort = null;
        }
    }

    public void flush() throws IOException {
        outStream.flush();
    }

    public void setDTR(boolean state) {
        if (commPort instanceof gnu.io.RXTXPort) {
            ((RXTXPort) commPort).setDTR(state);
        }
    }

    public void dropDTR(int duration) {
        setDTR(false);
        try {
            Thread.sleep(duration);
        } catch (InterruptedException ex) {
        }
        setDTR(true);
    }
}
