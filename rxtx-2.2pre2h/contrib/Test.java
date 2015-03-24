/*
* Test.class is just that.. a Test
*/


import java.io.*;
import java.util.*;
import gnu.io.*;

public class Test implements SerialPortEventListener {

InputStream inputStream;
OutputStream outputStream;
SerialPort serialPort;
Thread readThread;

	public static void main(String[] args) {
		if (args.length < 1) {
			System.out.print("Test.class /dev/ports/serialx\n");
			System.exit(-1);
		}
		System.out.println("opening the Port: " + args[0]);		
		Test reader = new Test(args[0]);
	}

	public Test(String PortName) {
		RXTXCommDriver TxPort = new RXTXCommDriver();
		System.out.print("open Ports\n");		
		serialPort = (SerialPort) TxPort.getCommPort(PortName, CommPortIdentifier.PORT_SERIAL);
		System.out.print("Get Streams\n");		
		try {
			inputStream = serialPort.getInputStream();
			outputStream = serialPort.getOutputStream();
		} catch (IOException e) {
			e.printStackTrace();
		}
		try {
			serialPort.addEventListener(this);
		} catch (TooManyListenersException e) {
			e.printStackTrace();
		}
		try {
			System.out.println("Baud is " + serialPort.getBaudRate());	
			System.out.println("Bits is " + serialPort.getDataBits());	
			System.out.println("Stop is " + serialPort.getStopBits());	
			System.out.println("Par is " + serialPort.getParity());	
			System.out.print("Set Params\n");		
			serialPort.setSerialPortParams(19200, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);
			System.out.println("Baud is " + serialPort.getBaudRate());	
			System.out.println("Bits is " + serialPort.getDataBits());	
			System.out.println("Stop is " + serialPort.getStopBits());	
			System.out.println("Par is " + serialPort.getParity());	
			System.out.print("Set Params\n");		
			serialPort.setSerialPortParams(9600, SerialPort.DATABITS_7, SerialPort.STOPBITS_2, SerialPort.PARITY_ODD);
			System.out.println("Baud is " + serialPort.getBaudRate());	
			System.out.println("Bits is " + serialPort.getDataBits());	
			System.out.println("Stop is " + serialPort.getStopBits());	
			System.out.println("Par is " + serialPort.getParity());	
		} catch (UnsupportedCommOperationException e) {
			e.printStackTrace();
		}
		System.out.print("Sending 0x01\n");		
		try {
			outputStream.write((byte)0x01);
			System.out.print("0x01 Sent\n");		
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
        public void serialEvent(SerialPortEvent event) {
                switch(event.getEventType()) {
                        case SerialPortEvent.BI:
                                System.out.print("BI\n");
                        case SerialPortEvent.OE:
                                System.out.print("OE\n");
                        case SerialPortEvent.FE:
                                System.out.print("FE\n");
                        case SerialPortEvent.PE:
                                System.out.print("PE\n");
                        case SerialPortEvent.CD:
                                System.out.print("CD\n");
                        case SerialPortEvent.CTS:
                                System.out.print("CTS\n");
                        case SerialPortEvent.DSR:
                                System.out.print("DSR\n");
                        case SerialPortEvent.RI:
                                System.out.print("RI\n");
                        case SerialPortEvent.OUTPUT_BUFFER_EMPTY:
                                System.out.print("Out Buff Empty\n");
                                break;
                        case SerialPortEvent.DATA_AVAILABLE:
                                System.out.print("Data Available\n");
                                break;
                }
        }

}

