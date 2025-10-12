/*
Copyright (C) 2016, 2025 Bengt Martensson.

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

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonValue;
import javax.json.stream.JsonParser;
import org.harctoolbox.girr.Command;
import org.harctoolbox.girr.Remote;
import org.harctoolbox.girr.RemoteSet;
import org.harctoolbox.ircore.ThisCannotHappenException;
import org.harctoolbox.irscrutinizer.Props;

public class ControlTowerIrDatabase extends DatabaseImporter implements IRemoteSetImporter {

    public final static String protocol = "https";
    public final static String controlTowerIrDatabaseHost = "irdb.globalcache.com";
    public final static int portNo = 8081;
    public final static String path = "/api";
    private final static String globalCacheDbOrigin = controlTowerIrDatabaseHost;

    // See https://www.globalcache.com/files/docs/API-GlobalIRDB_ver1.pdf page 6f.
    private static String httpEncode(String s) throws UnsupportedEncodingException {
        return s.replaceAll("&", "xampx").replaceAll("/", "xfslx")
                .replaceAll(">", "xgtx").replaceAll("<", "xltx")
                .replaceAll(":", "xcolx").replaceAll("\\?", "xquex")
                .replaceAll("%", "xmodx").replaceAll("\\+", "xaddx");
    }

    @SuppressWarnings("UseOfSystemOutOrSystemErr")
    public static void main(String[] args) {
        Props props = new Props(null);
        Importer.setProperties(props);
        try {
            ControlTowerIrDatabase gcdb = new ControlTowerIrDatabase(true);
            System.out.println(gcdb.getDeviceTypes("Sony"));
            System.out.println(gcdb.getModels("Sony", "Projector"));
            System.out.println(gcdb.getManufacturers("Blu Ray"));
            System.out.println(gcdb.getModels("Oppo Digital", "Blu Ray"));
            System.out.println(gcdb.getModel(1758));
        } catch (MalformedURLException ex) {
            Logger.getLogger(ControlTowerIrDatabase.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException | URISyntaxException ex) {
            System.err.println(ex.getMessage());
        }
    }

    private boolean verbose = false;
    private Map<String, String> manufacturerMap;
    private Map<String, String> typesMap;
    private String manufacturer;
    private String deviceType;
    private RemoteSet remoteSet;

    public ControlTowerIrDatabase(boolean verbose) {
        super(globalCacheDbOrigin);
        this.verbose = verbose;
    }

    @SuppressWarnings("UseOfSystemOutOrSystemErr")
    private InputStreamReader getReader(String str) throws IOException, URISyntaxException {
        URL url = new URI(protocol, null, controlTowerIrDatabaseHost, portNo, path + "/" + str, null, null).toURL();
        if (verbose)
            System.err.println("Opening " + url);
        URLConnection urlConnection = url.openConnection();
        InputStream is = urlConnection.getInputStream();
        return new InputStreamReader(is, Charset.forName("US-ASCII"));
    }

    private JsonValue readFrom(String str) throws IOException, URISyntaxException {
        return readFrom(getReader(str));
    }

    private JsonValue readFrom(Reader reader) throws IOException {
        JsonParser parser = Json.createParser(reader);
        parser.next();
        return parser.getValue();
    }

    private JsonArray getJsonArray(String str) throws IOException, URISyntaxException {
        return readFrom(str).asJsonArray();
    }

    private JsonObject getJsonObject(String str) throws IOException, URISyntaxException {
        return readFrom(str).asJsonObject();
    }

    private Map<String, String> getMap(String urlFragment, String keyName, String valueName) throws IOException, URISyntaxException {
        JsonArray array = getJsonArray(urlFragment);
        Map<String,String> map = new HashMap<>(array.size());
        array.stream().map(val -> val.asJsonObject()).forEachOrdered(obj -> {
            map.put(obj.getString(keyName), obj.getString(valueName));
        });
        return map;
    }

    private void loadManufacturers() throws IOException, URISyntaxException {
        manufacturerMap = getMap("brands", "Name", "$id");
    }

    private void loadTypes() throws IOException, URISyntaxException {
        typesMap = getMap("types", "Name", "$id");
    }

    public Collection<String> getManufacturers() throws IOException, URISyntaxException {
        if (manufacturerMap == null)
            loadManufacturers();
        return manufacturerMap.keySet();
    }

    public Collection<String> getDeviceTypes() throws IOException, URISyntaxException {
        if (typesMap == null)
            loadTypes();
        return typesMap.keySet();
    }

    public Collection<String> getDeviceTypes(String manufacturerKey) throws IOException, URISyntaxException {
        return getMap("brands/" + httpEncode(manufacturerKey) + "/types", "Type", "$id").keySet();
    }

    // Note: This call is not optimized, it may not perform as well as a call to
    //“api/brands/{brand}/types”.
    public Collection<String> getManufacturers(String type) throws IOException, URISyntaxException {
        return getMap("types/" + httpEncode(type) + "/brands", "Brand", "$id").keySet();
    }

    public Map<String, String> getModels(String manufacturer, String type) throws IOException, URISyntaxException {
        return getMap("brands/" + httpEncode(manufacturer) + "/types/" + httpEncode(type) + "/models", "Name", "ID");
    }

    public Model getModel(int setId) throws IOException, URISyntaxException {
        JsonObject obj = getJsonObject("/codesets/" + Integer.toString(setId) + "/models");
        return new Model(obj);
    }

    public Map<String, String> getCodesetTable(String manufacturerKey, String deviceTypeKey) throws IOException, URISyntaxException {
        return getMap("brands/" + httpEncode(manufacturerKey) + "/types/" + httpEncode(deviceTypeKey) + "/models",
                "Name", "ID");
    }

    public Collection<String> getCommands(int setId) throws IOException, URISyntaxException {
        return getMap("codesets/" + Integer.toString(setId) + "/functions",
                "Function", "$id").keySet();
    }

    public void load(String manufacturerKey, String deviceTypeKey, String codeSet) throws IOException, URISyntaxException {
        clearCommands();
        manufacturer = manufacturerKey;
        deviceType = deviceTypeKey;
        JsonArray array = getJsonArray("codesets/" + codeSet + "/functions");
        for (JsonValue val : array) {
            JsonObject obj = val.asJsonObject();
            String keyName = obj.getString("Function");
            Command cmd = new Command(keyName);
            addCommand(cmd);
        }

        Remote.MetaData metaData = new Remote.MetaData(
                manufacturer + "_" + deviceType + "_" + codeSet, // name
                manufacturer + " " + deviceType + " " + codeSet, // display name,
                manufacturer,
                null, //java.lang.String model,
                deviceType, //java.lang.String deviceClass,
                null //java.lang.String remoteName;
        );

        Remote remote = new Remote(
               metaData,
                null, //java.lang.String comment,
                null, // notes
                getCommandIndex(),
                null //java.util.HashMap<java.lang.String,java.util.HashMap<java.lang.String,java.lang.String>> applicationParameters)
                );

        remoteSet = new RemoteSet(getCreatingUser(), origin, remote);
    }

    @Override
    public RemoteSet getRemoteSet() {
        return remoteSet;
    }

    @Override
    public String getFormatName() {
        return "Global Caché Control Tower IR Database";
    }

    /**
     * Not called.
     * @return Nuthin...
     */
    @Override
    public Remote.MetaData getMetaData() {
        throw new ThisCannotHappenException();
    }

    @SuppressWarnings("PublicInnerClass")
    public static class LoginException extends Exception {
        public LoginException(String message) {
            super(message);
        }
    }
    @SuppressWarnings("PublicInnerClass")
    public static class Model {
        private final String brand;
        private final String type;
        private final String name;
        private final String notes;

        public Model(JsonObject obj) {
            brand = obj.getString("Brand");
            type = obj.getString("Type");
            name = obj.getString("Name");
            notes = obj.getString("Notes");
        }

        @Override
        public String toString() {
            return "Brand: " + brand + "; Type: " + type + "; Name: " + name + "; Notes: " + notes;
        }
    }
}
