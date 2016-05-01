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

import org.harctoolbox.harchardware.comm.IWeb;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.*;
import org.harctoolbox.IrpMaster.IncompatibleArgumentException;
import org.harctoolbox.IrpMaster.IrSignal;
import org.harctoolbox.IrpMaster.IrpMasterException;
import org.harctoolbox.IrpMaster.IrpUtils;
import org.harctoolbox.harchardware.IHarcHardware;

public class IrTrans implements IHarcHardware, IRawIrSender, ITransmitter, IWeb {

    public final static String defaultIrTransIP = "192.168.0.32";
    protected final static int dummyDelay = 10; // milliseconds
    protected final static int defaultTimeout = 2000;

    protected int timeout = defaultTimeout;
    protected String irTransIP;
    protected InetAddress inetAddress = null;
    protected boolean verbose = true;

    /** port number, not possible to change. */
    public final static int portNumber = 21000;
    protected Interface interfaze = Interface.tcpAscii;

    @Override
    public void close() {
    }

    @Override
    public URI getUri(String user, String password) {
        try {
            return new URI("http", irTransIP, null, null);
        } catch (URISyntaxException ex) {
            return null;
        }
    }

    @Override
    public void open() {
    }

    @Override
    public void setDebug(int debug) {
    }

    /** Interface that can be used to command an IrTrans unit */
    public enum Interface {
        /** "Raw" tcp interface. Not really usable unless you are Marcus Müller. */
        tcp,

        /** ASCII TCP socket interface */
        tcpAscii,

        /** UDP interface */
        udp,

        /** http */
        http,

        /** presently not supported */
        serial,

        /** presently not supported */
        usb
    }

    public static class IrTransTransmitter extends Transmitter {

        private Led led = Led.intern;

        private IrTransTransmitter(Led led) {
            this.led = led;
        }

        private IrTransTransmitter(String str) throws NoSuchTransmitterException {
            led = Led.parse(str);
        }

        private IrTransTransmitter() {
            led = Led.intern;
        }

        private IrTransTransmitter(int c) {
            led = Led.values()[c - 1];
        }

        public Led getLed() {
            return led;
        }
    }

    @Override
    public IrTransTransmitter getTransmitter() {
        return new IrTransTransmitter();
    }

    //@Override
    //public IrTransTransmitter newTransmitter(int port) {
    //    return new IrTransTransmitter(port);
    //}

    public IrTransTransmitter newTransmitter(Led led) {
        return new IrTransTransmitter(led);
    }

    @Override
    public IrTransTransmitter getTransmitter(String port) throws NoSuchTransmitterException {
        return new IrTransTransmitter(Led.parse(port));
    }

    @Override
    public String[] getTransmitterNames() {
        String [] result = new String[Led.values().length];
        int i = 0;
        for (Led led : Led.values())
            result[i++] = led.name();
        return result;
    }

    /**
     * IR LEDs on the IrTrans
     */
    public enum Led {

        intern,
        extern,
        led0, // synonym default
        led1,
        led2,
        led3,
        led4,
        led5,
        led6,
        led7,
        led8,
        led9,
        led10,
        led11,
        led12,
        led13,
        led14,
        led15,
        all;

        public static String ledChar(Led l) {
            return "l" + (l == intern ? "i"
                    : l == extern ? "e"
                    : l == all ? "b"
                    : l.toString().substring(4));
        }

        public static Led parse(String s) throws NoSuchTransmitterException {
            try {
                return Led.valueOf(s);
            } catch (IllegalArgumentException ex) {
                throw new NoSuchTransmitterException(s);
            }
        }

        // questionable...
        //public static Led parse(int i) {
        //    return i == 2 ? Led.extern : Led.intern;
        //}
    }

    // so many char name[20] in itrans code (19?)
    //private final static int maxNameLength = 21;

    /**
     *
     * @param hostname
     * @param verbose
     * @param timeout
     * @param interfaze
     * @throws UnknownHostException
     */
    public IrTrans(String hostname, boolean verbose, int timeout, Interface interfaze) throws UnknownHostException {
        this.timeout = timeout;
        irTransIP = hostname != null ? hostname : defaultIrTransIP;
        inetAddress = InetAddress.getByName(irTransIP);
        this.verbose = verbose;
        this.interfaze = interfaze;
    }

    public IrTrans(String hostname, boolean verbose, int timeout) throws UnknownHostException {
        this(hostname, verbose, timeout, Interface.tcpAscii);
    }

    public IrTrans(String hostname, boolean verbose) throws UnknownHostException {
        this(hostname, verbose, defaultTimeout, Interface.tcpAscii);
    }

    public IrTrans(String hostname) throws UnknownHostException {
        this(hostname, false, defaultTimeout, Interface.tcpAscii);
    }

    @Override
    public boolean isValid() {
        return inetAddress != null;
    }

    @Override
    public void setVerbosity(boolean verbosity) {
        this.verbose = verbosity;
    }

    @Override
    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    // TODO: shorten the timeout if host does not respond
    protected String sendCommand(String cmd) throws IOException {
        if (verbose)
            System.err.println("Sending command `" + cmd + "' to IrTrans (tcp ascii)");

        Socket sock = null;
        PrintStream outToServer = null;
        BufferedReader inFromServer = null;
        String result = "";

        try {
            //sock = new Socket(InetAddress.getByName(irTransIP), portNumber);
            sock = new Socket();
            sock.connect(new InetSocketAddress(inetAddress, portNumber), timeout);
            sock.setSoTimeout(timeout);
            outToServer = new PrintStream(sock.getOutputStream(), false, IrpUtils.dumbCharsetName);
            inFromServer = new BufferedReader(new InputStreamReader(sock.getInputStream(), IrpUtils.dumbCharset));

            outToServer.print("ASCI");
            try {
                Thread.sleep(dummyDelay);
            } catch (InterruptedException ex) {
            }
            outToServer.print(cmd);
            while (!inFromServer.ready())
                try {
                Thread.sleep(20);
            } catch (InterruptedException ex) {
            }
            result = inFromServer.readLine();
        } finally {
            if (outToServer != null)
                outToServer.close();
            if (inFromServer != null)
                inFromServer.close();
            if (sock != null)
                sock.close();
        }
        if (verbose)
            System.err.println(result);
        return result;
    }

    @Override
    public String getVersion() throws IOException {
        return sendCommand("Aver");
    }

    protected boolean getUrl(String url) throws MalformedURLException, IOException {
        if (verbose)
            System.err.println("Getting URL " + url);

        (new URL(url)).openStream();
        return true;
    }

    protected boolean sendCommandUdp(String cmd) throws IOException {
        boolean success = false;
        if (verbose)
            System.err.println("Sending command `" + cmd + "' to IrTrans over UDP");

        try (DatagramSocket sock = new DatagramSocket()) {
            sock.setSoTimeout(timeout);
            byte[] buf = cmd.getBytes("US-ASCII");
            DatagramPacket dp = new DatagramPacket(buf, buf.length, inetAddress, portNumber);
            sock.send(dp);
            success = true;
        }
        return success;
    }

    public boolean sendCcf(String ccf, int count, Led led) throws IOException {
        boolean success = true;
        for (int c = 0; c < count; c++)
            success = success && sendCcf(ccf, c > 0, led);
        return success;
    }

    public boolean sendCcf(String ccf, int count, Transmitter transmitter) throws IOException, NoSuchTransmitterException {
        if (!IrTransTransmitter.class.isInstance(transmitter))
            throw new NoSuchTransmitterException(transmitter);
        IrTransTransmitter trans = (IrTransTransmitter) transmitter;
        return sendCcf(ccf, count, trans.led);
    }

    public boolean sendCcfRepeat(String ccf, Transmitter transmitter) throws IOException, NoSuchTransmitterException {
        return sendCcf(ccf, IRawIrSender.repeatMax, transmitter);
    }

    private boolean sendCcf(String ccf, boolean repeat, Led led) throws IOException {
        //try {
        return sendCommandUdp((repeat ? "sndccfr " : "sndccf ") + ccf.trim() + "," + Led.ledChar(led));
        //} catch (IOException ex) {
        //    throw new HarcHardwareException(ex);
        //}
    }

    private boolean sendIr(IrSignal code, boolean repeat, Led led)
            throws IncompatibleArgumentException, IOException {
        return sendCcf(code.ccfString(), repeat, led);
    }

    public boolean sendIr(IrSignal code, int count, Led led)
            throws IncompatibleArgumentException, IOException {
        boolean success = true;
        for (int c = 0; c < count; c++) {
            success = success && sendIr(code, c > 0, led);
        }
        return success;
    }

    @Override
    public boolean sendIr(IrSignal code, int count, Transmitter transmitter)
            throws IrpMasterException, IOException, NoSuchTransmitterException {
        if (!IrTransTransmitter.class.isInstance(transmitter))
            throw new NoSuchTransmitterException(transmitter);
        return sendIr(code, count, ((IrTransTransmitter) transmitter).led);
    }

    //@Override
    //public boolean stopIr(Transmitter transmitter) {
    //    throw new UnsupportedOperationException("stopIr not implemented in IrTrans.");
    //}

    private static void usage(int exitstatus) {
        System.err.println("Usage:");
        System.err.println("\tIrTrans [-v][-h <hostname>] -r [<remotename>]");
        //System.err.println("\tIrTrans [-v][-h <hostname>] listenfile");
        doExit(exitstatus);
    }

    private static void doExit(int exitcode) {
        System.exit(exitcode);
    }

    public static void main(String args[]) {
        boolean verbose = false;
        String IrTransHost = defaultIrTransIP;

        int optarg = 0;
        if (args.length > optarg && args[optarg].equals("-v")) {
            optarg++;
            verbose = true;
        }
        if (args.length > optarg + 1 && args[optarg].equals("-h")) {
            IrTransHost = args[optarg + 1];
            optarg += 2;
        }

        try {
            IrTrans irt = new IrTrans(IrTransHost, verbose, defaultTimeout, Interface.tcpAscii);
            if (verbose)
                System.out.println(irt.getVersion());

            if (args.length > optarg && args[optarg].equals("-c")) {
                StringBuilder ccf = new StringBuilder();
                for (int i = optarg+1; i < args.length; i++)
                    ccf.append(' ').append(args[i]);
                irt.sendCcf(ccf.toString(), 1, Led.intern);
            }
        } catch (IOException ex) {
            System.err.println(ex.getMessage());
            usage(IrpUtils.exitUsageError);
        }
    }
}
