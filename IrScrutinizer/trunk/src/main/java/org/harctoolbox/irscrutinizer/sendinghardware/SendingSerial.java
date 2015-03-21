/*
Copyright (C) 2013, 2014 Bengt Martensson.

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

import gnu.io.NoSuchPortException;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import javax.swing.JPanel;
import org.harctoolbox.guicomponents.GuiUtils;
import org.harctoolbox.guicomponents.SerialPortSimpleBean;
import org.harctoolbox.harchardware.IHarcHardware;
import org.harctoolbox.harchardware.ir.IRawIrSender;
import org.harctoolbox.irscrutinizer.Props;

/**
 * This class does something interesting and useful. Or not...
 * @param <T>
 */
public class SendingSerial<T extends IRawIrSender & IHarcHardware> extends SendingHardware<T> implements ISendingHardware<T> {
    private SerialPortSimpleBean serialPortSimpleBean;
    private String portName;
    private int baudRate;
    private Class<T> clazz;

    public SendingSerial(final Class<T> clazz, JPanel panel, SerialPortSimpleBean serialPortSimpleBean, Props props, GuiUtils guiUtils_) {
        super(panel, props, guiUtils_);
        this.serialPortSimpleBean = serialPortSimpleBean;
        this.baudRate = serialPortSimpleBean.getBaudRate();
        this.portName = serialPortSimpleBean.getPortName();
        this.clazz = clazz;
        serialPortSimpleBean.addPropertyChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                String propertyName = evt.getPropertyName();
                try {
                    if (propertyName.equals(SerialPortSimpleBean.PROP_VERSION)) {
                        // nothing
                    } else if (propertyName.equals(SerialPortSimpleBean.PROP_PORT)) {
                        if (evt.getNewValue() == null)
                            return;
                        setupSerial();
                    } else if (propertyName.equals(SerialPortSimpleBean.PROP_BAUD)) {
                        setupSerial();
                    } else if (propertyName.equals(SerialPortSimpleBean.PROP_ISOPEN)) {
                        // nothing
                    } else {
                        guiUtils.error("Unknown property " + propertyName);
                    }
                } catch (IOException ex) {
                    guiUtils.error(ex);
                }
            }
        });
    }

    private void setupSerial() throws IOException {
        int newBaud = serialPortSimpleBean.getBaudRate();
        String newPort = serialPortSimpleBean.getPortName();
        if (newPort == null || (rawIrSender != null && newPort.equals(portName) && (baudRate == newBaud)))
            return;

        if (rawIrSender != null)
            rawIrSender.close();
        rawIrSender = null;
        try {
            rawIrSender = clazz.getConstructor(String.class, int.class, int.class, boolean.class).newInstance(newPort, newBaud, properties.getSendingTimeout(), properties.getVerbose());
            portName = newPort;
            Props.class.getMethod("set" + clazz.getSimpleName() + "PortName", String.class).invoke(properties, portName);
            this.baudRate = newBaud;
            Props.class.getMethod("set" + clazz.getSimpleName() + "PortBaudRate", int.class).invoke(properties, newBaud);
            serialPortSimpleBean.setHardware(rawIrSender);
        } catch (NoSuchMethodException ex) {
            guiUtils.error(ex);
        } catch (SecurityException ex) {
            guiUtils.error(ex);
        } catch (InstantiationException ex) {
            guiUtils.error(ex);
        } catch (IllegalAccessException ex) {
            guiUtils.error(ex);
        } catch (IllegalArgumentException ex) {
            guiUtils.error(ex);
        } catch (InvocationTargetException ex) {
            // Likely NoSuchPortException
            if (NoSuchPortException.class.isInstance(ex.getCause()))
                throw new IOException("No such port: " + newPort);
            else
                guiUtils.error(ex);
            guiUtils.error(ex.getCause().getClass().getName() + " " + ex.getCause().getMessage());
        }
    }

    @Override
    public void setup() throws IOException {
        setupSerial();
    }

    @Override
    public String getName() {
        return clazz.getSimpleName();
    }
}
