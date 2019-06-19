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

import java.io.IOException;
import javax.swing.JPanel;
import org.harctoolbox.guicomponents.AudioParametersBean;
import org.harctoolbox.guicomponents.GuiUtils;
import org.harctoolbox.harchardware.HarcHardwareException;
import org.harctoolbox.harchardware.ir.IrAudioDevice;
import org.harctoolbox.irscrutinizer.Props;

/**
 * This class does something interesting and useful. Or not...
 */
public class SendingIrAudioPort extends SendingHardware<IrAudioDevice> implements ISendingHardware<IrAudioDevice> {
    private final AudioParametersBean audioParametersBean;
    private IrAudioDevice rawIrSender;

    public SendingIrAudioPort(JPanel panel, AudioParametersBean audioParametersBean, Props properties, GuiUtils guiUtils) {
        super(panel, properties, guiUtils);
        this.audioParametersBean = audioParametersBean;
        audioParametersBean.setVerbose(properties.getVerbose());
    }

    @Override
    public void setup() throws IOException, HarcHardwareException {
        audioParametersBean.setVerbose(properties.getVerbose());
        audioParametersBean.setupHardware();
        rawIrSender = audioParametersBean.getHardware();
    }

    @Override
    public String getName() {
        return "Audio Port";
    }

    @Override
    public IrAudioDevice getRawIrSender() {
        return rawIrSender;
    }
}
