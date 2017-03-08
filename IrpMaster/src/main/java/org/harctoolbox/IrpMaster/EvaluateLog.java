/*
Copyright (C) 2011, 2012 Bengt Martensson.

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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.LinkedHashMap;

/**
 * This class is a quick and dirty evaluation of the log file.
 *
 * @author Bengt Martensson
 */
public class EvaluateLog {

    private static void usage(int exitcode) {
        System.err.println("Usage:\n\tEvaluateLogs <logfile>");
        IrpUtils.exit(exitcode);
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        if (args.length == 0)
            usage(IrpUtils.exitUsageError);
        EvaluateLog el = new EvaluateLog(args[0]);
        System.out.println(el);
    }

    private LinkedHashMap<String, Integer>map = new LinkedHashMap<>(16);

    public EvaluateLog(String filename) {
        BufferedReader in;
        try {
            in = new BufferedReader(new InputStreamReader(new FileInputStream(filename), IrpUtils.dumbCharset));
        } catch (FileNotFoundException ex) {
            System.err.println(ex.getMessage());
            return;
        }
        String line;
        try {
            for (;;) {
                line = in.readLine();
                if (line == null)
                    break;
                String[] s = line.split(":");
                if (s[s.length - 1].equals(" failed")) {
                    map.put(s[0], map.containsKey(s[0]) ? map.get(s[0]) + 1 : 1);
                }
            }
        } catch (IOException ex) {
            System.err.println(ex.getMessage());
        } finally {
            try {
                in.close();
            } catch (IOException ex) {
                System.err.println(ex);
            }
        }
    }

    @Override
    public String toString() {
        int total = 0;
        StringBuilder result = new StringBuilder(128);
        total = map.keySet().stream().map((protocol) -> {
            result.append(protocol).append(": ").append(map.get(protocol)).append("\n");
            return protocol;
        }).map((protocol) -> map.get(protocol)).reduce(total, Integer::sum);
        return result.toString() + "\nTotal: " + total;
    }

    /**
     * @return the map
     */
    @SuppressWarnings("ReturnOfCollectionOrArrayField")
    public LinkedHashMap<String, Integer> getMap() {
        return map;
    }


}
