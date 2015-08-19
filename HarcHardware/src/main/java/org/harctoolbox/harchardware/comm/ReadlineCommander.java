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

package org.harctoolbox.harchardware.comm;

import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.gnu.readline.Readline;
import org.gnu.readline.ReadlineLibrary;
import org.harctoolbox.harchardware.FramedDevice;
import org.harctoolbox.harchardware.ICommandLineDevice;

/**
 * Wrapper around <a href="https://github.com/bengtmartensson/java-readline.git">java-readline</a>.
 * It consists of static members only, since that goes for java-readline too.
 */
public class ReadlineCommander {

    private static String historyFile;
    private static String prompt;
    private static boolean initialized = false;

    private ReadlineCommander() {
    }

    /**
     * Version of init with default (dumb) defaults.
     */
    public static void init() {
        init(null, null, ">>> ", "noname");
    }

    /**
     * Initializes the readline.
     *
     * @param confFile File name of the configuration file.
     * @param historyFile_ File name of the history file.
     * @param prompt_ Prompt for Readline to use.
     * @param appName appName for readline to use when interpreting its configuration.
     */
    public static void init(String confFile, String historyFile_, String prompt_, String appName) {
        historyFile = historyFile_;
        prompt = prompt_;

        try {
            Readline.load(ReadlineLibrary.GnuReadline);
            Readline.initReadline(appName);
            if (confFile != null) {
                if (new File(confFile).exists()) {
                    try {
                        Readline.readInitFile(confFile);
                    } catch (IOException ex) {
                        System.err.println(ex.getMessage());
                    }
                } else {
                    System.err.println("Warning: Cannot open readline configuration " + confFile + ", ignoring");
                }
            }

            if (historyFile != null) {
                if (new File(historyFile).exists()) {
                    try {
                        Readline.readHistoryFile(historyFile);
                    } catch (EOFException | UnsupportedEncodingException ex) {
                        System.err.println("This cannot happen.");
                    }
                } else {
                    System.err.println("Cannot open readline history " + historyFile
                            + ", will try to write it anyhow.");
                }
            }
        } catch (UnsatisfiedLinkError ignore_me) {
            System.err.println("Could not load readline lib. Using simple stdin.");
        }
        initialized = true;
    }

    /**
     * Reads a line, using readline editing, and returns it.
     * @return Line the user typed. Is empty ("") if the user entered an empty line, is null if EOF.
     * @throws IOException if Readline threw it, of if called without calling init(...) first.
     */
    // Readline delivers null for empty line, and throws EOFException for EOF.
    // We repacket this here.
    public static String readline() throws IOException {
        if (!initialized)
            throw new IOException("Readline not initialized");
        String line;
        try {
            line = Readline.readline(prompt, false);
            int size = Readline.getHistorySize();
            if ((line != null && !line.isEmpty())
                    && (size == 0 || !line.equals(Readline.getHistoryLine(size - 1))))
                Readline.addToHistory(line);
        } catch (EOFException ex) {
            return null;
        }
        return line == null ? "" : line;
    }

    /**
     * Closes the history file (if used) and cleans up.
     * @throws IOException
     */
    public static void close() throws IOException {
        initialized = false;
        if (historyFile != null)
            Readline.writeHistoryFile(historyFile);
        Readline.cleanup();
    }

    /**
     * Reads a command using readline and sends it to the hardware instance in the first argument.
     * Responses are sent to stdout.
     * This continues until EOF.
     *
     * Catches all "normal" exceptions that appear.
     * Does not detect if the hardware closes the connection :-(
     * @param stringCommander hardware compoent to be controlled.
     * @param waitForAnswer milliseconds to wait for an answer.
     * @param returnlines If >= 0 wait for this many return lines. If &lt; 0,
     * takes as many lines that are available within waitForAnswer milliseconds-
     */
    public static void readEvalPrint(FramedDevice stringCommander, int waitForAnswer, int returnlines) {
        while (true) {

            String line = null;
            try {
                line = readline();
            } catch (IOException ex) {
                System.err.println(ex.getMessage());
            }

            if (line == null) { // EOF
                System.out.println();
                break;
            }
            String[] result = null;
            try {
                result = stringCommander.sendString(line, returnlines <= 0 ? -1 : returnlines, waitForAnswer);
                for (String str : result)
                    if (str != null) {
                        System.out.println(str);
                    }
                if (result != null && result.length > 0 && result[result.length-1].equals("Bye!"))
                    break;
            } catch (IOException ex) {
                Logger.getLogger(ReadlineCommander.class.getName()).log(Level.SEVERE, null, ex); // FIXME
            }
        }
        System.out.println("Readline.readEvalPrint exited"); // ???
    }

    public static void readEvalPrint(ICommandLineDevice hardware, int waitForAnswer, int returnLines) {
        readEvalPrint(new FramedDevice(hardware), waitForAnswer, returnLines);
    }

    /**
     * This one just for testing and as example, nothing of general interest.
     * @param args ignored.
     */

    public static void main(String[] args) {
        int waitForAnswer = 500;
        init("nosuchfile", ".rljunk", "$$$ ", "null");//".rljunk", "$$$ ");
        try (ICommandLineDevice denon = new TcpSocketPort("denon", 23, 2000, true, TcpSocketPort.ConnectionMode.keepAlive)) {
            FramedDevice stringCommander = new FramedDevice(denon, "{0}\r", true);
            readEvalPrint(stringCommander, waitForAnswer, -1);
        } catch (IOException ex) {
            System.err.println(ex.getMessage());
        } finally {
            try {
                close();
            } catch (IOException ex) {
                System.err.println(ex.getMessage());
            }
        }
    }
}

