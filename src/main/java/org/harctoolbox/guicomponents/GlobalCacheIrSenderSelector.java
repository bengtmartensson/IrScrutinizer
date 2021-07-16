/*
 Copyright (C) 2013, 2021 Bengt Martensson.

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
import java.beans.PropertyChangeEvent;
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
import org.harctoolbox.harchardware.ir.GlobalCache;
import org.harctoolbox.harchardware.ir.NoSuchTransmitterException;
import org.harctoolbox.ircore.InvalidArgumentException;
import org.harctoolbox.ircore.IrSignal;
import org.harctoolbox.ircore.ModulatedIrSequence;
import org.harctoolbox.irscrutinizer.HardwareUnavailableException;

public final class GlobalCacheIrSenderSelector extends HardwareBean /*javax.swing.JPanel implements ISendingHardware<GlobalCache>, ISendingReceivingBean*/ {

//    private GuiUtils guiUtils;
//    private transient GlobalCache globalCache;
//    private boolean verbose;
//    private int timeout;

    private InetAddress inetAddress;
    private int module;
    private int port;
//    private boolean senderSupport; // TODO: remove
    private final boolean openOnSelect = false; // To implement (or not?)

//    private final PropertyChangeSupport propertyChangeSupport;
//    public static final String PROP_MODULE = "PROP_MODULE";
//    public static final String PROP_PORT = "PROP_PORT";
//    private static final int DEFAULT_TIMEOUT = 5000;

    private void setGlobalCache(InetAddress globalCacheInetAddress) throws UnknownHostException, IOException {
        if (globalCacheInetAddress == null)
            return;
        if (hardware != null && hardware.isValid() && globalCacheInetAddress.equals(this.inetAddress))
            return;

        String old = inetAddress != null ? inetAddress.getHostAddress() : "";
        if (!globalCacheInetAddress.equals(inetAddress)) {
            close();
            inetAddress = globalCacheInetAddress;
            if (openOnSelect)
                open();
        }

        GlobalCacheManager.getInstance().addManualGlobalCache(inetAddress);
        openCloseToggleButton.setEnabled(true);
        enableStuff();
        propertyChangeSupport.firePropertyChange(PROP_IPNAME, old, inetAddress.getHostAddress());
    }

    private void assertValidModule() {
        if (hardware == null)
            return;

        List<Integer> irModules = ((GlobalCache) hardware).getIrModules();
        if (! irModules.contains(module))
            module = irModules.get(0);
    }

    private void enableStuff() {
        enableStuff(isOpen());
    }

    private void enableStuff(boolean open) {
        int index = GlobalCacheManager.getInstance().getIndex(inetAddress);
        if (index != globalCacheIpComboBox.getSelectedIndex())
            globalCacheIpComboBox.setSelectedIndex(index);
        String type = GlobalCacheManager.getInstance().getType(index);
        typeLabel.setText(/*open ? */type/* : "<unknown>"*/);
        addButton.setEnabled(!open);
        globalCacheIpComboBox.setEnabled(!open);
        globalCacheBrowseButton.setEnabled(inetAddress != null);
        if (open) {
            List<Integer> irModules = ((GlobalCache) hardware).getIrModules();
            String[] modules;
            if (irModules.isEmpty())
                modules = new String[]{"-"};
            else {
                modules = new String[irModules.size()];
                for (int i = 0; i < irModules.size(); i++)
                    modules[i] = Integer.toString(irModules.get(i));
            }
            moduleComboBox.setModel(new DefaultComboBoxModel<>(modules));
        }
        moduleComboBox.setSelectedItem(Integer.toString(module));
        moduleComboBox.setEnabled(open);

        setNumberOfPortsForModule(module);
        portComboBox.setSelectedItem(Integer.toString(port));
        portComboBox.setEnabled(open);
        openCloseToggleButton.setSelected(open);
    }

    private void setGlobalCache(int index) throws UnknownHostException, IOException {
        setGlobalCache(GlobalCacheManager.getInstance().getInetAddress(index));
    }

    public void setGlobalCache(String ipName) throws UnknownHostException, IOException {
        setGlobalCache(InetAddress.getByName(ipName));
    }
//    @Override
//    public void addPropertyChangeListener(PropertyChangeListener listener) {
//        if (propertyChangeSupport != null)
//            propertyChangeSupport.addPropertyChangeListener(listener);
//    }
//
//    @Override
//    public void removePropertyChangeListener(PropertyChangeListener listener) {
//        propertyChangeSupport.removePropertyChangeListener(listener);
//    }

    /**
     * @return the ipName
     */
    public String getIpName() {
        return inetAddress != null ? inetAddress.getHostName() : null;
    }

    private void updateGlobalCacheList() {
        ComboBoxModel<String> cbm = new DefaultComboBoxModel<>(GlobalCacheManager.getInstance().getAllNames());
        globalCacheIpComboBox.setModel(cbm);
        int index = GlobalCacheManager.getInstance().getIndex(inetAddress);
        globalCacheIpComboBox.setSelectedIndex(index);
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
//        if (senderSupport) {
            int oldModule = this.module;
            this.module = module;
            setNumberOfPortsForModule(module);
            propertyChangeSupport.firePropertyChange(PROP_MODULE, oldModule, module);
//        }
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
//        if (senderSupport) {
            int oldPort = this.port;
            this.port = port;
            propertyChangeSupport.firePropertyChange(PROP_PORT, oldPort, port);
//        }
    }

    private void setNumberOfPortsForModule(int module) {
        if (hardware != null) {
            int number = ((GlobalCache) hardware).getModuleSecondNumber(module);
            setNumberOfPorts(number);
        }
    }

    private void setNumberOfPorts(int ports) {
        String[] arr = new String[ports];
        for (int i = 0; i < ports; i++)
            arr[i] = Integer.toString(i+1);
        portComboBox.setModel(new javax.swing.DefaultComboBoxModel<>(arr));
    }

    /**
     *
     * @return
     * @throws NoSuchTransmitterException
     */
//    @Override
    public GlobalCache.GlobalCacheIrTransmitter getTransmitter() throws NoSuchTransmitterException {
        return ((GlobalCache) hardware).newTransmitter(module, port);
    }

    /**
     * Creates new form GlobalCacheIrSenderSelector
     * @param guiUtils
     * @param verbose
     * @param timeout
     * @param senderSupport
     */
//    // FIXME
//    public GlobalCacheIrSenderSelector(GuiUtils guiUtils, boolean verbose, int timeout, boolean senderSupport) {
//        this(guiUtils, verbose, timeout);
//    }

    public GlobalCacheIrSenderSelector(GuiUtils guiUtils, boolean verbose, int timeout) {
        super(guiUtils, verbose, timeout);
//        this.propertyChangeSupport = new java.beans.PropertyChangeSupport(this);
//        this.guiUtils = guiUtils;
//        this.verbose = verbose;
//        this.timeout = timeout;
//        this.senderSupport = senderSupport;
//        this.globalCache = null;
        this.port = GlobalCache.defaultGlobalCachePort;
        initComponents();
//        this.moduleLabel.setVisible(true);
//        this.moduleComboBox.setVisible(true);
//        this.portLabel.setVisible(true);
//        this.portComboBox.setVisible(true);
//        this.stopButton.setVisible(true);
        updateGlobalCacheList();

        GlobalCacheManager.getInstance().addPropertyChangeListener((PropertyChangeEvent evt) -> {
            if (evt.getPropertyName().equals(GlobalCacheManager.PROP_GCMANAGER_NAME))
                updateGlobalCacheList();
        });
    }

    public GlobalCacheIrSenderSelector() {
        this(null, false, GlobalCache.DEFAULT_BEGIN_TIMEOUT);
    }

    public boolean isStopEnabled() {
        return stopButton.isEnabled();
    }

    public void setStopEnabled(boolean value) {
        stopButton.setEnabled(value);
    }

    @Override
    public boolean canSend() {
        return true;
    }

    @Override
    public boolean sendIr(IrSignal irSignal, int count) throws NoSuchTransmitterException, IOException, HardwareUnavailableException, HarcHardwareException, InvalidArgumentException {
        assertHardwareValid();
        return ((GlobalCache) hardware).sendIr(irSignal, count, getTransmitter());
    }

    @Override
    public String getName() {
        return "Global Caché";
    }

//    @Override
    public void setup() throws IOException, HarcHardwareException {
        if (inetAddress != null)
            open();
    }

    @Override
    public void open() throws IOException {
        boolean oldIsOpen = isOpen();
        if (isOpen())
            close();
        hardware = new GlobalCache(inetAddress.getHostAddress(), verbose, timeout);
        if (!hardware.isValid())
            throw new IOException("Set up of GlobalCache@" + inetAddress.getHostName() + " failed.");
        String version = hardware.getVersion();
        globalCacheVersionLabel.setText(version);
        assertValidModule();
        propertyChangeSupport.firePropertyChange(PROP_ISOPEN, oldIsOpen, isOpen());
        enableStuff();
    }

//    @Override
//    public JPanel getPanel() {
//        return (JPanel) getParent();
//    }

    @Override
    public void close() {
        if (hardware != null) {
            try {
                boolean oldIsOpen = isOpen();
                hardware.close();
                hardware = null;
                globalCacheVersionLabel.setText(null);
                propertyChangeSupport.firePropertyChange(PROP_ISOPEN, oldIsOpen, isOpen());
                if (verbose)
                    guiUtils.message("Closed GlobalCache");
            } catch (IOException ex) {
                guiUtils.error(ex);
            }
        }
        enableStuff();
    }

    @Override
    public boolean canCapture() {
        return true;
    }

    @Override
    public ModulatedIrSequence capture() throws HarcHardwareException, InvalidArgumentException {
        if (hardware == null)
            return null;

        return ((GlobalCache) hardware).capture();
    }

//    @Override
//    public GlobalCache getRawIrSender() {
//        return globalCache;
//    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        globalCacheIpComboBox = new javax.swing.JComboBox<>();
        moduleComboBox = new javax.swing.JComboBox<>();
        portComboBox = new javax.swing.JComboBox<>();
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
        openCloseToggleButton = new javax.swing.JToggleButton();

        globalCacheIpComboBox.setToolTipText("Used to select between several Global Caché units.");
        globalCacheIpComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                globalCacheIpComboBoxActionPerformed(evt);
            }
        });

        moduleComboBox.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "1", "2", "3", "4", "5" }));
        moduleComboBox.setMinimumSize(new java.awt.Dimension(60, 24));
        moduleComboBox.setPreferredSize(new java.awt.Dimension(60, 24));
        moduleComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                moduleComboBoxActionPerformed(evt);
            }
        });

        portComboBox.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "1", "2", "3" }));
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

        openCloseToggleButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/Crystal-Clear/22x22/actions/connect_creating.png"))); // NOI18N
        openCloseToggleButton.setText("Open/Close");
        openCloseToggleButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                openCloseToggleButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel1)
                            .addComponent(globalCacheIpComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, 226, javax.swing.GroupLayout.PREFERRED_SIZE))
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
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(openCloseToggleButton)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
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
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(openCloseToggleButton)
                .addContainerGap(25, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents

    private void globalCacheBrowseButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_globalCacheBrowseButtonActionPerformed
        URI uri = hardware != null
                ? ((GlobalCache) hardware).getUri(null, null)
                : URI.create("http://" + GlobalCacheManager.getInstance().getInetAddress(globalCacheIpComboBox.getSelectedIndex()).getHostAddress());
        guiUtils.browse(uri);
    }//GEN-LAST:event_globalCacheBrowseButtonActionPerformed

    private void addButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addButtonActionPerformed
        String ip = guiUtils.getInput("Enter GlobalCache IP-Name or -address", "GlobalCache entry", GlobalCache.DEFAULT_IP);
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

    private void openCloseToggleButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_openCloseToggleButtonActionPerformed
        Cursor oldCursor = setBusyCursor();
        try {
            openClose(! isOpen());
        } catch (IOException | HarcHardwareException ex) {
            guiUtils.error(ex);
            enableStuff();
        } finally {
            openCloseToggleButton.setSelected(isOpen());
            resetCursor(oldCursor);
        }
    }//GEN-LAST:event_openCloseToggleButtonActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton addButton;
    private javax.swing.JButton globalCacheBrowseButton;
    private javax.swing.JComboBox<String> globalCacheIpComboBox;
    private javax.swing.JLabel globalCacheVersionLabel;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JComboBox<String> moduleComboBox;
    private javax.swing.JLabel moduleLabel;
    private javax.swing.JToggleButton openCloseToggleButton;
    private javax.swing.JComboBox<String> portComboBox;
    private javax.swing.JLabel portLabel;
    private javax.swing.JButton stopButton;
    private javax.swing.JLabel typeLabel;
    // End of variables declaration//GEN-END:variables
}
