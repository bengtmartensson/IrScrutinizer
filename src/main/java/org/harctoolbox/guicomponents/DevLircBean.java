/*
Copyright (C) 2016, 2021 Bengt Martensson.

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

package org.harctoolbox.guicomponents;

import java.awt.Cursor;
import java.io.File;
import java.io.IOException;
import javax.swing.DefaultComboBoxModel;
import org.harctoolbox.devslashlirc.LircDeviceException;
import org.harctoolbox.harchardware.HarcHardwareException;
import org.harctoolbox.harchardware.ir.DevLirc;
import org.harctoolbox.harchardware.ir.LircTransmitter;
import org.harctoolbox.ircore.IrSignal;
import org.harctoolbox.ircore.ModulatedIrSequence;
import org.harctoolbox.ircore.OddSequenceLengthException;
import org.harctoolbox.irscrutinizer.HardwareUnavailableException;

/**
 *
 */
public final class DevLircBean extends HardwareBean {

    private String portName;
    private String propsString;

    /**
     * Creates new form DevLircBean
     */
    public DevLircBean() {
        this(null, false, DevLirc.DEFAULT_BEGIN_TIMEOUT, null);
    }

    public DevLircBean(GuiUtils guiUtils) {
        this(guiUtils, false, DevLirc.DEFAULT_BEGIN_TIMEOUT, null);
    }

    public DevLircBean(GuiUtils guiUtils, boolean verbose, int timeout, String initialPort) {//, boolean enableSending) {
        super(guiUtils, verbose, timeout);
        initComponents();
        DefaultComboBoxModel<String> model;
        try {
            model = new DefaultComboBoxModel<>(candidates());
        } catch (LinkageError | IOException ex) {
            model = new DefaultComboBoxModel<>(new String[]{ initialPort != null ? initialPort : NOT_INITIALIZED });
        }

        portComboBox.setModel(model);
        boolean hit = false;
        if (initialPort != null) {
            for (int i = 0; i < model.getSize(); i++) {
                if (initialPort.equalsIgnoreCase(model.getElementAt(i))) {
                    hit = true;
                    portComboBox.setSelectedIndex(i);
                    break;
                }
            }
        }
        String actualPort = initialPort;
        if (!hit) {
            // Got a problem here, want to select a port that is not there, at least not now
            if (model.getSize() > 0) {
                portComboBox.setSelectedIndex(0);
                actualPort = portComboBox.getItemAt(0);
            }
        }
        setPortName(actualPort);
        enableStuff(false);
    }

    @Override
    public String getName() {
        return DevLirc.DEVSLASHLIRC;
    }

    private static String[] candidates() throws IOException {
        File[] candidatesFile = DevLirc.getCandidates();
        String[] candidates = new String[candidatesFile.length];
        for (int i = 0; i < candidatesFile.length; i++)
            candidates[i] = candidatesFile[i].getCanonicalPath();
        return candidates;
    }

    private void conditionallyEnableOpen() {
        openToggleButton.setEnabled(/*hardware != null && */ portComboBox.getSelectedItem() != null);
    }

    /**
     * @return the port
     */
    public String getPortName() {
        return portName;
    }

    /**
     * @param portName the port to set
     */
    public void setPortName(String portName) {
        if (portName == null || portName.isEmpty())
            return;

        conditionallyEnableOpen();
        String oldPort = this.portName;
        this.portName = portName;
        // this propery changer should set up the hardware and call setHardware()
        propertyChangeSupport.firePropertyChange(PROP_PORTNAME, oldPort, portName);
    }

    public void setHardware(DevLirc hardware) {
        this.hardware = hardware;
        conditionallyEnableOpen();
        openToggleButton.setSelected(isOpen());
    }

    private void setHardware() {
        try {
            setHardware(new DevLirc(portName, verbose, timeout));
        } catch (LircDeviceException ex) {
            guiUtils.error(ex);
        }
    }

    public LircTransmitter getTransmitter() {
        return ((DevLirc) getHardware()).canSetTransmitter()
                ? new LircTransmitter((String) transmitterComboBox.getSelectedItem())
                : new LircTransmitter();
    }

    private void setProps(String version) {
        java.lang.String oldVersion = this.propsString;
        this.propsString = version;
        propsLabel.setEnabled(isOpen());
        versionLiteralLabel.setEnabled(isOpen());
        propsLabel.setText(version);
        propertyChangeSupport.firePropertyChange(PROP_PROPS, oldVersion, version);
    }

    private void setProps() {
        setProps(isOpen() ? ((DevLirc) hardware).toString() : NOT_CONNECTED);
    }

    private void setupPortComboBox(/*boolean useCached*/) throws IOException {
        if (hardware != null)
            hardware.close();

        DefaultComboBoxModel<String> model = new DefaultComboBoxModel<>(candidates());
        portComboBox.setModel(model);
    }

    private void enableStuff(boolean isOpen) {
        portComboBox.setEnabled(!isOpen);
        boolean enableTransmitters = isOpen && ((DevLirc) getHardware()).canSetTransmitter();
        transmitterLabel.setEnabled(enableTransmitters);
        transmitterComboBox.setEnabled(enableTransmitters);
        conditionallyEnableOpen();
    }

    @Override
    public boolean canCapture() {
        return isOpen() && ((DevLirc) hardware).canReceive();
    }

    @Override
    public ModulatedIrSequence capture() throws HarcHardwareException, OddSequenceLengthException {
        return ((DevLirc) hardware).capture();
    }

    @Override
    public boolean canSend() {
        return isOpen() && ((DevLirc) hardware).canSend();
    }

    @Override
    public boolean sendIr(IrSignal irSignal, int count) throws HardwareUnavailableException, HarcHardwareException {
        return ((DevLirc) hardware).sendIr(irSignal, count, getTransmitter());
    }

    @Override
    public void open() throws IOException, HarcHardwareException {
        boolean oldIsOpen = isOpen();
        if (hardware == null)
            setHardware();

        try {
            hardware.open();
            DevLirc devLirc = (DevLirc) getHardware();
            if (devLirc.canSetTransmitter()) {
                DefaultComboBoxModel<String> transmitterComboBoxModel = new javax.swing.DefaultComboBoxModel<>(
                        devLirc.getNumberTransmitters() > 1
                        ? devLirc.getTransmitterNames()
                        : new String[]{"default", "1", "2", "3", "4", "5", "6", "7", "8"});
                transmitterComboBox.setModel(transmitterComboBoxModel);
            }
        } finally {
            enableStuff(isOpen());
            setProps();
            propertyChangeSupport.firePropertyChange(PROP_ISOPEN, oldIsOpen, hardware.isValid());
        }
    }

    @Override
    public void close() throws IOException {
        if (hardware == null)
            return;

        boolean oldIsOpen = hardware.isValid();
        try {
            hardware.close();
        } finally {
            enableStuff(false);
            setProps();
            propertyChangeSupport.firePropertyChange(PROP_ISOPEN, oldIsOpen, hardware.isValid());
        }
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        portComboBox = new javax.swing.JComboBox<>();
        refreshButton = new javax.swing.JButton();
        jLabel1 = new javax.swing.JLabel();
        openToggleButton = new javax.swing.JToggleButton();
        versionLiteralLabel = new javax.swing.JLabel();
        propsLabel = new javax.swing.JLabel();
        transmitterComboBox = new javax.swing.JComboBox<>();
        transmitterLabel = new javax.swing.JLabel();

        setPreferredSize(new java.awt.Dimension(800, 80));

        portComboBox.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { NOT_INITIALIZED }));
        portComboBox.setToolTipText("Device name to use");
        portComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                portComboBoxActionPerformed(evt);
            }
        });

        refreshButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/Crystal-Clear/22x22/actions/reload.png"))); // NOI18N
        refreshButton.setText("Refresh");
        refreshButton.setToolTipText("Reload list of available /dev/lirc devices");
        refreshButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                refreshButtonActionPerformed(evt);
            }
        });

        jLabel1.setText("Device");

        openToggleButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/Crystal-Clear/22x22/actions/connect_creating.png"))); // NOI18N
        openToggleButton.setText("Open");
        openToggleButton.setToolTipText("Open or close connection to device.");
        openToggleButton.setEnabled(false);
        openToggleButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                openToggleButtonActionPerformed(evt);
            }
        });

        versionLiteralLabel.setText("Detected properties");
        versionLiteralLabel.setToolTipText("Version of the firmware on the device, if supported by the device. Verifies that the connection is working.");
        versionLiteralLabel.setEnabled(false);

        propsLabel.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        propsLabel.setText("--");
        propsLabel.setToolTipText("Properties of the device, as reported from it.");
        propsLabel.setBorder(new javax.swing.border.SoftBevelBorder(javax.swing.border.BevelBorder.LOWERED));
        propsLabel.setEnabled(false);
        propsLabel.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);

        transmitterComboBox.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "default", "1", "2", "3", "4" }));
        transmitterComboBox.setToolTipText("The transmitter to use to send the command");
        transmitterComboBox.setEnabled(false);

        transmitterLabel.setText("xmitter");
        transmitterLabel.setEnabled(false);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(6, 6, 6)
                .addComponent(refreshButton)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(portComboBox, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jLabel1)
                        .addGap(0, 107, Short.MAX_VALUE)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(transmitterLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 66, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(transmitterComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, 72, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(openToggleButton)))
                .addGap(12, 12, 12)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(propsLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 356, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(versionLiteralLabel))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(6, 6, 6)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(transmitterLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 24, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(versionLiteralLabel))
                .addGap(4, 4, 4)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(openToggleButton)
                        .addComponent(propsLabel))
                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(transmitterComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(portComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(refreshButton)))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents

    private void refreshButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_refreshButtonActionPerformed
        Cursor oldCursor = setBusyCursor();
        try {
            setupPortComboBox();
            conditionallyEnableOpen();
        } catch (IOException ex) {
            guiUtils.error(ex);
        } finally {
            resetCursor(oldCursor);
        }
    }//GEN-LAST:event_refreshButtonActionPerformed

    private void portComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_portComboBoxActionPerformed
        setPortName((String) portComboBox.getSelectedItem());
    }//GEN-LAST:event_portComboBoxActionPerformed

    private void openToggleButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_openToggleButtonActionPerformed
        Cursor oldCursor = setBusyCursor();
        boolean opening = openToggleButton.isSelected();
        try {
            openClose(opening);
        } catch (HarcHardwareException | IOException ex) {
            guiUtils.error(ex);
        } finally {
            openToggleButton.setSelected(isOpen());
            resetCursor(oldCursor);
        }
    }//GEN-LAST:event_openToggleButtonActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel jLabel1;
    private javax.swing.JToggleButton openToggleButton;
    private javax.swing.JComboBox<String> portComboBox;
    private javax.swing.JLabel propsLabel;
    private javax.swing.JButton refreshButton;
    private javax.swing.JComboBox<String> transmitterComboBox;
    private javax.swing.JLabel transmitterLabel;
    private javax.swing.JLabel versionLiteralLabel;
    // End of variables declaration//GEN-END:variables
}
