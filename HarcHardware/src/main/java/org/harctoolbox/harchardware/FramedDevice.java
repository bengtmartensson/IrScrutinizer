/*
Copyright (C) 2015 Bengt Martensson.

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

import java.io.IOException;
import java.text.FieldPosition;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import org.harctoolbox.IrpMaster.IrpUtils;
import org.harctoolbox.harchardware.comm.TcpSocketPort;

/**
 * This class is basically the encapsulation of a ICommandLineDevice and a Framer (a formatter for the lines).
 */

public class FramedDevice {

    private ICommandLineDevice hardware;
    private IFramer framer;

    public FramedDevice(ICommandLineDevice hardware, IFramer framer) {
        this.hardware = hardware;
        this.framer = framer;
    }

    public FramedDevice(ICommandLineDevice hardware, String format, boolean touppercase) {
        this(hardware, new Framer(format, touppercase));
    }

    public FramedDevice(ICommandLineDevice hardware, String format) {
        this(hardware, new Framer(format, false));
    }

    public FramedDevice(ICommandLineDevice hardware) {
        this(hardware, new Framer());
    }

    public String[] sendString(String[] cmds, int count, int returnLines, int delay, int waitForAnswer) throws IOException {
        if (count < 1)
            throw new IllegalArgumentException("Count = " + count + " < 1; this is meaningless.");

        boolean sentStuff = false;
        try {
            for (String cmd : cmds) {
                if (!cmd.isEmpty()) {
                    sentStuff = true;
                    String command = framer.frame(cmd);
                    for (int c = 0; c < count; c++) {
                        if (delay > 0 && c > 0)
                            Thread.sleep(delay);
                        hardware.sendString(command);
                    }
                }
            }
            if (returnLines == 0)
                return new String[0];
            else if (returnLines > 0) {
                String[] result = new String[returnLines];
                for (int i = 0; i < returnLines; i++)
                    result[i] = hardware.readString(sentStuff); // wait only if we have sent something
                return result;
            } else {
                if (!hardware.ready() && waitForAnswer > 0)
                    Thread.sleep(waitForAnswer);
                List<String> answer = new ArrayList<>();
                while (hardware.ready()) {
                    String ans = hardware.readString(false);
                    answer.add(ans);
                }
                return answer.toArray(new String[answer.size()]);
            }
        } catch (InterruptedException ex) {
        }
        return null;
    }

    public String[] sendString(String cmd, int returnLines, int waitForAnswer) throws IOException {
        return sendString(new String[]{ cmd }, 1, returnLines, 0, waitForAnswer);
    }

    public void sendString(String cmd) throws IOException {
        sendString(cmd, 0, 0);
    }

    public boolean ready() throws IOException {
        return hardware.ready();
    }

    public String readString() throws IOException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public String readString(boolean wait) throws IOException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public String getVersion() throws IOException {
        return hardware.getVersion();
    }

    public void setVerbosity(boolean verbosity) {
        hardware.setVerbosity(verbosity);
    }

    public void setDebug(int debug) {
        hardware.setDebug(debug);
    }

    public void setTimeout(int timeout) throws IOException {
        hardware.setTimeout(timeout);
    }

    public boolean isValid() {
        return hardware.isValid();
    }

    public void open() throws HarcHardwareException, IOException {
        hardware.open();
    }

    public void close() throws IOException {
        hardware.close();
    }

    // Note: does not return, but loops forever!
    /*private void listenForever(PrintStream printStream) throws IOException {
        while (true) {
            String result = hardware.readString();
            // Ignore whitspace lines
            if (result.trim().length() > 0) {
                printStream.println(result);
            }
        }
    }*/

    /*public static class StringReaderThread extends Thread {

        //private ICommandLineDevice hardware;
        private final PrintStream printStream;
        private final StringCommander stringCommander;

        public StringReaderThread(ICommandLineDevice hardware, PrintStream printStream) {
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

    public static class StringWriterThread extends Thread {
        private final StringCommander stringCommander;
        private final BufferedReader inStream;
        private final Framer commandFramer;

        public StringWriterThread(ICommandLineDevice hardware, BufferedReader inStream, Framer commandFramer) {
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
                        //doExit(IrpUtils.exitSuccess);
                        System.exit(IrpUtils.exitSuccess);
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
    }*/

    //private static void doExit(int exitcode) {
    //    System.exit(exitcode);
    //}

    public static interface IFramer {

        public String frame(String arg);

        public String frame(Object[] args);
    }

    public static class Framer implements IFramer {
        MessageFormat format;
        private final boolean toUpper;

        public Framer(String format, boolean toUpper) {
            this.format = new MessageFormat(format, IrpUtils.dumbLocale);
            this.toUpper = toUpper;
        }

        public Framer() {
            this("{0}", false);
        }

        @Override
        public String frame(String arg) {
            //return format.format(toUpper ? arg.toUpperCase(IrpUtils.dumbLocale) : arg);
            return frame(new Object[]{ toUpper ? arg.toUpperCase(IrpUtils.dumbLocale) : arg });
        }

        @Override
        public String frame(Object[] args) {
            return format.format(args, new StringBuffer(), new FieldPosition(0)).toString();
        }
    }

/*
    public static void telnet(ICommandLineDevice hardware, Framer commandFramer) {
        StringReaderThread readerThread = new StringReaderThread(hardware, System.out);
        readerThread.start();
        StringWriterThread writerThread = new StringWriterThread(hardware, new BufferedReader(new InputStreamReader(System.in, IrpUtils.dumbCharset)), commandFramer);
        writerThread.start();
    }

    public static void telnet(ICommandLineDevice hardware) {
        telnet(hardware, new Framer());
    }*/

    public static void main(String[] args) {
        try (ICommandLineDevice denon = new TcpSocketPort("denon", 23, 2000, true, TcpSocketPort.ConnectionMode.keepAlive)) {
            FramedDevice commandLineDevice = new FramedDevice(denon, "{0}\r", true);
            String[] result = commandLineDevice.sendString("mvdown", 1, 0);
            System.out.println(result[0]);
        } catch (IOException ex) {
            System.err.println(ex.getMessage());
        }
    }
}

