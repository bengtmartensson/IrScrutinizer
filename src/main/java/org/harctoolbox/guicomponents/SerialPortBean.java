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

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import javax.swing.DefaultComboBoxModel;
import org.harctoolbox.harchardware.HarcHardwareException;
import org.harctoolbox.harchardware.IHarcHardware;
import org.harctoolbox.harchardware.comm.LocalSerialPort;

/**
 *
 */
public final class SerialPortBean extends javax.swing.JPanel {
    private String portName;
    private int baud;
    private int dataSize;
    private LocalSerialPort.FlowControl flowControl;
    private LocalSerialPort.Parity parity;
    private int stopBits; // semantics follows gnu.io.SerialPort
    private GuiUtils guiUtils;
    private boolean verbose;
    private IHarcHardware hardware;

    public static final String PROP_PORTNAME = "PROP_PORTNAME";
    public static final String PROP_BAUD = "PROP_BAUD";
    public static final String PROP_DATASIZE = "PROP_DATASIZE";
    public static final String PROP_FLOWCONTROL = "PROP_FLOWCONTROL";
    public static final String PROP_PARITY = "PROP_PARITY";
    public static final String PROP_STOPBITS = "PROP_STOPBITS";
    //public static final String PROP_PORT = "PROP_PORT";
    public static final Integer[] KNOWN_BAUD_RATES = { // from nrjavaserial
        50,
        75,
        110,
        134,
        150,
        200,
        300,
        600,
        1200,
        1800,
        2400,
        4800,
        9600,
        14400,
        19200,
        28800,
        38400,
        57600,
        115200,
        128000,
        230400,
        256000,
        460800,
        500000,
        576000,
        921600,
        1000000,
        1152000,
        1500000,
        2000000,
        2500000,
        3000000,
        3500000,
        4000000
    };

    /**
     * Creates new form SerialPortSimpleBean
     * @param guiUtils
     */
    public SerialPortBean(GuiUtils guiUtils) {
        this.guiUtils = guiUtils;
        initComponents();
        portComboBoxActionPerformed(null);
        baudComboBoxActionPerformed(null);
        bitsComboBoxActionPerformed(null);
        flowControlComboBoxActionPerformed(null);
        stopBitsComboBoxActionPerformed(null);
        parityComboBoxActionPerformed(null);
    }

    public SerialPortBean() {
        this(null);
    }

    /**
     * @return the portName
     */
    public String getPortName() {
        return portName;
    }

    /**
     * @param portName the portName to set
     */
    public void setPortName(String portName) {
        if (portName == null || portName.isEmpty())
            return;

        java.lang.String oldPortName = this.portName;
        this.portName = portName;
        propertyChangeSupport.firePropertyChange(PROP_PORTNAME, oldPortName, portName);
    }

    /**
     * @return the baud
     */
    public int getBaud() {
        return baud;
    }

    /**
     * @param baud the baud to set
     */
    public void setBaud(int baud) {
        int oldBaud = this.baud;
        this.baud = baud;
        propertyChangeSupport.firePropertyChange(PROP_BAUD, oldBaud, baud);
    }

    /**
     * @return the dataSize
     */
    public int getDataSize() {
        return dataSize;
    }

    /**
     * @param dataSize the dataSize to set
     */
    public void setDataSize(int dataSize) {
        int oldDataSize = this.dataSize;
        this.dataSize = dataSize;
        propertyChangeSupport.firePropertyChange(PROP_DATASIZE, oldDataSize, dataSize);
    }

    /**
     * @return the flowControl
     */
    public LocalSerialPort.FlowControl getFlowControl() {
        return flowControl;
    }

    /**
     * @param flowControl the flowControl to set
     */
    public void setFlowControl(LocalSerialPort.FlowControl flowControl) {
        LocalSerialPort.FlowControl oldFlowControl = this.flowControl;
        this.flowControl = flowControl;
        propertyChangeSupport.firePropertyChange(PROP_FLOWCONTROL, oldFlowControl, flowControl);
    }

    /**
     * @return the parity
     */
    public LocalSerialPort.Parity getParity() {
        return parity;
    }

    /**
     * @param parity the parity to set
     */
    public void setParity(LocalSerialPort.Parity parity) {
        LocalSerialPort.Parity oldParity = this.parity;
        this.parity = parity;
        propertyChangeSupport.firePropertyChange(PROP_PARITY, oldParity, parity);
    }

    /**
     * @return the stopBits
     */
    public int getStopBits() {
        return stopBits;
    }

    /**
     * @param stopBits the stopBits to set
     */
    public void setStopBits(int stopBits) {
        int oldStopBits = this.stopBits;
        this.stopBits = stopBits;
        propertyChangeSupport.firePropertyChange(PROP_STOPBITS, oldStopBits, stopBits);
    }

    /**
     * @param guiUtils the guiUtils to set
     */
    public void setGuiUtils(GuiUtils guiUtils) {
        this.guiUtils = guiUtils;
    }

    /**
     * @param verbose the verbose to set
     */
    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

    public void setHardware(IHarcHardware hardware) {
        this.hardware = hardware;
        openToggleButton.setEnabled(hardware !=  null);
        openToggleButton.setSelected(hardware.isValid());
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

    private final PropertyChangeSupport propertyChangeSupport = new java.beans.PropertyChangeSupport(this);

    public void setup(String desiredPort) throws IOException {
        if (portComboBox.getModel().getSize() == 0 || ((portComboBox.getModel().getSize() == 1) && ((String)portComboBox.getSelectedItem()).equals("not initialized")))
            setupPortComboBox(true);

        portComboBox.setSelectedItem(desiredPort != null ? desiredPort : portName);
    }

    public void setupPortComboBox(boolean useCached) throws IOException {
        if (hardware != null)
            hardware.close();

        List<String> portNames = LocalSerialPort.getSerialPortNames(useCached);
        portNames.add(0, null);
        DefaultComboBoxModel model = new DefaultComboBoxModel(portNames.toArray(new String[0]));
        portComboBox.setModel(model);
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
        bitsComboBox = new javax.swing.JComboBox<>();
        flowControlComboBox = new javax.swing.JComboBox<>();
        parityComboBox = new javax.swing.JComboBox<>();
        stopBitsComboBox = new javax.swing.JComboBox<>();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        baudComboBox = new javax.swing.JComboBox<Integer>();
        jLabel7 = new javax.swing.JLabel();
        openToggleButton = new javax.swing.JToggleButton();

        portComboBox.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "not initialized" }));
        portComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                portComboBoxActionPerformed(evt);
            }
        });

        refreshButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/Crystal-Clear/22x22/actions/reload.png"))); // NOI18N
        refreshButton.setText("Refresh");
        refreshButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                refreshButtonActionPerformed(evt);
            }
        });

        jLabel1.setText("Serial Port");

        bitsComboBox.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "5", "6", "7", "8" }));
        bitsComboBox.setSelectedIndex(3);
        bitsComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                bitsComboBoxActionPerformed(evt);
            }
        });

        flowControlComboBox.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "None", "RTS/CTS", "Xon/Xoff" }));
        flowControlComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                flowControlComboBoxActionPerformed(evt);
            }
        });

        parityComboBox.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "none", "odd", "even", "mark", "space" }));
        parityComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                parityComboBoxActionPerformed(evt);
            }
        });

        stopBitsComboBox.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "1", "2", "1.5" }));
        stopBitsComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                stopBitsComboBoxActionPerformed(evt);
            }
        });

        jLabel2.setText("Bits");

        jLabel3.setText("Flow cntrl");

        jLabel4.setText("parity");

        jLabel5.setText("stop bits");

        baudComboBox.setModel(new DefaultComboBoxModel<Integer>(KNOWN_BAUD_RATES)
        );
        baudComboBox.setSelectedItem(9600);
        baudComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                baudComboBoxActionPerformed(evt);
            }
        });

        jLabel7.setText("bits/s");

        openToggleButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/Crystal-Clear/22x22/actions/connect_creating.png"))); // NOI18N
        openToggleButton.setText("Open");
        openToggleButton.setEnabled(false);
        openToggleButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                openToggleButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(refreshButton)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(portComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, 142, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel1))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(baudComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel7))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(bitsComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel2))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(flowControlComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel3))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(parityComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel4))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel5)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(stopBitsComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(openToggleButton)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(jLabel2)
                    .addComponent(jLabel3)
                    .addComponent(jLabel4)
                    .addComponent(jLabel5)
                    .addComponent(jLabel7))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(portComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(refreshButton)
                    .addComponent(bitsComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(flowControlComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(parityComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(stopBitsComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(baudComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(openToggleButton))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents

    private void refreshButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_refreshButtonActionPerformed
        try {
            setupPortComboBox(false);
        } catch (IOException ex) {
            guiUtils.error(ex);
        }
    }//GEN-LAST:event_refreshButtonActionPerformed

    private void portComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_portComboBoxActionPerformed
        setPortName((String) portComboBox.getSelectedItem());
    }//GEN-LAST:event_portComboBoxActionPerformed

    private void baudComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_baudComboBoxActionPerformed
        setBaud(Integer.parseInt((String) baudComboBox.getSelectedItem()));
    }//GEN-LAST:event_baudComboBoxActionPerformed

    private void bitsComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_bitsComboBoxActionPerformed
        setDataSize(Integer.parseInt((String) bitsComboBox.getSelectedItem()));
    }//GEN-LAST:event_bitsComboBoxActionPerformed

    private void flowControlComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_flowControlComboBoxActionPerformed
        int index = flowControlComboBox.getSelectedIndex();
        LocalSerialPort.FlowControl flow = index == 0 ? LocalSerialPort.FlowControl.NONE
                : index == 1 ? LocalSerialPort.FlowControl.RTSCTS
                : LocalSerialPort.FlowControl.XONXOFF;
        this.setFlowControl(flow);
    }//GEN-LAST:event_flowControlComboBoxActionPerformed

    private void stopBitsComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_stopBitsComboBoxActionPerformed
        setStopBits(stopBitsComboBox.getSelectedIndex() + 1);
    }//GEN-LAST:event_stopBitsComboBoxActionPerformed

    private void parityComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_parityComboBoxActionPerformed
        setParity(LocalSerialPort.Parity.parse(((String)parityComboBox.getSelectedItem()).toUpperCase(Locale.US)));
    }//GEN-LAST:event_parityComboBoxActionPerformed

    private void openToggleButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_openToggleButtonActionPerformed
        try {
            if (openToggleButton.isSelected())
                hardware.open();
            else
                hardware.close();
        } catch (HarcHardwareException | IOException ex) {
            guiUtils.error(ex);
        } finally {
            openToggleButton.setSelected(hardware.isValid());
            refreshButton.setEnabled(!hardware.isValid() || !openToggleButton.isSelected());
        }
    }//GEN-LAST:event_openToggleButtonActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JComboBox<Integer> baudComboBox;
    private javax.swing.JComboBox<String> bitsComboBox;
    private javax.swing.JComboBox<String> flowControlComboBox;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JToggleButton openToggleButton;
    private javax.swing.JComboBox<String> parityComboBox;
    private javax.swing.JComboBox<String> portComboBox;
    private javax.swing.JButton refreshButton;
    private javax.swing.JComboBox<String> stopBitsComboBox;
    // End of variables declaration//GEN-END:variables
}
