/*
* This class is one end of a test pair of programs
* SimpleRead.class is the other end.
* To run the test requires two com ports
* and a null modem cable.
* Start SimpleSnuV1 and then run SimpleRead
* on the other machine/com port.
* This code is supplied for demonstration purposes only.
* You are free to use it as you please.  
*/


import java.io.*;
import java.util.*;
import gnu.io.*;

public class SimpleSnuV1 implements Runnable, SerialPortEventListener {

static CommPortIdentifier portId;
static Enumeration portList;

InputStream inputStream;
OutputStream outputStream;
SerialPort serialPort;
Thread readThread;

int numBytes = 0;
String num = "021891383";
String rst = "atz";
String dial ="atd";
String outData = "";
String outDataBuff = "";
boolean running = true;
boolean process = true;
boolean waitForInput = true;

	public static void main(String[] args) {
		if (args.length < 1) {
			System.out.print("SimpleSnuV1.class /dev/ttyxx\n");
			System.exit(-1);
		}
        		portList = CommPortIdentifier.getPortIdentifiers();
		while (portList.hasMoreElements()) {
            		portId = (CommPortIdentifier) portList.nextElement();
           			if (portId.getPortType() == CommPortIdentifier.PORT_SERIAL) {
                			if (portId.getName().equals(args[0])) {
                    				SimpleSnuV1 reader = new SimpleSnuV1();
                			} 
            		}
		}
	}

	public SimpleSnuV1() {
		try {
			serialPort = (SerialPort) portId.open("SimpleSnu", 2000);
		} catch (PortInUseException e) {}
		try {
			inputStream = serialPort.getInputStream();
			outputStream = serialPort.getOutputStream();
		} catch (IOException e) {}
		try {
serialPort.setSerialPortParams(19200, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);
		} catch (UnsupportedCommOperationException e) {}
		byte[] readBuffer = new byte[20];
		try {
			serialPort.addEventListener(this);
		} catch (TooManyListenersException e) {
			System.out.print("To Many Event Listeners\n");
			System.exit(-1);
		}
		serialPort.notifyOnDataAvailable(true);
		readThread = new Thread(this);
		readThread.start();
	}

	public void run() {
		int resetCount = 0;
		int numBytes = 0;
		byte[] readBuffer = new byte[20];
		String sReadBuff = "";
		boolean connected = false;
		while (running) {
			if (!connected) {
				try {
					while (inputStream.available() > 0) {
						numBytes = inputStream.read(readBuffer);
						String tmpR = new String(readBuffer);
						sReadBuff += tmpR.substring(0, numBytes); 
					}
				} catch (IOException e) {
					System.exit(1);
				}
				if (!sReadBuff.equals("")) {
					System.out.print(sReadBuff + "\n");
				} else {
					
				}
				int pos = 0;
				if ((pos = sReadBuff.indexOf("atz")) != -1) {
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {}
					try {
						outputStream.write(new String("OK").getBytes());
						outputStream.write((byte)0x0D);
						outputStream.write((byte)0x0A);
						System.out.print("OK Sent\n");		
					} catch (IOException e) {
						System.exit(1);					
					}
					sReadBuff = "";
				} else if ((pos = sReadBuff.indexOf("atd")) != -1) {
					sReadBuff = "";
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {}
					try {
						outputStream.write(new String("CONNECT 9600").getBytes());
						outputStream.write((byte)0x0D);
						outputStream.write((byte)0x0A);
//						connected = true;
					} catch (IOException e) {
						System.exit(1);
					}
					try {
						Thread.sleep(2000);
					} catch (InterruptedException e) {}
				}
			} else {

			}
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {}
		}
					
		System.out.print("Normal Exit...\n");
    	}
	public void serialEvent(SerialPortEvent event) {
//		try {
//			Thread.sleep(500);
//		} catch (InterruptedException e) {}
		switch(event.getEventType()) {
			case SerialPortEvent.BI:
				System.out.print("BI\n");				
				break;
			case SerialPortEvent.OE:
				System.out.print("OE\n");
				break;
			case SerialPortEvent.FE:
				System.out.print("FE\n");
				break;
			case SerialPortEvent.PE:
				System.out.print("PE\n");
				break;
			case SerialPortEvent.CD:
				System.out.print("CD\n");
				break;
			case SerialPortEvent.CTS:
				System.out.print("CTS\n");
				break;
			case SerialPortEvent.DSR:
				System.out.print("DSR\n");
				break;
			case SerialPortEvent.RI:
				System.out.print("RI\n");
				break;
			case SerialPortEvent.OUTPUT_BUFFER_EMPTY:
				System.out.print("Out Buff Empty\n");
				break;
			case SerialPortEvent.DATA_AVAILABLE:
//				waitForInput = false;
				System.out.print("Data Available\n");
            			break;
        		}
    	}
}

