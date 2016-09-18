/*
Copyright (C) 2016 Bengt Martensson.

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

import java.io.IOException;
import java.util.Collection;

/**
 * A "Batch command" executor.
 */
public interface ICommandExecutor extends IHarcHardware {

    /**
     * Executes a command, does not return until its finished. Then returns its Output
     * as a collection of Strings.
     *
     * @param command Command given.
     * @return Output produced. Collection of Strings.
     * @throws IOException
     * @throws org.harctoolbox.harchardware.HarcHardwareException
     */
    public Collection<String> exec(String command) throws IOException, HarcHardwareException;
}
