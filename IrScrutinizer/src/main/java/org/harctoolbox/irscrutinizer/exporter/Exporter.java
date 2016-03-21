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

package org.harctoolbox.irscrutinizer.exporter;

import java.awt.Component;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import javax.swing.JFileChooser;
import org.harctoolbox.guicomponents.SelectFile;
import org.harctoolbox.irscrutinizer.Utils;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * This class is a common base class of all the exporters.
 */
public abstract class Exporter {
    /**
     * @param aDateFormatString the dateFormatString to set
     */
    public static void setDateFormatString(String aDateFormatString) {
        dateFormatString = aDateFormatString;
    }

    public String getDateFormatString() {
        return dateFormatString;
    }

    /**
     * @param aDateFormatFileString the dateFormatFileString to set
     */
    public static void setDateFormatFileString(String aDateFormatFileString) {
        dateFormatFileString = aDateFormatFileString;
    }

    private static File lastSaveFile = null;
    private static final String defaultDateFormatString = "yyyy-MM-dd_HH:mm:ss";
    private static final String defaultDateFormatFileString = "yyyy-MM-dd_HH-mm-ss";
    private static String dateFormatString = defaultDateFormatString;
    private static String dateFormatFileString = defaultDateFormatFileString;

    private final String name;
    private final String[][] fileExtensions;
    private final String preferredFileExtension;
    private final String documentation;
    private final URL url;
    private final List<Option> options;
    //protected JPanel panel;
    private final boolean simpleSequence;
    private final boolean binary;

    /**
     * @return the documentation
     */
    public String getDocumentation() {
        return documentation;
    }

    /**
     * @return the url
     */
    public URL getUrl() {
        return url;
    }

    /**
     * @return the options
     */
    public List<Option> getOptions() {
        return options;
    }

    /**
     * @return the panel
     */
    //public JPanel getPanel() {
    //    return panel;
    //}

    /**
     * @return the simpleSequence
     */
    public boolean isSimpleSequence() {
        return simpleSequence;
    }

    /**
     * @return the binary
     */
    public boolean isBinary() {
        return binary;
    }

    /**
     * @return the fileExtensions
     */
    public String[][] getFileExtensions() {
        return fileExtensions;
    }

    /**
     * @return the preferredFileExtension
     */
    public String getPreferredFileExtension() {
        return preferredFileExtension;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    static abstract class Option {
        private final String name;
        private final String displayName;
        //private final JComponent component;
        private final String documentation;

        protected Option(String name, String displayName, /*JComponent component,*/ String documentation) {
            this.name = name.trim();
            this.displayName = (displayName != null && !displayName.trim().isEmpty()) ? displayName.trim() : this.name;
            //this.component = component;
            this.documentation = documentation.trim();
        }

        //Option(String name) {
        //    this(name, name, null, null);
        //}

        /**
         * @return the name
         */
        public String getName() {
            return name;
        }

        /**
         * @return the component
         */
        //public JComponent getComponent() {
        //    return component;
        //}

        /**
         * @return the documentation
         */
        public String getDocumentation() {
            return documentation;
        }

        /**
         * @return the displayName
         */
        public String getDisplayName() {
            return displayName;
        }

        public abstract String getValue();

        public static Option parseOption(Element element) {
            NodeList children = element.getChildNodes();
            for (int i = 0; i < children.getLength(); i++) {
                Node node = children.item(i);
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    Element el = (Element) node;
                    return el.getTagName().equals("text")       ? new TextOption(element)
                            : el.getTagName().equals("boolean") ? new BooleanOption(element)
                            : el.getTagName().equals("selection")  ? new SelectOption(element)
                            : null;
                }
            }
            throw new UnsupportedOperationException();
        }

/*
        private static JComponent parseComponent(Element element) {
            switch (element.getTagName()) {
                case "combobox":
                    String[] values = parseValues(element);
                    return new JComboBox<>(values);
                default:
                    return null;
                    //throw new ParseException("Unknown component: " + element.getTagName(), 0);
                    //break;
            }
            //return null;
        }

        private static JComponent parseComponent(NodeList nodeList) {
            for (int i = 0; i < nodeList.getLength(); i++)
                if (nodeList.item(i).getNodeType() == Node.ELEMENT_NODE)
                    return parseComponent((Element) nodeList.item(i));

            return null;
        }

        private static String[] parseValues(Element element) {
            NodeList nl = element.getElementsByTagName("value");
            String[] answer = new String[nl.getLength()];
            for (int i = 0; i < nl.getLength(); i++)
                answer[i] = ((Element) nl.item(i)).getTextContent().trim();
            return answer;
        }*/

        public static List<Option> parseOptions(Element el) {
            return parseOption(el.getElementsByTagName("option"));
        }

        public static List<Option> parseOption(NodeList nl) {
            List<Option> opts = new ArrayList<>(nl.getLength());
            for (int i = 0; i < nl.getLength(); i++) {
                Option opt = parseOption((Element) nl.item(i));
                opts.add(opt);
            }
            return opts;
        }
    }

    static class SelectOption extends Option {

        private List<String> values;
        private int selectedIndex;

        SelectOption(Element el) {
            super(el.getAttribute("name"), el.getAttribute("displayName"), el.getAttribute("title"));
            NodeList nodeList = el.getElementsByTagName("value");
            values = new ArrayList<>(nodeList.getLength());
            selectedIndex = 0;
            for (int i = 0; i < nodeList.getLength(); i++) {
                Element e = (Element) nodeList.item(i);
                values.add(e.getAttribute("text").trim());
                if (Boolean.parseBoolean(e.getAttribute("default")))
                    selectedIndex = i;
            }
        }

        SelectOption(String name, String displayName, String documentation,
                List<String> values, int defaultIndex) {
            super(name, displayName, documentation);
            this.values = values;
            selectedIndex = defaultIndex;
        }

        @Override
        public String getValue() {
            return values.get(selectedIndex);
        }
    }

    static class TextOption extends Option {

        private String value;

        TextOption(String name, String displayName, /*JComponent component,*/ String documentation,
                String defaultValue) {
            super(name, displayName, documentation);
            this.value = defaultValue.trim();
        }

        TextOption(Element el) {
            this(el.getAttribute("name"), el.getAttribute("displayName"), el.getAttribute("title"),
            ((Element) el.getElementsByTagName("text").item(0)).getAttribute("default"));
        }


        @Override
        public String getValue() {
            return value;
        }
    }

    static class BooleanOption extends Option {

        private boolean value;

        BooleanOption(String name, String displayName, /*JComponent component,*/ String documentation,
                boolean defaultValue) {
            super(name, displayName, documentation);
            this.value = defaultValue;
        }

        BooleanOption(Element el) {
            this(el.getAttribute("name"), el.getAttribute("displayName"), el.getAttribute("title"),
                    Boolean.parseBoolean(el.getElementsByTagName("boolean").item(0).getTextContent().trim()));
        }

        @Override
        public String getValue() {
            return Boolean.toString(value);
        }
    }



    protected Exporter(String name, String[][] fileExtensions, String preferredFileExtension, String documentation, URL url,
            List<Option> options, boolean simpleSequence,
            boolean binary) {
        this.name = name;
        this.fileExtensions = fileExtensions;
        this.preferredFileExtension = preferredFileExtension;
        this.documentation = documentation;
        this.url = null;
        this.options = options;
        //this.panel = null;//panel;
        this.simpleSequence = simpleSequence;
        this.binary = binary;
    }

    /*protected Exporter(String formatName, String[][] fileExtensions, String preferredFileExtension, String documentation, URL url,
            HashMap<String, Option> options, JPanel panel, boolean simpleSequence, boolean binary) {
        this(formatName, fileExtensions, preferredFileExtension, documentation, url,
            options, panel, simpleSequence, binary, System.getProperty("user.name", "unknown"));
    }*/
    //public abstract String[][] getFileExtensions();

    // Dummy
    //public abstract String getPreferredFileExtension();

    private static void checkExportDir(File exportDir) throws IOException {
        if (!exportDir.exists()) {
            boolean success = exportDir.mkdirs();
            if (!success)
                throw new IOException("Export directory " + exportDir + " does not exist, attempt to create failed.");
        }
        if (!exportDir.isDirectory() || !exportDir.canWrite())
            throw new IOException("Export directory `" + exportDir + "' is not a writable directory.");
    }

    private File selectExportFile(Component parent, File exportDir) {
        File answer = SelectFile.selectFile(parent, "Select file for " + getName() + " export.",
                exportDir.getPath(), true, false, JFileChooser.FILES_ONLY, getFileExtensions());
        if (answer != null && getPreferredFileExtension() != null && ! getPreferredFileExtension().isEmpty())
            answer = new File(Utils.addExtensionIfNotPresent(answer.getPath(), getPreferredFileExtension()));
        return answer;
    }

    private File automaticFilename(File exportDir) throws IOException {
        checkExportDir(exportDir);
        String name = getName().toLowerCase(Locale.US) + "_" + (new SimpleDateFormat(dateFormatFileString)).format(new Date());
        if (getPreferredFileExtension() != null)
            name += "." + getPreferredFileExtension();
        return new File(exportDir, name);
    }

    public File exportFilename(boolean automatic, Component parent, File exportDir) throws IOException {
        File file =  automatic ? automaticFilename(exportDir) : selectExportFile(parent, exportDir);
        if (file != null)
            setLastSaveFile(file);
        return file;
    }

    protected static String getDateString() {
        return (new SimpleDateFormat(dateFormatString)).format(new Date());
    }

    public synchronized static File getLastSaveFile() {
        return lastSaveFile;
    }

    private synchronized static void setLastSaveFile(File theLastSaveFile) {
        lastSaveFile = theLastSaveFile;
    }
}
