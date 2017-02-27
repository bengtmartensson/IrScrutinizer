/*
Copyright (C) 2011 Bengt Martensson.

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
package org.harctoolbox.IrpMaster;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;

public class UserComm {
    private static UserComm instance = new UserComm();
    public static void setQuiet(boolean quiet) {
        instance.quiet = quiet;
    }
    public static void warning(String msg) {
        instance.warningMsg(msg);
    }
    public static void error(String msg) {
        instance.errorMsg(msg);
    }
    public static void print(String msg) {
        instance.printMsg(msg);
    }
    public static void exception(Exception ex) {
        instance.exceptionMsg(ex);
    }
    public static void setLogging(PrintStream logfile) {
        instance.setLogfile(logfile);
    }
    /**
     * For testing and debugging only.
     * @param args
     */
    public static void main(String[] args) {
        try {
            System.out.println(instance.getLine("Enter what you desire>"));
        } catch (IOException ex) {
            System.err.println(ex.getMessage());
        }
    }

    private boolean quiet = false;
    private PrintStream logfile = null;

    public void setLogfile(PrintStream printStream) {
        logfile = printStream;
    }

    /** Issue a non-fatal warning.
     * @param msg
     */
    // May be overridden for GUI etc.
    public void warningMsg(String msg) {
        if (!quiet)
            System.err.println("WARNING: " + msg);
        if (logfile != null)
            logfile.println("WARNING: " + msg);
    }

    public void errorMsg(String msg) {
        System.err.println("ERROR: " + msg);
        if (logfile != null)
            logfile.println("ERROR: " + msg);
    }

    public void exceptionMsg(Exception ex) {
        //if (Debug.getInstance().debugOn(Debug.Item.Main))
        //    ex.printStackTrace();

        errorMsg(ex.getMessage());
    }

    public void debugMsg(String type, String msg) {
        System.out.println("Debug[" + type + "]: " + msg);
    }

    public void printMsg(String msg) {
        System.out.println(msg);
    }

    public String getLine(String prompt) throws IOException {
        if (prompt != null && ! prompt.isEmpty())
            System.out.print(prompt);
        BufferedReader isr = new BufferedReader(new InputStreamReader(System.in, IrpUtils.dumbCharset));
        return isr.readLine();
    }

}
