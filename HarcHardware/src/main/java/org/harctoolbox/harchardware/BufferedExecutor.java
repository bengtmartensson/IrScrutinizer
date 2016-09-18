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
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * This class wraps an ICommandExecutor into a ICommandLineDevice.
 */
public class BufferedExecutor implements ICommandLineDevice {

    private final ICommandExecutor executor;
    private final List<String> output;

    public BufferedExecutor(ICommandExecutor executor) {
        this.executor = executor;
        output = new ArrayList<>(8);
    }

    @Override
    public void sendString(String cmd) throws IOException, HarcHardwareException {
        Collection<String> out = executor.exec(cmd);
        output.addAll(out);
    }

    @Override
    public String readString() throws IOException {
        return readString(false);
    }

    @Override
    public String readString(boolean wait) throws IOException {
        String result;
        if (wait)
            throw new UnsupportedOperationException("Not supported yet.");
        else if (!output.isEmpty()) {
             result = output.get(0);
            output.remove(0);
        } else
            result = null;
        return result;
    }

    @Override
    public boolean ready() throws IOException {
        return output.size() > 0;
    }

    @Override
    public void flushInput() throws IOException {
    }

    @Override
    public String getVersion() throws IOException {
        return executor.getVersion();
    }

    @Override
    public void setVerbosity(boolean verbosity) {
        executor.setVerbosity(verbosity);
    }

    @Override
    public void setDebug(int debug) {
        executor.setDebug(debug);
    }

    @Override
    public void setTimeout(int timeout) throws IOException {
        executor.setTimeout(timeout);
    }

    @Override
    public boolean isValid() {
        return executor.isValid();
    }

    @Override
    public void open() throws HarcHardwareException, IOException {
        executor.open();
    }

    @Override
    public void close() throws IOException {
        executor.close();
    }
}
