/*
Copyright (C) 2012, 2013 Bengt Martensson.

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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import org.harctoolbox.IrpMaster.IrpUtils;
import org.harctoolbox.harchardware.ICommandLineDevice;
import org.harctoolbox.harchardware.Utils;

/**
 * This a helper class, to bundle the socket operations in a unified manner.
 * It can be instantiated, possibly in multiple instances.
 * It is not meant to be inherited from, or exported.
 * It should therefore throw low-level exceptions, not HarcHardwareException.
 */
public class TcpSocketChannel implements ICommandLineDevice, IBytesCommand {
    private InetAddress inetAddress = null;
    private int portNumber;
    private boolean verbose;
    private int timeout;
    private TcpSocketPort.ConnectionMode connectionMode;
    private Socket socket = null;
    private OutputStream outStream = null;
    private InputStream inStream = null;
    private BufferedReader bufferedInStream = null;

    /**
     *
     * @param hostIp
     * @param portNumber
     * @param timeout
     * @param verbose
     * @param connectionMode
     * @throws UnknownHostException
     */
    public TcpSocketChannel(String hostIp, int portNumber, int timeout, boolean verbose,
            TcpSocketPort.ConnectionMode connectionMode) throws UnknownHostException {
        this(InetAddress.getByName(hostIp), portNumber, timeout, verbose, connectionMode);
    }

    /**
     *
     * @param inetAddress
     * @param portNumber
     * @param timeout
     * @param verbose
     * @param connectionMode
     */
    public TcpSocketChannel(InetAddress inetAddress, int portNumber, int timeout, boolean verbose,
            TcpSocketPort.ConnectionMode connectionMode) {
        this.inetAddress = inetAddress;
        this.portNumber = portNumber;
        this.timeout = timeout;
        this.verbose = verbose;
        this.connectionMode = connectionMode;
    }

    /**
     *
     * @throws IOException
     */
    public void connect() throws IOException {
        if (socket == null || !socket.isConnected()) {
            socket = new Socket();
            if (verbose)
                System.err.println("Connecting socket to " + inetAddress.getHostAddress() + ":" + portNumber);

            socket.connect(new InetSocketAddress(inetAddress, portNumber), timeout);
            socket.setSoTimeout(timeout);
            socket.setKeepAlive(connectionMode == TcpSocketPort.ConnectionMode.keepAlive);
        }

        if (outStream == null)
            //outStream = new PrintStream(socket.getOutputStream(), false, IrpUtils.dumbCharsetName);
            outStream = socket.getOutputStream();

        if (inStream == null) {
            inStream = socket.getInputStream();
            bufferedInStream = new BufferedReader(new InputStreamReader(inStream, IrpUtils.dumbCharset));
        }
    }

    /**
     *
     * @param force
     * @throws IOException
     */
    public void close(boolean force) throws IOException {
        if (force || connectionMode == TcpSocketPort.ConnectionMode.justInTime) {
            if (outStream != null) {
                outStream.close();
                outStream = null;
            }
            if (inStream != null) {
                inStream.close();
                inStream = null;
                bufferedInStream = null;
            }
            if (socket != null) {
                socket.close();
                socket = null;
            }
        }
    }

    public OutputStream getOut() {
        return outStream;
    }

    public InputStream getIn() {
        return inStream;
    }

    public BufferedReader getBufferedIn() {
        return bufferedInStream;
    }

    @Override
    public boolean isValid() {
        return socket != null;
    }

    @Override
    public void setTimeout(int timeout) throws SocketException {
        this.timeout = timeout;
        socket.setSoTimeout(timeout);
    }

    @Override
    public void setVerbosity(boolean verbose) {
        this.verbose = verbose;
    }

    @Override
    public void setDebug(int debug) {
    }

    @Override
    public void sendString(String cmd) throws IOException {
        sendBytes(cmd.getBytes(IrpUtils.dumbCharset));
    }

    @Override
    public String readString() throws IOException {
        return readString(true);
    }

    @Override
    public String readString(boolean wait) throws IOException {
        if (!wait && !bufferedInStream.ready()) {
            if (verbose)
                System.err.println("<(null)");
            return null;
        }
        String line = bufferedInStream.readLine();
        if (verbose)
            System.err.println("<" + line);
        return line;
    }

    @Override
    public void close() throws IOException {
        close(true);
    }

    @Override
    public void sendBytes(byte[] cmd) throws IOException {
        outStream.write(cmd);
    }

    @Override
    public byte[] readBytes(int length) throws IOException {
        return Utils.readBytes(inStream, length);
    }

    @Override
    public String getVersion() {
        return null;
    }

    @Override
    public void open() {
    }

    @Override
    public boolean ready() throws IOException {
        return bufferedInStream != null && bufferedInStream.ready();
    }
}
