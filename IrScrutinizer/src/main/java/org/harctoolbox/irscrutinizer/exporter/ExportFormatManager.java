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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map.Entry;
import java.util.Set;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.xml.parsers.ParserConfigurationException;
import org.harctoolbox.guicomponents.GuiUtils;
import org.xml.sax.SAXException;

/**
 * This class does something interesting and useful. Or not...
 */
public class ExportFormatManager implements Serializable {
    public interface IExportFormatSelector {
        public void select(String name);
    }

    private final HashMap<String, IExporterFactory> exportFormats;
    private final IExportFormatSelector exportFormatSelector;
    JMenu menu;
    ButtonGroup buttonGroup;

    public ExportFormatManager(GuiUtils guiUtils, File exportFormatFile, IExportFormatSelector exportFormatSelector,
            IExporterFactory girrExporter, IExporterFactory waveExporter, IExporterFactory textExporter,
            IExporterFactory lircExporter, IExporterFactory prontoExporter) throws ParserConfigurationException, SAXException, IOException {
        this.exportFormatSelector = exportFormatSelector;
        exportFormats = new LinkedHashMap<>();
        exportFormats.put("Girr", girrExporter);
        exportFormats.put("Text", textExporter);
        exportFormats.put("LIRC", lircExporter);
        exportFormats.put("Wave", waveExporter);
        exportFormats.put("ProntoClassic", prontoExporter);

        if (!exportFormatFile.exists())
            throw new FileNotFoundException(exportFormatFile + " does not exist.");

        exportFormats.putAll(DynamicRemoteSetExportFormat.parseExportFormats(exportFormatFile));
    }

    public IExporterFactory get(String name) {
        return exportFormats.get(name);
    }

    private void createMenu(String selection) {
        menu = new JMenu();
        menu.setText("Export formats");
        menu.setToolTipText("Allows direct selection of export format");
        buttonGroup = new ButtonGroup();
        for (Entry<String, IExporterFactory> kvp : exportFormats.entrySet()) {
            final String name = kvp.getKey();
            JMenuItem menuItem = new JCheckBoxMenuItem(name);
            menuItem.setSelected(name.equals(selection));
            menuItem.addActionListener(new java.awt.event.ActionListener() {
                @Override
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    exportFormatSelector.select(name);
                }
            });

            buttonGroup.add(menuItem);
            menu.add(menuItem);
        }
    }

    public void setMenuSelection(String format) {
        if (menu == null)
            createMenu(format);

        for (int i = 0; i < menu.getItemCount(); i++) {
            JMenuItem item = menu.getItem(i);
            item.setSelected(item.getText().equals(format));
        }
    }

    public JMenu getMenu(String format) {
        createMenu(format);
        return menu;
    }

    public Set<String> getNames() {
        return exportFormats.keySet();
    }

    public String[] toArray() {
        return exportFormats.keySet().toArray(new String[exportFormats.keySet().size()]);
    }
}
