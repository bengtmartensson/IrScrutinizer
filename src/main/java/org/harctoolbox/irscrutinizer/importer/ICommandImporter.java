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
package org.harctoolbox.irscrutinizer.importer;

import java.util.Collection;
import org.harctoolbox.girr.Command;
import org.harctoolbox.girr.Remote;
import org.harctoolbox.ircore.IrCoreException;
import org.harctoolbox.ircore.ModulatedIrSequence;
import org.harctoolbox.irp.IrpException;

/**
 * This interface models a class that can import a number of IR signals.
 */
public interface ICommandImporter extends IImporter {

    /**
     *
     * @return A Collection of the Command-s. May contain non-unique names.
     */
    public Collection<Command> getCommands();

    /**
     *
     * @return All commands in the collection concatenated to a single sequence.
     * Frequency and duty cycle are set to the average between minimum and maximum values by the components, if it makes sense.
     * @throws org.harctoolbox.ircore.IrCoreException
     * @throws org.harctoolbox.irp.IrpException
     */
    public ModulatedIrSequence getConcatenatedCommands() throws IrCoreException, IrpException;

    /**
     *
     * @param name
     * @return One of the Command-s in the collection with the correct name, or null if not found.
     */
    public Command getCommand(String name);

    /**
     * Returns MetaData in some sensible form.
     * @return MetaData
     */
    public Remote.MetaData getMetaData();
}

