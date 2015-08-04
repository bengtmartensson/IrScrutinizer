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

import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import org.harctoolbox.IrpMaster.IrSignal;
import org.harctoolbox.IrpMaster.IrpUtils;
import org.harctoolbox.girr.Command;
import org.harctoolbox.girr.Remote;
import org.harctoolbox.girr.RemoteSet;
import org.harctoolbox.irscrutinizer.Props;

public class GlobalCacheIrDatabase extends DatabaseImporter implements IRemoteSetImporter {

    public final static String globalCacheIrDatabaseHost = "irdatabase.globalcache.com";
    private final static String path = "/api/v1/";
    private final static String globalCacheDbOrigin = globalCacheIrDatabaseHost;
    private static final long serialVersionUID = 1L;
    private boolean verbose = false;

    //private transient Proxy proxy = Proxy.NO_PROXY;

    private final String apiKey;
    private HashMap<String, String> manufacturerMap = null;
    private String manufacturer;
    private String deviceType;
    //private String codeSet;
    private RemoteSet remoteSet;

    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

    private JsonArray getJsonArray(String str) throws IOException {
        URL url = new URL("http", globalCacheIrDatabaseHost, path + apiKey + "/" + str);
        if (verbose)
            System.err.println("Opening " + url.toString());
        URLConnection urlConnection = url.openConnection(/*proxy*/);
        return JsonArray.readFrom(new InputStreamReader(urlConnection.getInputStream(), IrpUtils.dumbCharset));
    }

    private HashMap<String, String> getMap(String urlFragment, String keyName, String valueName) throws IOException {
        JsonArray array = getJsonArray(urlFragment);
        HashMap<String,String> map = new HashMap<>();
        for (JsonValue val : array) {
            JsonObject obj = val.asObject();
            map.put(obj.get(keyName).asString(), obj.get(valueName).asString());
        }
        return map;
    }

    private void loadManufacturers() throws IOException {
        manufacturerMap = getMap("manufacturers", "Manufacturer", "Key");
    }

    public Collection<String> getManufacturers() throws IOException {
        if (manufacturerMap == null)
            loadManufacturers();
        return manufacturerMap.values();
    }

    public Collection<String> getDeviceTypes(String manufacturerKey) throws IOException {
        return getMap("manufacturers/" + httpEncode(manufacturerKey) + "/devicetypes", "DeviceType", "Key").values();
    }

    public Collection<String> getCodeset(String manufacturerKey, String deviceTypeKey) throws IOException {
        return getMap("manufacturers/" + httpEncode(manufacturerKey) + "/devicetypes/" + httpEncode(deviceTypeKey) + "/codesets",
                "Codeset", "Key").values();
    }

    public ArrayList<Command> getCommands(String manufacturerKey, String deviceTypeKey, String codeSet) throws IOException {
        load(manufacturerKey, deviceTypeKey, codeSet);
        return getCommands();//codesMap;
    }

    public void load(String manufacturerKey, String deviceTypeKey, String codeSet) throws IOException {
        clearCommands();
        manufacturer = manufacturerKey;
        deviceType = deviceTypeKey;
        //this.codeSet = codeSet;
        JsonArray array = getJsonArray("manufacturers/" + httpEncode(manufacturerKey) + "/devicetypes/" + httpEncode(deviceTypeKey) + "/codesets/" + codeSet);
        //codesMap = new HashMap<String, IrSignal>();
        for (JsonValue val : array) {
            JsonObject obj = val.asObject();
            String str = obj.get("IRCode").asString();
            String[] chunks = str.split(",");
            int index = 0;
            int frequency = Integer.parseInt(chunks[index++].trim());
            index++; // number repetitions, discard
            int repIndex = Integer.parseInt(chunks[index++].trim());
            int[] durations = new int[chunks.length - index];
            double T = 1000000f/(double)frequency; // period time in micro seconds
            for (int i = 0; i <  chunks.length - index; i++)
                durations[i] = (int) Math.round(Integer.parseInt(chunks[i + index]) * T);
            IrSignal irSignal = new IrSignal(durations, (repIndex - 1)/2, (durations.length - repIndex + 1)/2, frequency);
            String keyName = obj.get("KeyName").asString();
            Command cmd = new Command(keyName,
                    "GCDB: " + manufacturer + "/" + deviceType + "/" + codeSet, irSignal,
                    isGenerateCcf(), isInvokeDecodeIr());
            addCommand(cmd);
        }

        Remote remote = new Remote(manufacturer + "_" + deviceType + "_" + codeSet, //java.lang.String name,
                manufacturer,
                null, //java.lang.String model,
                deviceType, //java.lang.String deviceClass,
                null, //java.lang.String remoteName,
                null, //java.lang.String comment,
                null,
                getCommandIndex(),
                null //java.util.HashMap<java.lang.String,java.util.HashMap<java.lang.String,java.lang.String>> applicationParameters)
                );

        remoteSet = new RemoteSet(getCreatingUser(), origin, remote);
    }

    private static String httpEncode(String s) throws UnsupportedEncodingException {
        return URLEncoder.encode(s, "utf-8").replaceAll("\\+", "%20");
    }

    public GlobalCacheIrDatabase(String apiKey, boolean verbose) {
        super(globalCacheDbOrigin);
        this.apiKey = apiKey;
        this.verbose = verbose;
    }

    @Override
    public RemoteSet getRemoteSet() {
        return remoteSet;
    }

    public static void main(String[] args) {
        Props props = new Props(null);
        Importer.setProperties(props);
        try {
            GlobalCacheIrDatabase gcdb = new GlobalCacheIrDatabase(props.getGlobalCacheApiKey(), true);
            System.out.println(gcdb.getManufacturers());
            System.out.println(gcdb.getDeviceTypes("philips"));
            //System.out.println(gcdb.getDeviceTypes("bell & howell"));
            System.out.println(gcdb.getCodeset("sony", "laser disc"));
            System.out.println(gcdb.getCommands("sony", "laser disc", "201"));
        } catch (IOException ex) {
            System.err.println(ex.getMessage());
        }
    }

    @Override
    public String getFormatName() {
        return "Global Caché IR Database";
    }
}
