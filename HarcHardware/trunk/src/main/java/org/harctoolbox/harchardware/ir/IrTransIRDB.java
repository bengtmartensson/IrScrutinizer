/*
Copyright (C) 2009-2012 Bengt Martensson.

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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.*;
import java.util.ArrayList;
import org.harctoolbox.IrpMaster.IrpUtils;

public class IrTransIRDB extends IrTrans implements IRemoteCommandIrSender {
    private final static String sendFlashedCommandAck = "**00018 RESULT OK";

    public IrTransIRDB(String hostname, boolean verbose, int timeout, Interface interfaze) throws UnknownHostException {
        super(hostname, verbose, timeout, interfaze);
    }

    public IrTransIRDB(String hostname, boolean verbose, int timeout) throws UnknownHostException {
        super(hostname, verbose, timeout);
    }

    public IrTransIRDB(String hostname, boolean verbose) throws UnknownHostException {
        super(hostname, verbose);
    }

    public IrTransIRDB(String hostname) throws UnknownHostException {
        super(hostname);
    }

    private String[] getTable(String str) throws IOException, NumberFormatException {
        if (verbose)
            System.err.println("Sending command `" + str + "0' to IrTrans");

        //Socket sock = new Socket(InetAddress.getByName(irTransIP), portNumber);
        Socket sock = new Socket();
        sock.connect(new InetSocketAddress(inetAddress, portNumber), timeout);
        PrintStream outToServer = new PrintStream(sock.getOutputStream(), false, IrpUtils.dumbCharsetName);
        BufferedReader inFromServer = new BufferedReader(new InputStreamReader(sock.getInputStream(), IrpUtils.dumbCharset));

        ArrayList<String> items = new ArrayList<String>();
        try {
            outToServer.print("ASCI");
            try {
                Thread.sleep(dummyDelay);
            } catch (InterruptedException ex) {
            }
            int index = 0;
            int noRemotes = 99999;
            while (index < noRemotes) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ex) {
                }
                outToServer.print(str + index + "\r");
                String result = inFromServer.readLine(); // hangs here sometimes
                int secondSpace = result.indexOf(' ', 9);
                if (verbose) {
                    System.err.println(result);
                }
                String[] words = result.substring(secondSpace + 1, result.length()).split(",");
                index = Integer.parseInt(words[0]);
                noRemotes = Integer.parseInt(words[1]);
                int chunk = Integer.parseInt(words[2]);

                for (int c = 0; c < chunk; c++) {
                    items.add(words[3 + c]);
                }

                index += chunk;
            }
        } catch (NumberFormatException ex) {
            throw ex;
        } finally {
            outToServer.close();
            inFromServer.close();
            sock.close();
        }
        return items.toArray(new String[items.size()]);
    }

    @Override
    public String[] getRemotes() throws IOException {
        return getTable("Agetremotes ");
    }

    @Override
    public String[] getCommands(String remote) throws IOException {
        return getTable("Agetcommands " + remote + ",");
    }

    public static String makeUrl(String hostname, String remote, String command, Led led) {
        return "http://" + (hostname != null ? hostname : defaultIrTransIP)
                + "/send.htm?remote=" + remote + "&command=" + command + "&led=" + Led.ledChar(led);
    }

    public String makeUrl(String remote, String command, Led led) {
        return makeUrl(this.irTransIP, remote, command, led);
    }

    private boolean sendFlashedCommandHttp(String remote, String command, Led led) throws MalformedURLException, IOException {
        return getUrl(makeUrl(remote, command, led));
    }

    @Override
    public boolean sendIrCommand(String remote, String command, int count, Transmitter transmitter) throws IOException, NoSuchTransmitterException {
        if (!IrTransTransmitter.class.isInstance(transmitter))
            throw new NoSuchTransmitterException(transmitter);
        boolean success = true;
        Led led = ((IrTransTransmitter) transmitter).getLed();
        for (int i = 0; i < count; i++)
            success = success && sendIrCommand(remote, command, i > 0, led);

        return success;
    }

    public boolean sendIrCommand(String remote, String command, int count, Led led) throws IOException, NoSuchTransmitterException {
        return sendIrCommand(remote, command, count, newTransmitter(led));
    }

    @Override
    public boolean sendIrCommandRepeat(String remote, String command, Transmitter transmitter) throws IOException, NoSuchTransmitterException {
        return sendIrCommand(remote, command, IRemoteCommandIrSender.repeatMax, transmitter);
    }

    private boolean sendIrCommand(String remote, String command, boolean repeat,
            Led led) throws IOException {
        return sendIrCommand(remote, command, repeat, led, this.interfaze);
    }

    private boolean sendIrCommand(String remote, String command, boolean repeat,
            Led led, IrTrans.Interface interf) throws IOException {
        boolean result;
        switch (interf) {
            case tcpAscii:
                result = sendFlashedCommandTCP(remote, command, led, repeat);
                break;
            case udp:
                result = sendFlashedCommandUdp(remote, command, led, repeat);
                break;
            case http:
                result = sendFlashedCommandHttp(remote, command, led);
                break;
            default:
                throw new IllegalArgumentException("Sending named commands on IrTrans using interface "
                        + interfaze + " not supported");
                //break;
        }
        return result;
    }

    private boolean sendFlashedCommandTCP(String remote, String command, Led led, boolean repeat) throws IOException {
        return sendCommand("Asnd" + (repeat ? "r" : "") + " " + remote + "," + command + "," + Led.ledChar(led)).equals(sendFlashedCommandAck);
    }

     private boolean sendFlashedCommandUdp(String remote, String command,
            Led led, boolean repeat) throws IOException {
        return sendCommandUdp("snd" + (repeat ? "r" : "") + " " + remote + "," + command + "," + Led.ledChar(led));
    }

    //@Override
    //public boolean stopIr(String remote, String command, Transmitter transmitter) {
    //    return stopIr(transmitter);
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
        String configfilename = "listen.xml";

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
            IrTransIRDB irt = new IrTransIRDB(IrTransHost, verbose, defaultTimeout, IrTrans.Interface.tcpAscii);
            if (verbose)
                System.out.println(irt.getVersion());

            if (args.length > optarg && args[optarg].equals("-r")) {
                if (args.length == optarg + 1) {
                    String[] remotes = irt.getRemotes();
                    for (String remote : remotes) {
                        System.err.println(remote);
                    }
                } else {
                    String remote = args[optarg + 1];
                    String[] commands = irt.getCommands(remote);
                    for (String command : commands) {
                        System.err.println(command);
                    }
                }
            } else if (args.length > optarg && args[optarg].equals("-c")) {
                StringBuilder ccf = new StringBuilder();
                for (int i = optarg+1; i < args.length; i++)
                    ccf.append(' ').append(args[i]);
                irt.sendCcf(ccf.toString(), 1, Led.intern);
            }
        } catch (IOException e) {
             System.err.println(e.getMessage());
            usage(IrpUtils.exitUsageError);
        }
    }
}
