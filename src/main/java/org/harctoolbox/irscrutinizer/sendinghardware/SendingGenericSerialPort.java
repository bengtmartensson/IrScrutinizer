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

import gnu.io.NoSuchPortException;
import gnu.io.PortInUseException;
import gnu.io.UnsupportedCommOperationException;
import java.beans.PropertyChangeEvent;
import java.io.IOException;
import javax.swing.JPanel;
import org.harctoolbox.guicomponents.GuiUtils;
import org.harctoolbox.harchardware.HarcHardwareException;
import org.harctoolbox.harchardware.comm.LocalSerialPort;
import org.harctoolbox.harchardware.ir.IrGenericSerial;
import org.harctoolbox.irscrutinizer.Props;

/**
 * This class does something interesting and useful. Or not...
 */
public class SendingGenericSerialPort extends SendingHardware<IrGenericSerial> implements ISendingHardware<IrGenericSerial> {
    private GenericSerialSenderBean genericSerialSenderBean;
    //private IrGenericSerial irGenericSerial;
//    private String command;
//    private boolean raw;
//    private String separator;
//    private boolean useSigns;
//    private String lineEnding;
    private String initialPort;
    private String portName;
    private IrGenericSerial rawIrSender;

    /**
     *
     * @param panel
     * @param genericSerialSenderBean_
     * @param properties_
     * @param guiUtils_
     */

    public SendingGenericSerialPort(JPanel panel, GenericSerialSenderBean genericSerialSenderBean_, Props properties_, GuiUtils guiUtils_) {
        super(panel, properties_, guiUtils_);
        this.portName = null;
        this.genericSerialSenderBean = genericSerialSenderBean_;
        /*this.command = command;
        this.raw = raw;
        this.separator = separator;
        this.useSigns = useSigns;
        this.lineEnding = lineEnding;*/
        rawIrSender = null;
        initialPort = properties.getGenericSerialPortDeviceName();
        //serialPortBean.setVerbose(properties.getVerbose());
        genericSerialSenderBean.addPropertyChangeListener((PropertyChangeEvent evt) -> {
            switch (evt.getPropertyName()) {
                case GenericSerialSenderBean.PROP_PORTNAME:
                    properties.setGenericSerialPortDeviceName((String) evt.getNewValue());
                    try {
                        setupGenericSerialPort();
                    } catch (IOException ex) {
                        guiUtils.error(ex);
                    }
                    break;
                case GenericSerialSenderBean.PROP_COMMAND:
                    rawIrSender.setCommand(genericSerialSenderBean.getCommand());
                    break;
                case GenericSerialSenderBean.PROP_RAW:
                    rawIrSender.setRaw(genericSerialSenderBean.getRaw());
                    break;
                case GenericSerialSenderBean.PROP_SEPARATOR:
                    rawIrSender.setSeparator(genericSerialSenderBean.getSeparator());
                    break;
                case GenericSerialSenderBean.PROP_USESIGNS:
                    rawIrSender.setUseSigns(genericSerialSenderBean.getUseSigns());
                    break;
                case GenericSerialSenderBean.PROP_LINEENDING:
                    rawIrSender.setLineEnding(genericSerialSenderBean.getLineEnding());
                    break;
                default:
                    throw new RuntimeException("Programming error");
            }
        });
    }

    @Override
    public void setup() throws IOException, HarcHardwareException {
        genericSerialSenderBean.setup(initialPort);
        initialPort = null;
        setupGenericSerialPort();
        //hasChanged = false;
    }

    // Temporarily disabled
    private void setupGenericSerialPort() throws IOException {
        String newPort = this.genericSerialSenderBean.getPortName();
        if (rawIrSender != null && (newPort == null || newPort.equals(portName)))
            return;

        if (rawIrSender != null)
            rawIrSender.close();
        rawIrSender = null;

        //genericSerialSenderBean.setVerbose(properties.getVerbose());
        close();

//        try {
            rawIrSender = null;
//            new IrGenericSerial(genericSerialSenderBean.getPortName(), properties.getVerbose(), properties.getSendingTimeout(),
//                    genericSerialSenderBean.getBaud(),
//                    genericSerialSenderBean.getDataSize(), genericSerialSenderBean.getStopBits(), genericSerialSenderBean.getParity(),
//                    genericSerialSenderBean.getFlowControl());
//            rawIrSender.setCommand(genericSerialSenderBean.getCommand());
//            rawIrSender.setRaw(genericSerialSenderBean.getRaw());
//            rawIrSender.setSeparator(genericSerialSenderBean.getSeparator());
//            rawIrSender.setUseSigns(genericSerialSenderBean.getUseSigns());
//            rawIrSender.setLineEnding(genericSerialSenderBean.getLineEnding());
//        } catch (NoSuchPortException | PortInUseException | UnsupportedCommOperationException | IOException ex) {
//            // Should not happen
//            guiUtils.error(ex);
//        }
        portName = genericSerialSenderBean.getPortName();
        genericSerialSenderBean.setHardware(rawIrSender);
    }

    @Override
    public String getName() {
        return "Generic serial port";
    }

    @Override
    public IrGenericSerial getRawIrSender() {
        return rawIrSender;
    }
}
