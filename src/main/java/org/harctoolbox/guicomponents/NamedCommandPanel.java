/*
Copyright (C) 2021 Bengt Martensson.

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
import org.harctoolbox.harchardware.IHarcHardware;
import org.harctoolbox.harchardware.ir.IRemoteCommandIrSender;
import org.harctoolbox.harchardware.ir.IrTrans;
import org.harctoolbox.harchardware.ir.IrTransIRDB;
import org.harctoolbox.harchardware.ir.LircClient;

/**
 * A tool for sending commands to IR senders using named commands (IRemoteCommandIrSender).
 */
public class NamedCommandPanel extends HarcPanel {
    private static final String HELP_TEXT =
            "This pane is used with IR senders that sort their commands into named remotes contained named commands.\n"
            + "It queries the sender for its remotes, and, on need, inquiring the content of those remotes,"
            + "and offers to send those commands."
            ;

    public static final String VERSION = "0.2.0";
    public static final String ABOUT =
            "NamedCommandPanel version " + VERSION + ".\n"
            + "Copyright 2021 by Bengt Martensson.\n\n"
            + "License: GPL3+.\n\n"
            + "Project home page: https://www.harctoolbox.org";

    public static void main(String args[]) {
        HarcletFrame.run("NamedCommandPanel", args);
    }
    private IRemoteCommandIrSender irSender;
    private final GuiUtils guiUtils;
    private final static boolean verbose = false;
    private final static int timeout = 5000;

    /**
     * Creates new form NamedCommandPanel
     */
    public NamedCommandPanel() {
        guiUtils = new GuiUtils(this);
        guiUtils.setUsePopupsForErrors(true);
        guiUtils.setUsePopupsForHelp(true);

        initComponents();
    }

    public void setup() throws IOException {
        irSender = mkSender();
        if (irSender == null)
            return;

        try {
            internetHostPanel.setHardware((IHarcHardware) irSender);
            if (getKind() == Kind.irTrans) {
                internetHostPanel.setPortNumber(IrTrans.portNumber);
            }
            internetHostPanel.setBrowsingEnabled(getKind() == Kind.irTrans);
        } catch (IOException ex) {
            namedCommandLauncher.setHardware(null);
            throw ex;
        }
        namedCommandLauncher.setHardware(irSender);
    }

    private IRemoteCommandIrSender mkSender() throws IOException {
        Kind kind = getKind();
        switch (kind) {
            case lirc:
                return new LircClient(internetHostPanel.getIpName(), internetHostPanel.getPortNumber(), verbose, timeout);
            case irTrans:
                return new IrTransIRDB(internetHostPanel.getIpName(), verbose, timeout);
            default:
                guiUtils.error("Kind " + kind.toString() + " not implemented yet.");
                return null;
        }
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        namedCommandLauncher = new org.harctoolbox.guicomponents.NamedCommandLauncher(guiUtils);
        internetHostPanel = new org.harctoolbox.guicomponents.InternetHostPanel(guiUtils, true);
        kindComboBox = new javax.swing.JComboBox<>();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        openButton = new javax.swing.JButton();

        kindComboBox.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Lirc", "IrTrans (Ethernet, with database)", "Girs (Ethernet, with NamedCommand)" }));
        kindComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                kindComboBoxActionPerformed(evt);
            }
        });

        jLabel1.setText("Kind:");

        jLabel2.setText("This tool is used to send named commands, residing in named remotes, to supporting IR senders, like Lirc.");

        openButton.setText("Open");
        openButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                openButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(internetHostPanel, javax.swing.GroupLayout.DEFAULT_SIZE, 912, Short.MAX_VALUE)
                    .addComponent(namedCommandLauncher, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(jLabel1)
                                .addGap(29, 29, 29)
                                .addComponent(kindComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(18, 18, 18)
                                .addComponent(openButton))
                            .addComponent(jLabel2))
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel2)
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(kindComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel1)
                    .addComponent(openButton))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(internetHostPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(namedCommandLauncher, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(28, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents

    private void openButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_openButtonActionPerformed
        try {
            setup();
        } catch (IOException ex) {
            guiUtils.error(ex);
        }
    }//GEN-LAST:event_openButtonActionPerformed

    private void kindComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_kindComboBoxActionPerformed
        internetHostPanel.setPortNumber(getKind() == Kind.lirc ? LircClient.lircDefaultPort : IrTrans.portNumber);
    }//GEN-LAST:event_kindComboBoxActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private org.harctoolbox.guicomponents.InternetHostPanel internetHostPanel;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JComboBox<String> kindComboBox;
    private org.harctoolbox.guicomponents.NamedCommandLauncher namedCommandLauncher;
    private javax.swing.JButton openButton;
    // End of variables declaration//GEN-END:variables

    @Override
    public String getHelpMessage() {
        return HELP_TEXT;
    }

    @Override
    public String getAboutMessage() {
        return ABOUT;
    }

    @Override
    public String getProgName() {
        return "NamedCommandPanel";
    }

    @Override
    public String getIconPath() {
        return "/icons/Crystal-Clear/22x22/apps/cache.png";
    }

    @Override
    public void close() throws IOException {
    }

    private Kind getKind() {
        int index = kindComboBox.getSelectedIndex();
        return Kind.values()[index];
    }

    private enum Kind {
        lirc,
        irTrans,
        girs;
    }
}
