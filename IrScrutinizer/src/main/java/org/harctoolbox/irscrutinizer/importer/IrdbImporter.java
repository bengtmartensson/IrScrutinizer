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
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.net.Proxy;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map.Entry;
import java.util.Set;
import org.harctoolbox.IrpMaster.IrpMasterException;
import org.harctoolbox.IrpMaster.IrpUtils;
import org.harctoolbox.girr.Command;
import org.harctoolbox.girr.Remote;
import org.harctoolbox.girr.RemoteSet;
import org.harctoolbox.irscrutinizer.Utils;

public class IrdbImporter extends DatabaseImporter implements IRemoteSetImporter {

    private final static Proxy proxy = Proxy.NO_PROXY;
    private final static long invalid = -1L;
    private final static String irdbOriginName = "IRDB";
    private static final long serialVersionUID = 1L;

    private boolean verbose = false;
    private String manufacturer;
    //private HashMap<String,Command> commands = null;
    private RemoteSet remoteSet;

    @Override
    public RemoteSet getRemoteSet() {
        return remoteSet;
    }

    @Override
    public String getFormatName() {
        return "IRDB";
    }

    public static URI getHomeUri() {
        try {
            return new URI("http", irdbHost, null, null);
        } catch (URISyntaxException ex) {
            return null;
        }
    }

    /*@Override
    public HashMap<String, Remote> getRemotes() {
        HashMap<String, Remote> result = new HashMap<String,Remote>(1);
        result.put(remote.getName(), remote);
        return result;
    }

    @Override
    public Remote getFirstRemote() {
        return remote;
    }* /

    @Override
    public Collection<Command> getCommands() {
        return commands.values();
    }

    @Override
    public Command getCommand(String name) {
        return commands.get(name);
    }*/

    public static class ProtocolDeviceSubdevice implements Serializable {
        private static final long serialVersionUID = 1L;

        private String protocol;
        private long device;
        private long subdevice; // use -1 for no subdevice

        public long getDevice() {
            return device;
        }

        public long getSubdevice() {
            return subdevice;
        }

        public String getProtocol() {
            return protocol;
        }

        public ProtocolDeviceSubdevice(String protocol, long device, long subdevice) {
            this.protocol = protocol;
            this.device = device;
            this.subdevice = subdevice;
        }

        public ProtocolDeviceSubdevice(String protocol, long device) {
            this(protocol, device, invalid);
        }

        public ProtocolDeviceSubdevice(JsonObject json) {
            this(json.get("protocol").asString(),
                    parseLong(json.get("device").asString()),
                    parseLong(json.get("subdevice").asString()));
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
                    + (subdevice == invalid ? "" : (", subdevice=" + Long.toString(subdevice)));
        }
    }

    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

    private static long parseLong(String str) {
        try {
            return ((str == null) || str.isEmpty()) ? invalid : Long.parseLong(str);
        } catch (NumberFormatException e) { // Do not treat "None" explicitly
            return invalid;
        }
    }

    private HashMap<String, HashMap<ProtocolDeviceSubdevice, HashMap<String, Long>>> deviceTypes;

    private static ArrayList<String> manufacturers;

    public static String[] getManufacturers(boolean verbose) throws IOException {
        if (manufacturers == null)
            setupManufacturers(verbose);
        return manufacturers.toArray(new String[manufacturers.size()]);
    }

    private final static String irdbHost = "irdb.tk";
    private final static String urlFormat = "/api/code/?brand=%s&page=%d";
    private final static String urlFormatBrands = "/api/brand/?page=%d";

    private static JsonObject getJsonObject(String urlString, boolean verbose) throws IOException {
        URL url = urlString.contains("//") ? new URL(urlString) : new URL("http", irdbHost, urlString);
        if (verbose)
            System.err.print("Accessing " + url.toString() + "...");
        URLConnection urlConnection = url.openConnection(proxy);
        InputStream stream = urlConnection.getInputStream();
        return JsonObject.readFrom(new InputStreamReader(stream, IrpUtils.dumbCharset));
    }

    private static void setupManufacturers(boolean verbose) throws IOException {
        manufacturers = new ArrayList<>();
        String path = String.format(urlFormatBrands, 1);
        for (int index = 1; index <= 100 && !path.isEmpty(); index++) {
            JsonObject o = getJsonObject(path, verbose);
            JsonValue meta = o.get("meta");
            JsonObject metaObject = meta.asObject();
            path = metaObject.get("next").asString();
            if (verbose)
                System.err.println("Read page " + metaObject.get("page").asInt());
            JsonArray objects = o.get("objects").asArray();
            for (JsonValue val : objects) {
                JsonObject obj = val.asObject();
                String brand = obj.get("brand").asString();
                if (!manufacturers.contains(brand))
                    manufacturers.add(brand);
            }
        }
    }

    public IrdbImporter(String manufacturer, boolean verbose) throws IOException {
        super(irdbOriginName);
        this.manufacturer = manufacturer;
        this.verbose = verbose;
        deviceTypes = new LinkedHashMap<>();
        String path = String.format(urlFormat, URLEncoder.encode(manufacturer, "utf-8"), 1);
        for (int index = 1; index <= 100 && !path.isEmpty(); index++) {
            JsonObject o = getJsonObject(path, verbose);
            JsonValue meta = o.get("meta");
            JsonObject metaObject = meta.asObject();
            path = metaObject.get("next").asString();
            if (verbose)
                System.err.println("Read page " + metaObject.get("page").asInt());
            JsonArray objects = o.get("objects").asArray();
            for (JsonValue val : objects) {
                JsonObject obj = val.asObject();
                String deviceType = obj.get("devicetype").asString();
                if (!deviceTypes.containsKey(deviceType))
                    deviceTypes.put(deviceType, new LinkedHashMap<ProtocolDeviceSubdevice, HashMap<String, Long>>());
                HashMap<ProtocolDeviceSubdevice, HashMap<String, Long>> devCollection = deviceTypes.get(deviceType);
                ProtocolDeviceSubdevice pds = new ProtocolDeviceSubdevice(obj);
                if (!devCollection.containsKey(pds))
                    devCollection.put(pds, new LinkedHashMap<String, Long>());
                HashMap<String, Long> cmnds = devCollection.get(pds);
                long function = parseLong(obj.get("function").asString()); // barfs for illegal
                String functionName = obj.get("functionname").asString();
                if (cmnds.containsKey(functionName))
                    System.err.println("functionname " + functionName + " more than once present, overwriting");
                cmnds.put(functionName, function);
            }
        }
    }

    public Set<String> getDeviceTypes() {
        return deviceTypes.keySet();
    }

    public Set<ProtocolDeviceSubdevice> getProtocolDeviceSubdevice(String deviceType) {
        HashMap<ProtocolDeviceSubdevice, HashMap<String, Long>> map = deviceTypes.get(deviceType);
        return map == null ? null : map.keySet();
    }

    //public HashMap<String, Long> getCommands(String deviceType, ProtocolDeviceSubdevice pds) {
    public void load(String deviceType, ProtocolDeviceSubdevice pds) throws IrpMasterException {
        clearCommands();
        HashMap<ProtocolDeviceSubdevice, HashMap<String, Long>> map = deviceTypes.get(deviceType);
        if (map == null)
            return;

        HashMap<String, Long> commandMap = map.get(pds);
        load(commandMap, pds, deviceType);
        Remote remote = new Remote(manufacturer + "_" + deviceType + "_" + pds.toString(), //java.lang.String name,
                null, //java.lang.String manufacturer,
                null, //java.lang.String model,
                deviceType,//java.lang.String deviceClass,
                null, //java.lang.String remoteName,
                null, //java.lang.String comment,
                null, //java.lang.String notes,
                getCommandIndex(),
                null //java.util.HashMap<java.lang.String,java.util.HashMap<java.lang.String,java.lang.String>> applicationParameters)
                );
        remoteSet = new RemoteSet(getCreatingUser(), irdbOriginName, remote);
    }

    public void load(String deviceType) throws IrpMasterException {
        clearCommands();
        HashMap<ProtocolDeviceSubdevice, HashMap<String, Long>> map = deviceTypes.get(deviceType);
        if (map == null)
            return;

        HashMap<String, Remote> remoteList = new LinkedHashMap<>();

        //HashMap<String,Command> cmds;
        for (Entry<ProtocolDeviceSubdevice, HashMap<String, Long>> kvp : map.entrySet()) {
            HashMap<String, Long> commandMap = kvp.getValue();
            ProtocolDeviceSubdevice pds = kvp.getKey();
            HashMap<String, Command> cmds = load(commandMap, pds, deviceType);

            Remote remote = new Remote(manufacturer + "_" + deviceType + "_" + pds.toString(), //java.lang.String name,
                    null, //java.lang.String manufacturer,
                    null, //java.lang.String model,
                    deviceType,//java.lang.String deviceClass,
                    null, //java.lang.String remoteName,
                    null, //java.lang.String comment,
                    null, //java.lang.String notes,
                    cmds, //getCommandIndex(),
                    null //java.util.HashMap<java.lang.String,java.util.HashMap<java.lang.String,java.lang.String>> applicationParameters)
                    );
            remoteList.put(remote.getName(), remote);
        }
        remoteSet = new RemoteSet(getCreatingUser(), irdbOriginName, remoteList);
    }

    private HashMap<String, Command> load(HashMap<String, Long> commandMap, ProtocolDeviceSubdevice pds, String deviceType) throws IrpMasterException {
        HashMap<String,Command> cmds = new LinkedHashMap<>();
        for (Entry<String, Long> kvp : commandMap.entrySet()) {
            //ParametrizedIrSignal paramSig = new ParametrizedIrSignal(pds.getProtocol(), pds.getDevice(), pds.subdevice,
            //        kvp.getValue().longValue(), kvp.getKey(),
            //        "IRDB: " + manufacturer + "/" + deviceType + "/" + pds.toString());
            HashMap<String, Long> parameters = Utils.mkParameters(pds.getDevice(), pds.subdevice, kvp.getValue().longValue());
            Command command = new Command(kvp.getKey(),
                    "IRDB: " + manufacturer + "/" + deviceType + "/" + pds.toString(),
                    pds.getProtocol(),
                    parameters);
            //commands.put(command.getName(), command);
            cmds.put(command.getName(), command);
            addCommand(command);
        }
        return cmds;
    }

    public ArrayList<Command> getCommands(String deviceType, ProtocolDeviceSubdevice pds) throws IrpMasterException {
        load(deviceType, pds);
        return getCommands();
    }

    public void load(String deviceType, String protocol, long device, long subdevice) throws IrpMasterException {
        //return getCommands(deviceType, new ProtocolDeviceSubdevice(protocol, device, subdevice));
        load(deviceType, new ProtocolDeviceSubdevice(protocol, device, subdevice));
    }

    public Command getCommand(String deviceType, String protocol, long device, long subdevice, String functionName) throws IrpMasterException {
        //HashMap<String, Long> map = getCommands(deviceType, protocol, device, subdevice);
        load(deviceType, new ProtocolDeviceSubdevice(protocol, device, subdevice));
        //return commandIndex == null ? invalid : map.get(functionName);
        return getCommand(functionName);
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
        } catch (IOException | IrpMasterException ex) {
            System.err.println(ex.getMessage());
        }
    }

}

