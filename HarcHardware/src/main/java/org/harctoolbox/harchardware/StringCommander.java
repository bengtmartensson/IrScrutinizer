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

package org.harctoolbox.harchardware;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.text.MessageFormat;
import org.harctoolbox.IrpMaster.IrpUtils;
import org.harctoolbox.harchardware.comm.TcpSocketPort;
import org.harctoolbox.harchardware.ir.GlobalCache;
import org.harctoolbox.harchardware.ir.NoSuchTransmitterException;

public class StringCommander {

    IStringCommand hardware;

    public StringCommander(IStringCommand hardware) {
        this.hardware = hardware;
    }

    public String[] sendStringCommand(String[] cmds, int count, int returnLines, int delay) throws IOException {
        if (count < 1)
            throw new IllegalArgumentException("Count = " + count + " < 1; this is meaningless.");

        String[] result = new String[returnLines];

        try {
            for (String cmd : cmds) {
                String command = cmd;//Utils.evaluateEscapes(cmd);

                if (command != null)
                    for (int c = 0; c < count; c++) {
                        if (delay > 0 && c > 0)
                            Thread.sleep(delay);

                        hardware.sendString(command);
                        //Thread.sleep(10);
                    }
            }
            for (int i = 0; i < returnLines; i++) {
                result[i] = hardware.readString();
            }
        } catch (InterruptedException ex) {
        //} catch (IOException ex) {
        }

        return result;
    }

    public String sendStringCommand(String cmd, int returnLines) throws IOException {
        return sendStringCommand(new String[]{ cmd }, 1, returnLines, 0)[0];
    }

    public String sendStringCommand(String cmd) throws IOException {
        return sendStringCommand(cmd, 0);
    }

    // Note: does not return, but loops forever!
    private void listenForever(PrintStream printStream) throws IOException {
        while (true) {
            String result = hardware.readString();
            // Ignore whitspace lines
            if (result.trim().length() > 0) {
                printStream.println(result);
            }
        }
    }

    public static class StringReaderThread extends Thread {

        //private IStringCommand hardware;
        private final PrintStream printStream;
        private final StringCommander stringCommander;

        public StringReaderThread(IStringCommand hardware, PrintStream printStream) {
            //this.hardware = hardware;
            this.printStream = printStream;
            stringCommander = new StringCommander(hardware);
        }

        @Override
        public void run() {

            try {
                stringCommander.listenForever(printStream);
            } catch (IOException ex) {
                System.err.println(ex.getMessage());
            }
        }
    }

    /** Basically for testing */
    public static class StringWriterThread extends Thread {
        private final StringCommander stringCommander;
        private final BufferedReader inStream;
        private final Framer commandFramer;

        public StringWriterThread(IStringCommand hardware, BufferedReader inStream, Framer commandFramer) {
            //this.hardware = hardware;
            this.inStream = inStream;
            stringCommander = new StringCommander(hardware);
            this.commandFramer = commandFramer;
        }

        @Override
        public void run() {
            String line = null;
            boolean done = false;
            while (! done) {
                try {
                    line = inStream.readLine();
                    if (line == null || line.startsWith("quit")) {
                        System.err.println("Goodbye");
                        doExit(IrpUtils.exitSuccess);
                        break;
                    }
                } catch (IOException ex) {
                    System.err.println(ex.getMessage());
                }
                if (line != null && line.equals("quit"))
                    done = true;
                else
                    try {
                    stringCommander.hardware.sendString(commandFramer.frame(line));
                } catch (IOException ex) {
                    System.err.println(ex.getMessage());
                }
            }
            //System.err.println("Goodbye!");

        }
    }

    private static void doExit(int exitcode) {
        System.exit(exitcode);
    }

    /*
    public interface ICommandFramer {
        String frame(String cmd);
    }

    public static class OppoFramer implements ICommandFramer {
        @Override
        public String frame(String payload) {
            return "#" + payload.toUpperCase(IrpUtils.dumbLocale) + "\r";
        }
    }

    public static class SanyoFramer implements ICommandFramer {
        @Override
        public String frame(String payload) {
            return "C" + payload.toUpperCase(IrpUtils.dumbLocale) + "\r";
        }
    }

    public static class UppercaseCRFramer implements ICommandFramer {
        @Override
        public String frame(String payload) {
            return payload.toUpperCase(IrpUtils.dumbLocale) + "\r";
        }
    }

    public static class CRFramer implements ICommandFramer {
        @Override
        public String frame(String payload) {
            return payload + "\r";
        }
    }

    public static class IdFramer implements ICommandFramer {
        @Override
        public String frame(String payload) {
            return payload;
        }
    }

    public static class UniversalFramer implements ICommandFramer {
        private final String prefix;
        private final String suffix;
        private final boolean toUpper;

        public UniversalFramer(String prefix, String suffix, boolean toUpper) {
            this.prefix = prefix != null ? prefix : "";
            this.suffix = suffix != null ? suffix : "";
            this.toUpper = toUpper;
        }

        @Override
        public String frame(String payload) {
            //return Utils.evaluateEscapes(prefix + (toUpper ? payload.toUpperCase(IrpUtils.dumbLocale) : payload) + suffix);
            return prefix + (toUpper ? payload.toUpperCase(IrpUtils.dumbLocale) : payload) + suffix;
        }
    }*/

    public static class Framer {
        MessageFormat format;
        private final boolean toUpper;

        public Framer(String format, boolean toUpper) {
            this.format = new MessageFormat(format);
            this.toUpper = toUpper;
        }

        public Framer() {
            this("{0}", false);
        }

        public String frame(String arg) {
            return format.format(toUpper ? arg.toUpperCase(IrpUtils.dumbLocale) : arg);
        }

        public String frame(Object[] args) {
            // TODO: act on toUpper
            return format.format(args, new StringBuffer(), null).toString();
        }
    }

    public static void telnet(IStringCommand hardware, Framer commandFramer) {
        StringReaderThread readerThread = new StringReaderThread(hardware, System.out);
        readerThread.start();
        StringWriterThread writerThread = new StringWriterThread(hardware, new BufferedReader(new InputStreamReader(System.in, IrpUtils.dumbCharset)), commandFramer);
        writerThread.start();
    }

    public static void telnet(IStringCommand hardware) {
        telnet(hardware, new Framer());
    }

    public static void main(String[] args) {
        try {
            TcpSocketPort denon = new TcpSocketPort("denon", 23, 2000, true, TcpSocketPort.ConnectionMode.keepAlive);
            StringCommander stringCommander = new StringCommander(denon);
            String result = stringCommander.sendStringCommand("MVDOWN\r", 1);
            System.out.println(result);
            denon.close();
            System.exit(0);

            GlobalCache gc = new GlobalCache("gc", true);
            GlobalCache.SerialPort port = gc.getSerialPort(1);
            port.sendString("CR0\r");
            System.out.println(port.readString());
            port.close();
        } catch (IOException ex) {
            System.err.println(ex.getMessage());
        } catch (NoSuchTransmitterException ex) {
            System.err.println(ex.getMessage());
        }
    }
}

