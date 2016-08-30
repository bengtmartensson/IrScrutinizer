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

package org.harctoolbox.harchardware.ir;

import java.io.IOException;
import org.harctoolbox.IrpMaster.IrpMasterException;
import org.harctoolbox.IrpMaster.ModulatedIrSequence;
import org.harctoolbox.harchardware.HarcHardwareException;

/**
 * This hardware captures IR signals, for analyzing rather than for deployment use.
 * It delivers a modulation frequency, and normally uses a non-demodulating sensor.
 *
 */
public interface ICapture extends IIrReader {

    /**
     * Listens to the device and returns a sequence.
     * Requires the device to be previously opened.
     * Should itself neither open or close the device.
     * @return Captured sequence, or null by timeout.
     * @throws HarcHardwareException if the device is not in valid/open state.
     * @throws IOException
     * @throws IrpMasterException
     */
    public ModulatedIrSequence capture() throws HarcHardwareException, IOException, IrpMasterException;

    /**
     * Signals the capturing device that it should stop capturing.
     * May not be implemented in all hardware.
     * @return status
     */
    public boolean stopCapture();
}
