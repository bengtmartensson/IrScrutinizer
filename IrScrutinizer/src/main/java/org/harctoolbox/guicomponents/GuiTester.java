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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.harctoolbox.IrpMaster.IncompatibleArgumentException;
import org.harctoolbox.IrpMaster.IrSignal;
import org.harctoolbox.IrpMaster.IrpMaster;
import org.harctoolbox.IrpMaster.IrpMasterException;
import org.harctoolbox.IrpMaster.Iterate.RandomValueSet;
import org.harctoolbox.IrpMaster.ParseException;
import org.harctoolbox.IrpMaster.UnassignedException;
import org.harctoolbox.girr.Command;
import org.harctoolbox.harchardware.ir.GlobalCache;
import org.harctoolbox.harchardware.ir.IrTransIRDB;
import org.harctoolbox.harchardware.ir.LircClient;
import org.harctoolbox.harchardware.ir.NoSuchTransmitterException;

public class GuiTester extends javax.swing.JFrame {
    private boolean verbose = true;
    private final LookAndFeelManager lookAndFeelManager;
    private static final String helpText = "Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam erat, sed diam voluptua. At vero eos et accusam et justo duo dolores et ea rebum. Stet clita kasd gubergren, no sea takimata sanctus est Lorem ipsum dolor sit amet. Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam erat, sed diam voluptua. At vero eos et accusam et justo duo dolores et ea rebum. Stet clita kasd gubergren, no sea takimata sanctus est Lorem ipsum dolor sit amet.";

    private GuiUtils guiUtils = new GuiUtils(this, "tester", 1000);
    private IrTransIRDB irtrans;
    private final GlobalCacheManagerMenu globalCacheManagerMenu;
    private IrpMaster irpMaster;

    private class TestCaller implements LookAndFeelManager.ILookAndFeelManagerCaller {
        @Override
        public void err(Exception ex, String str) {
            guiUtils.error(ex, str);
        }

        @Override
        public void setLAFProperty(int index) {
            //properties.setLookAndFeel(index);
        }
    }

    private void setupLirc() throws IOException {
        try {
            LircClient lircClient = new LircClient(internetHostPanel1.getIpName(), internetHostPanel1.getPortNumber(), verbose, 5000);
            lircNamedCommandLauncher.setHardware(lircClient);
        } catch (UnknownHostException ex) {
            guiUtils.error("unknown host: " + internetHostPanel1.getIpName());
        }
    }

    private void setupIrTrans() throws IOException {
        try {
            irtrans = new IrTransIRDB(internetHostPanel2.getIpName());
            irtransNamedCommandLauncher.setHardware(irtrans);
        } catch (UnknownHostException ex) {
            guiUtils.error("unknown host: " + internetHostPanel2.getIpName());
        }
    }

    /**
     * Creates new form GuiTester
     */
    public GuiTester() {
        try {
            this.irpMaster = new IrpMaster("../IrpMaster/data/IrpProtocols.ini");
        } catch (FileNotFoundException | IncompatibleArgumentException ex) {
            guiUtils.error(ex);
        }
        try {
            irtrans = new IrTransIRDB("irtrans", verbose);
        } catch (UnknownHostException ex) {
            guiUtils.error("Unknown host: " + ex.getMessage());
        }

        //DefaultMutableTreeNode root = new DefaultMutableTreeNode("Nodes");
        //tree = new DefaultTreeModel(root);

        initComponents();
        //console1.setStdErr();
        //console1.setStdOut();
        lookAndFeelManager = new LookAndFeelManager(this, lafMenu, new TestCaller());
        //lookAndFeelManager.setLAF(properties.getLookAndFeel());
        //lookAndFeelManager.updateLAF();
        lafMenu.setVisible(true);
        statusLine.setStatus("Everything is fine");
        try {
            setupLirc();
        } catch (IOException ex) {
            guiUtils.error(ex);
        }
        try {
            setupIrTrans();
        } catch (IOException ex) {
            guiUtils.error(ex);
        }

        globalCacheManagerMenu = new GlobalCacheManagerMenu(globalCacheMenu, guiUtils, buttonGroup1, null, true);
        globalCacheManagerMenu.addPropertyChangeListener((PropertyChangeEvent evt) -> {
            globalCacheLearnerTextField.setText(((InetAddress) evt.getNewValue()).getHostName());
        });

        globalCacheIrSenderSelector.setGuiUtils(guiUtils);
        try {
            globalCacheIrSenderSelector.setIpName("192.168.1.70");
        } catch (UnknownHostException ex) {
            guiUtils.error(ex);
        } catch (IOException ex) {
            guiUtils.error(ex);
        }

        //this.beaconTree;
        //jythonPanel1.setPythonDir("python");
        //jythonPanel1.init("init.py");
        //jythonPanel1.assign("guitester", this);
        //jythonPanel1.assign("globalcacheselector", globalCacheIrSenderSelector);
        irpMasterBean.addPropertyChangeListener((java.beans.PropertyChangeEvent evt) -> {
            if (evt.getPropertyName().equals(IrpMasterBean.PROP_PROTOCOL_NAME))
                guiUtils.message("Someone changed protocol from " + evt.getOldValue() + " to " + evt.getNewValue());
        });
    }

    public GlobalCache getGlobalCache() {
        return globalCacheIrSenderSelector.getGlobalCache();
    }

    //public JTree nodes2tree(Collection<AmxBeaconListener.Node> nodes) {

    //}

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        copyPastePopupMenu1 = new org.harctoolbox.guicomponents.CopyPastePopupMenu(true);
        buttonGroup1 = new javax.swing.ButtonGroup();
        jLabel1 = new javax.swing.JLabel();
        statusLine = new org.harctoolbox.guicomponents.StatusLine();
        helpButton1 = new org.harctoolbox.guicomponents.HelpButton(this.guiUtils, helpText);
        globalCacheLearnerTextField = new javax.swing.JTextField();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTextArea1 = new javax.swing.JTextArea();
        console1 = new org.harctoolbox.guicomponents.Console();
        jCheckBox1 = new javax.swing.JCheckBox();
        jTabbedPane1 = new javax.swing.JTabbedPane();
        jPanel1 = new javax.swing.JPanel();
        hexCalculator1 = new org.harctoolbox.guicomponents.HexCalculator();
        jPanel2 = new javax.swing.JPanel();
        timeFrequencyCalculator1 = new org.harctoolbox.guicomponents.TimeFrequencyCalculator();
        lircPanel = new javax.swing.JPanel();
        internetHostPanel1 = new org.harctoolbox.guicomponents.InternetHostPanel(guiUtils, true, true, true);
        lircNamedCommandLauncher = new org.harctoolbox.guicomponents.NamedCommandLauncher(guiUtils);
        jPanel4 = new javax.swing.JPanel();
        internetHostPanel2 = new org.harctoolbox.guicomponents.InternetHostPanel(guiUtils, false, true, true);
        irtransNamedCommandLauncher = new org.harctoolbox.guicomponents.NamedCommandLauncher();
        globalCachePanel = new javax.swing.JPanel();
        globalCacheTextField = new javax.swing.JTextField();
        globalCacheTransmitterTextField = new javax.swing.JTextField();
        globalCacheIrSenderSelector = new org.harctoolbox.guicomponents.GlobalCacheIrSenderSelector(guiUtils, verbose, 5000, true);
        globalCachePanel1 = new javax.swing.JPanel();
        globalCacheTextField1 = new javax.swing.JTextField();
        globalCacheTransmitterTextField1 = new javax.swing.JTextField();
        globalCacheIrSenderSelector1 = new org.harctoolbox.guicomponents.GlobalCacheIrSenderSelector(guiUtils, verbose, 5000, false);
        beaconPanel = new javax.swing.JPanel();
        amxBeaconListenerTree1 = new org.harctoolbox.guicomponents.AmxBeaconListenerPanel();
        jPanel3 = new javax.swing.JPanel();
        irpMasterBean = new org.harctoolbox.guicomponents.IrpMasterBean(this, guiUtils, irpMaster, "rc5", false);
        jScrollPane2 = new javax.swing.JScrollPane();
        ccfTextArea = new javax.swing.JTextArea();
        jButton2 = new javax.swing.JButton();
        jButton3 = new javax.swing.JButton();
        jPanel5 = new javax.swing.JPanel();
        audioParametersBean1 = new org.harctoolbox.guicomponents.AudioParametersBean();
        jPanel6 = new javax.swing.JPanel();
        serialPortBean1 = new org.harctoolbox.guicomponents.SerialPortSimpleBean(guiUtils);
        jPanel7 = new javax.swing.JPanel();
        serialPortBean2 = new org.harctoolbox.guicomponents.SerialPortBean();
        jCheckBox2 = new javax.swing.JCheckBox();
        jMenuBar1 = new javax.swing.JMenuBar();
        jMenu1 = new javax.swing.JMenu();
        jMenu2 = new javax.swing.JMenu();
        jMenu3 = new javax.swing.JMenu();
        lafMenu = new javax.swing.JMenu();
        jMenu4 = new javax.swing.JMenu();
        jMenuItem1 = new javax.swing.JMenuItem();
        jMenuItem2 = new javax.swing.JMenuItem();
        jSeparator1 = new javax.swing.JPopupMenu.Separator();
        jMenu5 = new javax.swing.JMenu();
        globalCacheMenu = new javax.swing.JMenu();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        jLabel1.setText("Tester");

        statusLine.setToolTipText("Status linator");
        statusLine.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                statusLineActionPerformed(evt);
            }
        });

        globalCacheLearnerTextField.setEditable(false);
        globalCacheLearnerTextField.setText("unassigned");

        jTextArea1.setColumns(20);
        jTextArea1.setRows(5);
        jTextArea1.setComponentPopupMenu(copyPastePopupMenu1);
        jScrollPane1.setViewportView(jTextArea1);

        jCheckBox1.setText("use popups for help");
        jCheckBox1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBox1ActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 1166, Short.MAX_VALUE)
            .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(jPanel1Layout.createSequentialGroup()
                    .addGap(28, 28, 28)
                    .addComponent(hexCalculator1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addContainerGap(738, Short.MAX_VALUE)))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 248, Short.MAX_VALUE)
            .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                    .addContainerGap(44, Short.MAX_VALUE)
                    .addComponent(hexCalculator1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addContainerGap()))
        );

        jTabbedPane1.addTab("Hexcalc", jPanel1);

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGap(155, 155, 155)
                .addComponent(timeFrequencyCalculator1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(778, Short.MAX_VALUE))
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                .addContainerGap(20, Short.MAX_VALUE)
                .addComponent(timeFrequencyCalculator1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        jTabbedPane1.addTab("TimeFrequencyCalc", jPanel2);

        internetHostPanel1.addPropertyChangeListener(new java.beans.PropertyChangeListener() {
            public void propertyChange(java.beans.PropertyChangeEvent evt) {
                internetHostPanel1PropertyChange(evt);
            }
        });

        javax.swing.GroupLayout lircPanelLayout = new javax.swing.GroupLayout(lircPanel);
        lircPanel.setLayout(lircPanelLayout);
        lircPanelLayout.setHorizontalGroup(
            lircPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(lircPanelLayout.createSequentialGroup()
                .addComponent(lircNamedCommandLauncher, javax.swing.GroupLayout.DEFAULT_SIZE, 1154, Short.MAX_VALUE)
                .addContainerGap())
            .addGroup(lircPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(internetHostPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        lircPanelLayout.setVerticalGroup(
            lircPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(lircPanelLayout.createSequentialGroup()
                .addGap(22, 22, 22)
                .addComponent(internetHostPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 11, Short.MAX_VALUE)
                .addComponent(lircNamedCommandLauncher, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(27, 27, 27))
        );

        jTabbedPane1.addTab("LIRC", lircPanel);

        internetHostPanel2.setIpName("irtrans");

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(internetHostPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(irtransNamedCommandLauncher, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(372, Short.MAX_VALUE))
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(internetHostPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(irtransNamedCommandLauncher, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(42, Short.MAX_VALUE))
        );

        jTabbedPane1.addTab("IrTrans", jPanel4);

        globalCacheTextField.setEditable(false);
        globalCacheTextField.setText("unassigned");

        globalCacheTransmitterTextField.setEditable(false);
        globalCacheTransmitterTextField.setText("unassigned");

        globalCacheIrSenderSelector.addPropertyChangeListener(new java.beans.PropertyChangeListener() {
            public void propertyChange(java.beans.PropertyChangeEvent evt) {
                globalCacheIrSenderSelectorPropertyChange(evt);
            }
        });

        javax.swing.GroupLayout globalCachePanelLayout = new javax.swing.GroupLayout(globalCachePanel);
        globalCachePanel.setLayout(globalCachePanelLayout);
        globalCachePanelLayout.setHorizontalGroup(
            globalCachePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(globalCachePanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(globalCachePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(globalCacheIrSenderSelector, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(globalCachePanelLayout.createSequentialGroup()
                        .addComponent(globalCacheTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(globalCacheTransmitterTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addGap(270, 270, 270))
        );
        globalCachePanelLayout.setVerticalGroup(
            globalCachePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(globalCachePanelLayout.createSequentialGroup()
                .addGap(40, 40, 40)
                .addComponent(globalCacheIrSenderSelector, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(globalCachePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(globalCacheTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(globalCacheTransmitterTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );

        jTabbedPane1.addTab("GlobalCache", globalCachePanel);

        globalCacheTextField1.setEditable(false);
        globalCacheTextField1.setText("unassigned");

        globalCacheTransmitterTextField1.setEditable(false);
        globalCacheTransmitterTextField1.setText("unassigned");

        globalCacheIrSenderSelector1.addPropertyChangeListener(new java.beans.PropertyChangeListener() {
            public void propertyChange(java.beans.PropertyChangeEvent evt) {
                globalCacheIrSenderSelector1PropertyChange(evt);
            }
        });

        javax.swing.GroupLayout globalCachePanel1Layout = new javax.swing.GroupLayout(globalCachePanel1);
        globalCachePanel1.setLayout(globalCachePanel1Layout);
        globalCachePanel1Layout.setHorizontalGroup(
            globalCachePanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(globalCachePanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(globalCachePanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(globalCacheIrSenderSelector1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(globalCachePanel1Layout.createSequentialGroup()
                        .addComponent(globalCacheTextField1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(globalCacheTransmitterTextField1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addGap(270, 270, 270))
        );
        globalCachePanel1Layout.setVerticalGroup(
            globalCachePanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(globalCachePanel1Layout.createSequentialGroup()
                .addGap(40, 40, 40)
                .addComponent(globalCacheIrSenderSelector1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(globalCachePanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(globalCacheTextField1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(globalCacheTransmitterTextField1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );

        jTabbedPane1.addTab("GlobalCache/capture", globalCachePanel1);

        javax.swing.GroupLayout beaconPanelLayout = new javax.swing.GroupLayout(beaconPanel);
        beaconPanel.setLayout(beaconPanelLayout);
        beaconPanelLayout.setHorizontalGroup(
            beaconPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(beaconPanelLayout.createSequentialGroup()
                .addComponent(amxBeaconListenerTree1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(766, Short.MAX_VALUE))
        );
        beaconPanelLayout.setVerticalGroup(
            beaconPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(amxBeaconListenerTree1, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 248, Short.MAX_VALUE)
        );

        jTabbedPane1.addTab("AMX Beacon", beaconPanel);

        ccfTextArea.setEditable(false);
        ccfTextArea.setColumns(20);
        ccfTextArea.setLineWrap(true);
        ccfTextArea.setRows(5);
        ccfTextArea.setWrapStyleWord(true);
        jScrollPane2.setViewportView(ccfTextArea);

        jButton2.setText("Render");
        jButton2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton2ActionPerformed(evt);
            }
        });

        jButton3.setText("Iterate");
        jButton3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton3ActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(irpMasterBean, javax.swing.GroupLayout.DEFAULT_SIZE, 1130, Short.MAX_VALUE)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel3Layout.createSequentialGroup()
                        .addComponent(jScrollPane2)
                        .addGap(18, 18, 18)
                        .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jButton2)
                            .addComponent(jButton3))
                        .addGap(31, 31, 31)))
                .addGap(24, 24, 24))
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(irpMasterBean, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 92, Short.MAX_VALUE)
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addComponent(jButton2)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButton3)
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );

        jTabbedPane1.addTab("IrpMaster", jPanel3);

        javax.swing.GroupLayout jPanel5Layout = new javax.swing.GroupLayout(jPanel5);
        jPanel5.setLayout(jPanel5Layout);
        jPanel5Layout.setHorizontalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel5Layout.createSequentialGroup()
                .addGap(23, 23, 23)
                .addComponent(audioParametersBean1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(705, Short.MAX_VALUE))
        );
        jPanel5Layout.setVerticalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel5Layout.createSequentialGroup()
                .addGap(27, 27, 27)
                .addComponent(audioParametersBean1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(155, Short.MAX_VALUE))
        );

        jTabbedPane1.addTab("Wave", jPanel5);

        javax.swing.GroupLayout jPanel6Layout = new javax.swing.GroupLayout(jPanel6);
        jPanel6.setLayout(jPanel6Layout);
        jPanel6Layout.setHorizontalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel6Layout.createSequentialGroup()
                .addGap(42, 42, 42)
                .addComponent(serialPortBean1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(612, Short.MAX_VALUE))
        );
        jPanel6Layout.setVerticalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel6Layout.createSequentialGroup()
                .addGap(49, 49, 49)
                .addComponent(serialPortBean1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(114, Short.MAX_VALUE))
        );

        jTabbedPane1.addTab("Serial simple", jPanel6);

        javax.swing.GroupLayout jPanel7Layout = new javax.swing.GroupLayout(jPanel7);
        jPanel7.setLayout(jPanel7Layout);
        jPanel7Layout.setHorizontalGroup(
            jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel7Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(serialPortBean2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(363, Short.MAX_VALUE))
        );
        jPanel7Layout.setVerticalGroup(
            jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel7Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(serialPortBean2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(151, Short.MAX_VALUE))
        );

        jTabbedPane1.addTab("serial", jPanel7);

        jCheckBox2.setSelected(verbose);
        jCheckBox2.setText("verbose");
        jCheckBox2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBox2ActionPerformed(evt);
            }
        });

        jMenu1.setText("File");
        jMenuBar1.add(jMenu1);

        jMenu2.setText("Edit");
        jMenuBar1.add(jMenu2);

        jMenu3.setText("Options");

        lafMenu.setText("Look and feel");
        jMenu3.add(lafMenu);

        jMenuBar1.add(jMenu3);

        jMenu4.setText("Tools");

        jMenuItem1.setMnemonic('H');
        jMenuItem1.setText("HexCalculator");
        jMenuItem1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem1ActionPerformed(evt);
            }
        });
        jMenu4.add(jMenuItem1);

        jMenuItem2.setMnemonic('T');
        jMenuItem2.setText("TimeFrequencyCalculator");
        jMenuItem2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem2ActionPerformed(evt);
            }
        });
        jMenu4.add(jMenuItem2);
        jMenu4.add(jSeparator1);

        jMenuBar1.add(jMenu4);

        jMenu5.setText("Hardware");

        globalCacheMenu.setText("GlobalCache Units");
        jMenu5.add(globalCacheMenu);

        jMenuBar1.add(jMenu5);

        setJMenuBar(jMenuBar1);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(statusLine, javax.swing.GroupLayout.PREFERRED_SIZE, 184, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(jCheckBox1)
                                .addGap(80, 80, 80)
                                .addComponent(jCheckBox2)))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 371, Short.MAX_VALUE))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jLabel1)
                        .addGap(62, 62, 62)))
                .addComponent(helpButton1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(119, 119, 119))
            .addComponent(console1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addGap(0, 0, Short.MAX_VALUE)
                .addComponent(globalCacheLearnerTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 227, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(49, 49, 49))
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addComponent(jTabbedPane1))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(layout.createSequentialGroup()
                        .addGap(36, 36, 36)
                        .addComponent(helpButton1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jCheckBox1)
                            .addComponent(jCheckBox2))
                        .addGap(5, 5, 5)
                        .addComponent(jLabel1)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(statusLine, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addGap(15, 15, 15)
                .addComponent(globalCacheLearnerTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 307, Short.MAX_VALUE)
                .addComponent(console1, javax.swing.GroupLayout.PREFERRED_SIZE, 107, javax.swing.GroupLayout.PREFERRED_SIZE))
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                    .addContainerGap(157, Short.MAX_VALUE)
                    .addComponent(jTabbedPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 293, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addContainerGap(105, Short.MAX_VALUE)))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jCheckBox1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBox1ActionPerformed
        guiUtils.setUsePopupsForHelp(jCheckBox1.isSelected());
    }//GEN-LAST:event_jCheckBox1ActionPerformed

    private void jMenuItem1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem1ActionPerformed
        HarcletFrame.newHarcletFrame(this, new HexCalculator(), false, lookAndFeelManager.getCurrentLAFClassName());
    }//GEN-LAST:event_jMenuItem1ActionPerformed

    private void jMenuItem2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem2ActionPerformed
        HarcletFrame.newHarcletFrame(this, new TimeFrequencyCalculator(), false, lookAndFeelManager.getCurrentLAFClassName());
    }//GEN-LAST:event_jMenuItem2ActionPerformed

    private void internetHostPanel1PropertyChange(java.beans.PropertyChangeEvent evt) {//GEN-FIRST:event_internetHostPanel1PropertyChange
        try {
            setupLirc();
        } catch (IOException ex) {
            Logger.getLogger(GuiTester.class.getName()).log(Level.SEVERE, null, ex);
        }
    }//GEN-LAST:event_internetHostPanel1PropertyChange

    private void jButton2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton2ActionPerformed
        try {
            IrSignal irSignal = irpMasterBean.render();
            ccfTextArea.setText(irSignal.ccfString());
        } catch (UnassignedException ex) {
            guiUtils.error(ex);
        } catch (ParseException ex) {
            guiUtils.error(ex);
        } catch (IrpMasterException ex) {
            guiUtils.error(ex);
        }
    }//GEN-LAST:event_jButton2ActionPerformed

    private void statusLineActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_statusLineActionPerformed

    }//GEN-LAST:event_statusLineActionPerformed

    private void jCheckBox2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBox2ActionPerformed
        verbose = jCheckBox2.isSelected();
    }//GEN-LAST:event_jCheckBox2ActionPerformed

    private void globalCacheIrSenderSelectorPropertyChange(java.beans.PropertyChangeEvent evt) {//GEN-FIRST:event_globalCacheIrSenderSelectorPropertyChange
        switch (evt.getPropertyName()) {
            case GlobalCacheIrSenderSelector.PROP_IPNAME:
                globalCacheTextField.setText((String) evt.getNewValue());
                break;
            case GlobalCacheIrSenderSelector.PROP_MODULE:
            case GlobalCacheIrSenderSelector.PROP_PORT:
                try {
                    GlobalCache.GlobalCacheIrTransmitter transmitter = globalCacheIrSenderSelector.getTransmitter();
                    globalCacheTransmitterTextField.setText(transmitter.toString());
                    globalCacheTextField.setText(globalCacheIrSenderSelector.getIpName());
                } catch (NoSuchTransmitterException ex) {
                    guiUtils.error(ex);
                }   break;
            default:
                throw new RuntimeException("Programming error detected.");
        }
    }//GEN-LAST:event_globalCacheIrSenderSelectorPropertyChange

    private void globalCacheIrSenderSelector1PropertyChange(java.beans.PropertyChangeEvent evt) {//GEN-FIRST:event_globalCacheIrSenderSelector1PropertyChange

    }//GEN-LAST:event_globalCacheIrSenderSelector1PropertyChange

    private void jButton3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton3ActionPerformed
        Map<String, Command> commands = null;
        RandomValueSet.initRng();
        try {
            commands = irpMasterBean.getCommands();
        } catch (UnassignedException ex) {
            Logger.getLogger(GuiTester.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ParseException ex) {
            Logger.getLogger(GuiTester.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IrpMasterException ex) {
            Logger.getLogger(GuiTester.class.getName()).log(Level.SEVERE, null, ex);
        }
        if (commands != null)
            for (String name : commands.keySet()) {
                System.err.println(name);
            }
    }//GEN-LAST:event_jButton3ActionPerformed

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Create and display the form */
        java.awt.EventQueue.invokeLater(() -> {
            new GuiTester().setVisible(true);
        });
    }
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private org.harctoolbox.guicomponents.AmxBeaconListenerPanel amxBeaconListenerTree1;
    private org.harctoolbox.guicomponents.AudioParametersBean audioParametersBean1;
    private javax.swing.JPanel beaconPanel;
    private javax.swing.ButtonGroup buttonGroup1;
    private javax.swing.JTextArea ccfTextArea;
    private org.harctoolbox.guicomponents.Console console1;
    private org.harctoolbox.guicomponents.CopyPastePopupMenu copyPastePopupMenu1;
    private org.harctoolbox.guicomponents.GlobalCacheIrSenderSelector globalCacheIrSenderSelector;
    private org.harctoolbox.guicomponents.GlobalCacheIrSenderSelector globalCacheIrSenderSelector1;
    private javax.swing.JTextField globalCacheLearnerTextField;
    private javax.swing.JMenu globalCacheMenu;
    private javax.swing.JPanel globalCachePanel;
    private javax.swing.JPanel globalCachePanel1;
    private javax.swing.JTextField globalCacheTextField;
    private javax.swing.JTextField globalCacheTextField1;
    private javax.swing.JTextField globalCacheTransmitterTextField;
    private javax.swing.JTextField globalCacheTransmitterTextField1;
    private org.harctoolbox.guicomponents.HelpButton helpButton1;
    private org.harctoolbox.guicomponents.HexCalculator hexCalculator1;
    private org.harctoolbox.guicomponents.InternetHostPanel internetHostPanel1;
    private org.harctoolbox.guicomponents.InternetHostPanel internetHostPanel2;
    private org.harctoolbox.guicomponents.IrpMasterBean irpMasterBean;
    private org.harctoolbox.guicomponents.NamedCommandLauncher irtransNamedCommandLauncher;
    private javax.swing.JButton jButton2;
    private javax.swing.JButton jButton3;
    private javax.swing.JCheckBox jCheckBox1;
    private javax.swing.JCheckBox jCheckBox2;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JMenu jMenu1;
    private javax.swing.JMenu jMenu2;
    private javax.swing.JMenu jMenu3;
    private javax.swing.JMenu jMenu4;
    private javax.swing.JMenu jMenu5;
    private javax.swing.JMenuBar jMenuBar1;
    private javax.swing.JMenuItem jMenuItem1;
    private javax.swing.JMenuItem jMenuItem2;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JPanel jPanel6;
    private javax.swing.JPanel jPanel7;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JPopupMenu.Separator jSeparator1;
    private javax.swing.JTabbedPane jTabbedPane1;
    private javax.swing.JTextArea jTextArea1;
    private javax.swing.JMenu lafMenu;
    private org.harctoolbox.guicomponents.NamedCommandLauncher lircNamedCommandLauncher;
    private javax.swing.JPanel lircPanel;
    private org.harctoolbox.guicomponents.SerialPortSimpleBean serialPortBean1;
    private org.harctoolbox.guicomponents.SerialPortBean serialPortBean2;
    private org.harctoolbox.guicomponents.StatusLine statusLine;
    private org.harctoolbox.guicomponents.TimeFrequencyCalculator timeFrequencyCalculator1;
    // End of variables declaration//GEN-END:variables
}
