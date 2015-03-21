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

/**
 * This is a bunch of functions for IR senders using a remote/command-model of sending.
 */
public interface IRemoteCommandIrSender {

    /** Number of repeats to perform by sendIrCommandRepeat */
    static int repeatMax = 1000;

    //public String getRemoteCommand(String remote, String command) throws IOException;

    /**
     * Returns an array of "remote" names.
     * @return
     * @throws IOException
     */
    public String[] getRemotes() throws IOException;

    /**
     * Returns an array of command names for the remote given in the argument.
     * @param remote The "remote" to search in
     * @return
     * @throws IOException
     */
    public String[] getCommands(String remote) throws IOException;

    /**
     * Sends the command to the hardware, to be sent count number of times.
     * @param remote
     * @param command
     * @param count Number of times to repeat the command
     * @param transmitter
     * @return success
     * @throws IOException
     * @throws NoSuchTransmitterException
     */
    public boolean sendIrCommand(String remote, String command, int count, Transmitter transmitter) throws IOException, NoSuchTransmitterException;

    /**
     * Like sendIr, but sends the IR signal until stopped by stopIr.
     * An implementation, or a hardware, may limit the number of repeats though.
     * @param remote
     * @param command
     * @param transmitter
     * @return
     * @throws IOException
     * @throws NoSuchTransmitterException
     */
    public boolean sendIrCommandRepeat(String remote, String command, Transmitter transmitter) throws IOException, NoSuchTransmitterException;

    /**
     * Returns a default Transmitter for the device. May be null if the device ignores the Transmitter argument in sendIr.
     * @return
     */
    public Transmitter getTransmitter();
}
