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

package org.harctoolbox.irscrutinizer.capturinghardware;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JPanel;
import org.harctoolbox.guicomponents.GlobalCacheIrSenderSelector;
import org.harctoolbox.guicomponents.GuiUtils;
import org.harctoolbox.harchardware.ir.GlobalCache;
import org.harctoolbox.harchardware.HarcHardwareException;
import org.harctoolbox.irscrutinizer.Props;

/**
 * This class does something interesting and useful. Or not...
 */
public class CapturingGlobalCache extends CapturingHardware<GlobalCache> implements ICapturingHardware<GlobalCache> {

    private String initialIp;
    GlobalCacheIrSenderSelector globalCacheIrSenderSelector;

    public CapturingGlobalCache(String hostname, final GlobalCacheIrSenderSelector globalCacheIrSenderSelector,
            JPanel panel, Props properties_, GuiUtils guiUtils, CapturingHardwareManager capturingHardwareManager) {
        super(panel, properties_, guiUtils, capturingHardwareManager);
        this.initialIp = hostname.isEmpty() ? null : hostname;
        this.globalCacheIrSenderSelector = globalCacheIrSenderSelector;
        globalCacheIrSenderSelector.setTimeout(properties.getCaptureStartTimeout());
        properties.addCaptureStartTimeoutChangeListener(new Props.IPropertyChangeListener() {

            @Override
            public void propertyChange(String name, Object oldValue, Object newValue) {
                globalCacheIrSenderSelector.setTimeout(properties.getCaptureStartTimeout());
            }
        });
        //setupHardwareCommonStart();

        globalCacheIrSenderSelector.addPropertyChangeListener(new PropertyChangeListener() {

            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                try {
                    setup();
                    if (evt.getPropertyName().equals(GlobalCacheIrSenderSelector.PROP_IPNAME)) {
                        //rawIrSender = globalCacheIrSenderSelector.getGlobalCache();
                        properties.setGlobalCacheCaptureIpName((String) evt.getNewValue());
                    }
                } catch (IOException ex) {
                    Logger.getLogger(CapturingGlobalCache.class.getName()).log(Level.SEVERE, null, ex);
                } catch (HarcHardwareException ex) {
                    Logger.getLogger(CapturingGlobalCache.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        });
    }

    @Override
    public void setup() throws IOException, HarcHardwareException {
        hardware = null;
        try {
            if (initialIp != null) {
                globalCacheIrSenderSelector.setIpName(initialIp);
                initialIp = null;
            }
            hardware = globalCacheIrSenderSelector.getGlobalCache();
            if (hardware != null && !hardware.isValid())
                hardware.open();
            selectMe();
        } catch (UnknownHostException ex) {
            guiUtils.error("Hostname \"" + initialIp + "\" does not resolve.");
        } catch (IOException ex) {
            guiUtils.error(ex);
        }
    }

    @Override
    public String getName() {
        return "Global Caché";
    }
}
