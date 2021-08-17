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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import javax.swing.JFileChooser;
import javax.xml.transform.TransformerException;
import org.harctoolbox.girr.Command;
import org.harctoolbox.girr.GirrException;
import org.harctoolbox.guicomponents.SelectFile;
import org.harctoolbox.ircore.IrCoreException;
import org.harctoolbox.ircore.IrCoreUtils;
import org.harctoolbox.ircore.ThisCannotHappenException;
import org.harctoolbox.irp.IrpException;
import org.harctoolbox.xml.XmlUtils;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

/**
 * This class is a common base class of all the exporters.
 */
public abstract class Exporter {

    private static File lastSaveFile = null;
    private static final String DEFAULT_DATE_FORMAT = "yyyy-MM-dd_HH:mm:ss";
    private static final String DEFAULT_DATE_FORMATFILE = "yyyy-MM-dd_HH-mm-ss";
    private static String dateFormatString = DEFAULT_DATE_FORMAT;
    private static String dateFormatFileString = DEFAULT_DATE_FORMATFILE;
    private static String creatingUser = System.getProperty("user.name");
    private static String encodingName = IrCoreUtils.UTF8_NAME;
    @SuppressWarnings("StaticNonFinalUsedInInitialization")
    private static Charset charset = Charset.forName(encodingName);

    /**
     * @param aDateFormatString the dateFormatString to set
     */
    public static void setDateFormatString(String aDateFormatString) {
        dateFormatString = aDateFormatString;
    }

    /**
     * @param aDateFormatFileString the dateFormatFileString to set
     */
    public static void setDateFormatFileString(String aDateFormatFileString) {
        dateFormatFileString = aDateFormatFileString;
    }

    private static void checkExportDir(File exportDir) throws IOException {
        if (!exportDir.exists()) {
            boolean success = exportDir.mkdirs();
            if (!success)
                throw new IOException("Export directory " + exportDir + " does not exist, attempt to create failed.");
        }
        if (!exportDir.isDirectory() || !exportDir.canWrite())
            throw new IOException("Export directory `" + exportDir + "' is not a writable directory.");
    }
    protected static String getDateString() {
        return (new SimpleDateFormat(dateFormatString)).format(new Date());
    }

    public synchronized static File getLastSaveFileOrCopy() throws IOException {
        if (lastSaveFile == null || !lastSaveFile.getName().endsWith(".girr"))
            return lastSaveFile;

        Path newPath = Paths.get(lastSaveFile.getCanonicalPath() + ".txt");
        Files.copy(Paths.get(lastSaveFile.getCanonicalPath()), newPath, REPLACE_EXISTING);
        return newPath.toFile();
    }

    private synchronized static void setLastSaveFile(File theLastSaveFile) {
        lastSaveFile = theLastSaveFile;
    }

    public static String getCreatingUser() {
        return creatingUser;
    }

    public static void setCreatingUser(String newCreatingUser) {
        creatingUser = newCreatingUser;
    }

    public final static String getEncoding() {
        return encodingName;
    }

    public static Charset getCharset() {
        return charset;
    }

    public static void setEncoding(String newEncoding) {
        charset = Charset.forName(encodingName);
        encodingName = newEncoding;
    }

    public static Document getDocument(DocumentFragment fragment) {
        if (fragment == null)
            return null;

        Document doc = XmlUtils.newDocument();
        Element root = doc.createElement("html");
        doc.appendChild(root);
        Element body = doc.createElement("body");
        root.appendChild(body);
        Element div = doc.createElement("div");
        div.setAttribute("class", "documentation");
        body.appendChild(div);
        div.appendChild(doc.importNode(fragment, true));
        return doc;
    }

    protected static DocumentFragment parseToDocumentFragment(String str) {
        try {
            Document doc = XmlUtils.parseStringToXmlDocument(str, true, true);
            DocumentFragment fragment = doc.createDocumentFragment();
            fragment.appendChild(doc.getDocumentElement());
            return fragment;
        } catch (SAXException ex) {
            throw new ThisCannotHappenException(ex);
        }
    }

    protected Exporter() {
    }

    protected void possiblyMakeExecutable(File file) {
    }

    public String getDateFormatString() {
        return dateFormatString;
    }

    public abstract String[][] getFileExtensions();

    // Dummy
    public abstract String getPreferredFileExtension();

    public abstract String getName();

    public abstract DocumentFragment getDocumentation();

    public Document getDocument() {
        return getDocument(getDocumentation());
    }

    private File selectExportFile(Component parent, File exportDir) {
        File answer = SelectFile.selectFile(parent, "Select file for " + getName() + " export.",
                exportDir.getPath(), true, false, JFileChooser.FILES_ONLY, getFileExtensions());
        if (answer != null && getPreferredFileExtension() != null && ! getPreferredFileExtension().isEmpty())
            answer = new File(IrCoreUtils.addExtensionIfNotPresent(answer.getPath(), getPreferredFileExtension()));
        return answer;
    }

    private File automaticFilename(File exportDir) throws IOException {
        checkExportDir(exportDir);
        String cleanedFormatName = getName().toLowerCase(Locale.US).replaceAll("[^a-z0-9_\\-\\.]", "_");
        String name = cleanedFormatName + "_" + (new SimpleDateFormat(dateFormatFileString)).format(new Date());
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

    public boolean considersRepetitions() {
        return false;
    }

    public void export(Document document, File file, String charsetName) throws FileNotFoundException, UnsupportedEncodingException {
        XmlUtils.printDOM(file, document, charsetName, null);
    }

    public void export(String payload, File file, String charsetName) throws FileNotFoundException, UnsupportedEncodingException {
        try (PrintStream printStream = new PrintStream(file, charsetName)) {
            printStream.println(payload);
        }
    }

    public File export(String payload, boolean automaticFilenames, Component parent, File exportDir, String charsetName) throws IOException {
        File file = exportFilename(automaticFilenames, parent, exportDir);
        export(payload, file, charsetName);
        return file;
    }
//    private void export(Command command, String source, String title, int repeatCount, File file, String charsetName) {
//        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
//    }

    public File export(Command command, String source, String title,
                int noRepeats, boolean automaticFilenames, Component component,
                File exportDir, String charsetName) throws IOException, TransformerException, IrCoreException, IrpException, GirrException {
        File file = exportFilename(automaticFilenames, component, exportDir);
        export(command, source, title, noRepeats, file, charsetName);
        return file;
    }

    protected abstract void export(Command command, String source, String title, int noRepeats, File file, String charsetName)
            throws IOException, TransformerException, IrCoreException, IrpException, GirrException;
}
