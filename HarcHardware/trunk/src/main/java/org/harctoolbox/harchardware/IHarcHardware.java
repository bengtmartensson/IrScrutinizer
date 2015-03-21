/*
Copyright (C) 2012-2014 Bengt Martensson.

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

package org.harctoolbox.harchardware;

import java.io.Closeable;
import java.io.IOException;

/**
 * Some standard functions that all hardware interfaces should implement.
 */
public interface IHarcHardware extends Closeable {

    /**
     * Returns the hardware version (not the version of the driver software).
     * May be null if no relevant information available.
     *
     * @return Version string. Semantics can vary.
     * @throws IOException
     */
    public String getVersion() throws IOException;

    /**
     * Sets a verbosity flag, causing commands to be executed verbosely. Exact semantic depends on the implementation.
     * @param verbosity on or off
     */
    public void setVerbosity(boolean verbosity);

    /**
     * Sets a debug parameter. Exact semantic depends on the implementation.
     * @param debug
     */
    public void setDebug(int debug);

    /**
     * Set timeout in milliseconds. Exact semantics may be dependent on the hardware.
     * @param timeout Timeout in milliseconds.
     * @throws IOException
     */
    public void setTimeout(int timeout) throws IOException;

    /**
     * Tries to identify instances that are not valid. Exact meaning can vary.
     * @return validity of the instance.
     */
    public boolean isValid();

    /**
     * Opens the device with previously set parameters.
     *
     * @throws org.harctoolbox.harchardware.HarcHardwareException
     * @throws IOException
     */
    public void open() throws HarcHardwareException, IOException;
}
