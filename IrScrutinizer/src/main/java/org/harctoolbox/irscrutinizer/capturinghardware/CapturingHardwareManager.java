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

package org.harctoolbox.irscrutinizer.capturinghardware;

import java.awt.Component;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedHashMap;
import javax.swing.ButtonGroup;
import javax.swing.JMenu;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTabbedPane;
import org.harctoolbox.IrpMaster.IrpMasterException;
import org.harctoolbox.IrpMaster.ModulatedIrSequence;
import org.harctoolbox.guicomponents.GuiUtils;
import org.harctoolbox.harchardware.HarcHardwareException;
import org.harctoolbox.harchardware.IHarcHardware;
import org.harctoolbox.harchardware.ir.ICapture;
import org.harctoolbox.irscrutinizer.Props;

/**
 * This class does something interesting and useful. Or not...
 */
public class CapturingHardwareManager {
    private final LinkedHashMap<String, ICapturingHardware<?>> table;
    private final JTabbedPane tabbedPane;
    private JMenu menu = null;
    private ButtonGroup buttonGroup;
    private final Props properties;
    private ICapturingHardware<? extends IHarcHardware> selected;
    private final GuiUtils guiUtils;

    public CapturingHardwareManager(GuiUtils guiUtils, Props properties, JTabbedPane tabbedPane) {
        this.guiUtils = guiUtils;
        this.properties = properties;
        this.tabbedPane = tabbedPane;
        table = new LinkedHashMap<>(16);
    }

    public Collection<ICapturingHardware<?>> getCapturingHardware() {
        return table.values();
    }

    public ICapturingHardware<?> getSelectedHardware() {
        return selected;
    }

    public ICapture getCapturer() {
        return selected.getCapturer();
    }

    public ICapturingHardware<?> getHardware(JPanel panel) {
        for (ICapturingHardware<?> hardware : table.values())
            if (hardware.getPanel() == panel)
                return hardware;
        return null;
    }

    public void add(ICapturingHardware<?> hardware) {
        if (table.containsKey(hardware.getName()))
            throw new IllegalArgumentException(hardware.getName() + " already exists in map.");
        table.put(hardware.getName(), hardware);
    }

    public String[] getNames() {
        return table.keySet().toArray(new String[table.size()]);
    }

    public ICapturingHardware<?> getCapturingHardware(String name) {
        return table.get(name);
    }

    public void setVerbose(boolean verbose) {
        selected.setVerbose(verbose);
    }

    public boolean isReady() {
        return (selected != null) && selected.isValid();
    }

    public boolean stopCapture() {
        return selected.stopCapture();
    }

    public boolean hasSelection() {
        return selected != null;
    }

    public ModulatedIrSequence capture() throws IOException, HarcHardwareException, IrpMasterException {
        if (selected == null)
            throw new IllegalArgumentException("No transitting hardware selected.");
        return selected.capture();
    }

    private void createMenu(String selection) {
        menu = new JMenu();
        menu.setText("Capturing Hardware");
        menu.setToolTipText("Allows direct selection of capturing hardware");
        buttonGroup = new ButtonGroup();
        table.entrySet().stream().map((kvp) -> {
            String name = kvp.getKey();
            final ICapturingHardware<?> hardware = kvp.getValue();
            JRadioButton menuItem = new JRadioButton(name);
            menuItem.setSelected(name.equals(selection));
            //portRadioButtons[i] = menuItem;
            menuItem.addActionListener((java.awt.event.ActionEvent evt) -> {
                try {
                    select(hardware);
                } catch (IOException | HarcHardwareException ex) {
                    guiUtils.error(ex);
                }
            });
            return menuItem;
        }).map((menuItem) -> {
            buttonGroup.add(menuItem);
            return menuItem;
        }).forEachOrdered((menuItem) -> {
            menu.add(menuItem);
        });
    }

    public JMenu getMenu() {
        if (menu == null)
            createMenu(selected != null ? selected.getName() : null);
        return menu;
    }

    /**
     *
     * @param name
     * @throws java.io.IOException
     * @throws org.harctoolbox.harchardware.HarcHardwareException
     */
    public void select(String name) throws IOException, HarcHardwareException {
        ICapturingHardware<?> hardware = table.get(name);
        if (hardware == null)
            throw new IllegalArgumentException(name + " does not exist in map.");

        select(hardware);
    }

    /**
     *
     * @param hardware
     * @throws IllegalArgumentException
     */
    private void select(ICapturingHardware<?> hardware) throws IOException, HarcHardwareException {
        // this calls selectHardware(...)
        tabbedPane.setSelectedComponent(hardware.getPanel()); // throws IllegalArgumentException
        if (selected == null)
            selectDoWork(hardware);
    }

    /**
     *
     * @param name
     * @throws IOException
     * @throws HarcHardwareException
     * @throws IllegalArgumentException
     */
    public void selectDoWork(String name) throws IOException, HarcHardwareException {
        ICapturingHardware<?> hardware = table.get(name);
        if (hardware == null) {
            throw new IllegalArgumentException(name + " does not exist in map.");
        }
        selectDoWork(hardware);
    }

    private void selectDoWork(ICapturingHardware<?> hardware) throws IOException, HarcHardwareException {
        if (selected != hardware) {
            //if (selected != null)
            //    selected.close();
            selected = hardware;
            hardware.setup();
        }
        selected.setVerbose(properties.getVerbose());

        properties.setCaptureDevice(hardware.getName());

        if (menu != null) {
            for (Component component : menu.getMenuComponents()) {
                JRadioButton menuItem = (JRadioButton) component;
                menuItem.setSelected(menuItem.getText().equals(hardware.getName()));
            }
        }
    }

    public void close() {
        table.values().forEach((hardware) -> {
            hardware.close();
        });
    }
}
