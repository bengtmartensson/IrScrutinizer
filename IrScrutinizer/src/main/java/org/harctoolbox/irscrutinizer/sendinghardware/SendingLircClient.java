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
import org.harctoolbox.guicomponents.GuiUtils;
import org.harctoolbox.guicomponents.InternetHostPanel;
import org.harctoolbox.guicomponents.NamedCommandLauncher;
import org.harctoolbox.harchardware.HarcHardwareException;
import org.harctoolbox.harchardware.ir.LircCcfClient;
import org.harctoolbox.harchardware.ir.NoSuchTransmitterException;
import org.harctoolbox.harchardware.ir.Transmitter;
import org.harctoolbox.irscrutinizer.Props;

/**
 * This class does something interesting and useful. Or not...
 */
public class SendingLircClient extends SendingHardware<LircCcfClient> implements ISendingHardware<LircCcfClient> {

    InternetHostPanel internetHostPanel;
    NamedCommandLauncher namedCommandLauncher;

    public SendingLircClient(JPanel panel, Props properties, GuiUtils gui,
            InternetHostPanel internetHostPanel, NamedCommandLauncher namedCommandLauncher) {
        super(panel, properties, gui);
        this.internetHostPanel = internetHostPanel;
        this.namedCommandLauncher = namedCommandLauncher;
        this.internetHostPanel.addPropertyChangeListener(new PropertyChangeListener() {

            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                try {
                    setup();
                } catch (IOException ex) {
                    guiUtils.error(ex);
                } catch (HarcHardwareException ex) {
                    guiUtils.error(ex);
                }
            }

        });
    }

    @Override
    public String getName() {
        return "Lirc";
    }

    @Override
    public Transmitter getTransmitter() throws NoSuchTransmitterException {
        return namedCommandLauncher.getTransmitter();
    }

    @Override
    public void setup() throws IOException, HarcHardwareException {
        String lircIp = internetHostPanel.getIpName();
        int lircPort = internetHostPanel.getPortNumber();
        rawIrSender = new LircCcfClient(lircIp, lircPort, properties.getVerbose(), properties.getSendingTimeout());
        try {
            internetHostPanel.setHardware(rawIrSender);
        } catch (IOException ex) {
            namedCommandLauncher.setHardware(null);
            throw ex;
        }
        namedCommandLauncher.setHardware(rawIrSender);
        properties.setLircIpName(lircIp);
        properties.setLircPort(lircPort);
    }
}
