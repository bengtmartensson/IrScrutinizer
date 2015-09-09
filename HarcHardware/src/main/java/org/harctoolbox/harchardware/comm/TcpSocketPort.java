/*
Copyright (C) 2012,2013 Bengt Martensson.

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
import org.harctoolbox.IrpMaster.IrpUtils;
import org.harctoolbox.harchardware.IHarcHardware;
import org.harctoolbox.harchardware.ICommandLineDevice;
import org.harctoolbox.harchardware.Utils;

public class TcpSocketPort implements ICommandLineDevice, IBytesCommand, IHarcHardware {

    public final static int defaultTimeout = 2000;

    @Override
    public void open() throws IOException {
        tcpSocketChannel.connect();
    }

    @Override
    public boolean ready() throws IOException {
        return tcpSocketChannel.ready();
    }

    public enum ConnectionMode {
        keepAlive,
        justInTime;
    };

    @Override
    public void sendBytes(byte[] cmd) throws IOException {
        tcpSocketChannel.connect();
        tcpSocketChannel.getOut().write(cmd);
        tcpSocketChannel.close(false);
    }

    @Override
    public byte[] readBytes(int length) throws IOException {
        tcpSocketChannel.connect();
        byte[] result = Utils.readBytes(tcpSocketChannel.getIn(), length);
        tcpSocketChannel.close(false);
        return result;
    }

    TcpSocketChannel tcpSocketChannel;

    public TcpSocketPort(String hostIp, int portNumber, int timeout, boolean verbose, ConnectionMode connectionMode) throws UnknownHostException {
        tcpSocketChannel = new TcpSocketChannel(hostIp, portNumber, timeout, verbose, connectionMode);
    }

    public TcpSocketPort(String hostIp, int portNumber, boolean verbose, ConnectionMode connectionMode) throws UnknownHostException {
        tcpSocketChannel = new TcpSocketChannel(hostIp, portNumber, defaultTimeout, verbose, connectionMode);
    }

    @Override
    public void close() {
        try {
            tcpSocketChannel.close(true);
        } catch (IOException ex) {
        } finally {
            tcpSocketChannel = null;
        }
    }

    @Override
    public void sendString(String str) throws IOException {
        sendBytes(str.getBytes(IrpUtils.dumbCharset));
    }

    @Override
    public String readString() throws IOException {
        return readString(true);
    }

    @Override
    public String readString(boolean wait) throws IOException {
        tcpSocketChannel.connect();
        String result = tcpSocketChannel.readString(wait);
        tcpSocketChannel.close(false);
        return result;
    }

    @Override
    public boolean isValid() {
        return tcpSocketChannel != null && tcpSocketChannel.isValid();
    }

    @Override
    public String getVersion() {
        return "";
    }

    @Override
    public void setTimeout(int timeout) {
        try {
            tcpSocketChannel.setTimeout(timeout);
        } catch (SocketException ex) {
        }
    }

    @Override
    public void setVerbosity(boolean verbose) {
        tcpSocketChannel.setVerbosity(verbose);
    }

    @Override
    public void setDebug(int debug) {
        tcpSocketChannel.setDebug(debug);
    }

    public static void main(String[] args) {
        try {
            TcpSocketPort port = new TcpSocketPort("denon", 23, defaultTimeout, true, ConnectionMode.keepAlive);
            port.sendString("MVDOWN\r");
            String result = port.readString();
            System.out.println(result);
            port.close();
        } catch (IOException ex) {
            System.err.println(ex.getMessage());
        }
    }
}
