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
import org.harctoolbox.harchardware.ir.ICapture;
import org.harctoolbox.ircore.InvalidArgumentException;
import org.harctoolbox.ircore.ModulatedIrSequence;
import org.harctoolbox.irscrutinizer.Props;

/**
 *
 * @param <T>
 */
public abstract class CapturingHardware <T extends ICapture & IHarcHardware> implements ICapture {
    private final JPanel panel;
    protected Props properties;
    protected GuiUtils guiUtils;
    private final CapturingHardwareManager capturingHardwareManager;

    protected CapturingHardware(JPanel panel, Props properties, GuiUtils guiUtils,
            CapturingHardwareManager capturingHardwareManager) {
        this.panel = panel;
        this.properties = properties;
        this.guiUtils = guiUtils;
        this.capturingHardwareManager = capturingHardwareManager;
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
    public abstract T getCapturer();

    @Override
    public void close() {
        try {
            if (getCapturer() != null && getCapturer().isValid())// && captureDevice != lircMode2command)
                getCapturer().close();
        } catch (IOException ex) {
            guiUtils.warning("Could not close present capture device: " + ex.getMessage());
        }
    }

    protected void setupHardwareCommonEnd() {
        if (properties.getVerbose() && getCapturer() != null)
            try {
                guiUtils.info("Capture device: " + getCapturer().getClass().getSimpleName() + ", Hardware version: " + getCapturer().getVersion());
            } catch (IOException ex) {
                guiUtils.error(ex);
            }
        //startButton.setEnabled(getCapturer() != null);
    }

    protected void selectMe() throws IOException, HarcHardwareException {
        capturingHardwareManager.select(getName());
    }

    @Override
    public ModulatedIrSequence capture() throws HarcHardwareException, IOException, InvalidArgumentException {
        return getCapturer().capture();
    }

    @Override
    public boolean stopCapture() {
        return getCapturer().stopCapture();
    }

    @Override
    public String getVersion() throws IOException {
        return getCapturer().getVersion();
    }

    @Override
    public void setVerbose(boolean verbose) {
        if (getCapturer() != null)
            getCapturer().setVerbose(verbose);
    }

    @Override
    public void setTimeout(int i) throws IOException {
        getCapturer().setTimeout(i);
    }

    @Override
    @SuppressWarnings("NoopMethodInAbstractClass")
    public void setDebug(int debug) {
    }

    @Override
    public void setBeginTimeout(int beginTimeout) throws IOException {
        getCapturer().setBeginTimeout(beginTimeout);
    }

    @Override
    public void setCaptureMaxSize(int captureMaxSize) {
        getCapturer().setCaptureMaxSize(captureMaxSize);
    }

    @Override
    public void setEndingTimeout(int endingTimeout) {
        getCapturer().setEndingTimeout(endingTimeout);
    }

    @Override
    public boolean isValid() {
        return getCapturer() != null && getCapturer().isValid();
    }

    public abstract String getName();
}
