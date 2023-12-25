/*
Copyright (C) 2021 Bengt Martensson.

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

package org.harctoolbox.irscrutinizer.exporter;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.validation.Schema;
import org.harctoolbox.guicomponents.GuiUtils;
import org.harctoolbox.ircore.InvalidArgumentException;
import org.harctoolbox.irp.IrpUtils;
import org.harctoolbox.xml.XmlUtils;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Main routine to invoke and validate dynamic export formats.
 */
public class DynamicRemoteSetExportFormatMain {

    private static JCommander argumentParser;
    private static final CommandLineArgs commandLineArgs = new CommandLineArgs();

    private static boolean isFat(Document doc) {
        NodeList nodeList = doc.getElementsByTagName("flash");
        return (nodeList.getLength() > 0);
    }

    private static void usage(int exitcode) {
        argumentParser.usage();
        doExit(exitcode); // placifying FindBugs...
    }

    private static void doExit(int exitcode) {
        System.exit(exitcode);
    }

    @SuppressWarnings({"UseOfSystemOutOrSystemErr", "AccessingNonPublicFieldOfAnotherObject"})
    public static void main(String[] args) {
        argumentParser = new JCommander(commandLineArgs);
        argumentParser.setProgramName("DynamicRemoteSetExportFormatVersion");

        try {
            argumentParser.parse(args);
        } catch (ParameterException ex) {
            System.err.println(ex.getMessage());
            usage(IrpUtils.EXIT_USAGE_ERROR);
        }

        if (commandLineArgs.helpRequested)
            usage(IrpUtils.EXIT_SUCCESS);

        File configFile = new File(commandLineArgs.exportFormatsPathname);
        String formatName = commandLineArgs.formatName;

        try {
            Schema schema = commandLineArgs.schemaUrl != null ? XmlUtils.readSchema(new URL(commandLineArgs.schemaUrl)) : null;
            Map<String, IExporterFactory> exportFormats = DynamicRemoteSetExportFormat.parseExportFormats(null, configFile, schema);
            //Schema schema = (SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI)).newSchema(new URL("http://www.harctoolbox.org/schemas/exportformats.xsd"));

            if (formatName.equals("?")) {
                Set<String> names = exportFormats.keySet();
                names.forEach(name -> {
                    System.out.println(name);
                });
                System.exit(IrpUtils.EXIT_SUCCESS);
            }

            IExporterFactory format = exportFormats.get(formatName);
            if (format == null) {
                System.err.println("No such export format ``" + formatName + "''");
                System.exit(IrpUtils.EXIT_SEMANTIC_USAGE_ERROR);
            }

            Exporter exporter = format.newExporter();

            if (commandLineArgs.documentation) {
                File htmlFileName;
                Document docu = exporter.getDocument();
                htmlFileName = File.createTempFile("formatdocu", ".html");
                try (OutputStream ostr = new FileOutputStream(htmlFileName)) {
                    XmlUtils.printHtmlDOM(ostr, docu, commandLineArgs.encoding);
                }
                URI uri = new URI("file://" + htmlFileName.getCanonicalPath());
                GuiUtils.browseStatic(uri);
            } else {
                File girrFile = new File(commandLineArgs.parameters.get(0));

                Document doc = XmlUtils.openXmlFile(girrFile);
                if (!isFat(doc))
                    throw new InvalidArgumentException("Not a fat Girr file");

                //String outFileName = commandLineArgs.outputFile != null
                //        ? commandLineArgs.outputFile : "-";
                        //: girrFile.getCanonicalPath().replaceAll("\\.girr$", "." + exporter.getPreferredFileExtension());
                if (DynamicRemoteSetExportFormat.class.isInstance(exporter))
                    ((DynamicRemoteSetExportFormat) exporter).export(doc, commandLineArgs.outputFile, commandLineArgs.encoding);
                else
                    ((DynamicCommandExportFormat) exporter).export(doc, commandLineArgs.outputFile, commandLineArgs.encoding, commandLineArgs.nrTimes);

                if (! commandLineArgs.outputFile.equals("-"))
                    System.err.println("Created " + commandLineArgs.outputFile);
                System.exit(IrpUtils.EXIT_SUCCESS);
            }
        } catch (ParserConfigurationException | SAXException | IOException | TransformerException | InvalidArgumentException | URISyntaxException ex) {
            System.err.println(ex + ": " + ex.getMessage());
        }
    }

    private final static class CommandLineArgs {

        @Parameter(names = {"-c", "--configuration"}, required = true, description = "Pathname of exportformats file/directory")
        private String exportFormatsPathname = null;

        @Parameter(names = {"-d", "--documentation"}, description = "Generate documentation of the protocol.")
        private boolean documentation = false;

        @Parameter(names = {"-e", "--encoding"}, description = "Encoding of the generated document")
        private String encoding = "ISO-8859-1";

        @Parameter(names = {"-f", "--format"}, required = true, description = "Name of the desired export format, or \"?\" for a list.")
        private String formatName = null;

        @Parameter(names = {"-h", "--help", "-?"}, description = "Display help message")
        private boolean helpRequested = false;

        @Parameter(names = {"-n", "--nrtimes"}, description = "Number of times to repeat a signal (only for CommandExport)")
        private int nrTimes = 1;

        @Parameter(names = {"-o", "--outputfile"}, description = "Name of output file")
        private String outputFile = "-";

        @Parameter(names = {"-s", "--schema"}, description = "Url for a schema to be validated against.")
        private String schemaUrl = null;

        @Parameter(description = "Girr file to be transformed")
        @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
        private List<String> parameters = new ArrayList<>(4);
    }
}
