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
import java.io.Serializable;
import java.net.InetAddress;
import java.net.UnknownHostException;
import javax.swing.AbstractButton;
import javax.swing.ButtonGroup;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu.Separator;
import javax.swing.JRadioButton;
import javax.swing.MenuElement;
import org.harctoolbox.harchardware.ir.GlobalCache;

public class GlobalCacheManagerMenu implements Serializable /*extends GlobalCacheManagerAbstractMenu*/ {
    private static final long serialVersionUID = 1L;
    private final JMenu globalCacheMenu;
    private JRadioButton[] globalCacheRadioButtons = new JRadioButton[0];
    private ButtonGroup buttonGroup = null;
    private final GuiUtils guiUtils;
    private InetAddress globalCacheInetAddress;
    private final boolean verbose;

    public static final String PROP_GC_ADDR = "PROP_GC_ADDR";
    private final PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        propertyChangeSupport.addPropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        propertyChangeSupport.removePropertyChangeListener(listener);
    }

    protected void setGlobalCache(InetAddress inetAddress) {
        if (verbose)
            guiUtils.info("Selected GlobalCache @ " + inetAddress.getHostName());
        GlobalCacheManager.getInstance().addManualGlobalCache(inetAddress);
        InetAddress oldName = globalCacheInetAddress;
        globalCacheInetAddress = inetAddress;
        propertyChangeSupport.firePropertyChange(PROP_GC_ADDR, oldName, inetAddress);
    }

    protected void addGlobalCache(int position, String prettyName, final InetAddress inetAddress, String tooltip) {
        JRadioButton menuItem = new JRadioButton(prettyName);
        menuItem.setToolTipText(tooltip);
        globalCacheRadioButtons[position] = menuItem;
        buttonGroup.add(menuItem);
        globalCacheMenu.add(menuItem);

        menuItem.setSelected(inetAddress.equals(globalCacheInetAddress));

        menuItem.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                setGlobalCache(inetAddress);
            }
        });
    }

    protected final void setup() {
        for (MenuElement me : globalCacheMenu.getSubElements()) {
            if (AbstractButton.class.isInstance(me)) {
                buttonGroup.remove((AbstractButton) me);
            }
        }
        globalCacheMenu.removeAll();
        globalCacheRadioButtons = new JRadioButton[GlobalCacheManager.getInstance().getAutomatic().length + GlobalCacheManager.getInstance().getManual().length];

        InetAddress[] automaticGlobalCaches = GlobalCacheManager.getInstance().getAutomatic();
        InetAddress[] manualGlobalCaches = GlobalCacheManager.getInstance().getManual();

        int index = 0;
        for (InetAddress ipName : manualGlobalCaches) {
            addGlobalCache(index++, ipName.getHostName(), ipName, "Manually entered GlobalCache.");
        }

        if (automaticGlobalCaches.length > 0 && manualGlobalCaches.length > 0)
            globalCacheMenu.add(new Separator());

        String[] automaticGlobalCachesPretty = GlobalCacheManager.getInstance().getAutomaticPrettyNames();
        for (int i = 0; i < automaticGlobalCaches.length; i++) {
            addGlobalCache(index++, automaticGlobalCachesPretty[i],
                    automaticGlobalCaches[i], "Automatically discovered GlobalCache.");
        }

        globalCacheMenu.add(new Separator());
        JMenuItem manualAddMenuItem = new JMenuItem("Add manually...");
        manualAddMenuItem.setMnemonic('A');
        globalCacheMenu.add(manualAddMenuItem);
        manualAddMenuItem.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                try {
                    String ip = guiUtils.getInput("Enter GlobalCache IP-Name or -address", "GlobalCache entry", GlobalCache.defaultGlobalCacheIP);
                    if (ip != null)
                        GlobalCacheManager.getInstance().addManualGlobalCache(InetAddress.getByName(ip));
                } catch (UnknownHostException ex) {
                    guiUtils.error(ex);
                }
            }
        });
        globalCacheMenu.repaint();
        globalCacheMenu.getParent().repaint();
    }

    public GlobalCacheManagerMenu(JMenu globalCacheMenu, GuiUtils guiUtils, ButtonGroup buttonGroup, String initialSelection, boolean verbose) {
        //super(guiUtils, verbose);
        this.guiUtils = guiUtils;
        this.globalCacheInetAddress = null;
        this.verbose = verbose;
        this.globalCacheMenu = globalCacheMenu;
        this.buttonGroup = buttonGroup;
        setup();

        GlobalCacheManager.getInstance().addPropertyChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                setup();
            }
        });
    }

    /*@Override
    public String getGlobalCacheIpName() {
        for (int i = 0; i < globalCacheMenu.getItemCount(); i++)
            if (globalCacheMenu.getItem(i).isSelected())
                return GlobalCacheManager.getIpName(i);
        return null;
    }*/
}
