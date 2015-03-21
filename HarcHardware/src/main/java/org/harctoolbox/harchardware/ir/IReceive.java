/*
Copyright (C) 2014 Bengt Martensson.

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
import org.harctoolbox.IrpMaster.IrpMasterException;
import org.harctoolbox.IrpMaster.IrSequence;
import org.harctoolbox.harchardware.HarcHardwareException;
import org.harctoolbox.harchardware.IHarcHardware;

/**
 * This hardware can receive an IR signal for deployment.
 * It does not return a modulation frequency, and normally uses demodulating hardware.
 */
public interface IReceive extends IHarcHardware {

    /**
     * Listens to the device and returns a sequence.
     * Requires the device to be previously opened.
     * Should itself neither open or close the device.
     * @return Captured sequence, or null by timeout.
     * @throws HarcHardwareException if the device is not in valid/open state.
     * @throws IOException
     * @throws IrpMasterException
     */
    public IrSequence receive() throws HarcHardwareException, IOException, IrpMasterException;

    /**
     * Signals the receiving device that it should stop receiving.
     * May not be implemented in all hardware.
     * @return status
     */
    public boolean stopReceive();

   /**
    *
    * @param timeout
    * @throws IOException
    */
    @Override
    public void setTimeout(int timeout) throws IOException;
}
