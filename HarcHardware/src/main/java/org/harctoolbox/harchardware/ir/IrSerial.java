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

import org.harctoolbox.harchardware.comm.LocalSerialPort;
import gnu.io.NoSuchPortException;
import gnu.io.PortInUseException;
import gnu.io.UnsupportedCommOperationException;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import org.harctoolbox.harchardware.HarcHardwareException;
import org.harctoolbox.harchardware.IHarcHardware;

/**
 * This class models a serial device that takes text commands from a serial port, like the Arduino.
 * @param <T>
 */
public abstract class IrSerial<T extends LocalSerialPort> implements IHarcHardware {

    protected boolean verbose;
    protected int debug;
    private int timeout;
    protected T serialPort;
    private String portName;
    private int baudRate;
    private int dataSize;
    private int stopBits;
    private LocalSerialPort.Parity parity;
    private LocalSerialPort.FlowControl flowControl;
    private final Class<T> clazz;

    /**
     * @param baudRate the baudRate to set
     */
    public void setBaudRate(int baudRate) {
        this.baudRate = baudRate;
    }

    /**
     * @param dataSize the dataSize to set
     */
    public void setDataSize(int dataSize) {
        this.dataSize = dataSize;
    }

    /**
     * @param stopBits the stopBits to set
     */
    public void setStopBits(int stopBits) {
        this.stopBits = stopBits;
    }

    /**
     * @param parity the parity to set
     */
    public void setParity(LocalSerialPort.Parity parity) {
        this.parity = parity;
    }

    /**
     * @param flowControl the flowControl to set
     */
    public void setFlowControl(LocalSerialPort.FlowControl flowControl) {
        this.flowControl = flowControl;
    }

    /**
     * @param portName the portName to set
     */
    public void setPortName(String portName) {
        this.portName = portName;
    }

    @Override
    // Default version for hardware that does not support a sensible version.
    // NOTE: just return null, not something "user friendly"
    // -- this is the task of the user interface.
    public String getVersion() throws IOException {
        return null;
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
    public void setTimeout(int timeout) throws IOException {
        if (!isValid())
            throw new IOException("Port not valid, cannot set timeout.");
        this.timeout = timeout;
        serialPort.setTimeout(timeout);
    }

    @Override
    public boolean isValid() {
        return serialPort != null && serialPort.isValid();
    }

    @Override
    public void close() throws IOException {
        if (!isValid())
            return;

        try {
            serialPort.flush();
        } finally {
            try {
                serialPort.close();
            } finally {
                serialPort = null;
            }
        }
    }

    public Transmitter getTransmitter() {
        return null;
    }

    public IrSerial(Class<T> clazz, String portName, int baudRate, int dataSize, int stopBits, LocalSerialPort.Parity parity, LocalSerialPort.FlowControl flowControl, int timeout, boolean verbose)
            throws NoSuchPortException, PortInUseException, UnsupportedCommOperationException, IOException {
        this.clazz = clazz;
        this.portName = portName;
        this.baudRate = baudRate;
        this.dataSize = dataSize;
        this.stopBits = stopBits;
        this.parity = parity;
        this.flowControl = flowControl;
        this.timeout = timeout;
        this.verbose = verbose;
        this.debug = 0;
        //open();
    }

    @Override
    @SuppressWarnings("unchecked")
    public void open() throws HarcHardwareException, IOException {
        try {
            Constructor<T> constructor =  clazz.getConstructor(String.class, int.class, int.class, int.class,
                    LocalSerialPort.Parity.class, LocalSerialPort.FlowControl.class, int.class, boolean.class);
            serialPort = constructor.newInstance(portName, baudRate, dataSize, stopBits, parity, flowControl, timeout, verbose);
        } catch (NoSuchMethodException ex) {
            throw new RuntimeException("Programming error in IrSerial");
        } catch (SecurityException ex) {
            throw new RuntimeException("Programming error in IrSerial");
        } catch (InstantiationException ex) {
            throw new RuntimeException("Programming error in IrSerial");
        } catch (IllegalAccessException ex) {
            throw new RuntimeException("Programming error in IrSerial");
        } catch (IllegalArgumentException ex) {
            throw new RuntimeException("Programming error in IrSerial");
        } catch (InvocationTargetException ex) {
            throw new RuntimeException("Programming error in IrSerial");
        }
        serialPort.open();
    }
}
