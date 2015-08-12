/*
Copyright (C) 2012, 2014 Bengt Martensson.

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

/**
 * This interface specifies a device that can be sent commands, and that can be queried for responses.
 */

public interface ICommandLineDevice extends IHarcHardware {

    /**
     * Sends a command (a String) to the instance.
     * @param cmd Command string to be sent.
     * @throws IOException
     */

    void sendString(String cmd) throws IOException;

    /**
     * Reads a line of text. A line is considered to be terminated by any one of a line feed ('\n'), a carriage return ('\r'), or a carriage return followed immediately by a linefeed.
     *
     * @return A String containing the contents of the line, not including any line-termination characters, or null if no input available.
     * @throws IOException
     */
    String readString() throws IOException;

    /**
     * Reads a line of text. A line is considered to be terminated by any one of a line feed ('\n'), a carriage return ('\r'), or a carriage return followed immediately by a linefeed.
     *
     * @param wait if true, wait until something arrives, otherwise return null if nothing there.
     * @return A String containing the contents of the line, not including any line-termination characters, or null if no input available.
     * @throws IOException
     */
    String readString(boolean wait) throws IOException;

    /**
     * Tells whether this stream is ready to be read. A buffered character stream is ready if the buffer is not empty, or if the underlying character stream is ready.
     * @return True if the next read() is guaranteed not to block for input, false otherwise. Note that returning false does not guarantee that the next read will block.
     * @throws IOException  If an I/O error occurs
     */
    boolean ready() throws IOException;
}
