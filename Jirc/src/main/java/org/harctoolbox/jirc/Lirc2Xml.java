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

// This file is not derived from LIRC code.

package org.harctoolbox.jirc;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collection;
import org.harctoolbox.IrpMaster.DecodeIR;
import org.harctoolbox.IrpMaster.IrpUtils;
import org.harctoolbox.IrpMaster.XmlUtils;
import org.harctoolbox.girr.RemoteSet;
import org.w3c.dom.Document;

/**
 * This class consists of a command line interface to Jirc.
 * It has only a public static main function, and cannot be instantiated.
 */

final public class Lirc2Xml {

    final static boolean useSignsInRawSequences = true;
    final static String defaultExtension = "girr";
    static final String DEFAULT_EXPORTDIR = ".";

    private static JCommander argumentParser;
    private static CommandLineArgs commandLineArgs = new CommandLineArgs();

    private static String basename(String s) {
        StringBuilder sb = new StringBuilder(s);
        int n = sb.lastIndexOf(File.separator);
        if (n != -1)
            sb.delete(0, n+1);
        n = sb.lastIndexOf(".");
        if (n != -1)
            sb.delete(n, sb.length());
        return sb.toString();
    }

    private static void usage(int exitcode) {
        StringBuilder str = new StringBuilder();
        argumentParser.usage(str);

        (exitcode == IrpUtils.exitSuccess ? System.out : System.err).println(str);
        System.exit(exitcode);
    }

    /**
     * This is the "Lirc2Xml" command line program. Use the --help command line for a short synopsis.
     * <a href="http://www.harctoolbox.org/Jirc.html">Online documentation</a>.
     *
     * @param args Program arguments.
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
            System.out.println(Version.appName + " version " + Version.version);
            System.out.println("JVM: " + System.getProperty("java.vendor") + " " + System.getProperty("java.version") + " " + System.getProperty("os.name") + "-" + System.getProperty("os.arch"));
            System.out.println("DecodeIR version " + DecodeIR.getVersion());
            System.out.println();
            System.out.println(Version.licenseString);
            System.exit(IrpUtils.exitSuccess);
        }

        if (!commandLineArgs.generateCcf && !commandLineArgs.generateRaw && !commandLineArgs.generateParameters) {
            System.out.println("Warning: Neither parameters (\"-p\"), ccf/hex (\"-c\"), nor raw (\"-R\") requested.");
            System.err.println("Output will be generated per your request, but it will likely be useless.");
        }

        String configFilename = commandLineArgs.configfile.isEmpty() ? null : commandLineArgs.configfile.get(0);
        if (commandLineArgs.debug > 0) {
            System.err.println("debug = " + commandLineArgs.debug);
            System.err.println("outputfilename = " + commandLineArgs.outputfile);
            System.err.println("configfile = " + configFilename);
            System.err.println("remote = " + commandLineArgs.remote);
        }

        try {
            Collection<IrRemote> remotes;
            if (configFilename == null) {
                if (commandLineArgs.debug > 0)
                    System.err.println("Reading stdin.");

                remotes = ConfigFile.readConfig(new InputStreamReader(System.in, commandLineArgs.inputEncoding), "<stdin>", commandLineArgs.lircCode);
            } else {
                try {
                    URL url = new URL(configFilename);
                    if (commandLineArgs.debug > 0)
                        System.err.println("Looks like an url.");
                    URLConnection urlConnection = url.openConnection();
                    InputStream inputStream = urlConnection.getInputStream();
                    remotes = ConfigFile.readConfig(new InputStreamReader(inputStream, commandLineArgs.inputEncoding),
                            url.toString(), commandLineArgs.lircCode);
                } catch (MalformedURLException ex) {
                    if (commandLineArgs.debug > 0)
                        System.err.println("Does not look like an url, hope it is a file.");
                    remotes = ConfigFile.readConfig(new File(configFilename),
                            commandLineArgs.inputEncoding, commandLineArgs.lircCode);
                }
            }
            if (commandLineArgs.remote != null) {
                IrRemote selected = null;
                for (IrRemote irRemote : remotes) {
                    if (irRemote.getName().equals(commandLineArgs.remote)) {
                        selected = irRemote;
                        break;
                    }
                }
                if (selected != null) {
                    remotes = new ArrayList(1);
                    remotes.add(selected);
                } else {
                    System.err.println("No such remote " + commandLineArgs.remote + " found, exiting.");
                    System.exit(IrpUtils.exitFatalProgramFailure);
                }
            }

            RemoteSet remoteSet = IrRemote.newRemoteSet(remotes, configFilename,
                    System.getProperty("user.name", "unknown"), /*alternatingSigns=*/ true, commandLineArgs.debug);
            if (remoteSet == null) {
                System.err.println("No remotes in found in file " + configFilename + ", no output generated.");
                System.exit(IrpUtils.exitFatalProgramFailure);
            }
            Document doc = remoteSet.xmlExportDocument("Lirc2XML export from " + configFilename,
                    "xsl", commandLineArgs.stylesheetUrl,
                    commandLineArgs.fatRaw,
                    commandLineArgs.createSchemaLocation, commandLineArgs.generateRaw,
                    commandLineArgs.generateCcf, commandLineArgs.generateParameters);

            OutputStream xmlStream;
            String outFilename = null;
            if (commandLineArgs.outputfile == null) {
                String outputDirName = System.getenv("LIRC_XML_EXPORTDIR") != null
                        ? System.getenv("LIRC_XML_EXPORTDIR") : DEFAULT_EXPORTDIR;
                if (configFilename == null)
                    configFilename = "STDIN";

                if (configFilename.endsWith(File.separator))
                    configFilename = configFilename.substring(0, configFilename.length() - 1);
                File f = new File(outputDirName, basename(configFilename) + "." + defaultExtension);

                outFilename = f.getCanonicalPath();
                xmlStream = new FileOutputStream(f);
            } else if (commandLineArgs.outputfile.equals("-")) {
                xmlStream = System.out;
                outFilename = "<stdout>";
            } else {
                xmlStream = new FileOutputStream(commandLineArgs.outputfile);
                outFilename = commandLineArgs.outputfile;
            }

            XmlUtils.printDOM(xmlStream, doc, commandLineArgs.encoding);
            System.err.println(remotes.size() + " remote(s) written to XML export file " + outFilename + ".");
            System.exit(IrpUtils.exitSuccess);
        } catch (UnsupportedEncodingException ex) {
            System.err.println("Unsupported encoding: " + ex.getMessage());
            System.exit(IrpUtils.exitUsageError);
        } catch (FileNotFoundException ex) {
            System.err.println(ex.getMessage() + " could not be found.");
            System.exit(IrpUtils.exitConfigReadError);
        } catch (IOException ex) {
            System.err.println(ex.getMessage());
            System.exit(IrpUtils.exitIoError);
        }
    }

    private Lirc2Xml() {
    }

    private final static class CommandLineArgs {

        @Parameter(names = {"-c", "--ccf", "--hex", "--pronto"}, description = "Generate the CCF (\"Hex\", \"Pronto\") form of the signals")
                boolean generateCcf = false;
        @Parameter(names = {"-d", "--debug"}, description = "Debug. Not really useful...")
                int debug = 0;
        @Parameter(names = {"-e", "--encoding", "--outputencoding"}, description = "Character encoding of the generated XML file")
                String encoding = "UTF-8";
        @Parameter(names = {"-h", "--help", "-?"}, description = "Display help message")
                boolean helpRequested = false;
        @Parameter(names = {"-f", "--fatraw"}, description = "Use the fat format for raw signals")
                boolean fatRaw = false;
        @Parameter(names = {"-i", "--inputencoding"}, description = "Character encoding used for reading input")
                String inputEncoding = ConfigFile.defaultCharsetName;
        @Parameter(names = {"-l", "--lirccode"}, description = "Also accept lirc files without timing info, so-called Lirccode remotes.")
                boolean lircCode = false;
        @Parameter(names = {"-o", "--outfile"}, description = "Output filename")
                String outputfile = null;
        @Parameter(names = {"-p", "--parameters"}, description = "Generate the protocol name and parameters (if possible) for the signals")
                boolean generateParameters = false;
        @Parameter(names = {"-s", "--schemalocation"}, description = "Create schema location attribute")
                boolean createSchemaLocation = false;
        @Parameter(names = {"-r", "--remote"}, description = "Name of the remote to include in the export (will export all if left empty)")
                String remote = null;
        @Parameter(names = {"-R", "--raw"}, description = "Generate the raw form of the signals")
                boolean generateRaw = false;
        @Parameter(names = {"-v", "--version"}, description = "Display version information")
                boolean versionRequested;
        @Parameter(names = {"-x", "--xslt"}, description = "Link to XSLT stylesheet")
                String stylesheetUrl = null;
        @Parameter(description = "[configfile]")
                ArrayList<String> configfile = new ArrayList<>();
    }
}
