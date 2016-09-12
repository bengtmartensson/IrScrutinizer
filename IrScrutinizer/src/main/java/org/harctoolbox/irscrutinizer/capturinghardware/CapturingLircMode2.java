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

import java.io.IOException;
import javax.swing.JPanel;
import org.harctoolbox.guicomponents.GuiUtils;
import org.harctoolbox.harchardware.HarcHardwareException;
import org.harctoolbox.harchardware.IHarcHardware;
import org.harctoolbox.harchardware.ir.LircMode2;
import org.harctoolbox.irscrutinizer.Props;

/**
 * This class does something interesting and useful. Or not...
 */
public class CapturingLircMode2 extends CapturingHardware<LircMode2> implements ICapturingHardware<LircMode2>,IHarcHardware {

    private String commandName;
    private LircMode2 hardware;

    // Since the LIRC process is finicky, start it only once, and then leave it running until the program terminates.
    public CapturingLircMode2(String commandName, JPanel panel, Props properties, GuiUtils guiUtils,
            CapturingHardwareManager capturingHardwareManager) {
        super(panel, properties, guiUtils, capturingHardwareManager);
        this.commandName = commandName;
    }

    /**
     * @return the commandName
     */
    public String getCommandName() {
        return commandName;
    }

    /**
     * @param commandName the commandName to set
     */
    public void setCommandName(String commandName) {
        this.commandName = commandName;
        if (hardware != null)
            hardware.setCommand(commandName);
        properties.setLircMode2Command(commandName);
    }

    @Override
    public void open() throws IOException {
        hardware.open();
    }

    @Override
    public void setup() {
        //setupHardwareCommonStart();
        try {
            hardware = new LircMode2(commandName, properties.getVerbose(),
                    properties.getCaptureBeginTimeout(), properties.getCaptureMaxSize(), properties.getCaptureEndingTimeout());
            selectMe();
        } catch (IOException | HarcHardwareException ex) {
            guiUtils.error(ex);
        }
        //setupHardwareCommonEnd(lircMode2command);
        //properties.setCaptureDevice(lircMode2RadioButtonMenuItem.getText());

        //setupHardwareCommonEnd(lircMode2command);
        //properties.setCaptureDevice(lircMode2RadioButtonMenuItem.getText());
    }

    @Override
    public String getName() {
        return "LIRC Mode 2";
    }

    @Override
    public void setDebug(int debug) {
    }

    @Override
    public LircMode2 getCapturer() {
        return hardware;
    }
}
