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
import org.harctoolbox.harchardware.HarcHardwareException;
import org.harctoolbox.harchardware.IHarcHardware;
import org.harctoolbox.harchardware.ir.IRawIrSender;
import org.harctoolbox.harchardware.ir.NoSuchTransmitterException;
import org.harctoolbox.harchardware.ir.Transmitter;
import org.harctoolbox.irscrutinizer.HardwareUnavailableException;

/**
 *
 * @param <T>
 */
public interface ISendingHardware <T extends IRawIrSender & IHarcHardware> {

    public T getRawIrSender();

    public Transmitter getTransmitter() throws NoSuchTransmitterException, HardwareUnavailableException;

    public String getName();

    public JPanel getPanel();

    public void setVerbosity(boolean verbosity);

    public void close();

    public boolean isValid();

    public boolean sendIr(IrSignal irSignal, int count) throws IOException, IrpMasterException, NoSuchTransmitterException, HardwareUnavailableException, HarcHardwareException;

    /**
     * This function (re-)initializes the hardware. The construction should not.
     * If the parameters has not changed, should not re-initialize.
     * @throws IOException
     * @throws HarcHardwareException
     */
    public void setup() throws IOException, HarcHardwareException;
}
