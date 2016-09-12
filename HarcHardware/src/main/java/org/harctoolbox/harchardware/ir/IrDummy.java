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
import org.harctoolbox.IrpMaster.IrpMasterException;
import org.harctoolbox.IrpMaster.ModulatedIrSequence;
import org.harctoolbox.harchardware.HarcHardwareException;
import org.harctoolbox.harchardware.IHarcHardware;

/**
 * A dummy device just for testing.
 */

public class IrDummy implements IHarcHardware, IRawIrSender, IIrSenderStop, ITransmitter, ICapture, IReceive, IRemoteCommandIrSender {

    private static final Logger logger = Logger.getLogger(IrDummy.class.getName());
    private static final int[] necdata = new int[] { 9041, 4507, 573, 573, 573, 573, 573, 1694, 573, 1694, 573, 573, 573, 573, 573, 573, 573, 573, 573, 573, 573, 1694, 573, 573, 573, 573, 573, 573, 573, 1694, 573, 573, 573, 573, 573, 573, 573, 573, 573, 573, 573, 1694, 573, 1694, 573, 1694, 573, 573, 573, 573, 573, 1694, 573, 1694, 573, 1694, 573, 573, 573, 573, 573, 573, 573, 1694, 573, 1694, 573, 44293 };
    private static IrSequence irSequence;
    private static ModulatedIrSequence modulatedIrSequence;

    public static final String defaultVersion = "1.2.3";

    static {
        try {
            irSequence = new IrSequence(necdata);
            modulatedIrSequence = new ModulatedIrSequence(necdata, 38400);
        } catch (IncompatibleArgumentException ex) {
            Logger.getLogger(IrDummy.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private boolean verbosity;
    private int debug;
    private int timeout;
    private boolean valid;
    private boolean dummySuccess;
    private int[] transmitterNumber;
    private int endTimeOut;
    private int captureMaxSize;
    private int beginTimeout;
    private String version;

    public IrDummy(String version) {
        logger.log(Level.INFO, "IrDummy({0})", version);
        this.version = version;
    }

    public IrDummy() {
        this(defaultVersion);
    }

    @Override
    public String getVersion() throws IOException {
        return version;
    }

    @Override
    public void setVerbosity(boolean verbosity) {
        this.verbosity = verbosity;
    }

    @Override
    public void setDebug(int debug) {
        this.debug = debug;
    }

    @Override
    public void setTimeout(int timeout) throws IOException {
        this.timeout = timeout;
    }

    @Override
    public boolean isValid() {
        return valid;
    }

    @Override
    public void open() throws HarcHardwareException, IOException {
        logger.log(Level.INFO, "open");
        valid = true;
    }

    @Override
    public void close() throws IOException {
        logger.log(Level.INFO, "close");
        valid = false;
    }

    @Override
    public boolean sendIr(IrSignal irSignal, int count, Transmitter transmitter) throws HarcHardwareException, NoSuchTransmitterException, IrpMasterException, IOException {
        logger.log(Level.INFO, "sendIr: {0} #{1} {2}", new Object[]{irSignal.toString(true), count, transmitter});
        return dummySuccess;
    }

    @Override
    public Transmitter getTransmitter() {
        return new LircTransmitter(transmitterNumber);
    }

    @Override
    public boolean stopIr(Transmitter transmitter) throws NoSuchTransmitterException, IOException {
        logger.log(Level.INFO, "stopIr");
        return dummySuccess;
    }

    @Override
    public Transmitter getTransmitter(String connector) throws NoSuchTransmitterException {
        return new LircTransmitter(connector);
    }

    @Override
    public String[] getTransmitterNames() {
        return new String[] { "Orange", "Banana" };
    }

    @Override
    public ModulatedIrSequence capture() throws HarcHardwareException, IOException, IrpMasterException {
        return modulatedIrSequence;
    }

    @Override
    public boolean stopCapture() {
        logger.log(Level.INFO, "stopCapture");
        return dummySuccess;
    }

    @Override
    public void setBeginTimeout(int integer) throws IOException {
        beginTimeout = integer;
    }

    @Override
    public void setCaptureMaxSize(int integer) {
        captureMaxSize = integer;
    }

    @Override
    public void setEndingTimeout(int integer) {
        endTimeOut = integer;
    }

    @Override
    public String[] getRemotes() throws IOException {
        return new String[] { "TV", "Bluray" };
    }

    @Override
    public String[] getCommands(String remote) throws IOException {
        return new String[] { "power_on", "power_off", "power_toggle" };
    }

    @Override
    public boolean sendIrCommand(String remote, String command, int count, Transmitter transmitter) throws IOException, NoSuchTransmitterException {
        logger.log(Level.INFO, "sendIrCommand: {0} {1} #{2} {3}", new Object[]{remote, command, count, transmitter});
        return dummySuccess;
    }

    @Override
    public boolean sendIrCommandRepeat(String remote, String command, Transmitter transmitter) throws IOException, NoSuchTransmitterException {
        logger.log(Level.INFO, "sendIrCommand: {0} {1} {2}", new Object[]{remote, command, transmitter});
        return dummySuccess;
    }

    @Override
    public IrSequence receive() throws HarcHardwareException, IOException, IrpMasterException {
        return irSequence;
    }

    @Override
    public boolean stopReceive() {
        return dummySuccess;
    }
}
