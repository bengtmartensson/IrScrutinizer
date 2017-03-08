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

package org.harctoolbox.irscrutinizer.sendinghardware;

import java.beans.PropertyChangeEvent;
import java.io.IOException;
import javax.swing.JPanel;
import org.harctoolbox.devslashlirc.LircDeviceException;
import org.harctoolbox.guicomponents.DevLircBean;
import org.harctoolbox.guicomponents.GuiUtils;
import org.harctoolbox.harchardware.ir.DevLirc;
import org.harctoolbox.harchardware.ir.LircTransmitter;
import org.harctoolbox.irscrutinizer.Props;

/**
 *
 */
public class SendingDevLirc extends SendingHardware<DevLirc> implements ISendingHardware<DevLirc> {
    private DevLircBean devLircBean;
    private String portName;
    private DevLirc rawIrSender;

    public SendingDevLirc(JPanel panel, DevLircBean devLircBean, Props props, GuiUtils guiUtils_) {
        super(panel, props, guiUtils_);
        this.devLircBean = devLircBean;
        this.portName = devLircBean.getPortName();
        devLircBean.addPropertyChangeListener((PropertyChangeEvent evt) -> {
            String propertyName = evt.getPropertyName();
            switch (propertyName) {
                case DevLircBean.PROP_PROPS:
                    break;
                case DevLircBean.PROP_PORTNAME:
                    if (evt.getNewValue() == null)
                        return;
                    setup();
                    break;
                case DevLircBean.PROP_ISOPEN:
                    break;
                default:
                    throw new RuntimeException("Unknown property " + propertyName);
            }
        });
    }

    @Override
    public void setup() {
        String newPort = devLircBean.getPortName();
        if (newPort == null || (rawIrSender != null && newPort.equals(portName)))
            return;

        if (rawIrSender != null)
            rawIrSender.close();
        rawIrSender = null;
        try {
            rawIrSender = new DevLirc(newPort, properties.getVerbose());
            rawIrSender.setBeginTimeout(properties.getSendingTimeout());
        } catch (LircDeviceException | IOException ex) {
            guiUtils.error(ex);
        }
        portName = newPort;
        properties.setDevLircName(newPort);
        devLircBean.setHardware(rawIrSender);
    }

    @Override
    public String getName() {
        return "/dev/lirc";
    }

    @Override
    public LircTransmitter getTransmitter() {
        return devLircBean.getTransmitter();
    }

    @Override
    public DevLirc getRawIrSender() {
        return rawIrSender;
    }
}
