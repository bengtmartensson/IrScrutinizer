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

package org.harctoolbox.harchardware.misc;

import gnu.io.NoSuchPortException;
import gnu.io.PortInUseException;
import gnu.io.UnsupportedCommOperationException;
import java.io.IOException;
import org.harctoolbox.IrpMaster.IrpUtils;
import org.harctoolbox.harchardware.HarcHardwareException;
import org.harctoolbox.harchardware.IHarcHardware;
import org.harctoolbox.harchardware.comm.IBytesCommand;
import org.harctoolbox.harchardware.comm.LocalSerialPort;
import org.harctoolbox.harchardware.comm.LocalSerialPortRaw;
import org.harctoolbox.harchardware.ir.GlobalCache;
import org.harctoolbox.harchardware.ir.NoSuchTransmitterException;

public class SonySerialCommand {

    public final static int size = 8;

    private final static byte startToken = (byte) 0xa9;
    private final static byte stopToken = (byte) 0x9a;

    public static byte[] bytes(int n1, int n2, Type sg, int payload) {
        return (new SonySerialCommand(n1, n2, sg, payload)).data.clone();
    }
    public static byte[] bytes(int n1, int n2, Type sg) {
        return bytes(n1, n2, sg, 0);
    }
    public static byte[] bytes(int n1, int n2) {
        return bytes(n1, n2, Type.set, 0);
    }
    public static Command interpret(byte[] data) {
        SonySerialCommand ssc = new SonySerialCommand(data);
        if (data[0] != startToken || data[size-1] != stopToken)
            return null;
        if (ssc.checksum() != ssc.data[6])
            return null;
        return new Command(ssc);
    }
    public static void main(String[] args) {
        boolean useGlobalCache = true;
        IBytesCommand port = null;

        if (useGlobalCache) {
            GlobalCache gc;
            try {
                gc = new GlobalCache("gc", true);
                gc.setSerial(1, "38400,FLOW_NONE,PARITY_EVEN");
                port = gc.getSerialPort(1);
            } catch (IOException | NoSuchTransmitterException ex) {
                System.err.println(ex.getMessage());
                System.exit(IrpUtils.exitIoError);
            }
        } else {
            try {
                port = new LocalSerialPortRaw("/dev/ttyS0", 38400, 8, 1, LocalSerialPort.Parity.EVEN, LocalSerialPort.FlowControl.NONE, 2000, true);
                ((IHarcHardware) port).open();
            } catch (NoSuchPortException | HarcHardwareException | PortInUseException | UnsupportedCommOperationException | IOException ex) {
                System.err.println(ex.getMessage());
                System.exit(IrpUtils.exitIoError);
            }
        }

        if (port == null) {
            System.err.println("Could not set up the serial port, bailing out");
            System.exit(IrpUtils.exitIoError);
        }

        int upper = 0x1;
        int lower = 0x13;
        SonySerialCommand.Type type = SonySerialCommand.Type.get;  // get lamp time
        //int upper = 0x17; int lower = 0x15; SonySerialCommand.Type type = SonySerialCommand.Type.set;
        //int upper = 0x17; int lower = 0x2f; SonySerialCommand.Type type = SonySerialCommand.Type.set;
        //byte[] cmd = SonySerialCommand.bytes(0x17, 0x15); // power toggle
        //byte[] cmd = SonySerialCommand.bytes(0x17, 0x2f); // power off
        byte[] cmd = SonySerialCommand.bytes(upper, lower, type);
        try {
            port.sendBytes(cmd);
            if (upper <= 1) {
                byte[] answer = port.readBytes(SonySerialCommand.size);
                //for (int i = 0; i < SonySerialCommand.size; i++)
                //    System.out.println(i + "\t" + answer[i]);

                SonySerialCommand.Command response = SonySerialCommand.interpret(answer);
                System.out.println(response);
                System.out.println(response.getData());
            }
            port.close();
        } catch (IOException ex) {
            System.err.println(ex.getMessage());
        }
    }

    private byte[] data;


    private SonySerialCommand() {
    }

    private SonySerialCommand(byte[] data) {
        if (data.length != size)
            throw new IllegalArgumentException("Wrong size data, " + data.length);
        this.data = data;
    }

    private SonySerialCommand(int n1, int n2, Type sg, int payload) {
        int index = 0;
        data = new byte[size];
        data[index++] = startToken;
        data[index++] = (byte) (n1 & 0xff);
        data[index++] = (byte) (n2 & 0xff);
        data[index++] = (byte) sg.toInt();
        data[index++] = (byte) ((payload >> Byte.SIZE) & 0xff);
        data[index++] = (byte) (payload & 0xff);
        data[index++] = checksum();
        data[index] = stopToken;
    }

    private SonySerialCommand(int n1, int n2, Type sg) {
        this(n1, n2, sg, 0);
    }

    private SonySerialCommand(int n1, int n2) {
        this(n1, n2, Type.set, 0);
    }
    private byte checksum() {
        int sum = 0;
        for (int i = 1; i <= 5; i++) {
            sum |= data[i];
        }
        return (byte)sum;
    }

    public static class Command {
        private final int n1;
        private final int n2;
        private final Type type;
        private final int data;

        public Command(int[] data) {
            n1 = data[0];
            n2 = data[1];
            type = Type.values()[data[2]];
            this.data = data[3];
        }

        public Command(SonySerialCommand ssc) {
            n1 = ssc.data[1];
            n2 = ssc.data[2];
            type = Type.values()[ssc.data[3]];
            data = 256*unsigned(ssc.data[4]) + unsigned(ssc.data[5]);
        }

        private int unsigned(byte b) {
            return b >= 0 ? b : b + 256;
        }

        @Override
        public String toString() {
            StringBuilder str = new StringBuilder(32);
            str.append("n1 = ").append(n1);
            str.append("; n2 = ").append(n2);
            str.append("; type = ").append(type);
            str.append("; data = ").append(data);

            return str.toString();
        }

        public int getData() {
            return data;
        }
    }
    public enum Type {

        set,
        get,
        replyWithData,
        replyWithoutData;

        int toInt() {
            return this.ordinal();
        }
    }
}
