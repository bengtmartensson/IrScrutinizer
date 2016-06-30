/*
Copyright (C) 2015 Bengt Martensson.

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

import gnu.io.NoSuchPortException;
import gnu.io.PortInUseException;
import gnu.io.UnsupportedCommOperationException;
import java.awt.Cursor;
import java.awt.Desktop;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import org.harctoolbox.harchardware.HarcHardwareException;
import org.harctoolbox.harchardware.ICommandLineDevice;
import org.harctoolbox.harchardware.IHarcHardware;
import org.harctoolbox.harchardware.comm.LocalSerialPort;
import org.harctoolbox.harchardware.comm.LocalSerialPortBuffered;
import org.harctoolbox.harchardware.comm.TcpSocketPort;

/**
 *
 */
public class TcpSerialComboBean extends javax.swing.JPanel implements ISendingReceivingBean {

    //public static final String PROP_VERSION = "PROP_VERSION";
    //public static final String PROP_PORTNAME = "PROP_PORTNAME";
    //public static final String PROP_BAUD = "PROP_BAUD";
    public static final String PROP_IPNAME = "PROP_IPNAME";
    //public static final String PROP_PORT = "PROP_PORT";
    public static final String PROP_TYPE = "PROP_TYPE";
    //public static final String PROP_ISOPEN = "PROP_ISOPEN";

    public static final int defaultPingTimeout = 5000;

    private static final int defaultBaudRate = 115200;//9600;
    private static final String notInitialized = "not initialized";
    private static final String notConnected = "not connected";
    private static final String noVersionAvailable = "no version available";


    private String portName;
    private int baudRate;
    private boolean usePort;
    private boolean usePing;
    private boolean useBrowse;
    private String version;
    private GuiUtils guiUtils;
    private transient IHarcHardware hardware;
    private final boolean settableBaudRate;
    private boolean listenable;
    private int portNumber;
    private String ipName;
    private int pingTimeout;
    private Type type;

    public void setHardware(boolean verbose) {
        ICommandLineDevice comm = createComm(verbose);
        if (comm == null)
            return;
      
    }

    private ICommandLineDevice createComm(boolean verbose) {
        ICommandLineDevice comm = null;
        switch (getType()) {
            case serial: {
                try {
                    comm = new LocalSerialPortBuffered(getPortName(), getBaudRate(), verbose);
                } catch (NoSuchPortException | PortInUseException | UnsupportedCommOperationException | IOException ex) {
                    guiUtils.error(ex);
                }
            }
            break;
            case tcp: {
                try {
                    comm = new TcpSocketPort(getIpName(), getPortNumber(), verbose, TcpSocketPort.ConnectionMode.keepAlive);
                } catch (UnknownHostException ex) {
                    Logger.getLogger(TcpSerialComboBean.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            break;
            default:
                throw new IllegalArgumentException("Type " + type + " not yet supported");
        }
        return comm;
    }

    public static enum Type {
        serial,
        tcp,
        upd,
        http
    }

    /**
     * Creates new form SerialPortSimpleBean
     */
    public TcpSerialComboBean() {
        this(null);
    }

    public TcpSerialComboBean(GuiUtils guiUtils) {
        this(guiUtils, null, defaultBaudRate, "localhost", 0, Type.serial, true, false, true, true);
    }

    @SuppressWarnings("unchecked")
    public TcpSerialComboBean(GuiUtils guiUtils, String initialPort, int initialBaud,
            String initialIp, int initialPortNumber, Type type, boolean settableBaudRate,
            boolean usePort, boolean usePing, boolean useBrowse) {
        initComponents();
        this.guiUtils = guiUtils;
        listenable = false;
        DefaultComboBoxModel<String> model;
        try {
            List<String> portList = LocalSerialPort.getSerialPortNames(true);
            model = new DefaultComboBoxModel<>(portList.toArray(new String[portList.size()]));
        } catch (IOException | LinkageError ex) {
            model =  new DefaultComboBoxModel<>(new String[]{ initialPort != null ? initialPort : notInitialized });
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
        setBaudRateUnconditionally(initialBaud);
        this.settableBaudRate = settableBaudRate;
        baudComboBox.setEnabled(settableBaudRate);
        baudRateLabel.setEnabled(settableBaudRate);
        setIpName(ipName);
        setPortNumber(initialPortNumber);
        setType(type);
    }

    public final void setType(Type type) {
        Type oldType = type;
        this.type = type;
        serialTcpTabbedPane.setSelectedIndex(type == Type.serial ? 0 : 1);
        if (type != Type.serial)
            typeComboBox.setSelectedItem(type.toString());
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
    public final void setPortName(String portName) {
        if (portName == null || portName.isEmpty())
            return;

        openToggleButton.setEnabled(hardware != null);
        String oldPort = this.portName;
        this.portName = portName;
        // this propery changer should set up the hardware and call setHardware()
        propertyChangeSupport.firePropertyChange(PROP_PORTNAME, oldPort, portName);
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
        int oldBaudRate = baudRate;
        if (settableBaudRate)
            setBaudRateUnconditionally(baudRate);
        propertyChangeSupport.firePropertyChange(PROP_BAUD, oldBaudRate, baudRate);
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
        String oldVersion = this.version;
        this.version = version;
        versionLabel.setEnabled(hardware.isValid());
        versionTextField.setEnabled(hardware.isValid());
        versionTextField.setText(version);
        propertyChangeSupport.firePropertyChange(PROP_VERSION, oldVersion, version);
    }

    private void setVersion() {
        try {
            setVersion(hardware.isValid() ? hardware.getVersion() : notConnected);
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

    public void setup(String desiredPort, int baud, String ip, int port, Type type) throws IOException {
        if (type == Type.serial) {
            ComboBoxModel model = portComboBox.getModel();
            if (model == null || model.getSize() == 0 || ((model.getSize() == 1) && ((String) portComboBox.getSelectedItem()).equals(notInitialized)))
                setupPortComboBox(true);

            portComboBox.setSelectedItem(desiredPort != null ? desiredPort : portName);
        } else {
            setIpName(ipName);
            setPortNumber(baud);
        }
        setType(type);
    }

    private void setupPortComboBox(boolean useCached) throws IOException {
        if (hardware != null)
            hardware.close();

        List<String> portNames = LocalSerialPort.getSerialPortNames(useCached);
        portNames.add(0, "");
        DefaultComboBoxModel<String> model = new DefaultComboBoxModel<>(portNames.toArray(new String[portNames.size()]));
        portComboBox.setModel(model);
    }

    public boolean isListenable() {
        return listenable;
    }

    private int getPortNumber() {
        return portNumber;
    }

    private void setIpName(String name) {
        String old = ipName;
        ipName = name;
        propertyChangeSupport.firePropertyChange(PROP_IPNAME, old, name);
    }

    private void setPortNumber(int val) {
        int old = portNumber;
        portNumber = val;
        propertyChangeSupport.firePropertyChange(PROP_PORTNAME, old, val);
    }

    private String getIpName() {
        return ipName;
    }

    public int getPingTimeout() {
        return pingTimeout;
    }

    public void setPingTimeout(int val) {
        pingTimeout = val;
    }

    public void setGuiUtils(GuiUtils guiUtils) {
        this.guiUtils = guiUtils;
    }

    public boolean isPingable() {
        boolean success = false;
        String host = getIpName();
        try {
            success = InetAddress.getByName(host).isReachable(pingTimeout);
            guiUtils.info(host + (success ? " is reachable" : " is not reachable (using Java's isReachable)"));
        } catch (IOException ex) {
            guiUtils.info(host + " is not reachable (using Java's isReachable): " + ex.getMessage());
        }
        return success;
    }

    public void testPing() {
        boolean result = isPingable();
        System.err.println(result);
    }

    private void openClose(boolean opening) throws IOException, HarcHardwareException {
        Cursor oldCursor = setBusyCursor();
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
            propertyChangeSupport.firePropertyChange(PROP_ISOPEN, oldIsOpen, hardware.isValid());
        } finally {
            resetCursor(oldCursor);
            enableStuff(opening && hardware.isValid());
            setVersion();
        }
    }

    private void enableStuff(boolean isOpen) {
        serialTcpTabbedPane.setEnabled(!isOpen);
        refreshButton.setEnabled(!isOpen);
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

        serialTcpTabbedPane = new javax.swing.JTabbedPane();
        serialPanel = new javax.swing.JPanel();
        refreshButton = new javax.swing.JButton();
        portComboBox = new javax.swing.JComboBox<>();
        baudComboBox = new javax.swing.JComboBox<>();
        jLabel2 = new javax.swing.JLabel();
        baudRateLabel = new javax.swing.JLabel();
        ethernetPanel = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        ipNameTextField = new javax.swing.JTextField();
        portLabel = new javax.swing.JLabel();
        portTextField = new javax.swing.JTextField();
        browseButton = new javax.swing.JButton();
        pingButton = new javax.swing.JButton();
        typeComboBox = new javax.swing.JComboBox<>();
        jLabel4 = new javax.swing.JLabel();
        openToggleButton = new javax.swing.JToggleButton();
        versionLabel = new javax.swing.JLabel();
        versionTextField = new javax.swing.JTextField();

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

        portComboBox.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { notInitialized }));
        portComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                portComboBoxActionPerformed(evt);
            }
        });

        baudComboBox.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "115200", "57600", "38400", "19200", "9600", "4800", "2400", "1200" }));
        baudComboBox.setSelectedItem("9600");
        baudComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                baudComboBoxActionPerformed(evt);
            }
        });

        jLabel2.setText("Serial Port");

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
                    .addComponent(jLabel2))
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
                    .addComponent(jLabel2)
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

        jLabel1.setText("IP Name/Address");

        ipNameTextField.setText(getIpName());
        ipNameTextField.setToolTipText("IP-Name or -address of host");
        ipNameTextField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ipNameTextFieldActionPerformed(evt);
            }
        });

        portLabel.setText("Port");

        portTextField.setText(Integer.toString(getPortNumber()));
        portTextField.setToolTipText("Portnumber");
        portTextField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                portTextFieldActionPerformed(evt);
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
        typeComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                typeComboBoxActionPerformed(evt);
            }
        });

        jLabel4.setText("Type");

        javax.swing.GroupLayout ethernetPanelLayout = new javax.swing.GroupLayout(ethernetPanel);
        ethernetPanel.setLayout(ethernetPanelLayout);
        ethernetPanelLayout.setHorizontalGroup(
            ethernetPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(ethernetPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(ethernetPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel1)
                    .addComponent(ipNameTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 164, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(ethernetPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(portTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 56, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(portLabel))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(ethernetPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(ethernetPanelLayout.createSequentialGroup()
                        .addComponent(typeComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(browseButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(pingButton))
                    .addComponent(jLabel4))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        ethernetPanelLayout.setVerticalGroup(
            ethernetPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, ethernetPanelLayout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(ethernetPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(portLabel)
                    .addComponent(jLabel4))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(ethernetPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(ipNameTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(portTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
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

        versionTextField.setEnabled(false);
        versionTextField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                versionTextFieldActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(serialTcpTabbedPane, javax.swing.GroupLayout.DEFAULT_SIZE, 518, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(openToggleButton, javax.swing.GroupLayout.PREFERRED_SIZE, 108, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(versionLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 65, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(49, 49, 49))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(versionTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 264, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addContainerGap())))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(serialTcpTabbedPane, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, Short.MAX_VALUE))
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(openToggleButton)
                    .addComponent(versionLabel, javax.swing.GroupLayout.Alignment.TRAILING))
                .addGap(18, 18, 18)
                .addComponent(versionTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
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
        setBaudRate(Integer.parseInt((String) baudComboBox.getSelectedItem()));
    }//GEN-LAST:event_baudComboBoxActionPerformed

    private void ipNameTextFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ipNameTextFieldActionPerformed
        String name = ipNameTextField.getText();
        try {
            InetAddress.getByName(name);
            setIpName(name);
        } catch (UnknownHostException ex) {
            guiUtils.warning(name + " does not resolve.");
        }
    }//GEN-LAST:event_ipNameTextFieldActionPerformed

    private void portTextFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_portTextFieldActionPerformed
        try {
            int val = Integer.parseInt(portTextField.getText());
            setPortNumber(val);
        } catch (NumberFormatException ex) {
            guiUtils.error("Cannot parse " + portTextField.getText());
            portTextField.setText(Integer.toString(portNumber));
        }
    }//GEN-LAST:event_portTextFieldActionPerformed

    private void browseButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_browseButtonActionPerformed
        ipName = ipNameTextField.getText();
        try {
            Desktop.getDesktop().browse(URI.create("http://" + this.getIpName()));
        } catch (IOException ex) {
            guiUtils.error(ex);
        }
    }//GEN-LAST:event_browseButtonActionPerformed

    private void pingButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_pingButtonActionPerformed
        isPingable();
    }//GEN-LAST:event_pingButtonActionPerformed

    private void typeComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_typeComboBoxActionPerformed
        setType(Type.valueOf((String)typeComboBox.getSelectedItem()));
    }//GEN-LAST:event_typeComboBoxActionPerformed

    private void openToggleButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_openToggleButtonActionPerformed
        try {
            openClose(openToggleButton.isSelected());
        } catch (IOException | HarcHardwareException ex) {
            guiUtils.error(ex);
        }
    }//GEN-LAST:event_openToggleButtonActionPerformed

    private void serialPanelComponentShown(java.awt.event.ComponentEvent evt) {//GEN-FIRST:event_serialPanelComponentShown
        setType(Type.serial);
    }//GEN-LAST:event_serialPanelComponentShown

    private void ethernetPanelComponentShown(java.awt.event.ComponentEvent evt) {//GEN-FIRST:event_ethernetPanelComponentShown
        setType(Type.valueOf((String)typeComboBox.getSelectedItem()));
    }//GEN-LAST:event_ethernetPanelComponentShown

    private void versionTextFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_versionTextFieldActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_versionTextFieldActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JComboBox<String> baudComboBox;
    private javax.swing.JLabel baudRateLabel;
    private javax.swing.JButton browseButton;
    private javax.swing.JPanel ethernetPanel;
    private javax.swing.JTextField ipNameTextField;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JToggleButton openToggleButton;
    private javax.swing.JButton pingButton;
    private javax.swing.JComboBox<String> portComboBox;
    private javax.swing.JLabel portLabel;
    private javax.swing.JTextField portTextField;
    private javax.swing.JButton refreshButton;
    private javax.swing.JPanel serialPanel;
    private javax.swing.JTabbedPane serialTcpTabbedPane;
    private javax.swing.JComboBox<String> typeComboBox;
    private javax.swing.JLabel versionLabel;
    private javax.swing.JTextField versionTextField;
    // End of variables declaration//GEN-END:variables
}
