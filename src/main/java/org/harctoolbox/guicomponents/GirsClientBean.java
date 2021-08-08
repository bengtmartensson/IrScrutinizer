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
import java.awt.Desktop;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.util.List;
import java.util.Locale;
import javax.swing.DefaultComboBoxModel;
import org.harctoolbox.harchardware.HarcHardwareException;
import org.harctoolbox.harchardware.comm.LocalSerialPort;
import org.harctoolbox.harchardware.comm.LocalSerialPortBuffered;
import org.harctoolbox.harchardware.comm.TcpSocketPort;
import org.harctoolbox.harchardware.ir.GirsClient;
import org.harctoolbox.ircore.IrSignal;
import org.harctoolbox.ircore.ModulatedIrSequence;
import org.harctoolbox.ircore.OddSequenceLengthException;

public final class GirsClientBean extends HardwareBean {

    public static final int DEFAULT_TIMEOUT         = 1000;
    public static final int DEFAULT_BAUD            = 115200;
    private static final int DEFAULT_SERIAL_TIMEOUT = 12000; // 10 seconds + margin
    public static final Type DEFAULT_TYPE           = Type.SERIAL;
    public static final String DEFAULT_PORTNAME     = "/dev/arduino";
    public static final String DEFAULT_IPNAME       = "arduino";
    public static final int DEFAULT_PORT            = 33333;

    private String portName;
    private int baud;
    private int portNumber;
    private String ipName;
    private Type type;

    public GirsClientBean() {
        this(null, false);
    }

    public GirsClientBean(GuiUtils guiUtils, boolean verbose) {
        this(guiUtils, verbose, DEFAULT_TIMEOUT, DEFAULT_PORTNAME, DEFAULT_BAUD, DEFAULT_IPNAME, DEFAULT_PORT, DEFAULT_TYPE);
    }

    public GirsClientBean(GuiUtils guiUtils, boolean verbose, int timeout, String initialPort, int baud,
            String ipName, int portNumber, Type type) {
        super(guiUtils, verbose, timeout);
        initComponents();
        if (initialPort == null)
            initialPort = DEFAULT_PORTNAME;
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
        setBaud(baud);
        setIpName(ipName);
        setPortNumber(portNumber);
        setType(type != null ? type : DEFAULT_TYPE);
        enableStuff(false);
    }

    private void initHardware() throws HarcHardwareException, IOException {
        if (hardware != null) {
            if (hardware.isValid())
                return;
            else {
                hardware.close();
                enableStuff(false);
            }
        }

        switch (getType()) {
            case SERIAL: {
                try {
                    LocalSerialPortBuffered comm = new LocalSerialPortBuffered(getPortName(), verbose, timeout, getBaud());
                    hardware = new GirsClient<>(comm);
                } catch (IOException | HarcHardwareException ex) {
                    guiUtils.error(ex);
                }
            }
            break;
            case TCP: {
                try {
                    TcpSocketPort comm = new TcpSocketPort(ipName, portNumber, DEFAULT_SERIAL_TIMEOUT, verbose, TcpSocketPort.ConnectionMode.keepAlive);
                    hardware = new GirsClient<>(comm);
                } catch (HarcHardwareException | IOException ex) {
                    guiUtils.error(ex);
                }
            }
            break;
            default:
                throw new IllegalArgumentException("Type " + getType() + " not yet supported");
        }
    }

    @Override
    public boolean sendIr(IrSignal irSignal, int count) throws IOException, HarcHardwareException {
        return ((GirsClient<?>) hardware).sendIr(irSignal, count);
    }

    @Override
    public ModulatedIrSequence capture() throws HarcHardwareException, OddSequenceLengthException, IOException {
        return isOpen()
                ? ((GirsClient<?>) hardware).capture()
                : null;
    }

    @Override
    public String getName() {
        return "Girs Client";
    }

    public void setPort(int portNumber) {
        int oldPort = this.portNumber;
        this.portNumber = portNumber;
        propertyChangeSupport.firePropertyChange(PROP_PORT, oldPort, portNumber);
    }

    public void setType(String typeName) {
        setType(Type.valueOf(typeName.toUpperCase(Locale.US)));
    }

    public void setType(Type type) {
        Type oldType = this.type;
        this.type = type;
        // ???
        propertyChangeSupport.firePropertyChange(PROP_TYPE, oldType, type);
    }

    public Type getType() {
        return type;
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

        String oldPort = this.portName;
        this.portName = portName;

        propertyChangeSupport.firePropertyChange(PROP_PORTNAME, oldPort, portName);
    }

    /**
     * @return the baudRate
     */
    public int getBaud() {
        return baud;
    }

    public void setBaud(int baud) {
        int oldBaud = this.baud;
        this.baud = baud;
        this.baudComboBox.setSelectedItem(Integer.toString(baud));
        propertyChangeSupport.firePropertyChange(PROP_BAUD, oldBaud, baud);
    }

    /**
     * @return the version
     */
    public String getVersion() {
        return isValid() ? versionTextField.getText() : null;
    }

    private void displayVersion(String version) {
        versionLabel.setEnabled(isOpen());
        versionTextField.setEnabled(isOpen());
        versionTextField.setText(version);
    }

    private void displayVersion() {
        try {
            displayVersion(isOpen() ? hardware.getVersion() : NOT_CONNECTED);
            displayModules(isOpen() ? ((GirsClient<?>) hardware).getModules() : null);
        } catch (IOException ex) {
            displayVersion(NOT_CONNECTED);
            displayModules(null);
        }
    }

    private void displayModules(List<String>modules) {
        modulesTextField.setText(modules == null ? "" : String.join(" ", modules));
    }

    private void setupPortComboBox(boolean useCached) throws IOException {
        if (hardware != null)
            hardware.close();

        List<String> portNames = LocalSerialPort.getSerialPortNames(useCached);
        portNames.add(0, "");
        DefaultComboBoxModel<String> model = new DefaultComboBoxModel<>(portNames.toArray(new String[portNames.size()]));
        portComboBox.setModel(model);
    }

    public void setIpName(String name) {
        ipNameTextField.setText(name);
        String old = ipName;
        ipName = name;
        propertyChangeSupport.firePropertyChange(PROP_IPNAME, old, name);
    }

    private void setPortNumber(int val) {
        portNumberTextField.setText(Integer.toString(val));
        int old = portNumber;
        portNumber = val;
        propertyChangeSupport.firePropertyChange(PROP_PORTNAME, old, val);
    }

    public boolean isPingable(boolean useGui) {
        Cursor oldCursor = setBusyCursor();
        boolean success = false;
        try {
            success = InetAddress.getByName(ipName).isReachable(timeout);
            if (useGui)
                guiUtils.info(ipName + (success ? " is reachable" : " is not reachable (using Java's isReachable)"));
        } catch (IOException ex) {
            if (useGui)
                guiUtils.info(ipName + " is not reachable (using Java's isReachable): " + ex.getMessage());
        } finally {
            resetCursor(oldCursor);
        }
        return success;
    }

    @Override
    public boolean canCapture() {
        return isOpen() && ((GirsClient<?>) hardware).hasCaptureModule();
    }

    @Override
    public boolean canSend() {
        return isOpen() && ((GirsClient<?>) hardware).hasTransmitModule();
    }

    @Override
    public void open() throws IOException, HarcHardwareException {
        Cursor oldCursor = setBusyCursor();
        boolean oldIsOpen = isOpen();
        try {
            initHardware();
            hardware.open();
            boolean canCapture = ((GirsClient<?>) hardware).hasCaptureModule();
            useReceiveForCaptureCheckBox.setEnabled(canCapture);
            useReceiveForCaptureCheckBox.setSelected(!canCapture);
            propertyChangeSupport.firePropertyChange(PROP_ISOPEN, oldIsOpen, hardware.isValid());
        } finally {
            resetCursor(oldCursor);
            enableStuff(isOpen());
        }
    }

    @Override
    public void close() throws IOException {
        if (hardware == null)
            return;
        Cursor oldCursor = setBusyCursor();
        boolean oldIsOpen = isOpen();//hardware.isValid();
        try {
            hardware.close();
            useReceiveForCaptureCheckBox.setSelected(false);
            propertyChangeSupport.firePropertyChange(PROP_ISOPEN, oldIsOpen, hardware.isValid());
        } finally {
            resetCursor(oldCursor);
            enableStuff(false);
        }
    }

    private void enableStuff(boolean isOpen) {
        serialTcpTabbedPane.setEnabled(!isOpen);
        refreshButton.setEnabled(!isOpen);
        baudComboBox.setEnabled(!isOpen /*&& settableBaudRate*/);
        baudRateLabel.setEnabled(!isOpen);
        portComboBox.setEnabled(!isOpen);
        serialPortLabel.setEnabled(!isOpen);
        ipNameTextField.setEnabled(!isOpen);
        ipLabel.setEnabled(!isOpen);
        portNumberTextField.setEnabled(!isOpen);
        portNumberLabel.setEnabled(!isOpen);
        //typeComboBox.setEnabled(!isOpen);
        typeLabel.setEnabled(!isOpen);
        modulesTextField.setEnabled(isOpen);
        modulesLabel.setEnabled(isOpen);
        openToggleButton.setSelected(isOpen);
        useReceiveForCaptureCheckBox.setEnabled(isOpen && ((GirsClient<?>) hardware).hasCaptureModule());
        displayVersion();
    }

    public static enum Type {
        SERIAL,
        TCP,
        UPD,
        HTTP
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        serialTcpTabbedPane = new javax.swing.JTabbedPane();
        serialPanel = new javax.swing.JPanel();
        refreshButton = new javax.swing.JButton();
        portComboBox = new javax.swing.JComboBox<>();
        baudComboBox = new javax.swing.JComboBox<>();
        serialPortLabel = new javax.swing.JLabel();
        baudRateLabel = new javax.swing.JLabel();
        ethernetPanel = new javax.swing.JPanel();
        ipLabel = new javax.swing.JLabel();
        ipNameTextField = new javax.swing.JTextField();
        portNumberLabel = new javax.swing.JLabel();
        portNumberTextField = new javax.swing.JTextField();
        browseButton = new javax.swing.JButton();
        pingButton = new javax.swing.JButton();
        typeComboBox = new javax.swing.JComboBox<>();
        typeLabel = new javax.swing.JLabel();
        openToggleButton = new javax.swing.JToggleButton();
        versionLabel = new javax.swing.JLabel();
        versionTextField = new javax.swing.JTextField();
        modulesTextField = new javax.swing.JTextField();
        modulesLabel = new javax.swing.JLabel();
        useReceiveForCaptureCheckBox = new javax.swing.JCheckBox();

        setPreferredSize(new java.awt.Dimension(800, 120));

        serialPanel.addComponentListener(new java.awt.event.ComponentAdapter() {
            public void componentShown(java.awt.event.ComponentEvent evt) {
                serialPanelComponentShown(evt);
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

        portComboBox.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { NOT_INITIALIZED }));
        portComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                portComboBoxActionPerformed(evt);
            }
        });

        baudComboBox.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "115200", "57600", "38400", "19200", "9600", "4800", "2400", "1200" }));
        baudComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                baudComboBoxActionPerformed(evt);
            }
        });

        serialPortLabel.setText("Serial Port");

        baudRateLabel.setText("bits/s");

        javax.swing.GroupLayout serialPanelLayout = new javax.swing.GroupLayout(serialPanel);
        serialPanel.setLayout(serialPanelLayout);
        serialPanelLayout.setHorizontalGroup(
            serialPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(serialPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(refreshButton)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(serialPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(portComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, 200, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(serialPortLabel))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(serialPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(baudRateLabel)
                    .addComponent(baudComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        serialPanelLayout.setVerticalGroup(
            serialPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, serialPanelLayout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(serialPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(serialPortLabel)
                    .addComponent(baudRateLabel))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(serialPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(refreshButton)
                    .addComponent(portComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(baudComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );

        serialTcpTabbedPane.addTab("Serial", serialPanel);

        ethernetPanel.addComponentListener(new java.awt.event.ComponentAdapter() {
            public void componentShown(java.awt.event.ComponentEvent evt) {
                ethernetPanelComponentShown(evt);
            }
        });

        ipLabel.setText("IP Name/Address");

        ipNameTextField.setText("192.168.1.29");
        ipNameTextField.setToolTipText("IP-Name or -address of host");
        ipNameTextField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ipNameTextFieldActionPerformed(evt);
            }
        });

        portNumberLabel.setText("Port");

        portNumberTextField.setText("33333");
        portNumberTextField.setToolTipText("Port Number");
        portNumberTextField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                portNumberTextFieldActionPerformed(evt);
            }
        });

        browseButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/Crystal-Clear/22x22/apps/browser.png"))); // NOI18N
        browseButton.setMnemonic('B');
        browseButton.setText("Browse");
        browseButton.setToolTipText("Browse WWW server at host");
        browseButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                browseButtonActionPerformed(evt);
            }
        });

        pingButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/Crystal-Clear/22x22/apps/bell.png"))); // NOI18N
        pingButton.setMnemonic('P');
        pingButton.setText("Ping");
        pingButton.setToolTipText("Try to ping host");
        pingButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                pingButtonActionPerformed(evt);
            }
        });

        typeComboBox.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "tcp", "udp", "http" }));
        typeComboBox.setToolTipText("Socket type");
        typeComboBox.setEnabled(false);
        typeComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                typeComboBoxActionPerformed(evt);
            }
        });

        typeLabel.setText("Type");

        javax.swing.GroupLayout ethernetPanelLayout = new javax.swing.GroupLayout(ethernetPanel);
        ethernetPanel.setLayout(ethernetPanelLayout);
        ethernetPanelLayout.setHorizontalGroup(
            ethernetPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(ethernetPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(ethernetPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(ipLabel)
                    .addComponent(ipNameTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 158, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(ethernetPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(portNumberTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 56, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(portNumberLabel))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(ethernetPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(ethernetPanelLayout.createSequentialGroup()
                        .addComponent(typeComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(browseButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(pingButton))
                    .addComponent(typeLabel))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        ethernetPanelLayout.setVerticalGroup(
            ethernetPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, ethernetPanelLayout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(ethernetPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(ipLabel)
                    .addComponent(portNumberLabel)
                    .addComponent(typeLabel))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(ethernetPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(ipNameTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(portNumberTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(typeComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(browseButton)
                    .addComponent(pingButton))
                .addContainerGap())
        );

        serialTcpTabbedPane.addTab("Ethernet", ethernetPanel);

        openToggleButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/Crystal-Clear/22x22/actions/connect_creating.png"))); // NOI18N
        openToggleButton.setText("Open");
        openToggleButton.setToolTipText("Open or close connection to device.");
        openToggleButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                openToggleButtonActionPerformed(evt);
            }
        });

        versionLabel.setText("Version");
        versionLabel.setEnabled(false);

        versionTextField.setEditable(false);
        versionTextField.setEnabled(false);

        modulesTextField.setEditable(false);
        modulesTextField.setEnabled(false);

        modulesLabel.setText("Modules:");
        modulesLabel.setEnabled(false);

        useReceiveForCaptureCheckBox.setText("Use receive for capture");
        useReceiveForCaptureCheckBox.setEnabled(false);
        useReceiveForCaptureCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                useReceiveForCaptureCheckBoxActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(serialTcpTabbedPane, javax.swing.GroupLayout.PREFERRED_SIZE, 535, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(versionLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 65, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(versionTextField))
                            .addGroup(layout.createSequentialGroup()
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(openToggleButton, javax.swing.GroupLayout.PREFERRED_SIZE, 108, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(useReceiveForCaptureCheckBox))
                                .addGap(0, 54, Short.MAX_VALUE))))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(modulesLabel)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(modulesTextField)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addGroup(layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(openToggleButton)
                        .addGap(18, 18, 18)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(versionTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(versionLabel))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(useReceiveForCaptureCheckBox))
                    .addComponent(serialTcpTabbedPane, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(modulesTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(modulesLabel))
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

    private void baudComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_baudComboBoxActionPerformed
        setBaud(Integer.parseInt((String) baudComboBox.getSelectedItem()));
    }//GEN-LAST:event_baudComboBoxActionPerformed

    private void ipNameTextFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ipNameTextFieldActionPerformed
        setIpName(ipNameTextField.getText());
    }//GEN-LAST:event_ipNameTextFieldActionPerformed

    private void portNumberTextFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_portNumberTextFieldActionPerformed
        try {
            int val = Integer.parseInt(portNumberTextField.getText());
            setPortNumber(val);
        } catch (NumberFormatException ex) {
            guiUtils.error("Cannot parse " + portNumberTextField.getText());
        }
    }//GEN-LAST:event_portNumberTextFieldActionPerformed

    private void browseButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_browseButtonActionPerformed
        try {
            Desktop.getDesktop().browse(URI.create("http://" + ipName));
        } catch (IOException ex) {
            guiUtils.error(ex);
        }
    }//GEN-LAST:event_browseButtonActionPerformed

    private void pingButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_pingButtonActionPerformed
        setIpName(ipNameTextField.getText());
        isPingable(true);
    }//GEN-LAST:event_pingButtonActionPerformed

    private void typeComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_typeComboBoxActionPerformed
        setType(Type.valueOf(typeComboBox.getSelectedItem().toString().toUpperCase(Locale.US)));
    }//GEN-LAST:event_typeComboBoxActionPerformed

    private void openToggleButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_openToggleButtonActionPerformed
        try {
            openClose(openToggleButton.isSelected());
        } catch (IOException | HarcHardwareException ex) {
            guiUtils.error(ex);
        } finally {
            openToggleButton.setSelected(isOpen());
        }
    }//GEN-LAST:event_openToggleButtonActionPerformed

    private void serialPanelComponentShown(java.awt.event.ComponentEvent evt) {//GEN-FIRST:event_serialPanelComponentShown
        setType(Type.SERIAL);
    }//GEN-LAST:event_serialPanelComponentShown

    private void ethernetPanelComponentShown(java.awt.event.ComponentEvent evt) {//GEN-FIRST:event_ethernetPanelComponentShown
        setType(Type.TCP);
    }//GEN-LAST:event_ethernetPanelComponentShown

    private void useReceiveForCaptureCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_useReceiveForCaptureCheckBoxActionPerformed
        if (hardware.isValid())
            try {
                ((GirsClient<?>) hardware).setUseReceiveForCapture(useReceiveForCaptureCheckBox.isSelected());
        } catch (HarcHardwareException ex) {
            guiUtils.error("Girs server does not support capture");
        }
    }//GEN-LAST:event_useReceiveForCaptureCheckBoxActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JComboBox<String> baudComboBox;
    private javax.swing.JLabel baudRateLabel;
    private javax.swing.JButton browseButton;
    private javax.swing.JPanel ethernetPanel;
    private javax.swing.JLabel ipLabel;
    private javax.swing.JTextField ipNameTextField;
    private javax.swing.JLabel modulesLabel;
    private javax.swing.JTextField modulesTextField;
    private javax.swing.JToggleButton openToggleButton;
    private javax.swing.JButton pingButton;
    private javax.swing.JComboBox<String> portComboBox;
    private javax.swing.JLabel portNumberLabel;
    private javax.swing.JTextField portNumberTextField;
    private javax.swing.JButton refreshButton;
    private javax.swing.JPanel serialPanel;
    private javax.swing.JLabel serialPortLabel;
    private javax.swing.JTabbedPane serialTcpTabbedPane;
    private javax.swing.JComboBox<String> typeComboBox;
    private javax.swing.JLabel typeLabel;
    private javax.swing.JCheckBox useReceiveForCaptureCheckBox;
    private javax.swing.JLabel versionLabel;
    private javax.swing.JTextField versionTextField;
    // End of variables declaration//GEN-END:variables
}
