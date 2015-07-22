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

package org.harctoolbox.guicomponents;

import java.awt.Cursor;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.IOException;
import java.util.ArrayList;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import org.harctoolbox.harchardware.HarcHardwareException;
import org.harctoolbox.harchardware.IHarcHardware;
import org.harctoolbox.harchardware.comm.LocalSerialPort;

/**
 *
 */
public class SerialPortSimpleBean extends javax.swing.JPanel {
    private static final long serialVersionUID = 1L;
    private static final int defaultBaudRate = 9600;
    public static final String PROP_VERSION = "PROP_VERSION";
    public static final String PROP_BAUD = "PROP_BAUD";
    public static final String PROP_ISOPEN = "PROP_ISOPEN";
    private static final String notInitialized = "not initialized";

    private String portName;
    private int baudRate;
    private String version;
    private GuiUtils guiUtils;
    private transient IHarcHardware hardware;
    private final boolean settableBaudRate;
    private boolean listenable;

    /**
     * Creates new form SerialPortSimpleBean
     */
    public SerialPortSimpleBean() {
        this(null, null, defaultBaudRate, true);
    }

    public SerialPortSimpleBean(GuiUtils guiUtils) {
        this(guiUtils, null, defaultBaudRate, true);
    }

    @SuppressWarnings("unchecked")
    public SerialPortSimpleBean(GuiUtils guiUtils, String initialPort, int initialBaud, boolean settableBaudRate) {
        initComponents();
        this.guiUtils = guiUtils;
        listenable = false;
        DefaultComboBoxModel<String> model;
        try {
            ArrayList<String> portList = LocalSerialPort.getSerialPortNames(true);
            model = new DefaultComboBoxModel<String>(portList.toArray(new String[portList.size()]));
        } catch (IOException ex) {
            model =  new DefaultComboBoxModel<String>(new String[]{ initialPort != null ? initialPort : notInitialized });
        } catch (LinkageError ex) {
            // Just to make Javabeans safe
            model =  new DefaultComboBoxModel<String>(new String[]{ initialPort != null ? initialPort : notInitialized });
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
        if (!hit) {
            // Got a problem here, want to select a port that is not there, at least not now
            if (model.getSize() > 0) {
                portComboBox.setSelectedIndex(0);
                initialPort = (String) portComboBox.getItemAt(0);
            }

        }
        setPortName(initialPort);
        setBaudRateUnconditionally(initialBaud);
        this.settableBaudRate = settableBaudRate;
        baudComboBox.setEnabled(settableBaudRate);
        baudRateLabel.setEnabled(settableBaudRate);
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
    public final void setPortName(String portName) {
        if (portName == null || portName.isEmpty())
            return;

        openToggleButton.setEnabled(hardware != null);
        String oldPort = this.portName;
        this.portName = portName;
        // this propery changer should set up the hardware and call setHardware()
        propertyChangeSupport.firePropertyChange(PROP_PORT, oldPort, portName);
    }

    /**
     * @return the baudRate
     */
    public int getBaudRate() {
        return baudRate;
    }

    /**
     * @param baudRate the baudRate to set
     */
    public void setBaudRate(int baudRate) {
        if (settableBaudRate)
            setBaudRateUnconditionally(baudRate);
    }

    private void setBaudRateUnconditionally(int baudRate) {
        int oldBaud = this.baudRate;
        this.baudRate = baudRate;
        this.baudComboBox.setSelectedItem(Integer.toString(baudRate));
        propertyChangeSupport.firePropertyChange(PROP_BAUD, oldBaud, baudRate);
    }

    public void setHardware(IHarcHardware hardware) {
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
        java.lang.String oldVersion = this.version;
        this.version = version;
        versionLabel.setEnabled(hardware.isValid());
        versionLiteralLabel.setEnabled(hardware.isValid());
        versionLabel.setText(version);
        propertyChangeSupport.firePropertyChange(PROP_VERSION, oldVersion, version);
    }

    private void setVersion() {
        try {
            setVersion(hardware.isValid() ? hardware.getVersion() : "<not connected>");
        } catch (IOException ex) {
        }
    }

    @Override
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        // just to be Javabeans safe
        if (propertyChangeSupport == null)
            super.addPropertyChangeListener(listener);
        else
            propertyChangeSupport.addPropertyChangeListener(listener);
    }

    @Override
    public void removePropertyChangeListener(PropertyChangeListener listener) {
        propertyChangeSupport.removePropertyChangeListener(listener);
    }

    private final transient PropertyChangeSupport propertyChangeSupport = new java.beans.PropertyChangeSupport(this);
    public static final String PROP_PORT = "PROP_PORT";

    public void setup(String desiredPort) throws IOException {
        ComboBoxModel model = portComboBox.getModel();
        if (model == null || model.getSize() == 0 || ((model.getSize() == 1) && ((String)portComboBox.getSelectedItem()).equals(notInitialized)))
            setupPortComboBox(true);

        portComboBox.setSelectedItem(desiredPort != null ? desiredPort : portName);
    }

    private void setupPortComboBox(boolean useCached) throws IOException {
        if (hardware != null)
            hardware.close();

        ArrayList<String> portNames = LocalSerialPort.getSerialPortNames(useCached);
        portNames.add(0, "");
        DefaultComboBoxModel<String> model = new DefaultComboBoxModel<String>(portNames.toArray(new String[portNames.size()]));
        portComboBox.setModel(model);
    }

    public boolean isListenable() {
        return listenable;
    }

    private void openClose(boolean opening) throws IOException, HarcHardwareException {
        boolean oldIsOpen = hardware.isValid();
        try {
            if (opening) {
                hardware.open();
                listenable = true;
                //openToggleButton.setSelected(hardware.isValid());
                //refreshButton.setEnabled(!hardware.isValid());
            } else {
                listenable = false;
                hardware.close();
                //openToggleButton.setSelected(false);
                //refreshButton.setEnabled(true);
            }
        } finally {
            enableStuff(opening && hardware.isValid());
            setVersion();
            propertyChangeSupport.firePropertyChange(PROP_ISOPEN, oldIsOpen, hardware.isValid());
        }
    }

    private void enableStuff(boolean isOpen) {
        baudComboBox.setEnabled(!isOpen && settableBaudRate);
        portComboBox.setEnabled(!isOpen);
    }

    private Cursor setBusyCursor() {
        Cursor oldCursor = getCursor();
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        return oldCursor;
    }

    private void resetCursor(Cursor cursor) {
        setCursor(cursor);
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        portComboBox = new javax.swing.JComboBox<String>();
        refreshButton = new javax.swing.JButton();
        jLabel1 = new javax.swing.JLabel();
        openToggleButton = new javax.swing.JToggleButton();
        versionLiteralLabel = new javax.swing.JLabel();
        versionLabel = new javax.swing.JLabel();
        baudComboBox = new javax.swing.JComboBox();
        baudRateLabel = new javax.swing.JLabel();

        setPreferredSize(new java.awt.Dimension(800, 80));

        portComboBox.setModel(new javax.swing.DefaultComboBoxModel<String>(new String[] { notInitialized }));
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
        openToggleButton.setEnabled(false);
        openToggleButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                openToggleButtonActionPerformed(evt);
            }
        });

        versionLiteralLabel.setText("Ver:");
        versionLiteralLabel.setToolTipText("Version of the firmware on the device, if supported by the device. Verifies that the connection is working.");
        versionLiteralLabel.setEnabled(false);

        versionLabel.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        versionLabel.setText("no version available");
        versionLabel.setToolTipText("Version of firmware on device. Serves to verify the connection.");
        versionLabel.setBorder(new javax.swing.border.SoftBevelBorder(javax.swing.border.BevelBorder.LOWERED));
        versionLabel.setEnabled(false);
        versionLabel.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);

        baudComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "115200", "57600", "38400", "19200", "9600", "4800", "2400", "1200" }));
        baudComboBox.setSelectedItem("9600");
        baudComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                baudComboBoxActionPerformed(evt);
            }
        });

        baudRateLabel.setText("bits/s");

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
                    .addComponent(portComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, 200, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(baudComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(openToggleButton)
                        .addGap(6, 6, 6)
                        .addComponent(versionLiteralLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 36, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(versionLabel, javax.swing.GroupLayout.DEFAULT_SIZE, 254, Short.MAX_VALUE))
                    .addComponent(baudRateLabel))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(baudRateLabel))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(portComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(refreshButton)
                    .addComponent(openToggleButton)
                    .addComponent(versionLiteralLabel)
                    .addComponent(versionLabel)
                    .addComponent(baudComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
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
        } catch (HarcHardwareException ex) {
            guiUtils.error(ex);
        } catch (IOException ex) {
            guiUtils.error(ex);
        } finally {
            openToggleButton.setSelected(hardware.isValid());
            //refreshButton.setEnabled(!hardware.isValid());
            resetCursor(oldCursor);
        }
    }//GEN-LAST:event_openToggleButtonActionPerformed

    private void baudComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_baudComboBoxActionPerformed
        setBaudRate(Integer.parseInt((String) baudComboBox.getSelectedItem()));
    }//GEN-LAST:event_baudComboBoxActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JComboBox baudComboBox;
    private javax.swing.JLabel baudRateLabel;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JToggleButton openToggleButton;
    private javax.swing.JComboBox portComboBox;
    private javax.swing.JButton refreshButton;
    private javax.swing.JLabel versionLabel;
    private javax.swing.JLabel versionLiteralLabel;
    // End of variables declaration//GEN-END:variables
}
