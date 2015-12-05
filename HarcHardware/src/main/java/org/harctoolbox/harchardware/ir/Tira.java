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

package org.harctoolbox.harchardware.ir;

import gnu.io.CommPort;
import gnu.io.CommPortIdentifier;
import gnu.io.NoSuchPortException;
import gnu.io.PortInUseException;
import gnu.io.SerialPort;
import gnu.io.UnsupportedCommOperationException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.harctoolbox.IrpMaster.IrSignal;
import org.harctoolbox.IrpMaster.IrpMasterException;
import org.harctoolbox.IrpMaster.IrpUtils;
import org.harctoolbox.IrpMaster.Pronto;
import org.harctoolbox.harchardware.IHarcHardware;

/**
 * This is not ready for deployment.
 */
/*public*/ class Tira implements IHarcHardware, IRawIrSender {

    private CommPort commPort;

    public static final String defaultPortName = "/dev/ttyUSB0";
    private static final int baudRate = 9600;//115200;//9600;
    private static final int flowControl = SerialPort.FLOWCONTROL_RTSCTS_IN | SerialPort.FLOWCONTROL_RTSCTS_OUT;// SerialPort.FLOWCONTROL_NONE;
    private static final int bufsize = 255;

    private static final double oscillatorFrequency = 48000000;
    private static final double period = 21.3333; // microseconds
    private static final byte dutyCycle = 0; // semantically: don't care

    private String portName;

    private final static boolean transmitNotifyEnabled = true;
    private final static boolean transmitByteCountReportEnabled = true;
    private final static boolean transmitHandshakeEnabled = true;

    private final static byte cmdReset = 0x00; // Reset (returns to remote decoder mode)
    // 0x01 RESERVED for SUMP RUN
    // 0x02 RESERVED for SUMP ID
    private final static byte cmdTransmit = 0x03; // Transmit (FW v07+)
    // 0x04 Frequency report (reserved for future hardware)
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
    // 0x30 IO write
    // 0x31 IO direction
    // 0x32 IO read
    // 0x40 UART setup
    // 0x41 UART close
    // 0x42 UART write

    private final static byte cmdSamplingMode = (byte) 's';
    private final static byte cmdSelfTest = (byte) 't';
    private final static byte cmdVersion = (byte) 'v';
    private final static byte cmdBootloaderMode = (byte) '$';
    private final static byte endOfData = (byte) 0xff;
    private final static int transmitByteCountToken = (int) 't';
    private final static int transmitCompleteSuccess = (int) 'C';
    private final static int transmitCompleteFailure = (int) 'F';

    private OutputStream out;
    private InputStream in;

    //private String protocolVersion;
    private String version;

    //private boolean verbosity;
    //private int debug;
    /* public */ Tira() throws NoSuchPortException, PortInUseException, UnsupportedCommOperationException, IOException, InterruptedException  {
        this(defaultPortName);
    }

    /*public*/ Tira(String portName) throws NoSuchPortException, PortInUseException, UnsupportedCommOperationException, IOException, InterruptedException {
        this.portName = portName;
        CommPortIdentifier portIdentifier = CommPortIdentifier.getPortIdentifier(portName);
        commPort = portIdentifier.open(this.getClass().getName(), 2000);

        if (commPort instanceof gnu.io.SerialPort) {
            SerialPort serialPort = (SerialPort) commPort;
            serialPort.setSerialPortParams(baudRate, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);

            serialPort.setFlowControlMode(flowControl);

            out = serialPort.getOutputStream();
            in = serialPort.getInputStream();

            //reset();
            send(new byte[] {(byte)'I', (byte)'V'});
            Thread.sleep(50);
            version = readString();
            //System.err.println(version);
            //version = readString();
            //System.err.println(version);
            //version = readString();
            //System.err.println(version);

        }
    }

    public final void reset() throws IOException, InterruptedException {
        send(new byte[]{cmdReset, cmdReset, cmdReset, cmdReset, cmdReset});
        Thread.sleep(100);
    }

    private void send(byte[] buf) throws IOException {
        out.write(buf);
        out.flush();
    }

    private void send(byte[] buf, int offset, int length) throws IOException {
        out.write(buf, offset, length);
        out.flush();
    }

    private void send(byte b) throws IOException {
        out.write(b);
        out.flush();
    }

    private byte[] toByteArray(int[] data) {
        byte[] buf = new byte[2*data.length + 2];
        for (int i = 0; i < data.length; i++) {
            int periods = (int)Math.round(((double)data[i])/period);
            buf[2*i] = (byte)(periods / 256);
            buf[2*i+1] = (byte) (periods % 256);
        }
        buf[2*data.length] = endOfData;
        buf[2*data.length+1] = endOfData;
        return buf;
    }

    private boolean transmit(double frequency, int[] data) throws IOException {
        if (frequency > 0)
            setFrequency(frequency);
        return transmit(data);
    }

    private boolean transmit(int[] data) throws IOException {
        send(cmdTransmit);
        byte[] buf = toByteArray(data);

        if (transmitHandshakeEnabled) {
            int bytesSent = 0;
            while (bytesSent < buf.length) {
                int noBytes = readByte();
                int toSend = Math.min(noBytes, buf.length - bytesSent);
                send(buf, bytesSent, toSend);
                bytesSent += toSend;
            }

        }
        readByte();

        if (transmitByteCountReportEnabled) {
            int token = readByte();
            if (token != transmitByteCountToken)
                return false;
            int msb = readByte();
            int lsb = readByte();
            int bytesSent = 256*msb + lsb;
            if (bytesSent != data.length*2+2)
                return false;
        }

        if (transmitNotifyEnabled) {
            int token = readByte();
            if (token != transmitCompleteSuccess)
                return false;
        }
        return true;
    }

    private String readString() throws IOException {
        byte[] buf = new byte[bufsize];
        int length = in.read(buf);
        return new String(buf, 0, length, IrpUtils.dumbCharset);
    }

    private int readByte() throws IOException {
        return in.read();
    }

    public String selftest() throws IOException, InterruptedException {
        reset();
        send(cmdSelfTest);
        byte[] buf = new byte[4];
        for (int i = 0; i < 4; i++)
            buf[i] = (byte) readByte();

        return new String(buf, IrpUtils.dumbCharset);
    }

    public void bootloaderMode() throws IOException, InterruptedException {
        reset();
        send(cmdBootloaderMode);
    }

    @Override
    public void close() {
        try {
            out.flush();
            out.close();
            in.close();
            Thread.sleep(1000);
            commPort.close();
        } catch (IOException | InterruptedException ex) {
            System.err.println(ex.getMessage());
        }
    }

    //public String getProtocolVersion() {
    //    return protocolVersion;
    //}

    public String getPortName() {
        return portName;
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

/*
    public void listen() throws IOException, UnsupportedCommOperationException {
        int toRead = 7000;
        byte[] data = new byte[toRead];

        RXTXPort serial = (RXTXPort) commPort;
        InputStream in = serial.getInputStream();
        serial.setDTR(true);
        //serial.setRTS(true);
        serial.disableReceiveThreshold();
        //serial.disableReceiveFraming();
        serial.enableReceiveTimeout(5000);
        //serial.setLowLatency();
        serial.disableReceiveFraming();
        System.err.println(serial.getDivisor());

        int bytesRead = 0;
        boolean status = serial.clearCommInput();
        System.err.println(status);
        while (bytesRead < toRead) {

           int x = in.read(data, bytesRead, toRead - bytesRead);
           bytesRead += x;
           //int avail = serial.getInputStream().available();
           System.out.println(x + "\t" + bytesRead + "\t" + data[bytesRead-1]);
        }
    }*/

    @Override
    public String getVersion() {
        return version;
    }

    @Override
    public void setVerbosity(boolean verbosity) {
    }

    @Override
    public void setDebug(int debug) {
    }

    @Override
    public void setTimeout(int timeout) {
        // ???
    }

    @Override
    public boolean isValid() {
        return commPort != null;
    }

    @Override
    public boolean sendIr(IrSignal code, int count, Transmitter transmitter) throws IrpMasterException, IOException {
        return transmit(code.getFrequency(), code.toIntArray(count-1));
    }

    public boolean sendCcf(String ccf, int count, Transmitter transmitter) throws IOException, IrpMasterException {
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
     * Not supported due to hardware restrictions.
     * @param transmitter
     * @return
     */
    //@Override
    //public boolean stopIr(Transmitter transmitter) {
    //    throw new UnsupportedOperationException("Not supported due to hardware restrictions.");
    //}

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        try (Tira tira = new Tira()) {
            System.out.println(tira.getVersion());
            //String result = toy.selftest();
            //System.out.println(result);
            //toy.setLed(true);
            //toy.setLedMute(false);
            //boolean success = toy.transmit(36000, data);
            //IrSignal signal = new IrSignal("../IrpMaster/data/IrpProtocols.ini", "nec1", "D=122 F=26");
            //boolean success = toy.sendIr(signal, 10, null);
            //System.out.println(success);
            //w.listen();
        } catch (NoSuchPortException ex) {
            System.err.println("Port for IRToy " + defaultPortName + " was not found");
        } catch (PortInUseException ex) {
            System.err.println("Port for IRToy in use");
        } catch (UnsupportedCommOperationException | IOException | InterruptedException ex) {
            System.err.println("xxx" + ex.getMessage());
            //ex.printStackTrace();
        }
    }

    @Override
    public Transmitter getTransmitter() {
        return null;
    }

    @Override
    public void open() {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
