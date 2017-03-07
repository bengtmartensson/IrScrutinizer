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

package org.harctoolbox.harchardware.comm;

import java.io.ByteArrayOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import org.harctoolbox.IrpMaster.IrpUtils;

/**
 * This a helper class, to bundle the socket operations in a unified manner.
 * It can be instantiated, possibly in multiple instances.
 * It is not meant to be inherited from, or exported.
 * It should therefore throw low-level exceptions, not HarcHardwareException.
 */
public class UdpSocketChannel {
    private final static int BUFFERSIZE = 65000;
    public static void main(String[] args) {
        try {
            UdpSocketChannel ch = new UdpSocketChannel("irtrans", 21000, 2000, true);
            ch.sendString("snd philips_37pfl9603,power_toggle");
            String response = ch.readString();
            System.out.println(response);
            ch.close();
        } catch (IOException ex) {
            System.err.println(ex.getMessage());
        }
    }

    private final InetAddress inetAddress;
    private final int portNumber;
    private boolean verbose;
    private DatagramSocket socket = null;
    private PrintStream outStream = null;

    private final byte[] byteBuffer = new byte[BUFFERSIZE];

    public UdpSocketChannel(InetAddress inetAddress, int portNumber, int timeout, boolean verbose) throws UnknownHostException, SocketException {
        this.inetAddress = inetAddress;
        this.portNumber = portNumber;
        this.verbose = verbose;
        socket = new DatagramSocket();
        socket.setSoTimeout(timeout);
        try {
            outStream = new PrintStream(new FilteredStream(new ByteArrayOutputStream()), false, IrpUtils.dumbCharsetName);
        } catch (UnsupportedEncodingException ex) {
            // cannot happen
        }
    }

    public UdpSocketChannel(String hostIp, int portNumber, int timeout, boolean verbose) throws UnknownHostException, SocketException {
        this(InetAddress.getByName(hostIp), portNumber, timeout, verbose);
    }

    private void send(byte[] buf) throws IOException {
        if (verbose)
            System.err.println("Sending command `" + new String(buf, IrpUtils.dumbCharset) + "' over UDP to " + inetAddress.getCanonicalHostName() + ":" +  portNumber);
        DatagramPacket dp = new DatagramPacket(buf, buf.length, inetAddress, portNumber);
        socket.send(dp);
    }

    public void sendString(String string) throws IOException {
        send(string.getBytes("US-ASCII"));
    }

    public void close() throws IOException {

            if (outStream != null) {
                outStream.close();
                outStream = null;
            }
            if (socket != null) {
                socket.disconnect();
                socket.close();
                socket = null;
            }
    }

    public PrintStream getOut() {
        return outStream;
    }

    public String readString() throws SocketException, IOException {
        DatagramPacket pack = new DatagramPacket(byteBuffer, byteBuffer.length);
        if (verbose)
            System.err.println("listening at:" + portNumber + "...");
        socket.receive(pack);
        String payload = (new String(pack.getData(), 0, pack.getLength(), IrpUtils.dumbCharset));
        InetAddress a = pack.getAddress();
        int port = pack.getPort();
        if (verbose)
            System.err.println("Got package for " + a + ":" + port + ": " + payload);
        //socket.disconnect();
        //socket.close();
        return payload;
    }

    public boolean isValid() {
        return socket != null;
    }

    public void setTimeout(int timeout) throws SocketException {
        socket.setSoTimeout(timeout);
    }

    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

    public void setDebug(int debug) {
    }

    private class FilteredStream extends FilterOutputStream {

        FilteredStream(OutputStream stream) {
            super(stream);
        }

        @Override
        public void write(byte b[]) throws IOException {
            send(b);
        }

        @Override
        public void write(byte b[], int off, int len) throws IOException {
            byte[] buf = new byte[len];
            System.arraycopy(b, off, buf, 0, len);
            send(buf);
        }
    }
}
