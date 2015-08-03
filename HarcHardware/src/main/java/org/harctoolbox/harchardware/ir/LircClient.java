/*
Copyright (C) 2009-2013 Bengt Martensson.

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

import org.harctoolbox.harchardware.comm.TcpSocketPort;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import org.harctoolbox.harchardware.IHarcHardware;
import org.harctoolbox.harchardware.comm.TcpSocketChannel;

/**
 * A <a href="http://www.lirc.org">LIRC</a> client, talking to a remote LIRC
 * server through a TCP port.
 */
public class LircClient implements IHarcHardware, IRemoteCommandIrSender,IIrSenderStop, ITransmitter {

    private String lircServerIp;
    public final static int lircDefaultPort = 8765;
    private int lircPort;
    public final static String defaultLircIP = "127.0.0.1"; // localhost
    public final static int defaultTimeout = 5000; // WinLirc can be really slow...

    private final int portMin = 1;
    private final int portMax = 8;

    private boolean verbose = true;
    private int debug = 0;
    private int timeout = defaultTimeout;

    private InetAddress inetAddress = null;

    private String lastRemote = null;
    private String lastCommand = null;

    public LircClient(String hostname, int port, boolean verbose, int timeout) throws UnknownHostException {
        this.timeout = timeout;
        lircServerIp = (hostname != null) ? hostname : defaultLircIP;
        inetAddress = InetAddress.getByName(hostname);
        lircPort = port;
        this.verbose = verbose;
    }

    public LircClient(String hostname, boolean verbose, int timeout) throws UnknownHostException {
        this(hostname, lircDefaultPort, verbose, timeout);
    }

    public LircClient(String hostname, boolean verbose) throws UnknownHostException {
        this(hostname, verbose, defaultTimeout);
    }

    public LircClient(String hostname) throws UnknownHostException {
        this(hostname, false);
    }

    @Override
    public void close() {
    }

    @Override
    public void open() {
    }

    public static class LircIrTransmitter extends Transmitter {
        private int[] transmitters;

        private LircIrTransmitter() {
            transmitters = null;
        }

        private LircIrTransmitter(int[] transmitters) {
            this.transmitters = transmitters;
        }

        private LircIrTransmitter(int selectedTransmitter) {
            if (selectedTransmitter >= 0) {
                transmitters = new int[1];
                transmitters[0] = selectedTransmitter;
            }
        }

        private LircIrTransmitter(boolean[] ports) {
            ArrayList<Integer> prts = new ArrayList<Integer>();
            for (int i = 0; i < ports.length; i++)
                if (ports[i])
                    prts.add(i+1);

            transmitters = new int[prts.size()];
            for (int i = 0; i < prts.size(); i++)
                transmitters[i] = prts.get(i);
        }

        @Override
        public String toString() {
            if (transmitters == null || transmitters.length == 0)
                return null;
            StringBuilder s = new StringBuilder();
            for (int i = 0; i < transmitters.length; i++)
                s.append(' ').append(transmitters[i]);

            return s.toString();
        }
    }

    @Override
    public LircIrTransmitter getTransmitter() {
        return new LircIrTransmitter();
    }

    public LircIrTransmitter getTransmitter(int portNo) throws NoSuchTransmitterException {
        if (portNo < portMin || portNo > portMax)
            throw new NoSuchTransmitterException(Integer.toString(portNo));
        return new LircIrTransmitter(portNo);
    }

    @Override
    public LircIrTransmitter getTransmitter(String port) throws NoSuchTransmitterException {
        if (port.equals("default"))
            return null;
        try {
            int portNo = Integer.parseInt(port);
            return getTransmitter(portNo);
        } catch (NumberFormatException ex) {
            throw new NoSuchTransmitterException(port);
        }
    }

    @Override
    public String[] getTransmitterNames() {
        String[] result = new String[portMax - portMin + 2];
        int index = 0;
        result[index++] = "default";
        for (int i = portMin; i <= portMax; i++)
            result[index++] = Integer.toString(i);

        return result;
    }

    private LircIrTransmitter lircIrTransmitter = new LircIrTransmitter();

    @Override
    public void setVerbosity(boolean verbosity) {
        this.verbose = verbosity;
    }

    @Override
    public void setDebug(int debug) {
        this.debug = debug;
    }

    private final static int P_BEGIN = 0;
    private final static int P_MESSAGE = 1;
    private final static int P_STATUS = 2;
    private final static int P_DATA = 3;
    private final static int P_N = 4;
    private final static int P_DATA_N = 5;
    private final static int P_END = 6;

    @Override
    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    private static class BadPacketException extends Exception {
        private static final long serialVersionUID = 1L;

        BadPacketException() {
            super();
        }

        BadPacketException(String message) {
            super(message);
        }
    }

    protected String[] sendCommand(String packet, boolean oneWord) throws IOException {
        if (verbose) {
            System.err.println("Sending command `" + packet + "' to Lirc@" + lircServerIp);
        }

        TcpSocketChannel tcpSocketChannel = new TcpSocketChannel(lircServerIp, lircPort,
                timeout, verbose, TcpSocketPort.ConnectionMode.justInTime);
        tcpSocketChannel.connect();
        OutputStream outToServer = tcpSocketChannel.getOut();
        BufferedReader inFromServer = tcpSocketChannel.getBufferedIn();
        if (outToServer == null || inFromServer == null)
            throw new IOException("Could not open socket connection to LIRC server " + lircServerIp);

        tcpSocketChannel.sendString(packet + '\n');

        ArrayList<String> result = new ArrayList<String>();
        int status = 0;
        try {
            int state = P_BEGIN;
            int n = 0;
            boolean done = false;
            //int errno = 0;
            int dataN = -1;

            while (!done) {
                String string = inFromServer.readLine();
                //System.out.println("***"+string+"***"+state);
                if (string == null) {
                    done = true;
                    status = -1;
                } else {
                    switch (state) {
                        case P_BEGIN:
                            if (!string.equals("BEGIN")) {
                                System.err.println("!begin");
                                continue;
                            }
                            state = P_MESSAGE;
                            break;
                        case P_MESSAGE:
                            if (!string.equals(packet)) {
                                state = P_BEGIN;
                                continue;
                            }
                            state = P_STATUS;
                            break;
                        case P_STATUS:
                            if (string.equals("SUCCESS")) {
                                status = 0;
                            } else if (string.equals("END")) {
                                status = 0;
                                done = true;
                            } else if (string.equals("ERROR")) {
                                System.err.println("command failed: " + packet);
                                status = -1;
                            } else {
                                throw new BadPacketException();
                            }
                            state = P_DATA;
                            break;
                        case P_DATA:
                            if (string.equals("END")) {
                                done = true;
                                break;
                            } else if (string.equals("DATA")) {
                                state = P_N;
                                break;
                            }
                            throw new BadPacketException();
                        case P_N:
                            //errno = 0;
                            dataN = Integer.parseInt(string);
                            //result = new String[data_n];

                            state = dataN == 0 ? P_END : P_DATA_N;
                            break;
                        case P_DATA_N:
                            if (verbose) {
                                System.err.println(string);
                            }
                            // Different LIRC servers seems to deliver commands in different
                            // formats. Just take the last word.
                            //result[n++] = one_word ? string.replaceAll("\\S*\\s+", "") : string;

                            result.add(oneWord ? string.replaceAll("\\S*\\s+", "") : string);
                            n++;
                            if (n == dataN) {
                                state = P_END;
                            }
                            break;
                        case P_END:
                            if (string.equals("END")) {
                                done = true;
                            } else {
                                throw new BadPacketException();
                            }
                            break;
                        default:
                            assert false : "Unhandled case";
                            break;
                    }
                }
            }
        } catch (BadPacketException e) {
            System.err.println("bad return packet");
            status = -1;
        } catch (SocketTimeoutException e) {
            System.err.println("Sockettimeout Lirc: " + e.getMessage());
            result = null;
            status = -1;
        } catch (IOException e) {
            System.err.println("Couldn't read from " + lircServerIp);
            status = -1;
            //System.exit(1);
        } finally {
            try {
                tcpSocketChannel.close(true);
            } catch (IOException e) {
                System.err.println(e.getMessage());
                //System.exit(1);
            }
        }
        if (verbose) {
            System.err.println(status == 0 ? "Lirc command succeded."
                    : "Lirc command failed.");
        }
        if (result != null && !result.isEmpty() && verbose) {
            System.err.println("result[0] = " + result.get(0));
        }

        return status == 0 && result != null ? result.toArray(new String[result.size()]) : null;
    }

    @Override
    public boolean sendIrCommand(String remote, String command, int count, Transmitter transmitter) throws IOException, NoSuchTransmitterException {
        this.lastRemote = remote;
        this.lastCommand = command;
        if (transmitter != null) {
            if (!LircIrTransmitter.class.isInstance(transmitter))
                throw new NoSuchTransmitterException(transmitter);
            LircIrTransmitter trans = (LircIrTransmitter) transmitter;
            if (trans.transmitters != null /*&& (this.lircIrTransmitter.transmitters == null
                    || this.lircIrTransmitter.transmitters[0] != trans.transmitters[0])*/) {
                boolean result = setTransmitters(transmitter);
                if (!result)
                    throw new NoSuchTransmitterException("Error selecting transmitter " + transmitter);
            }
        }
        return sendCommand("SEND_ONCE " + remote + " " + command + " " + (count - 1), false) != null;
    }

    public boolean sendIrCommand(String remote, String command, int count, int connector) throws IOException, NoSuchTransmitterException {
        return sendIrCommand(remote, command, count, new LircIrTransmitter(connector));
    }

    @Override
    public boolean sendIrCommandRepeat(String remote, String command, Transmitter transmitter) throws IOException, NoSuchTransmitterException {
        this.lastRemote = remote;
        this.lastCommand = command;
        return setTransmitters(transmitter)
                && sendCommand("SEND_START " + remote + " " + command, false) != null;
    }

    public boolean stopIr(String remote, String command, Transmitter transmitter) throws IOException, NoSuchTransmitterException {
            return setTransmitters(transmitter)
                    && sendCommand("SEND_STOP " + remote + " " + command, false) != null;
    }

    public boolean stopIr(String remote, String command, int port) throws IOException, NoSuchTransmitterException {
        return stopIr(remote, command, new LircIrTransmitter(port));
    }

    @Override
    public boolean stopIr(Transmitter transmitter) throws IOException, NoSuchTransmitterException {
        return stopIr(lastRemote, lastCommand, transmitter);
    }

    @Override
    public String[] getRemotes() throws IOException {
        return sendCommand("LIST", false);
    }

    @Override
    public String[] getCommands(String remote) throws IOException {
        if (remote == null || remote.isEmpty())
            throw new NullPointerException("Null remote");
        return sendCommand("LIST " + remote, true);
    }

    // Questionable
    /*public String getRemoteCommand(String remote, String command) throws HarcHardwareException {
        try {
            String[] result = sendCommand("LIST " + remote + " " + command, false);
            return result != null ? result[0] : null;
        } catch (IOException ex) {
            throw new HarcHardwareException(ex);
        }
    }*/

    /**
     * Sends the SET_TRANSMITTER command to the LIRC server.
     * @param transmitter
     * @return
     * @throws IOException
     * @throws NoSuchTransmitterException
     */
    public boolean setTransmitters(Transmitter transmitter) throws IOException, NoSuchTransmitterException {
        if (!LircIrTransmitter.class.isInstance(transmitter))
            throw new NoSuchTransmitterException(transmitter);
        LircIrTransmitter trans = (LircIrTransmitter) transmitter;
        lircIrTransmitter = trans;
        return setTransmitters();
    }

    public boolean setTransmitters(int port) throws IOException, NoSuchTransmitterException {
        return setTransmitters(new LircIrTransmitter(port));
    }

    public boolean setTransmitters(boolean[] ports) throws NoSuchTransmitterException, IOException {
        LircIrTransmitter transmitter = new LircIrTransmitter(ports);
        if (transmitter.transmitters.length == 0)
            throw new NoSuchTransmitterException("Cannot disable all transmitters");
        return setTransmitters(transmitter);
    }

    private boolean setTransmitters(/*int[] trans*/) throws IOException {
        if (lircIrTransmitter.transmitters != null) {
            String s = "SET_TRANSMITTERS" + lircIrTransmitter.toString();
            return sendCommand(s, false) != null;
        }
        return true;
    }

    @Override
    public String getVersion() throws IOException {
        String[] result = sendCommand("VERSION", false);
        return (result == null || result.length == 0) ? null : result[0];
    }

    /**
     * Dummy implementation, always returns true
     * @return true
     */
    @Override
    public boolean isValid() {
        return true;
    }
}
