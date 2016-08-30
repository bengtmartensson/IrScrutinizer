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
import org.harctoolbox.guicomponents.GirsClientBean;
import org.harctoolbox.guicomponents.GuiUtils;
import org.harctoolbox.guicomponents.ISendingReceivingBean;
import org.harctoolbox.harchardware.HarcHardwareException;
import org.harctoolbox.harchardware.IHarcHardware;
import org.harctoolbox.harchardware.ir.ICapture;
import org.harctoolbox.irscrutinizer.Props;
import org.harctoolbox.irscrutinizer.sendinghardware.SendingHardware;

public class CapturingSendingHardware <T extends ICapture & IHarcHardware> extends CapturingHardware<T> implements ICapturingHardware<T>,IHarcHardware {

    private final SendingHardware<?> sendingHardware;
    private final CapturingSendingBean capturingSendingBean;

    public CapturingSendingHardware(JPanel panel, JPanel sendingPanel,
            final CapturingSendingBean capturingSendingBean, ISendingReceivingBean sendingBean,
            final SendingHardware<?> sendingHardware,
            Props properties_, GuiUtils guiUtils_,
            CapturingHardwareManager capturingHardwareManager) {
        super(panel, properties_, guiUtils_, capturingHardwareManager);
        this.capturingSendingBean = capturingSendingBean;
        this.sendingHardware = sendingHardware;
        capturingSendingBean.setOpened(sendingHardware.getRawIrSender() != null && sendingHardware.getRawIrSender().isValid());
        capturingSendingBean.setSenderPanel(sendingPanel);

        sendingBean.addPropertyChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                String propertyName = evt.getPropertyName();
                switch (propertyName) {
                    case ISendingReceivingBean.PROP_BAUD:
                    case ISendingReceivingBean.PROP_PORTNAME:
                    case GirsClientBean.PROP_IPNAME:
                    case GirsClientBean.PROP_TYPE:
                    case ISendingReceivingBean.PROP_PROPS:
                        break;
                    case ISendingReceivingBean.PROP_ISOPEN:
                        capturingSendingBean.setOpened(sendingHardware.getRawIrSender().isValid());
                        break;
                    default:
                        throw new RuntimeException("Unknown property: " + propertyName);
                }
            }
        });
    }

    @Override
    public void setup() throws IOException, HarcHardwareException {
        if (!sendingHardware.isValid())
            sendingHardware.setup();
        selectMe();
        capturingSendingBean.setOpened(sendingHardware.getRawIrSender().isValid());
    }

    @Override
    public String getName() {
        return sendingHardware.getName();
    }

    @Override
    public void open() throws HarcHardwareException, IOException {
        sendingHardware.getRawIrSender().open();
        capturingSendingBean.setOpened(sendingHardware.getRawIrSender().isValid());
    }

    @Override
    @SuppressWarnings("unchecked")
    public T getCapturer() {
        return (T) sendingHardware.getRawIrSender();
    }
}
