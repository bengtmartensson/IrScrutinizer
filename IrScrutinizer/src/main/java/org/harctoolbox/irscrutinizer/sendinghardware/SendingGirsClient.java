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

package org.harctoolbox.irscrutinizer.sendinghardware;

import java.io.IOException;
import javax.swing.JPanel;
import org.harctoolbox.guicomponents.GuiUtils;
import org.harctoolbox.guicomponents.GirsClientBean;
import org.harctoolbox.harchardware.HarcHardwareException;
import org.harctoolbox.harchardware.ir.GirsClient;
import org.harctoolbox.irscrutinizer.Props;

public class SendingGirsClient extends SendingHardware<GirsClient<?>> implements ISendingHardware<GirsClient<?>> {
    private GirsClientBean girsClientBean;

    public SendingGirsClient(JPanel panel, GirsClientBean girsClientBean, Props props, GuiUtils guiUtils_) {
        super(panel, props, guiUtils_);
        this.girsClientBean = girsClientBean;
    }

    @Override
    public void setup() throws IOException, HarcHardwareException {
        girsClientBean.initHardware();
    }

    @Override
    public String getName() {
        return "GirsClient";
    }

    @Override
    public GirsClient<?> getRawIrSender() {
        return girsClientBean.getHardware();
    }
}
