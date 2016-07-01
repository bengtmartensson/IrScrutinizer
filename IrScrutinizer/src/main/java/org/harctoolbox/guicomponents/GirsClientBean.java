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
import java.util.List;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import org.harctoolbox.harchardware.HarcHardwareException;
import org.harctoolbox.harchardware.comm.LocalSerialPort;
import org.harctoolbox.harchardware.comm.LocalSerialPortBuffered;
import org.harctoolbox.harchardware.comm.TcpSocketPort;
import org.harctoolbox.harchardware.ir.GirsClient;
import org.harctoolbox.irscrutinizer.Props;

/**
 *
 */
public class GirsClientBean extends javax.swing.JPanel implements ISendingReceivingBean {

    //public static final String PROP_VERSION = "PROP_VERSION";
    //public static final String PROP_PORTNAME = "PROP_PORTNAME";
    //public static final String PROP_BAUD = "PROP_BAUD";
    public static final String PROP_IPNAME = "PROP_IPNAME";
    //public static final String PROP_PORT = "PROP_PORT";
    public static final String PROP_TYPE = "PROP_TYPE";
    //public static final String PROP_ISOPEN = "PROP_ISOPEN";

    public static final int defaultPingTimeout = 5000;

    private static final int defaultBaudRate = 115200;//9600;
    private static final int defaultPortNumber = 33333;
    private static final int defaultSerialTimeout = 12000; // 10 seconds + a bit
    private static final Type defaultType = Type.serial;
    private static final String defaultPortName = "/dev/arduino";
    private static final String defaultHost = "localhost";
    private static final String notInitialized = "not initialized";
    private static final String notConnected = "not connected";
    private static final String noVersionAvailable = "no version available";


    private String portName;
    private int baudRate;
//    private boolean usePort;
//    private boolean usePing;
//    private boolean useBrowse;
//    private String version;
    private GuiUtils guiUtils;
    private transient GirsClient<?> hardware;
    //private final boolean settableBaudRate;
    private boolean listenable;
    private int portNumber;
    private String ipName;
    private int pingTimeout;
    private Type type;
    private final Props properties;


    public void initHardware() throws HarcHardwareException, IOException {
        if (hardware != null)
            hardware.close();

        boolean verbose = properties.getVerbose();
        switch (getType()) {
            case serial: {
                try {
                    LocalSerialPortBuffered comm = new LocalSerialPortBuffered(getPortName(), getBaudRate(), defaultSerialTimeout, verbose);
                    hardware = new GirsClient<>(comm);
                } catch (NoSuchPortException | PortInUseException | UnsupportedCommOperationException | IOException | HarcHardwareException ex) {
                    guiUtils.error(ex);
                }
            }
            break;
            case tcp: {
                try {
                    TcpSocketPort comm = new TcpSocketPort(ipName, portNumber, defaultSerialTimeout, verbose, TcpSocketPort.ConnectionMode.keepAlive);
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

    public static enum Type {
        serial,
        tcp,
        upd,
        http
    }

    /**
     *
     */
    public GirsClientBean() {
        this(null, null);
    }

    public GirsClientBean(GuiUtils guiUtils, Props properties) {
//        this(guiUtils, properties.getGirsClientSerialPortName(), properties.getGirsClientSerialPortBaudRate(),
//                properties.getGirsClientIPName(), properties.getGirsClientPortNumber(), Type.valueOf(properties.getGirsClientType()));
//        this.properties = properties;
//    }
//
//    public GirsClientBean(GuiUtils guiUtils, Props properties, String initialPort, int initialBaud,
//            String initialIp, int initialPortNumber, Type type) {
//        this(guiUtils, properties.getGirsClientSerialPortName(), properties.getGirsClientSerialPortBaudRate(),
//                properties.getGirsClientIPName(), properties.getGirsClientPortNumber(), Type.valueOf(properties.getGirsClientType()));
//    }
//
//    public GirsClientBean(GuiUtils guiUtils, String initialPort, int initialBaud,
//            String initialIp, int initialPortNumber, Type type/*, boolean settableBaudRate,
//            boolean usePort, boolean usePing, boolean useBrowse*/) {
        this.guiUtils = guiUtils;
        this.properties = properties;
        this.pingTimeout = defaultPingTimeout;
        initComponents();
        String initialPort = properties != null ? properties.getGirsClientSerialPortName() : defaultPortName;
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
        setBaudRate(properties != null ? properties.getGirsClientSerialPortBaudRate() : defaultBaudRate);
        //this.settableBaudRate = settableBaudRate;
        //baudComboBox.setEnabled(settableBaudRate);
        //baudRateLabel.setEnabled(settableBaudRate);
        setIpName(properties != null ? properties.getGirsClientIPName() : defaultHost);
        setPortNumber(properties != null ? properties.getGirsClientPortNumber() : defaultPortNumber);
        setType(properties != null ? Type.valueOf(properties.getGirsClientType()) : defaultType);
        //setSerial(properties != null ? properties.getGirsClientIsSerial() : true);
        //setEthernetType(properties != null ? properties.getGirsClientEthernetType() : "tcp");

        if (properties != null) { // to be javabeans safe...
            properties.addVerboseChangeListener(new Props.IPropertyChangeListener() {
                @Override
                public void propertyChange(String name, Object oldValue, Object newValue) {
                    if (hardware != null)
                        hardware.setVerbosity((Boolean) newValue);
                }
            });
        }
    }

//    private void setSerial(boolean isSerial) {
//        serialTcpTabbedPane.setSelectedIndex(isSerial ? 0 : 1);
//    }
//
//    private void setEthernetType(String type) {
//        typeComboBox.setSelectedItem(type);
//    }

//        properties != null ? properties.getGirsClientEthernetType() : "tcp"
//            });
//
    private void setType(Type type) {
        Type oldType = this.type;
        this.type = type;
        //serialTcpTabbedPane.setSelectedIndex(type == Type.serial ? 0 : 1);
        //if (type != Type.serial)
        //    typeComboBox.setSelectedItem(type.toString());
        propertyChangeSupport.firePropertyChange(PROP_TYPE, oldType, type);
        properties.setGirsClientType(type.toString());
    }

//    private void setType() {
//        setType(serialTcpTabbedPane.getSelectedIndex() == 0
//                ? Type.serial
//                : Type.valueOf((String) typeComboBox.getSelectedItem()));
//    }
//

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
    private void setPortName(String portName) {
        if (portName == null || portName.isEmpty())
            return;

        //openToggleButton.setEnabled(hardware != null);
        String oldPort = this.portName;
        this.portName = portName;

        propertyChangeSupport.firePropertyChange(PROP_PORTNAME, oldPort, portName);
        if (properties != null)
            properties.setGirsClientSerialPortName(portName);
    }

    /**
     * @return the baudRate
     */
    public int getBaudRate() {
        return baudRate;
    }

//    /**
//     * @param baudRate the baudRate to set
//     */
//    public void setBaudRate(int baudRate) {
//        int oldBaudRate = baudRate;
//        if (settableBaudRate)
//            setBaudRateUnconditionally(baudRate);
//        propertyChangeSupport.firePropertyChange(PROP_BAUD, oldBaudRate, baudRate);
//    }

    private void setBaudRate(int baudRate) {
        int oldBaud = this.baudRate;
        this.baudRate = baudRate;
        this.baudComboBox.setSelectedItem(Integer.toString(baudRate));
        propertyChangeSupport.firePropertyChange(PROP_BAUD, oldBaud, baudRate);
        if (properties != null)
            properties.setGirsClientSerialPortBaudRate(baudRate);
    }

    private void setHardware(GirsClient<?> hardware) {
        this.hardware = hardware;
        openToggleButton.setEnabled(hardware != null);
        openToggleButton.setSelected(hardware.isValid());
        setVersion();
    }

    /**
     * @return the version
     */
    public String getVersion() {
        return isValid() ? versionTextField.getText() : null;
    }

    private void setVersion(String version) {
        //String oldVersion = this.version;
        //this.version = version;
        versionLabel.setEnabled(hardware.isValid());
        versionTextField.setEnabled(hardware.isValid());
        versionTextField.setText(version);
        //propertyChangeSupport.firePropertyChange(PROP_VERSION, oldVersion, version);
    }

    private void setVersion() {
        try {
            setVersion(hardware.isValid() ? hardware.getVersion() : notConnected);
            setModules(hardware.isValid() ? hardware.getModules() : null);
        } catch (IOException ex) {
            setVersion(notConnected);
            setModules(null);
        }
    }

    private void setModules(List<String>modules) {
        modulesTextField.setText(modules == null ? "" : String.join(" ", modules));
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

    private void setup(String desiredPort, int baud, String ip, int port, Type type) throws IOException {
        if (type == Type.serial) {
            ComboBoxModel<String> model = portComboBox.getModel();
            if (model == null || model.getSize() == 0 || ((model.getSize() == 1) && portComboBox.getSelectedItem().equals(notInitialized)))
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

//    public boolean isListenable() {
//        return listenable;
//    }

//    private int getPortNumber() {
//        return portNumber;
//    }

    private void setIpName(String name) {
        ipNameTextField.setText(name);
        String old = ipName;
        ipName = name;
        propertyChangeSupport.firePropertyChange(PROP_IPNAME, old, name);
        if (properties != null)
            properties.setGirsClientIPName(name);
    }

    private void setPortNumber(int val) {
        portNumberTextField.setText(Integer.toString(val));
        int old = portNumber;
        portNumber = val;
        propertyChangeSupport.firePropertyChange(PROP_PORTNAME, old, val);
        if (properties != null)
            properties.setGirsClientPortNumber(val);
    }

//    private String getIpName() {
//        return ipName;
//    }
//
//    public int getPingTimeout() {
//        return pingTimeout;
//    }

    public void setPingTimeout(int val) {
        pingTimeout = val;
    }

//    private void setGuiUtils(GuiUtils guiUtils) {
//        this.guiUtils = guiUtils;
//    }

    public GirsClient<?> getHardware() {
        return hardware;
    }

    public boolean isPingable(boolean useGui) {
        Cursor oldCursor = setBusyCursor();
        boolean success = false;
        //String ipName = getIpName();
        try {
            success = InetAddress.getByName(ipName).isReachable(pingTimeout);
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

    private void openClose(boolean opening) throws IOException, HarcHardwareException {
        Cursor oldCursor = setBusyCursor();
        boolean oldIsOpen = hardware.isValid();
        try {
            if (opening) {
                initHardware();
                hardware.open();
                hardware.setUseReceiveForCapture(useReceiveForCaptureCheckBox.isSelected());
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
        baudComboBox.setEnabled(!isOpen /*&& settableBaudRate*/);
        baudRateLabel.setEnabled(!isOpen);
        portComboBox.setEnabled(!isOpen);
        serialPortLabel.setEnabled(!isOpen);
        ipNameTextField.setEnabled(!isOpen);
        ipLabel.setEnabled(!isOpen);
        portNumberTextField.setEnabled(!isOpen);
        portNumberLabel.setEnabled(!isOpen);
        typeComboBox.setEnabled(!isOpen);
        typeLabel.setEnabled(!isOpen);
        modulesTextField.setEnabled(isOpen);
        modulesLabel.setEnabled(isOpen);
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

        serialTcpTabbedPane.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                serialTcpTabbedPaneStateChanged(evt);
            }
        });

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
                    .addComponent(ipNameTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 164, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(ethernetPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(portNumberTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 56, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(portNumberLabel))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(ethernetPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(ethernetPanelLayout.createSequentialGroup()
                        .addComponent(typeComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(browseButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
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

        useReceiveForCaptureCheckBox.setText("Use receive instead of capture");
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
                        .addComponent(serialTcpTabbedPane, javax.swing.GroupLayout.DEFAULT_SIZE, 517, Short.MAX_VALUE)
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
                                .addGap(0, 37, Short.MAX_VALUE))))
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
        setBaudRate(Integer.parseInt((String) baudComboBox.getSelectedItem()));
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
        setType(Type.valueOf((String)typeComboBox.getSelectedItem()));
    }//GEN-LAST:event_typeComboBoxActionPerformed

    private void openToggleButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_openToggleButtonActionPerformed
        try {
            openClose(openToggleButton.isSelected());
        } catch (IOException | HarcHardwareException ex) {
            guiUtils.error(ex);
        } finally {
            openToggleButton.setSelected(hardware.isValid());
        }
    }//GEN-LAST:event_openToggleButtonActionPerformed

    private void serialPanelComponentShown(java.awt.event.ComponentEvent evt) {//GEN-FIRST:event_serialPanelComponentShown
        setType(Type.serial);
    }//GEN-LAST:event_serialPanelComponentShown

    private void ethernetPanelComponentShown(java.awt.event.ComponentEvent evt) {//GEN-FIRST:event_ethernetPanelComponentShown
        setType(Type.valueOf((String)typeComboBox.getSelectedItem()));
    }//GEN-LAST:event_ethernetPanelComponentShown

    private void useReceiveForCaptureCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_useReceiveForCaptureCheckBoxActionPerformed
        if (hardware.isValid())
            hardware.setUseReceiveForCapture(useReceiveForCaptureCheckBox.isSelected());
    }//GEN-LAST:event_useReceiveForCaptureCheckBoxActionPerformed

    private void serialTcpTabbedPaneStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_serialTcpTabbedPaneStateChanged
        //setType();
    }//GEN-LAST:event_serialTcpTabbedPaneStateChanged

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
