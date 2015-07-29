/*
Copyright (C) 2013, 2014, 2015 Bengt Martensson.

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
import java.util.ArrayList;
import org.harctoolbox.IrpMaster.IncompatibleArgumentException;
import org.harctoolbox.IrpMaster.IrSequence;
import org.harctoolbox.IrpMaster.IrSignal;
import org.harctoolbox.IrpMaster.IrpMasterException;
import org.harctoolbox.IrpMaster.IrpUtils;
import org.harctoolbox.IrpMaster.ModulatedIrSequence;
import org.harctoolbox.IrpMaster.Pronto;
import org.harctoolbox.harchardware.HarcHardwareException;
import org.harctoolbox.harchardware.comm.LocalSerialPort;
import org.harctoolbox.harchardware.comm.LocalSerialPortRaw;

/**
 * This class contains a driver for Dangerous Prototype's IrToy.
 * @see <a href="http://www.dangerousprototypes.com/docs/USB_IR_Toy:_Sampling_mode">http://www.dangerousprototypes.com/docs/USB_IR_Toy:_Sampling_mode</a>
 *
 */
public final class IrToy extends IrSerial<LocalSerialPortRaw> implements IRawIrSender, ICapture, IReceive {

    public static final String defaultPortName = "/dev/ttyACM0";
    public static final int defaultBaudRate = 9600;//115200;//9600;
    public static final LocalSerialPort.FlowControl defaultFlowControl = LocalSerialPort.FlowControl.RTSCTS;
    public static final int defaultTimeout = 2000;
    public static final int defaultMaxLearnLength = 1000;

    private static final int dataSize = 8;
    private static final int stopBits = 1;
    private static final LocalSerialPort.Parity parity = LocalSerialPort.Parity.NONE;

    private static final int bufsize = 255;

    private static final double oscillatorFrequency = 48000000;
    private static final double period = 21.3333; // microseconds
    private static final double PICClockFrequency = 12000000;
    private static final byte dutyCycle = 0; // semantically: don't care

    private static final boolean transmitNotifyEnabled = true;
    private static final boolean transmitByteCountReportEnabled = true;
    private static final boolean transmitHandshakeEnabled = true;

    private final static byte cmdReset = 0x00; // Reset (returns to remote decoder mode)
    // 0x01 RESERVED for SUMP RUN
    // 0x02 RESERVED for SUMP ID
    private final static byte cmdTransmit = 0x03; // Transmit (FW v07+)
    private final static byte cmdFrequencyReport = 0x04; // Frequency report (reserved for future hardware)
    // 0x05 Setup sample timer (FW v07+)
    private final static byte cmdSetFrequency = 0x06; // Setup frequency modulation timer (FW v07+)
    private final static byte cmdLedMuteOn = 0x10; // LED mute on (FW v07+)
    private final static byte cmdLedMuteOff = 0x11; // LED mute off (FW v07+)
    private final static byte cmdLedOn = 0x12; // LED on (FW v07+)
    private final static byte cmdLedOff = 0x13; // LED off (FW v07+)
    // 0x23 Settings descriptor report (FW v20+)
    private final static byte cmdTransmitByteCountReport = 0x24; // Enable transmit byte count report (FW v20+)
    private final static byte cmdTransmitNotify = 0x25; // Enable transmit notify on complete (FW v20+)
    private final static byte cmdTransmitHandshake = 0x26; // Enable transmit handshake (FW v20+)
    private final static byte cmdIOwrite = 0x30; // Sets the IO pins to ground (0) or +5volt (1).
    private final static byte cmdIOdirection = 0x31; // Sets the IO pins to input (1) or output (0).
    private final static byte cmdIOread = 0x32; // Read the IO pins, returns 1 byte.
    private final static byte cmdUARTsetup = 0x40; // Setup the UART to send serial data. Uses the current virtual serial port settings.
    private final static byte cmdUARTclose = 0x41; // Close the UART.
    private final static byte cmdUARTwrite = 0x42; // Send a byte to the serial UART.

    // Source: http://dangerousprototypes.com/docs/USB_IR_Toy:_IRman_decoder_mode
    private final static byte cmdSamplingMode = (byte) 's';
    private final static byte cmdSelfTest = (byte) 't';
    private final static byte cmdVersion = (byte) 'v';
    private final static byte cmdBootloaderMode = (byte) '$';

    private final static byte endOfData = (byte) 0xff;
    private final static int transmitByteCountToken = (int) 't';
    private final static int transmitCompleteSuccess = (int) 'C';
    private final static int transmitCompleteFailure = (int) 'F';

    // Versions strings are exactly 4 chars in length, see http://dangerousprototypes.com/docs/USB_IR_Toy:_IRman_decoder_mode
    private final static int lengthVersionString = 4;
    private final static int lengthSelftestVersionString = 4;
    private final static int lengthProtocolVersionString = 3;
    private final static String expectedProtocolVersion = "S01";
    private final static int emptyBufferSize = 62;

    private final static int powerPin = 5;
    private final static int receivePin = 3;
    private final static int sendingPin = 4;

    private boolean stopCaptureRequest = true;
    private String protocolVersion;
    private String version;
    private int maxLearnLength = defaultMaxLearnLength;
    private int IOdirections = -1;
    private int IOdata = 0;

    public IrToy() throws NoSuchPortException, PortInUseException, UnsupportedCommOperationException, IOException {
        this(defaultPortName, defaultBaudRate, defaultFlowControl, defaultTimeout, defaultMaxLearnLength, false);
    }

    public IrToy(String portName) throws NoSuchPortException, PortInUseException, UnsupportedCommOperationException, IOException {
        this(portName, defaultBaudRate, defaultFlowControl, defaultTimeout, defaultMaxLearnLength, false);
    }

    public IrToy(String portName, int timeout, boolean verbose) throws NoSuchPortException, PortInUseException, UnsupportedCommOperationException, IOException {
        this(portName, defaultBaudRate, defaultFlowControl, timeout, defaultMaxLearnLength, verbose);
    }

    public IrToy(String portName, int baudRate, int timeout, boolean verbose) throws NoSuchPortException, PortInUseException, UnsupportedCommOperationException, IOException {
        this(portName, baudRate, defaultFlowControl, timeout, defaultMaxLearnLength, verbose);
    }

    public IrToy(String portName, int beginTimeout, int middleTimeout, int endingTimeout, boolean verbose) throws NoSuchPortException, PortInUseException, UnsupportedCommOperationException, IOException {
        this(portName, defaultBaudRate, defaultFlowControl, beginTimeout, middleTimeout, verbose);
    }

    public IrToy(String portName, int baud, int beginTimeout, int middleTimeout, int endingTimeout, boolean verbose) throws NoSuchPortException, PortInUseException, UnsupportedCommOperationException, IOException {
        this(portName, baud, defaultFlowControl, beginTimeout, middleTimeout, verbose);
    }

    public IrToy(String portName, int baudRate, LocalSerialPort.FlowControl flowControl, int timeout, int maxLearnLength, boolean verbose) throws NoSuchPortException, PortInUseException, UnsupportedCommOperationException, IOException {
        super(LocalSerialPortRaw.class, portName, baudRate, dataSize, stopBits, parity, flowControl, timeout, verbose);
        this.maxLearnLength = maxLearnLength;
    }

    private void goSamplingMode() throws IOException, HarcHardwareException {
        send(cmdSamplingMode);
        try {
            Thread.sleep(10);
        } catch (InterruptedException ex) {
        }
        protocolVersion = readString(lengthProtocolVersionString);
        if (!protocolVersion.equals(expectedProtocolVersion))
            throw new HarcHardwareException("Unsupported IrToy protocol version: " + protocolVersion);
    }

    private void setupSendingModes() throws IOException {
        if (transmitNotifyEnabled)
            send(cmdTransmitNotify);
        if (transmitHandshakeEnabled)
            send(cmdTransmitHandshake);
        if (transmitByteCountReportEnabled)
            send(cmdTransmitByteCountReport);
    }

    private byte[] prepare3(byte cmd, int data) {
        byte[] array = new byte[3];
        array[0] = cmd;
        array[1] = (byte) ((data >> 8) & 0xff);
        array[2] = (byte) (data & 0xff);
        return array;
    }

    private void setIOData() throws IOException {
        send(prepare3(cmdIOdirection, IOdirections));
        send(prepare3(cmdIOwrite, IOdata));
    }

    /**
     * pin 2: RA2
     * pin 3: RA3
     * pin 4: RA4
     * pin 5: RA5
     * pin 11: RB3
     * pin 13: RB5
     *
     * @param pin
     * @param state
     * @throws IOException
     */
    public void setPin(int pin, boolean state) throws IOException {
        int mask = 1 << pin;
        IOdirections &= ~mask;
        if (state)
            IOdata |= mask;
        else
            IOdata &= ~mask;
        setIOData();
    }

    @Override
    public void open() throws IOException, HarcHardwareException {
        super.open();
        reset(5);
        send(cmdVersion);
        version = readString(lengthVersionString);
        goSamplingMode();
        setupSendingModes();
        setPin(powerPin, true);
    }

    @Override
    public void close() throws IOException {
        if (isValid()) {
            IOdirections = -1;
            setIOData();
            reset(1);
        }
        super.close();
    }

    public void reset(int times) throws IOException {
        for (int i = 0; i < times; i++)
            send(cmdReset);
        try {
            Thread.sleep(10);
        } catch (InterruptedException ex) {
        }
        serialPort.flushInput();
    }

    private void send(byte[] buf) throws IOException {
        serialPort.sendBytes(buf);
        //serialPort.flush();
    }

    private void send(byte[] buf, int offset, int length) throws IOException {
        serialPort.sendBytes(buf, offset, length);
        //serialPort.flush();
    }

    private void send(byte b) throws IOException {
        serialPort.sendByte(b);
        //serialPort.flush();
    }

    private byte[] toByteArray(int[] data) {
        byte[] buf = new byte[2*data.length];
        for (int i = 0; i < data.length; i++) {
            int periods = (int)Math.round(((double)data[i])/period);
            buf[2*i] = (byte)(periods / 256);
            buf[2*i+1] = (byte) (periods % 256);
        }
        // REPLACE last gap by 0xFFFF
        buf[2*data.length-2] = endOfData;
        buf[2*data.length-1] = endOfData;
        return buf;
    }

    private int[] recv() throws IOException  {
        int[] result = null;
        try {
            ArrayList<Integer> array = new ArrayList<>();
            stopCaptureRequest = false;
            long maxLearnLengthMicroSeconds = maxLearnLength * 1000L;
            long sum = 0;
            setPin(receivePin, true);
            while (!stopCaptureRequest && sum <= maxLearnLengthMicroSeconds) { // if leaving here, reset is needed.
                int val = read2Bytes(); // throws TimeoutException
                int ms = (int) Math.round(val * period);
                array.add(ms);
                sum += ms;
                if (val == 0xffff)
                    // Only way for timeout, 1.4 seconds. Too long for most use cases ... :-\
                    break;
            }
            if (stopCaptureRequest) {
                setPin(receivePin, false);
                return null;
            }
            result = new int[array.size()];
            for (int i = 0; i < array.size(); i++) {
                result[i] = array.get(i);
            }
        } finally {
            setPin(receivePin, false);
        }
        return result;
    }

    private double getFrequency(int onTimes) throws IOException {
        send(cmdFrequencyReport);
        /*int t1 =*/ read2Bytes();
        /*int t2 =*/ read2Bytes();
        /*int t3 =*/ read2Bytes();
        int count = read2Bytes();
        //System.err.println(t1);System.err.println(t2);System.err.println(t3);System.err.println(count);System.err.println(onTimes);
        //return (2*PICClockFrequency)/((double)(t3 - t1));
        return ((double)count)/(((double) onTimes) * IrpUtils.microseconds2seconds) ;
    }

    @Override
    public ModulatedIrSequence capture() throws HarcHardwareException, IOException {
        // reset, while I do not want any already recorder signals.
        reset(5);
        goSamplingMode();
        int[] data = recv(); // throws TimeoutException
        if (stopCaptureRequest || data == null)
            return null;

        int sum = 0;
        for (int i = 0; i < data.length / 2; i++) {
            sum += data[2 * i];
        }
        double frequency = getFrequency(sum);
        ModulatedIrSequence seq = null;
        try {
            seq = new ModulatedIrSequence(data, frequency);
        } catch (IncompatibleArgumentException ex) {
            throw new HarcHardwareException("IrToy: Erroneous data received.");
        }
        return seq;
    }

    @Override
    public boolean stopCapture() {
        stopCaptureRequest = true;
        return true;
    }

    @Override
    public IrSequence receive() throws HarcHardwareException, IOException {
        return capture();
    }

    @Override
    public boolean stopReceive() {
        return stopCapture();
    }

    private boolean transmit(int[] data, double frequency) throws IOException, HarcHardwareException {
        if (frequency > 0)
            setFrequency(frequency);
        return transmit(data);
    }

    private boolean transmit(int[] data) throws IOException, HarcHardwareException {
        reset(1);
        goSamplingMode();
        setupSendingModes();
        setPin(sendingPin, true);
        byte[] buf = toByteArray(data);
        send(cmdTransmit);
        boolean succcess = true;

        try {
            if (transmitHandshakeEnabled) {
                int bytesSent = 0;
                while (bytesSent < buf.length) {
                    int noBytes = readByte(); // number of bytes free in buffer, the number we should send
                    if (noBytes != emptyBufferSize)
                        continue;
                    int toSend = Math.min(noBytes, buf.length - bytesSent);
                    send(buf, bytesSent, toSend);
                    bytesSent += toSend;
                }
            }
            int noBytes = readByte();
            if (noBytes != emptyBufferSize) {
                System.err.println("got " + noBytes + " should: " + emptyBufferSize);
                succcess = false;
            }

            if (succcess && transmitByteCountReportEnabled) {
                int token = readByte();
                if (token == transmitByteCountToken) { // 't'
                    int bytesSent = read2Bytes();
                    if (bytesSent != data.length * 2) {
                        System.err.println("sent " + bytesSent + " should: " + (data.length * 2));
                        succcess = false;
                    }
                } else {
                    System.err.println("did not get t but " + token);
                    succcess = false;
                }
            }

            if (succcess && transmitNotifyEnabled) {
                int token = readByte();
                if (token != transmitCompleteSuccess) {
                    System.err.println("Status: " + token);
                    succcess = false;
                }
            }
        } finally {
            setPin(sendingPin, false);
        }
        return succcess;
    }

    private int byte2unsignedInt(byte b) {
        return b >= 0 ? (int) b : b + 256;
    }

    private String readString(int length) throws IOException {
        byte[] buf = serialPort.readBytes(length);
        return new String(buf, 0, length, IrpUtils.dumbCharset);
    }

    private int readByte() throws IOException {
        byte[] a = serialPort.readBytes(1);
        return a.length > 0 ? byte2unsignedInt(a[0]) : -1;
    }

    private int read2Bytes() throws IOException {
        byte[] a = serialPort.readBytes(2);
        return a.length < 2 ? -1 : 256*byte2unsignedInt(a[0]) + byte2unsignedInt(a[1]);
    }

    public String selftest() throws IOException {
        reset(5);
        send(cmdSelfTest);
        String ver = readString(lengthSelftestVersionString);
        return ver;
    }

    public void bootloaderMode() throws IOException {
        reset(5);
        send(cmdBootloaderMode);
    }

    public String getProtocolVersion() {
        return protocolVersion;
    }

    public void setLedMute(boolean status) throws IOException {
        send(status ? cmdLedMuteOn : cmdLedMuteOff);
    }

    public void setLed(boolean status) throws IOException {
        send(status ? cmdLedOn : cmdLedOff);
    }

    private void setFrequency(double frequency) throws IOException {
        byte pr2 = (byte) Math.round(oscillatorFrequency/(16*frequency) - 1);
        byte[] buf = new byte[3];
        buf[0] = cmdSetFrequency;
        buf[1] = pr2;
        buf[2] = dutyCycle;
        send(buf);
    }

    @Override
    public String getVersion() {
        return version;
    }

    @Override
    public void setTimeout(int beginTimeout, int maxLearnLength, int endTimeout) throws IOException {
        setTimeout(beginTimeout);
        this.maxLearnLength = maxLearnLength;
    }

    @Override
    public boolean sendIr(IrSignal code, int count, Transmitter transmitter) throws IrpMasterException, IOException, HarcHardwareException {
        return transmit(code.toIntArrayCount(count), code.getFrequency());
    }

    public boolean sendCcf(String ccf, int count, Transmitter transmitter) throws IOException, IrpMasterException, HarcHardwareException {
        return sendIr(Pronto.ccfSignal(ccf), count, transmitter);
    }

    /**
     * Not supported due to hardware restrictions.
     *
     * @param ccf
     * @param transmitter
     * @return
     */
    public boolean sendCcfRepeat(String ccf, Transmitter transmitter) {
        throw new UnsupportedOperationException("Not supported due to hardware restrictions.");
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        String portName = "/dev/ttyACM0";
        IrToy toy = null;
        try {
            toy = new IrToy(portName);
            toy.open();
            if (args.length >= 1 && args[0].equals("-b"))
                toy.bootloaderMode();
            else {
                //int[] data = new int[]{889, 889, 1778, 889, 889, 889, 889, 889, 889, 889, 889, 889, 889, 889, 889, 889, 889, 889, 889, 889, 889, 889, 889, 889, 889, 90886};
                System.out.println(toy.getVersion());
                //String result = toy.selftest();
                //System.out.println(result);
                toy.setLed(true);
                toy.setLedMute(false);
                IrSignal signal = new IrSignal("../IrpMaster/data/IrpProtocols.ini", "nec1", "D=122 F=26");
                boolean success = toy.sendIr(signal, 10, null);
                //String success = toy.selftest();
                toy.setPin(powerPin, true);
                toy.setPin(receivePin, true);
                toy.setPin(sendingPin, true);

                System.out.println(success);
            }
        } catch (NoSuchPortException ex) {
            System.err.println("Port for IRToy " + portName + " was not found");
        } catch (PortInUseException ex) {
            System.err.println("Port for IRToy in use");
        } catch (HarcHardwareException | UnsupportedCommOperationException | IOException | IrpMasterException ex) {
            System.err.println(ex.getMessage());
        } finally {
            if (toy != null) {
                try {
                    toy.close();
                } catch (IOException ex) {
                }
            }
        }
    }
}
