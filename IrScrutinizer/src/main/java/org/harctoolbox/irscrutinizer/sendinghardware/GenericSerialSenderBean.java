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

package org.harctoolbox.irscrutinizer.sendinghardware;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.IOException;
import org.harctoolbox.guicomponents.GuiUtils;
import org.harctoolbox.guicomponents.SerialPortBean;
import org.harctoolbox.harchardware.IHarcHardware;
import org.harctoolbox.harchardware.comm.LocalSerialPort;

public class GenericSerialSenderBean extends javax.swing.JPanel {
    private String portName;
    private int baud; // Technically correct would have been bit/s. See http://en.wikipedia.org/wiki/Baud
    private int dataSize;
    private LocalSerialPort.FlowControl flowControl;
    private LocalSerialPort.Parity parity;
    private int stopBits;
    private String command;
    private boolean raw; // raw/ccf
    private boolean useSigns;
    private String separator;
    private String lineEnding;
    private GuiUtils guiUtils;

    /**
     * Creates new form GenericSerialSenderBean
     * @param guiUtils
     */
    public GenericSerialSenderBean(GuiUtils guiUtils) {
        this.guiUtils = guiUtils;
        initComponents();
        serialCommandTextFieldActionPerformed(null);
        serialSignedComboBoxActionPerformed(null);
        serialFormatComboBoxActionPerformed(null);
        serialRawSeparatorComboBoxActionPerformed(null);
        serialLineEndingsComboBoxActionPerformed(null);
        setPortName(serialPortBean.getPortName());
        setBaud(serialPortBean.getBaud());
        setDataSize(serialPortBean.getDataSize());
        setFlowControl(serialPortBean.getFlowControl());
        setParity(serialPortBean.getParity());
        setStopBits(serialPortBean.getStopBits());

        this.serialPortBean.addPropertyChangeListener(new PropertyChangeListener() {

            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                if (evt.getPropertyName().equals(SerialPortBean.PROP_PORTNAME))
                    setPortName((String)evt.getNewValue());
                else if (evt.getPropertyName().equals(SerialPortBean.PROP_BAUD))
                    setBaud((Integer) evt.getNewValue());
                else if (evt.getPropertyName().equals(SerialPortBean.PROP_DATASIZE))
                    setDataSize((Integer)evt.getNewValue());
                else if (evt.getPropertyName().equals(SerialPortBean.PROP_FLOWCONTROL))
                    setFlowControl((LocalSerialPort.FlowControl)evt.getNewValue());
                else if (evt.getPropertyName().equals(SerialPortBean.PROP_PARITY))
                    setParity((LocalSerialPort.Parity)evt.getNewValue());
                else if (evt.getPropertyName().equals(SerialPortBean.PROP_STOPBITS))
                    setStopBits((Integer)evt.getNewValue());
                else
                    throw new RuntimeException("This cannot happen");
            }
        });
    }

    /**
     * Creates new form GenericSerialSenderBean
     */
    public GenericSerialSenderBean() {
        this(null);
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

    /**
     * @return the portName
     */
    public String getPortName() {
        return portName;
    }

    /**
     * @param portName the portName to set
     */
    public final void setPortName(String portName) {
        java.lang.String oldPortName = this.portName;
        this.portName = portName;
        propertyChangeSupport.firePropertyChange(PROP_PORTNAME, oldPortName, portName);
    }

    /**
     * @return the baudRate
     */
    public int getBaud() {
        return baud;
    }

    /**
     * @param baud
     */
    public final void setBaud(int baud) {
        int oldBaud = this.baud;
        this.baud = baud;
        propertyChangeSupport.firePropertyChange(PROP_BAUDRATE, oldBaud, baud);
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
    public final void setDataSize(int dataSize) {
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
    public final void setFlowControl(LocalSerialPort.FlowControl flowControl) {
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
    public final void setParity(LocalSerialPort.Parity parity) {
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
    public final void setStopBits(int stopBits) {
        int oldStopBits = this.stopBits;
        this.stopBits = stopBits;
        propertyChangeSupport.firePropertyChange(PROP_STOPBITS, oldStopBits, stopBits);
    }

    /**
     * @return the command
     */
    public String getCommand() {
        return command;
    }

    /**
     * @param command the command to set
     */
    public void setCommand(String command) {
        java.lang.String oldCommand = this.command;
        this.command = command;
        propertyChangeSupport.firePropertyChange(PROP_COMMAND, oldCommand, command);
    }

    /**
     * @return the raw
     */
    public boolean getRaw() {
        return raw;
    }

    /**
     * @param raw the raw to set
     */
    public void setRaw(boolean raw) {
        boolean oldRaw = this.raw;
        this.raw = raw;
        separatorLabel.setEnabled(raw);
        this.serialRawSeparatorComboBox.setEnabled(raw);
        this.serialSignedComboBox.setEnabled(raw);
        propertyChangeSupport.firePropertyChange(PROP_RAW, oldRaw, raw);
    }

    /**
     * @return the useSigns
     */
    public boolean getUseSigns() {
        return useSigns;
    }

    /**
     * @param useSigns the useSigns to set
     */
    public void setUseSigns(boolean useSigns) {
        boolean oldUseSigns = this.useSigns;
        this.useSigns = useSigns;
        propertyChangeSupport.firePropertyChange(PROP_USESIGNS, oldUseSigns, useSigns);
    }

    /**
     * @return the separator
     */
    public String getSeparator() {
        return separator;
    }

    /**
     * @param separator the separator to set
     */
    public void setSeparator(String separator) {
        java.lang.String oldSeparator = this.separator;
        this.separator = separator;
        propertyChangeSupport.firePropertyChange(PROP_SEPARATOR, oldSeparator, separator);
    }

    /**
     * @return the lineEnding
     */
    public String getLineEnding() {
        return lineEnding;
    }

    /**
     * @param lineEnding the lineEnding to set
     */
    public void setLineEnding(String lineEnding) {
        java.lang.String oldLineEnding = this.lineEnding;
        this.lineEnding = lineEnding;
        propertyChangeSupport.firePropertyChange(PROP_LINEENDING, oldLineEnding, lineEnding);
    }

    public void setHardware(IHarcHardware hardware) {
        serialPortBean.setHardware(hardware);
    }

    private final transient PropertyChangeSupport propertyChangeSupport = new java.beans.PropertyChangeSupport(this);
    public static final String PROP_PORTNAME = "PROP_PORTNAME";
    public static final String PROP_BAUDRATE = "PROP_BAUDRATE";
    public static final String PROP_DATASIZE = "PROP_DATASIZE";
    public static final String PROP_FLOWCONTROL = "PROP_FLOWCONTROL";
    public static final String PROP_PARITY = "PROP_PARITY";
    public static final String PROP_STOPBITS = "PROP_STOPBITS";
    public static final String PROP_COMMAND = "PROP_COMMAND";
    public static final String PROP_RAW = "PROP_RAW";
    public static final String PROP_USESIGNS = "PROP_USESIGNS";
    public static final String PROP_SEPARATOR = "PROP_SEPARATOR";
    public static final String PROP_LINEENDING = "PROP_LINEENDING";
    public static final String PROP_SERIALCOMMANDTEXTFIELD = "PROP_SERIALCOMMANDTEXTFIELD";

    public void setup(String desiredPort) throws IOException {
        serialPortBean.setup(desiredPort);
        setPortName(serialPortBean.getPortName());
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        serialPortBean = new org.harctoolbox.guicomponents.SerialPortBean(guiUtils);
        jLabel5 = new javax.swing.JLabel();
        formatLabel = new javax.swing.JLabel();
        serialCommandTextField = new javax.swing.JTextField();
        serialFormatComboBox = new javax.swing.JComboBox();
        separatorLabel = new javax.swing.JLabel();
        serialRawSeparatorComboBox = new javax.swing.JComboBox();
        jLabel23 = new javax.swing.JLabel();
        serialLineEndingsComboBox = new javax.swing.JComboBox();
        serialSignedComboBox = new javax.swing.JComboBox();
        jLabel1 = new javax.swing.JLabel();

        jLabel5.setText("Command");

        formatLabel.setText("Format");

        serialCommandTextField.setText("SEND");
        serialCommandTextField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                serialCommandTextFieldActionPerformed(evt);
            }
        });

        serialFormatComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "CCF", "RAW" }));
        serialFormatComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                serialFormatComboBoxActionPerformed(evt);
            }
        });

        separatorLabel.setText("Separator");
        separatorLabel.setEnabled(false);

        serialRawSeparatorComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { " ", ",", ";" }));
        serialRawSeparatorComboBox.setEnabled(false);
        serialRawSeparatorComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                serialRawSeparatorComboBoxActionPerformed(evt);
            }
        });

        jLabel23.setText("Line endings");

        serialLineEndingsComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "LF", "CR/LF", "CR" }));
        serialLineEndingsComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                serialLineEndingsComboBoxActionPerformed(evt);
            }
        });

        serialSignedComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Unsigned", "Signed" }));
        serialSignedComboBox.setEnabled(false);
        serialSignedComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                serialSignedComboBoxActionPerformed(evt);
            }
        });

        jLabel1.setText("Number format");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(serialPortBean, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel5)
                            .addComponent(serialCommandTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 150, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(serialFormatComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, 112, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(formatLabel))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(serialRawSeparatorComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(separatorLabel))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(jLabel23)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jLabel1))
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(serialLineEndingsComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(serialSignedComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))))))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(serialPortBean, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(0, 0, 0)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel5)
                            .addComponent(jLabel23)
                            .addComponent(jLabel1)
                            .addComponent(separatorLabel)))
                    .addComponent(formatLabel, javax.swing.GroupLayout.Alignment.TRAILING))
                .addGap(9, 9, 9)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(serialCommandTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(serialFormatComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(serialRawSeparatorComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(serialLineEndingsComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(serialSignedComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents

    private void serialCommandTextFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_serialCommandTextFieldActionPerformed
        setCommand(serialCommandTextField.getText());
    }//GEN-LAST:event_serialCommandTextFieldActionPerformed

    private void serialSignedComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_serialSignedComboBoxActionPerformed
        setUseSigns(serialSignedComboBox.getSelectedIndex() != 0);
    }//GEN-LAST:event_serialSignedComboBoxActionPerformed

    private void serialFormatComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_serialFormatComboBoxActionPerformed
        setRaw(((String)serialFormatComboBox.getSelectedItem()).equals("RAW"));
    }//GEN-LAST:event_serialFormatComboBoxActionPerformed

    private void serialRawSeparatorComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_serialRawSeparatorComboBoxActionPerformed
        setSeparator((String)serialRawSeparatorComboBox.getSelectedItem());
    }//GEN-LAST:event_serialRawSeparatorComboBoxActionPerformed

    private void serialLineEndingsComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_serialLineEndingsComboBoxActionPerformed
        String label = (String) serialLineEndingsComboBox.getSelectedItem();
        String ending = label.equals("LF") ? "\n"
                : label.equals("CR") ? "\r"
                : "\r\n";
        setLineEnding(ending);
    }//GEN-LAST:event_serialLineEndingsComboBoxActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel formatLabel;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel23;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel separatorLabel;
    private javax.swing.JTextField serialCommandTextField;
    private javax.swing.JComboBox serialFormatComboBox;
    private javax.swing.JComboBox serialLineEndingsComboBox;
    private org.harctoolbox.guicomponents.SerialPortBean serialPortBean;
    private javax.swing.JComboBox serialRawSeparatorComboBox;
    private javax.swing.JComboBox serialSignedComboBox;
    // End of variables declaration//GEN-END:variables
}
