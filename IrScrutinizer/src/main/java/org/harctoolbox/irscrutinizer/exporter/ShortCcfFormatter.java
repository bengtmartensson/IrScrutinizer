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

package org.harctoolbox.irscrutinizer.exporter;

import java.util.Map;
import org.harctoolbox.IrpMaster.DecodeIR;
import org.harctoolbox.IrpMaster.IncompatibleArgumentException;
import org.harctoolbox.IrpMaster.IrSignal;
import org.harctoolbox.IrpMaster.Pronto;
import org.harctoolbox.girr.Command;

/**
 * This class formats an IrSignal as a short form CCF format, if possible.
 */
public class ShortCcfFormatter implements Command.CommandTextFormat {

    @Override
    public String getName() {
        return "short-ccf";
    }

    private int getParameter(Map<String, Long> parameters, String name) {
        return parameters.containsKey(name) ?parameters.get(name).intValue() : -1;
    }

    @Override
    public String format(IrSignal irSignal, int count) {
        DecodeIR.DecodedSignal[] decodes = DecodeIR.decode(irSignal);
        if (decodes == null || decodes.length == 0)
            return null;

        Map<String, Long> parameters = decodes[0].getParameters();
        try {
            return Pronto.shortCCFString(decodes[0].getProtocol(),
                    getParameter(parameters, "D"), getParameter(parameters, "S"), getParameter(parameters, "F"));
        } catch (IncompatibleArgumentException ex) {
            return null;
        }
    }
}
