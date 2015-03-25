/*-------------------------------------------------------------------------
|   RXTX License v 2.1 - LGPL v 2.1 + Linking Over Controlled Interface.
|   RXTX is a native interface to serial ports in java.
|   Copyright 1997-2007 by Trent Jarvi tjarvi@qbang.org and others who
|   actually wrote it.  See individual source files for more information.
|
|   A copy of the LGPL v 2.1 may be found at
|   http://www.gnu.org/licenses/lgpl.txt on March 4th 2007.  A copy is
|   here for your convenience.
|
|   This library is free software; you can redistribute it and/or
|   modify it under the terms of the GNU Lesser General Public
|   License as published by the Free Software Foundation; either
|   version 2.1 of the License, or (at your option) any later version.
|
|   This library is distributed in the hope that it will be useful,
|   but WITHOUT ANY WARRANTY; without even the implied warranty of
|   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
|   Lesser General Public License for more details.
|
|   An executable that contains no derivative of any portion of RXTX, but
|   is designed to work with RXTX by being dynamically linked with it,
|   is considered a "work that uses the Library" subject to the terms and
|   conditions of the GNU Lesser General Public License.
|
|   The following has been added to the RXTX License to remove
|   any confusion about linking to RXTX.   We want to allow in part what
|   section 5, paragraph 2 of the LGPL does not permit in the special
|   case of linking over a controlled interface.  The intent is to add a
|   Java Specification Request or standards body defined interface in the 
|   future as another exception but one is not currently available.
|
|   http://www.fsf.org/licenses/gpl-faq.html#LinkingOverControlledInterface
|
|   As a special exception, the copyright holders of RXTX give you
|   permission to link RXTX with independent modules that communicate with
|   RXTX solely through the Sun Microsytems CommAPI interface version 2,
|   regardless of the license terms of these independent modules, and to copy
|   and distribute the resulting combined work under terms of your choice,
|   provided that every copy of the combined work is accompanied by a complete
|   copy of the source code of RXTX (the version of RXTX used to produce the
|   combined work), being distributed under the terms of the GNU Lesser General
|   Public License plus this exception.  An independent module is a
|   module which is not derived from or based on RXTX.
|
|   Note that people who make modified versions of RXTX are not obligated
|   to grant this special exception for their modified versions; it is
|   their choice whether to do so.  The GNU Lesser General Public License
|   gives permission to release a modified version without this exception; this
|   exception also makes it possible to release a modified version which
|   carries forward this exception.
|
|   You should have received a copy of the GNU Lesser General Public
|   License along with this library; if not, write to the Free
|   Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
|   All trademarks belong to their respective owners.
--------------------------------------------------------------------------*/
/*
	
	test   :  TestMonitorThread
	Author :  Trent Jarvi
	added  :  Sat Oct 13 17:45:31 MDT 2001
	Problem:  Monitor Thread didnt go away in the past.  This makes sure
		  the thread does exit and is GC's.  You can watch the threads
		  in a native threads jvm with top.
	todo   :  There are still some issues with the way BlackBox open/closes
		  the ports and the removal of the eventListener.

*/
import gnu.io.*;
import java.util.*;


public class TestMonitorThread implements SerialPortEventListener
{

	public TestMonitorThread()
	{
		CommPortIdentifier cpi;
    		Enumeration ports;
		SerialPort port = null;
		Date d = new Date();
		long result, t1 = d.getTime(), t2 = d.getTime();

		ports = CommPortIdentifier.getPortIdentifiers();
		while ( ports.hasMoreElements() )
		{
			cpi = (CommPortIdentifier) ports.nextElement();
			if ( cpi.getPortType() == CommPortIdentifier.PORT_SERIAL )
			{
				if ( cpi.getName().equals( "/dev/ttyS0" ) )
				{
					try {
						port = (SerialPort) cpi.open("TestMonitorThread", 2000);
					} catch (PortInUseException e) {}
					break;
				}
			} 
		} 
		for( int i=0;i<30;i++ )
		{
			try {
				port.addEventListener(this);
			} catch (TooManyListenersException e ) {
				e.printStackTrace();
			}
			t2 =  new Date().getTime();
			port.removeEventListener();
			System.out.println( t2 - t1 );
			t1 = t2;
		}
		port.close();
	}
	public static void main( String[] args )
	{
		System.out.println(">my TestMonitorThread");
		TestMonitorThread thisTestMonitorThread = new TestMonitorThread();
		System.out.println("<my TestMonitorThread");
	}
	public void serialEvent(SerialPortEvent event)
	{
		switch (event.getEventType())
		{
			case SerialPortEvent.BI:
			case SerialPortEvent.OE:
			case SerialPortEvent.FE:
			case SerialPortEvent.PE:
			case SerialPortEvent.CD:
			case SerialPortEvent.CTS:
			case SerialPortEvent.DSR:
			case SerialPortEvent.RI:
			case SerialPortEvent.OUTPUT_BUFFER_EMPTY:
			case SerialPortEvent.DATA_AVAILABLE:
				System.out.println("Event");
				break;
		}
	}
}
