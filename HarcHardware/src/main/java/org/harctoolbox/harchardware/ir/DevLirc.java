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

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.harctoolbox.IrpMaster.IncompatibleArgumentException;
import org.harctoolbox.IrpMaster.IrSequence;
import org.harctoolbox.IrpMaster.IrSignal;
import org.harctoolbox.IrpMaster.IrpUtils;
import org.harctoolbox.IrpMaster.ModulatedIrSequence;
import org.harctoolbox.devslashlirc.LircDeviceException;
import org.harctoolbox.devslashlirc.Mode2LircDevice;
import org.harctoolbox.devslashlirc.NotSupportedException;
import org.harctoolbox.harchardware.HarcHardwareException;

/**
 *
 */
public class DevLirc implements IRawIrSender, IReceive, ICapture, ITransmitter, IIrSenderStop {

    public static final String DEV = "/dev";
    public static final String DEVSLASHLIRC = "/dev/lirc";
    public static final String LIRCDEVPATTERN = "lirc\\d+";

    public static File[] getCandidates() {
        if (!new File(DEV).isDirectory())
            return new File[0];

        return new File(DEVSLASHLIRC).isDirectory()
                ? new File(DEVSLASHLIRC).listFiles()
                : new File(DEV).listFiles(new FilenameFilter() {
                    @Override
                    public boolean accept(File dir, String name) {
                        return name.matches(LIRCDEVPATTERN);
                    }
                });
    }

    public static void main(String[] args) {
        File[] candidates = getCandidates();
        for (File f : candidates)
            System.out.println(f);
        try (DevLirc instance = new DevLirc()) {
            double nec1_frequency = 38400f;
            int[] nec1_122_27 = {
                9024, 4512, 564, 564, 564, 1692, 564, 564, 564, 1692, 564, 1692, 564, 1692, 564, 1692, 564, 564, 564, 1692, 564, 564, 564, 1692, 564, 564, 564, 564, 564, 564, 564, 564, 564, 1692, 564, 1692, 564, 1692, 564, 564, 564, 1692, 564, 1692, 564, 564, 564, 564, 564, 564, 564, 564, 564, 564, 564, 1692, 564, 564, 564, 564, 564, 1692, 564, 1692, 564, 1692, 564, 39756
            };
            int[] nec1_repeat = { 9024, 2256, 564, 96156 };
            IrSignal yama_volume_down = new IrSignal(nec1_frequency, -1.0, new IrSequence(nec1_122_27),
                    new IrSequence(nec1_repeat), null);
            instance.open();
            System.out.println(instance);
            System.out.println(">>>>>>>>>>>>> Now send IR <<<<<<<<<<<<<<<");
            IrSequence irSequence = instance.receive();
            System.out.println(irSequence);
            instance.sendIr(yama_volume_down, 10, new LircTransmitter(1));
        } catch (HarcHardwareException | IncompatibleArgumentException | LircDeviceException ex) {
            Logger.getLogger(DevLirc.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private boolean verbose = false;
    private Mode2LircDevice device = null;
    private boolean canSend = false;
    private boolean canReceive = false;
    private int numberTransmitters = -1;
    private boolean canSetCarrier = false;
    private boolean canSetTransmitter = false;
    private boolean stopRequested;

    public DevLirc(String deviceName, boolean verbose) throws LircDeviceException {
        device = new Mode2LircDevice(deviceName);
        this.verbose = verbose;
    }

    public DevLirc(String deviceName) throws LircDeviceException {
        this(deviceName, false);
    }

    public DevLirc() throws LircDeviceException {
        this(Mode2LircDevice.defaultDeviceName, false);
    }

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


    private void sendIr(IrSequence irSequence) throws NotSupportedException {
        if (irSequence.isEmpty())
            return;
        device.send(irSequence.toInts());
    }

    @Override
    public boolean sendIr(IrSignal irSignal, int count, Transmitter transmitter) throws HarcHardwareException {
        if (transmitter != null && ! (transmitter instanceof LircTransmitter))
            throw new NoSuchTransmitterException("erroneous transmitter");
        return sendIr(irSignal, count, (LircTransmitter) transmitter);
    }

    public boolean sendIr(IrSignal irSignal, int count, LircTransmitter transmitter) throws HarcHardwareException {
        stopRequested = false;
        try {
            if (transmitter != null && canSetTransmitter) {
                int mask = transmitter.toMask();
                if (mask != LircTransmitter.NOMASK) {
                    device.setTransmitterMask(mask);
                    if (verbose)
                        System.err.println("Setting transmitter mask " + mask);
                }
            }

            if (verbose)
                System.err.println("DevLirc sending " + count + " IrSignals: " + irSignal);

            device.setSendCarrier((int) irSignal.getFrequency());

            sendIr(irSignal.getIntroSequence());
            for (int i = 0; i < irSignal.repeatsPerCountSemantic(count); i++) {
                if (stopRequested)
                    break;

                sendIr(irSignal.getRepeatSequence());
            }
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
    public boolean stopIr(Transmitter transmitter) {
        stopRequested = true;
        return true;
    }

    @Override
    public String getVersion() throws IOException {
        return device.getVersion();
    }

    @Override
    public void setVerbosity(boolean verbosity) {
        this.verbose = verbosity;
    }

    @Override
    public void setDebug(int debug) {
    }

    @Override
    public boolean isValid() {
        return device != null && device.isValid();
    }

    @Deprecated
    @Override
    public void setTimeout(int timeout) throws IOException {
        device.setBeginTimeout(timeout);
    }

    @Override
    public void setBeginTimeout(int timeout) throws IOException {
        device.setBeginTimeout(timeout);
    }

    @Override
    public void setCaptureMaxSize(int integer) {
        device.setMaxCaptureLength(integer);
    }

    @Override
    public void setEndTimeout(int integer) {
        device.setEndTimeout(integer);
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
        if (device != null)  {
            device.close();
            //device = null;
        }
    }

    @Override
    public Transmitter getTransmitter() {
        return new LircTransmitter();
    }

    @Override
    public LircTransmitter getTransmitter(String connector) throws NoSuchTransmitterException {
        return new LircTransmitter(connector);
    }

    public LircTransmitter getTransmitter(int number) throws NoSuchTransmitterException {
        return new LircTransmitter(number);
    }

    @Override
    public String[] getTransmitterNames() {
        String[] result = new String[numberTransmitters];
        for (int i = 1; i <= numberTransmitters; i++)
            result[i-1] = Integer.toString(i);

        return result;
    }

    @Override
    public String toString() {
        return device.toString();
    }

    @Override
    public ModulatedIrSequence capture() throws HarcHardwareException, IncompatibleArgumentException {
        IrSequence irSequence = receive();
        return irSequence.isEmpty() ? null
                : new ModulatedIrSequence(irSequence, IrpUtils.defaultFrequency, IrpUtils.invalid);
    }

    @Override
    public boolean stopCapture() {
        return false; //throw new UnsupportedOperationException("Not supported yet.");
    }
}
