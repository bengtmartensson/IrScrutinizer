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

package org.harctoolbox.irscrutinizer.sendinghardware;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import javax.swing.JPanel;
import org.harctoolbox.IrpMaster.IrSignal;
import org.harctoolbox.IrpMaster.IrpMasterException;
import org.harctoolbox.guicomponents.GlobalCacheIrSenderSelector;
import org.harctoolbox.guicomponents.GuiUtils;
import org.harctoolbox.harchardware.ir.GlobalCache;
import org.harctoolbox.harchardware.HarcHardwareException;
import org.harctoolbox.harchardware.ir.NoSuchTransmitterException;
import org.harctoolbox.harchardware.ir.Transmitter;
import org.harctoolbox.irscrutinizer.HardwareUnavailableException;
import org.harctoolbox.irscrutinizer.Props;

/**
 * This class does something interesting and useful. Or not...
 */
public class SendingGlobalCache extends SendingHardware<GlobalCache> implements ISendingHardware<GlobalCache> {

    private GlobalCacheIrSenderSelector globalCacheIrSenderSelector;
    private String initialIp;

    public SendingGlobalCache(JPanel panel, final Props properties, GuiUtils gui,
            GlobalCacheIrSenderSelector newGlobalCacheIrSenderSelector) {
        super(panel, properties, gui);
        initialIp = properties.getGlobalCacheIpName();
        this.globalCacheIrSenderSelector = newGlobalCacheIrSenderSelector;
        globalCacheIrSenderSelector.setTimeout(properties.getGlobalCacheTimeout());
        properties.addGlobalCacheTimeoutChangeListener(new Props.IPropertyChangeListener() {

            @Override
            public void propertyChange(String name, Object oldValue, Object newValue) {
                globalCacheIrSenderSelector.setTimeout((Integer) newValue);
            }
        });
        this.globalCacheIrSenderSelector.addPropertyChangeListener(new PropertyChangeListener() {

            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                try {
                    setup();

                    if (evt.getPropertyName().equals(GlobalCacheIrSenderSelector.PROP_IPNAME)) {
                        rawIrSender = globalCacheIrSenderSelector.getGlobalCache();
                        properties.setGlobalCacheIpName((String) evt.getNewValue());
                    } else if (evt.getPropertyName().equals(GlobalCacheIrSenderSelector.PROP_MODULE))
                        properties.setGlobalCacheModule((Integer) evt.getNewValue());
                    else if (evt.getPropertyName().equals(GlobalCacheIrSenderSelector.PROP_PORT))
                        properties.setGlobalCachePort((Integer) evt.getNewValue());
                } catch (IOException ex) {
                    guiUtils.error(ex);
                } catch (HarcHardwareException ex) {
                    guiUtils.error(ex);
                }
            }
        });
    }

    @Override
    public boolean sendIr(IrSignal irSignal, int count) throws NoSuchTransmitterException, IrpMasterException, IOException, HardwareUnavailableException, HarcHardwareException {
        if (rawIrSender == null)
            throw new HardwareUnavailableException("No Global Caché unit selected.");
        return super.sendIr(irSignal, count);
    }

    @Override
    public String getName() {
        return "Global Caché";
    }

    @Override
    public Transmitter getTransmitter() throws NoSuchTransmitterException, HardwareUnavailableException{
        try {
            return globalCacheIrSenderSelector.getTransmitter();
        } catch (NullPointerException ex) {
            throw new HardwareUnavailableException();
        }
    }

    @Override
    public void setup() throws IOException, HarcHardwareException {
        try {
            globalCacheIrSenderSelector.setIpName(initialIp);
        } finally {
            initialIp = null;
        }
        String globalCacheIp = globalCacheIrSenderSelector.getIpName();
        if (globalCacheIp != null)
            properties.setGlobalCacheIpName(globalCacheIp);
        rawIrSender = globalCacheIrSenderSelector.getGlobalCache();
    }
}
