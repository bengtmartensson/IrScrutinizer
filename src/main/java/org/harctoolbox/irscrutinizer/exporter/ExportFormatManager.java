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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Set;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.xml.parsers.ParserConfigurationException;
import org.harctoolbox.guicomponents.GuiUtils;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.xml.sax.SAXException;

/**
 * This class does something interesting and useful. Or not...
 */
public class ExportFormatManager {

    private final LinkedHashMap<String, IExporterFactory> exportFormats;
    private final IExportFormatSelector exportFormatSelector;
    private JMenu menu;
    private ButtonGroup buttonGroup;
    private IExporterFactory selected;

    public ExportFormatManager(IExportFormatSelector exportFormatSelector) {
        this.exportFormatSelector = exportFormatSelector;
        selected = null;
        exportFormats = new LinkedHashMap<>(32);
    }

    public void addDynamicFormats(GuiUtils guiUtils, File file) throws ParserConfigurationException, SAXException, IOException {
        exportFormats.putAll(DynamicRemoteSetExportFormat.parseExportFormats(guiUtils, file));
    }

    public void add(String name, IExporterFactory factory) {
         exportFormats.put(name, factory);
    }

    public IExporterFactory get(String name) {
        return exportFormats.get(name);
    }

    public IExporterFactory getSelected() {
        return selected;
    }

    private void createMenu(String selection) {
        menu = new JMenu();
        menu.setText("Export formats");
        menu.setToolTipText("Allows direct selection of export format");
        buttonGroup = new ButtonGroup();
        for (String formatName : toArray()) {
            final String name = formatName;
            JMenuItem menuItem = new JCheckBoxMenuItem(name);
            menuItem.setSelected(name.equals(selection));
            menuItem.addActionListener((java.awt.event.ActionEvent evt) -> {
                exportFormatSelector.select(name);
            });

            buttonGroup.add(menuItem);
            menu.add(menuItem);
        }
    }

    public DocumentFragment getDocumentation() {
        return getDocumentation(selected);
    }

    public DocumentFragment getDocumentation(String formatName) {
        return getDocumentation(get(formatName));
    }

    public DocumentFragment getDocumentation(IExporterFactory format) {
        Exporter exporter = format.newExporter();
        return exporter.getDocumentation();
    }

    public Document getDocument() {
        return getDocument(selected);
    }

    public Document getDocument(IExporterFactory factory) {
        Exporter exporter = factory.newExporter();
        return exporter.getDocument();
    }

    public String getFormatName(IExporterFactory format) {
        return format.newExporter().getName();
    }

    public String getFormatName() {
        return getFormatName(selected);
    }

    public void setMenuSelection(String format) {
        if (menu == null)
            createMenu(format);

        for (int i = 0; i < menu.getItemCount(); i++) {
            JMenuItem item = menu.getItem(i);
            item.setSelected(item.getText().equals(format));
        }
        selected = exportFormats.get(format);
    }

    public JMenu getMenu(String format) {
        createMenu(format);
        return menu;
    }

    public Set<String> getNames() {
        return exportFormats.keySet();
    }

    public String[] toArray() {
        ArrayList<String> list = new ArrayList<>(exportFormats.keySet());
        Collections.sort(list, String.CASE_INSENSITIVE_ORDER);
        return list.toArray(new String[0]);
    }

    @SuppressWarnings("PublicInnerClass")
    public interface IExportFormatSelector {
        public void select(String name);
    }
}
