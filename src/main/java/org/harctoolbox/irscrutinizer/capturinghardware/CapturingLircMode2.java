/*
Copyright (C) 2013, 2017 Bengt Martensson.

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
import org.harctoolbox.harchardware.ir.LircMode2;
import org.harctoolbox.irscrutinizer.Props;

/**
 * This class does something interesting and useful. Or not...
 */
public class CapturingLircMode2 extends CapturingHardware<LircMode2> implements ICapturingHardware<LircMode2> {

    private LircMode2 hardware;
    private ProcessBuilder processBuilder;
    private Process process;
    private String[] command;
    private boolean useStdin;

    public CapturingLircMode2(boolean useStdin, String commandName, JPanel panel, Props properties, GuiUtils guiUtils,
            CapturingHardwareManager capturingHardwareManager) {
        super(panel, properties, guiUtils, capturingHardwareManager);
        command = commandName.split("\\s+");
        hardware = null;
        this.useStdin = useStdin;
    }

    /**
     * @param useStdin
     * @param commandName the commandName to set
     */
    public void setCommandName(boolean useStdin, String commandName) {
        this.useStdin = useStdin;
        command = commandName.split("\\s+");
    }

    @Override
    public void open() throws IOException {
        close();
        if (useStdin)
            hardware = new LircMode2(properties.getVerbose(), properties.getCaptureEndingTimeout());
        else {
            processBuilder = new ProcessBuilder(command);
            processBuilder.redirectInput(ProcessBuilder.Redirect.INHERIT);
            processBuilder.redirectError(ProcessBuilder.Redirect.INHERIT);
            process = processBuilder.start();
            hardware = new LircMode2(process.getInputStream(), properties.getVerbose(), properties.getCaptureEndingTimeout());
        }
    }

    public void stop() throws IOException {
        if (hardware != null)
            hardware.close();
        if (process != null)
            process.destroy();
        process = null;
        processBuilder = null;
    }

    @Override
    public void setup() {
        try {
            selectMe();
        } catch (IOException | HarcHardwareException ex) {
            guiUtils.error(ex);
        }
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
