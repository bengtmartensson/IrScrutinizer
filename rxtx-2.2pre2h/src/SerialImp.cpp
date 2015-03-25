/*-------------------------------------------------------------------------
|   RXTX License v 2.1 - LGPL v 2.1 + Linking Over Controlled Interface.
|   RXTX is a native interface to serial ports in java.
|   Copyright 1997-2009 by Trent Jarvi tjarvi@qbang.org and others who
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
#include "config.h"
#include "gnu_io_RXTXPort.h"
#include <time.h>
#include <unistd.h>
#include <stdio.h>
//#include <OS.h> // !!!
//#include <iostream.h> // !!!
#include <SerialPort.h> // !!! 
//#include <Joystick.h>
//#include <Application.h> // !!!
#include <string.h>
#include <stdlib.h>
#include <errno.h>
#include <limits.h>
#ifndef WIN32
#include <sys/ioctl.h>
#include <sys/param.h>
#include <sys/utsname.h>
#ifdef HAVE_TERMIOS_H
#	include <termios.h>
#endif
#ifdef HAVE_SYS_SIGNAL_H
#   include <sys/signal.h>
#endif
#else
#	include <win32termios.h>
#endif
#include <sys/types.h>
#include <sys/stat.h>
#include <sys/time.h>
#ifdef HAVE_SYS_FCNTL_H
#   include <sys/fcntl.h>
#endif
#ifdef HAVE_SYS_FILE_H
#   include <sys/file.h>
#endif

extern int errno;
#include "SerialImp.h"
/* #define DEBUG */

/* this is so diff will not generate noise when merging 1.4 and 1.5 changes
 * It will eventually be removed.
 * */
#define RXTXPort(foo) Java_gnu_io_RXTXPort_ ## foo
#define RXTXCommDriver(foo) Java_gnu_io_RXTXCommDriver_ ## foo
//#define MAX_PORT_NUM 65;
BSerialPort *PortArray[65]; // !!!
BSerialPort *port; // !!!
FILE                    *fp; // !!!
/*----------------------------------------------------------
RXTXPort.Initialize

   accept:      none
   perform:     Initialize the native library
   return:      none
   exceptions:  none
----------------------------------------------------------*/
JNIEXPORT void JNICALL RXTXPort(Initialize)(
	JNIEnv *env,
	jclass jclazz
	)
{
   ////////////////// Open Log File /////////////////////
   fp = fopen("RXTXOut.log", "a");
   fprintf(fp, "Brian is inside RXTXPort.Initialize\n"); // !!!
   fclose(fp);
   ////////////////// Close Log File ////////////////////

#ifndef WIN32
#ifndef __BEOS__
#if defined(DEBUG) && defined (UTS_RELEASE)
	struct utsname name;
#endif
	/* This bit of code checks to see if there is a signal handler installed
	   for SIGIO, and installs SIG_IGN if there is not.  This is necessary
	   for the native threads jdk, but we don't want to do it with green
	   threads, because it slows things down.  Go figure. */

	/* POSIX signal handling functions */
#if !defined(__FreeBSD___)
	struct sigaction handler;
	sigaction( SIGIO, NULL, &handler );
	if( !handler.sa_handler ) signal( SIGIO, SIG_IGN );
#endif /* !__FreeBSD__ */
#if defined(DEBUG) && defined (UTS_RELEASE)
	/* Lets let people who upgraded kernels know they may have problems */
	if (uname (&name) == -1)
	{
		report("RXTX WARNING:  cannot get system name\n");
		return;
	}
	if(!strcmp(name.release,UTS_RELEASE))
	{
		fprintf(stderr, LINUX_KERNEL_VERSION_ERROR, UTS_RELEASE,
			name.release);
		getchar();
	}
#endif /* DEBUG */
#endif /* __BEOS__ */
#endif /* WIN32 */
}


/*----------------------------------------------------------
RXTXPort.open

   accept:      The device to open.  ie "/dev/ttyS0"
   perform:     open the device, set the termios struct to sane settings and
                return the filedescriptor
   return:      fd
   exceptions:  IOException
   comments:    Very often people complain about not being able to get past
                this function and it turns out to be permissions on the
                device file or bios has the device disabled.
----------------------------------------------------------*/
JNIEXPORT jint JNICALL RXTXPort(open)(
	JNIEnv *env,
	jobject jobj,
	jstring jstr
	)
{
    int i = 1;
	int fd = -1;
	for (i = 1; i < 65; i++) // we start at 1 because fd = 0 is invalid to RXTXPort.java
	{
	   if (!PortArray[i]) // Here, we are finding the first non-null index into PortArray.
	   {
	      fd = i;  // fd is no longer a "file descriptor" but an index
	               // into an array of pointers to BSerialPort Objects.
	      break;  // got a good one.
	   }
	}
	if (fd < 1) // if no good fd found, throw exception.
	{
	   throw_java_exception( env, PORT_IN_USE_EXCEPTION, "open",
		   strerror( errno ) );
	   return -1;
	}
	
	// retrieve the serial port filename, strip off extra goo to leave just serial1, serial2, etc.
	const char *filename = env->GetStringUTFChars(jstr, 0); // retrieve filename from JAVAland.
	char devName[B_OS_NAME_LENGTH];
	const char *newDevName = strstr(filename, "serial");
	strncpy(&devName[0], &newDevName[0], (strlen(newDevName) + 1));
	
	// Instantiate the BSerialPort associated with devName (serial1, serial2, etc).
	PortArray[fd] = new BSerialPort();
	// Actually open serial port here:
	//do
	//{
	   status_t results = PortArray[fd]->Open(&devName[0]);
	//} while (errno==EINTR);
	if (results < 1) // throw exception if can't open port.
	{
	   throw_java_exception( env, PORT_IN_USE_EXCEPTION, "open",
		   strerror( errno ) );
	   return -1;
    }
	
    // Set initial port parameters
    PortArray[fd]->SetFlowControl(0);
    PortArray[fd]->SetBlocking(false);
    if (PortArray[fd]->SetDataRate(B_9600_BPS) != B_OK)
    {
	   throw_java_exception( env, PORT_IN_USE_EXCEPTION, "open",
		   strerror( errno ) );
	   return -1;
    } 

	env->ReleaseStringUTFChars(jstr, NULL); // releasing filename for JAVAland.
	
	
    // Open Debug Error log file
    fp = fopen("RXTXOut.log", "a");
    fprintf(fp, "inside RXTXPort.Open fd for BSerialPort is %i.\n",fd);
    fprintf(fp, "inside RXTXPort.Open devName for BSerialPort is %s.\n", devName);
    fprintf(fp, "inside RXTXPort.Open results of open is %i.\n", (int)results);
    fclose(fp);
    // Close Log File 
	
	return (jint)fd; // returning the "fd" (the index into array of BSerialPort pointers).
}

/*----------------------------------------------------------
RXTXPort.nativeClose

   accept:      none
   perform:     get the fd from the java end and close it
   return:      none
   exceptions:  none
----------------------------------------------------------*/
JNIEXPORT void JNICALL RXTXPort(nativeClose)( JNIEnv *env,
	jobject jobj )
{
   int fd = get_java_var( env, jobj,"fd","I" );

   ////////////////// Open Log File /////////////////////
   fp = fopen("RXTXOut.log", "a");
   fprintf(fp, "Brian is inside nativeClose\n"); // !!!
   fclose(fp);
   ////////////////// Close Log File ////////////////////

   delete PortArray[fd];
   return;
}

/*----------------------------------------------------------
 RXTXPort.nativeSetSerialPortParams

   accept:     speed, data bits, stop bits, parity
   perform:    set the serial port parameters
   return:     void
   exceptions: UnsupportedCommOperationException
----------------------------------------------------------*/
JNIEXPORT void JNICALL RXTXPort(nativeSetSerialPortParams)(
	JNIEnv *env, jobject jobj, jint speed, jint dataBits, jint stopBits,
	jint parity )
{
   int fd = get_java_var( env, jobj,"fd","I" );
   data_rate cspeed = translate_speed( env, speed );
   data_bits numofdatabits;
   stop_bits numofstopbits;
   parity_mode whichparitymode;
	
   if( !cspeed ) return;
   if( !translate_data_bits( env, &(numofdatabits), dataBits ) ) return;
   if( !translate_stop_bits( env, &(numofstopbits), stopBits ) ) return;
   if( !translate_parity( env, &(whichparitymode), parity ) ) return;
	
   PortArray[fd]->SetDataBits(numofdatabits);
   PortArray[fd]->SetStopBits(numofstopbits);
   PortArray[fd]->SetParityMode(whichparitymode);
   if (PortArray[fd]->SetDataRate(cspeed) != B_OK)
   {
      throw_java_exception( env, UNSUPPORTED_COMM_OPERATION,
	      "nativeSetSerialPortParams", strerror( errno ) );
   } 

   ////////////////// Open Log File /////////////////////
   fp = fopen("RXTXOut.log", "a");
   fprintf(fp, "Brian is inside RXTXPort.nativeSetSerialPortParams\n");
   fprintf(fp, "speed = %i\n", speed);
   fprintf(fp, "fd = %i\n", fd);
   fprintf(fp, "RXTXPort:nativeSetSerialPortParams right before return\n");
   fclose(fp);
   ////////////////// Close Log File /////////////////////
   
   return;
}

/*----------------------------------------------------------
 translate_speed

   accept:     speed in bits-per-second
   perform:    convert bits-per-second to a speed_t constant
   return:     speed_t constant
   exceptions: UnsupportedCommOperationException
   comments:   Only the lowest level code should know about
               the magic constants.
----------------------------------------------------------*/
data_rate translate_speed( JNIEnv *env, jint speed )
{

   ////////////////// Open Log File /////////////////////
   fp = fopen("RXTXOut.log", "a");
   fprintf(fp, "Brian is inside translate_speed\n"); 
   fclose(fp);
   ////////////////// Close Log File ////////////////////


	switch( speed ) {
		case 0:			return B_0_BPS;
		case 50:		return B_50_BPS;
		case 75:		return B_75_BPS;
		case 110:		return B_110_BPS;
		case 134:		return B_134_BPS;
		case 150:		return B_150_BPS;
		case 200:		return B_200_BPS;
		case 300:		return B_300_BPS;
		case 600:		return B_600_BPS;
		case 1200:		return B_1200_BPS;
		case 1800:		return B_1800_BPS;
		case 2400:		return B_2400_BPS;
		case 4800:		return B_4800_BPS;
		case 9600:		return B_9600_BPS;
		case 19200:		return B_19200_BPS;
		case 38400:		return B_38400_BPS;
		case 57600:		return B_57600_BPS;
		case 115200:	return B_115200_BPS;
		case 230400:	return B_230400_BPS;
		case 31250:     return B_31250_BPS;  // this one was in BeOS's SerialPort.h, but I am
		                                     // skeptical that this setting will work...
	}

	throw_java_exception( env, UNSUPPORTED_COMM_OPERATION,
		"translate_speed", "speed" );
	return B_0_BPS;
}

/*----------------------------------------------------------
 translate_data_bits

   accept:     gnu.io.SerialPort.DATABITS_* constant
   perform:    convert to BeOS eqivalent bits
   return:     1 if successful
               0 if an exception is thrown
   exceptions: UnsupportedCommOperationException
----------------------------------------------------------*/
int translate_data_bits( JNIEnv *env, data_bits *dbits, jint dataBits )
{
   ////////////////// Open Log File /////////////////////
   fp = fopen("RXTXOut.log", "a");
   fprintf(fp, "Brian is inside translate_data_bits\n"); // !!!
   fclose(fp);
   ////////////////// Close Log File ////////////////////

   switch( dataBits ) 
   {
		case DATABITS_7:
		    (*dbits) = B_DATA_BITS_7;
			return 1;
		case DATABITS_8:
			(*dbits) = B_DATA_BITS_8;
			return 1;
   }

   throw_java_exception( env, UNSUPPORTED_COMM_OPERATION,
		"translate_data_bits", "data bits" );
   return 0;
}

/*----------------------------------------------------------
 translate_stop_bits

   accept:     gnu.io.SerialPort.STOPBITS_* constant
   perform:    convert to BeOS eqivalent bits
   return:     1 if successful
					0 if an exception is thrown
   exceptions: UnsupportedCommOperationException
   comments:   You now can't specify anything but 1 or 2 stop bits.
----------------------------------------------------------*/
int translate_stop_bits( JNIEnv *env, stop_bits *sbits, jint stopBits )
{

   ////////////////// Open Log File /////////////////////
   fp = fopen("RXTXOut.log", "a");
   fprintf(fp, "Brian is inside translate_stop_bits\n"); // !!!
   fclose(fp);
   ////////////////// Close Log File ////////////////////

   switch( stopBits ) 
   {
		case STOPBITS_1:
			(*sbits) = B_STOP_BITS_1;
			return 1;
		case STOPBITS_2:
			(*sbits) = B_STOP_BITS_2;
			return 1;
	}

	throw_java_exception( env, UNSUPPORTED_COMM_OPERATION,
		"translate_stop_bits", "stop bits" );
	return 0;
}

/*----------------------------------------------------------
 translate_parity

   accept:     gnu.io.SerialPort.PARITY_* constant
   perform:    convert parity parameters to BeOS specific ones.
   return:     1 if successful
               0 if an exception is thrown
   exceptions: UnsupportedCommOperationException
   comments:   The CMSPAR bit not there???
----------------------------------------------------------*/
int translate_parity( JNIEnv *env, parity_mode *parmode, jint parity )
{
   ////////////////// Open Log File /////////////////////
   fp = fopen("RXTXOut.log", "a");
   fprintf(fp, "Brian is inside translate_parity\n");
   fclose(fp);
   ////////////////// Close Log File ////////////////////

   switch( parity ) 
   {
      case PARITY_NONE:
		    (*parmode) = B_NO_PARITY;
			return 1;
      case PARITY_EVEN:
			(*parmode) = B_EVEN_PARITY;
			return 1;
      case PARITY_ODD:
			(*parmode) = B_ODD_PARITY;
			return 1;
	}

	throw_java_exception( env, UNSUPPORTED_COMM_OPERATION,
		"translate_parity", "parity" );
	return 0;
}


/*----------------------------------------------------------
RXTXPort.writeByte

   accept:      byte to write (passed as int)
   perform:     write a single byte to the port
   return:      none
   exceptions:  IOException
----------------------------------------------------------*/
JNIEXPORT void JNICALL RXTXPort(writeByte)( JNIEnv *env,
	jobject jobj, jint ji )
{

   ////////////////// Open Log File /////////////////////
   fp = fopen("RXTXOut.log", "a");
   fprintf(fp, "Brian is inside RXTXPort.writeByte\n"); // !!!
   fclose(fp);
   ////////////////// Close Log File ////////////////////

	unsigned char byte = (unsigned char)ji;
	int fd = get_java_var( env, jobj,"fd","I" );
	//fprintf(fp, "RXTXPort.writeByte fd = %i\n", fd);
	ssize_t result;

    do
    {
       result = PortArray[fd]->Write(&byte, sizeof(unsigned char));
    } while ((result < (ssize_t) 0) && (errno==EINTR));
    //fclose(fp);
    //return;

	//do {
	//	result=write (14, &byte, sizeof(unsigned char));
	//}  while (result < 0 && errno==EINTR);

	if(result >= (ssize_t)0)
		return;
	throw_java_exception( env, IO_EXCEPTION, "writeByte",
		strerror( errno ) );
}


/*----------------------------------------------------------
RXTXPort.writeArray

   accept:      jbarray: bytes used for writing
                offset: offset in array to start writing
                count: Number of bytes to write
   perform:     write length bytes of jbarray
   return:      none
   exceptions:  IOException
----------------------------------------------------------*/
JNIEXPORT void JNICALL RXTXPort(writeArray)( JNIEnv *env,
	jobject jobj, jbyteArray jbarray, jint offset, jint count )
{

   ////////////////// Open Log File /////////////////////
   fp = fopen("RXTXOut.log", "a");
   fprintf(fp, "Brian is inside RXTXPort.writeArray\n"); // !!!
   fclose(fp);
   ////////////////// Close Log File ////////////////////


	int fd = get_java_var( env, jobj,"fd","I" );
	int total=0,i;
	size_t result = 0;

	unsigned char *bytes = (unsigned char *)malloc( count );

	jbyte *body = env->GetByteArrayElements(jbarray, 0); 
	for( i = 0; i < count; i++ ) bytes[ i ] = body[ i + offset ];
	env->ReleaseByteArrayElements(jbarray, body, 0);
	do {
		//result=write (fd, bytes + total, count - total);
		result = PortArray[fd]->Write(bytes + total, count - total);
		if(result >0){
			total += result;
		}
	}  while ((total<count)||(result < 0 && errno==EINTR));
	free( bytes );
	if( result < 0 ) throw_java_exception( env, IO_EXCEPTION,
		"writeArray", strerror( errno ) );
}


/*----------------------------------------------------------
RXTXPort.drain

   accept:      none
   perform:     wait until all data is transmitted
   return:      none
   exceptions:  IOException
   comments:    java.io.OutputStream.flush() is equivalent to tcdrain,
                not tcflush, which throws away unsent bytes

                count logic added to avoid infinite loops when EINTR is
                true...  Thread.yeild() was suggested.
----------------------------------------------------------*/
JNIEXPORT void JNICALL RXTXPort(drain)( JNIEnv *env,
	jobject jobj )
{

   ////////////////// Open Log File /////////////////////
   fp = fopen("RXTXOut.log", "a");
   fprintf(fp, "Brian is inside RXTXPort.drain\n"); // !!!
   fclose(fp);
   ////////////////// Close Log File ////////////////////


	int fd = get_java_var( env, jobj,"fd","I" );
	int count=0;

	do 
	{
	    PortArray[fd]->ClearOutput();
		count++;
	}  while (errno==EINTR && count <5);

	//if( result ) throw_java_exception( env, IO_EXCEPTION, "drain",
	//	strerror( errno ) );
}

/*----------------------------------------------------------
RXTXPort.sendBreak

   accept:     duration in milliseconds.
   perform:    send break for actual time.  not less than 0.25 seconds.
   exceptions: none
   comments:   not very precise
----------------------------------------------------------*/
JNIEXPORT void JNICALL RXTXPort(sendBreak)( JNIEnv *env,
	jobject jobj, jint duration )
{

   ////////////////// Open Log File /////////////////////
   fp = fopen("RXTXOut.log", "a");
   fprintf(fp, "Brian is inside RXTXPort.sendBreak\n"); // !!!
   fclose(fp);
   ////////////////// Close Log File ////////////////////


	int fd = get_java_var( env, jobj,"fd","I" );
	tcsendbreak( fd, (int)( duration / 250 ) );
}


/*----------------------------------------------------------
RXTXPort.NativegetReceiveTimeout

   accept:     none
   perform:    get termios.c_cc[VTIME]
   return:     VTIME
   comments:   see  NativeEnableReceiveTimeoutThreshold
----------------------------------------------------------*/
JNIEXPORT jint JNICALL RXTXPort(NativegetReceiveTimeout)(
	JNIEnv *env,
	jobject jobj
	)
{

   ////////////////// Open Log File /////////////////////
   fp = fopen("RXTXOut.log", "a");
   fprintf(fp, "Brian is inside RXTXPort.NativeReceiveTimeout\n"); // !!!
   fclose(fp);
   ////////////////// Close Log File ////////////////////


	int fd = get_java_var( env, jobj,"fd","I" );
	struct termios ttyset;

	if( tcgetattr( fd, &ttyset ) < 0 ) goto fail;
	return(ttyset.c_cc[ VTIME ] * 100);
fail:
	throw_java_exception( env, IO_EXCEPTION, "getReceiveTimeout",
		strerror( errno ) );
	return -1;
}

/*----------------------------------------------------------
RXTXPort.NativeisReceiveTimeoutEnabled

   accept:     none
   perform:    determine if VTIME is none 0
   return:     JNI_TRUE if VTIME > 0 else JNI_FALSE
   comments:   see  NativeEnableReceiveTimeoutThreshold
----------------------------------------------------------*/
JNIEXPORT jboolean JNICALL RXTXPort(NativeisReceiveTimeoutEnabled)(
	JNIEnv *env,
	jobject jobj
	)
{

   ////////////////// Open Log File /////////////////////
   fp = fopen("RXTXOut.log", "a");
   fprintf(fp, "Brian is inside RXTXPort.NativeisReceiveTimeoutEnabled\n"); // !!!
   fclose(fp);
   ////////////////// Close Log File ////////////////////


	int fd = get_java_var( env, jobj,"fd","I" );
	struct termios ttyset;

	if( tcgetattr( fd, &ttyset ) < 0 ) goto fail;
	return(ttyset.c_cc[ VTIME ] > 0 ? JNI_TRUE:JNI_FALSE);
fail:
	throw_java_exception( env, IO_EXCEPTION, "isReceiveTimeoutEnabled",
		strerror( errno ) );
	return JNI_FALSE;
}


/*----------------------------------------------------------
RXTXPort.isDSR

   accept:      none
   perform:     check status of DSR
   return:      true if TIOCM_DSR is set
                false if TIOCM_DSR is not set
   exceptions:  none
   comments:    DSR stands for Data Set Ready
----------------------------------------------------------*/
JNIEXPORT jboolean JNICALL RXTXPort(isDSR)( JNIEnv *env,
	jobject jobj )
{

   ////////////////// Open Log File /////////////////////
   fp = fopen("RXTXOut.log", "a");
   fprintf(fp, "Brian is inside RXTXPort.isDSR\n"); // !!!
   fclose(fp);
   ////////////////// Close Log File ////////////////////


	int fd = get_java_var( env, jobj,"fd","I" );

    if (PortArray[fd]->IsDSR())
    {
       return JNI_TRUE;
    }
    else
    {
       return JNI_FALSE;
    }
}

/*----------------------------------------------------------
RXTXPort.isCD

   accept:      none
   perform:     check status of CD
   return:      true if TIOCM_CD is set
                false if TIOCM_CD is not set
   exceptions:  none
   comments:    CD stands for Carrier Detect
                The following comment has been made...
                "well, it works, there might ofcourse be a bug, but making DCD
                permanently on fixed it for me so I don't care"

----------------------------------------------------------*/
JNIEXPORT jboolean JNICALL RXTXPort(isCD)( JNIEnv *env,
	jobject jobj )
{

   ////////////////// Open Log File /////////////////////
   fp = fopen("RXTXOut.log", "a");
   fprintf(fp, "Brian is inside RXTXPort.isCD\n"); // !!!
   fclose(fp);
   ////////////////// Close Log File ////////////////////


	int fd = get_java_var( env, jobj,"fd","I" );

    if (PortArray[fd]->IsDCD())
    {
       return JNI_TRUE;
    }
    else
    {
       return JNI_FALSE;
    }
}

/*----------------------------------------------------------
RXTXPort.isCTS

   accept:      none
   perform:     check status of CTS
   return:      true if TIOCM_CTS is set
                false if TIOCM_CTS is not set
   exceptions:  none
   comments:    CTS stands for Clear To Send.
----------------------------------------------------------*/
JNIEXPORT jboolean JNICALL RXTXPort(isCTS)( JNIEnv *env,
	jobject jobj )
{

   ////////////////// Open Log File /////////////////////
   fp = fopen("RXTXOut.log", "a");
   fprintf(fp, "Brian is inside RXTXPort.isCTS\n"); // !!!
   fclose(fp);
   ////////////////// Close Log File ////////////////////


	int fd = get_java_var( env, jobj,"fd","I" );

    if (PortArray[fd]->IsCTS())
    {
       return JNI_TRUE;
    }
    else
    {
       return JNI_FALSE;
    }
}

/*----------------------------------------------------------
RXTXPort.isRI

   accept:      none
   perform:     check status of RI
   return:      true if TIOCM_RI is set
                false if TIOCM_RI is not set
   exceptions:  none
   comments:    RI stands for Ring Indicator
----------------------------------------------------------*/
JNIEXPORT jboolean JNICALL RXTXPort(isRI)( JNIEnv *env,
	jobject jobj )
{

   ////////////////// Open Log File /////////////////////
   fp = fopen("RXTXOut.log", "a");
   fprintf(fp, "Brian is inside RXTXPort.isRI\n"); // !!!
   fclose(fp);
   ////////////////// Close Log File ////////////////////


	int fd = get_java_var( env, jobj,"fd","I" );

    if (PortArray[fd]->IsRI())
    {
       return JNI_TRUE;
    }
    else
    {
       return JNI_FALSE;
    }
}

#ifndef __BEOS__
/*----------------------------------------------------------
RXTXPort.isRTS

   accept:      none
   perform:     check status of RTS
   return:      true if TIOCM_RTS is set
                false if TIOCM_RTS is not set
   exceptions:  none
   comments:    tcgetattr with c_cflag CRTS_IFLOW
----------------------------------------------------------*/
JNIEXPORT jboolean JNICALL RXTXPort(isRTS)( JNIEnv *env,
	jobject jobj )
{

   ////////////////// Open Log File /////////////////////
   fp = fopen("RXTXOut.log", "a");
   fprintf(fp, "Brian is inside RXTXPort.isRTS\n"); // !!!
   fclose(fp);
   ////////////////// Close Log File ////////////////////


	int fd = get_java_var( env, jobj,"fd","I" );

    if (PortArray[fd]->IsRTS())
    {
       return JNI_TRUE;
    }
    else
    {
       return JNI_FALSE;
    }
}
#endif /* __BEOS__ */

/*----------------------------------------------------------
RXTXPort.setRTS

   accept:      state  flag to set/unset.
   perform:     depends on the state flag
                if true TIOCM_RTS is set
                if false TIOCM_RTS is unset
   return:      none
   exceptions:  none
   comments:    tcsetattr with c_cflag CRTS_IFLOW
----------------------------------------------------------*/
JNIEXPORT void JNICALL RXTXPort(setRTS)( JNIEnv *env,
	jobject jobj, jboolean state )
{

   ////////////////// Open Log File /////////////////////
   fp = fopen("RXTXOut.log", "a");
   fprintf(fp, "Brian is inside RXTXPort.setRTS\n"); // !!!
   fclose(fp);
   ////////////////// Close Log File ////////////////////


	int fd = get_java_var( env, jobj,"fd","I" );

    if (state == JNI_TRUE)
    {
       PortArray[fd]->SetRTS(true);
    }
    else
    {
       PortArray[fd]->SetRTS(false);
    }
	return;
}

#ifndef __BEOS__
/*----------------------------------------------------------
RXTXPort.setDSR

   accept:      state  flag to set/unset.
   perform:     depends on the state flag
                if true TIOCM_DSR is set
                if false TIOCM_DSR is unset
   return:      none
   exceptions:  none
   comments:    tcsetattr with c_cflag CRTS_IFLOW
----------------------------------------------------------*/
JNIEXPORT void JNICALL RXTXPort(setDSR)( JNIEnv *env,
	jobject jobj, jboolean state )
{

   ////////////////// Open Log File /////////////////////
   fp = fopen("RXTXOut.log", "a");
   fprintf(fp, "Brian is inside RXTXPort.setDSR\n"); // !!!
   fclose(fp);
   ////////////////// Close Log File ////////////////////


	int fd = get_java_var( env, jobj,"fd","I" );

    if (state == JNI_TRUE)
    {
       PortArray[fd]->SetDSR(true);
    }
    else
    {
       PortArray[fd]->SetDSR(false);
    }
	return;
}

/*----------------------------------------------------------
RXTXPort.isDTR

   accept:      none
   perform:     check status of DTR
   return:      true if TIOCM_DTR is set
                false if TIOCM_DTR is not set
   exceptions:  none
   comments:    DTR stands for Data Terminal Ready
----------------------------------------------------------*/
JNIEXPORT jboolean JNICALL RXTXPort(isDTR)( JNIEnv *env,
	jobject jobj )
{

   ////////////////// Open Log File /////////////////////
   fp = fopen("RXTXOut.log", "a");
   fprintf(fp, "Brian is inside RXTXPort.isDTR\n"); // !!!
   fclose(fp);
   ////////////////// Close Log File ////////////////////


	int fd = get_java_var( env, jobj,"fd","I" );

    if (PortArray[fd]->IsDTR())
    {
       return JNI_TRUE;
    }
    else
    {
       return JNI_FALSE;
    }
}
#endif /* __BEOS__ */

/*----------------------------------------------------------
RXTXPort.setDTR

   accept:      new DTR state
   perform:     if state is true, TIOCM_DTR is set
                if state is false, TIOCM_DTR is unset
   return:      none
   exceptions:  none
   comments:    DTR stands for Data Terminal Ready
----------------------------------------------------------*/
JNIEXPORT void JNICALL RXTXPort(setDTR)( JNIEnv *env,
	jobject jobj, jboolean state )
{

   ////////////////// Open Log File /////////////////////
   fp = fopen("RXTXOut.log", "a");
   fprintf(fp, "Brian is inside RXTXPort.setDTR\n"); // !!!
   fclose(fp);
   ////////////////// Close Log File ////////////////////


	int fd = get_java_var( env, jobj,"fd","I" );

    if (state == JNI_TRUE)
    {
       PortArray[fd]->SetDTR(true);
    }
    else
    {
       PortArray[fd]->SetDTR(false);
    }
	return;
}

/*----------------------------------------------------------
read_byte_array

   accept:      int                fd   file descriptor to read from
                unsigned char *buffer   buffer to read data into
                int            length   number of bytes to read
		int           timeout   milliseconds to wait before returning
   perform:     read bytes from the port into a buffer
   return:      status of read
                -1 fail (IOException)
                 0 timeout
                >0 number of bytes read
   comments:    According to the Communications API spec, a receive threshold
                of 1 is the same as having the threshold disabled.
		
		The nuts and bolts are documented in
		NativeEnableReceiveTimeoutThreshold()
----------------------------------------------------------*/
int read_byte_array( int fd, unsigned char *buffer, int length, int timeout )
{

   ////////////////// Open Log File /////////////////////
   fp = fopen("RXTXOut.log", "a");
   fprintf(fp, "Brian is inside read_byte_array\n"); // !!!
   fclose(fp);
   ////////////////// Close Log File ////////////////////


	int ret, left, bytes = 0;
	struct timeval sleep;
	struct timeval *psleep=&sleep;
#ifndef __BEOS__
	fd_set rfds;

	FD_ZERO( &rfds );
	FD_SET( fd, &rfds );
	if( timeout != 0 )
	{
		sleep.tv_sec = timeout / 1000;
		sleep.tv_usec = 1000 * ( timeout % 1000 );
	}
#endif __BEOS__
	left = length;
	while( bytes < length )
	{
#ifndef __BEOS__
         /* FIXME: In Linux, select updates the timeout automatically, so
            other OSes will need to update it manually if they want to have
            the same behavior.  For those OSes, timeouts will occur after no
            data AT ALL is received for the timeout duration.  No big deal. */
		do {
			if( timeout == 0 ) psleep = NULL;
			ret=select( fd + 1, &rfds, NULL, NULL, psleep );
		}  while (ret < 0 && errno==EINTR);
		if( ret == 0 ) break;
		if( ret < 0 ) return -1;
#endif __BEOS__
		ret = read( fd, buffer + bytes, left );
		if( ret == 0 ) break;
		if( ret < 0 ) return -1;
		bytes += ret;
		left -= ret;
	}
	return bytes;
}

/*----------------------------------------------------------
NativeEnableReceiveTimeoutThreshold
   accept:      int  threshold, int vtime,int buffer
   perform:     Set c_cc->VMIN to threshold and c_cc=>VTIME to vtime
   return:      void
   exceptions:  IOException
   comments:    This is actually all handled in read with select in
                canonical input mode.
----------------------------------------------------------*/
JNIEXPORT void JNICALL RXTXPort(NativeEnableReceiveTimeoutThreshold)(
	JNIEnv *env, jobject jobj, jint vtime, jint threshold, jint buffer)
{

   ////////////////// Open Log File /////////////////////
   fp = fopen("RXTXOut.log", "a");
   fprintf(fp, "Brian is inside NativeEnableReceiveTimeoutThreshold\n"); // !!!
   fclose(fp);
   ////////////////// Close Log File ////////////////////


	int fd = get_java_var( env, jobj,"fd","I" );
	struct termios ttyset;

	if( tcgetattr( fd, &ttyset ) < 0 ) goto fail;
	ttyset.c_cc[ VMIN ] = threshold;
	ttyset.c_cc[ VTIME ] = vtime/100;
	if( tcsetattr( fd, TCSANOW, &ttyset ) < 0 ) goto fail;

	return;
fail:
	throw_java_exception( env, IO_EXCEPTION, "TimeoutThreshold",
		strerror( errno ) );
	return;
}

/*----------------------------------------------------------
RXTXPort.readByte

   accept:      none
   perform:     Read a single byte from the port
   return:      The byte read
   exceptions:  IOException
----------------------------------------------------------*/
JNIEXPORT jint JNICALL RXTXPort(readByte)( JNIEnv *env,
	jobject jobj )
{

   ////////////////// Open Log File /////////////////////
   fp = fopen("RXTXOut.log", "a");
   fprintf(fp, "Brian is inside RXTXPort.readByte\n"); // !!!
   fclose(fp);
   ////////////////// Close Log File ////////////////////

//status_t SetTimeout(bigtime_t timeout)
	//int bytes;
	unsigned char buffer[ 1 ];
	int fd = get_java_var( env, jobj,"fd","I" );
	int timeout = get_java_var( env, jobj, "timeout", "I" );

    if (PortArray[fd]->SetTimeout((bigtime_t) timeout) != B_OK)
    {
		throw_java_exception( env, IO_EXCEPTION, "readByte",
			strerror( errno ) );
		return -1;
    }
    if (PortArray[fd]->Read(&buffer[0], sizeof(unsigned char)) != 1)
    {
		throw_java_exception( env, IO_EXCEPTION, "readByte",
			strerror( errno ) );
		return -1;
    }

    return (jint)buffer[0];
    /*
	bytes = read_byte_array( fd, buffer, 1, timeout );
	if( bytes < 0 ) {
		throw_java_exception( env, IO_EXCEPTION, "readByte",
			strerror( errno ) );
		return -1;
	}
	return (bytes ? (jint)buffer[ 0 ] : -1);
    */
}

/*----------------------------------------------------------
RXTXPort.readArray

   accept:       offset (offset to start storing data in the jbarray) and
                 Length (bytes to read)
   perform:      read bytes from the port into a byte array
   return:       bytes read on success
                 0 on read timeout
   exceptions:   IOException
   comments:     throws ArrayIndexOutOfBoundsException if asked to
                 read more than SSIZE_MAX bytes
----------------------------------------------------------*/
JNIEXPORT jint JNICALL RXTXPort(readArray)( JNIEnv *env,
	jobject jobj, jbyteArray jbarray, jint offset, jint length )
{

   ////////////////// Open Log File /////////////////////
   fp = fopen("RXTXOut.log", "a");
   fprintf(fp, "Brian is inside RXTXPort.readArray\n"); // !!!
   fclose(fp);
   ////////////////// Close Log File ////////////////////


	int bytes;
	jbyte *body;
	unsigned char *buffer;
	int fd = get_java_var( env, jobj, "fd", "I" );
	int timeout = get_java_var( env, jobj, "timeout", "I" );

	if( length > SSIZE_MAX || length < 0 ) {
		throw_java_exception( env, ARRAY_INDEX_OUT_OF_BOUNDS,
			"readArray", "Invalid length" );
		return -1;
	}

	buffer = (unsigned char *)malloc( sizeof( unsigned char ) * length );
	if( buffer == 0 ) {
		throw_java_exception( env, OUT_OF_MEMORY, "readArray",
			"Unable to allocate buffer" );
		return -1;
	}

	bytes = read_byte_array( fd, buffer, length, timeout );
	if( bytes < 0 ) {
		free( buffer );
		throw_java_exception( env, IO_EXCEPTION, "readArray",
			strerror( errno ) );
		return -1;
	}
	//body = (*env)->GetByteArrayElements( env, jbarray, 0 );
	body = env->GetByteArrayElements( jbarray, 0 ); // !!!
	memcpy(body + offset, buffer, bytes);
	//(*env)->ReleaseByteArrayElements( env, jbarray, body, 0 );
	env->ReleaseByteArrayElements( jbarray, body, 0 ); // !!!
	free( buffer );
	return (bytes ? bytes : -1);
}

/*----------------------------------------------------------
RXTXPort.nativeavailable

   accept:      none
   perform:     find out the number of bytes available for reading
   return:      available bytes
                -1 on error
   exceptions:  none
----------------------------------------------------------*/
JNIEXPORT jint JNICALL RXTXPort(nativeavailable)( JNIEnv *env,
	jobject jobj )
{

   ////////////////// Open Log File /////////////////////
   fp = fopen("RXTXOut.log", "a");
   fprintf(fp, "Brian is inside RXTXPort.nativeavailable\n"); // !!!
   fclose(fp);
   ////////////////// Close Log File ////////////////////


//#ifndef __BEOS__
	int fd = get_java_var( env, jobj,"fd","I" );
	//int result;
	ssize_t result = 0;
    result = PortArray[fd]->WaitForInput(); // WaitForInput() not the best choice, but all I could find.
    return (jint)result;
    /*
	if( ioctl( fd, FIONREAD, &result ) )
	{
		throw_java_exception( env, IO_EXCEPTION, "nativeavailable",
			strerror( errno ) );
		return -1;
	}
	else return (jint)result;
	*/
//#endif
//return (-1);
}

/*----------------------------------------------------------
RXTXPort.setflowcontrol

   accept:      flowmode
	FLOWCONTROL_NONE        none
	FLOWCONTROL_RTSCTS_IN   hardware flow control
	FLOWCONTROL_RTSCTS_OUT         ""
	FLOWCONTROL_XONXOFF_IN  input software flow control
	FLOWCONTROL_XONXOFF_OUT output software flow control
   perform:     set flow control to flowmode
   return:      none
   exceptions:  IOException
   comments:  there is no differentiation between input and output hardware
              flow control
----------------------------------------------------------*/
JNIEXPORT void JNICALL RXTXPort(setflowcontrol)( JNIEnv *env,
	jobject jobj, jint flowmode )
{

   ////////////////// Open Log File /////////////////////
   fp = fopen("RXTXOut.log", "a");
   fprintf(fp, "Brian is inside RXTXPort.setflowcontrol\n"); // !!!
   fclose(fp);
   ////////////////// Close Log File ////////////////////

   int fd = get_java_var( env, jobj,"fd","I" );
   switch( flowmode ) 
   {
       case FLOWCONTROL_NONE:
		    PortArray[fd]->SetFlowControl(B_NOFLOW_CONTROL);
		    if (PortArray[fd]->FlowControl() != B_NOFLOW_CONTROL)
		    {
	           throw_java_exception( env, IO_EXCEPTION, "setHWFC",
		       strerror( errno ) );
	           return;   
		    }
			break;
       case FLOWCONTROL_RTSCTS_IN:
			PortArray[fd]->SetFlowControl(B_HARDWARE_CONTROL);
		    if (PortArray[fd]->FlowControl() != B_HARDWARE_CONTROL)
		    {
	           throw_java_exception( env, IO_EXCEPTION, "setHWFC",
		       strerror( errno ) );
	           return;   
		    }			
			break;
       case FLOWCONTROL_RTSCTS_OUT:
			PortArray[fd]->SetFlowControl(B_HARDWARE_CONTROL);
		    if (PortArray[fd]->FlowControl() != B_HARDWARE_CONTROL)
		    {
	           throw_java_exception( env, IO_EXCEPTION, "setHWFC",
		       strerror( errno ) );
	           return;   
		    }
			break;
	   case FLOWCONTROL_XONXOFF_IN:
	        PortArray[fd]->SetFlowControl(B_SOFTWARE_CONTROL);
		    if (PortArray[fd]->FlowControl() != B_SOFTWARE_CONTROL)
		    {
	           throw_java_exception( env, IO_EXCEPTION, "setHWFC",
		       strerror( errno ) );
	           return;   
		    }
            break;
	   case FLOWCONTROL_XONXOFF_OUT:
	        PortArray[fd]->SetFlowControl(B_SOFTWARE_CONTROL);
		    if (PortArray[fd]->FlowControl() != B_SOFTWARE_CONTROL)
		    {
	           throw_java_exception( env, IO_EXCEPTION, "setHWFC",
		       strerror( errno ) );
	           return;   
		    }
	        break;
	}
    return;
    

    
/*
	if( tcgetattr( fd, &ttyset ) ) goto fail;
	
	if ( flowmode & ( FLOWCONTROL_RTSCTS_IN | FLOWCONTROL_RTSCTS_OUT ) )
		ttyset.c_cflag |= HARDWARE_FLOW_CONTROL;
	else ttyset.c_cflag &= ~HARDWARE_FLOW_CONTROL;

	ttyset.c_iflag &= ~IXANY;

	if ( flowmode & FLOWCONTROL_XONXOFF_IN )
		ttyset.c_iflag |= IXOFF;
	else ttyset.c_iflag &= ~IXOFF;

	if ( flowmode & FLOWCONTROL_XONXOFF_OUT )
		ttyset.c_iflag |= IXON;
	else ttyset.c_iflag &= ~IXON;

	if( tcsetattr( fd, TCSANOW, &ttyset ) ) goto fail;
	return;
fail:
	throw_java_exception( env, IO_EXCEPTION, "setHWFC",
		strerror( errno ) );
	return;
*/
}

#ifdef  __BEOS__
/*----------------------------------------------------------
RXTXPort.eventLoop

   accept:      none
   perform:     periodically check for SerialPortEvents
   return:      none
   exceptions:  none
   comments:
----------------------------------------------------------*/
JNIEXPORT void JNICALL RXTXPort(eventLoop)( JNIEnv *env, jobject jobj )
{
	printf("BeOS eventLoop not Implemented\n");
#else /*  __BEOS__ */

   ////////////////// Open Log File /////////////////////
   fp = fopen("RXTXOut.log", "a");
   fprintf(fp, "Brian is inside RXTXPort.eventLoop\n"); // !!!
   fclose(fp);
   ////////////////// Close Log File ////////////////////


	int fd, ret, change;
	fd_set rfds;
	struct timeval tv_sleep;
	unsigned int mflags, omflags;
	jboolean interrupted = 0;
#if defined TIOCSERGETLSR
	struct stat fstatbuf;
#endif /* TIOCSERGETLSR */

#if defined(TIOCGICOUNT)
	struct serial_icounter_struct sis, osis;
	/* JK00: flag if this can be used on this port */
	int has_tiocgicount = 1;
#endif /* TIOCGICOUNT */

#if defined(TIOCSERGETLSR)
	int has_tiocsergetlsr = 1;
#endif /* TIOCSERGETLSR */

	fd = get_java_var(env, jobj, "fd", "I");

#if defined(TIOCGICOUNT)
	/* Some multiport serial cards do not implement TIOCGICOUNT ... */
	/* So use the 'dumb' mode to enable using them after all! JK00 */
	if( ioctl( fd, TIOCGICOUNT, &osis ) < 0 ) {
		report("Port does not support TIOCGICOUNT events\n" );
		has_tiocgicount = 0;
	}
#endif /*  TIOCGICOUNT */

#if defined(TIOCSERGETLSR)
	/* JK00: work around for multiport cards without TIOCSERGETLSR */
	/* Cyclades is one of those :-(				       */
	if( ioctl( fd, TIOCSERGETLSR, &change ) ) {
		report("Port does not support TIOCSERGETLSR\n" );
			has_tiocsergetlsr = 0;
	}
#endif /* TIOCSERGETLSR */

	if( ioctl( fd, TIOCMGET, &omflags) <0 ) {
		report("Port does not support events\n" );
 		return;
	}

	FD_ZERO( &rfds );
	while( !interrupted ) {
		FD_SET( fd, &rfds );
		tv_sleep.tv_sec = 0;
		tv_sleep.tv_usec = 100000;
		do {
			ret=select( fd + 1, &rfds, NULL, NULL, &tv_sleep );
		}  while (ret < 0 && errno==EINTR);
		if( ret < 0 ) break;

		interrupted = is_interrupted(env, jobj);
		if(interrupted) return;

#if defined TIOCSERGETLSR
		/* JK00: work around for Multi IO cards without TIOCSERGETLSR */
		if( has_tiocsergetlsr ) {
			if (fstat(fd, &fstatbuf))  break;
			if( ioctl( fd, TIOCSERGETLSR, &change ) ) break;
			else if( change )
				send_event( env, jobj, SPE_OUTPUT_BUFFER_EMPTY,
					1 );
		}
#endif /* TIOCSERGETLSR */
#if defined(TIOCGICOUNT)
	/*	wait for RNG, DSR, CD or CTS  but not DataAvailable
	 *      The drawback here is it never times out so if someone
	 *      reads there will be no chance to try again.
	 *      This may make sense if the program does not want to
	 *      be notified of data available or errors.
	 *	ret=ioctl(fd,TIOCMIWAIT);
	 */
		/* JK00: only use it if supported by this port */
		if (has_tiocgicount) {
			if( ioctl( fd, TIOCGICOUNT, &sis ) ) break;
			while( sis.frame != osis.frame ) {
				send_event( env, jobj, SPE_FE, 1);
				osis.frame++;
			}
			while( sis.overrun != osis.overrun ) {
				send_event( env, jobj, SPE_OE, 1);
				osis.overrun++;
			}
			while( sis.parity != osis.parity ) {
				send_event( env, jobj, SPE_PE, 1);
				osis.parity++;
			}
			while( sis.brk != osis.brk ) {
				send_event( env, jobj, SPE_BI, 1);
				osis.brk++;
			}
			osis = sis;
		}
#endif /*  TIOCGICOUNT */
	       /* A Portable implementation */

		if( ioctl( fd, TIOCMGET, &mflags ) ) break;

		change = (mflags&TIOCM_CTS) - (omflags&TIOCM_CTS);
		if( change ) send_event( env, jobj, SPE_CTS, change );

		change = (mflags&TIOCM_DSR) - (omflags&TIOCM_DSR);
		if( change ) send_event( env, jobj, SPE_DSR, change );

		change = (mflags&TIOCM_RNG) - (omflags&TIOCM_RNG);
		if( change ) send_event( env, jobj, SPE_RI, change );

		change = (mflags&TIOCM_CD) - (omflags&TIOCM_CD);
		if( change ) send_event( env, jobj, SPE_CD, change );

		omflags = mflags;

		ioctl( fd, FIONREAD, &change );
		if( change ) {
			if(!send_event( env, jobj, SPE_DATA_AVAILABLE, 1 ))
				usleep(100000); /* select wont block */
		}
	}
	return;
#endif /* __BEOS__ */
}

/*----------------------------------------------------------
 isDeviceGood

   accept:      a port name
   perform:     see if the port is valid on this OS.
   return:      JNI_TRUE if it exhists otherwise JNI_FALSE
   exceptions:  none
   comments:
----------------------------------------------------------*/
JNIEXPORT jboolean  JNICALL RXTXCommDriver(isDeviceGood)(JNIEnv *env,
	jobject jobj, jstring tty_name)
{

   ////////////////// Open Log File /////////////////////
   fp = fopen("RXTXOut.log", "a");
   fprintf(fp, "Brian is inside isDeviceGood\n"); // !!!
   fclose(fp);
   ////////////////// Close Log File ////////////////////


	jboolean result;
	static struct stat mystat;
	char teststring[256];
	int fd, i;
	const char *name = env->GetStringUTFChars(tty_name, 0); // !!!
	printf("inside RXTXCommDriver:isDeviceGood() deviceName is %s.  ",name);
    //!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
    //Let's try to enumerate the ports
    /*
    int32 portcount = 0;
    int32 n = 0;
    port = new BSerialPort();
    char devName[B_OS_NAME_LENGTH];
    portcount = port->CountDevices();
    for (n = portcount - 1; n >= 0; n--)
    {
       port->GetDeviceName(n, devName);
       printf("inside RXTXCommDriver:isDeviceGood() deviceName is %s.  ",devName);
    }
    */
	for(i=0;i<64;i++){
#if defined(_GNU_SOURCE)
		snprintf(teststring, 256, "%s%s%i",DEVICEDIR,name, i);
#else
		sprintf(teststring,"%s%s%i",DEVICEDIR,name, i);
#endif /* _GNU_SOURCE */
		stat(teststring,&mystat);
		if(S_ISCHR(mystat.st_mode)){
			printf("inside RXTXCommDriver:isDeviceGood() trying to open %s.  ",teststring);
			fd=open(teststring,O_RDONLY|O_NONBLOCK);
			printf("got %i\n",fd);
			if (fd>0){
				printf("inside RXTXCommDriver:isDeviceGood() %s is valid on this system.\n",teststring);
				close(fd);
				result=JNI_TRUE;
				break;
			}
		}
		result=JNI_FALSE;
	}
#if defined(_GNU_SOURCE)
	snprintf(teststring, 256, "%s%s",DEVICEDIR,name);
#else
	sprintf(teststring,"%s%s",DEVICEDIR,name);
#endif /* _GNU_SOURCE */
	stat(teststring,&mystat);
	if(S_ISCHR(mystat.st_mode)){
		fd=open(teststring,O_RDONLY|O_NONBLOCK);
		if (fd>0){
			close(fd);
			result=JNI_TRUE;
		}
	}
	printf("result is %i for %s\n",result, name);
	env->ReleaseStringUTFChars(tty_name, name); // !!!
	return(result);
}

/*----------------------------------------------------------
 getDeviceDirectory

   accept:      
   perform:     
   return:      the directory containing the device files
   exceptions:  
   comments:    use this to avoid hard coded "/dev/"
   		values are in SerialImp.h
----------------------------------------------------------*/
/*
JNIEXPORT jstring JNICALL Java_gnu_io_RXTXCommDriver_getDeviceDirectory(JNIEnv*, jobject);

*/
JNIEXPORT jstring  JNICALL RXTXCommDriver(getDeviceDirectory)(JNIEnv *env,
	jobject jobj, int test)
{
	return env->NewStringUTF(DEVICEDIR);
}
/*----------------------------------------------------------
 setInputBufferSize

   accept:      
   perform:     
   return:      none
   exceptions:  none
   comments:    see fopen/fclose/fwrite/fread man pages.
----------------------------------------------------------*/
JNIEXPORT void JNICALL RXTXPort(setInputBufferSize)(JNIEnv *env,
	jobject jobj,  jint size )
{
	report("setInputBufferSize is not implemented\n");
}
/*----------------------------------------------------------
 getIputBufferSize

   accept:      
   perform:     
   return:      none
   exceptions:  none
   comments:    see fopen/fclose/fwrite/fread man pages.
----------------------------------------------------------*/
JNIEXPORT jint JNICALL RXTXPort(getInputBufferSize)(JNIEnv *env,
	jobject jobj)
{
	report("getInputBufferSize is not implemented\n");
	return(1);
}
/*----------------------------------------------------------
 setOutputBufferSize

   accept:      
   perform:     
   return:      none
   exceptions:  none
   comments:    see fopen/fclose/fwrite/fread man pages.
----------------------------------------------------------*/
JNIEXPORT void JNICALL RXTXPort(setOutputBufferSize)(JNIEnv *env,
	jobject jobj, jint size )
{
	report("setOutputBufferSize is not implemented\n");
}
/*----------------------------------------------------------
 getOutputBufferSize

   accept:      
   perform:     
   return:      none
   exceptions:  none
   comments:    see fopen/fclose/fwrite/fread man pages.
----------------------------------------------------------*/
JNIEXPORT jint JNICALL RXTXPort(getOutputBufferSize)(JNIEnv *env,
	jobject jobj)
{
	report("getOutputBufferSize is not implemented\n");
	return(1);
}

/*----------------------------------------------------------
 is_interrupted

   accept:      
   perform:     see if the port is being closed. 
   return:      a positive value if the port is being closed.
   exceptions:  none
   comments:
----------------------------------------------------------*/
jboolean is_interrupted(JNIEnv *env, jobject jobj)
{

   ////////////////// Open Log File /////////////////////
   fp = fopen("RXTXOut.log", "a");
   fprintf(fp, "Brian is is_interrupted\n"); // !!!
   fclose(fp);
   ////////////////// Close Log File ////////////////////


	jmethodID foo;
	jclass jclazz;
	int result;

	//(*env)->ExceptionClear(env);
    env->ExceptionClear(); // !!!

	//jclazz = (*env)->GetObjectClass( env, jobj );
	jclazz = env->GetObjectClass( jobj ); // !!!
	if(jclazz == NULL) return JNI_TRUE;

	//foo = (*env)->GetMethodID( env, jclazz, "checkMonitorThread", "()Z");
	foo = env->GetMethodID( jclazz, "checkMonitorThread", "()Z"); // !!!
	if(foo == NULL) return JNI_TRUE;

	//result = (*env)->CallBooleanMethod( env, jobj, foo );
	result = env->CallBooleanMethod( jobj, foo ); // !!!

#ifdef DEBUG
	//if((*env)->ExceptionOccurred(env)) {
    if(env->ExceptionOccurred()) {    // !!!	
		report ("an error occured calling sendEvent()\n");
		//(*env)->ExceptionDescribe(env);
		env->ExceptionDescribe(); // !!!
		//(*env)->ExceptionClear(env);
		env->ExceptionClear(); // !!!
	}
#endif /* DEBUG */
	//(*env)->DeleteLocalRef( env, jclazz );
    env->DeleteLocalRef( jclazz ); // !!!
	return(result);
}

/*----------------------------------------------------------
 send_event

   accept:      The event type and the event state     
   perform:     if state is > 0 send a JNI_TRUE event otherwise send JNI_FALSE
   return:      a positive value if the port is being closed.
   exceptions:  none
   comments:
----------------------------------------------------------*/
int send_event(JNIEnv *env, jobject jobj, jint type, int flag)
{

   ////////////////// Open Log File /////////////////////
   fp = fopen("RXTXOut.log", "a");
   fprintf(fp, "Brian is inside send_event\n"); // !!!
   fclose(fp);
   ////////////////// Close Log File ////////////////////


	int result;
	jmethodID foo;
	//jclass jclazz = (*env)->GetObjectClass( env, jobj );
	jclass jclazz = env->GetObjectClass( jobj ); // !!!

	if(jclazz == NULL) return JNI_TRUE;
	//foo = (*env)->GetMethodID( env, jclazz, "sendEvent", "(IZ)Z" );
    foo = env->GetMethodID( jclazz, "sendEvent", "(IZ)Z" ); // !!!

	//(*env)->ExceptionClear(env);
	env->ExceptionClear(); // !!!

	//result = (*env)->CallBooleanMethod( env, jobj, foo, type,
	result = env->CallBooleanMethod( jobj, foo, type,  // !!!
		flag > 0 ? JNI_TRUE : JNI_FALSE );

#ifdef DEBUG
	//if((*env)->ExceptionOccurred(env)) {
	if(env->ExceptionOccurred()) {  // !!!
		report ("an error occured calling sendEvent()\n");
		//(*env)->ExceptionDescribe(env);
		env->ExceptionDescribe(); // !!!
		//(*env)->ExceptionClear(env);
		env->ExceptionClear(); // !!!
	}
#endif /* DEBUG */
	//(*env)->DeleteLocalRef( env, jclazz );
	env->DeleteLocalRef( jclazz ); // !!!
	return(result);
}
/*----------------------------------------------------------
get_java_var

   accept:      env (keyhole to java)
                jobj (java RXTXPort object)
   return:      the fd field from the java object
   exceptions:  none
   comments:
----------------------------------------------------------*/
int get_java_var( JNIEnv *env, jobject jobj, char *id, char *type )
{


   ////////////////// Open Log File /////////////////////
   fp = fopen("RXTXOut.log", "a");
   fprintf(fp, "Brian is inside get_java_var\n"); // !!!
   fclose(fp);
   ////////////////// Close Log File ////////////////////

	int result = 0;
	//jclass jclazz = (*env)->GetObjectClass( env, jobj );
	jclass jclazz = env->GetObjectClass( jobj ); // !!!
	//jfieldID jfd = (*env)->GetFieldID( env, jclazz, id, type );
    jfieldID jfd = env->GetFieldID( jclazz, id, type ); // !!!
    
	if( !jfd ) {
		//(*env)->ExceptionDescribe( env );
		env->ExceptionDescribe(); // !!!
		//(*env)->ExceptionClear( env );
		env->ExceptionClear(); // !!!
		return result;
	}
	//result = (int)( (*env)->GetIntField( env, jobj, jfd ) );
	result = (int)( env->GetIntField( jobj, jfd ) ); // !!!
/* ct7 & gel * Added DeleteLocalRef */
	//(*env)->DeleteLocalRef( env, jclazz );
	env->DeleteLocalRef( jclazz ); // !!!
#ifdef DEBUG
	if(!strncmp("fd",id,2) && result == 0)
		report("invalid file descriptor\n");
#endif
	return result;
}

/*----------------------------------------------------------
throw_java_exception

   accept:      env (keyhole to java)
                *exc (exception class name)
                *foo (function name)
                *msg (error message)
   perform:     Throw a new java exception
   return:      none
   exceptions:  haha!
   comments:
----------------------------------------------------------*/
void throw_java_exception( JNIEnv *env, char *exc, char *foo, char *msg )
{

   ////////////////// Open Log File /////////////////////
   fp = fopen("RXTXOut.log", "a");
   fprintf(fp, "Brian is inside throw_java_exception\n\n"); // !!!
   fprintf(fp, "%s in %s\n", msg, foo ); // !!!
   fclose(fp);
   ////////////////// Close Log File ////////////////////


	char buf[ 60 ];
	//jclass clazz = (*env)->FindClass( env, exc );
	jclass clazz = env->FindClass( exc ); // !!!
	if( !clazz ) {
		//(*env)->ExceptionDescribe( env );
		env->ExceptionDescribe(); // !!!
		//(*env)->ExceptionClear( env );
		env->ExceptionClear(); // !!!
		return;
	}
#if defined(_GNU_SOURCE)
	snprintf( buf, 60, "%s in %s", msg, foo );
#else
	sprintf( buf,"%s in %s", msg, foo );
#endif /* _GNU_SOURCE */
	//(*env)->ThrowNew( env, clazz, buf );
	env->ThrowNew( clazz, buf ); // !!!
/* ct7 * Added DeleteLocalRef */
	//(*env)->DeleteLocalRef( env, clazz );
	env->DeleteLocalRef( clazz ); // !!!
}

/*----------------------------------------------------------
 report

   accept:      string to send to stderr     
   perform:     if DEBUG is defined send the string to stderr.
   return:      none
   exceptions:  none
   comments:
----------------------------------------------------------*/
void report(char *msg)
{

   ////////////////// Open Log File /////////////////////
   fp = fopen("RXTXOut.log", "a");
   fprintf(fp, "Brian is inside report\n"); // !!!
   fclose(fp);
   ////////////////// Close Log File ////////////////////


#ifdef DEBUG
	fprintf(stderr, "%s", msg);
#endif
}

/*----------------------------------------------------------
 dump_termios

   accept:      string to indicate where this was called.
                termios struct
   perform:     print the termios struct to stderr.
   return:      none
   exceptions:  none
   comments:    used to debug the termios struct.
----------------------------------------------------------*/
void dump_termios(char *foo,struct termios *ttyset)
{

   ////////////////// Open Log File /////////////////////
   fp = fopen("RXTXOut.log", "a");
   fprintf(fp, "Brian is inside dump_termios\n"); // !!!
   fclose(fp);
   ////////////////// Close Log File ////////////////////


	int i;

	//fprintf(stderr,"%s %o\n",foo,ttyset->c_iflag);
	//fprintf(stderr,"%s %o\n",foo,ttyset->c_lflag);
	//fprintf(stderr,"%s %o\n",foo,ttyset->c_oflag);
	//fprintf(stderr,"%s %o\n",foo,ttyset->c_cflag);
	for(i=0;i<NCCS;i++)
	{
		fprintf(stderr,"%s %o ",foo,ttyset->c_cc[i]);
	}
	fprintf(stderr,"\n");
}
