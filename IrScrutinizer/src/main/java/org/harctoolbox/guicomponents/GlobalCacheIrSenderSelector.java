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

package org.harctoolbox.guicomponents;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import org.harctoolbox.harchardware.ir.GlobalCache;
import org.harctoolbox.harchardware.ir.NoSuchTransmitterException;

public class GlobalCacheIrSenderSelector extends javax.swing.JPanel {
    private static final long serialVersionUID = 1L;

    private GuiUtils guiUtils;
    private transient GlobalCache globalCache;
    private boolean verbose;
    private int timeOut;

    private InetAddress inetAddress;
    private int module;
    private int port;
    private boolean senderSupport;

    private final transient PropertyChangeSupport propertyChangeSupport = new java.beans.PropertyChangeSupport(this);
    public static final String PROP_GLOBALCACHE = "PROP_GLOBALCACHE";
    public static final String PROP_IPNAME = "PROP_IPNAME";
    public static final String PROP_MODULE = "PROP_MODULE";
    public static final String PROP_PORT = "PROP_PORT";
    private static final int defaultTimeout = 5000;

    /**
     * @param timeOut the timeOut to set
     */
    public void setTimeOut(int timeOut) {
        this.timeOut = timeOut;
    }

    private void setGlobalCache(InetAddress globalCacheInetAddress) throws UnknownHostException, IOException {
        if (globalCacheInetAddress == null)
            return;
        if (globalCache != null && globalCache.isValid() && globalCacheInetAddress.equals(InetAddress.getByName(globalCache.getIp())))
            return;

        String oldIpName = getIpName();
        closeGlobalCache();
        globalCache = null;
        typeLabel.setText("<unknown>");
        moduleComboBox.setEnabled(false);
        portComboBox.setEnabled(false);

        globalCache = new GlobalCache(globalCacheInetAddress.getHostName(), verbose, timeOut);

        if (!globalCache.isValid())
            throw new IOException("Set up of GlobalCache@" + globalCacheInetAddress.getHostName() + " failed.");

        inetAddress = globalCacheInetAddress;
        GlobalCacheManager.getInstance().addManualGlobalCache(inetAddress);
        setup();
        int index = GlobalCacheManager.getInstance().getIndex(globalCacheInetAddress);
        String type = GlobalCacheManager.getInstance().getType(index);
        typeLabel.setText(type);
        globalCacheBrowseButton.setEnabled(true);
        ArrayList<Integer> irModules = globalCache.getIrModules();
        this.port = GlobalCache.defaultGlobalCachePort;
        String[] modules;
        if (irModules.isEmpty())
            modules = new String[]{"-"};
        else {
            modules = new String[irModules.size()];
            for (int i = 0; i < irModules.size(); i++) {
                modules[i] = Integer.toString(irModules.get(i));
            }
            setModule(irModules.get(0));
        }
        moduleComboBox.setModel(new DefaultComboBoxModel(modules));
        moduleComboBox.setEnabled(true);
        portComboBox.setEnabled(true);
        globalCacheVersionLabel.setText(globalCache.getVersion());
        globalCacheIpComboBox.setSelectedIndex(index);
        globalCacheIpComboBox.repaint();

        propertyChangeSupport.firePropertyChange(PROP_IPNAME, oldIpName, getIpName());
    }

    private void setGlobalCache(int index) throws UnknownHostException, IOException {
            setGlobalCache(GlobalCacheManager.getInstance().getInetAddress(index));
    }

    private void closeGlobalCache() throws IOException {
        if (globalCache != null)
            globalCache.close();
    }

    @Override
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        if (propertyChangeSupport != null)
            propertyChangeSupport.addPropertyChangeListener(listener);
    }

    @Override
    public void removePropertyChangeListener(PropertyChangeListener listener) {
        propertyChangeSupport.removePropertyChangeListener(listener);
    }

    /**
     * @param verbose the verbose to set
     */
    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

    /**
     * @return the ipName
     */
    public String getIpName() {
        return inetAddress != null ? inetAddress.getHostName() : null;
    }

    /**
     * @param ipName the ipName to set
     * @throws UnknownHostException
     * @throws IOException
     */
    public final void setIpName(String ipName) throws UnknownHostException, IOException {
        if (ipName == null || ipName.isEmpty() || ipName.equals(getIpName()))
            return;

        //String oldIpName = ipName;
        inetAddress = InetAddress.getByName(ipName);
        setGlobalCache(inetAddress);
        //propertyChangeSupport.firePropertyChange(PROP_IPNAME, oldIpName, ipName);
    }

    private void setup() {
        ComboBoxModel cbm = new DefaultComboBoxModel(GlobalCacheManager.getInstance().getAllNames());
        globalCacheIpComboBox.setModel(cbm);
        int index = GlobalCacheManager.getInstance().getIndex(inetAddress);
        globalCacheIpComboBox.setSelectedIndex(index);
        globalCacheIpComboBox.repaint();
    }

    /**
     * @return the module
     */
    public int getModule() {
        return module;
    }

    /**
     * @param module the module to set
     */
    public void setModule(int module) {
        if (senderSupport) {
            int oldModule = this.module;
            this.module = module;
            propertyChangeSupport.firePropertyChange(PROP_MODULE, oldModule, module);
        }
    }

    /**
     * @return the port
     */
    public int getPort() {
        return port;
    }

    /**
     * @param port the port to set
     */
    public void setPort(int port) {
        if (senderSupport) {
            int oldPort = this.port;
            this.port = port;
            propertyChangeSupport.firePropertyChange(PROP_PORT, oldPort, port);
        }
    }

    /**
     * @return the stopButton
     * /
    public javax.swing.JButton getStopButton() {
        return stopButton;
    }*/

    /**
     *
     * @return
     * @throws NoSuchTransmitterException
     */
    public GlobalCache.GlobalCacheIrTransmitter getTransmitter() throws NoSuchTransmitterException {
        return globalCache.newTransmitter(module, port);
    }

    /**
     * @param stopButton the stopButton to set
     * /
    public void setStopButton(javax.swing.JButton stopButton) {
        javax.swing.JButton oldStopButton = this.stopButton;
        this.stopButton = stopButton;
        propertyChangeSupport.firePropertyChange(PROP_STOPBUTTON, oldStopButton, stopButton);
    }*/

    /**
     * Creates new form GlobalCacheIrSenderSelector
     * @param guiUtils
     * @param verbose
     * @param timeOut
     * @param senderSupport
     */
    public GlobalCacheIrSenderSelector(final GuiUtils guiUtils, final boolean verbose, int timeOut, boolean senderSupport) {
        this.globalCache = null;
        this.guiUtils = guiUtils;
        this.verbose = verbose;
        this.timeOut = timeOut;
        this.senderSupport = senderSupport;
        initComponents();
        this.moduleLabel.setVisible(senderSupport);
        this.moduleComboBox.setVisible(senderSupport);
        this.portLabel.setVisible(senderSupport);
        this.portComboBox.setVisible(senderSupport);
        this.stopButton.setVisible(senderSupport);
        setup();

        GlobalCacheManager.getInstance().addPropertyChangeListener(new PropertyChangeListener() {

            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                setup();
            }
        });
    }

    public GlobalCacheIrSenderSelector() {
        this(null, false, defaultTimeout, true);
        this.port = GlobalCache.defaultGlobalCachePort;
        this.globalCache = null;
    }

    public void setGuiUtils(GuiUtils guiUtils) {
        this.guiUtils = guiUtils;
    }

    public boolean isStopEnabled() {
        return stopButton.isEnabled();
    }

    public void setStopEnabled(boolean value) {
        stopButton.setEnabled(value);
    }

    public GlobalCache getGlobalCache() {
        return globalCache;
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        globalCacheIpComboBox = new javax.swing.JComboBox();
        moduleComboBox = new javax.swing.JComboBox();
        portComboBox = new javax.swing.JComboBox();
        stopButton = new javax.swing.JButton();
        globalCacheBrowseButton = new javax.swing.JButton();
        jLabel1 = new javax.swing.JLabel();
        moduleLabel = new javax.swing.JLabel();
        portLabel = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        typeLabel = new javax.swing.JLabel();
        addButton = new javax.swing.JButton();
        jLabel5 = new javax.swing.JLabel();
        globalCacheVersionLabel = new javax.swing.JLabel();

        globalCacheIpComboBox.setToolTipText("Used to select between several Global Caché units.");
        globalCacheIpComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                globalCacheIpComboBoxActionPerformed(evt);
            }
        });

        moduleComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "1", "2", "3", "4", "5" }));
        moduleComboBox.setMinimumSize(new java.awt.Dimension(60, 24));
        moduleComboBox.setPreferredSize(new java.awt.Dimension(60, 24));
        moduleComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                moduleComboBoxActionPerformed(evt);
            }
        });

        portComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "1", "2", "3" }));
        portComboBox.setMinimumSize(new java.awt.Dimension(60, 24));
        portComboBox.setPreferredSize(new java.awt.Dimension(60, 24));
        portComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                portComboBoxActionPerformed(evt);
            }
        });

        stopButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/Crystal-Clear/22x22/actions/stop.png"))); // NOI18N
        stopButton.setText("Stop IR");
        stopButton.setToolTipText("Stop presently not implemented.");
        stopButton.setEnabled(false);

        globalCacheBrowseButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/Crystal-Clear/22x22/apps/browser.png"))); // NOI18N
        globalCacheBrowseButton.setText("Browse");
        globalCacheBrowseButton.setToolTipText("Visit the GlobalCache with browser");
        globalCacheBrowseButton.setEnabled(false);
        globalCacheBrowseButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                globalCacheBrowseButtonActionPerformed(evt);
            }
        });

        jLabel1.setText("IP Name/Address");

        moduleLabel.setText("Module");

        portLabel.setText("Port");

        jLabel4.setText("Reported Global Caché type:");

        typeLabel.setText("<unknown>");

        addButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/Crystal-Clear/22x22/actions/edit_add.png"))); // NOI18N
        addButton.setText("Add...");
        addButton.setToolTipText("The IP-Address or -name of Global Caché units not supporting the beacon can be entered here.");
        addButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addButtonActionPerformed(evt);
            }
        });

        jLabel5.setText("Firmware version:");

        globalCacheVersionLabel.setText("<unknown>");
        globalCacheVersionLabel.setToolTipText("Firmware version as reported by the device.");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(globalCacheIpComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, 180, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel1))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(moduleLabel)
                            .addComponent(moduleComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, 60, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(12, 12, 12)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(portComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, 60, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(stopButton)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(globalCacheBrowseButton)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(addButton))
                            .addComponent(portLabel)))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jLabel4)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(typeLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 146, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel5)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(globalCacheVersionLabel)))
                .addGap(9, 9, 9))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(13, 13, 13)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(moduleLabel)
                    .addComponent(portLabel))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(globalCacheIpComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(moduleComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(portComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(stopButton)
                    .addComponent(globalCacheBrowseButton)
                    .addComponent(addButton))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel4)
                    .addComponent(typeLabel)
                    .addComponent(jLabel5)
                    .addComponent(globalCacheVersionLabel))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents

    private void globalCacheBrowseButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_globalCacheBrowseButtonActionPerformed
        if (globalCache != null)
            guiUtils.browse(globalCache.getUri(null, null));
    }//GEN-LAST:event_globalCacheBrowseButtonActionPerformed

    private void addButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addButtonActionPerformed
        String ip = guiUtils.getInput("Enter GlobalCache IP-Name or -address", "GlobalCache entry", GlobalCache.defaultGlobalCacheIP);
        try {
            if (ip != null) {
                GlobalCacheManager.getInstance().addManualGlobalCache(InetAddress.getByName(ip));
                setGlobalCache(InetAddress.getByName(ip));
            }
        } catch (UnknownHostException ex) {
            guiUtils.error(ex);
        } catch (IOException ex) {
            guiUtils.error(ex);
        }
    }//GEN-LAST:event_addButtonActionPerformed

    private void globalCacheIpComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_globalCacheIpComboBoxActionPerformed
        try {
            setGlobalCache(globalCacheIpComboBox.getSelectedIndex());
        } catch (UnknownHostException ex) {
            guiUtils.error(ex);
        } catch (IOException ex) {
            guiUtils.error(ex);
        }
    }//GEN-LAST:event_globalCacheIpComboBoxActionPerformed

    private void moduleComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_moduleComboBoxActionPerformed
        setModule(Integer.parseInt((String) moduleComboBox.getSelectedItem()));
    }//GEN-LAST:event_moduleComboBoxActionPerformed

    private void portComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_portComboBoxActionPerformed
        setPort(Integer.parseInt((String)portComboBox.getSelectedItem()));
    }//GEN-LAST:event_portComboBoxActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton addButton;
    private javax.swing.JButton globalCacheBrowseButton;
    private javax.swing.JComboBox globalCacheIpComboBox;
    private javax.swing.JLabel globalCacheVersionLabel;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JComboBox moduleComboBox;
    private javax.swing.JLabel moduleLabel;
    private javax.swing.JComboBox portComboBox;
    private javax.swing.JLabel portLabel;
    private javax.swing.JButton stopButton;
    private javax.swing.JLabel typeLabel;
    // End of variables declaration//GEN-END:variables
}
