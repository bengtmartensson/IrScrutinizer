/*
Copyright (C) 2013, 2014 Bengt Martensson.

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

import org.harctoolbox.harchardware.HarcHardwareException;

/**
 * This exception is thrown when trying to use a non existing transmitter.
 */
public class NoSuchTransmitterException extends HarcHardwareException {
    public NoSuchTransmitterException(String string) {
        super("No such transmitter: " + string);
    }

    public NoSuchTransmitterException(Transmitter transmitter) {
        super("No such transmitter: " + transmitter); // null-safe!
    }
}
