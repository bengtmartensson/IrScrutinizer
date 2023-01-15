/*
Copyright (C) 2023 Bengt Martensson.

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

package org.harctoolbox.irscrutinizer.exporter;

import org.harctoolbox.girr.Command;
import org.harctoolbox.harchardware.ir.Broadlink;
import org.harctoolbox.ircore.IrSequence;
import org.harctoolbox.ircore.IrSignal;

public class BroadlinkBase64Formatter implements Command.CommandTextFormat {

    public BroadlinkBase64Formatter() {
    }

    @Override
    public String getName() {
        return "broadlink-base64";
    }

    @Override
    public String format(IrSignal irSignal, int count) {
        if (irSignal.repeatOnly())
            return Broadlink.broadlinkBase64String(irSignal.getRepeatSequence(), count);

        IrSequence irSequence = irSignal.getRepeatLength() > 0 ? irSignal.toModulatedIrSequence(true, count, true) : irSignal.getIntroSequence();
        return Broadlink.broadlinkBase64String(irSequence, 1);
    }
}
