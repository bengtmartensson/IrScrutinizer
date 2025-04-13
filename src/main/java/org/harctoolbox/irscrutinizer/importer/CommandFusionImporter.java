/*
Copyright (C) 2015 Bengt Martensson.

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
import java.io.Reader;
import java.io.Serializable;
import java.text.ParseException;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonValue;
import javax.json.stream.JsonParser;
import org.harctoolbox.girr.Command;
import org.harctoolbox.girr.GirrException;
import org.harctoolbox.girr.Remote;
import org.harctoolbox.girr.RemoteSet;
import org.harctoolbox.ircore.InvalidArgumentException;
import org.harctoolbox.irscrutinizer.Version;

/**
 * This class imports CommandFusion files. Only CommandFusion files having exactly one remote are presently supported.
 *
 */
public class CommandFusionImporter extends RemoteSetImporter implements IReaderImporter, Serializable {
    public static final String homeUrl = "https://www.commandfusion.com/index.php";

    public static void main(String[] args) {
        CommandFusionImporter importer = new CommandFusionImporter();
        try {
            importer.load(args[0]);
        } catch (IOException | ParseException | InvalidArgumentException ex) {
            Logger.getLogger(CommandFusionImporter.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public void load(Reader reader, String origin) throws IOException {
        prepareLoad(origin);
        JsonParser parser = Json.createParser(reader);
        JsonParser.Event x = parser.next();
        JsonObject jsonObject = parser.getObject();
        Remote remote = parseRemote(jsonObject);
        remoteSet = new RemoteSet(getCreatingUser(),
                 origin, //java.lang.String source,
                 (new Date()).toString(), //java.lang.String creationDate,
                 Version.appName, //java.lang.String tool,
                 Version.version, //java.lang.String toolVersion,
                 null, //java.lang.String tool2,
                 null, //java.lang.String tool2Version,
                 null, //java.lang.String notes,
                 remote);
         setupCommands();
    }

    Remote parseRemote(JsonObject jsonObject) {
        JsonObject remoteInfo = jsonObject.getJsonObject("RemoteInfo");
        JsonArray remoteFunctions = jsonObject.getJsonArray("RemoteFunctions");

        Map<String, Command> commands = new LinkedHashMap<>(8);
        for (JsonValue c : remoteFunctions) {
            Command command = parseCommand((JsonObject) c);
            if (command != null)
                commands.put(command.getName(), command);
        }
        String name = remoteInfo.getString("RemoteID", null);
        String deviceClass = remoteInfo.getString("DeviceFamily", null);
        String manufacturer = remoteInfo.getString("Manufacturer", null);
        String model = remoteInfo.getString("DeviceModel", null);
        String remoteName = remoteInfo.getString("RemoteModel", null);
        Map<String, String> notes = new HashMap<>(1);
        notes.put("Description", remoteInfo.getString("Description", null));

        return new Remote(new Remote.MetaData(name, null, manufacturer, model, deviceClass, remoteName),
                null /* String comment */, notes, commands,
                null /* HashMap<String,HashMap<String,String>> applicationParameters*/);
    }


    private Command parseCommand(JsonObject cmd) {
        String name = cmd.getString("ID", null);
        String ccf = cmd.getString("CCF", null);
        Command command = null;
        try {
            command = new Command(name, null, ccf);
        } catch (GirrException ex) {
            Logger.getLogger(CommandFusionImporter.class.getName()).log(Level.SEVERE, null, ex);
        }
        return command;
    }

    @Override
    public boolean canImportDirectories() {
        return false;
    }

    @Override
    public String[][] getFileExtensions() {
        return new String[][]{new String[]{"CommandFusion files (*.cfir)", "cfir" }};
    }

    @Override
    public String getFormatName() {
        return "CommandFusion";
    }
}
