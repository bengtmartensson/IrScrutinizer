/*
Copyright (C) 2013, 2014 Bengt Martensson.

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

package org.harctoolbox.irscrutinizer.exporter;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import org.harctoolbox.IrpMaster.IrpMasterException;
import org.harctoolbox.IrpMaster.IrpUtils;
import org.harctoolbox.girr.Command;
import org.harctoolbox.girr.Remote;
import org.harctoolbox.girr.RemoteSet;
import org.harctoolbox.irscrutinizer.Version;

/**
 * This class exports a RemoteSet to a LIRC configuration file.
 */
public class LircExporter extends RemoteSetExporter implements IRemoteSetExporter {
    private final static boolean forceRaw = false; // FIXME

    public LircExporter() {
        super();
    }

    public LircExporter(String creatingUser) {
        super(creatingUser);
    }

    @Override
    public String[][] getFileExtensions() {
        return new String[][] { new String[]{ "LIRC files", "lirc" }, new String[]{ "Text files", "txt" }};
    }

    // LIRC in general does not add extensions to its files, therefore we should not do it either.
    @Override
    public String getPreferredFileExtension() {
        return "lircd.conf";
    }

    @Override
    public void export(RemoteSet remoteSet, String title, int count, File saveFile) throws FileNotFoundException, IrpMasterException {
        // LIRC often does not add extensions to its files, therefore we should not do it either.
        if (remoteSet.getAllCommands().isEmpty())
            throw new IllegalArgumentException("No commands in the remotes.");

        PrintStream out = null;
        try {
            out = new PrintStream(saveFile, IrpUtils.dumbCharsetName);
        } catch (UnsupportedEncodingException ex) {
            // cannot happen
            return;
        }

        exportPreamble(out, title);

        for (Remote remote : remoteSet.getRemotes()) {
            if (!forceRaw && remote.hasThisProtocol("nec1"))
                exportRemoteNec1(out, remote);
            else if (!forceRaw && remote.hasThisProtocol("rc5"))
                exportRemoteRc5(out, remote);
            else
                exportRemoteRaw(out, remote, count);
        }
        out.close();
    }

    @Override
    public String getFormatName() {
        return "LIRC";
    }

    /**
     * Write the instance to a the PrintStream in the argument. It is not closed.
     * @param stream PrintStream, should be ready for writing.
     */
    private void exportPreamble(PrintStream stream, String title) {
        if (title != null && !title.isEmpty())
            stream.println("# " + title);
        else
            stream.println("# Automatically generated LIRC file");
        stream.println("#");
        stream.println("# Creating tool: " + Version.versionString);
        stream.println("# Creating user: " + creatingUser);
        stream.println("# CreateDate: " + (new Date()).toString());
        stream.println("#");
    }

    private void exportRemoteHead(PrintStream stream, Remote remote) {
        stream.println("#");
        if (remote.getManufacturer() != null)
            stream.println("# Manufacturer: " + remote.getManufacturer());
        if (remote.getModel() != null)
            stream.println("# Model: " + remote.getModel());
        if (remote.getComment() != null) {
            stream.println("# " + remote.getComment());
        }
        stream.println("#");
    }

    private void exportRemoteRaw(PrintStream stream, Remote remote, int count) throws IrpMasterException {
        exportRemoteHead(stream, remote);
        Command randomCommand = remote.getCommands().values().iterator().next();
        randomCommand.checkForRaw();
        stream.println("begin remote");
        stream.println("\tname\t" + remote.getName());
        stream.println("\tflags\tRAW_CODES");
        stream.println("\teps\t30");
        stream.println("\taeps\t100");
        stream.println("\tfrequency\t" + randomCommand.getFrequency());
        stream.println("\tgap\t" + Math.round(randomCommand.toIrSignal().getGap()));
        stream.println("\t\tbegin raw_codes");
        for (Map.Entry<String, Command> kvp : remote.getCommands().entrySet()) {
            stream.println();
            stream.print("\t\t\tname " + kvp.getKey());
            int[] signal = kvp.getValue().toIrSignal().toIntArrayCount(count);
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

    private void exportRemoteNec1(PrintStream stream, Remote remote) {
        stream.println("begin remote");
        stream.println("\tname\t" + remote.getName());
        stream.println("\tbits\t32");
        stream.println("\tflags\tSPACE_ENC");
        stream.println("\teps\t30");
        stream.println("\taeps\t100");
        stream.println("\tzero\t573\t573");
        stream.println("\tone\t573\t1694");
        stream.println("\theader\t9041\t4507");
        stream.println("\tptrail\t573");
        stream.println("\trepeat\t9041\t2267");
        stream.println("\tgap\t36000");
        stream.println("\trepeat_bit\t0");
        stream.println("\t\tbegin codes");
        for (Map.Entry<String, Command> kvp : remote.getCommands().entrySet())
            stream.println(String.format("\t\t\t%1$s\t%2$#016x", kvp.getKey(), formatNec1(kvp.getValue())));

        stream.println("\t\tend codes");
        stream.println("end remote");
    }

    private long formatNec1(Command command) {
        HashMap<String, Long> parameters = command.getParameters();
        long D = IrpUtils.reverse(parameters.get("D"), 8);
        long F = IrpUtils.reverse(parameters.get("F"), 8);
        long S = IrpUtils.reverse(parameters.containsKey("S") ? parameters.get("S") : 255L - parameters.get("D"), 8);
        return (D << 24L) | (S << 16L) | (F << 8L) | ((~F) & 0xFFL) ;
    }

    private void exportRemoteRc5(PrintStream stream, Remote remote) throws IrpMasterException {
        stream.println("begin remote");
        stream.println("\tname\t" + remote.getName());
        stream.println("\tbits\t13");
        stream.println("\tflags\tRC5|CONST_LENGTH");
        stream.println("\teps\t30");
        stream.println("\taeps\t100");
        stream.println("\tzero\t889\t889");
        stream.println("\tone\t889\t889");
        stream.println("\tplead\t889");
        stream.println("\tgap\t90886");
        stream.println("\ttoggle_bit\t2");
        stream.println("\t\tbegin codes");
        for (Map.Entry<String, Command> kvp : remote.getCommands().entrySet())
            stream.println(String.format("\t\t\t%1$s\t%2$#016x", kvp.getKey(), formatRc5(kvp.getValue())));
        stream.println("\t\tend codes");
        stream.println("end remote");
    }

    private long formatRc5(Command command) {
        HashMap<String, Long> parameters = command.getParameters();
        long D = parameters.get("D");
        long F = parameters.get("F");
        long T = parameters.containsKey("T") ? parameters.get("T") : 0;
        return (((~F) & 64L) << 6L)| (T << 11L) | (D << 6L) | F;
    }
}
