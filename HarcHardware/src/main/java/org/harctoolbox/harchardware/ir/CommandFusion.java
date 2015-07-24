/*
Copyright (C) 2015 Bengt Martensson.

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

package org.harctoolbox.harchardware.ir;

import gnu.io.NoSuchPortException;
import gnu.io.PortInUseException;
import gnu.io.UnsupportedCommOperationException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.harctoolbox.IrpMaster.DecodeIR;
import org.harctoolbox.IrpMaster.IncompatibleArgumentException;
import org.harctoolbox.IrpMaster.IrSignal;
import org.harctoolbox.IrpMaster.IrpMasterException;
import org.harctoolbox.IrpMaster.ModulatedIrSequence;
import org.harctoolbox.IrpMaster.Pronto;
import org.harctoolbox.harchardware.HarcHardwareException;
import org.harctoolbox.harchardware.comm.LocalSerialPort;
import org.harctoolbox.harchardware.comm.LocalSerialPortRaw;

/**
 * This class implements capturing and sending support for the CommandFusion Learner.
 * @see http://www.commandfusion.com/wiki2/hardware/cflink/ir-learner
 * @see http://www.commandfusion.com/wiki2/hardware/cflink/ir-module
 * @see https://docs.google.com/document/d/1BMRwD9RlUYtf4VeJNXgRwo6-lkkSAIVo8tczrynJ7CU/preview?pli=1
 */
public class CommandFusion extends IrSerial<LocalSerialPortRaw> implements IRawIrSender, ICapture {
    // USB parameters:
    //    VID = 0403
    //    PID = 6001

    /*
    From http://www.commandfusion.com/wiki2/hardware/cflink/ir-module
    Error numbers:
    003 = Invalid Port Number
    004 = Invalid Module Number
    450 = Invalid IR Format Type
    451 = Invalid IR Database Parameters
    452 = Invalid IR Memory Parameters
    453 = Invalid IR Raw Hex Code
    454 = Invalid CF IR format
    */

    private static enum Status {
        ok,
        timeout,
        error
    }

    private static class Payload {
        public String command;
        public String data;

        @Override
        public String toString() {
            return "command \"" + command + "\", data \"" + data + "\"";
        }
    }

    private int beginTimeout;
    private int middleTimeout;
    private int endingTimeout;

    private final static int defaultBeginTimeout = 20000;
    private final static int defaultMiddleTimeout = 2000;
    private final static int defaultEndingTimeout = -1;
    private final static int defaultSerialTimeout = 30000;
    private static final int portId = 1;
    private static final byte[] introBytes = { (byte) 0xF2, (byte) portId, (byte) 0xF3 };
    private static final byte middleToken = (byte) 0xF4;
    private static final byte endingToken = (byte) 0xF5;
    private static final byte transmitToken = (byte) 'T';
    private static final byte receiveToken = (byte) 'R';
    private static final byte queryToken = (byte) 'Q';
    private static final int commandLength = 3;
    private static final String learnerName = "IRL";
    private static final String sendCommand = "SND";
    private static final String captureCommand = "LIR";
    private static final String readCommand = "RIR";
    private static final String versionCommand = "WHO";
    private static final String timeout = "TIMEOUT";
    private static final String start = "START";
    private static final String signal = "SIGNAL";
    private static final String ircode = "IRCODE";
    private static final String end = "END";
    private static final int tick = 25; // micro seconds
    public static final String defaultPortName = "/dev/ttyUSB0";
    public static final int defaultBaudRate = 115200;
    private static final int dataSize = 8;
    private static final int stopBits = 1;
    private static final LocalSerialPort.Parity parity = LocalSerialPort.Parity.NONE;
    private static final LocalSerialPort.FlowControl defaultFlowControl = LocalSerialPort.FlowControl.NONE;
    private boolean stopRequested = false;
    private int serialTimeout = 30000;//12345;
    private String versionString = "n/a";

    public CommandFusion() throws NoSuchPortException, PortInUseException, UnsupportedCommOperationException, IOException {
        this(defaultPortName, defaultBaudRate, defaultBeginTimeout, defaultMiddleTimeout, defaultEndingTimeout, false);
    }

    public CommandFusion(String portName) throws NoSuchPortException, PortInUseException, UnsupportedCommOperationException, IOException {
        this(portName, defaultBaudRate, defaultBeginTimeout, defaultMiddleTimeout, defaultEndingTimeout, false);
    }

    public CommandFusion(String portName, boolean verbose) throws NoSuchPortException, PortInUseException, UnsupportedCommOperationException, IOException {
        this(portName, defaultBaudRate, defaultBeginTimeout, defaultMiddleTimeout, defaultEndingTimeout, verbose);
    }

    public CommandFusion(String portName, int baudRate , int beginTimeout, boolean verbose) throws NoSuchPortException, PortInUseException, UnsupportedCommOperationException, IOException {
        this(portName, baudRate, beginTimeout, defaultMiddleTimeout, defaultEndingTimeout, verbose);
    }

    public CommandFusion(String portName, int baudRate, int beginTimeout, int middleTimeout, int endingTimeout, boolean verbose)
            throws NoSuchPortException, PortInUseException, UnsupportedCommOperationException, IOException {
        super(LocalSerialPortRaw.class, portName, baudRate, dataSize, stopBits, parity, defaultFlowControl, beginTimeout, verbose);
        this.serialTimeout = beginTimeout;
        this.middleTimeout = middleTimeout;
    }

    @Override
    public void open() throws IOException, HarcHardwareException {
        super.open();
        send(encode(versionCommand, "", queryToken));
        byte[] response = readUntilTwoEndTokens();
        Payload payload = decode(response, receiveToken);
        if (verbose)
            System.err.println("<Received " + payload);
        if (payload == null) {
            close();
            throw new HarcHardwareException("Cannot open CommandFusion.");
        }
        if (payload.command.equals(versionCommand)) {
            String s[] = payload.data.split(":");
            versionString = s[2];
        }
    }

    /**
     * Sends an IR signal from the built-in, proprietary data base.
     *
     * @param deviceType
     * @param codeset
     * @param key function code
     * @return success of operation
     * @throws IOException
     *
     * @see http://www.commandfusion.com/irdatabase
     */
    public boolean sendIr(int deviceType, int codeset, int key) throws IOException {
        return sendIr(encode(sendCommand,
                             String.format("P%02d:DBA:%02d:%04d:%02d", portId, deviceType, codeset, key)));
    }

    @Override
    public boolean sendIr(IrSignal irSignal, int count, Transmitter transmitter) throws IncompatibleArgumentException, IOException {
        return sendIr(encode(irSignal, count));
    }

    private synchronized boolean sendIr(byte[] data) throws IOException {
        send(data);
        return expect(sendCommand, "") == Status.ok;
    }

    private Status expect(String command, String data) throws IOException {
        byte[] response = readUntilTwoEndTokens();// serialPort.readBytes(13 + data.length());
        Payload payload = decode(response, receiveToken);
        if (verbose)
            System.err.println("<Received " + payload);

        return payload == null ? Status.error
                : payload.data.equals(timeout) ? Status.timeout
                : (payload.command.equals(command) && payload.data.equals(data)) ? Status.ok
                : Status.error;
    }

    private byte[] encode(IrSignal irSignal, int count) throws IncompatibleArgumentException {
        if (irSignal == null)
            throw new IllegalArgumentException("irSignal cannot be null");
        String data = "P0" + Integer.toString(portId) + ":RAW:" + irSignal.toOneShot(count).ccfString();
        return encode(sendCommand, data);
    }

    // "All communications to and from the IR Learner’s serial port use the below format:
    //  \xF2\x01\xF3<COMMAND>\xF4<DATA>\xF5\xF5"

    // <COMMAND> is always made up of 7 characters:
    // 1 = T (transmitted TO IR learner) or R (reply FROM IR learner)
    // 2-4 = IRL (signifying we are communicating with an IR Learner)
    // 5-7 = The command name. See below for available commands.
    private Payload decode(byte[] data, Byte token) {
        int index = 0;
        for (int i = 0; i < introBytes.length; i++)
            if (data[index++] != introBytes[i])
                return null;
        if (token != null && data[index] != token)
                return null;
        index++;
        for (int i = 0; i < learnerName.length(); i++)
            if (data[index++] != learnerName.charAt(i))
                return null;
        Payload payload = new Payload();
        payload.command = new String(data, index, commandLength, Charset.forName("US-ASCII"));
        index += commandLength;
        if (data[index++] != middleToken)
                return null;
        for (int i = data.length-2; i < data.length; i++)
            if (data[i] != endingToken)
                return null;
        payload.data = new String(data, index, data.length - index - 2, Charset.forName("US-ASCII")); // possilby ""
        return payload;
    }

    private byte[] encode(String cmd, String data) {
        return encode(cmd, data, transmitToken);
    }

    private byte[] encode(String cmd, String data, byte token) {
        byte[] result = new byte[7 + learnerName.length() + cmd.length() + data.length()];
        int index = 0;
        for (int i = 0; i < introBytes.length; i++)
            result[index++] = introBytes[i];
        result[index++] = token;
        for (int i = 0; i < learnerName.length(); i++)
            result[index++] = (byte) learnerName.charAt(i);
        for (int i = 0; i < cmd.length(); i++)
            result[index++] = (byte) cmd.charAt(i);
        result[index++] = (byte) 0xF4;
        for (int i = 0; i < data.length(); i++)
            result[index++] = (byte) data.charAt(i);
        result[index++] = (byte) 0xF5;
        result[index++] = (byte) 0xF5;
        return result;
    }

    private byte[] encode(String cmd) {
        return encode(cmd, "");
    }

    private byte[] readUntilTwoEndTokens() throws IOException {
        ArrayList<Byte> data = new ArrayList<Byte>(200);
        int noEndingTokensFound = 0;
        while (noEndingTokensFound < 2) {
            byte x = (byte) serialPort.readByte();
            data.add(x);
            if (x == endingToken)
                noEndingTokensFound++;
        }
        byte[] result = new byte[data.size()];
        int i = 0;
        for (Byte b : data)
            result[i++] = b;
        return result;
    }

    private void send(byte[] buf) throws IOException {
        if (verbose) {
            Payload payload = decode(buf, null);
            if (payload != null)
                System.err.println(">Sending " + payload);
        }
        serialPort.sendBytes(buf);
    }

    @Override
    public ModulatedIrSequence capture() throws IOException, IncompatibleArgumentException {
        //Send LIR command (Learn IR) to tell the learner we are ready to learn.
        send(encode(captureCommand));

        //Learner will reply with an LIR command with data value START
        Status status = expect(captureCommand, start);
        if (status != Status.ok)
            return null;

        //The learner is now waiting for you to press a button on your remote and send an IR signal to the ‘LEARN’ window of the IR Learner.
        //When a signal is first detected, IR Learner will send back an LIR command with data value SIGNAL. If no signal was detected for 20 seconds after the START command was issued, the IR learner will send back an LIR command with data value TIMEOUT
        status = expect(captureCommand, signal); // Hangs until signal starts, or timeout in 20 seconds
        if (status != Status.ok)
            return null;

        try {
            //Wait for 2 seconds for the IR buffer to fill whilst holding the remote button.
            Thread.sleep(middleTimeout);
        } catch (InterruptedException ex) {
        }

        //Send a RIR command (Read IR) with no data.
        send(encode(readCommand));

        //IR Learner will send back a RIR reply with data in the format of IRCODE:<irdata>
        //Process the IR Data using the CFIRProcessor.dll or your own code. The IR data format is documented below.
        ModulatedIrSequence modulatedIrSequence = null;
        try {
            modulatedIrSequence = readCapture();
        } catch (IncompatibleArgumentException ex) {
            throw ex;
        } finally {
            //Finally, the IR Learner will send back an LIR command with a data value END to signify the end of the IR
            // Do this also in the case of an exception, to not lose sync.
            status = expect(captureCommand, end);
        }
        if (status != Status.ok)
            return null;

        return modulatedIrSequence;
    }

    ModulatedIrSequence readCapture() throws IOException, IncompatibleArgumentException {
        //IR Learner will send back a RIR reply with data in the format of IRCODE:<irdata>
        byte[] response = readUntilTwoEndTokens();
        Payload payload = decode(response, receiveToken);
        if (verbose) {
            System.err.println("<Received " + payload);
        }
        if (payload == null || !payload.command.equals(captureCommand))
            return null;

        if (!payload.data.startsWith(ircode + ":"))
            return null;
        int index = ircode.length() + 1;
        double frequency = Pronto.getFrequency(Integer.parseInt(payload.data.substring(index, index+4), 16));
        index += 4;
        if ((payload.data.length() - index) % 4 != 0)
            throw new IncompatibleArgumentException("Receive length erroneous");

        ArrayList<Integer> durations = new ArrayList<Integer>(payload.data.length() - index);
        boolean lastState = false;
        int accumulated = 0;
        for (int i = index; i < payload.data.length(); i += 4) {
            boolean state = payload.data.substring(i, i + 2).equals("01");
            int duration = Integer.parseInt(payload.data.substring(i + 2, i + 4), 16);
            if (lastState != state) {
                if (accumulated > 0)
                    durations.add(tick * accumulated);
                accumulated = duration;
            } else {
                accumulated += duration;
            }
            lastState = state;
        }
        durations.add(tick * accumulated);
        int[] data = new int[durations.size()];
        for (int i = 0; i < data.length; i++)
            data[i] = durations.get(i);

        ModulatedIrSequence result = new ModulatedIrSequence(data, frequency, -1.0);
        return result;
    }

    @Override
    public boolean stopCapture() {
        stopRequested = true;
        return true;
    }

    @Override
    public void setTimeout(int beginTimeout, int middleTimeout, int endingTimeout) throws IOException {
        //setTimeout(beginTimeout);
        this.serialTimeout = beginTimeout;
        this.middleTimeout = middleTimeout;
        //this.endingTimeout = endingTimeout;
    }

    @Override
    public String getVersion() {
        return versionString;
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        String portName = defaultPortName;
        CommandFusion commandFusion = null;
        boolean verbose = true;
        boolean send = true;
        try {
            commandFusion = new CommandFusion(portName, verbose);
            commandFusion.open();
            System.out.println("Version: " + commandFusion.getVersion());
            if (send) {
                IrSignal irSignal = new IrSignal("/local/irscrutinizer/IrpProtocols.ini", "rc5", "D=0 F=0");
                boolean success = commandFusion.sendIr(irSignal, 1, null);
                //boolean success = w.sendIr(0, 556, 18);
                System.out.println(success ? "Sending succeeded" : "Sending failed");
            } else {
                System.out.println("Press a key");
                ModulatedIrSequence seq = commandFusion.capture();
                if (seq == null) {
                    System.err.println("No input");
                    commandFusion.close();
                    System.exit(1);
                }
                System.out.println(seq);
                DecodeIR.invoke(seq);
            }
        } catch (IOException ex) {
            System.err.println("exception: " + ex.toString() + ex.getMessage());
            //ex.printStackTrace();
        } catch (NoSuchPortException ex) {
            System.err.println("No such port: " + portName);
        } catch (HarcHardwareException ex) {
            System.err.println(ex.getMessage());
        } catch (PortInUseException ex) {
            System.err.println("Port " + portName + " in use.");
        } catch (UnsupportedCommOperationException ex) {
            System.err.println(ex.getMessage());
        } catch (IrpMasterException ex) {
            Logger.getLogger(CommandFusion.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            if (commandFusion != null)
                try {
                    commandFusion.close();
                } catch (IOException ex) {
                    System.err.println(ex.getMessage());
                }
        }
        System.exit(0);
    }
}
