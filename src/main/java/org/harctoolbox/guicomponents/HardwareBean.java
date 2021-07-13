/*
 * Copyright (C) 2021 Bengt Martensson
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.harctoolbox.guicomponents;

import java.awt.Cursor;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.Closeable;
import java.io.IOException;
import javax.swing.JPanel;
import org.harctoolbox.harchardware.HarcHardwareException;
import org.harctoolbox.harchardware.IHarcHardware;
import org.harctoolbox.harchardware.ir.NoSuchTransmitterException;
import org.harctoolbox.ircore.InvalidArgumentException;
import org.harctoolbox.ircore.IrSignal;
import org.harctoolbox.ircore.ModulatedIrSequence;
import org.harctoolbox.irscrutinizer.HardwareUnavailableException;

/**
 *
 *
 */
public abstract class HardwareBean extends JPanel implements Closeable {

    public static final String PROP_VERSION = "PROP_VERSION";
    public static final String PROP_BAUD = "PROP_BAUD";
    public static final String PROP_ISOPEN = "PROP_ISOPEN";
    public static final String PROP_PROPS = "PROP_PROPS";
    public static final String PROP_PORTNAME = "PROP_PORTNAME";
    public static final String PROP_IPNAME = "PROP_IPNAME";
    public static final String PROP_MODULE = "PROP_MODULE";
    public static final String PROP_PORT = "PROP_PORT";

    protected final PropertyChangeSupport propertyChangeSupport;
    protected final GuiUtils guiUtils;
    protected boolean verbose;
    protected int timeout;
    protected IHarcHardware hardware;

    HardwareBean(final GuiUtils guiUtils, boolean verbose, int timeout) {
        this.propertyChangeSupport = new java.beans.PropertyChangeSupport(this);
        this.guiUtils = guiUtils;
        this.verbose = verbose;
        this.timeout = timeout;
        this.hardware = null;
    }

   @Override
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        if (propertyChangeSupport != null)
            propertyChangeSupport.addPropertyChangeListener(listener);
    }

    @Override
    public void removePropertyChangeListener(PropertyChangeListener listener) {
        propertyChangeSupport.removePropertyChangeListener(listener);
    }

    public IHarcHardware getHardware() {
        return hardware;
    }

    /**
     * @param timeout the timeout to set
     */
    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    /**
     * @param verbose the verbose to set
     */
    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

    /**
     * Default implementation, override if necessary.
     * @return
     */
    public JPanel getTabPanel() {
        return (JPanel) getParent();
    }

    protected void openClose(boolean opening) throws IOException {
        if (opening)
            open();
        else
            close();
    }

    abstract void open() throws IOException;

    @Override
    public abstract void close();

    public boolean isOpen() {
        return hardware != null && hardware.isValid();
    }

    protected void assertHardwareNonNull() throws HardwareUnavailableException {
        if (hardware == null)
            throw new HardwareUnavailableException("No hardware selected.");
    }

    protected void assertHardwareValid() throws HardwareUnavailableException {
        assertHardwareNonNull();
        if (!hardware.isValid())
            throw new HardwareUnavailableException("Hardware not opened.");
    }

    protected Cursor setBusyCursor() {
        Cursor oldCursor = getCursor();
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        return oldCursor;
    }

    protected void resetCursor(Cursor cursor) {
        setCursor(cursor);
    }

    /**
     * Default implementation.
     * @return false
     */
    public boolean canCapture() {
        return false;
    }

    /**
     * Default implementation.
     * @return false
     */
    public boolean canSend() {
        return false;
    }

    public ModulatedIrSequence capture() throws CannotCaptureException {
        throw new CannotCaptureException(getName());
    }

    public boolean sendIr(IrSignal irSignal, int count) throws NoSuchTransmitterException, IOException, HardwareUnavailableException, HarcHardwareException, InvalidArgumentException {
        throw new CannotSendException(getName());
    }

   public static class CannotCaptureException extends HarcHardwareException {

        private CannotCaptureException(String name) {
            super("Selected hardware " + name + " cannot capture.");
        }
    }

    public static class CannotSendException extends HarcHardwareException {

        public CannotSendException(String name) {
            super("Selected hardware " + name + " cannot send.");
        }
    }
}