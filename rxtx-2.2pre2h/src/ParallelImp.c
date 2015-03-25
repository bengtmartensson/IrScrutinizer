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
/*
   fear he who enter here.  It appears that things have changed.  An attempt
   has been made to put things the way the should be.

        It compiles and ParallelBlackBox runs.  No further guarantees.

	Well... One.. it will print "Hello World!" on an epson DX 10 printer.
	you know.. the 10 character per second daisy wheel printer ;)

        - Trent Jarvi
*/

#if defined(__MWERKS__)/* dima */
#include "LPRPort.h"
#else /* dima */
#ifndef WIN32
#	include "config.h"
#endif
/* work around for libc5 */
/*#include <typedefs_md.h>*/
#include "gnu_io_LPRPort.h"
#endif /* dima */
#include <time.h>
#include <stdio.h>
#include <string.h>
#ifndef WIN32
#	include <unistd.h>
#	include <sys/ioctl.h>
#	include <sys/errno.h>
#	include <sys/param.h>
#else
#	include <errno.h>
#	include "win32termios.h"
#endif
#include <sys/types.h>
#include <sys/stat.h>
#include <stdlib.h>

#ifdef HAVE_TERMIOS_H
#	include <termios.h>
#endif
#ifdef HAVE_SYS_FCNTL_H
#   include <sys/fcntl.h>
#endif
#ifdef HAVE_SYS_FILE_H
#   include <sys/file.h>
#endif
#ifdef HAVE_SYS_SIGNAL_H
#   include <sys/signal.h>
#endif
#ifdef HAVE_SYS_TIME_H
#   include <sys/time.h>
#endif /* HAVE_SYS_TIME_H */
#if defined(__linux__)
#	include <linux/lp.h>
#endif
#if defined(__FreeBSD__)
/* #	include <machine/lpt.h> ? is this changed or wrong */
#	include <dev/ppbus/lpt.h>
#endif
#ifdef __unixware__
#	include  <sys/filio.h>
#endif

extern int errno;

#include "ParallelImp.h"

#define LPRPort(foo) Java_gnu_io_LPRPort_ ## foo
/* #define DEBUG */

/*----------------------------------------------------------
LPRPort.getOutputBufferFree
   accept:    none
   perform:
   return:    number of bytes available in buffer.
   exceptions: none
   comments:  have not seen how to do this in the kernel yet.
----------------------------------------------------------*/
JNIEXPORT jint JNICALL LPRPort(getOutputBufferFree)(JNIEnv *env,
	jclass jclazz)
{
	printf("getOutputBufferFree is not implemented yet\n");

	return(0);

}
/*----------------------------------------------------------
LPRPort.setLPRMode
   accept:     mode
   perform:    set the Printer communication mode
	LPT_MODE_ANY:     pick the best possible mode
	LPT_MODE_SPP:     compatibility mode/unidirectional
	LPT_MODE_PS2:     byte mode/bidirectional
	LPT_MODE_EPP:     extended parallel port
	LPT_MODE_ECP:     enhanced capabilities port
	LPT_MODE_NIBBLE:  Nibble Mode. Bi-directional. HP Bi-tronics.
	                  4 bits at a time.
   return:     none
   exceptions: UnsupportedCommOperationException
   comments:
----------------------------------------------------------*/
JNIEXPORT jboolean JNICALL LPRPort(setLPRMode)(JNIEnv *env,
	jclass jclazz, jint mode)
{
	switch(mode)
	{
		case LPT_MODE_ANY:
			break;
		case LPT_MODE_SPP:
		case LPT_MODE_PS2:
		case LPT_MODE_EPP:
		case LPT_MODE_ECP:
		case LPT_MODE_NIBBLE:
		default:
			throw_java_exception( env, UNSUPPORTED_COMM_OPERATION,
				"nativeSetSerialPortParams",
				"setLPRMode was unable to proced the requested \
				mode"
			);
	}
	return(JNI_TRUE);
}

#if defined (WIN32)

#if ! defined(_MSC_VER)
/* CTL_CODE is already defined in winioctl.h with MSVC - not redefining over platform SDK headers.
   MSVC does not need this */
#define CTL_CODE( DeviceType, Function, Method, Access ) (((DeviceType) << 16) | ((Access) << 14) | ((Function) << 2) | (Method))
#endif /* MSVC */

#define FILE_DEVICE_PARALLEL_PORT       0x00000016
#define METHOD_BUFFERED                 0
#define FILE_ANY_ACCESS                 0

#define IOCTL_PAR_QUERY_INFORMATION CTL_CODE(FILE_DEVICE_PARALLEL_PORT,1,METHOD_BUFFERED,FILE_ANY_ACCESS)

#define PARALLEL_INIT            0x1
#define PARALLEL_AUTOFEED        0x2
#define PARALLEL_PAPER_EMPTY     0x4
#define PARALLEL_OFF_LINE        0x8
#define PARALLEL_POWER_OFF       0x10
#define PARALLEL_NOT_CONNECTED   0x20
#define PARALLEL_BUSY            0x40
#define PARALLEL_SELECTED        0x80

int getWin32ParallelStatusFlags(int fd){
	int status = 0;
	DWORD count;
	if(!DeviceIoControl( (HANDLE)fd, IOCTL_PAR_QUERY_INFORMATION, NULL,0, &status, sizeof(status), &count, 0) && count == 1){
		YACK();
		return 0;
	}
#ifdef DEBUG
	printf("getWin32ParallelStatusFlags: %d\n", status );
#endif
	return status;
}

jboolean getWin32ParallelStatus(int fd, int flag){
	return( (getWin32ParallelStatusFlags(fd) & flag) ? JNI_TRUE : JNI_FALSE);
}
#endif

/*----------------------------------------------------------
LPRPort.isPaperOut
   accept:      none
   perform:     check if printer reports paper is out
   return:      Paper Out: JNI_TRUE  Not Paper Out: JNI_FALSE
   exceptions:  none
   comments:    LP_NOPA    unchanged out-of-paper input, active high
----------------------------------------------------------*/
JNIEXPORT jboolean JNICALL LPRPort(isPaperOut)(JNIEnv *env,
	jobject jobj)
{

	int fd = get_java_var( env, jobj,"fd","I" );
#if defined (__linux__)
	int status;
	ioctl(fd, LPGETSTATUS,&status);
	return( status & LP_NOPA ? JNI_TRUE : JNI_FALSE );
#elif defined (WIN32)
	return getWin32ParallelStatus( fd, PARALLEL_PAPER_EMPTY);
#else
/*  FIXME??  */
	printf("ParallelImp.c LPGETSTATUS not defined\n");
	return(JNI_TRUE);
#endif
}
/*----------------------------------------------------------
LPRPort.isPrinterBusy
   accept:      none
   perform:     Check to see if the printer is printing.
   return:      JNI_TRUE if the printer is Busy, JNI_FALSE if its idle
   exceptions:  none
   comments:    LP_BUSY     inverted busy input, active high
----------------------------------------------------------*/
JNIEXPORT jboolean JNICALL LPRPort(isPrinterBusy)(JNIEnv *env,
	jobject jobj)
{
	int fd = get_java_var( env, jobj,"fd","I" );
#if defined(__FreeBSD__) || defined(__linux__)
	int status;
#endif
#if defined (__linux__)
	ioctl(fd, LPGETSTATUS, &status);
#elif defined (WIN32)
	return getWin32ParallelStatus( fd, PARALLEL_BUSY);
#else
/*  FIXME??  */
	printf("ParallelImp.c LPGETSTATUS not defined\n");
#endif
#if defined(__linux__)
	return( status & LP_BUSY ? JNI_TRUE : JNI_FALSE );
#endif
#if defined(__FreeBSD__)
	return( status & EBUSY ? JNI_TRUE : JNI_FALSE );
#endif
	return(JNI_FALSE);
}
/*----------------------------------------------------------
LPRPort.isPrinterError
   accept:      none
   perform:     check for printer error
   return:      JNI_TRUE if there is an printer error otherwise JNI_FALSE
   exceptions:  none
   comments:    LP_ERR   unchanged error input, active low
----------------------------------------------------------*/
JNIEXPORT jboolean JNICALL LPRPort(isPrinterError)(JNIEnv *env,
	jobject jobj)
{
	int fd = get_java_var( env, jobj,"fd","I" );
#if defined (__linux__)
	int status;
	ioctl(fd, LPGETSTATUS, &status);
	return( status & LP_ERR ? JNI_TRUE : JNI_FALSE );
#elif defined (WIN32)
	return getWin32ParallelStatus( fd, PARALLEL_PAPER_EMPTY |
									   PARALLEL_OFF_LINE |
									   PARALLEL_POWER_OFF |
									   PARALLEL_NOT_CONNECTED |
									   PARALLEL_BUSY);
#else
/*  FIXME??  */
	printf("ParallelImp.c LPGETSTATUS not defined\n");
	return(JNI_FALSE);
#endif
}
/*----------------------------------------------------------
LPRPort.isPrinterSelected
   accept:      none
   perform:     check if printer is selected
   return:      JNI_TRUE if printer is selected other wise JNI_FALSE
   exceptions:  none
   comments:    LP_SELEC   unchanged selected input, active high
----------------------------------------------------------*/
JNIEXPORT jboolean JNICALL LPRPort(isPrinterSelected)(JNIEnv *env,
	jobject jobj)
{
	int fd = get_java_var( env, jobj,"fd","I" );
#if defined (__linux__)
	int status;
	ioctl(fd, LPGETSTATUS, &status);
	return( status & LP_SELEC ? JNI_TRUE : JNI_FALSE );
#elif defined (WIN32)
	return getWin32ParallelStatus( fd, PARALLEL_SELECTED);
#else
/*  FIXME??  */
	printf("ParallelImp.c LPGETSTATUS not defined\n");
	return(JNI_FALSE);
#endif
}
/*----------------------------------------------------------
LPRPort.isPrinterTimedOut
   accept:       none
   perform:      Not really sure see isPaperOut
   return:       JNI_FALSE if the printer does not return out of paper other
                 wise JNI_TRUE.
   exceptions:   none
   comments:     Is this documented right in the javadocs?
	         not sure this is correct FIXME
----------------------------------------------------------*/
JNIEXPORT jboolean JNICALL LPRPort(isPrinterTimedOut)(JNIEnv *env,
	jobject jobj)
{
#if defined(__linux__)
	int fd = get_java_var( env, jobj,"fd","I" );
	int status;
	ioctl(fd, LPGETSTATUS, &status);
	return( status & LP_BUSY ? JNI_TRUE : JNI_FALSE );
#endif
#if defined(__FreeBSD__)
	printf("ParallelImp.c LPGETSTATUS not defined\n");
	/*
	return( status & EBUSY ? JNI_TRUE : JNI_FALSE );
	*/
#endif
	return( JNI_FALSE );
}


/*----------------------------------------------------------
LPRPort.Initialize

   accept:      none
   perform:     Initialize the native library
   return:      none
   comments:    lots of reading to do here. FIXME
----------------------------------------------------------*/
JNIEXPORT void JNICALL LPRPort(Initialize)( JNIEnv *env,
	jclass jclazz )
{
	/* This bit of code checks to see if there is a signal handler installed
	   for SIGIO, and installs SIG_IGN if there is not.  This is necessary
		for the native threads jdk, but we don't want to do it with green
		threads, because it slows things down.  Go figure. */
#if !defined(WIN32)
	struct sigaction handler;
	sigaction( SIGIO, NULL, &handler );
	if( !handler.sa_handler ) signal( SIGIO, SIG_IGN );
#endif /* !WIN32 */
}


/*----------------------------------------------------------
LPRPort.open

   accept:      The device to open.  ie "/dev/lp0"
   perform:     open the device and return the filedescriptor
   return:      fd
   exceptions:  PortInUseException
   comments:    Very often people complain about not being able to get past
                this function and it turns out to be permissions on the
                device file or bios has the device disabled.
		FIXME  Lock Files?
----------------------------------------------------------*/
JNIEXPORT jint JNICALL LPRPort(open)( JNIEnv *env, jobject jobj,
	jstring jstr )
{
	/*struct termios ttyset;*/
	const char *filename = (*env)->GetStringUTFChars( env, jstr, 0 );
#ifdef WIN32
	int fd = (int)CreateFile( filename, GENERIC_READ | GENERIC_WRITE, 0, 0, OPEN_EXISTING, 0, 0 );
#else
	int fd = open( filename, O_RDWR | O_NONBLOCK );
#endif
	(*env)->ReleaseStringUTFChars( env, jstr, NULL );
	if( fd < 0 ) goto fail;
	return (jint)fd;

fail:
	throw_java_exception_system_msg( env, PORT_IN_USE_EXCEPTION, "open" );
	return -1;
}


/*----------------------------------------------------------
LPRPort.nativeClose

   accept:      none
   perform:     get the fd from the java end and close it
   return:      none
   exceptions:  none
----------------------------------------------------------*/
JNIEXPORT void JNICALL LPRPort(nativeClose)( JNIEnv *env,
	jobject jobj )
{
	int fd = get_java_var( env, jobj,"fd","I" );

#ifdef WIN32
	CloseHandle( (HANDLE)fd );
#else
	close( fd );
#endif
	return;
}

/*----------------------------------------------------------
LPRPort.writeByte

   accept:      byte to write (passed as int)
   perform:     write a single byte to the port
   return:      none
   exceptions:  IOException
----------------------------------------------------------*/
JNIEXPORT void JNICALL LPRPort(writeByte)( JNIEnv *env,
	jobject jobj, jint ji )
{
	unsigned char byte = (unsigned char)ji;
	int fd = get_java_var( env, jobj,"fd","I" );

#ifdef WIN32
	DWORD countWritten = 0; /* Fixme, should be a loop until all is written */
	if( WriteFile( (HANDLE)fd, &byte, sizeof( unsigned char ), &countWritten, NULL ) != 0 && countWritten == sizeof( unsigned char ) ) return;
#else
	if( write( fd, &byte, sizeof( unsigned char ) ) >= 0 ) return;
#endif
	throw_java_exception_system_msg( env, IO_EXCEPTION, "writeByte" );
}


/*----------------------------------------------------------
LPRPort.writeArray

   accept:      jbarray: bytes used for writing
                offset: offset in array to start writing
                count: Number of bytes to write
   perform:     write length bytes of jbarray
   return:      none
   exceptions:  IOException
----------------------------------------------------------*/
JNIEXPORT void JNICALL LPRPort(writeArray)( JNIEnv *env,
	jobject jobj, jbyteArray jbarray, jint offset, jint count )
{
#ifdef WIN32
	DWORD countWritten; /* Fixme, should be a loop until all is written */
	COMMTIMEOUTS timeouts;
	int errorCount = 0;
#endif
	int fd = get_java_var( env, jobj,"fd","I" );
	jbyte *body = (*env)->GetByteArrayElements( env, jbarray, 0 );
	unsigned char *bytes = (unsigned char *)malloc( count );
	int i;
	for( i = 0; i < count; i++ ) bytes[ i ] = body[ i + offset ];
	(*env)->ReleaseByteArrayElements( env, jbarray, body, 0 );
#ifdef WIN32
	/*
	we set a timeout because all calls are sequentiell (also with asynchron)
	 this means that the default timeout of unlimited
	blocks all calls (also close and status request) if the device is down
	*/
	GetCommTimeouts( (HANDLE)fd, &timeouts );
	timeouts.WriteTotalTimeoutMultiplier = 0;
	timeouts.WriteTotalTimeoutConstant = 2000; /*  2000 is the min value for the default Windows NT and Windows 2000 driver */
	SetCommTimeouts( (HANDLE)fd, &timeouts );
	while( count > 0 ){
		if(!WriteFile( (HANDLE)fd, bytes, count, &countWritten, NULL ) && countWritten == 0){
			/*
			this are 20 * 2 seconds, in this time a printer (or other parallel device)
			should solv all problems like buffer full, etc
			*/
			if(errorCount++ < 20){
				Sleep( 20 ); /* make a small pause to execute all other requests in all other threads */
				continue;
			}
			throw_java_exception_system_msg( env, IO_EXCEPTION, "writeArray" );
			break;
		}
		errorCount = 0;
		bytes += countWritten;
		count -= countWritten;
	}
#else
	if( write( fd, bytes, count ) < 0 )
		throw_java_exception_system_msg( env, IO_EXCEPTION, "writeArray" );
#endif
	free( bytes );
}


/*----------------------------------------------------------
read_byte_array

   accept:      int                fd   file descriptor to read from
                unsigned char *buffer   buffer to read data into
                int            length   number of bytes to read
                int         threshold   receive threshold
                int           timeout   milliseconds to wait before returning
   perform:     read bytes from the port into a buffer
   return:      status of read
                -1 fail (IOException)
                 0 timeout
                >0 number of bytes read
   comments:    According to the Communications API spec, a receive threshold
                of 1 is the same as having the threshold disabled.
----------------------------------------------------------*/
int read_byte_array( int fd, unsigned char *buffer, int length, int threshold,
	int timeout )
{
	int ret, left, bytes = 0;
	fd_set rfds;
	struct timeval sleep;

	FD_ZERO( &rfds );
	FD_SET( fd, &rfds );
	sleep.tv_sec = timeout / 1000;
	sleep.tv_usec = 1000 * ( timeout % 1000 );
	left = length;

	while( bytes < length && bytes < threshold )
	{
		if( timeout > 0 )
		{
         /* FIXME: In Linux, select updates the timeout automatically, so
            other OSes will need to update it manually if they want to have
            the same behavior.  For those OSes, timeouts will occur after no
            data AT ALL is received for the timeout duration.  No big deal. */
			do {
				ret=select( fd + 1, &rfds, NULL, NULL, &sleep );
			} while(ret < 0 && errno ==EINTR);
			if( ret == 0 ) break;
			if( ret < 0 ) return -1;
		}
#if defined(WIN32)
		if(!ReadFile( (HANDLE)fd, buffer + bytes, left, (DWORD *)&ret, NULL )){
			YACK();
			ret = -1;
		}
#else
		ret = read( fd, buffer + bytes, left );
#endif
		if( ret == 0 ) break;
		if( ret < 0 ) return -1;
		bytes += ret;
		left -= ret;
	}
	return bytes;
}


/*----------------------------------------------------------
LPRPort.readByte

   accept:      none
   perform:     Read a single byte from the port
   return:      The byte read
   exceptions:  IOException
----------------------------------------------------------*/
JNIEXPORT jint JNICALL LPRPort(readByte)( JNIEnv *env,
	jobject jobj )
{
	int bytes, fd, timeout;
	unsigned char buffer[ 1 ];

	fd = get_java_var( env, jobj,"fd","I" );
	timeout = get_java_var( env, jobj, "timeout","I");

	bytes = read_byte_array( fd, buffer, 1, 1, timeout );
	if( bytes < 0 )
	{
		throw_java_exception_system_msg( env, IO_EXCEPTION, "readByte" );

		return -1;
	}
	return (bytes ? (jint)buffer[ 0 ] : -1);
}


/*----------------------------------------------------------
LPRPort.readArray

   accept:       offset (bytes to skip) and Length (bytes to read)
   perform:      read bytes from the port into a byte array
   return:       bytes read on success
                 0 on read timeout
   exceptions:   IOException
   comments:     throws IOException if asked to read > SSIZE_MAX
----------------------------------------------------------*/
JNIEXPORT jint JNICALL LPRPort(readArray)( JNIEnv *env,
	jobject jobj, jbyteArray jbarray, jint offset, jint length )
{
	int bytes, i, fd, threshold, timeout;
	jbyte *body;
	unsigned char *buffer;
	fd = get_java_var( env, jobj,"fd","I" );
	threshold = get_java_var( env, jobj,"threshold","I" );
	timeout = get_java_var( env, jobj,"threshold","I" );

	if( (size_t) length < 1 || (size_t) length > SSIZE_MAX )
	{
		throw_java_exception( env, IO_EXCEPTION, "readArray",
			"Invalid length" );
		return -1;
	}

	buffer = (unsigned char *)malloc( sizeof( unsigned char ) * length );
	if( buffer == 0 )
	{
		throw_java_exception( env, IO_EXCEPTION, "readArray",
			"Unable to allocate buffer" );

		return -1;
	}

	bytes = read_byte_array( fd, buffer, length, threshold, timeout );
	if( bytes < 0 )
	{
		free( buffer );
		throw_java_exception_system_msg( env, IO_EXCEPTION, "readArray" );

		return -1;
	}

	body = (*env)->GetByteArrayElements( env, jbarray, 0 );
	for( i = 0; i < bytes; i++ ) body[ i + offset ] = buffer[ i ];
	(*env)->ReleaseByteArrayElements( env, jbarray, body, 0 );
	free( buffer );
	return (bytes ? bytes : -1);
}


/*----------------------------------------------------------
LPRPort.nativeavailable

   accept:      none
   perform:     find out the number of bytes available for reading
   return:      available bytes
                -1 on error
   exceptions:  none
----------------------------------------------------------*/
JNIEXPORT jint JNICALL LPRPort(nativeavailable)( JNIEnv *env,
	jobject jobj )
{
	int fd = get_java_var( env, jobj,"fd","I" );
	int result;
/*
	char message[80];

	ENTER( "LPRPort:nativeavailable" );
*/
/*
    On SCO OpenServer FIONREAD always fails for serial devices,
    so try ioctl FIORDCHK instead; will only tell us whether
    bytes are available, not how many, but better than nothing.

    This turns out to be true on Solaris also.  taj.
*/
#ifdef FIORDCHK  /* __unixware__ __sun__ probably others */
	result = ioctl(fd, FIORDCHK, 0);
#else
	if( ioctl( fd, FIONREAD, &result ) < 0 )
	{
		goto fail;
	}
#endif /* FIORDCHK */
	if (result == -1) {
		goto fail;
	}
	if( result )
	{
/*		sprintf(message, "    nativeavailable: FIORDCHK result %d, \
				errno %d\n", result , result == -1 ? errno : 0);
		report( message );
*/
	}
/*
	LEAVE( "LPRPort:nativeavailable" );
*/
	return (jint)result;
fail:
/*
	report("LPRPort:nativeavailable:  ioctl() failed\n");
	LEAVE( "LPRPort:nativeavailable" );
*/
	throw_java_exception_system_msg( env, IO_EXCEPTION, "nativeavailable" );
	return (jint)result;
}


/*----------------------------------------------------------
LPRPort.setHWFC

   accept:      state (JNI_FALSE 0, JNI_TRUE 1)
   perform:     set hardware flow control to state
   return:      none
   exceptions:  IOException
----------------------------------------------------------*/
JNIEXPORT void JNICALL LPRPort(setHWFC)( JNIEnv *env,
	jobject jobj, jboolean state )
{
	/* int fd = get_java_var( env, jobj,"fd","I" ); */
	return;
}

/*----------------------------------------------------------
LPRPort.eventLoop

   accept:      none
   perform:     periodically check for ParallelPortEvents
   return:      none
   exceptions:  none
   comments:    lots of work needed here.  Yes its a mess.
struct lp_stats
{
	unsigned long chars;
	unsigned long sleeps;
	unsigned int maxrun;
	unsigned int maxwait;
	unsigned int meanwait;
	unsigned int mdev;
};

----------------------------------------------------------*/
JNIEXPORT void JNICALL LPRPort(eventLoop)( JNIEnv *env,
	jobject jobj )
{
	int fd, ret;
	unsigned int pflags = 0;
	fd_set rfds;
	struct timeval sleep;
	jboolean interrupted = 0;

	fd = get_java_var( env, jobj,"fd","I" );

	interrupted = is_interrupted(env, jobj);

	FD_ZERO( &rfds );
	while( !interrupted )
	{
		FD_SET( fd, &rfds );
		/* Check every 1 second, or on receive data */
		sleep.tv_sec = 1;
		sleep.tv_usec = 0;
		do {
			ret = select( fd + 1, &rfds, NULL, NULL, &sleep );
		}
		while (ret < 0 && errno == EINTR);
		if( ret < 0 ) break;
		interrupted = is_interrupted(env, jobj);
		if(interrupted) {
			return;
		}

#if defined(LPGETSTATUS)
		ioctl( fd, LPGETSTATUS, &pflags );
#elif defined(WIN32)
		pflags = getWin32ParallelStatusFlags(fd);
#else
	/*  FIXME??  */
	printf("ParallelImp.c LPGETSTATUS is undefined!\n");
#endif

/*
			PAR_EV_BUFFER:
			PAR_EV_ERROR:
*/

#if defined(PARALLEL_BUSY)
		if (pflags & PARALLEL_BUSY)
			send_event( env, jobj, PAR_EV_ERROR, JNI_TRUE );
#elif defined(LP_BUSY)
		if (pflags&LP_BUSY)    /* inverted input, active high */
			send_event( env, jobj, PAR_EV_ERROR, JNI_TRUE );
#elif defined(EBUSY)
		if (pflags&EBUSY)    /* inverted input, active high */
			send_event( env, jobj, PAR_EV_ERROR, JNI_TRUE );
#endif /* EBUSY LP_BUSY */
/*  FIXME  this has moved into the ifdef __kernel__?  Need to get the
           posix documentation on this.
		if (pflags&LP_ACK)
			send_event( env, jobj, PAR_EV_ERROR, JNI_TRUE );
*/


#if defined (__linux__)
		/* unchanged input, active low */
		if (pflags&LP_NOPA)   /* unchanged input, active high */
			send_event( env, jobj, PAR_EV_ERROR, 1 );
		if (pflags&LP_SELEC)  /* unchanged input, active high */
			send_event( env, jobj, PAR_EV_ERROR, 1 );
		if (pflags&LP_ERR)  /* unchanged input, active low */
			send_event( env, jobj, PAR_EV_ERROR, 1 );
#else
	/*  FIXME??  */
	printf("ParallelImp.c LPGETSTATUS is undefined!\n");
#endif
		usleep(1000);
	}
	return;
}

JNIEXPORT void JNICALL LPRPort(setInputBufferSize)(JNIEnv *env,
	jobject jobj, jint size )
{
#ifdef DEBUG
	printf("setInputBufferSize is not implemented\n");
#endif
}
JNIEXPORT jint JNICALL LPRPort(getInputBufferSize)(JNIEnv *env,
	jobject jobj)
{
#ifdef DEBUG
	printf("getInputBufferSize is not implemented\n");
#endif
	return(1);
}
JNIEXPORT void JNICALL LPRPort(setOutputBufferSize)(JNIEnv *env,
	jobject jobj, jint size )
{
#ifdef DEBUG
	printf("setOutputBufferSize is not implemented\n");
#endif
}
JNIEXPORT jint JNICALL LPRPort(getOutputBufferSize)(JNIEnv *env,
	jobject jobj)
{
#ifdef DEBUG
	printf("getOutputBufferSize is not implemented\n");
#endif
	return(1);
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
#define MSG_SIZE 128
	char buf[ 128 ];
	jclass clazz = (*env)->FindClass( env, exc );
	if( !clazz )
	{
		(*env)->ExceptionDescribe( env );
		(*env)->ExceptionClear( env );
		return;
	}
	/* reduce the message size if it to large for the message buffer */
	if(MSG_SIZE < (strlen( msg ) + strlen(foo) + 5)){
		msg[ MSG_SIZE - strlen(foo) - 5] = 0;
	}
#if defined(_GNU_SOURCE)
	snprintf( buf, 60, "%s in %s", msg, foo );
#else
	sprintf( buf,"%s in %s", msg, foo );
#endif /* _GNU_SOURCE */
	(*env)->ThrowNew( env, clazz, buf );
/* ct7 * Added DeleteLocalRef */
	(*env)->DeleteLocalRef( env, clazz );
}

void throw_java_exception_system_msg( JNIEnv *env, char *exc, char *foo )
{
#ifdef WIN32
	char *allocTextBuf;
	FormatMessage (
		FORMAT_MESSAGE_ALLOCATE_BUFFER |
		FORMAT_MESSAGE_FROM_SYSTEM,
		NULL,
		GetLastError(),
		MAKELANGID(LANG_NEUTRAL, SUBLANG_DEFAULT),
		(LPSTR)&allocTextBuf,
		16,
		NULL );
	throw_java_exception( env, exc, foo, allocTextBuf );
	LocalFree(allocTextBuf);
#else
	throw_java_exception( env, exc, foo, strerror( errno ) );
#endif
}
/*----------------------------------------------------------
 report_error

   accept:      string to send to report as an error
   perform:     send the string to stderr or however it needs to be reported.
   return:      none
   exceptions:  none
   comments:
----------------------------------------------------------*/
void report_error(char *msg)
{
	fprintf(stderr, "%s", msg);
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
#ifdef DEBUG
        fprintf(stderr, "%s", msg);
#endif /* DEBUG */
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
	jmethodID foo;
	jclass jclazz;
	int result;

	(*env)->ExceptionClear(env);

	jclazz = (*env)->GetObjectClass( env, jobj );
	if(jclazz == NULL) return JNI_TRUE;

	foo = (*env)->GetMethodID( env, jclazz, "checkMonitorThread", "()Z");
	if(foo == NULL) return JNI_TRUE;

	result = (*env)->CallBooleanMethod( env, jobj, foo );

#ifdef DEBUG
	if((*env)->ExceptionOccurred(env)) {
		report ("an error occured calling sendEvent()\n");
		(*env)->ExceptionDescribe(env);
		(*env)->ExceptionClear(env);
	}
#endif /* DEBUG */
	(*env)->DeleteLocalRef( env, jclazz );

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
	int result;
	jmethodID foo;
	jclass jclazz = (*env)->GetObjectClass( env, jobj );

	if(jclazz == NULL) return JNI_TRUE;
	foo = (*env)->GetMethodID( env, jclazz, "sendEvent", "(IZ)Z" );

	(*env)->ExceptionClear(env);

	result = (*env)->CallBooleanMethod( env, jobj, foo, type,
		flag > 0 ? JNI_TRUE : JNI_FALSE );

#ifdef DEBUG
	if((*env)->ExceptionOccurred(env)) {
		report ("an error occured calling sendEvent()\n");
		(*env)->ExceptionDescribe(env);
		(*env)->ExceptionClear(env);
	}
#endif /* DEBUG */
	(*env)->DeleteLocalRef( env, jclazz );
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
	int result = 0;
	jclass jclazz = (*env)->GetObjectClass( env, jobj );
	jfieldID jfd = (*env)->GetFieldID( env, jclazz, id, type );
	if( !jfd )
	{
		(*env)->ExceptionDescribe( env );
		(*env)->ExceptionClear( env );
		return result;
	}
	result = (int)( (*env)->GetIntField( env, jobj, jfd ) );
/* ct7 & gel * Added DeleteLocalRef */
	(*env)->DeleteLocalRef( env, jclazz );
	return result;
}
