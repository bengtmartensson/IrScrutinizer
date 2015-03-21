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

package org.harctoolbox.irscrutinizer;

import org.harctoolbox.IrpMaster.IrpUtils;

/**
 * This class fires up a command line IrpMaster.
 */
class IrpMaster {

    private IrpMaster() {}

    public static void main(String[] args) {
        org.harctoolbox.IrpMaster.IrpMaster.main(args);
        System.exit(IrpUtils.exitSuccess); // just to be safe
    }
}