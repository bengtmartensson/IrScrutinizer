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
package gnu.io;

import java.io.*;
import java.util.*;


/**
* @author Trent Jarvi
* @version %I%, %G%
* @since JDK1.0
*/

public  abstract class ParallelPort extends CommPort
{
	public static final int  LPT_MODE_ANY   =0;
	public static final int  LPT_MODE_SPP   =1;
	public static final int  LPT_MODE_PS2   =2;
	public static final int  LPT_MODE_EPP   =3;
	public static final int  LPT_MODE_ECP   =4;
	public static final int  LPT_MODE_NIBBLE=5;

	public abstract int getMode();
	public abstract int setMode(int mode)
		throws UnsupportedCommOperationException;
	public abstract void restart();
	public abstract void suspend();
	public abstract boolean isPaperOut();
	public abstract boolean isPrinterBusy();
	public abstract boolean isPrinterError();
	public abstract boolean isPrinterSelected();
	public abstract boolean isPrinterTimedOut();
	public abstract int getOutputBufferFree();
	public abstract void addEventListener( ParallelPortEventListener lsnr )
		throws TooManyListenersException;
	public abstract void removeEventListener();
	public abstract void notifyOnError( boolean enable );
	public abstract void notifyOnBuffer( boolean enable );
/*
	public int  PAR_EV_ERROR    1
	public int  PAR_EV_BUFFER   2
	public ParallelPort(){}
	private native static void Initialize();
	public LPRPort( String name ) throws IOException;
	private native int open( String name ) throws IOException;
	private int fd;
	private final ParallelOutputStream out = new ParallelOutputStream();
	public OutputStream getOutputStream();
	private final ParallelInputStream in = new ParallelInputStream();
	public InputStream getInputStream();
	private int lprmode=LPT_MODE_ANY;
	public native boolean setLPRMode(int mode)
		throws UnsupportedCommOperationException;
        private int speed;
        public int getBaudRate();
        private int dataBits;
        public int getDataBits();
	private int stopBits;
	public int getStopBits();
	private int parity;
	public int getParity();
	private native void nativeClose();
	public void close();
	public void enableReceiveFraming( int f )
		throws UnsupportedCommOperationException;
	public void disableReceiveFraming() {}
	public boolean isReceiveFramingEnabled();
	public int getReceiveFramingByte();
	private int timeout = 0;
	public void enableReceiveTimeout( int t );
	public void disableReceiveTimeout();
	public boolean isReceiveTimeoutEnabled();
	public int getReceiveTimeout();
	private int threshold = 1;
	public void enableReceiveThreshold( int t );
	public void disableReceiveThreshold();
	public int getReceiveThreshold();
	public boolean isReceiveThresholdEnabled();
	public native void setInputBufferSize( int size );
	public native int getInputBufferSize();
	public native void setOutputBufferSize( int size );
	public Abstract int getOutputBufferSize();
	private native void writeByte( int b ) throws IOException;
	private native void writeArray( byte b[], int off, int len )
		throws IOException;
	private native void drain() throws IOException;
	private native int nativeavailable() throws IOException;
	private native int readByte() throws IOException;
	private native int readArray( byte b[], int off, int len )
		throws IOException;
	private ParallelPortEventListener PPEventListener;
	private MonitorThread monThread;
	native void eventLoop();
	void sendEvent( int event, boolean state );
	protected void finalize();
*/
}
