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
import org.harctoolbox.IrpMaster.IrpMasterException;
import org.harctoolbox.IrpMaster.ModulatedIrSequence;
import org.harctoolbox.guicomponents.GuiUtils;
import org.harctoolbox.harchardware.HarcHardwareException;
import org.harctoolbox.harchardware.ir.ICapture;
import org.harctoolbox.harchardware.IHarcHardware;
import org.harctoolbox.irscrutinizer.Props;

/**
 * This class does something interesting and useful. Or not...
 * @param <T>
 */
public abstract class CapturingHardware <T extends ICapture & IHarcHardware> {
    private final JPanel panel;

    protected T hardware = null;
    protected Props properties;
    protected GuiUtils guiUtils;
    protected boolean verbose;
    //protected int debug = 0;
    protected int beginTimeout;
    protected int maxLearnLength;
    protected int endTimeout;
    private final CapturingHardwareManager capturingHardwareManager;

    protected CapturingHardware(JPanel panel, Props properties, GuiUtils guiUtils,
            CapturingHardwareManager capturingHardwareManager) {
        this.panel = panel;
        this.properties = properties;
        this.guiUtils = guiUtils;
        this.capturingHardwareManager = capturingHardwareManager;
        //this.startButton = startButton;
        this.verbose = properties.getVerbose();
        this.beginTimeout = properties.getCaptureStartTimeout();
        this.maxLearnLength = properties.getCaptureRunTimeout();
        this.endTimeout = properties.getCaptureEndingTimeout();
    }

    /**
     * @return the panel
     */
    public JPanel getPanel() {
        return panel;
    }

    /**
     * @return the hardware
     */
    public T getCapturer() {
        return hardware;
    }

    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

    public void close() {
        try {
            if (hardware != null && hardware.isValid())// && captureDevice != lircMode2command)
                hardware.close();
        } catch (IOException ex) {
            guiUtils.warning("Could not close present capture device: " + ex.getMessage());
        }
    }

    //public void open() throws IOException, NoSuchPortException, PortInUseException, UnsupportedCommOperationException {
    //    hardware.open();
    //}

    protected void setupHardwareCommonEnd() {
        if (verbose && hardware != null)
            try {
                guiUtils.info("Capture device: " + hardware.getClass().getSimpleName() + ", Hardware version: " + hardware.getVersion());
            } catch (IOException ex) {
                guiUtils.error(ex);
            }
        //startButton.setEnabled(hardware != null);
    }

    protected void selectMe() throws IOException, HarcHardwareException {
        capturingHardwareManager.select(getName(), false);
    }

    //public ModulatedIrSequence capture() throws HarcHardwareException, IOException {
    //    return hardware.capture(beginTimeout, maxLearnLength, endTimeout);
    //}

    public ModulatedIrSequence capture() throws HarcHardwareException, IOException, IrpMasterException {
        return hardware.capture();
    }

    public boolean stopCapture() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public String getVersion() throws IOException {
        return hardware.getVersion();
    }

    public void setVerbosity(boolean verbosity) {
        hardware.setVerbosity(verbosity);
    }

    public void setTimeout(int i) throws IOException {
        hardware.setTimeout(i);
    }

    public boolean isValid() {
        return hardware != null && hardware.isValid();
    }

    public abstract String getName();
}
