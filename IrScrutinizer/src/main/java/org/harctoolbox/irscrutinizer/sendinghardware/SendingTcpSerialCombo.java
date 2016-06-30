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

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import javax.swing.JPanel;
import org.harctoolbox.guicomponents.GuiUtils;
import org.harctoolbox.guicomponents.SerialPortSimpleBean;
import org.harctoolbox.guicomponents.TcpSerialComboBean;
import org.harctoolbox.harchardware.IHarcHardware;
import org.harctoolbox.harchardware.ir.IRawIrSender;
import org.harctoolbox.irscrutinizer.Props;

/**
 * @param <T>
 */
public class SendingTcpSerialCombo<T extends IRawIrSender & IHarcHardware> extends SendingHardware<T> implements ISendingHardware<T> {
    private TcpSerialComboBean tcpSerialComboBean;
    private String portName;
    private int baudRate;
    private Class<T> clazz;

    public SendingTcpSerialCombo(final Class<T> clazz, JPanel panel, TcpSerialComboBean tcpSerialComboBean, Props props, GuiUtils guiUtils_) {
        super(panel, props, guiUtils_);
        this.tcpSerialComboBean = tcpSerialComboBean;
        this.baudRate = tcpSerialComboBean.getBaudRate();
        this.portName = tcpSerialComboBean.getPortName();
        this.clazz = clazz;
        tcpSerialComboBean.addPropertyChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                String propertyName = evt.getPropertyName();
                try {
                    switch (propertyName) {
                        case SerialPortSimpleBean.PROP_VERSION:
                            break;
                        case SerialPortSimpleBean.PROP_PORTNAME:
                            if (evt.getNewValue() == null)
                                return;
                            setup();
                            break;
                        case SerialPortSimpleBean.PROP_BAUD:
                            setup();
                            break;
                        case SerialPortSimpleBean.PROP_ISOPEN:
                            break;
                        default:
                            guiUtils.error("Unknown property " + propertyName);
                            break;
                    }
                } catch (IOException ex) {
                    guiUtils.error(ex);
                }
            }
        });
    }

    @Override
    public void setup() throws IOException {/*
        int newBaud = tcpSerialComboBean.getBaudRate();
        String newPort = tcpSerialComboBean.getPortName();
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
            tcpSerialComboBean.setHardware(rawIrSender);
        } catch (NoSuchMethodException | SecurityException | InstantiationException | IllegalAccessException | IllegalArgumentException ex) {
            guiUtils.error(ex);
        } catch (InvocationTargetException ex) {
            // Likely NoSuchPortException
            if (NoSuchPortException.class.isInstance(ex.getCause()))
                throw new IOException("No such port: " + newPort);
            else
                guiUtils.error(ex);
            guiUtils.error(ex.getCause().getClass().getName() + " " + ex.getCause().getMessage());
        }*/
        tcpSerialComboBean.setHardware(properties.getVerbose());
    }

    @Override
    public String getName() {
        return clazz.getSimpleName();
    }
}
