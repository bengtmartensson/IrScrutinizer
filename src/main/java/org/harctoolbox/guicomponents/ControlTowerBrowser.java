/*
Copyright (C) 2024 Bengt Martensson.

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

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import javax.swing.DefaultComboBoxModel;
import org.harctoolbox.irscrutinizer.importer.ControlTowerIrDatabase;

public class ControlTowerBrowser extends HarcPanel {
    private static final String versionString = "0.3.0";
    private static final String helpText
            ="The support for the Control Tower Data base is limited. It can be used for browsing the data base\n"
            + "for devices and the contained commands, but it cannot download the codes of the commands.\n"
            + "For this, login to irdb.globalcache.com, and have the codes emailed.\n"
            + "The received email can be parsed by Import -> Text/CSV;\n"
            + "use Name col. = 1 and Raw signal col. = 3 (or 2), Field separator \", (comma)\".\n\n"
            + "Please observe their Terms of service."
            ;

    private static final String aboutText =
            "ControlTowerBrowser version " + versionString + ".\n"
            + "Copyright 2024 by Bengt Martensson.\n\n"
            + "License: GPL3.\n\n"
            + "Project home page: https://www.harctoolbox.org";

    //private static final String invalidString = "****";

    public static void main(String args[]) {
        HarcletFrame.run("ControlTowerBrowser", args);
    }

    private final GuiUtils guiUtils;
    private ControlTowerIrDatabase controlTowerIrDatabase = null;
    private final static boolean verbose = false;
    private Map<String, String> controlTowerCodesetTable = null;

    /**
     * Creates new form ControlTowerBrowser.
     */
    public ControlTowerBrowser() {
        initComponents();
        guiUtils = new GuiUtils(this);
    }

    @Override
    public String getHelpMessage() {
        return helpText;
    }

    @Override
    public String getAboutMessage() {
        return aboutText;
    }

    @Override
    public String getProgName() {
        return "ControlTowerBrowser";
    }

    @Override
    public String getIconPath() {
        return "/icons/Crystal-Clear/22x22/apps/database.png";
    }

    @Override
    public void close() throws IOException {
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        controlTowerPanel = new javax.swing.JPanel();
        controlTowerImportButton = new javax.swing.JButton();
        controlTowerManufacturerComboBox = new javax.swing.JComboBox<>();
        controlTowerDeviceTypeComboBox = new javax.swing.JComboBox<>();
        controlTowerCodeSetComboBox = new javax.swing.JComboBox<>();
        jLabel50 = new javax.swing.JLabel();
        jLabel51 = new javax.swing.JLabel();
        jLabel52 = new javax.swing.JLabel();
        controlTowerTreeImporter = new org.harctoolbox.irscrutinizer.importer.TreeImporter(this.guiUtils, true);
        controlTowerBrowseButton = new javax.swing.JButton();

        controlTowerImportButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/Crystal-Clear/22x22/actions/reload.png"))); // NOI18N
        controlTowerImportButton.setText("Load");
        controlTowerImportButton.setEnabled(false);
        controlTowerImportButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                controlTowerImportButtonActionPerformed(evt);
            }
        });

        controlTowerManufacturerComboBox.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Select me to load" }));
        controlTowerManufacturerComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                controlTowerManufacturerComboBoxActionPerformed(evt);
            }
        });

        controlTowerDeviceTypeComboBox.setEnabled(false);
        controlTowerDeviceTypeComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                controlTowerDeviceTypeComboBoxActionPerformed(evt);
            }
        });

        controlTowerCodeSetComboBox.setEnabled(false);

        jLabel50.setText("Manufacturer");

        jLabel51.setText("Device Type");

        jLabel52.setText("Setup Code");

        controlTowerBrowseButton.setText("Web site");
        controlTowerBrowseButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                controlTowerBrowseButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout controlTowerPanelLayout = new javax.swing.GroupLayout(controlTowerPanel);
        controlTowerPanel.setLayout(controlTowerPanelLayout);
        controlTowerPanelLayout.setHorizontalGroup(
            controlTowerPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(controlTowerPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(controlTowerPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(controlTowerPanelLayout.createSequentialGroup()
                        .addGroup(controlTowerPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel50)
                            .addComponent(controlTowerManufacturerComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, 184, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(controlTowerPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(controlTowerDeviceTypeComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, 162, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel51))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(controlTowerPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(controlTowerPanelLayout.createSequentialGroup()
                                .addComponent(jLabel52)
                                .addGap(461, 461, 461))
                            .addGroup(controlTowerPanelLayout.createSequentialGroup()
                                .addComponent(controlTowerCodeSetComboBox, 0, 340, Short.MAX_VALUE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(controlTowerImportButton)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(controlTowerBrowseButton, javax.swing.GroupLayout.PREFERRED_SIZE, 124, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addContainerGap())))
                    .addGroup(controlTowerPanelLayout.createSequentialGroup()
                        .addComponent(controlTowerTreeImporter, javax.swing.GroupLayout.PREFERRED_SIZE, 925, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(0, 0, 0))))
        );
        controlTowerPanelLayout.setVerticalGroup(
            controlTowerPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(controlTowerPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(controlTowerPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel50)
                    .addComponent(jLabel51)
                    .addComponent(jLabel52))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(controlTowerPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(controlTowerDeviceTypeComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(controlTowerCodeSetComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(controlTowerImportButton)
                    .addComponent(controlTowerManufacturerComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(controlTowerBrowseButton))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(controlTowerTreeImporter, javax.swing.GroupLayout.PREFERRED_SIZE, 420, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, 0))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(controlTowerPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, 0))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(0, 0, 0)
                .addComponent(controlTowerPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, 0))
        );
    }// </editor-fold>//GEN-END:initComponents

    private void controlTowerBrowseButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_controlTowerBrowseButtonActionPerformed
        try {
            guiUtils.browse(new URI(ControlTowerIrDatabase.protocol, ControlTowerIrDatabase.controlTowerIrDatabaseHost, null));
        } catch (URISyntaxException ex) {
            guiUtils.error(ex);
        }
    }//GEN-LAST:event_controlTowerBrowseButtonActionPerformed

    private void controlTowerDeviceTypeComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_controlTowerDeviceTypeComboBoxActionPerformed
        try {
            String manufacturer = (String) controlTowerManufacturerComboBox.getSelectedItem();
            String deviceType = (String) controlTowerDeviceTypeComboBox.getSelectedItem();
            controlTowerCodesetTable = controlTowerIrDatabase.getCodesetTable(manufacturer, deviceType);
            String[] arr = controlTowerCodesetTable.keySet().toArray(new String[controlTowerCodesetTable.size()]);
            Arrays.sort(arr, String.CASE_INSENSITIVE_ORDER);
            DefaultComboBoxModel<String> dcbm = new DefaultComboBoxModel<>(arr);
            controlTowerCodeSetComboBox.setModel(dcbm);
            controlTowerCodeSetComboBox.setEnabled(true);
            controlTowerImportButton.setEnabled(true);
        } catch (IOException | URISyntaxException ex) {
            guiUtils.error(ex);
        }
    }//GEN-LAST:event_controlTowerDeviceTypeComboBoxActionPerformed

    private void controlTowerManufacturerComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_controlTowerManufacturerComboBoxActionPerformed
        try {
            if (controlTowerIrDatabase == null) {
                controlTowerIrDatabase = new ControlTowerIrDatabase(verbose);
                Collection<String> manufacturers = controlTowerIrDatabase.getManufacturers();
                String[] arr = manufacturers.toArray(new String[0]);
                Arrays.sort(arr, String.CASE_INSENSITIVE_ORDER);
                DefaultComboBoxModel<String> dcbm = new DefaultComboBoxModel<>(arr);
                controlTowerManufacturerComboBox.setModel(dcbm);
                controlTowerManufacturerComboBoxActionPerformed(null);
            } else {
                String manufacturer = (String) controlTowerManufacturerComboBox.getSelectedItem();
                Collection<String> devTypes = controlTowerIrDatabase.getDeviceTypes(manufacturer);
                String[] arr = devTypes.toArray(new String[0]);
                Arrays.sort(arr, String.CASE_INSENSITIVE_ORDER);
                DefaultComboBoxModel<String> dcbm = new DefaultComboBoxModel<>(arr);
                controlTowerDeviceTypeComboBox.setModel(dcbm);
                controlTowerDeviceTypeComboBoxActionPerformed(null);
                controlTowerDeviceTypeComboBox.setEnabled(true);
            }
            controlTowerTreeImporter.clear();
        } catch (IOException | URISyntaxException ex) {
            guiUtils.error(ex);
        }
    }//GEN-LAST:event_controlTowerManufacturerComboBoxActionPerformed

    private void controlTowerImportButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_controlTowerImportButtonActionPerformed
        try {
            String manufacturer = (String) controlTowerManufacturerComboBox.getSelectedItem();
            String deviceType = (String) controlTowerDeviceTypeComboBox.getSelectedItem();
            String modelName = (String) controlTowerCodeSetComboBox.getSelectedItem();
            String codeSet = controlTowerCodesetTable.get(modelName);
            controlTowerIrDatabase.load(manufacturer, deviceType, codeSet);
            controlTowerTreeImporter.setRemoteSet(controlTowerIrDatabase.getRemoteSet(), "Control Tower");
        } catch (IOException | URISyntaxException ex) {
            guiUtils.error(ex);
        }
    }//GEN-LAST:event_controlTowerImportButtonActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton controlTowerBrowseButton;
    private javax.swing.JComboBox<String> controlTowerCodeSetComboBox;
    private javax.swing.JComboBox<String> controlTowerDeviceTypeComboBox;
    private javax.swing.JButton controlTowerImportButton;
    private javax.swing.JComboBox<String> controlTowerManufacturerComboBox;
    private javax.swing.JPanel controlTowerPanel;
    private org.harctoolbox.irscrutinizer.importer.TreeImporter controlTowerTreeImporter;
    private javax.swing.JLabel jLabel50;
    private javax.swing.JLabel jLabel51;
    private javax.swing.JLabel jLabel52;
    // End of variables declaration//GEN-END:variables
}
