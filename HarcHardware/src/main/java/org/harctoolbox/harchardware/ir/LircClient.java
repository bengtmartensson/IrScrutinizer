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

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.beust.jcommander.Parameters;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import org.harctoolbox.IrpMaster.IrpUtils;
import org.harctoolbox.harchardware.IHarcHardware;
import org.harctoolbox.harchardware.Version;
import org.harctoolbox.harchardware.comm.TcpSocketChannel;
import org.harctoolbox.harchardware.comm.TcpSocketPort;

/**
 * A <a href="http://www.lirc.org">LIRC</a> client, talking to a remote LIRC
 * server through a TCP port.
 * Functionally, it resembles the command line program irsend.
 */
public class LircClient implements IHarcHardware, IRemoteCommandIrSender, IIrSenderStop, ITransmitter, Serializable {

    public final static int lircDefaultPort = 8765;
    public final static String defaultLircIP = "127.0.0.1"; // localhost
    public final static int defaultTimeout = 5000; // WinLirc can be really slow...
    private final static int P_BEGIN = 0;
    private final static int P_MESSAGE = 1;
    private final static int P_STATUS = 2;
    private final static int P_DATA = 3;
    private final static int P_N = 4;
    private final static int P_DATA_N = 5;
    private final static int P_END = 6;
    private static JCommander argumentParser;
    private static CommandLineArgs commandLineArgs = new CommandLineArgs();

    private static void usage(int exitcode) {
        StringBuilder str = new StringBuilder(256);
        argumentParser.usage(str);

        (exitcode == IrpUtils.exitSuccess ? System.out : System.err).println(str);
        doExit(exitcode); // placifying FindBugs...
    }

    private static void doExit(int exitcode) {
        System.exit(exitcode);
    }

    private static void doExit(boolean success) {
        if (!success)
            System.err.println("Failed");
        System.exit(success ? IrpUtils.exitSuccess : IrpUtils.exitIoError);
    }
    private static void doExit(String message, int exitcode) {
        System.err.println(message);
        System.exit(exitcode);
    }

    /**
     * @param args the command line arguments.
     */
    public static void main(String[] args) {
        argumentParser = new JCommander(commandLineArgs);
        argumentParser.setCaseSensitiveOptions(false);
        argumentParser.setAllowAbbreviatedOptions(true);
        argumentParser.setProgramName("LircClient");

        CommandSendOnce cmdSendOnce = new CommandSendOnce();
        argumentParser.addCommand("send_once", cmdSendOnce);
        CommandSendStart cmdSendStart = new CommandSendStart();
        argumentParser.addCommand("send_start", cmdSendStart);
        CommandSendStop cmdSendStop = new CommandSendStop();
        argumentParser.addCommand("send_stop", cmdSendStop);
        CommandList cmdList = new CommandList();
        argumentParser.addCommand("list", cmdList);
        CommandSetTransmitters cmdSetTransmitters = new CommandSetTransmitters();
        argumentParser.addCommand("set_transmitters", cmdSetTransmitters);
        CommandSimulate cmdSimulate = new CommandSimulate();
        argumentParser.addCommand("simulate", cmdSimulate);
        CommandVersion cmdVersion = new CommandVersion();
        argumentParser.addCommand("version", cmdVersion);

        try {
            argumentParser.parse(args);
        } catch (ParameterException ex) {
            System.err.println(ex.getMessage());
            usage(IrpUtils.exitUsageError);
        }

        if (commandLineArgs.helpRequested)
            usage(IrpUtils.exitSuccess);

        if (commandLineArgs.versionRequested) {
            System.out.println(Version.versionString);
            doExit(true);
        }

        String[] splitAddress = commandLineArgs.address.split(":");
        String hostname = splitAddress[0];
        int port = splitAddress.length == 2 ? Integer.parseInt(splitAddress[1]) : commandLineArgs.port;
        try (LircClient lircClient = new LircClient(hostname, port,
                commandLineArgs.verbose, commandLineArgs.timeout)) {

            if (commandLineArgs.listen) {
                lircClient.readLoop();
                System.exit(IrpUtils.exitSuccess);
            }

            if (argumentParser.getParsedCommand() == null)
                usage(IrpUtils.exitUsageError);
            boolean success = true;
            switch (argumentParser.getParsedCommand()) {
                case "send_once":
                    String remote = cmdSendOnce.commands.get(0);
                    cmdSendOnce.commands.remove(0);
                    for (String command : cmdSendOnce.commands) {
                        success = lircClient.sendIrCommand(remote, command, cmdSendOnce.count, null);
                        if (!success)
                            break;
                    }
                    break;
                case "send_start":
                    success = lircClient.sendIrCommandRepeat(cmdSendStart.args.get(0), cmdSendStart.args.get(1), null);
                    break;
                case "send_stop":
                    success = lircClient.stopIr(cmdSendStop.args.get(0), cmdSendStop.args.get(1), null);
                    break;
                case "list":
                    String[] result = cmdList.remote.isEmpty() ? lircClient.getRemotes()
                            : lircClient.getCommands(cmdList.remote.get(0));
                    for (String s : result)
                        System.out.println(s);
                    break;
                case "set_transmitters":
                    if (cmdSetTransmitters.transmitters.size() < 1)
                        doExit("Command \"set_transmitters\" requires at least one argument", IrpUtils.exitUsageError);

                    LircTransmitter xmitter = new LircTransmitter(cmdSetTransmitters.transmitters);
                    success = lircClient.setTransmitters(xmitter);
                    break;
                case "simulate":
                    doExit("Command \"simulate\" not implemented", IrpUtils.exitUsageError);
                    break;
                case "version":
                    System.out.println(lircClient.getVersion());
                    break;
                default:
                    doExit("Unknown command", IrpUtils.exitUsageError);
            }
            doExit(success);
        } catch (IOException ex) {
            doExit(ex.getMessage(), IrpUtils.exitFatalProgramFailure);
        } catch (IndexOutOfBoundsException ex) {
            doExit("Too few arguments to command", IrpUtils.exitUsageError);
        } catch (NoSuchTransmitterException ex) {
            doExit("No such transmitter " + ex.getMessage(), IrpUtils.exitSemanticUsageError);
        }
    }
    private String lircServerIp;
    private int lircPort;

    private final int portMin = 1;
    private final int portMax = 8;

    private boolean verbose = true;
    private int debug = 0;
    private int timeout = defaultTimeout;

    private InetAddress inetAddress = null;

    private String lastRemote = null;
    private String lastCommand = null;

    private String version = null;

    private LircTransmitter lircTransmitter;

    public LircClient(String hostname, int port, boolean verbose, int timeout) throws UnknownHostException, IOException {
        this.lircTransmitter = new LircTransmitter();
        this.timeout = timeout;
        lircServerIp = (hostname != null) ? hostname : defaultLircIP;
        inetAddress = InetAddress.getByName(hostname);
        lircPort = port;
        this.verbose = verbose;
        String[] result = sendCommand("VERSION", false);
        version = (result == null || result.length == 0) ? null : result[0];
    }

    public LircClient(String hostname, boolean verbose, int timeout) throws UnknownHostException, IOException {
        this(hostname, lircDefaultPort, verbose, timeout);
    }

    public LircClient(String hostname, boolean verbose) throws UnknownHostException, IOException {
        this(hostname, verbose, defaultTimeout);
    }

    public LircClient(String hostname) throws UnknownHostException, IOException {
        this(hostname, false);
    }

    @Override
    public void close() {
    }

    @Override
    public void open() {
    }

    @Override
    public LircTransmitter getTransmitter() {
        return new LircTransmitter();
    }

    public LircTransmitter getTransmitter(int portNo) throws NoSuchTransmitterException {
        if (portNo < portMin || portNo > portMax)
            throw new NoSuchTransmitterException(Integer.toString(portNo));
        return new LircTransmitter(portNo);
    }

    @Override
    public LircTransmitter getTransmitter(String port) throws NoSuchTransmitterException {
        return new LircTransmitter(port);
    }

    @Override
    public String[] getTransmitterNames() {
        String[] result = new String[portMax - portMin + 2];
        int index = 0;
        result[index] = "default";
        index++;
        for (int i = portMin; i <= portMax; i++) {
            result[index] = Integer.toString(i);
            index++;
        }

        return result;
    }

    @Override
    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

    @Override
    public void setDebug(int debug) {
        this.debug = debug;
    }


    @Override
    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }


    protected void readLoop() throws IOException {
        TcpSocketChannel tcpSocketChannel = new TcpSocketChannel(lircServerIp, lircPort,
                0 /*timeout*/, verbose, TcpSocketPort.ConnectionMode.keepAlive);
        tcpSocketChannel.connect();
        //OutputStream outToServer = tcpSocketChannel.getOut();
        BufferedReader inFromServer = tcpSocketChannel.getBufferedIn();
        if (inFromServer == null)
            throw new IOException("Could not open socket connection to LIRC server " + lircServerIp);
        while (true) {
            String line = inFromServer.readLine();
            System.out.println(line);
        }
    }

    protected final String[] sendCommand(String packet, boolean oneWord) throws IOException {
        if (verbose)
            System.err.println("Sending command `" + packet + "' to Lirc@" + lircServerIp);

        TcpSocketChannel tcpSocketChannel = new TcpSocketChannel(lircServerIp, lircPort,
                timeout, verbose, TcpSocketPort.ConnectionMode.justInTime);
        tcpSocketChannel.connect();
        OutputStream outToServer = tcpSocketChannel.getOut();
        BufferedReader inFromServer = tcpSocketChannel.getBufferedIn();
        if (outToServer == null || inFromServer == null)
            throw new IOException("Could not open socket connection to LIRC server " + lircServerIp);

        tcpSocketChannel.sendString(packet + '\n');

        ArrayList<String> result = new ArrayList<>(8);
        int status = 0;
        try {
            int state = P_BEGIN;
            int n = 0;
            boolean done = false;
            int dataN = -1;

            while (!done) {
                String string = inFromServer.readLine();
                if (verbose)
                    System.err.println("Received `" + string + "'");
                if (string == null) {
                    done = true;
                    status = -1;
                } else {
                    OUTER:
                    switch (state) {
                        case P_BEGIN:
                            if (!string.equals("BEGIN")) {
                                System.err.println("!begin");
                                continue;
                            }
                            state = P_MESSAGE;
                            break;
                        case P_MESSAGE:
                            if (!string.trim().equalsIgnoreCase(packet)) {
                                state = P_BEGIN;
                                continue;
                            }
                            state = P_STATUS;
                            break;
                        case P_STATUS:
                            switch (string) {
                                case "SUCCESS":
                                    status = 0;
                                    break;
                                case "END":
                                    status = 0;
                                    done = true;
                                    break;
                                case "ERROR":
                                    System.err.println("command failed: " + packet);
                                    status = -1;
                                    break;
                                default:
                                    throw new BadPacketException();
                            }
                            state = P_DATA;
                            break;
                        case P_DATA:
                            switch (string) {
                                case "END":
                                    done = true;
                                    break OUTER;
                                case "DATA":
                                    state = P_N;
                                    break OUTER;
                            }
                            throw new BadPacketException();
                        case P_N:
                            //errno = 0;
                            dataN = Integer.parseInt(string);
                            //result = new String[data_n];

                            state = dataN == 0 ? P_END : P_DATA_N;
                            break;
                        case P_DATA_N:
                            // Different LIRC servers seems to deliver commands in different
                            // formats. Just take the last word.
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
        } finally {
            try {
                tcpSocketChannel.close(true);
            } catch (IOException e) {
                System.err.println(e.getMessage());
            }
        }
        if (verbose)
            System.err.println("Lirc command " + (status == 0 ? "succeded." : "failed."));

        return status == 0 && result != null ? result.toArray(new String[result.size()]) : null;
    }

    @Override
    public boolean sendIrCommand(String remote, String command, int count, Transmitter transmitter) throws IOException, NoSuchTransmitterException {
        this.lastRemote = remote;
        this.lastCommand = command;
        if (transmitter != null) {
            if (!LircTransmitter.class.isInstance(transmitter))
                throw new NoSuchTransmitterException(transmitter);
            LircTransmitter trans = (LircTransmitter) transmitter;
            if (!trans.isTrivial()) {
                boolean result = setTransmitters(transmitter);
                if (!result)
                    throw new NoSuchTransmitterException("Error selecting transmitter " + transmitter);
            }
        }
        return sendCommand("SEND_ONCE " + remote + " " + command + " " + (count - 1), false) != null;
    }

    public boolean sendIrCommand(String remote, String command, int count, int connector) throws IOException, NoSuchTransmitterException {
        return sendIrCommand(remote, command, count, new LircTransmitter(connector));
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
        return stopIr(remote, command, new LircTransmitter(port));
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

    /**
     * Sends the SET_TRANSMITTER command to the LIRC server.
     * @param transmitter
     * @return
     * @throws IOException
     * @throws NoSuchTransmitterException
     */
    public boolean setTransmitters(Transmitter transmitter) throws NoSuchTransmitterException, IOException {
        if (transmitter == null)
            return true;
        if (!LircTransmitter.class.isInstance(transmitter))
            throw new NoSuchTransmitterException(transmitter);
        lircTransmitter = (LircTransmitter) transmitter;
        return setTransmitters();
    }

    public boolean setTransmitters(int port) throws NoSuchTransmitterException, IOException {
        return setTransmitters(new LircTransmitter(port));
    }

    public boolean setTransmitters(boolean[] ports) throws NoSuchTransmitterException, IOException {
        LircTransmitter transmitter = new LircTransmitter(ports);
        return setTransmitters(transmitter);
    }

    private boolean setTransmitters() throws IOException {
        if (lircTransmitter.isTrivial())
            return true;

        String s = "SET_TRANSMITTERS " + lircTransmitter.toString();
        return sendCommand(s, false) != null;
    }

    @Override
    public String getVersion() throws IOException {
        return version;
    }

    /**
     * Dummy implementation, always returns true
     * @return true
     */
    @Override
    public boolean isValid() {
        return version != null;
    }

    private static class BadPacketException extends Exception {
        BadPacketException() {
            super();
        }

        BadPacketException(String message) {
            super(message);
        }
    }

    private final static class CommandLineArgs {

        @Parameter(names = {"-a", "--address"}, description = "IP name or address of lircd host")
        private String address = "localhost";

        @Parameter(names = {"-h", "--help", "-?"}, description = "Display help message")
        private boolean helpRequested = false;

        @Parameter(names = {"-l", "--listen"}, description = "Listen for messages, like irw")
        private boolean listen = false;

        @Parameter(names = {"-p", "--port"}, description = "Port of lircd")
        private int port = 8765;

        @Parameter(names = {"-t", "--timeout"}, description = "Timeout in milliseconds")
        private int timeout = 5000;

        @Parameter(names = {"--version"}, description = "Display version information")
        private boolean versionRequested;

        @Parameter(names = {"-v", "--verbose"}, description = "Have some commands executed verbosely")
        private boolean verbose;
    }

    @Parameters(commandDescription = "Send one or many commands")
    public final static class CommandSendOnce {
        @Parameter(names = {"-#", "-c", "--count"}, description = "Number of times to send command in send_once")
        private int count = 1;

        @Parameter(description = "remote command...")
        private List<String> commands = new ArrayList<>(16);
    }

    @Parameters(commandDescription = "Send one commands many times")
    public final static class CommandSendStart {
        @Parameter(arity = 2, description = "remote command")
        @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
        private List<String> args = new ArrayList<>(2);
    }

    @Parameters(commandDescription = "Send one commands many times")
    public final static class CommandSendStop {
        @Parameter(arity = 2, description = "remote command")
        @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
        private List<String> args = new ArrayList<>(2);
    }

    @Parameters(commandDescription = "Inquire the known remotes, or the commands in a remote")
    public final static class CommandList {
        @Parameter(arity = 1, description = "[remote]")
        @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
        private List<String> remote = new ArrayList<>(1);
    }

    @Parameters(commandDescription = "Set transmitters")
    public final static class CommandSetTransmitters {
        @Parameter(description = "transmitter...")
        private List<Integer> transmitters = new ArrayList<>(8);
    }

    @Parameters(commandDescription = "Simulate sending")
    @SuppressWarnings("ClassMayBeInterface")
    public final static class CommandSimulate {
    }

    @Parameters(commandDescription = "Inquire version of lircd")
    @SuppressWarnings("ClassMayBeInterface")
    public final static class CommandVersion {
    }

}
