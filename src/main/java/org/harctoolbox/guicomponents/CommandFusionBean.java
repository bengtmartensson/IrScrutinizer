/*
Copyright (C) 2013, 2014, 2021 Bengt Martensson.

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
import java.io.IOException;
import java.util.List;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import org.harctoolbox.harchardware.HarcHardwareException;
import org.harctoolbox.harchardware.IHarcHardware;
import org.harctoolbox.harchardware.comm.LocalSerialPort;
import org.harctoolbox.harchardware.ir.CommandFusion;
import org.harctoolbox.ircore.InvalidArgumentException;
import org.harctoolbox.ircore.IrSignal;
import org.harctoolbox.ircore.ModulatedIrSequence;

/**
 *
 */
public final class CommandFusionBean extends HardwareBean {

    private String portName;
    private String version;

    /**
     * Creates new form SerialPortSimpleBean
     */
    public CommandFusionBean() {
        this(null);
    }

    public CommandFusionBean(GuiUtils guiUtils) {
        this(guiUtils, false, null);
    }

    public CommandFusionBean(GuiUtils guiUtils, boolean verbose, String initialPort) {
        super(guiUtils, verbose, 0);
        initComponents();
        DefaultComboBoxModel<String> model;
        try {
            List<String> portList = LocalSerialPort.getSerialPortNames(true);
            model = new DefaultComboBoxModel<>(portList.toArray(new String[portList.size()]));
        } catch (IOException | LinkageError ex) {
            model =  new DefaultComboBoxModel<>(new String[]{ initialPort != null ? initialPort : NOT_INITIALIZED });
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

        openToggleButton.setEnabled(! isOpen());
        String oldPort = this.portName;
        this.portName = portName;
        // this propery changer should set up the hardware and call setHardware()
        propertyChangeSupport.firePropertyChange(PROP_PORTNAME, oldPort, portName);
    }

    private void setHardware(IHarcHardware hardware) {
        this.hardware = hardware;
        openToggleButton.setEnabled(hardware != null);
        openToggleButton.setSelected(hardware.isValid());
        setVersion();
    }

    /**
     * @return the version
     */
    public String getVersion() {
        return version;
    }

    private void setVersion(String version) {
        //java.lang.String oldVersion = this.version;
        this.version = version != null ? version : NOT_INITIALIZED;
        versionLabel.setEnabled(hardware.isValid());
        versionLiteralLabel.setEnabled(hardware.isValid());
        versionLabel.setText(this.version); // Looks ugly if this is null.
        //propertyChangeSupport.firePropertyChange(PROP_VERSION, oldVersion, version);
    }

    private void setVersion() {
        try {
            setVersion(isOpen() ? hardware.getVersion() : NOT_CONNECTED);
        } catch (IOException ex) {
        }
    }

    private void setup() throws IOException {
        setup(portName);
    }

    private void setup(String desiredPort) throws IOException {
        ComboBoxModel<String> model = portComboBox.getModel();
        if (model == null || model.getSize() == 0 || ((model.getSize() == 1) && ((String)portComboBox.getSelectedItem()).equals(NOT_INITIALIZED)))
            setupPortComboBox(true);

        portComboBox.setSelectedItem(desiredPort != null ? desiredPort : portName);
    }

    private void setupPortComboBox(boolean useCached) throws IOException {
        if (hardware != null)
            hardware.close();

        List<String> portNames = LocalSerialPort.getSerialPortNames(useCached);
        portNames.add(0, "");
        DefaultComboBoxModel<String> model = new DefaultComboBoxModel<>(portNames.toArray(new String[portNames.size()]));
        portComboBox.setModel(model);
    }

    private void enableStuff(boolean isOpen) {
        portComboBox.setEnabled(! isOpen);
    }

    private void enableStuff() {
        enableStuff(isOpen());
    }

    @Override
    public void open() throws HarcHardwareException, IOException {
        boolean oldIsOpen = isOpen();
        try {
            if (!oldIsOpen) {
                hardware = new CommandFusion(portName, verbose);
                hardware.open();
            }
        } finally {
            enableStuff();
            setVersion();
            propertyChangeSupport.firePropertyChange(PROP_ISOPEN, oldIsOpen, hardware.isValid());
        }
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
    public boolean canCapture() {
        return true;
    }

    @Override
    public ModulatedIrSequence capture() throws IOException, InvalidArgumentException {
        return ((CommandFusion) hardware).capture();
    }

    @Override
    public boolean canSend() {
        return true;
    }

    @Override
    public boolean sendIr(IrSignal irSignal, int count) throws IOException {
        return ((CommandFusion) hardware).sendIr(irSignal, count, null);
    }

    @Override
    public String getName() {
        return CommandFusion.COMMAND_FUSION;
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
        versionLabel = new javax.swing.JLabel();

        setPreferredSize(new java.awt.Dimension(800, 80));

        portComboBox.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { NOT_INITIALIZED }));
        portComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                portComboBoxActionPerformed(evt);
            }
        });

        refreshButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/Crystal-Clear/22x22/actions/reload.png"))); // NOI18N
        refreshButton.setText("Refresh");
        refreshButton.setToolTipText("Reload list of available serial ports.");
        refreshButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                refreshButtonActionPerformed(evt);
            }
        });

        jLabel1.setText("Serial Port");

        openToggleButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/Crystal-Clear/22x22/actions/connect_creating.png"))); // NOI18N
        openToggleButton.setText("Open");
        openToggleButton.setToolTipText("Open or close connection to device.");
        openToggleButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                openToggleButtonActionPerformed(evt);
            }
        });

        versionLiteralLabel.setText("Ver:");
        versionLiteralLabel.setToolTipText("Version of the firmware on the device, if supported by the device. Verifies that the connection is working.");
        versionLiteralLabel.setEnabled(false);

        versionLabel.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        versionLabel.setText(NOT_INITIALIZED);
        versionLabel.setToolTipText("Version of firmware on device. Serves to verify the connection.");
        versionLabel.setBorder(new javax.swing.border.SoftBevelBorder(javax.swing.border.BevelBorder.LOWERED));
        versionLabel.setEnabled(false);
        versionLabel.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(6, 6, 6)
                .addComponent(refreshButton)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel1)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(portComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, 244, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(openToggleButton, javax.swing.GroupLayout.PREFERRED_SIZE, 130, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addGap(6, 6, 6)
                .addComponent(versionLiteralLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 36, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(versionLabel, javax.swing.GroupLayout.DEFAULT_SIZE, 221, Short.MAX_VALUE)
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(portComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(refreshButton)
                    .addComponent(openToggleButton)
                    .addComponent(versionLiteralLabel)
                    .addComponent(versionLabel))
                .addContainerGap(15, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents

    private void refreshButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_refreshButtonActionPerformed
        Cursor oldCursor = setBusyCursor();
        try {
            setupPortComboBox(false);
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
            resetCursor(oldCursor);
        }
    }//GEN-LAST:event_openToggleButtonActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel jLabel1;
    private javax.swing.JToggleButton openToggleButton;
    private javax.swing.JComboBox<String> portComboBox;
    private javax.swing.JButton refreshButton;
    private javax.swing.JLabel versionLabel;
    private javax.swing.JLabel versionLiteralLabel;
    // End of variables declaration//GEN-END:variables
}
