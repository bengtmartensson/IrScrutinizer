/*
Copyright (C) 2013-2015 Bengt Martensson.

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
import java.net.ConnectException;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JPanel;
import org.harctoolbox.harchardware.ir.IRemoteCommandIrSender;
//import org.harctoolbox.harchardware.ir.IRemoteCommandIrSenderStop;
import org.harctoolbox.harchardware.ir.ITransmitter;
import org.harctoolbox.harchardware.ir.NoSuchTransmitterException;
import org.harctoolbox.harchardware.ir.Transmitter;

/**
 *
 */
public class NamedCommandLauncher extends JPanel {
    private static final long serialVersionUID = 1L;

    private GuiUtils guiUtils;
    private transient IRemoteCommandIrSender hardware;
    private DefaultComboBoxModel transmitterComboBoxModel;
    private DefaultComboBoxModel remoteComboBoxModel;
    private DefaultComboBoxModel commandComboBoxModel;
    // TODO private boolean supportsStopIr;

    public NamedCommandLauncher() {
        this(null, null);
    }

    public NamedCommandLauncher(GuiUtils guiUtils) {
        this(guiUtils, null);
    }


    /**
     * Creates new form NamedCommandLauncher
     * @param guiUtils
     * @param remoteCommandIrSender
     */
    public NamedCommandLauncher(GuiUtils guiUtils, IRemoteCommandIrSender remoteCommandIrSender) {
        this.guiUtils = guiUtils;
        try {
            setHardware(remoteCommandIrSender);
        } catch (ConnectException ex) {
            guiUtils.error("Connection was refused.");
        } catch (IOException ex) {
            guiUtils.error(ex);
        }
        initComponents();
        enableStuff();
    }

    public final void setHardware(IRemoteCommandIrSender hardware) throws IOException {
        this.hardware = hardware;
        //supportsStopIr = (hardware != null) && IRemoteCommandIrSenderStop.class.isInstance(hardware);
        if (hardware == null) {
            transmitterComboBoxModel = new javax.swing.DefaultComboBoxModel();//new String[]{"Tr. 1", "Tr. 2", "Tr. 3", "Tr. 4"});
            //transmitterComboBox.setEnabled(false;);
            remoteComboBoxModel = new javax.swing.DefaultComboBoxModel();
            commandComboBoxModel = new javax.swing.DefaultComboBoxModel();
            if (remoteComboBox != null)
                remoteComboBox.setModel(remoteComboBoxModel);
            if (commandComboBox != null)
                commandComboBox.setModel(commandComboBoxModel);
        } else {
            load(); // may throw exception
            enableStuff();
        }
    }

    private void enableStuff() {
        stopButton.setEnabled(false /*hardware != null && supportsStopIr*/);
        //stopButton.setVisible(hardware != null && IRemoteCommandIrSenderStop.class.isInstance(hardware));
        //stopButton.setToolTipText(IRemoteCommandIrSenderStop.class.isInstance(hardware) ? "Stops ongoing IR transmission" :
        sendButton.setEnabled(hardware != null);
        reloadButton.setEnabled(hardware != null);
        transmitterComboBox.setEnabled(hardware != null && transmitterComboBoxModel.getSize() > 1);
        noSendsComboBox.setEnabled(hardware != null);
        remoteComboBox.setEnabled(hardware != null);
        commandComboBox.setEnabled(hardware != null);
    }

    private void load() throws IOException {
        if (ITransmitter.class.isInstance(hardware))
            transmitterComboBoxModel = new DefaultComboBoxModel(((ITransmitter) hardware).getTransmitterNames());
        else
            transmitterComboBox.setEditable(false);
        transmitterComboBox.setModel(transmitterComboBoxModel);

        try {
            //if (IHarcHardware.class.isInstance(hardware))
            //    versionTextField.setText(versionPrefix + ((IHarcHardware) hardware).getVersion());
            String[] remotes = hardware.getRemotes();
            if (remotes == null || remotes.length == 0) {
                guiUtils.warning("No remotes present");
                remoteComboBox.setModel(new DefaultComboBoxModel());
                commandComboBox.setModel(new DefaultComboBoxModel());
                return;
            }
            java.util.Arrays.sort(remotes, String.CASE_INSENSITIVE_ORDER);
            remoteComboBoxModel = new DefaultComboBoxModel(remotes);
            remoteComboBox.setModel(remoteComboBoxModel);
            loadCommands((String) remoteComboBoxModel.getElementAt(0));
        } catch (IOException ex) {
            hardware = null;
            remoteComboBox.setModel(new DefaultComboBoxModel());
            commandComboBox.setModel(new DefaultComboBoxModel());
            throw ex;
        }
    }

    private void loadCommands(String remote) {
        try {
            String[] commands = hardware.getCommands(remote);
            java.util.Arrays.sort(commands, String.CASE_INSENSITIVE_ORDER);
            commandComboBoxModel = new DefaultComboBoxModel(commands);
            commandComboBox.setModel(commandComboBoxModel);
        } catch (IOException ex) {
            guiUtils.error(ex);
        }
    }

    private String getRemote() {
        return (String) remoteComboBox.getSelectedItem();
    }

    private String getCommand() {
        return (String) commandComboBox.getSelectedItem();
    }

    private int getNoSends() {
        return Integer.parseInt((String) noSendsComboBox.getSelectedItem());
    }

    public Transmitter getTransmitter() throws NoSuchTransmitterException {
        if (ITransmitter.class.isInstance(hardware)) {
            ITransmitter tr = (ITransmitter) hardware;
            return tr.getTransmitter((String) transmitterComboBox.getSelectedItem());
        } else
            return null;
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        copyPopupMenu = new org.harctoolbox.guicomponents.CopyPastePopupMenu();
        noSendsComboBox = new javax.swing.JComboBox();
        transmitterComboBox = new javax.swing.JComboBox();
        remoteComboBox = new javax.swing.JComboBox();
        commandComboBox = new javax.swing.JComboBox();
        sendButton = new javax.swing.JButton();
        stopButton = new javax.swing.JButton();
        reloadButton = new javax.swing.JButton();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();

        noSendsComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "1", "2", "3", "4", "5", "7", "10", "15", "20", "30", "40", "50", "70", "100", "200", "500", "1000" }));
        noSendsComboBox.setToolTipText("Number of times to send command");

        transmitterComboBox.setModel(transmitterComboBoxModel);
        transmitterComboBox.setToolTipText("The transmitter to use to send the command");

        remoteComboBox.setModel(remoteComboBoxModel);
        remoteComboBox.setToolTipText("Device for which command is defined");
        remoteComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                remoteComboBoxActionPerformed(evt);
            }
        });

        commandComboBox.setModel(commandComboBoxModel);
        commandComboBox.setToolTipText("Command name");

        sendButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/Crystal-Clear/22x22/apps/cache.png"))); // NOI18N
        sendButton.setMnemonic('S');
        sendButton.setText("Send");
        sendButton.setToolTipText("Send command");
        sendButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                sendButtonActionPerformed(evt);
            }
        });

        stopButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/Crystal-Clear/22x22/actions/stop.png"))); // NOI18N
        stopButton.setMnemonic('T');
        stopButton.setText("Stop");
        stopButton.setToolTipText("Stop ongoing transmission");
        stopButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                stopButtonActionPerformed(evt);
            }
        });

        reloadButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/Crystal-Clear/22x22/actions/reload.png"))); // NOI18N
        reloadButton.setMnemonic('R');
        reloadButton.setText("Reload");
        reloadButton.setToolTipText("Reload devices and their commands from the gateway");
        reloadButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                reloadButtonActionPerformed(evt);
            }
        });

        jLabel1.setText("# Sends");

        jLabel2.setText("Transm.");

        jLabel3.setText("Remote");

        jLabel4.setText("Command");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(0, 0, 0)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(noSendsComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel1))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel2)
                    .addComponent(transmitterComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, 72, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel3)
                    .addComponent(remoteComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, 150, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGap(51, 51, 51)
                        .addComponent(jLabel4))
                    .addGroup(layout.createSequentialGroup()
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(commandComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, 150, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(sendButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(stopButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(reloadButton)))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(jLabel2)
                    .addComponent(jLabel3)
                    .addComponent(jLabel4))
                .addGap(9, 9, 9)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(noSendsComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(transmitterComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(remoteComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(commandComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(sendButton)
                    .addComponent(stopButton)
                    .addComponent(reloadButton))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        layout.linkSize(javax.swing.SwingConstants.VERTICAL, new java.awt.Component[] {commandComboBox, noSendsComboBox, remoteComboBox, transmitterComboBox});

        layout.linkSize(javax.swing.SwingConstants.VERTICAL, new java.awt.Component[] {reloadButton, sendButton, stopButton});

    }// </editor-fold>//GEN-END:initComponents

    private void remoteComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_remoteComboBoxActionPerformed
        loadCommands(getRemote());
        commandComboBox.setModel(commandComboBoxModel);
    }//GEN-LAST:event_remoteComboBoxActionPerformed

    private void sendButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_sendButtonActionPerformed
        try {
            hardware.sendIrCommand(getRemote(), getCommand(), getNoSends(), getTransmitter());
        } catch (IOException ex) {
            guiUtils.error(ex);
        } catch (NoSuchTransmitterException ex) {
            guiUtils.error(ex);
        }
    }//GEN-LAST:event_sendButtonActionPerformed

    private void stopButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_stopButtonActionPerformed
        /*
        if (supportsStopIr) {
            try {
                ((IRemoteCommandIrSenderStop) hardware).stopIr(getRemote(), getCommand(), getTransmitter());
            } catch (IOException ex) {
                guiUtils.error(ex);
            } catch (NoSuchTransmitterException ex) {
                guiUtils.error(ex);
            }
        }
        */
    }//GEN-LAST:event_stopButtonActionPerformed

    private void reloadButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_reloadButtonActionPerformed
        try {
            load();
        } catch (IOException ex) {
            guiUtils.error(ex);
        }
        if (remoteComboBox.getItemCount() > 0) {
            remoteComboBox.setSelectedIndex(0);
            remoteComboBoxActionPerformed(null);
        }
        enableStuff();
    }//GEN-LAST:event_reloadButtonActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JComboBox commandComboBox;
    private org.harctoolbox.guicomponents.CopyPastePopupMenu copyPopupMenu;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JComboBox noSendsComboBox;
    private javax.swing.JButton reloadButton;
    private javax.swing.JComboBox remoteComboBox;
    private javax.swing.JButton sendButton;
    private javax.swing.JButton stopButton;
    private javax.swing.JComboBox transmitterComboBox;
    // End of variables declaration//GEN-END:variables
}
