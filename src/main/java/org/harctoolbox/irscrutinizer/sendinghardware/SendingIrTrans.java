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
import java.io.IOException;
import java.net.InetAddress;
import javax.swing.JPanel;
import org.harctoolbox.guicomponents.GuiUtils;
import org.harctoolbox.guicomponents.InternetHostPanel;
import org.harctoolbox.harchardware.HarcHardwareException;
import org.harctoolbox.harchardware.ir.IrTrans;
import org.harctoolbox.harchardware.ir.NoSuchTransmitterException;
import org.harctoolbox.harchardware.ir.Transmitter;
import org.harctoolbox.irscrutinizer.Props;

/**
 * This class does something interesting and useful. Or not...
 */
public class SendingIrTrans extends SendingHardware<IrTrans> implements ISendingHardware<IrTrans> {

    private final InternetHostPanel internetHostPanel;
    private String desiredIp;
    private String currentIp = null;
    private IrTrans rawIrSender;

    public SendingIrTrans(JPanel panel, Props properties, GuiUtils gui,
            InternetHostPanel internetHostPanel) {
        super(panel, properties, gui);
        this.internetHostPanel = internetHostPanel;
        desiredIp = properties.getIrTransIpName();
        this.internetHostPanel.addPropertyChangeListener((PropertyChangeEvent evt) -> {
            try {
                setup();
            } catch (IOException | HarcHardwareException ex) {
                //guiUtils.error(ex);
            }
        });
    }

    @Override
    public String getName() {
        return "IrTrans";
    }

    @Override
    public Transmitter getTransmitter() throws NoSuchTransmitterException {
        return rawIrSender.newTransmitter(IrTrans.Led.all);
    }

    @Override
    public void setup() throws IOException, HarcHardwareException {
        internetHostPanel.setIpName(desiredIp);
        desiredIp = null;
        if (rawIrSender == null || currentIp == null || !currentIp.equals(internetHostPanel.getIpName())) {
            close();
            rawIrSender = null;
            InetAddress ip = InetAddress.getByName(internetHostPanel.getIpName());
            rawIrSender = new IrTrans(ip, properties.getVerbose(), properties.getSendingTimeout());
            currentIp = internetHostPanel.getIpName();
            properties.setIrTransIpName(internetHostPanel.getIpName());
        }
    }

    @Override
    public IrTrans getRawIrSender() {
        return rawIrSender;
    }
}
