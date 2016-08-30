/*
Copyright (C) 2012,2013,2014 Bengt Martensson.

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

package org.harctoolbox.harchardware.comm;

import gnu.io.NoSuchPortException;
import gnu.io.PortInUseException;
import gnu.io.UnsupportedCommOperationException;
import java.io.IOException;
import java.util.ArrayList;
import org.harctoolbox.harchardware.Utils;
import org.harctoolbox.harchardware.misc.SonySerialCommand;

public final class LocalSerialPortRaw extends LocalSerialPort implements IBytesCommand {

    public static void main(String[] args) {
        ArrayList<String> names;
        try {
            names = getSerialPortNames(false);
            for (String name : names) {
                System.out.println(name);
            }

            LocalSerialPortRaw port = new LocalSerialPortRaw(defaultPort, 38400, 8, 1, Parity.EVEN, FlowControl.NONE, 10000, true);

            if (args.length == 0) {
                int upper = 0x1;
                int lower = 0x13;
                SonySerialCommand.Type type = SonySerialCommand.Type.get;
                //byte[] cmd = SonySerialCommand.bytes(0x17, 0x15); // power toggle
                //byte[] cmd = SonySerialCommand.bytes(0x17, 0x2f); // power off
                byte[] cmd = SonySerialCommand.bytes(upper, lower, type); // get lamp time
                port.sendBytes(cmd);
                if (upper <= 1) {
                    byte[] answer = port.readBytes(SonySerialCommand.size);
                    for (int i = 0; i < SonySerialCommand.size; i++) {
                        System.out.println(i + "\t" + answer[i]);
                    }
                    SonySerialCommand.Command response = SonySerialCommand.interpret(answer);
                    System.out.println(response);
                }
                port.close();
            }
        } catch (NoSuchPortException | PortInUseException | UnsupportedCommOperationException | IOException ex) {
            System.err.println(ex.getMessage());
        }
    }

    public LocalSerialPortRaw(String portName, int baud, int length, int stopBits, Parity parity, FlowControl flowControl, int timeout, boolean verbose) throws NoSuchPortException, PortInUseException, UnsupportedCommOperationException, IOException {
        super(portName, baud, length, stopBits, parity, flowControl, timeout);
    }

    @Override
    public byte[] readBytes(int size) throws IOException {
        return Utils.readBytes(inStream, size);
    }

    public int readBytes(byte[] buf) throws IOException {
        return inStream.read(buf);
    }

    public int readByte() throws IOException {
        return inStream.read();
    }

    @Override
    public void sendBytes(byte[] data) throws IOException {
        outStream.write(data);
    }

    public void sendBytes(byte[] data, int offset, int length) throws IOException {
        outStream.write(data, offset, length);
    }

    public void sendByte(byte b) throws IOException {
        outStream.write(b);
    }


    @Override
    public void setDebug(int debug) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
