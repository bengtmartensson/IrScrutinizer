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

package org.harctoolbox.harchardware.comm;

import java.io.IOException;
import java.net.SocketException;
import java.net.UnknownHostException;
import org.harctoolbox.harchardware.IHarcHardware;
import org.harctoolbox.harchardware.ICommandLineDevice;

public class UdpSocketPort implements ICommandLineDevice, IHarcHardware {

    UdpSocketChannel udpSocketChannel;

    public UdpSocketPort(String hostIp, int portNumber, int timeout, boolean verbose) throws UnknownHostException, SocketException {
        udpSocketChannel = new UdpSocketChannel(hostIp, portNumber, timeout, verbose);
    }

    @Override
    public void close() throws IOException {
        udpSocketChannel.close();
        udpSocketChannel = null;
    }

    @Override
    public void sendString(String str) throws IOException {
        udpSocketChannel.sendString(str);
    }

    @Override
    public String readString() throws IOException {
        return udpSocketChannel.readString();
    }

    @Override
    public String readString(boolean wait) throws IOException {
        if (wait)
            return readString();

        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public boolean isValid() {
        return udpSocketChannel != null && udpSocketChannel.isValid();
    }

    @Override
    public String getVersion() {
        return "";
    }

    @Override
    public void setTimeout(int timeout) {
        try {
            udpSocketChannel.setTimeout(timeout);
        } catch (SocketException ex) {
        }
    }

    @Override
    public void setVerbosity(boolean verbose) {
        udpSocketChannel.setVerbosity(verbose);
    }

    @Override
    public void setDebug(int debug) {
        udpSocketChannel.setDebug(debug);
    }

    public static void main(String[] args) {
        try {
            UdpSocketPort port = new UdpSocketPort("irtrans", 21000, 2000, true);
            port.sendString("snd philips_37pfl9603,power_toggle");
            String result = port.readString();
            System.out.println(result);
        } catch (IOException ex) {
            System.err.println(ex.getMessage());
        }
    }

    @Override
    public void open() {
    }

    @Override
    public boolean ready() {
        return true; // ???
    }
}
