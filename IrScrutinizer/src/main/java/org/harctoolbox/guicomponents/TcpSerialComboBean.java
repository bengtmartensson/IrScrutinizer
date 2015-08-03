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

import java.awt.Cursor;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.IOException;
import org.harctoolbox.harchardware.HarcHardwareException;
import org.harctoolbox.harchardware.IHarcHardware;

/**
 *
 */
public class TcpSerialComboBean extends javax.swing.JPanel {
    private static final long serialVersionUID = 1L;
    private static final int defaultBaudRate = 9600;
    public static final String PROP_VERSION = "PROP_VERSION";
    public static final String PROP_BAUD = "PROP_BAUD";
    public static final String PROP_ISOPEN = "PROP_ISOPEN";
    private static final String notInitialized = "not initialized";

    private String portName;
    private int baudRate;
    private boolean useTcp;
    private boolean usePort;
    private boolean usePing;
    private boolean useBrowse;
    private String version;
    private GuiUtils guiUtils;
    private transient IHarcHardware hardware;
    private final boolean settableBaudRate;
    private boolean listenable;
    private Object portComboBox;

    /**
     * Creates new form SerialPortSimpleBean
     */
    public TcpSerialComboBean() {
        this(null);
    }

    public TcpSerialComboBean(GuiUtils guiUtils) {
        this(guiUtils, null, defaultBaudRate, true, false, true, true, true);
    }

    @SuppressWarnings("unchecked")
    public TcpSerialComboBean(GuiUtils guiUtils, String initialPort, int initialBaud, boolean settableBaudRate,
            boolean useTcp, boolean usePort, boolean usePing, boolean useBrowse) {
        initComponents();
        this.guiUtils = guiUtils;
        listenable = false;
       /* DefaultComboBoxModel<String> model;
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
        setBaudRateUnconditionally(initialBaud);*/
        this.settableBaudRate = settableBaudRate;
        /*baudComboBox.setEnabled(settableBaudRate);
        baudRateLabel.setEnabled(settableBaudRate);
        */
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
    public final void setPortName(String portName) {/*
        if (portName == null || portName.isEmpty())
            return;

        openToggleButton.setEnabled(hardware != null);
        String oldPort = this.portName;
        this.portName = portName;
        // this propery changer should set up the hardware and call setHardware()
        propertyChangeSupport.firePropertyChange(PROP_PORT, oldPort, portName);
  */  }

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

    private void setBaudRateUnconditionally(int baudRate) {/*
        int oldBaud = this.baudRate;
        this.baudRate = baudRate;
        this.baudComboBox.setSelectedItem(Integer.toString(baudRate));
        propertyChangeSupport.firePropertyChange(PROP_BAUD, oldBaud, baudRate);*/
    }

    public void setHardware(IHarcHardware hardware) {/*
        this.hardware = hardware;
        openToggleButton.setEnabled(hardware != null);
        openToggleButton.setSelected(hardware.isValid());
        setVersion();*/
    }

    /**
     * @return the version
     */
    public String getVersion() {
        return version;
    }

    private void setVersion(String version) {/*
        java.lang.String oldVersion = this.version;
        this.version = version;
        versionLabel.setEnabled(hardware.isValid());
        versionLiteralLabel.setEnabled(hardware.isValid());
        versionLabel.setText(version);
        propertyChangeSupport.firePropertyChange(PROP_VERSION, oldVersion, version);*/
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

    public void setup(String desiredPort) throws IOException {/*
        ComboBoxModel model = portComboBox.getModel();
        if (model == null || model.getSize() == 0 || ((model.getSize() == 1) && ((String)portComboBox.getSelectedItem()).equals(notInitialized)))
            setupPortComboBox(true);

        portComboBox.setSelectedItem(desiredPort != null ? desiredPort : portName);*/
    }

    private void setupPortComboBox(boolean useCached) throws IOException {/*
        if (hardware != null)
            hardware.close();

        ArrayList<String> portNames = LocalSerialPort.getSerialPortNames(useCached);
        portNames.add(0, "");
        DefaultComboBoxModel<String> model = new DefaultComboBoxModel<String>(portNames.toArray(new String[portNames.size()]));
        portComboBox.setModel(model);*/
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

    private void enableStuff(boolean isOpen) {/*
        baudComboBox.setEnabled(!isOpen && settableBaudRate);
        portComboBox.setEnabled(!isOpen);*/
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

        comboTabbedPane = new javax.swing.JTabbedPane();
        serialPanel = new javax.swing.JPanel();
        serialPortSimpleBean1 = new org.harctoolbox.guicomponents.SerialPortSimpleBean();
        tcpPanel = new javax.swing.JPanel();
        internetHostPanel1 = new org.harctoolbox.guicomponents.InternetHostPanel();

        setPreferredSize(new java.awt.Dimension(800, 80));

        javax.swing.GroupLayout serialPanelLayout = new javax.swing.GroupLayout(serialPanel);
        serialPanel.setLayout(serialPanelLayout);
        serialPanelLayout.setHorizontalGroup(
            serialPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(serialPanelLayout.createSequentialGroup()
                .addComponent(serialPortSimpleBean1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, Short.MAX_VALUE))
        );
        serialPanelLayout.setVerticalGroup(
            serialPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(serialPanelLayout.createSequentialGroup()
                .addComponent(serialPortSimpleBean1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 11, Short.MAX_VALUE))
        );

        comboTabbedPane.addTab("Serial", serialPanel);

        javax.swing.GroupLayout tcpPanelLayout = new javax.swing.GroupLayout(tcpPanel);
        tcpPanel.setLayout(tcpPanelLayout);
        tcpPanelLayout.setHorizontalGroup(
            tcpPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(tcpPanelLayout.createSequentialGroup()
                .addComponent(internetHostPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 137, Short.MAX_VALUE))
        );
        tcpPanelLayout.setVerticalGroup(
            tcpPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(tcpPanelLayout.createSequentialGroup()
                .addComponent(internetHostPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, Short.MAX_VALUE))
        );

        comboTabbedPane.addTab("TCP", tcpPanel);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(comboTabbedPane)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(comboTabbedPane, javax.swing.GroupLayout.DEFAULT_SIZE, 127, Short.MAX_VALUE)
        );
    }// </editor-fold>//GEN-END:initComponents

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JTabbedPane comboTabbedPane;
    private org.harctoolbox.guicomponents.InternetHostPanel internetHostPanel1;
    private javax.swing.JPanel serialPanel;
    private org.harctoolbox.guicomponents.SerialPortSimpleBean serialPortSimpleBean1;
    private javax.swing.JPanel tcpPanel;
    // End of variables declaration//GEN-END:variables
}
