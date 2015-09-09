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

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.Serializable;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map.Entry;
import org.harctoolbox.IrpMaster.IrpMasterException;
import org.harctoolbox.IrpMaster.IrpUtils;
import org.harctoolbox.IrpMaster.XmlUtils;
import org.harctoolbox.girr.Command;
import org.harctoolbox.girr.Remote;
import org.harctoolbox.girr.RemoteSet;
import org.harctoolbox.irscrutinizer.Utils;
import org.harctoolbox.irscrutinizer.Version;
import org.w3c.dom.Document;

/**
 * This class is a simple-minded importer of RMDU files.
 */
public class RmduImporter extends RemoteSetImporter implements Serializable, IReaderImporter {
    private static final long serialVersionUID = 1L;

    //public static final String[][] fileExtensions = new String[][]{ new String[]{"rmdu", "RemoteMaster device updates"}};

    private HashMap<String,String>parameters;
    //private HashMap<String,Command>commands;
    private Remote remote;
    //private String protocolsIniPath;
    private ProtocolsIni protocolsIni;

    public static final String homeUrl = "http://www.hifi-remote.com/wiki/index.php?title=Remote_Master_Manual";

    /**
     * @param protocolsIni the protocolsIni to set
     */
    public void setProtocolsIni(ProtocolsIni protocolsIni) {
        this.protocolsIni = protocolsIni;
    }

    public RmduImporter() {
        super();
    }

    /**
     *
     * @param protocolsIni
     */
    public RmduImporter(ProtocolsIni protocolsIni) {
        super();
        this.protocolsIni = protocolsIni;
    }

    //public RmduImporter(File protocolsIniPath) throws IOException {
    //    this(protocolsIniPath != null ? new ProtocolsIni(protocolsIniPath) : null);
    //}

    private final static String separator = "=";

    /**
     * @return the parameters
     */
    public HashMap<String,String> getParameters() {
        return parameters;
    }

    @Override
    public String[][] getFileExtensions() {
        return new String[][]{
            new String[]{"RemoteMaster device updates (*.rmdu)", "rmdu" },
            new String[]{"RemoteMaster all files (*.rmdu *.rmir)", "rmdu", "rmir"},
            new String[]{"RemoteMaster remote configurations (*.rmir)", "rmir" }
        };
    }

    /*@Override
    public HashMap<String, Remote> getRemotes() {
        HashMap<String, Remote> result = new HashMap<String, Remote>(1);
        result.put(remote.getName(), remote);
        return result;
    }

    @Override
    public Remote getFirstRemote() {
        return remote;
    }*/

    public Remote getRemote() {
        return remote;
    }

    /*public RmduImporter(String rmduFile, ProtocolsIni protocolsIni) throws FileNotFoundException, IOException {
        this(new FileReader(rmduFile), rmduFile, protocolsIni);
    }

    public RmduImporter(ProtocolsIni protocolsIni) throws IOException {
        this(new InputStreamReader(System.in), "<STDIN>", protocolsIni);
    }

    public RmduImporter(Reader reader, String origin, ProtocolsIni protocolsIni) throws IOException {
        this();
                }
                * */

    @Override
    public void load(Reader reader, String origin) throws IOException, ParseException {
        prepareLoad(origin);
        parameters = new LinkedHashMap<>();
        //commands = new LinkedHashMap<String,Command>();
        BufferedReader bufferedReader = new BufferedReader(reader);
        HashMap<Integer,Long> functionHex = new HashMap<>();
        HashMap<Integer,String> functionName = new HashMap<>();
        HashMap<Integer,String> functionNotes = new HashMap<>();
        int lineNo = 0;
        while (true) {
            String line = bufferedReader.readLine();
            lineNo++;
            if (line == null)
                break;

            String[] chunks = line.split(separator);
            if (chunks.length != 2)
                continue;

            String key = chunks[0];
            String value = chunks[1];
            if (key.startsWith("Function.")) {
                int number = Integer.parseInt(key.substring(9, key.lastIndexOf('.')));
                if (key.endsWith(".name")) {
                    functionName.put(number, value);
                } else if (key.endsWith(".hex")) {
                    functionHex.put(number, Long.parseLong(value.replaceAll(" ", ""), 16));
                } else if (key.endsWith(".notes")) {
                    functionNotes.put(number, value);
                } else {
                    System.err.println("Unknown stuff");
                }
            } else {
                if (parameters.containsKey(key)) {
                    System.err.println("Warning: multiple occurances of key " + key + ", ignoring all but the first");
                } else {
                    parameters.put(key, value);
                }
            }
        }

        String protocolName = parameters.get("Protocol.name");
        if (protocolName == null)
            throw new ParseException("Protocol.name not found", lineNo);
        ProtocolsIni.ICmdTranslator[] translators = (protocolsIni != null)
                ? protocolsIni.getCmdTranslators(protocolName)
                : null;
        if ((translators == null || translators.length == 0) && protocolsIni != null)
            translators = protocolsIni.getCmdTranslators(Integer.parseInt(parameters.get("Protocol").replaceAll(" ", ""),16));

        String[] devParams = protocolsIni != null
                ? protocolsIni.getDeviceParameters(parameters.get("Protocol.name"))
                : null;
        if ((devParams == null || devParams.length == 0) && protocolsIni != null)
            devParams = protocolsIni.getDeviceParameters(Integer.parseInt(parameters.get("Protocol").replaceAll(" ", ""), 16));
        HashMap<String,Long> protocolParams = new HashMap<>();
        String protocolParamsString = parameters.get("ProtocolParms");
        String[] params = protocolParamsString != null ? protocolParamsString.split(" ") : new String[0];
        for (int i = 0; i < params.length; i++) {
            try {
                String paramName = (devParams != null && i < devParams.length && devParams[i] != null) ? devParams[i] : "param" + i;
                protocolParams.put(paramName, Long.parseLong(params[i]));
            } catch (NumberFormatException ex) {
                // just ignore those that cannot be parsed, like "null"
            }
        }

        for (Entry<Integer, String> kvp : functionName.entrySet()) {
            Integer functionNo = kvp.getKey();
            HashMap<String, Long> commandParameters = new HashMap<>();
            commandParameters.putAll(protocolParams);
            Long hexObject = functionHex.get(functionNo);
            long hex = hexObject != null ? hexObject : IrpUtils.invalid;
            if (hex != IrpUtils.invalid)
                commandParameters.put("hex", hex);
            if (translators != null && translators.length > 0 && translators[0] != null && hex != IrpUtils.invalid) {
                commandParameters.put("F", translators[0].translate(hex));
            }

            try {
                Command command = new Command(kvp.getValue(),
                        functionNotes.get(functionNo) != null ? functionNotes.get(functionNo) : "Function." + functionNo,
                        getParameters().get("Protocol.name"), // not completely right
                        commandParameters);
                //commands.put(command.getName(), command);
                addCommand(command);
            } catch (IrpMasterException ex) {
                // just ignore the silly command
                System.err.println("Warning: Command with name '" + kvp.getValue() + "' is erroneous and was ignored.");
            }
        }

        HashMap<String,HashMap<String,String>> appParams = new HashMap<>();
        appParams.put("rmdu", parameters);
        remote = new Remote(Utils.basename(origin),
                null, // manufacturer,
                null, // model,
                parameters.get("DeviceType"), // deviceClass,
                parameters.get("Remote.name"), // String remoteName,
                parameters.get("Description"), //String comment,
                parameters.get("Notes"),
                getCommandIndex(),
                appParams,
                parameters.get("Protocol.name"),
                protocolParams);

        remoteSet = new RemoteSet(getCreatingUser(),
                origin, //java.lang.String source,
                (new Date()).toString(), //java.lang.String creationDate,
                Version.appName, //java.lang.String tool,
                Version.version, //java.lang.String toolVersion,
                null, //java.lang.String tool2,
                null, //java.lang.String tool2Version,
                null, //java.lang.String notes,
                remote);
    }

    private static void usage(int exitcode) {
        StringBuilder str = new StringBuilder();
        argumentParser.usage(str);

        (exitcode == IrpUtils.exitSuccess ? System.out : System.err).println(str);
        doExit(exitcode);
    }

    private static void doExit(int exitcode) {
        System.exit(exitcode);
    }

    @Override
    public String getFormatName() {
        return "RemoteMaster";
    }

    private final static class CommandLineArgs {

        @Parameter(names = {"-d", "--debug"}, description = "Debug")
        int debug = 0;
        @Parameter(names = {"-h", "--help", "-?"}, description = "Display help message")
        boolean helpRequested = false;
        @Parameter(names = {"-i", "--inifile"}, description = "Path to protocols.ini")
        String inifile = null;
        @Parameter(names = {"-o", "--outfile"}, description = "Output filename")
        String outputfile = null;
        //@Parameter(names = {"-s", "--schemalocation"}, description = "Create schema location attribute")
        //boolean createSchemaLocation = false;
        @Parameter(names = {"-v", "--version"}, description = "Display version information")
        boolean versionRequested;
        @Parameter(names = {"-x", "--xslt"}, description = "Link to XSLT stylesheet")
        String stylesheetUrl = null;
        @Parameter(description = "[configfile]")
        ArrayList<String> configfile = new ArrayList<>();
    }
    private static JCommander argumentParser;
    private static CommandLineArgs commandLineArgs = new CommandLineArgs();

    /**
     * @param args
     */
    public static void main(String[] args) {
        argumentParser = new JCommander(commandLineArgs);
        argumentParser.setProgramName("Lirc2Xml");

        try {
            argumentParser.parse(args);
        } catch (ParameterException ex) {
            System.err.println(ex.getMessage());
            System.exit(IrpUtils.exitUsageError);
        }

        if (commandLineArgs.helpRequested)
            usage(IrpUtils.exitSuccess);

        if (commandLineArgs.versionRequested) {
            //System.out.println("Lirc2Xml version " + TOOL_VERSION);
            System.out.println("JVM: " + System.getProperty("java.vendor") + " " + System.getProperty("java.version") + " " + System.getProperty("os.name") + "-" + System.getProperty("os.arch"));
            System.exit(IrpUtils.exitSuccess);
        }

        String configFilename = commandLineArgs.configfile.isEmpty() ? "STDIN" : commandLineArgs.configfile.get(0);
        if (commandLineArgs.debug > 0) {
            System.err.println("debug = " + commandLineArgs.debug);
            System.err.println("outputfilename = " + commandLineArgs.outputfile);
            System.err.println("configfile = " + configFilename);
        }

        try {
            ProtocolsIni protocolsIni = commandLineArgs.inifile != null ? new ProtocolsIni(new File(commandLineArgs.inifile)) : null;

            RmduImporter rmdu = new RmduImporter(protocolsIni);
            if (commandLineArgs.configfile.isEmpty())
                rmdu.load();
            else
                rmdu.load(new File(commandLineArgs.configfile.get(0)));
            RemoteSet remoteSet = new RemoteSet(null,
                    configFilename, //String source,
                    rmdu.getRemote());
            Document doc = remoteSet.xmlExportDocument("Rmdu import of " + Utils.basename(configFilename), "xsl", commandLineArgs.stylesheetUrl, true, true, true, true, true);
            XmlUtils.printDOM(new File(commandLineArgs.outputfile), doc);
        } catch (IOException | ParseException | IrpMasterException ex) {
            System.err.println(ex.getMessage());
        }
    }
}
