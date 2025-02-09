/*
Copyright (C) 2013, 2014. 2018 Bengt Martensson.

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

package org.harctoolbox.irscrutinizer;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import java.awt.HeadlessException;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.ConsoleHandler;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import javax.xml.parsers.ParserConfigurationException;
import org.harctoolbox.cmdline.LevelParser;
import org.harctoolbox.guicomponents.GuiUtils;
import org.harctoolbox.harchardware.Utils;
import org.harctoolbox.ircore.IrCoreUtils;
import org.harctoolbox.irp.IrpParseException;
import org.harctoolbox.irp.IrpUtils;
import org.harctoolbox.irscrutinizer.importer.RemoteLocatorImporter;
import org.xml.sax.SAXException;

/**
 * This class decodes command line parameters and fires up the GUI.
 */
@SuppressWarnings("UseOfSystemOutOrSystemErr")
public class IrScrutinizer {

    private final static String backupsuffix = "back";

    private static JCommander argumentParser;
    private static final CommandLineArgs commandLineArgs = new CommandLineArgs();

    private static void usage(int exitcode) {
        StringBuilder str = new StringBuilder(256);
        argumentParser.usage();

        (exitcode == IrpUtils.EXIT_SUCCESS ? System.out : System.err).println(str);
        doExit(exitcode); // placifying FindBugs...
    }

    private static void doExit(int exitcode) {
        System.exit(exitcode);
    }

    /**
     * @param args the command line arguments.
     */
    @SuppressWarnings("AccessingNonPublicFieldOfAnotherObject")
    public static void main(String[] args) {
        argumentParser = new JCommander(commandLineArgs);
        argumentParser.setProgramName(Version.appName);

        try {
            argumentParser.parse(args);
        } catch (ParameterException ex) {
            System.err.println(ex.getMessage());
            usage(IrpUtils.EXIT_USAGE_ERROR);
        }

        setupLoggers();

        if (commandLineArgs.helpRequested)
            usage(IrpUtils.EXIT_SUCCESS);

        if (commandLineArgs.versionRequested) {
            System.out.println(Version.versionString);
            System.out.println("JVM: " + System.getProperty("java.vendor") + " " + System.getProperty("java.version")
                    + " " + System.getProperty("os.name") + "-" + System.getProperty("os.arch"));
            System.out.println();
            System.out.println(Version.licenseString);
            System.exit(IrpUtils.EXIT_SUCCESS);
        }

        if (commandLineArgs.nukeProperties) {
            nukeProperties(true);
            System.exit(IrpUtils.EXIT_SUCCESS);
        }

        if (commandLineArgs.scaling != null)
            System.setProperty("sun.java2d.uiScale", commandLineArgs.scaling);

        if (commandLineArgs.remoteLocatorUrl != null)
            RemoteLocatorImporter.setCatalog(commandLineArgs.remoteLocatorUrl);

        setupRadixPrefixes();

        String applicationHome = Utils.findApplicationHome(commandLineArgs.applicationHome, IrScrutinizer.class, Version.appName);
        guiExecute(applicationHome, commandLineArgs.propertiesFilename, commandLineArgs.verbose, commandLineArgs.arguments);
    }

    public static void setupRadixPrefixes() {
       Map<String, Integer> map = new LinkedHashMap<>(8);
        map.put("0b", 2);
        map.put("%", 2);
        map.put("0q", 4);
        map.put("0", 8);
        map.put("0x", 16);
        IrCoreUtils.setRadixPrefixes(map);
    }

    @SuppressWarnings("AccessingNonPublicFieldOfAnotherObject")
    private static String nukeProperties(boolean verbose) {
        Props properties = new Props(commandLineArgs.propertiesFilename, commandLineArgs.applicationHome);
        String filename = properties.getFilename();
        String newFilename = filename + "." + backupsuffix;
        if (verbose)
            System.out.println("Renaming the properties file " + filename + " to " + newFilename + ".");
        (new File(filename)).deleteOnExit();
        try {
            return Files.copy((new File(filename)).toPath(), (new File(newFilename)).toPath(), StandardCopyOption.REPLACE_EXISTING).toString();
        } catch (IOException ex) {
            System.err.println(ex.getMessage());
            return null;
        }
    }

    // This is a simplified version of org.harctoolbox.cmdlind.CmdLineProgram.setupLoggers()
    private static void setupLoggers() {
        Logger topLevelLogger = Logger.getLogger("");
        Formatter formatter = new SimpleFormatter();
        Handler[] handlers = topLevelLogger.getHandlers();
        for (Handler handler : handlers)
            topLevelLogger.removeHandler(handler);

        Handler handler = new ConsoleHandler();
        handler.setFormatter(formatter);
        topLevelLogger.addHandler(handler);

        handler.setLevel(commandLineArgs.logLevel);
        topLevelLogger.setLevel(commandLineArgs.logLevel);
    }

    private static void guiExecute(final String applicationHome, final String propsfilename,
            final boolean verbose, final List<String> arguments) {
        java.awt.EventQueue.invokeLater(() -> {
            try {
                new GuiMain(applicationHome, propsfilename, verbose, arguments).setVisible(true);
            } catch (HeadlessException ex) {
                System.err.println("This program does not run in headless mode.");
            } catch (IOException /*| URISyntaxException*/ | IrpParseException | RuntimeException ex) {
                GuiUtils.fatal(ex, IrpUtils.EXIT_CONFIG_READ_ERROR, new GuiUtils.EmergencyFixer() {
                    private String backupfile;

                    @Override
                    public void fix() {
                        backupfile = nukeProperties(false);
                    }

                    @Override
                    public String getQuestion() {
                        return "Remove the properites file?";
                    }

                    @Override
                    public String getYesMessage() {
                        return "Renamed the properties file to " + backupfile + ".";
                    }

                    @Override
                    public String getNoMessage() {
                        return null;
                    }
                });
            } catch (ParserConfigurationException | URISyntaxException ex) {
                GuiUtils.fatal(ex, IrpUtils.EXIT_INTERNAL_FAILURE);
            } catch (SAXException ex) {
                GuiUtils.fatal(ex, IrpUtils.EXIT_XML_ERROR);
            }
        });
    }

    private IrScrutinizer() {
    }

    private final static class CommandLineArgs {

        @Parameter(names = {"-h", "--help", "-?"}, description = "Display help message")
        private boolean helpRequested = false;

        @Parameter(names = {"-H", "--home", "--applicationhome", "--apphome"}, description = "Set application home (where files are located)")
        private String applicationHome = null;

        @Parameter(names = {"-l", "--loglevel"}, converter = LevelParser.class,
            description = "Log level { OFF, SEVERE, WARNING, INFO, CONFIG, FINE, FINER, FINEST, ALL }")
        public Level logLevel = Level.WARNING;

        @Parameter(names = {"--nuke-properties"}, description = "Get rid of present properties file")
        private boolean nukeProperties = false;

        @Parameter(names = {"-p", "--properties"}, description = "Pathname of properties file")
        private String propertiesFilename = null;

        @Parameter(names = {"-r", "--remotelocator"}, description = "Set RemoteLocator xml catalog (URL or file name).")
        private String remoteLocatorUrl = null;

        @Parameter(names = {"-s", "--scale", "--scaling"}, description = "Set scaling of the GUI. Accepted values and their semantics depend on the JVM.")
        private String scaling = null;

        @Parameter(names = {"-V", "--version"}, description = "Display version information")
        private boolean versionRequested;

        @Parameter(names = {"-v", "--verbose"}, description = "Have some commands executed verbosely")
        private boolean verbose;

        @Parameter(description = "Arguments...")
        private List<String> arguments = new ArrayList<>(4);
    }
}
