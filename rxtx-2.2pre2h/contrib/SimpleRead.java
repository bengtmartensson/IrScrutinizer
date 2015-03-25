/*
* This class is one end of a test pair of programs
* SimpleSnuV1.class is the other end.
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

public class SimpleRead implements Runnable, SerialPortEventListener {

static CommPortIdentifier portId;
static Enumeration portList;
SerialPort serialPort = null;
InputStream inputStream;
OutputStream outputStream;

Thread readThread;
String aS = "";
int numBytes = 0;
String num = "021891383";
String rst = "atz";
String dial ="atd";
String outData = "";
String outDataBuff = "";
boolean running = true;
boolean process = true;
boolean waitForInput = true;
boolean fatalErr = false;
String errMessage = "";

	public static void main(String[] args) {
		if (args.length < 1) {
			System.out.print("SimpleRead.class /dev/ttyxx\n");
			System.exit(-1);
		}
        		portList = CommPortIdentifier.getPortIdentifiers();
		while (portList.hasMoreElements()) {
            		portId = (CommPortIdentifier) portList.nextElement();
           			if (portId.getPortType() == CommPortIdentifier.PORT_SERIAL) {
                		// if (portId.getName().equals("COM1")) {
                			if (portId.getName().equals(args[0])) {
                    				SimpleRead reader = new SimpleRead();
                			}
            		}
		}
	}

	public SimpleRead() {
		try {
			serialPort = (SerialPort) portId.open("SimpleReadApp", 2000);
		} catch (PortInUseException e) {}
		try {
			inputStream = serialPort.getInputStream();
			outputStream = serialPort.getOutputStream();
		} catch (IOException e) {}
		try {
			serialPort.addEventListener(this);
		} catch (TooManyListenersException e) {}
		serialPort.notifyOnDataAvailable(true);
		try {
serialPort.setSerialPortParams(19200, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);
		} catch (UnsupportedCommOperationException e) {}
		readThread = new Thread(this);
		readThread.start();
	}

	public void run() {
		int resetCount = 0;
		while (running) {
			if (fatalErr) {
				System.out.print("Fatal Error...\n");
				System.exit(1);
			}
			if (!process) {
				try {
	            			Thread.sleep(500);
        				} catch (InterruptedException e) {}
				continue;
			}
			if (num.equals("Nil")) {
				try {
	            			Thread.sleep(500);
        				} catch (InterruptedException e) {}
				continue;
			}
			if (reset()) {
				if (dial()) {
					num = "Nil";
					System.out.print("Yahoooo OK...\n");
					//Here goes a class to handle any coms it gets passed the stuff it needs...
//					SNHdlcMessage myMess = new SNHdlcMessage();
//					String rply = myMess.processInput("loc1", "text", inputStream, outputStream);
//					System.out.print(rply + "\n");
					process = false;
					running = false;
					System.out.print("Normal Exit...\n");
					System.exit(1);
				} else {
					System.out.print("Dial Error...\n");
					process = false;
					running = false;
					System.exit(1);
				}
			} else {
				System.out.print("Reset Error...\n");
				resetCount ++;
				if (resetCount > 2 ) {
					System.out.print("Reset To Many Times Error ...\n");
					System.exit(1);	
				}
			}
			try {
            			Thread.sleep(100);
        			} catch (InterruptedException e) {}
		}
    	}
	public void closePort(SerialPort serialPort) {
		if (serialPort != null) {
			serialPort.notifyOnDataAvailable(false);
			serialPort.removeEventListener();
			if (inputStream != null) {
				try {
					inputStream.close();
					inputStream = null;
				}
				catch (IOException e) {}
			}
			if (outputStream != null) {
				try {
					outputStream.close();
					outputStream = null;
				}
				catch (IOException e) {}
			}
			serialPort.close();
			serialPort = null;
		}
	}
	public boolean reset() {
		try {
			outputStream.write(new String("atz").getBytes());
			outputStream.write((byte)0x0D);
			System.out.print("--> atz\n");
		} catch (IOException e) {
			System.out.print("Reset Output IO Exception");				
			return false;
		}
		int waitingCount = 0;
		waitForInput = true;
		while (waitForInput) {
			try {
	           			Thread.sleep(100);
			} catch (InterruptedException e) {}
			waitingCount ++;
			//2 seconds for it to reset
			if (waitingCount > 20) {
				return false;
			}
		}
		int numBytesTotal = 0;
		byte[] readBuffer = new byte[20];
		String sReadBuff = "";
		boolean dLoop = true;
		while (dLoop) {
			try {
				while (inputStream.available() > 0) {
					numBytes = inputStream.read(readBuffer);
					numBytesTotal += numBytes;
					String tmpR = new String(readBuffer);
					sReadBuff += tmpR.substring(0, numBytes);
				}
				if (sReadBuff.indexOf("NO CARRIER") != -1) {
					dLoop = false;
				} else if (sReadBuff.indexOf("BUSY") != -1) {
					dLoop = false;
				} else if (sReadBuff.indexOf("NO DIALTONE") != -1) {
					dLoop = false;
				} else if (sReadBuff.indexOf("OK") != -1) {
					dLoop = false;
					System.out.print("<-- " + new String(sReadBuff));
					return true;
				} else if (sReadBuff.indexOf("CONNECT") != -1) {
					dLoop = false;
				}
			} catch (IOException e) {
				System.out.print("Reset Input IO Exception");				
				return false;
			}
		}
		System.out.print("<-- " + new String(sReadBuff));
		return false;
	}
	public boolean dial() {
		try {
			outputStream.write(new String("atd").getBytes());
			outputStream.write(num.getBytes());
			outputStream.write((byte)0x0D);
			System.out.print("--> atdxxxxx\n");
		} catch (IOException e) {
			System.out.print("Dial Output IO Exception");				
			return false;
		}
		int waitingCount = 0;
		waitForInput = true;
		while (waitForInput) {
			try {
	           			Thread.sleep(100);
			} catch (InterruptedException e) {}
			waitingCount ++;
			if (waitingCount > 1000) {
				return false;
			}
		}
		int numBytesTotal = 0;
		byte[] readBuffer = new byte[20];
		String sReadBuff = "";
		boolean dLoop = true;
		while (dLoop) {
			try {
				while (inputStream.available() > 0) {
					numBytes = inputStream.read(readBuffer);
					numBytesTotal += numBytes;
					String tmpR = new String(readBuffer);
					sReadBuff += tmpR.substring(0, numBytes); 
				}
				if (sReadBuff.indexOf("NO CARRIER") != -1) {
					dLoop = false;
				} else if (sReadBuff.indexOf("BUSY") != -1) {
					dLoop = false;
				} else if (sReadBuff.indexOf("NO DIALTONE") != -1) {
					dLoop = false;
				} else if (sReadBuff.indexOf("OK") != -1) {
					dLoop = false;
				} else if (sReadBuff.indexOf("CONNECT") != -1) {
					System.out.print("<-- " + new String(sReadBuff));
					dLoop = false;
					return true;
				}
			} catch (IOException e) {
				System.out.print("Dial Input IO Exception");				
				return false;
			}
		}
		System.out.print("<-- " + new String(sReadBuff));
		return false;
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
				waitForInput = false;
//				System.out.print("Data Available\n");
            			break;
        		}
    	}
}
