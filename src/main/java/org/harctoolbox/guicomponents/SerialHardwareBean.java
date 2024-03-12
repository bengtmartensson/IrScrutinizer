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
package org.harctoolbox.guicomponents;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import org.harctoolbox.harchardware.HarcHardwareException;
import org.harctoolbox.harchardware.comm.LocalSerialPort;
import org.harctoolbox.harchardware.comm.NonExistingPortException;

/**
 *
 *
 */
@SuppressWarnings("serial")
public abstract class SerialHardwareBean extends HardwareBean {

    protected static DefaultComboBoxModel<String> createModel(boolean useCached) {
        List<String> portNames;
        try {
            portNames = LocalSerialPort.getSerialPortNames(useCached);
        } catch (IOException ex) {
            portNames = new ArrayList<>(2);
            portNames.add("");
        }
        //portNames.add(0, "");
        return new DefaultComboBoxModel<>(portNames.toArray(new String[0]));
    }

    protected String portName;

    public SerialHardwareBean(GuiUtils guiUtils, boolean verbose, int timeout) {
        super(guiUtils, verbose, timeout);
    }

    protected void setupPortComboBox(JComboBox<String> portComboBox, boolean useCached, String preferredPort) {
        DefaultComboBoxModel<String> model = createModel(useCached);
        portComboBox.setModel(model);
        if (preferredPort != null)
            portComboBox.setSelectedItem(preferredPort);

        String actualPort = (String) portComboBox.getSelectedItem();
        setPortName(actualPort);
    }

    /**
     * @return the port
     */
    public String getPortName() {
        return portName;
    }

    /**
     * @param portName the port to set.
     * No sanity test is made.
     */
    public void setPortName(String portName) {
        if (portName == null || portName.isEmpty())
            return;
        enableOpenToggleButton(!isOpen());
        String oldPort = this.portName;
        this.portName = portName;
        propertyChangeSupport.firePropertyChange(PROP_PORTNAME, oldPort, portName);
    }

    @Override
    public void close() throws IOException {
        boolean oldIsOpen = isOpen();
        try {
            if (oldIsOpen)
                hardware.close();
        } finally {
            enableStuff(false);
            setVersion();
            propertyChangeSupport.firePropertyChange(PROP_ISOPEN, oldIsOpen, isOpen());
            hardware = null;
        }
    }

    @Override
    public void open() throws HarcHardwareException, IOException {
        boolean oldIsOpen = isOpen();
        try {
            if (!oldIsOpen) {
                setupHardware();
                hardware.open();
            }
        } finally {
            enableStuff();
            setVersion();
            propertyChangeSupport.firePropertyChange(PROP_ISOPEN, oldIsOpen, isOpen());
        }
    }

    protected abstract void setVersion();

    protected abstract void enableStuff(boolean b);

    private void enableStuff() {
        enableStuff(isOpen());
    }

    protected abstract void enableOpenToggleButton(boolean enabled);

    protected abstract void setupHardware() throws IOException, NonExistingPortException;
}
