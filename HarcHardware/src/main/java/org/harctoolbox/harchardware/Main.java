/*
Copyright (C) 2012, 2013, 2014 Bengt Martensson.

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

import org.harctoolbox.harchardware.ir.IRemoteCommandIrSender;
import org.harctoolbox.harchardware.ir.IrTransIRDB;
import org.harctoolbox.harchardware.ir.LircCcfClient;
import org.harctoolbox.harchardware.ir.IrWidget;
import org.harctoolbox.harchardware.ir.IrToy;
import org.harctoolbox.harchardware.ir.IRawIrSender;
import org.harctoolbox.harchardware.ir.ICapture;
import org.harctoolbox.harchardware.ir.GlobalCache;
import org.harctoolbox.harchardware.ir.LircClient;
import org.harctoolbox.harchardware.beacon.AmxBeaconListener;
import org.harctoolbox.harchardware.ir.Arduino;
import org.harctoolbox.harchardware.ir.Transmitter;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import gnu.io.NoSuchPortException;
import gnu.io.PortInUseException;
import gnu.io.UnsupportedCommOperationException;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.harctoolbox.IrpMaster.DecodeIR;
import org.harctoolbox.IrpMaster.IrSignal;
import org.harctoolbox.IrpMaster.IrpMasterException;
import org.harctoolbox.IrpMaster.IrpUtils;
import org.harctoolbox.IrpMaster.ModulatedIrSequence;

/**
 * Gives possibilities to invoke many of the functions from the command line. Demonstrates the interfaces.
 */
public class Main {

    private final static int invalidPort = -1;
    private static IHarcHardware harcHardware = null;

    private Main() {
    }

    private static void printTable(String title, String[] arr, PrintStream str) {
        if (arr != null) {
            str.println(title);
            str.println();
            for (String s : arr) {
                str.println(s);
            }
        }
    }

    private static int noTrue(boolean... bool) {
        int sum = 0;
        for (boolean b : bool)
            if (b)
                sum++;

        return sum;
    }

    private static void usage(int exitcode) {
        StringBuilder str = new StringBuilder();
        argumentParser.usage(str);

        str.append("\n"
                + "parameters: <protocol> <deviceno> [<subdevice_no>] commandno [<toggle>]\n"
                + "   or       <Pronto code>");

        (exitcode == IrpUtils.exitSuccess ? System.out : System.err).println(str);
        doExit(exitcode);
    }

    private static void doExit(int exitcode) {
        System.exit(exitcode);
    }

    private final static class CommandLineArgs {
        private final static int defaultTimeout = 2000;

        @Parameter(names = {"-#", "--count"}, description = "Number of times to send sequence")
        private int count = 1;

        @Parameter(names = {"-a", "--arduino"}, description = "Use Ardiono")
        private boolean arduino = false;

        @Parameter(names = {"-B", "--beacon"}, description = "Run the beacon listener")
        private boolean beacon = false;

        @Parameter(names = {"-C", "--capture"}, description = "Capture \"learned\" signal")
        private boolean capture = false;

        @Parameter(names = {"-c", "--config"}, description = "Path to IrpProtocols.ini")
        private String irprotocolsIniFilename = "config" + File.separator + "IrpProtocols.ini";

        //@Parameter(names = {"-D", "--debug"}, description = "Debug code")
        //private int debug = 0;

        @Parameter(names = {"-d", "--device"}, description = "Device name, e.g. COM7: or /dev/ttyS0")
        private String device = null;

        @Parameter(names = {"-g", "--globalcache"}, description = "Use GlobalCache")
        private boolean globalcache = false;

        @Parameter(names = {"--getversion"}, description = "Call the getVersion() function")
        private boolean getversion = false;

        @Parameter(names = {"--getremotes"}, description = "Call the getRemotes() function")
        private boolean getremotes = false;

        @Parameter(names = {"--getcommands"}, description = "Call the getCommands() function")
        private String getcommands = null;

        @Parameter(names = {"-h", "--help", "-?"}, description = "Display help message")
        private boolean helpRequested = false;

        @Parameter(names = {"-i", "--ip"}, description = "IP address or name")
        private String ip = null;

        @Parameter(names = {"-I", "--irtrans"}, description = "Use IrTrans")
        private boolean irtrans = false;

        @Parameter(names = {"-l", "--lirc"}, description = "Use Lirc Client")
        private boolean lirc = false;

        @Parameter(names = {"--loop"}, description = "Send the irsignal this many times (taking the -# parameter into account)")
        private int loop = 1;

        @Parameter(names = {"-p", "--port"}, description = "Port number")
        private int port = invalidPort;

        @Parameter(names = {"-s", "--sendircommand"}, arity = 2, description = "Send a preprogrammed IR command by name of remote and command")
        private List<String> remotecommand = null;

        @Parameter(names = {"-t", "--transmitter"}, description = "Transmitter, semantic device dependent")
        private String transmitter = null;

        @Parameter(names = {"-T", "--timeout"}, description = "Timeout in milliseconds")
        private int timeout = defaultTimeout;

        @Parameter(names = {"-v", "--version"}, description = "Display version information")
        private boolean versionRequested;

        @Parameter(names = {"-V", "--verbose"}, description = "Execute commands verbosely")
        private boolean verbose;

        @Parameter(names = {"-w", "--irwidget"}, description = "Use IrWidget")
        private boolean irwidget;

        @Parameter(names = {"-y", "--irtoy"}, description = "Use IrToy")
        private boolean irtoy = false;

        @Parameter(description = "[parameters]")
        private ArrayList<String> parameters = new ArrayList<>();
    }

    private static JCommander argumentParser;
    private static CommandLineArgs commandLineArgs = new CommandLineArgs();

    private static Thread closeOnShutdown = new Thread() {
        @Override
        public void run() {
            try {
                //System.err.println("Running shutdown");
                if (harcHardware != null)
                    harcHardware.close();
            } catch (IOException e) {
                System.err.println(e.getMessage());
            }
        }
    };

    public static void main(String[] args) {
        argumentParser = new JCommander(commandLineArgs);
        argumentParser.setProgramName("HarcHardware");

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
            System.out.println("JVM: " + System.getProperty("java.vendor") + " " + System.getProperty("java.version") + " " + System.getProperty("os.name") + "-" + System.getProperty("os.arch"));
            System.out.println();
            System.out.println(Version.licenseString);
            doExit(IrpUtils.exitSuccess);
        }

        Runtime.getRuntime().addShutdownHook(closeOnShutdown);

        int noHardware = noTrue(commandLineArgs.lirc, commandLineArgs.globalcache, commandLineArgs.irtrans, commandLineArgs.irtoy, commandLineArgs.irwidget, commandLineArgs.arduino);

        if (noHardware == 0 && commandLineArgs.beacon) {
            System.err.println("Listening for AMX Beacons for " + commandLineArgs.timeout/1000 + " seconds, be patient.");
            Collection<AmxBeaconListener.Node> nodes = AmxBeaconListener.listen(commandLineArgs.timeout, commandLineArgs.verbose);
            for (AmxBeaconListener.Node node : nodes)
                System.out.println(node);
            doExit(IrpUtils.exitSuccess);
        }

        if (noHardware != 1) {
            System.err.println("Exactly one hardware device must be given.");
            doExit(IrpUtils.exitUsageError);
        }

        boolean didSomethingUseful = false;
        GlobalCache globalCache = null;
        IrTransIRDB irTrans = null;
        IrToy irtoy = null;
        LircCcfClient lircClient = null;
        IrWidget irWidget = null;
        Arduino arduino = null;
        IRemoteCommandIrSender remoteCommandIrSender = null;
        IRawIrSender rawIrSender = null;
        ICapture captureDevice = null;
        Transmitter transmitter = null;

        try {
            if (commandLineArgs.globalcache) {
                if (commandLineArgs.port != invalidPort)
                    System.err.println("Port for GlobalCache not implemented, ignoring.");
                String gcHostname = commandLineArgs.ip != null ? commandLineArgs.ip : GlobalCache.defaultGlobalCacheIP;
                if (commandLineArgs.beacon && commandLineArgs.globalcache) {
                    System.err.print("Invoking beacon listener, taking first response, be patient...");
                    AmxBeaconListener.Node gcNode = GlobalCache.listenBeacon(commandLineArgs.timeout);
                    if (gcNode != null) {
                        gcHostname = gcNode.getInetAddress().getHostName();
                        System.err.println("got " + gcHostname);
                    } else
                        System.err.println("failed.");
                }
                globalCache = new GlobalCache(gcHostname, commandLineArgs.verbose, commandLineArgs.timeout);
                rawIrSender = globalCache;
                harcHardware = globalCache;
                transmitter = globalCache.getTransmitter(commandLineArgs.transmitter);
                captureDevice = globalCache;
            } else if (commandLineArgs.irtoy) {
                if (commandLineArgs.port != invalidPort)
                    System.err.println("Port for IrToy not sensible, ignored.");

                irtoy = new IrToy(commandLineArgs.device != null ? commandLineArgs.device : IrToy.defaultPortName);
                rawIrSender = irtoy;
                harcHardware = irtoy;
                captureDevice = irtoy;
                transmitter = null;
            } else if (commandLineArgs.irtrans) {
                if (commandLineArgs.port != invalidPort)
                    System.err.println("Port for IrTrans not implemented, using standard port.");
                irTrans = new IrTransIRDB(commandLineArgs.ip, commandLineArgs.verbose, commandLineArgs.timeout);
                rawIrSender = irTrans;
                remoteCommandIrSender = irTrans;
                harcHardware = irTrans;
                transmitter = irTrans.getTransmitter(commandLineArgs.transmitter);
            } else if (commandLineArgs.lirc) {
                int port = commandLineArgs.port == invalidPort ? LircClient.lircDefaultPort : commandLineArgs.port;
                lircClient = new LircCcfClient(commandLineArgs.ip, port, commandLineArgs.verbose, commandLineArgs.timeout);
                rawIrSender = lircClient;
                remoteCommandIrSender = lircClient;
                harcHardware = lircClient;
                transmitter = lircClient.getTransmitter(commandLineArgs.transmitter);
            } else if (commandLineArgs.irwidget) {
                String device = commandLineArgs.device == null ? IrWidget.defaultPortName : commandLineArgs.device;
                irWidget = new IrWidget(device, commandLineArgs.timeout, IrWidget.defaultRunTimeout, IrWidget.defaultEndTimeout, false);
                captureDevice = irWidget;
                harcHardware = irWidget;
            } else if (commandLineArgs.arduino) {
                String device = commandLineArgs.device == null ? Arduino.defaultPortName : commandLineArgs.device;
                arduino = new Arduino(device, 9600, commandLineArgs.timeout, 0, 0, commandLineArgs.verbose);
                captureDevice = arduino;
                harcHardware = arduino;
                rawIrSender = arduino;
            }

            if (commandLineArgs.capture) {
                if (captureDevice == null) {
                    System.err.println("Hardware does not support capturing");
                    doExit(IrpUtils.exitUsageError);
                } else {
                    captureDevice.open();
                    captureDevice.setTimeout(commandLineArgs.timeout, commandLineArgs.timeout, 300);
                    ModulatedIrSequence seq = captureDevice.capture();
                    if (seq != null) {
                        System.out.println(seq);
                        System.out.println(DecodeIR.DecodedSignal.toPrintString(DecodeIR.decode(seq)));
                        doExit(IrpUtils.exitSuccess);
                    } else {
                        System.err.println("Nothing received");
                        doExit(IrpUtils.exitFatalProgramFailure);
                    }
                }
            }

            if (commandLineArgs.getversion) {
                if (harcHardware != null) {
                    harcHardware.open();
                    System.out.println(harcHardware.getVersion());
                }
                didSomethingUseful = true;
            }

            if (commandLineArgs.getremotes) {
                if (remoteCommandIrSender == null) {
                    System.err.println("getRemotes not supported by selected hardware");
                } else {
                    printTable("Result of getRemotes:", remoteCommandIrSender.getRemotes(), System.out);
                    didSomethingUseful = true;
                }
            }

            if (commandLineArgs.getcommands != null) {
                if (remoteCommandIrSender == null) {
                    System.err.println("getCommands not supported by selected hardware");
                } else {
                    printTable("Result of getCommands " + commandLineArgs.getcommands + ": ",
                            remoteCommandIrSender.getCommands(commandLineArgs.getcommands), System.out);
                    didSomethingUseful = true;
                }
            }

            if (commandLineArgs.remotecommand != null && !commandLineArgs.remotecommand.isEmpty()) {
                if (remoteCommandIrSender == null) {
                    System.err.println("sendCommand not supported by selected hardware");
                } else {
                    boolean success = remoteCommandIrSender.sendIrCommand(commandLineArgs.remotecommand.get(0),
                            commandLineArgs.remotecommand.get(1),
                            commandLineArgs.count, transmitter);
                    if (success) {
                        if (commandLineArgs.verbose)
                            System.err.println("sendIrCommand succeeded");
                    } else
                        System.err.println("sendIrCommand failed");
                    didSomethingUseful = true;
                }
            }

            if (commandLineArgs.parameters.isEmpty()) {
                if (didSomethingUseful)
                    doExit(IrpUtils.exitSuccess);
                else {
                    System.err.println("Nothing to do.");
                    usage(IrpUtils.exitUsageError);
                }
            }

            if (rawIrSender == null) {
                System.err.println("Hardware does not support raw IR signals");
                doExit(IrpUtils.exitUsageError);
            } else {

                IrSignal irSignal = new IrSignal(commandLineArgs.irprotocolsIniFilename, 0,
                        commandLineArgs.parameters.toArray(new String[commandLineArgs.parameters.size()]));
                for (int i = 0; i < commandLineArgs.loop; i++) {
                    boolean success = rawIrSender.sendIr(irSignal, commandLineArgs.count, transmitter);
                    if (success) {
                        if (commandLineArgs.verbose)
                            System.err.println("sendIr succeeded");
                    } else
                        System.err.println("sendIr failed");
                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException ex) {
                        Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }

            doExit(IrpUtils.exitSuccess);

        } catch (NoSuchPortException ex) {
            System.err.println("RXTX: No such port");
            System.exit(IrpUtils.exitFatalProgramFailure);
        } catch (HarcHardwareException | IrpMasterException | IOException | PortInUseException | UnsupportedCommOperationException ex) {
            System.err.println(ex.getMessage());
            System.exit(IrpUtils.exitFatalProgramFailure);
        }
    }
}
