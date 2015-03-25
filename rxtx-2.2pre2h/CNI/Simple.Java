import gnu.io.*;
import java.util.*;
import java.io.InputStream;
import java.io.OutputStream;


public class Simple implements SerialPortEventListener
{
	CommPortIdentifier cpi;
    	Enumeration ports;
	SerialPort port = null;
	InputStream input;
	OutputStream output;

	public static void main( String args[] )
	{
		boolean done = false;
		Simple reader = new Simple( );
		while ( !done )
		{
			try {
				Thread.sleep(100);
			} catch (Exception e) {}
		}
	}

	public Simple( )
	{

		System.out.println("Getting PortIdentifiers");
		ports = CommPortIdentifier.getPortIdentifiers();
		while ( ports.hasMoreElements() )
		{
			cpi = (CommPortIdentifier) ports.nextElement();
			if (cpi.getPortType() == CommPortIdentifier.PORT_SERIAL)
			{
				if ( cpi.getName().equals( "/dev/ttyS1" ) )
				{
					try {
						port = (SerialPort) cpi.open("Simple", 2000);
						port.addEventListener(this);
						port.notifyOnDataAvailable(true);
						output=port.getOutputStream();
						input=port.getInputStream();
						System.out.println("writing output");
						output.write( new String("123456789\0").getBytes() );
					} catch (Exception e)
					{
						e.printStackTrace();
					}
				}
			} 
		} 
		return;
	}
        public void serialEvent(SerialPortEvent event) {
                switch(event.getEventType())
		{
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
				byte in[] = new byte[800];
				int ret = 0;


				System.out.println("Got Data Available");	
				try {
					ret = input.read( in, 0, 63 );
				} catch (Exception e) {
					System.out.println("Input Exception");				
				}
				System.out.println("Printing read() results");	
				if ( ret > 0 )
				{
					try {
						System.out.write( in, 0, ret );
						System.out.println();
					} catch ( Exception e )
					{
						e.printStackTrace();
					}
				}
				System.exit( 0 );
				break;
                }
        }
}
