/*
 * Copyright (C) 2021 Bengt Martensson
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.harctoolbox.irscrutinizer;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.Closeable;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import javax.swing.AbstractButton;
import javax.swing.ButtonGroup;
import javax.swing.JMenu;
import javax.swing.JRadioButton;
import org.harctoolbox.guicomponents.GuiUtils;
import org.harctoolbox.guicomponents.HardwareBean;
import org.harctoolbox.guicomponents.HardwareBean.CannotCaptureException;
import org.harctoolbox.guicomponents.HardwareBean.CannotSendException;
import org.harctoolbox.harchardware.HarcHardwareException;
import org.harctoolbox.harchardware.TimeoutException;
import org.harctoolbox.harchardware.ir.NoSuchTransmitterException;
import org.harctoolbox.ircore.InvalidArgumentException;
import org.harctoolbox.ircore.IrSignal;
import org.harctoolbox.ircore.ModulatedIrSequence;
import org.harctoolbox.ircore.ThisCannotHappenException;

final class HardwareManager implements Iterable<String>, Closeable {
    private static final int INITIAL_MAP_CAPACITY = 8;
    public static final String PROP_SELECTED_HARDWARE = "PROP_SELECTED_HARDWARE";

    private final GuiUtils guiUtils;
    private boolean verbose;
    private final Map<String, HardwareBean> map;
    private HardwareBean selected;
    private JMenu menu;
    private ButtonGroup buttonGroup;
    private final PropertyChangeSupport propertyChangeSupport;

    HardwareManager(GuiUtils guiUtils) {
        this.guiUtils = guiUtils;
        this.propertyChangeSupport = new java.beans.PropertyChangeSupport(this);
        map = new LinkedHashMap<>(INITIAL_MAP_CAPACITY);
        menu = null;
        selected = null;
    }

    void setVerbose(boolean verbose) {
        this.verbose = verbose;
        if (selected != null)
            selected.setVerbose(verbose);
    }

    public void add(HardwareBean hardwareBean) {
        String name = hardwareBean.getName();
        map.put(name, hardwareBean);
    }

    public JMenu getMenu() {
        if (menu == null)
            createMenu();
        return menu;
    }

    private void createMenu() {
        menu = new JMenu();
        menu.setText("Selected Hardware");
        menu.setToolTipText("Allows direct selection of hardware");
        buttonGroup = new ButtonGroup();
        map.keySet().stream().map((String name) -> {
            JRadioButton menuItem = new JRadioButton(name);
//            menuItem.setSelected(name.equals(current));
            menuItem.addActionListener((java.awt.event.ActionEvent evt) -> {
                try {
                    select(name);
                } catch (HardwareUnavailableException ex) {
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
        updateMenuSelection();
    }

    private int selectedNr() {
        int i = 0;
        for (HardwareBean bean : map.values()) {
            if (bean == selected)
                return i;
            i++;
        }
        return -1;
    }

    private void updateMenuSelection() {
        if (menu != null) {
            int selNr = selectedNr();
            ((AbstractButton) menu.getMenuComponent(selNr)).setSelected(true);
        }
    }

    boolean canCapture() {
        return selected != null && selected.canCapture();
    }

    boolean canSend() throws HardwareUnavailableException {
        return selected != null && selected.canSend();
    }

    boolean isValid() {
        return selected != null && selected.getHardware().isValid();
    }

    boolean isReady() {
        return isValid();
    }

    private void select(HardwareBean hardwareBean) {
        if (hardwareBean != selected) {
            String old = selected != null ? selected.getName() : null;
            selected = hardwareBean;
            selected.setVerbose(verbose);
            updateMenuSelection();
            propertyChangeSupport.firePropertyChange(PROP_SELECTED_HARDWARE, old, hardwareBean.getName());
        }
    }

    void select(String newHardware) throws HardwareUnavailableException {
        HardwareBean sel = map.get(newHardware);
        if (sel == null)
            throw new HardwareUnavailableException("Hardware " + newHardware + " not found.");
        select(sel);
    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        if (propertyChangeSupport != null)
            propertyChangeSupport.addPropertyChangeListener(listener);
    }

    /**
     * This is a fallback if nothing is selected.
     */
    public String selectFallback() {
        HardwareBean first = map.values().iterator().next();
        select(first);
        return first.getName();
    }

    @Override
    public void close() {
        map.values().forEach(hardware -> {
            try {
                hardware.close();
            } catch (IOException ex) {
                guiUtils.error(ex);
            }
        });
        selected = null;
    }

    ModulatedIrSequence capture() throws TimeoutException, HardwareUnavailableException, CannotCaptureException, HarcHardwareException, IOException, InvalidArgumentException {
        assertSelected();
        return selected.capture();
    }

    boolean sendIr(IrSignal irSignal,  int count) throws HardwareUnavailableException, CannotSendException, HarcHardwareException, NoSuchTransmitterException, IOException, InvalidArgumentException {
        assertHardwareValid();
        return selected.sendIr(irSignal, count);
    }

    @Override
    public Iterator<String> iterator() {
        return map.keySet().iterator();
    }

    private void assertSelected() throws HardwareUnavailableException {
        if (selected == null)
            throw new HardwareUnavailableException("No hardware selected");
    }

    private void assertHardwareValid() throws HardwareUnavailableException {
        assertSelected();
        if (! selected.isValid())
            throw new HardwareUnavailableException("Hardware selected but not open.");
    }

    void remove(String name) {
        HardwareBean removed = map.remove(name);
        if (removed == null)
            throw new ThisCannotHappenException("Name " + name + " not existing."); // programming error
    }

    public HardwareBean getBean(String name) {
        Objects.requireNonNull(name);
        return map.get(name);
    }
}
