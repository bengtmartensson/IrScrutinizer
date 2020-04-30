/*
Copyright (C) 2013, 2014 Bengt Martensson.

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

package org.harctoolbox.irscrutinizer.importer;

import java.awt.Component;
import java.awt.Container;
import java.io.IOException;
import java.util.Collection;
import java.util.Enumeration;
import javax.swing.JTree;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import org.harctoolbox.girr.Command;
import org.harctoolbox.girr.GirrException;
import org.harctoolbox.girr.Remote;
import org.harctoolbox.girr.RemoteSet;
import org.harctoolbox.guicomponents.GuiUtils;
import org.harctoolbox.guicomponents.NoSelectionException;
import org.harctoolbox.harchardware.HarcHardwareException;
import org.harctoolbox.ircore.IrCoreException;
import org.harctoolbox.irp.IrpException;
import org.harctoolbox.irscrutinizer.GuiMain;
import org.harctoolbox.irscrutinizer.HardwareUnavailableException;

/**
 * Despite being a Java Bean, this is not really intended to be a recyclable component, it is just to be used in IrScrutinizer.
 */

public class TreeImporter extends javax.swing.JPanel implements TreeExpansionListener {
    private GuiUtils guiUtils;
    private GuiMain guiMain = null;
    private DefaultMutableTreeNode root;
    private RemoteSet remoteSet;
    private boolean restrictedMode = false;

    private static final int maxRemotesForImportAll = 10;
    private static final String EMPTY_MESSAGE = "--EMPTY--";

    @Override
    public void treeExpanded(TreeExpansionEvent event) {
        DefaultMutableTreeNode remote = (DefaultMutableTreeNode) event.getPath().getLastPathComponent();
        if (!Remote.class.isInstance(remote.getUserObject())) // Just to be on the safe side...
            return;

        for (Enumeration e = remote.children(); e.hasMoreElements();) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) e.nextElement();
            Command command = (Command) node.getUserObject();
        }
    }

    @Override
    public void treeCollapsed(TreeExpansionEvent event) {
    }

    /**
     * Creates new form TreeImporter
     */
    public TreeImporter() {
        this(null, false);
    }

    public TreeImporter(GuiUtils guiUtils) {
        this(guiUtils, false);
    }

    /**
     * Creates new form TreeImporter
     * @param guiUtils
     * @param restrictedMode
     */
    @SuppressWarnings("LeakingThisInConstructor")
    public TreeImporter(GuiUtils guiUtils, boolean restrictedMode) {
        this.guiUtils = guiUtils;
        this.restrictedMode = restrictedMode;
        initComponents();
        tree.setCellRenderer(new MyRenderer());
        tree.addTreeExpansionListener(this);
        if (restrictedMode) {
            tree.setComponentPopupMenu(null);
            tree.setToolTipText(null);
        }
        enableStuff(false);
    }

    public void setRemoteSet(RemoteSet remoteSet) {
        if (remoteSet == null || remoteSet.isEmpty()) {
            guiUtils.error("No remotes in import, aborting.");
            clear(); // Leaving old content can be confusing
            return;
        }
        try {
            remoteSet.checkForParameters();
        } catch (IrpException | IrCoreException ex) {
            guiUtils.warning(ex.getMessage());
        }
        remoteSet.sort(new Remote.CompareNameCaseInsensitive());
        this.remoteSet = remoteSet;
        updateTreeModel();
    }

    private void updateTreeModel() {
        DefaultTreeModel treeModel = newTreeModel();
        tree.setModel(treeModel);
        tree.expandRow(1);
        enableStuff(true);
    }

    public void clear() {
        this.remoteSet = null;
        DefaultTreeModel treeModel = new DefaultTreeModel(new DefaultMutableTreeNode(EMPTY_MESSAGE));
        tree.setModel(treeModel);
        enableStuff(false);
    }

    private void enableStuff(boolean value) {
        boolean val = value && !restrictedMode;
        importAllButton.setEnabled(val && enableImportAll());
        importSelectionButton.setEnabled(val);
        transmitSelectedButton.setEnabled(val);
        importSignalButton.setEnabled(val);
        importSelectionRawButton.setEnabled(val);
        importAllRawButton.setEnabled(val && enableImportAll());

        scrutinizeSignalMenuItem.setEnabled(val);
        printSignalMenuItem.setEnabled(val);
        sortAllMenuItem.setEnabled(val);
        sortSelectedMenuItem.setEnabled(val);
        clearMenuItem.setEnabled(val);
        importAllMenuItem.setEnabled(val && enableImportAll());
        importSelectionMenuItem.setEnabled(val);
        transmitSignalMenuItem.setEnabled(val);
        importSelectionRawMenuItem.setEnabled(val && enableImportAll());
        importAllRawMenuItem.setEnabled(val);
    }

    private boolean enableImportAll() {
        return remoteSet == null || remoteSet.getRemotes().size() <= maxRemotesForImportAll;
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        tree.setEditable(enabled);
    }

    private DefaultTreeModel newTreeModel() {
        root = new DefaultMutableTreeNode("Remotes");
        Collection<Remote> remotes = remoteSet.getRemotes();
        remotes.forEach((remote) -> {
            root.add(newRemoteNode(remote));
        });
        return new DefaultTreeModel(root);
    }

    private DefaultMutableTreeNode newRemoteNode(Remote remote) {
        DefaultMutableTreeNode n = new DefaultMutableTreeNode(remote);
        remote.getCommands().entrySet().stream().map((kvp) -> new DefaultMutableTreeNode(kvp.getValue())).forEachOrdered((leaf) -> {
            n.add(leaf);
        });
        return n;
    }

    private int importSelectedSignals(boolean raw) throws IrpException, IrCoreException, NoSelectionException {
        int count = 0;
        TreePath[] paths = tree.getSelectionPaths();
        if (paths == null)
            throw new NoSelectionException();
        checkGuiMain();
        for (TreePath path : paths) {
            Object thing = ((DefaultMutableTreeNode) path.getLastPathComponent()).getUserObject();
            if (Remote.class.isInstance(thing)) {
                guiMain.importCommands(((Remote) thing).getCommands().values(), getMetaData(), raw);
                count += ((Remote) thing).getCommands().size();
            } else if (Command.class.isInstance(thing)) {
                guiMain.importCommand((Command) thing, raw);
                count++;
            } else {
                guiUtils.error("This cannot happen: " + thing.getClass().getCanonicalName());
            }
        }
        return count;
    }

    private void importJump(int count, ImportType type) {
        boolean doJump = guiUtils.confirm("Import was successful with " + count + " signal(s). Jump to panel?");
        if (doJump)
            guiMain.selectImportPane(type);
    }

    private Remote.MetaData getMetaData() {
        return remoteSet.getFirstMetaData();
    }

    private static class MyRenderer extends DefaultTreeCellRenderer /*implements TreeCellRenderer*/ {

        @Override
        public Component getTreeCellRendererComponent(
                JTree tree,
                Object value,
                boolean sel,
                boolean expanded,
                boolean leaf,
                int row,
                boolean hasFocus) {

            Object thing = ((DefaultMutableTreeNode) value).getUserObject();
            String name = Command.class.isInstance(thing) ? ((Command) thing).getName()
                    : Remote.class.isInstance(thing) ? ((Remote) thing).getName()
                    : thing.toString();

            super.getTreeCellRendererComponent(
                    tree, name,
                    sel,
                    expanded, leaf, row,
                    hasFocus);
            if (Command.class.isInstance(thing)) {
                Command command = (Command) thing;
                setToolTipText(command.prettyValueString());
            } else {
                setToolTipText(null);
            }
            return this;
        }
    }

    private Command getSingleCommand() {
        if (tree.getSelectionCount() != 1) {
            guiUtils.error("Exactly one command has to be selected for this command.");
            return null;
        }
        TreePath path = tree.getSelectionPaths()[0];
        Object thing = ((DefaultMutableTreeNode) path.getLastPathComponent()).getUserObject();
        if (!Command.class.isInstance(thing)) {
            guiUtils.error("A command (not remote) has to be selected for this command.");
            return null;
        }
        Command command = (Command) thing;
        return command;
    }

    private void checkGuiMain() {
        if (guiMain == null) {
            Container component = getRootPane().getParent();
            assert(component instanceof GuiMain);
            guiMain = (GuiMain) component;
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

        popupMenu = new javax.swing.JPopupMenu();
        scrutinizeSignalMenuItem = new javax.swing.JMenuItem();
        printSignalMenuItem = new javax.swing.JMenuItem();
        transmitSignalMenuItem = new javax.swing.JMenuItem();
        jSeparator1 = new javax.swing.JPopupMenu.Separator();
        sortSelectedMenuItem = new javax.swing.JMenuItem();
        sortAllMenuItem = new javax.swing.JMenuItem();
        jSeparator4 = new javax.swing.JPopupMenu.Separator();
        clearMenuItem = new javax.swing.JMenuItem();
        jSeparator3 = new javax.swing.JPopupMenu.Separator();
        importAllMenuItem = new javax.swing.JMenuItem();
        importSelectionMenuItem = new javax.swing.JMenuItem();
        jSeparator2 = new javax.swing.JPopupMenu.Separator();
        importAllRawMenuItem = new javax.swing.JMenuItem();
        importSelectionRawMenuItem = new javax.swing.JMenuItem();
        jScrollPane1 = new javax.swing.JScrollPane();
        tree = new javax.swing.JTree();
        importSignalButton = new javax.swing.JButton();
        importAllButton = new javax.swing.JButton();
        importSelectionButton = new javax.swing.JButton();
        transmitSelectedButton = new javax.swing.JButton();
        importAllRawButton = new javax.swing.JButton();
        importSelectionRawButton = new javax.swing.JButton();

        scrutinizeSignalMenuItem.setMnemonic('S');
        scrutinizeSignalMenuItem.setText("Scrutinize Selected");
        scrutinizeSignalMenuItem.setToolTipText("Transfer selected signal to Scrutinize Signal pane.");
        scrutinizeSignalMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                scrutinizeSignalMenuItemActionPerformed(evt);
            }
        });
        popupMenu.add(scrutinizeSignalMenuItem);

        printSignalMenuItem.setMnemonic('P');
        printSignalMenuItem.setText("Print selected");
        printSignalMenuItem.setToolTipText("Print selected signal to console.");
        printSignalMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                printSignalMenuItemActionPerformed(evt);
            }
        });
        popupMenu.add(printSignalMenuItem);

        transmitSignalMenuItem.setMnemonic('T');
        transmitSignalMenuItem.setText("Transmit selected");
        transmitSignalMenuItem.setToolTipText("Send the single selected signal with selected hardware.");
        transmitSignalMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                transmitSignalMenuItemActionPerformed(evt);
            }
        });
        popupMenu.add(transmitSignalMenuItem);
        popupMenu.add(jSeparator1);

        sortSelectedMenuItem.setText("Sort selected");
        sortSelectedMenuItem.setToolTipText("Sort the commands in the selected remotes (NOT undoable).");
        sortSelectedMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                sortSelectedMenuItemActionPerformed(evt);
            }
        });
        popupMenu.add(sortSelectedMenuItem);

        sortAllMenuItem.setText("Sort all");
        sortAllMenuItem.setToolTipText("Sort the commands in all remotes (NOT undoable).");
        sortAllMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                sortAllMenuItemActionPerformed(evt);
            }
        });
        popupMenu.add(sortAllMenuItem);
        popupMenu.add(jSeparator4);

        clearMenuItem.setText("Clear");
        clearMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                clearMenuItemActionPerformed(evt);
            }
        });
        popupMenu.add(clearMenuItem);
        popupMenu.add(jSeparator3);

        importAllMenuItem.setMnemonic('A');
        importAllMenuItem.setText("Import all");
        importAllMenuItem.setToolTipText("Transfer all commands to either the raw or the parametrized remote.");
        importAllMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                importAllMenuItemActionPerformed(evt);
            }
        });
        popupMenu.add(importAllMenuItem);

        importSelectionMenuItem.setMnemonic('L');
        importSelectionMenuItem.setText("Import selection");
        importSelectionMenuItem.setToolTipText("Transfers selected signals to raw or parametric remote.");
        importSelectionMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                importSelectionMenuItemActionPerformed(evt);
            }
        });
        popupMenu.add(importSelectionMenuItem);
        popupMenu.add(jSeparator2);

        importAllRawMenuItem.setText("Import all as raw");
        importAllRawMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                importAllRawMenuItemActionPerformed(evt);
            }
        });
        popupMenu.add(importAllRawMenuItem);

        importSelectionRawMenuItem.setText("Import selection as raw");
        importSelectionRawMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                importSelectionRawMenuItemActionPerformed(evt);
            }
        });
        popupMenu.add(importSelectionRawMenuItem);

        tree.setModel(new javax.swing.tree.DefaultTreeModel(new javax.swing.tree.DefaultMutableTreeNode(EMPTY_MESSAGE)));
        tree.setToolTipText("Select with left, right for popup menu.");
        tree.setComponentPopupMenu(popupMenu);
        jScrollPane1.setViewportView(tree);

        importSignalButton.setText("Import signal");
        importSignalButton.setToolTipText("Transfers the (single!) selected signal to \"Scrutinize signal\"");
        importSignalButton.setEnabled(false);
        importSignalButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                importSignalButtonActionPerformed(evt);
            }
        });

        importAllButton.setMnemonic('A');
        importAllButton.setText("Import all");
        importAllButton.setToolTipText("Transfers all signals to either the raw or the parametric remote");
        importAllButton.setEnabled(false);
        importAllButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                importAllButtonActionPerformed(evt);
            }
        });

        importSelectionButton.setMnemonic('L');
        importSelectionButton.setText("Import selection");
        importSelectionButton.setToolTipText("Transfers the selected (remotes or commands) to either the raw or the parametrized remote.");
        importSelectionButton.setEnabled(false);
        importSelectionButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                importSelectionButtonActionPerformed(evt);
            }
        });

        transmitSelectedButton.setMnemonic('S');
        transmitSelectedButton.setText("Transmit selected");
        transmitSelectedButton.setToolTipText("Send the single selected signal with selected hardware.");
        transmitSelectedButton.setEnabled(false);
        transmitSelectedButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                transmitSelectedButtonActionPerformed(evt);
            }
        });

        importAllRawButton.setText("Import all/raw");
        importAllRawButton.setEnabled(false);
        importAllRawButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                importAllRawButtonActionPerformed(evt);
            }
        });

        importSelectionRawButton.setText("Import select./raw");
        importSelectionRawButton.setEnabled(false);
        importSelectionRawButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                importSelectionRawButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(5, 5, 5)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 378, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(importSignalButton, javax.swing.GroupLayout.Alignment.TRAILING)
                        .addComponent(importAllButton, javax.swing.GroupLayout.Alignment.TRAILING)
                        .addComponent(importSelectionButton, javax.swing.GroupLayout.Alignment.TRAILING))
                    .addComponent(transmitSelectedButton)
                    .addComponent(importAllRawButton)
                    .addComponent(importSelectionRawButton))
                .addGap(6, 6, 6))
        );

        layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {importAllButton, importAllRawButton, importSelectionButton, importSelectionRawButton, importSignalButton, transmitSelectedButton});

        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(5, 5, 5)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(importSignalButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(importAllButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(importSelectionButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(importAllRawButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(importSelectionRawButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 73, Short.MAX_VALUE)
                        .addComponent(transmitSelectedButton))
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE))
                .addGap(5, 5, 5))
        );
    }// </editor-fold>//GEN-END:initComponents

    private void printSignalMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_printSignalMenuItemActionPerformed
       Command command = getSingleCommand();
       if (command == null)
           return;

        try {
            guiUtils.message(command.toPrintString());
        } catch (IrpException | IrCoreException | GirrException ex) {
            guiUtils.error(ex);
        }
    }//GEN-LAST:event_printSignalMenuItemActionPerformed

    private void importSignalButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_importSignalButtonActionPerformed
        Command command = getSingleCommand();
        if (command == null)
            return;
        checkGuiMain();
        try {
            guiMain.scrutinizeIrSignal(command.toIrSignal());
            importJump(1, ImportType.signal);
        } catch (IrpException | IrCoreException ex) {
            guiUtils.error(ex);
        }
    }//GEN-LAST:event_importSignalButtonActionPerformed

    private void importAllButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_importAllButtonActionPerformed
        checkGuiMain();
        int result = guiMain.importCommands(remoteSet.getAllCommands(), getMetaData(), false);
        if (result > 0)
            importJump(result, ImportType.parametricRemote);
        else
            guiUtils.error("Import failed after importing " + Integer.toString(-result) + " signal(s)");
    }//GEN-LAST:event_importAllButtonActionPerformed

    private void importSelectionButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_importSelectionButtonActionPerformed
        try {
            int count = importSelectedSignals(false);
            importJump(count, ImportType.parametricRemote);
        } catch (IrpException | IrCoreException | NoSelectionException ex) {
            guiUtils.error(ex);
        }
    }//GEN-LAST:event_importSelectionButtonActionPerformed

    private void importAllMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_importAllMenuItemActionPerformed
        importAllButtonActionPerformed(evt);
    }//GEN-LAST:event_importAllMenuItemActionPerformed

    private void importSelectionMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_importSelectionMenuItemActionPerformed
        importSelectionButtonActionPerformed(evt);
    }//GEN-LAST:event_importSelectionMenuItemActionPerformed

    private void transmitSelectedButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_transmitSelectedButtonActionPerformed
        Command command = getSingleCommand();
        if (command == null)
            return;
        checkGuiMain();
        try {
            guiMain.transmit(command);
        } catch (IrpException | IrCoreException | IOException | HardwareUnavailableException | HarcHardwareException ex) {
            guiUtils.error(ex);
        }
    }//GEN-LAST:event_transmitSelectedButtonActionPerformed

    private void transmitSignalMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_transmitSignalMenuItemActionPerformed
        transmitSelectedButtonActionPerformed(evt);
    }//GEN-LAST:event_transmitSignalMenuItemActionPerformed

    private void importAllRawButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_importAllRawButtonActionPerformed
        checkGuiMain();
        int result = guiMain.importCommands(remoteSet.getAllCommands(), getMetaData(), true);
        if (result > 0)
            importJump(result, ImportType.rawRemote);
        else
            guiUtils.error("Import failed after importing " + Integer.toString(-result) + " signals");
    }//GEN-LAST:event_importAllRawButtonActionPerformed

    private void importSelectionRawButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_importSelectionRawButtonActionPerformed
        try {
            int count = importSelectedSignals(true);
            importJump(count, ImportType.rawRemote);
        } catch (IrpException | IrCoreException | NoSelectionException ex) {
            guiUtils.error(ex);
        }
    }//GEN-LAST:event_importSelectionRawButtonActionPerformed

    private void importAllRawMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_importAllRawMenuItemActionPerformed
        importAllRawButtonActionPerformed(evt);
    }//GEN-LAST:event_importAllRawMenuItemActionPerformed

    private void importSelectionRawMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_importSelectionRawMenuItemActionPerformed
        importSelectionRawButtonActionPerformed(evt);
    }//GEN-LAST:event_importSelectionRawMenuItemActionPerformed

    private void scrutinizeSignalMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_scrutinizeSignalMenuItemActionPerformed
        Command command = getSingleCommand();
        if (command == null)
            return;

        checkGuiMain();
        try {
            guiMain.scrutinizeIrSignal(command.toIrSignal());
        } catch (IrpException | IrCoreException ex) {
            guiUtils.error(ex);
        }
    }//GEN-LAST:event_scrutinizeSignalMenuItemActionPerformed

    private void clearMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_clearMenuItemActionPerformed
        clear();
    }//GEN-LAST:event_clearMenuItemActionPerformed

    private void sortAllMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_sortAllMenuItemActionPerformed
        for (Remote remote : remoteSet)
            remote.sort(new Command.CompareNameCaseInsensitive());
        updateTreeModel();
    }//GEN-LAST:event_sortAllMenuItemActionPerformed

    private void sortSelectedMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_sortSelectedMenuItemActionPerformed
        TreePath[] paths = tree.getSelectionPaths();
        if (paths == null) {
            guiUtils.error("No selection");
            return;
        }
        checkGuiMain();
        boolean hasAnnoyed = false;
        for (TreePath path : paths) {
            Object thing = ((DefaultMutableTreeNode) path.getLastPathComponent()).getUserObject();
            if (Remote.class.isInstance(thing)) {
                ((Remote) thing).sort(new Command.CompareNameCaseInsensitive());
            } else {
                if (!hasAnnoyed) {
                    guiUtils.warning("Only remotes can be sorted; selected commands ignored.");
                    hasAnnoyed = true;
                }
            }
        }
        updateTreeModel();
    }//GEN-LAST:event_sortSelectedMenuItemActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JMenuItem clearMenuItem;
    private javax.swing.JButton importAllButton;
    private javax.swing.JMenuItem importAllMenuItem;
    private javax.swing.JButton importAllRawButton;
    private javax.swing.JMenuItem importAllRawMenuItem;
    private javax.swing.JButton importSelectionButton;
    private javax.swing.JMenuItem importSelectionMenuItem;
    private javax.swing.JButton importSelectionRawButton;
    private javax.swing.JMenuItem importSelectionRawMenuItem;
    private javax.swing.JButton importSignalButton;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JPopupMenu.Separator jSeparator1;
    private javax.swing.JPopupMenu.Separator jSeparator2;
    private javax.swing.JPopupMenu.Separator jSeparator3;
    private javax.swing.JPopupMenu.Separator jSeparator4;
    private javax.swing.JPopupMenu popupMenu;
    private javax.swing.JMenuItem printSignalMenuItem;
    private javax.swing.JMenuItem scrutinizeSignalMenuItem;
    private javax.swing.JMenuItem sortAllMenuItem;
    private javax.swing.JMenuItem sortSelectedMenuItem;
    private javax.swing.JButton transmitSelectedButton;
    private javax.swing.JMenuItem transmitSignalMenuItem;
    private javax.swing.JTree tree;
    // End of variables declaration//GEN-END:variables
}
