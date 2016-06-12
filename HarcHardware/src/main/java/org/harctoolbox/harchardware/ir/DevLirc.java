/*
Copyright (C) 2016 Bengt Martensson.

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
import java.util.logging.Level;
import java.util.logging.Logger;
import org.harctoolbox.IrpMaster.IncompatibleArgumentException;
import org.harctoolbox.IrpMaster.IrSequence;
import org.harctoolbox.IrpMaster.IrSignal;
import org.harctoolbox.harchardware.HarcHardwareException;
import static org.harctoolbox.lircdevice.LircDevice.defaultDeviceName;
import org.harctoolbox.lircdevice.LircDeviceException;
import org.harctoolbox.lircdevice.NotSupportedException;
// DO NOT import org.harctoolbox.lircdevice.Mode2LircDeviceException;

/**
 *
 */
public class DevLirc implements IRawIrSender, IReceive, ITransmitter {

    private static final int NOTRANSMITTERSELECTION = -1;

    private boolean verbose = false;
    private org.harctoolbox.lircdevice.Mode2LircDevice device = null;
    private boolean canSend = false;
    private boolean canReceive = false;
    private int numberTransmitters = -1;
    private boolean canSetCarrier = false;
    private boolean canSetTransmitter = false;

    /**
     * @return the canSend
     */
    public boolean canSend() {
        return canSend;
    }

    /**
     * @return the canReceive
     */
    public boolean canReceive() {
        return canReceive;
    }

    /**
     * @return the numberTransmitters
     */
    public int getNumberTransmitters() {
        return numberTransmitters;
    }

    /**
     * @return the canSetCarrier
     */
    public boolean canSetCarrier() {
        return canSetCarrier;
    }

    /**
     * @return the canSetTransmitter
     */
    public boolean canSetTransmitter() {
        return canSetTransmitter;
    }

    public class LircTransmitter extends Transmitter {
        private int number;

        public LircTransmitter(int number) throws NoSuchTransmitterException {
            if (number < 1 || number > getNumberTransmitters())
                throw new NoSuchTransmitterException(Integer.toString(number));
            this.number = number;
        }

        private LircTransmitter(String connector) throws NoSuchTransmitterException {
            this(Integer.parseInt(connector));
        }

        private LircTransmitter() {
            number = NOTRANSMITTERSELECTION;
        }

        public boolean isTrivial() {
            return number == NOTRANSMITTERSELECTION;
        }

        public int getNumber() {
            return number;
        }

        public int getMask() {
            return 1 << (number - 1);
        }
    }

    public DevLirc(String deviceName) {
        device = new org.harctoolbox.lircdevice.Mode2LircDevice(deviceName);
    }

    public DevLirc() {
        this(defaultDeviceName);
    }

    private void sendIr(IrSequence irSequence) throws NotSupportedException {
        if (irSequence.isEmpty())
            return;
        device.send(irSequence.toInts());
    }

    @Override
    public boolean sendIr(IrSignal irSignal, int count, Transmitter transmitter) throws HarcHardwareException {
        return sendIr(irSignal, count, (LircTransmitter) transmitter);
    }

    public boolean sendIr(IrSignal irSignal, int count, LircTransmitter transmitter) throws HarcHardwareException {
        if (verbose)
            System.err.println("Sending " + count + " IrSignals: " + irSignal);
        try {
            if (!transmitter.isTrivial())
                device.setTransmitterMask(transmitter.getMask()); // checks canSetTransmitter

            device.setSendCarrier((int) irSignal.getFrequency());

            sendIr(irSignal.getIntroSequence());
            for (int i = 0; i < irSignal.repeatsPerCountSemantic(count); i++)
                    sendIr(irSignal.getRepeatSequence());
            sendIr(irSignal.getEndingSequence());
        } catch (LircDeviceException ex) {
            throw new HarcHardwareException(ex);
        }
        return true;
    }

    @Override
    public IrSequence receive() throws HarcHardwareException, IncompatibleArgumentException {
        int[] data;
        try {
            data = device.receive();
        } catch (NotSupportedException ex) {
            throw new HarcHardwareException(ex);
        }
        IrSequence irSequence = new IrSequence(data);
        if (verbose)
            System.err.println("Received " + irSequence);
        return irSequence;
    }

    @Override
    public boolean stopReceive() {
        return false;
    }

    @Override
    public void setTimeout(int timeout) throws IOException {
    }

    @Override
    public String getVersion() throws IOException {
        return org.harctoolbox.lircdevice.LircDevice.getVersion();
    }

    @Override
    public void setVerbosity(boolean verbosity) {
    }

    @Override
    public void setDebug(int debug) {
    }

    @Override
    public boolean isValid() {
        return device.isValid();
    }

    @Override
    public void open() throws HarcHardwareException {
        try {
            device.open();
            canSetTransmitter = device.canSetTransmitterMask();
            numberTransmitters = device.getNumberTransmitters();
            canSend = device.canSend();
            canReceive = device.canRec();
            canSetCarrier = device.canSetSendCarrier();
        } catch (LircDeviceException ex) {
            throw new HarcHardwareException(ex);
        }
    }

    @Override
    public void close() {
        device.close();
        device = null;
    }

    @Override
    public Transmitter getTransmitter() {
        return new LircTransmitter();
    }

    @Override
    public LircTransmitter getTransmitter(String connector) throws NoSuchTransmitterException {
        return new LircTransmitter(connector);
    }

    @Override
    public String[] getTransmitterNames() {
        String[] result = new String[numberTransmitters];
        for (int i = 1; i <= numberTransmitters; i++)
            result[i] = Integer.toString(i);

        return result;
    }

    @Override
    public String toString() {
        return device.toString();
    }

    public static void main(String[] args) {
        DevLirc instance = new DevLirc();
        try {
            instance.open();
            System.out.println(instance);
        } catch (HarcHardwareException ex) {
            Logger.getLogger(DevLirc.class.getName()).log(Level.SEVERE, null, ex);
        }
        instance.close();
    }
}
