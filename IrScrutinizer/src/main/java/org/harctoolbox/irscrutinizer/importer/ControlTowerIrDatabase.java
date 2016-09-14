/*
Copyright (C) 2016 Bengt Martensson.

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
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.harctoolbox.IrpMaster.IrpUtils;
import org.harctoolbox.girr.Command;
import org.harctoolbox.girr.Remote;
import org.harctoolbox.girr.RemoteSet;
import org.harctoolbox.irscrutinizer.Props;

public class ControlTowerIrDatabase extends DatabaseImporter implements IRemoteSetImporter {

    public final static String protocol = "https";
    public final static String controlTowerIrDatabaseHost = "irdb.globalcache.com";
    public final static int portNo = 8081;
    public final static String path = "/api";
    private final static String globalCacheDbOrigin = controlTowerIrDatabaseHost;

    private boolean verbose = false;
    private String apiKey;
    private final String email;
    private final String password;
    private int codesRequestedToday;
    private String accountType;
    private String name;
    private String company;

    private Map<String, String> manufacturerMap;
    private Map<String, String> typesMap;
    private String manufacturer;
    private String deviceType;
    private RemoteSet remoteSet;

    public static class LoginException extends Exception {
        public LoginException(String message) {
            super(message);
        }
    }

    public static class Model {
        private final String brand;
        private final String type;
        private final String name;
        private final String notes;

        public Model(JsonObject obj) {
            brand = obj.get("Brand").asString();
            type = obj.get("Type").asString();
            name = obj.get("Name").asString();
            notes = obj.get("Notes").asString();
        }

        @Override
        public String toString() {
            return "Brand: " + brand + "; Type: " + type + "; Name: " + name + "; Notes: " + notes;
        }
    }

    private JsonObject postAndGetObject(String str, String payload) throws MalformedURLException, IOException, LoginException {
        URL url = new URL(protocol, controlTowerIrDatabaseHost, portNo, path + str);
        if (verbose)
            System.err.println("Opening (POST) " + url.toString());
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Accept", "application/json");
        //if (payload != null) {
            connection.setRequestProperty("Content-Type", "application/json; charset=US-ASCII");
            connection.setRequestProperty("Content-Length", "" + Integer.toString(payload == null ? 0 : payload.getBytes(IrpUtils.dumbCharset).length));
            connection.setRequestProperty("Content-Language", "en-US");
        //}

        connection.setUseCaches(false);
        connection.setDoInput(true);
        connection.setDoOutput(payload != null);
        if (payload != null) {
            try (OutputStream outputStream = connection.getOutputStream()) {
                outputStream.write(payload.getBytes("US-ASCII"));
                outputStream.flush();
            }
        }

        int httpResult = connection.getResponseCode();

        if (httpResult != HttpURLConnection.HTTP_OK) {
            connection.disconnect();
            throw new LoginException("HTTP error " + httpResult);
        }

        InputStream is = connection.getInputStream();
        InputStreamReader isr = new InputStreamReader(is, IrpUtils.dumbCharset);
        JsonObject response = JsonObject.readFrom(isr);
        connection.disconnect();
        return response;
    }

    private String apiKeyString(char separator) {
        return apiKey == null ? "" : (separator + "apiKey=" + apiKey);
    }

    private void evaluateAccount(JsonObject acct) {
        accountType = acct.get("AccountType").asString();
        apiKey = acct.get("ApiKey").asString();
        codesRequestedToday = acct.get("CodesRequestedToday").asInt();
        //this.name = acct.get("Name").asString();
        //this.company = acct.get("Company").asString();
    }

    private void evaluateAccount() {
        accountType = null;
        apiKey = null;
        codesRequestedToday = -1;
    }

    public void login() throws MalformedURLException, IOException, LoginException {
        JsonObject jsonObject = new JsonObject();
        jsonObject.add("Email", email);
        jsonObject.add("Password", password);
        String payload = jsonObject.toString();

        JsonObject response = postAndGetObject("/account/login", payload);
        String status = response.get("Status").asString();
        if (!status.equals("success"))
            throw new LoginException(response.get("Message").asString());
        JsonObject acct = response.get("Account").asObject();
        evaluateAccount(acct);
    }

    public void logout() throws MalformedURLException, IOException, LoginException {
        if (apiKey == null)
            throw new LoginException("Not logged in");
        JsonObject response = postAndGetObject("/account/logout" + apiKeyString('?'), "");
        String status = response.get("Status").asString();
        if (!status.equals("success"))
            throw new LoginException(response.get("Message").asString());
        evaluateAccount();
    }

    private InputStreamReader getReader(String str) throws IOException {
        URL url = new URL(protocol, controlTowerIrDatabaseHost, portNo, path + "/" + str);
        if (verbose)
            System.err.println("Opening " + url.toString());
        URLConnection urlConnection = url.openConnection(/*proxy*/);
        InputStream is = urlConnection.getInputStream();
        InputStreamReader isr = new InputStreamReader(is, IrpUtils.dumbCharset);
        return isr;
    }

    private JsonArray getJsonArray(String str) throws IOException {
        JsonArray arr = JsonArray.readFrom(getReader(str));
        return arr;
    }

    private JsonObject getJsonObject(String str) throws IOException {
        JsonObject obj = JsonObject.readFrom(getReader(str));
        return obj;
    }


    private Map<String, String> getMap(String urlFragment, String keyName, String valueName) throws IOException {
        JsonArray array = getJsonArray(urlFragment);
        Map<String,String> map = new HashMap<>(16);
        for (JsonValue val : array) {
            JsonObject obj = val.asObject();
            map.put(obj.get(keyName).asString(), obj.get(valueName).asString());
        }
        return map;
    }

    private void loadManufacturers() throws IOException {
        manufacturerMap = getMap("brands", "Name", "$id");
    }

    private void loadTypes() throws IOException {
        typesMap = getMap("types", "Name", "$id");
    }

    public String getStatus() throws IOException {
        if (apiKey == null)
            return "Not logged in";
        JsonObject obj = getJsonObject("account" + apiKeyString('?'));
        evaluateAccount(obj);
        return obj.toString();
    }

    public Collection<String> getManufacturers() throws IOException {
        if (manufacturerMap == null)
            loadManufacturers();
        return manufacturerMap.keySet();
    }

    public Collection<String> getDeviceTypes() throws IOException {
        if (typesMap == null)
            loadTypes();
        return typesMap.keySet();
    }

    public Collection<String> getDeviceTypes(String manufacturerKey) throws IOException {
        return getMap("brands/" + httpEncode(manufacturerKey) + "/types", "Type", "$id").keySet();
    }

    // Note: This call is not optimized, it may not perform as well as a call to
    //“api/brands/{brand}/types”.
    public Collection<String> getManufacturers(String type) throws IOException {
        return getMap("types/" + httpEncode(type) + "/brands", "Brand", "$id").keySet();
    }

    public Map<String, String> getModels(String manufacturer, String type) throws IOException {
        return getMap("brands/" + httpEncode(manufacturer) + "/types/" + httpEncode(type) + "/models", "Name", "ID");
    }

    public Model getModel(int setId) throws IOException {
        JsonObject obj = getJsonObject("/codesets/" + Integer.toString(setId) + "/models");
        return new Model(obj);
    }

    public Map<String, String> getCodesetTable(String manufacturerKey, String deviceTypeKey) throws IOException {
        return getMap("brands/" + httpEncode(manufacturerKey) + "/types/" + httpEncode(deviceTypeKey) + "/models",
                "Name", "ID");
    }

    public Collection<String> getCommands(int setId) throws IOException {
        return getMap("codesets/" + Integer.toString(setId) + "/functions",
                "Function", "$id").keySet();
    }

    public void getCodeset(int setId, boolean email) throws IOException, LoginException {
        if (apiKey == null)
            throw new LoginException("Must be logged in");
        if (email) {
            JsonObject obj = getJsonObject("codesets/" + Integer.toString(setId) + "?output=email" + apiKeyString('&'));
            if (!obj.get("Status").asString().equals("success")) {
                System.err.println(obj.get("Message").asString());
            }
            String str = obj.toString();
            System.out.println(str);
        } else {
            JsonObject obj = getJsonObject("codesets/" + Integer.toString(setId) + "?output=direct" + apiKeyString('&'));
            System.out.println(obj.toString());
            if (!obj.get("Status").asString().equals("success")) {
                System.err.println(obj.get("Message").asString());
            }
            String str = obj.toString();
            System.out.println(str);
        }
    }

    public void load(String manufacturerKey, String deviceTypeKey, String codeSet) throws IOException {
        clearCommands();
        manufacturer = manufacturerKey;
        deviceType = deviceTypeKey;
        JsonArray array = getJsonArray("codesets/" + codeSet + "/functions");
        for (JsonValue val : array) {
            JsonObject obj = val.asObject();
            String keyName = obj.get("Function").asString();
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

    private static String httpEncode(String s) throws UnsupportedEncodingException {
        String str = s.replaceAll("&", "xampx").replaceAll("/", "xfslx")
                .replaceAll(">", "xgtx").replaceAll("<", "xltx")
                .replaceAll(":", "xcolx").replaceAll("\\?", "xquex")
                .replaceAll("%", "xmodx").replaceAll("\\+", "xaddx");
        return URLEncoder.encode(str, "utf-8").replaceAll("\\+", "%20");
    }

    public ControlTowerIrDatabase(String apiKey, boolean verbose) {
        super(globalCacheDbOrigin);
        this.company = null;
        this.name = null;
        this.accountType = null;
        this.codesRequestedToday = -1;
        this.email = null;
        this.password = null;
        this.apiKey = apiKey;
        this.verbose = verbose;
    }

    public ControlTowerIrDatabase(boolean verbose) {
        this(null, null, verbose);
    }

    public ControlTowerIrDatabase(String email, String password, boolean verbose) {
        super(globalCacheDbOrigin);
        this.company = null;
        this.name = null;
        this.accountType = null;
        this.codesRequestedToday = -1;
        this.email = email;
        this.password = password;
        this.apiKey = null;
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
            ControlTowerIrDatabase gcdb = new ControlTowerIrDatabase(args[0], args[1], true);
            //ControlTowerIrDatabase gcdb = new ControlTowerIrDatabase("6G7pBwnQwUCSg5JNPRtcQ", true);
            //System.out.println(gcdb.getTypes());
            //System.out.println(gcdb.getManufacturers());
            System.out.println(gcdb.getDeviceTypes("Sony"));
            System.out.println(gcdb.getModels("Sony", "Projector"));
            System.out.println(gcdb.getManufacturers("Blu Ray"));
            System.out.println(gcdb.getModels("Oppo Digital", "Blu Ray"));
            System.out.println(gcdb.getModel(1758));
            gcdb.login();
            System.out.println(gcdb.getStatus());
            gcdb.getCodeset(1758, false);
            System.out.println(gcdb.getStatus());
            gcdb.logout();
        } catch (MalformedURLException ex) {
            Logger.getLogger(ControlTowerIrDatabase.class.getName()).log(Level.SEVERE, null, ex);
        } catch (LoginException ex) {
            System.err.println("Login failed: " + ex.getMessage());
        } catch (IOException ex) {
            System.err.println(ex.getMessage());
        }
    }

    @Override
    public String getFormatName() {
        return "Global Caché Control Tower IR Database";
    }
}
