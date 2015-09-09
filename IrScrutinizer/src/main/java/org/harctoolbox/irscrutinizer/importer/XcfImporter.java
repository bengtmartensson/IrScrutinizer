/*
Copyright (C) 2009, 2013, 2014 Bengt Martensson.

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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.text.ParseException;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.harctoolbox.IrpMaster.IrpMasterException;
import org.harctoolbox.IrpMaster.XmlUtils;
import org.harctoolbox.girr.Command;
import org.harctoolbox.girr.Remote;
import org.harctoolbox.girr.RemoteSet;
import org.harctoolbox.irscrutinizer.Version;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Class for importing Pronto CCF files of the first generation.
 */
public class XcfImporter extends RemoteSetImporter implements IReaderImporter {

    private static final String xcfXmlFileName = "ConfigEdit.xml";
    private boolean translateProntoFont = true;

    private HashMap<String, String> nameIndex;
    //HashMap<String, Element> itemIndex;
    private HashMap<String, Element> pageIndex;
    private HashMap<String, Element> actionListIndex;
    private HashMap<String, Element> moduleIndex;
    private HashMap<String, Element> actionIndex;

    private int learnedIrCodeIndex;

    /**
     * @param translateProntoFont the translateProntoFont to set
     */
    public void setTranslateProntoFont(boolean translateProntoFont) {
        this.translateProntoFont = translateProntoFont;
    }

    private static Document openConfig(File filename) throws SAXException, IOException {
        ZipFile zipFile = null;
        InputStream stream = null;
        Document doc = null;
        try {
            zipFile = new ZipFile(filename);
            ZipEntry entry = zipFile.getEntry(xcfXmlFileName);
            if (entry == null)
                entry = zipFile.getEntry("/" + xcfXmlFileName);
            stream = zipFile.getInputStream(entry);
            doc = XmlUtils.openXmlStream(stream, null, false, false);
        } finally {
            if (zipFile != null)
                zipFile.close();
        }
        return doc;
    }

    public XcfImporter() {
        super();
    }

    @Override
    public void load(File filename, String origin) throws IOException, ParseException {
        prepareLoad(origin);
        try {
            load(openConfig(filename));
        } catch (SAXException ex) {
            throw new IOException(ex.getMessage());
        }
    }

    private String transmogrify(String s) {
        return s.isEmpty() ? ("empty_" + learnedIrCodeIndex++)
                : (s.equals("Learned IR Code") || s.equals("Learnt IR Code")) ? (s + "_" + learnedIrCodeIndex++)
                : s;
    }

    @SuppressWarnings("empty-statement")
    private void load(Document doc) throws ParseException {
        learnedIrCodeIndex = 1;
        nameIndex = null;
        pageIndex = null;
        actionListIndex = null;
        moduleIndex = null;
        actionIndex = null;

        // First read all the STRINGs into an index
        Element root = doc.getDocumentElement();
        NodeList topThings = root.getChildNodes();
        for (int i = 0; i < topThings.getLength(); i++) {
            if (topThings.item(i).getNodeType() != Node.ELEMENT_NODE)
                continue;
            Element element = (Element) topThings.item(i);
            if (element.getTagName().equals("Strings"))
                setupNamesIndex(element);
        }

        for (int i = 0; i < topThings.getLength(); i++) {
            if (topThings.item(i).getNodeType() != Node.ELEMENT_NODE)
                continue;

            Element element = (Element) topThings.item(i);
            switch (element.getTagName()) {
                case "Actions":
                    setupActionIndex(element);
                    break;
                case "Pages":
                    pageIndex = mkIndex(element, "PAGE");
                    break;
                case "ActionLists":
                    actionListIndex = mkIndex(element, "ACTIONLIST");
                    break;
                case "Modules":
                    moduleIndex = mkIndex(element, "MODULE");
                    break;
                    //else if (element.getTagName().equals("Strings"))
                    //    setupNamesIndex(element);
                    //else if (element.getTagName().equals("Items"))
                    //    itemIndex = mkIndex(element, "ITEM");
                default:
                    break;
            }
        }

        if (moduleIndex == null)
            throw new ParseException("No Modules element present.", -1);


        HashMap<String,Remote> remotes = new HashMap<>();

        for (Element module : moduleIndex.values()) {
            Remote remote = loadModule(module);
            if (!remote.getCommands().isEmpty())
                remotes.put(remote.getName(), remote);
        }
        remoteSet = new RemoteSet(getCreatingUser(), origin, //java.lang.String source,
                (new Date()).toString(), //creationDate,
                Version.appName, //java.lang.String tool,
                Version.version, //java.lang.String toolVersion,
                null, //java.lang.String tool2,
                null, //java.lang.String tool2Version,
                null, //java.lang.String notes,
                remotes);
    }

    private Remote loadModule(Element module) {
        //String id = module.getAttribute("id");
        NodeList names = module.getElementsByTagName("Name");
        String nameId = ((Element) names.item(0)).getAttribute("id");
        String name = nameIndex.get(nameId);
        //System.out.println(id + "\t" + nameId + "\t" + name);
        HashMap<String,Command> cmds = new HashMap<>();
        NodeList firstPages = module.getElementsByTagName("FirstPage");
        Element page = firstPages.getLength() > 0 ? pageIndex.get(((Element) firstPages.item(0)).getAttribute("id")) : null;
        while (page != null) {
            //System.out.print(page.getAttribute("id") + " ");
            cmds.putAll(loadPage(page));
            NodeList nextpages = page.getElementsByTagName("Next");
            page = nextpages.getLength() > 0 ? pageIndex.get(((Element) nextpages.item(0)).getAttribute("id")) : null;
        }
        Remote remote = new Remote(name,
                null, //java.lang.String manufacturer,
                null, //java.lang.String model,
                null, //java.lang.String deviceClass,
                null, //java.lang.String remoteName,
                null, //java.lang.String comment,
                null, //java.lang.String notes,
                cmds,
                null //java.util.HashMap<java.lang.String,java.util.HashMap<java.lang.String,java.lang.String>> applicationParameters)
                );
        return remote;
    }

    private HashMap<String,Command> loadPage(Element page) {
        HashMap<String,Command> cmds = new HashMap<>();
        NodeList items = page.getElementsByTagName("Item");
        for (int i = 0; i < items.getLength(); i++) {
            Element item = (Element) items.item(i);
            Command command = loadItem(item);
            if (command != null)
                cmds.put(command.getName(), command);
        }
        return cmds;
    }

    private Command loadItem(Element item) {
        Element actionList = actionListIndex.get(item.getAttribute("id"));
        NodeList actions = actionList.getElementsByTagName("Action");
        if (actions.getLength() == 0)
            return null;

        Element action = actionIndex.get(((Element) actions.item(0)).getAttribute("id"));
        String ccf = actionCodeCcf(action);
        if (ccf == null || ccf.isEmpty())
            return null;

        //String codeName = actionCodeName(action);
        try {
            return new Command(actionCodeName(action), null /*comment*/, ccf, isGenerateRaw(), isInvokeDecodeIr());
            //System.out.print(codeName);
        } catch (IrpMasterException ex) {
            return null;
        }
    }

    private String actionCodeCcf(Element action) {
        NodeList nl = action.getElementsByTagName("BASICIRCODE");
        if (nl.getLength() == 0)
            return null;

        nl = ((Element) nl.item(0)).getElementsByTagName("Code");
        if (nl.getLength() == 0)
            return null;

        String ccf = ((Element)nl.item(0)).getTextContent();
        return ccf;
    }

    private String actionCodeName(Element action) {
        NodeList nl = action.getElementsByTagName("BASICIRCODE");
        if (nl.getLength() == 0)
            return null;

        nl = ((Element) nl.item(0)).getElementsByTagName("Name");
        if (nl.getLength() == 0)
            return null;

        return nameIndex.get(((Element)nl.item(0)).getAttribute("id"));
    }

    private static HashMap<String, Element> mkIndex(Element element, String tagId) {
        HashMap<String, Element> index = new LinkedHashMap<>();
        NodeList things = element.getElementsByTagName(tagId);
        int length = things.getLength();
        for (int i = 0; i < length; i++) {
            Element thing = (Element) things.item(i);
            index.put(thing.getAttribute("id"), thing);
        }
        return index;
    }

    private void setupNamesIndex(Element element) {
        nameIndex = new HashMap<>();
        NodeList nl = element.getElementsByTagName("STRING");
        for (int i = 0; i < nl.getLength(); i++) {
            Element e = (Element) nl.item(i);
            NodeList vals = e.getElementsByTagName("Value");
            if (vals.getLength() > 0) {
                nameIndex.put(e.getAttribute("id"), transmogrify(vals.item(0).getTextContent()));
            }
        }
    }

    private void setupActionIndex(Element element) {
        actionIndex = mkIndex(element, "ACTION");
        for (Element action : actionIndex.values()) {
            NodeList basicIrCodes = action.getElementsByTagName("BASICIRCODE");
            if (basicIrCodes.getLength() == 0)
                continue;

            Element basicIrCode = (Element) basicIrCodes.item(0);
            String name = nameIndex.get(((Element) basicIrCode.getElementsByTagName("Name").item(0)).getAttribute("id"));
            Element codeElement = (Element) basicIrCode.getElementsByTagName("Code").item(0);
            String ccf = codeElement.getTextContent();
            if (ccf.isEmpty())
                continue;

            //buttons.add(new ProntoIrCode(codeElement.getTextContent(), name, null, translateProntoFont));
            try {

                //RawIrSignal raw = new RawIrSignal(Pronto.ccfSignal(codeElement.getTextContent()), name, null, invokeAnalyzer);
                Command raw = new Command(translateProntoFont ? ProntoIrCode.translateProntoFont(name) : name, null /* comment */,
                        ccf, isGenerateCcf(), isInvokeDecodeIr());
                //commands.add(raw);
                //commandIndex.put(raw.getName(), raw);
                addCommand(raw);
            } catch (IrpMasterException ex) {
                System.err.println(ex.getMessage());
            }
        }
    }

    public static RemoteSet importXcf(String filename) throws IOException, SAXException, ParseException, IrpMasterException {
        XcfImporter importer = new XcfImporter();
        importer.load(new File(filename));
        return importer.getRemoteSet();
    }

    @Override
    public boolean canImportDirectories() {
        return false;
    }

    @Override
    public String[][] getFileExtensions() {
        return new String[][]{ new String[]{ "Pronto professional files (*.xcf)", "xcf" }};
    }

    @Override
    public void load(Reader reader, String originName) throws IOException, FileNotFoundException, ParseException {
        dumbLoad(reader, originName);
    }

    @Override
    public String getFormatName() {
        return "Pronto Professional";
    }

    public static void main(String args[]) {
        try {
            RemoteSet buttons = importXcf(args[0]);
            for (Remote button : buttons.getRemotes()) {
                System.out.println(button.toString());
            }
        } catch (SAXException ex) {
            System.err.println(ex.getMessage());
        } catch (IOException | IrpMasterException | ParseException ex) {
            System.err.println(ex.getMessage());
        }
    }
}
