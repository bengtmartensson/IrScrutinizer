/*
Copyright (C) 2013 Bengt Martensson.

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

package org.harctoolbox.irscrutinizer.sendinghardware;

import java.io.IOException;
import javax.swing.JPanel;
import org.harctoolbox.IrpMaster.IrSignal;
import org.harctoolbox.IrpMaster.IrpMasterException;
import org.harctoolbox.guicomponents.GuiUtils;
import org.harctoolbox.harchardware.HarcHardwareException;
import org.harctoolbox.harchardware.IHarcHardware;
import org.harctoolbox.harchardware.ir.IRawIrSender;
import org.harctoolbox.harchardware.ir.NoSuchTransmitterException;
import org.harctoolbox.harchardware.ir.Transmitter;
import org.harctoolbox.irscrutinizer.HardwareUnavailableException;
import org.harctoolbox.irscrutinizer.Props;

/**
 * @param <T>
 */
public abstract class SendingHardware <T extends IRawIrSender & IHarcHardware> {
    public static final String PROP_PANEL = "PROP_PANEL";
    public static final String PROP_RAWIRSENDER = "PROP_RAWIRSENDER";
    public static final String PROP_VERBOSE = "PROP_VERBOSE";
    private final JPanel panel;
    protected Props properties;
    protected GuiUtils guiUtils;

    protected SendingHardware(JPanel panel, Props properties, GuiUtils guiUtils) {
        //this.rawIrSender = null;
        this.panel = panel;
        this.properties = properties;
        this.guiUtils = guiUtils;
    }

    /**
     * @return the panel
     */
    public JPanel getPanel() {
        return panel;
    }

    /**
     * @return the rawIrSender
     */
    public abstract T getRawIrSender();

    /**
     * @param verbose the verbose to set
     */
    public void setVerbose(boolean verbose) {
        if (getRawIrSender() != null)
            getRawIrSender().setVerbose(verbose);
    }

    /**
     *
     * @return transmitter
     * @throws NoSuchTransmitterException
     * @throws HardwareUnavailableException
     */

    public Transmitter getTransmitter() throws NoSuchTransmitterException, HardwareUnavailableException{
        return null;
    }

    public boolean sendIr(IrSignal irSignal, int count) throws NoSuchTransmitterException, IrpMasterException, IOException, HardwareUnavailableException, HarcHardwareException {

        if (getRawIrSender() == null)
            throw new HardwareUnavailableException("Internal error: rawIrSender == null");
        if (!getRawIrSender().isValid())
            throw new HardwareUnavailableException();
        return getRawIrSender().sendIr(irSignal, count, getTransmitter());
    }

    public void close() {
        if (getRawIrSender() != null)
            try {
                getRawIrSender().close();
            } catch (IOException ex) {
            }
    }

    public boolean isValid() {
        return getRawIrSender() != null && getRawIrSender().isValid();
    }

    public abstract void setup() throws IOException, HarcHardwareException;

    public abstract String getName();
}
