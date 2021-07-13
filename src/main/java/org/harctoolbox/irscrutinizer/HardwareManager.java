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

import java.io.Closeable;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.swing.ButtonGroup;
import javax.swing.JMenu;
import javax.swing.JRadioButton;
import javax.swing.JTabbedPane;
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
    private final GuiUtils guiUtils;
//    private final Props properties;
    private final JTabbedPane hardwareTabbedPane;
    private boolean verbose;
    private final Map<String, HardwareBean> map;
    private HardwareBean selected;
    private JMenu menu;
    private ButtonGroup buttonGroup;


    HardwareManager(GuiUtils guiUtils, JTabbedPane hardwareTabbedPane) {
        this.guiUtils = guiUtils;
//        this.properties = properties;
        this.hardwareTabbedPane = hardwareTabbedPane;
        map = new LinkedHashMap<>(INITIAL_MAP_CAPACITY);
        menu = null;
        selected = null;
    }

    void setVerbose(boolean verbose) {
        this.verbose = verbose;
        if (selected != null)
            selected.setVerbose(verbose);
//        properties.setVerbose(verbose);
    }

    public void add(HardwareBean hardwareBean) {
//        Hardware hardware = new Hardware(hardwareBean);
        String name = hardwareBean.getName();
        map.put(name, hardwareBean);
//        hardwareBean.addPropertyChangeListener((PropertyChangeEvent evt) -> {
//            switch (evt.getPropertyName()) {
////                case GlobalCacheIrSenderSelector.PROP_IPNAME:
////                    properties.setGlobalCacheIpName((String) evt.getNewValue());
////                    break;
////                case GlobalCacheIrSenderSelector.PROP_MODULE:
////                    properties.setGlobalCacheModule((Integer) evt.getNewValue());
////                    break;
////                case GlobalCacheIrSenderSelector.PROP_PORT:
////                    properties.setGlobalCachePort((Integer) evt.getNewValue());
////                    break;
//                case HardwareBean.PROP_ISOPEN:
//                    if ((Boolean) evt.getNewValue())
//                        hardware =
////                    guiUtils.message("PROP_ISOPEN received, now " + ((Boolean) evt.getNewValue() ? "open" : "closed"));
//                    break;
//                default:
////                    throw new ThisCannotHappenException("Unhandled property: " + evt.getPropertyName());
//            }
//        });
    }

    public JMenu getMenu() {
        if (menu == null)
            createMenu(selected != null ? selected.getName() : null);
        return menu;
    }

    private void createMenu(String current) {
        menu = new JMenu();
        menu.setText("Selected Hardware");
        menu.setToolTipText("Allows direct selection of hardware");
        buttonGroup = new ButtonGroup();
        map.keySet().stream().map((String name) -> {
//            String name = kvp.getKey();
//            Hardware hardware = kvp.getValue();
            JRadioButton menuItem = new JRadioButton(name);
            menuItem.setSelected(name.equals(current));
            //portRadioButtons[i] = menuItem;
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
    }

    boolean canCapture() throws HardwareUnavailableException {
        assertSelected();
        return selected.canCapture();
    }

    boolean canSend() throws HardwareUnavailableException {
        assertSelected();
        return selected.canSend();
    }

    boolean isValid() throws HardwareUnavailableException {
        assertSelected();
        return selected.isValid();
    }

    boolean isReady() {
        return selected != null && selected.isValid();
    }

    private void select(HardwareBean hardwareBean) {
//        String old = selected != null ? selected.getName() : null;
        selected = hardwareBean;
        selected.setVerbose(verbose);
        // TODO: update menu
    }

    void select(String newHardware) throws HardwareUnavailableException {
        HardwareBean sel = map.get(newHardware);
        if (sel == null)
            throw new HardwareUnavailableException("Hardware " + newHardware + " not found.");
        select(sel);
    }

    /**
     * This is essentially a fallback.
     */
    public String select() {
        HardwareBean first = map.values().iterator().next();
        select(first);
        return first.getName();
    }

    @Override
    public void close() {
        map.values().forEach(hardware -> {
            hardware.close();
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

/*
    private class Hardware {

//        IHarcHardware hardware;
//        private final Class<? extends IHarcHardware> clazz;
        private final HardwareBean hardwareBean;
        private Transmitter transmitter;

//        private Hardware(HardwareBean hardwareBean) {
////            this.clazz = clazz;
//            this.hardwareBean = hardwareBean;
//        }

        private Hardware(HardwareBean hardwareBean) {
//            this.clazz = null;
            this.hardwareBean = hardwareBean;
        }

        ICapture asCapturer() throws CannotCaptureException {
            if (!canCapture())
                throw new CannotCaptureException(getName());
            return (ICapture) getHardware();
        }

        IRawIrSender asSender() throws CannotSendException {
            if (!canSend())
                throw new CannotSendException(getName());
            return (IRawIrSender) getHardware();
        }

        private String getName() {
            return hardwareBean.getName();
        }

        private IHarcHardware getHardware() {
            return hardwareBean.getHardware();
        }

        private boolean canCapture() {
            return getHardware() instanceof ICapture;
        }

        private void setVerbose(boolean verbose) {
            if (getHardware() != null)
                hardwareBean.setVerbose(verbose);
        }

        private boolean canSend() {
            return getHardware() instanceof IRawIrSender;
        }

        private void close() {
            try {
                if (getHardware() != null)
                    getHardware().close();
            } catch (IOException ex) {
                guiUtils.error(ex);
            }
        }

        private ModulatedIrSequence capture() throws CannotCaptureException, HarcHardwareException, IOException, InvalidArgumentException {
            ICapture capturer = asCapturer();
            return capturer.capture();
        }

        private boolean sendIr(IrSignal irSignal, int count) throws CannotSendException, HarcHardwareException, NoSuchTransmitterException, IOException, InvalidArgumentException {
            IRawIrSender sender = asSender();
            return sender.sendIr(irSignal, count, transmitter);
        }

        private boolean isValid() {
            return getHardware() != null && getHardware().isValid();
        }
    }
*/
}
