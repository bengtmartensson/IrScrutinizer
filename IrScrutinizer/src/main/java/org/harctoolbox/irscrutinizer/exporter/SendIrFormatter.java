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

package org.harctoolbox.irscrutinizer.exporter;

import org.harctoolbox.IrpMaster.IrSignal;
import org.harctoolbox.girr.Command;
import org.harctoolbox.harchardware.ir.GlobalCache;
import org.harctoolbox.harchardware.ir.NoSuchTransmitterException;

/**
 * This class does something interesting and useful. Or not...
 */
public class SendIrFormatter implements Command.CommandTextFormat {

    private int module;
    private int connector;
    private boolean compressed;
    private final static int sendIndex = 1;

    public SendIrFormatter(int module, int connector, boolean compressed) {
        this.module = module;
        this.connector = connector;
        this.compressed = compressed;
    }

    public SendIrFormatter() {
        this(1, 1, false);
    }

    @Override
    public String getName() {
        return "sendir";
    }

    @Override
    public String format(IrSignal irSignal, int count) {
        try {
            return GlobalCache.sendIrString(irSignal, count, module, connector, sendIndex, compressed);
        } catch (NoSuchTransmitterException ex) {
            throw new IllegalArgumentException(ex.getMessage());
        }
    }
}
