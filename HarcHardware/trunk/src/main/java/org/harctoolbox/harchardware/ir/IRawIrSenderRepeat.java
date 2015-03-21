/*
Copyright (C) 2012 Bengt Martensson.

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
import org.harctoolbox.IrpMaster.IrSignal;
import org.harctoolbox.IrpMaster.IrpMasterException;

/**
 * Commands for IR senders being able to send IR signals repeating "forever".
 */
public interface IRawIrSenderRepeat extends IRawIrSender {

    /** Number of repeats to perform by sendCcfRepeat */
    //static int repeatMax = 1000;

    /**
     * Like sendCcf, but continues the transmission until interrupted by a stopIr command.
     * Particular implementations, or hardware, might limit the number of transmissions, though.
     * @param irSignal
     * @param transmitter
     * @return if false, command failed.
     * @throws NoSuchTransmitterException
     * @throws IOException
     * @throws IrpMasterException
     */
    public boolean sendIrRepeat(IrSignal irSignal, Transmitter transmitter) throws NoSuchTransmitterException, IOException, IrpMasterException;
}
