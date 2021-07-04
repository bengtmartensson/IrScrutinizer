/*
Copyright (C) 2013, 2014, 2015 Bengt Martensson.

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
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import javax.swing.JPanel;
import org.harctoolbox.harchardware.IHarcHardware;
import org.harctoolbox.harchardware.ir.LircClient;

public final class InternetHostPanel extends JPanel {
    public static final String PROP_IP_NAME = "ipName";
    public static final String PROP_PORT_NUMBER = "portNumber";
    public static final String PROP_READY = "PROP_READY";
    public static final String PROP_VERSION = "PROP_VERSION";
    public static final String PROP_ISOPEN = "PROP_ISOPEN";

    public static final int PING_TIMEOUT = 5000;
    public static final String INITIAL_IPNAME=  "localhost";

    private boolean usePort;
    private boolean usePing;
    private boolean useBrowse;
    private String ipName;
    private int portNumber;
    private boolean ready;
    private String version;
    private GuiUtils guiUtils;
    private IHarcHardware hardware;

    private final PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);

    public InternetHostPanel() {
        this(null, true, true, true);
    }

    /**
     * Creates new form InternetHostPanel
     * @param guiUtils
     * @param usePort
     * @param usePing
     * @param useBrowse
     */
    public InternetHostPanel(GuiUtils guiUtils, boolean usePort, boolean usePing, boolean useBrowse) {
        this.guiUtils = guiUtils;
        this.useBrowse = useBrowse;
        this.usePing = usePing;
        this.usePort = usePort;
        ipName = INITIAL_IPNAME;
        portNumber = LircClient.lircDefaultPort;
        ready = false;
        initComponents();
        setUsePort(usePort);
        portTextField.setEnabled(usePort);
        pingButton.setEnabled(usePing);
        browseButton.setEnabled(useBrowse);
    }

    public void setUsePort(boolean value) {
        usePort = value;
        portTextField.setEnabled(value);
        portLabel.setText(value ? "Port" : null);
    }

    public boolean getUsePort() {
        return usePort;
    }

    public String getIpName() {
        return ipName;
    }

    /**
     * @return the ready
     */
    public boolean isReady() {
        return ready;
    }

    public void setBrowsingEnabled(boolean enable) {
        browseButton.setEnabled(enable);
    }

    public void setIpName(String name) {
        if (name == null || name.equals(ipName))
            return;

        String oldIpName = ipName;
        ipName = name;

        ipNameTextField.setText(name);
        propertyChangeSupport.firePropertyChange(PROP_IP_NAME, oldIpName, name);
    }

    public int getPortNumber() {
        return portNumber;
    }

    public void setPortNumber(int value) {
        int oldPortNumber = portNumber;
        portNumber = value;
        portTextField.setText(Integer.toString(value));
        if (usePort) {
            propertyChangeSupport.firePropertyChange(PROP_PORT_NUMBER, oldPortNumber, value);
        }
    }

    public boolean isPingable() {
        boolean success = false;
        String host = getIpName();
        try {
            success = InetAddress.getByName(host).isReachable(PING_TIMEOUT);
            guiUtils.info(host + (success ? " is reachable" : " is not reachable (using Java's isReachable)"));
        } catch (IOException ex) {
            guiUtils.info(host + " is not reachable (using Java's isReachable): " + ex.getMessage());
        }
        return success;
    }

    public void setHardware(IHarcHardware hardware) throws IOException {
        this.hardware = hardware;
        //openToggleButton.setEnabled(hardware != null);
        //openToggleButton.setSelected(hardware.isValid());
        setVersion();
    }

    /**
     * @return the version
     */
    public String getVersion() {
        return version;
    }

    private void setVersion() throws IOException {
        try {
            setVersion(hardware.isValid() ? hardware.getVersion() : "<not connected>");
        } catch (IOException ex) {
            versionLabel.setText("<invalid>");
            version = null;
            throw ex;
        }
    }

    public void setVersion(String version) {
        String oldVersion = this.version;
        this.version = version;
        //versionLabel.setEnabled(hardware.isValid());
        //versionLiteralLabel.setEnabled(hardware.isValid());
        versionLabel.setText(version);
        versionLabel.setEnabled(true);
        versionLiteralLabel.setEnabled(true);
        propertyChangeSupport.firePropertyChange(PROP_VERSION, oldVersion, version);
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
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        ipNameTextField = new javax.swing.JTextField();
        portTextField = new javax.swing.JTextField();
        browseButton = new javax.swing.JButton();
        pingButton = new javax.swing.JButton();
        jLabel1 = new javax.swing.JLabel();
        portLabel = new javax.swing.JLabel();
        versionLiteralLabel = new javax.swing.JLabel();
        versionLabel = new javax.swing.JLabel();

        ipNameTextField.setText(getIpName());
        ipNameTextField.setToolTipText("IP-Name or -address of host");
        ipNameTextField.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                ipNameTextFieldFocusLost(evt);
            }
        });
        ipNameTextField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ipNameTextFieldActionPerformed(evt);
            }
        });

        portTextField.setText(Integer.toString(getPortNumber()));
        portTextField.setToolTipText("Portnumber");
        portTextField.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                portTextFieldFocusLost(evt);
            }
        });
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

        jLabel1.setText("IP Name/Address");

        portLabel.setText("Port");

        versionLiteralLabel.setText("Version:");
        versionLiteralLabel.setToolTipText("Version of the firmware on the device, if supported by the device. Verifies that the connection is working.");
        versionLiteralLabel.setEnabled(false);

        versionLabel.setText("no version available");
        versionLabel.setToolTipText("Version of firmware on device. Verifies the connection.");
        versionLabel.setBorder(new javax.swing.border.SoftBevelBorder(javax.swing.border.BevelBorder.LOWERED));
        versionLabel.setEnabled(false);
        versionLabel.setHorizontalTextPosition(javax.swing.SwingConstants.LEFT);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(0, 0, 0)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel1)
                    .addComponent(ipNameTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 138, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(portLabel)
                    .addComponent(portTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 56, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(browseButton)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(pingButton)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(versionLiteralLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(versionLabel, javax.swing.GroupLayout.DEFAULT_SIZE, 213, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(portLabel))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(ipNameTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(portTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(browseButton)
                    .addComponent(pingButton)
                    .addComponent(versionLiteralLabel)
                    .addComponent(versionLabel))
                .addContainerGap(23, Short.MAX_VALUE))
        );

        layout.linkSize(javax.swing.SwingConstants.VERTICAL, new java.awt.Component[] {browseButton, ipNameTextField, pingButton, portTextField});

    }// </editor-fold>//GEN-END:initComponents

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
        guiUtils.browse(URI.create("http://" + this.getIpName()));
    }//GEN-LAST:event_browseButtonActionPerformed

    private void pingButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_pingButtonActionPerformed
        isPingable();
    }//GEN-LAST:event_pingButtonActionPerformed

    private void ipNameTextFieldFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_ipNameTextFieldFocusLost
        ipNameTextFieldActionPerformed(null);
    }//GEN-LAST:event_ipNameTextFieldFocusLost

    private void portTextFieldFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_portTextFieldFocusLost
        portTextFieldActionPerformed(null);
    }//GEN-LAST:event_portTextFieldFocusLost

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton browseButton;
    private javax.swing.JTextField ipNameTextField;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JButton pingButton;
    private javax.swing.JLabel portLabel;
    private javax.swing.JTextField portTextField;
    private javax.swing.JLabel versionLabel;
    private javax.swing.JLabel versionLiteralLabel;
    // End of variables declaration//GEN-END:variables
}
