/*
Copyright (C) 2013 Bengt Martensson.

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

package org.harctoolbox.irscrutinizer.importer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Serializable;
import java.text.ParseException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import org.harctoolbox.IrpMaster.IrpUtils;

/**
 * This class contains a simple importer for the RemoteMaster's protocols.ini.
 */
public class ProtocolsIni implements Serializable {
    private static final long serialVersionUID = 1L;

    private HashMap<Integer, HashMap<String,String>> pidMap;
    private HashMap<String, HashMap<String,String>> nameMap;

    public ProtocolsIni(File file) throws IOException, ParseException {
        this(new InputStreamReader(new FileInputStream(file), IrpUtils.dumbCharset));
    }

    public ProtocolsIni(Reader reader) throws IOException, ParseException {
        pidMap = new LinkedHashMap<>();
        nameMap = new LinkedHashMap<>();

        BufferedReader in = new BufferedReader(reader);
        HashMap<String, String> currentProtocol = null;//new HashMap<String, String>();
        int pid = -1;
        int lineNo = 0;
        for (String lineRead = in.readLine(); lineRead != null; lineRead = in.readLine()) {
            lineNo++;
            String line = lineRead.trim();
            String[] kw = line.split("=", 2);
            String keyword = kw[0];
            String payload = kw.length > 1 ? kw[1].trim() : null;
            while (payload != null && payload.endsWith("\\")) {
                payload = payload.substring(0, payload.length() - 1)/* + "\n"*/;
                payload += in.readLine();
            }
            if (line.startsWith("#") || line.isEmpty()) {
                // comment, ignore
            } else if (line.startsWith("["))  {
                // new protocol
                String name = line.substring(1, line.length()-1);
                currentProtocol = new LinkedHashMap<>();
                currentProtocol.put("name", name);
                nameMap.put(name, currentProtocol);
                pid = -1;
            } else {
                // keyword=value
                if (currentProtocol == null)
                    throw new ParseException("currentProtocol not defined", lineNo);
                currentProtocol.put(keyword, payload);
                if (keyword.equals("PID")) {
                    if (payload == null) {
                        throw new ParseException("payload not defined", lineNo);
                    }
                    pid = Integer.parseInt(payload.replaceAll(" ", ""), 16);
                    // There are a number of multiply defined PIDs.
                    // Leave out a check for simplicity, and just overwrite.
                    pidMap.put(pid, currentProtocol);
                }
            }
        }
    }

    public String getProperty(int pid, String property) {
        return pidMap.get(pid).get(property);
    }

    public String getProperty(String protocol, String property) {
        return nameMap.get(protocol).get(property);
    }

    public String[] getDeviceParameters(String protocol) {
        return getDeviceParameters(nameMap.get(protocol));
    }

    public String[] getDeviceParameters(int pid) {
        return getDeviceParameters(pidMap.get(pid));
    }

    private String[] getDeviceParameters(HashMap<String, String> map) {
        if (map == null)
            return new String[0];
        String str = map.get("DevParms");
        if (str == null)
            return new String[0];

        String[] params = str.split(",");
        for (int i = 0; i < params.length; i++) {
            String in = params[i].split("=")[0];
            params[i] = translate(in);
        }
        return params;
    }

    private String translate(String in) {
        return
                in.equals("Device Number") ? "D"
                : in.equals("Device Code") ? "D"
                : in.equals("Device") ? "D"
                : in.equals("Sub Device") ? "S"
                : null;
    }

    public ICmdTranslator[] getCmdTranslators(String protocol) {
        return getCmdTranslators(nameMap.get(protocol));
    }

    public ICmdTranslator[] getCmdTranslators(int pid) {
        return getCmdTranslators(pidMap.get(pid));
    }

    private ICmdTranslator[] getCmdTranslators(HashMap<String, String> map) {
        if (map == null)
            return new ICmdTranslator[0];
        String str = map.get("CmdTranslator");
        if (str == null)
            return new ICmdTranslator[0];

        String[] chunks = str.split(" ");
        ICmdTranslator[] translators = new ICmdTranslator[chunks.length];
        for (int i = 0; i < chunks.length; i++) {
            translators[i] = translator(chunks[i]);
        }
        return translators;
    }

    private ICmdTranslator translator(String name) {
        return
                name.equals("Translator(lsb,comp)") ? new LsbComp()
                : name.equals("Translator(lsb,comp,3)") ? new LsbComp3()
                : null;
    }

    public interface ICmdTranslator {
        public long translate(long x);
    }

    public static class LsbComp implements ICmdTranslator {

        @Override
        public long translate(long x) {
            return reverse(complement(x, 8), 8);
        }

    }

    // FIXME: this is an ad-hoc solution
    public static class LsbComp3 implements ICmdTranslator {

        @Override
        public long translate(long x) {
            long y = x >> 8;
            return reverse(complement(y, 8), 8);
        }

    }

    private static long reverse(long x, int width) {
        long y = Long.reverse(x);
        if (width > 0)
            y >>>= Long.SIZE - width;
        return y;
    }

    private static long complement(long x, int width) {
        long y = -1L ^ x;
        return ((1 << width) - 1) & y;
    }

    public static void main(String[] args) {
        try {
            ProtocolsIni protocolsIni = new ProtocolsIni(new File(args[0]));
            System.out.println(protocolsIni);
        } catch (IOException | ParseException ex) {
            System.err.println(ex.getMessage());
        }
    }

}
