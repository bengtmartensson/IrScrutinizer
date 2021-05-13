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

package org.harctoolbox.irscrutinizer.importer;

import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Proxy;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.harctoolbox.girr.Command;
import org.harctoolbox.girr.GirrException;
import org.harctoolbox.girr.Remote;
import org.harctoolbox.girr.RemoteSet;
import org.harctoolbox.ircore.IrCoreUtils;

public class IrdbImporter extends DatabaseImporter implements IRemoteSetImporter {

    private final static String irdbOriginName = "IRDB";

    private static ArrayList<String> dbIndex;
    private static ArrayList<String> manufacturers;

    private final static String irdbCdnProto = "https";
    private final static String irdbCdnHost = "cdn.jsdelivr.net";
    private final static String irdbCdnFormat = "/gh/probonopd/irdb@master/codes/%s";

    private static Proxy proxy = Proxy.NO_PROXY;

    public static URI getHomeUri() {
        try {
            return new URI("https://github.com/probonopd/irdb");
        } catch (URISyntaxException ex) {
            return null;
        }
    }
    private static long parseLong(String str) {
        try {
            return ((str == null) || str.isEmpty()) ? IrCoreUtils.INVALID : Long.parseLong(str);
        } catch (NumberFormatException e) { // Do not treat "None" explicitly
            return IrCoreUtils.INVALID;
        }
    }
    public static String[] getManufacturers(boolean verbose) throws IOException {
        if (manufacturers == null)
            setupManufacturers(verbose);
        return manufacturers.toArray(new String[manufacturers.size()]);
    }

    private static ArrayList<String> getLines(String urlString, boolean verbose) throws IOException {
        ArrayList<String> lines = new ArrayList<>();
        URL index = urlString.contains("//") ? new URL(urlString) : new URL(irdbCdnProto, irdbCdnHost, urlString);
        URLConnection urlConnection = index.openConnection(proxy);
        InputStream stream = urlConnection.getInputStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream, Charset.forName("US-ASCII")));

        String line = reader.readLine();
        while (line != null) {
            lines.add(line);
            line = reader.readLine();
        }
        return lines;
    }

    private static void setupDbIndex(boolean verbose) throws IOException {
        dbIndex = getLines(String.format(irdbCdnFormat, "index"), verbose);
    }

    private static void setupManufacturers(boolean verbose) throws IOException {
        if (dbIndex == null)
            setupDbIndex(verbose);
        ArrayList<String> newManufacturers = new ArrayList<>(1024);

        for (String line: dbIndex) {
            String brand = line.substring(0, line.indexOf('/'));
            if (!newManufacturers.contains(brand))
                newManufacturers.add(brand);
        }
        manufacturers = newManufacturers;
    }

    public static Map<String,Long> mkParameters(long D, long S, long F) {
        Map<String, Long> result = new HashMap<>(3);
        if (D != IrCoreUtils.INVALID)
            result.put("D", D);
        if (S != IrCoreUtils.INVALID)
            result.put("S", S);
        if (F != IrCoreUtils.INVALID)
            result.put("F", F);

        return result;
    }

    public static void main(String[] args) {
        try {
            String[] manufacturers = getManufacturers(true);
            IrdbImporter irdb = new IrdbImporter(manufacturers[11], true);
            System.out.println(irdb.deviceTypes);
            System.out.println(irdb.getDeviceTypes());
            for (String type : irdb.getDeviceTypes()) {
                System.out.println(irdb.getProtocolDeviceSubdevice(type));
                for (ProtocolDeviceSubdevice pds : irdb.getProtocolDeviceSubdevice(type))
                    System.out.println(irdb.getCommands(type, pds));
            }
        } catch (IOException ex) {
            System.err.println(ex.getMessage());
        }
    }

    public static void setProxy(Proxy newProxy) {
        proxy = newProxy;
    }

    //private boolean verbose = false;
    private String manufacturer;
    private RemoteSet remoteSet;
    private LinkedHashMap<String, Map<ProtocolDeviceSubdevice, Map<String, Long>>> deviceTypes;

    public IrdbImporter(String manufacturer, boolean verbose) throws IOException {
        super(irdbOriginName);
        this.manufacturer = manufacturer;
        deviceTypes = new LinkedHashMap<>(16);
        for (String path : dbIndex) {
            if (!path.startsWith(manufacturer))
                continue;
            String deviceType = path.substring(1 + path.indexOf("/"), path.lastIndexOf("/"));
            if (!deviceTypes.containsKey(deviceType))
                deviceTypes.put(deviceType, new LinkedHashMap<>(8));
            Map<ProtocolDeviceSubdevice, Map<String, Long>> devCollection = deviceTypes.get(deviceType);
            ArrayList<String> deviceData = getLines(String.format(irdbCdnFormat, path), verbose);
            // deviceData is csv, header: functionname,protocol,device,subdevice,function
            // In a given file, the protocol, device, and subdevice appear to be constant.

            ProtocolDeviceSubdevice pds = null;
            Map<String, Long> cmnds = null;
            for (String entry : deviceData) {
                if (entry.equals("functionname,protocol,device,subdevice,function"))
                    continue;   // header row
                String[] elts = entry.split(",");
                if (elts.length != 5) {
                    if (entry.startsWith("\"")) {
                        // Special parsing in case the function name is quoted because it contains a comma.
                        // Protocol is unlikely to contain a comma, and the rest are numbers,
                        // so we'll just handle the function name here.
                        int endOfFunctionName = entry.lastIndexOf("\",");
                        String[] new_rest = entry.substring(endOfFunctionName + 2).split(",");
                        if (new_rest.length == 4) {
                            elts = new String[5];
                            elts[0] = entry.substring(1, endOfFunctionName);
                            int i = 1;
                            for (String elt : new_rest) {
                                elts[i++] = elt;
                            }
                        }
                    }
                }
                if (elts.length != 5) {
                    System.err.println("Could not parse line \"" + entry + "\" from file \"" + path + "\".");
                    continue;
                }
                if (pds == null) {
                    String protocol = elts[1];
                    long device = Long.parseLong(elts[2]);
                    long subdevice = Long.parseLong(elts[3]);
                    pds = new ProtocolDeviceSubdevice(protocol, device, subdevice);
                    if (!devCollection.containsKey(pds))
                        devCollection.put(pds, new LinkedHashMap<>(8));
                }
                if (cmnds == null)
                    cmnds = devCollection.get(pds);
                long function = Long.parseLong(elts[4]);
                String functionName = elts[0];
                if (cmnds.containsKey(functionName))
                    System.err.println("functionname " + functionName + " more than once present, overwriting");
                cmnds.put(functionName, function);
            }
        }
    }

    @Override
    public RemoteSet getRemoteSet() {
        return remoteSet;
    }

    @Override
    public String getFormatName() {
        return "IRDB";
    }

    public Set<String> getDeviceTypes() {
        return deviceTypes.keySet();
    }

    public Set<ProtocolDeviceSubdevice> getProtocolDeviceSubdevice(String deviceType) {
        Map<ProtocolDeviceSubdevice, Map<String, Long>> map = deviceTypes.get(deviceType);
        return map == null ? null : map.keySet();
    }

    public void load(String deviceType, ProtocolDeviceSubdevice pds) {
        clearCommands();
        Map<ProtocolDeviceSubdevice, Map<String, Long>> map = deviceTypes.get(deviceType);
        if (map == null)
            return;

        Map<String, Long> commandMap = map.get(pds);
        load(commandMap, pds, deviceType);
        Remote.MetaData metaData = new Remote.MetaData(manufacturer + "_" + deviceType + "_" + pds.toString(), //java.lang.String name,
                null, // displayName
                manufacturer, //java.lang.String manufacturer,
                null, //java.lang.String model,
                deviceType,//java.lang.String deviceClass,
                null //java.lang.String remoteName,
        );
        Remote remote = new Remote(metaData,
                null, //java.lang.String comment,
                null, //java.lang.String notes,
                getCommandIndex(),
                null //java.util.HashMap<java.lang.String,java.util.HashMap<java.lang.String,java.lang.String>> applicationParameters)
                );
        remoteSet = new RemoteSet(getCreatingUser(), irdbOriginName, remote);
    }

    public void load(String deviceType) {
        clearCommands();
        Map<ProtocolDeviceSubdevice, Map<String, Long>> map = deviceTypes.get(deviceType);
        if (map == null)
            return;

        Map<String, Remote> remoteList = new LinkedHashMap<>(16);

        for (Entry<ProtocolDeviceSubdevice, Map<String, Long>> kvp : map.entrySet()) {
            Map<String, Long> commandMap = kvp.getValue();
            ProtocolDeviceSubdevice pds = kvp.getKey();
            Map<String, Command> cmds = load(commandMap, pds, deviceType);
            Remote.MetaData metaData = new Remote.MetaData(manufacturer + "_" + deviceType + "_" + pds.toString(), //java.lang.String name,
                    null, // displayName
                    null, //java.lang.String manufacturer,
                    null, //java.lang.String model,
                    deviceType,//java.lang.String deviceClass,
                    null //java.lang.String remoteName,
            );
            Remote remote = new Remote(metaData,
                    null, //java.lang.String comment,
                    null, //java.lang.String notes,
                    cmds, //getCommandIndex(),
                    null //java.util.HashMap<java.lang.String,java.util.HashMap<java.lang.String,java.lang.String>> applicationParameters)
            );
            remoteList.put(remote.getName(), remote);
        }
        remoteSet = new RemoteSet(getCreatingUser(), irdbOriginName, remoteList);
    }

    private Map<String, Command> load(Map<String, Long> commandMap, ProtocolDeviceSubdevice pds, String deviceType) {
        Map<String, Command> cmds = new LinkedHashMap<>(16);
        commandMap.entrySet().forEach((kvp) -> {
            try {
                Map<String, Long> parameters = mkParameters(pds.getDevice(), pds.subdevice, kvp.getValue());
                Command command = new Command(kvp.getKey(),
                        "IRDB: " + manufacturer + "/" + deviceType + "/" + pds.toString(),
                        pds.getProtocol(),
                        parameters);
                cmds.put(command.getName(), command);
                addCommand(command);
            } catch (GirrException ex) {
                System.err.println(ex);
            }
        });
        return cmds;
    }

    public ArrayList<Command> getCommands(String deviceType, ProtocolDeviceSubdevice pds) {
        load(deviceType, pds);
        return getCommands();
    }

    public void load(String deviceType, String protocol, long device, long subdevice) {
        load(deviceType, new ProtocolDeviceSubdevice(protocol, device, subdevice));
    }

    public Command getCommand(String deviceType, String protocol, long device, long subdevice, String functionName) {
        load(deviceType, new ProtocolDeviceSubdevice(protocol, device, subdevice));
        return getCommand(functionName);
    }

    @Override
    public Remote.MetaData getMetaData() {
        return remoteSet.getFirstMetaData();
    }

    public static class ProtocolDeviceSubdevice {
        private String protocol;
        private long device;
        private long subdevice; // use -1 for no subdevice

        public ProtocolDeviceSubdevice(String protocol, long device, long subdevice) {
            this.protocol = protocol;
            this.device = device;
            this.subdevice = subdevice;
        }

        public ProtocolDeviceSubdevice(JsonValue jprotocol, long device, long subdevice) {
            this(jprotocol.isString() ? jprotocol.asString() : null, device, subdevice);
        }

        public ProtocolDeviceSubdevice(String protocol, long device) {
            this(protocol, device, IrCoreUtils.INVALID);
        }

        public ProtocolDeviceSubdevice(JsonObject json) {
            this(json.get("protocol"),
                    parseLong(json.get("device").asString()),
                    parseLong(json.get("subdevice").asString()));
        }

        public long getDevice() {
            return device;
        }

        public long getSubdevice() {
            return subdevice;
        }

        public String getProtocol() {
            return protocol;
        }

        @Override
        public boolean equals(Object o) {
            if (o == null ||  o.getClass() != this.getClass())
                return false;
            ProtocolDeviceSubdevice dsd = (ProtocolDeviceSubdevice) o;
            return protocol.equals(dsd.protocol) && (device == dsd.device) && (subdevice == dsd.subdevice);
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 97 * hash + (this.protocol != null ? this.protocol.hashCode() : 0);
            hash = 97 * hash + (int) (this.device ^ (this.device >>> 32));
            hash = 97 * hash + (int) (this.subdevice ^ (this.subdevice >>> 32));
            return hash;
        }

        @Override
        public String toString() {
            return "protocol=" + protocol
                    + ", device=" + Long.toString(device)
                    + (subdevice == IrCoreUtils.INVALID ? "" : (", subdevice=" + Long.toString(subdevice)));
        }
    }
}

