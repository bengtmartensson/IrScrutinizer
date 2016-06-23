/*
Copyright (C) 2016 Bengt Martensson.

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
import javax.swing.JPanel;
import org.harctoolbox.guicomponents.CapturingSendingBean;
import org.harctoolbox.guicomponents.DevLircBean;
import org.harctoolbox.guicomponents.GuiUtils;
import org.harctoolbox.harchardware.HarcHardwareException;
import org.harctoolbox.harchardware.IHarcHardware;
import org.harctoolbox.harchardware.ir.DevLirc;
import org.harctoolbox.irscrutinizer.Props;
import org.harctoolbox.irscrutinizer.sendinghardware.SendingDevLirc;

/**
 *
 */
public class CapturingDevLirc extends CapturingHardware<DevLirc> implements ICapturingHardware<DevLirc>, IHarcHardware {

    private final SendingDevLirc sendingDevLirc;
    private final CapturingSendingBean capturingSendingBean;
    private final DevLircBean devLircBean;

    public CapturingDevLirc(JPanel panel, JPanel jumpPanel, CapturingSendingBean bean, DevLircBean devLircBean,
            final SendingDevLirc sendingDevLirc, Props properties_,
            GuiUtils guiUtils_, CapturingHardwareManager capturingHardwareManager) {
        super(panel, properties_, guiUtils_, capturingHardwareManager);
        this.sendingDevLirc = sendingDevLirc;
        this.capturingSendingBean = bean;
        this.devLircBean = devLircBean;
        capturingSendingBean.setOpened(sendingDevLirc.getRawIrSender() != null && sendingDevLirc.getRawIrSender().isValid());
        capturingSendingBean.setSenderPanel(jumpPanel);

        devLircBean.addPropertyChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                String propertyName = evt.getPropertyName();
                switch (propertyName) {
                    case DevLircBean.PROP_PROPS:
                        // TODO?
                        break;
                    case DevLircBean.PROP_ISOPEN:
                        capturingSendingBean.setOpened(sendingDevLirc.getRawIrSender().isValid());
                        break;
                    default:
                        break;
                }
            }
        });
    }

    @Override
    public void setup() throws IOException {
        sendingDevLirc.setup();
        try {
            selectMe();
        } catch (HarcHardwareException ex) {
            guiUtils.error(ex);
        }
        capturingSendingBean.setOpened(sendingDevLirc.getRawIrSender().isValid());
    }

    @Override
    public String getName() {
        return sendingDevLirc.getName();
    }

    @Override
    public void open() throws HarcHardwareException, IOException {
        getCapturer().open();
        capturingSendingBean.setOpened(sendingDevLirc.getRawIrSender().isValid());
    }

    @Override
    public void setDebug(int debug) {
    }

    @Override
    public DevLirc getCapturer() {
        return sendingDevLirc.getRawIrSender();
    }
}
