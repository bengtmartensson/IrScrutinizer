/*
Copyright (C) 2011, 2013 Bengt Martensson.

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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;

/**
 * This class creates configuration files for LIRC.
 *
 */
public class LircExport {

    private String name;
    private String comment;
    private String user;
    private String manufacturer;
    private String model;
    private String tool;
    private String toolVersion;
    private int frequency = (int) IrpUtils.invalid;
    private int gap;
    private LinkedHashMap<String, int[]> signals;

    /**
     * Constructor for the class.
     *
     * @param name Name of the "remote", as in the name file in the LIRC remote
     * @param comment String Comment to go into the generated file.
     * @param user
     * @param manufacturer
     * @param model
     * @param tool
     * @param toolVersion
     * @param frequency Modulation frequency in Hz.
     */
    public LircExport(String name,
            String comment,
            String user,
            String manufacturer,
            String model,
            String tool,
            String toolVersion,
            double frequency) {
        this.name = name;
        this.comment = comment;
        this.user = user;
        this.manufacturer = manufacturer;
        this.model = model;
        this.tool = tool;
        this.toolVersion = toolVersion;
        this.frequency = (int) frequency;
        gap = 0;
        signals = new LinkedHashMap<String, int[]>();
    }

    public LircExport(String name, String comment, double frequency) {
        this(name,
                comment,
                System.getProperty("user.name", "unknown"),
                null,
                null,
                Version.appName,
                Version.version,
                frequency);
    }

    /**
     * Adds a signal to the export, to be a command in the remote.
     *
     * @param name Name of the command
     * @param data
     * @param frequency
     */
    public void addSignal(String name, int[] data, int frequency) {
        signals.put(name, data);
        gap = Math.max(gap, data[data.length-1]);
        if (frequency != (int) IrpUtils.invalid)
            this.frequency = frequency;
    }

    /**
     * Adds a signal to the export, to be a command in the remote.
     *
     * @param parameters Parameters, to form a name of the LIRC command
     * @param irSignal IrSignal to be the data of the command.
     * @param reps Number of repetitions
     */
    public void addSignal(HashMap<String, Long> parameters, IrSignal irSignal, int reps) {
        String[] keys = parameters.keySet().toArray(new String[parameters.keySet().size()]);
        java.util.Arrays.sort(keys, String.CASE_INSENSITIVE_ORDER);
        StringBuilder sigName = new StringBuilder();
        for (String key : keys)
            sigName.append(key).append(String.format("%03d", parameters.get(key)));

        addSignal(sigName.toString().toLowerCase(IrpUtils.dumbLocale), irSignal.toIntArray(reps), (int) irSignal.frequency);
    }

    /**
     * Adds a signal to the export, to be a command in the remote.
     *
     * @param parameters Parameters, to form a name of the LIRC command
     * @param irSignal IrSignal to be the data of the command.
     */
    public void addSignal(HashMap<String, Long> parameters, IrSignal irSignal) {
        addSignal(parameters, irSignal, 1);
    }

    /**
     * Adds a signal to the export, to be a command in the remote.
     *
     * @param name
     * @param irSignal
     */
    public void addSignal(String name, IrSignal irSignal) {
        addSignal(name, irSignal.toIntArray(1), (int) irSignal.frequency);
    }

    /**
     * Write the instance to a the file in the argument. It is closed.
     * @param file File to be written.
     * @throws FileNotFoundException
     */
    public void write(File file) throws FileNotFoundException {
        PrintStream stream = null;
        try {
            stream = new PrintStream(file, IrpUtils.dumbCharsetName);
            write(stream);
            stream.close();
        } catch (UnsupportedEncodingException ex) {
            assert false;
        }
    }

    /**
     * Write the instance to a the PrintStream in the argument. It is not closed.
     *
     * @param stream PrintStream, should be ready for writing.
     */
    public void write(PrintStream stream) {
        stream.println("# Automatically generated LIRC file");
        stream.println("#");
        stream.println("# Creating tool: " + tool + " version " + toolVersion);
        stream.println("# Creating user: " + user);
        stream.println("# CreateDate: " + (new Date()).toString());
        stream.println("#");
        if (manufacturer != null)
            stream.println("# Manufacturer: " + manufacturer);
        if (model != null)
            stream.println("# Model: " + model);
        stream.println("#");
        if (comment != null) {
            stream.println("# " + comment);
            stream.println("#");
        }
        stream.println("begin remote");
        stream.println("\tname\t" + name);
        stream.println("\tflags\tRAW_CODES");
        stream.println("\teps\t30");
        stream.println("\taeps\t100");
        stream.println("\tfrequency\t" + frequency);
        stream.println("\tgap\t" + gap);
        stream.println("\t\tbegin raw_codes");
        for (String sigName : signals.keySet()) {
            stream.println();
            stream.print("\t\t\tname " + sigName);
            int[] signal = signals.get(sigName);
            for (int i = 0; i < signal.length - 1; i++) {
                if (i % 8 == 0) {
                    stream.println();
                    stream.print("\t\t\t\t");
                }
                stream.print(signal[i] + " ");
            }
        }
        stream.println();
        stream.println("\t\tend raw_codes");
        stream.println("end remote");
    }
}
