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
#include "gnu/io/RXTXPort.h" /* dima */
#include "gnu/io/RXTXCommDriver.h" /* dima */
#include "gnu/io/PortInUseException.h"
#if defined(__MWERKS__) /* dima */
#else  /* dima */
#include "config.h"
#endif /* dima */
#ifndef __LCC__
#   include <unistd.h>
#else /* windows lcc compiler for fd_set. probably wrong */
#   include<winsock.h>
#endif /* __LCC__ */
#include <time.h>
#include <stdio.h>
#include <string.h>
#include <stdlib.h>
#include <errno.h>
#include <limits.h>
#include <sys/stat.h>
#ifndef WIN32
#include <sys/ioctl.h>
#include <sys/param.h>
#include <sys/utsname.h>
#include <pthread.h>
#else
#	include <win32termios.h>
/*  FIXME  returns 0 in all cases on win32
#define S_ISCHR(m)	(((m)&S_IFMT) == S_IFCHR)
*/
#	if !defined(S_ISCHR)
#		define S_ISCHR(m) (1)
#	endif /* S_ISCHR(m) */
#endif /* WIN32 */
#ifdef HAVE_ASM_TERMBITS_H
//#	include <asm/termbits.h>
#endif /* HAVE_ASM_TERMBITS_H */
#ifdef HAVE_TERMIOS_H
#	include <termios.h>
#endif /* HAVE_TERMIOS_H */
#ifdef HAVE_SIGNAL_H
#   include <signal.h>
#endif /* HAVE_SIGNAL_H */
#ifdef HAVE_SYS_SIGNAL_H
#   include <sys/signal.h>
#endif /* HAVE_SYS_SIGNAL_H */
#include <sys/types.h>
#ifdef HAVE_SYS_TIME_H
#   include <sys/time.h>
#endif /* HAVE_SYS_TIME_H */
#   include <fcntl.h>
#ifdef HAVE_SYS_FCNTL_H
#   include <sys/fcntl.h>
#endif /* HAVE_SYS_FCNTL_H */
#ifdef HAVE_SYS_FILE_H
#   include <sys/file.h>
#endif /* HAVE_SYS_FILE_H */
#ifdef LFS  /* File Lock Server */
#	include <sys/socket.h>
#	include <netinet/in.h>
#	include <arpa/inet.h>
#endif /* FLS */
#if defined(__linux__)
#	include <linux/types.h> /* fix for linux-2.3.4? kernels */
#	include <linux/serial.h>
#	include <linux/version.h>
#endif /* __linux__ */
#if defined(__sun__)
#	include <sys/filio.h>
#	include <sys/mkdev.h>
#endif /* __sun__ */
#if defined(__hpux__)
#	include <sys/modem.h>
#endif /* __hpux__ */
/* FIXME -- new file */
#if defined(__APPLE__)
#	include <CoreFoundation/CoreFoundation.h>
#	include <IOKit/IOKitLib.h>
#	include <IOKit/serial/IOSerialKeys.h>
#	include <IOKit/IOBSD.h>
#endif /* __APPLE__ */
#ifdef __unixware__
#	include  <sys/filio.h>
#endif /* __unixware__ */
#ifdef HAVE_PWD_H
#include	<pwd.h>
#endif /* HAVE_PWD_H */
#ifdef HAVE_GRP_H
#include 	<grp.h>
#endif /* HAVE_GRP_H */
#include <math.h>

extern int errno;
#include "SerialImp.h"
#include <gcj/cni.h>
#include <java/lang/System.h>
#include <java/io/PrintStream.h>
#include <java/lang/String.h>
#include <java/io/IOException.h>
#include <gnu/io/UnsupportedCommOperationException.h>
#include <java/lang/ArrayIndexOutOfBoundsException.h>
//#define TRENT_IS_HERE_DEBUGGING_ENUMERATION

struct preopened *preopened_port = NULL;

#if defined(__sun__) || defined(__hpux__)
/*----------------------------------------------------------
cfmakeraw

   accept:      termios to be set to raw
   perform:     initializes the termios structure.
   return:      int 0 on success
   exceptions:  none
   comments:    this is how linux cfmakeraw works.
		termios(3) manpage
----------------------------------------------------------*/

int cfmakeraw ( struct termios *term )
{
	ENTER( "cfmakeraw" );
	term->c_iflag &= ~(IGNBRK|BRKINT|PARMRK|ISTRIP|INLCR|IGNCR|ICRNL|IXON);
	term->c_oflag &= ~OPOST;
	term->c_lflag &= ~(ECHO|ECHONL|ICANON|ISIG|IEXTEN);
	term->c_cflag &= ~(CSIZE|PARENB);
	term->c_cflag |= CS8;
	LEAVE( "cfmakeraw" );
	return( 0 );
}
#endif /* __sun__  || __hpux__ */

#ifdef DEBUG_TIMING
struct timeval snow, enow, seloop, eeloop;
#define report_time_eventLoop( ) { \
	if ( seloop.tv_sec == eeloop.tv_sec && seloop.tv_usec == eeloop.tv_usec ) \
	{ \
		gettimeofday(&eeloop, NULL); \
		seloop.tv_sec = eeloop.tv_sec; \
		seloop.tv_usec = eeloop.tv_usec; \
		printf("%8i sec : %8i usec\n", eeloop.tv_sec - seloop.tv_sec, eeloop.tv_usec - seloop.tv_usec); \
	} \
}
#define report_time( ) \
{ \
	struct timeval now; \
	gettimeofday(&now, NULL); \
	mexPrintf("%8s : %5i : %8i sec : %8i usec\n", __TIME__, __LINE__, now.tv_sec, now.tv_usec); \
}
#define report_time_start( ) \
{ \
	gettimeofday(&snow, NULL); \
	mexPrintf("%8s : %5i : %8i sec : %8i usec", __TIME__, __LINE__, snow.tv_sec, snow.tv_usec); \
}
#define report_time_end( ) \
{ \
	gettimeofday(&enow, NULL); \
	mexPrintf("%8i sec : %8i usec\n", enow.tv_sec - snow.tv_sec, enow.tv_sec - snow.tv_sec?snow.tv_usec-enow.tv_usec:enow.tv_usec - snow.tv_usec); \
}
#else
#define report_time_eventLoop( ){};
#define report_time( ) {}
#define report_time_start( ) {}
#define report_time_end( ) {}
#endif /* DEBUG_TIMING */


struct event_info_struct *master_index = NULL;

/*----------------------------------------------------------
RXTXPort.Initialize

   accept:      none
   perform:     Initialize the native library
   return:      none
   exceptions:  none
   comments:    Basically this just causes rxtx to ignore signals.  signal
		handlers where tried but the VM (circa 1.1) did not like it.

		It also allows for some sanity checks on linux boxes if DEBUG
		is enabled.
----------------------------------------------------------*/
void gnu::io::RXTXPort::Initialize(
)
{
#if defined DEBUG && defined(__linux__)
	struct utsname name;
	char message[80];
#endif /* DEBUG && __linux__ */
	/* This bit of code checks to see if there is a signal handler installed
	   for SIGIO, and installs SIG_IGN if there is not.  This is necessary
	   for the native threads jdk, but we don't want to do it with green
	   threads, because it slows things down.  Go figure. */

	/* POSIX signal handling functions */
#if !defined(WIN32)
	struct sigaction old_action;
	sigaction(SIGIO, NULL, &old_action);
	/* green threads already has handler, no touch */
	if (old_action.sa_handler == NULL) {
		/* no handler when using native threads, set to ignore */
		struct sigaction new_action;
		sigset_t block_mask;
		sigemptyset(&block_mask);
		new_action.sa_handler = SIG_IGN;
#ifdef SA_RESTART
		new_action.sa_flags = SA_RESTART;
#endif /* SA_RESTART */
		new_action.sa_mask = block_mask;
		sigaction(SIGIO, &new_action, NULL);
	} 
#endif /* !WIN32 */
	ENTER( "RXTXPort:Initialize" );
#ifdef PRERELEASE
	/*  this is just for avoiding confusion while testing new libraries */
#ifdef DEBUG_MW
	mexPrintf("RXTX Prerelease for testing  Tue Feb 19 18:00:27 EST 2002\n");
#else
	printf("RXTX Prerelease for testing  Thu Feb 21 19:31:38\n");
#endif /* DEBUG_MW */
#endif /* PRERELEASE */
#if DEBUG_TIMING
	gettimeofday(&seloop, NULL);
#endif /* DEBUG_TIMING */
#if defined(DEBUG) && defined(__linux__)
	/* Lets let people who upgraded kernels know they may have problems */
	if (uname (&name) == -1)
	{
		report( "RXTX WARNING:  cannot get system name\n" );
		LEAVE( "RXTXPort:Initialize" );
		return;
	}
	if(strcmp(name.release,UTS_RELEASE)!=0)
	{
		sprintf( message, LINUX_KERNEL_VERSION_ERROR, UTS_RELEASE,
			name.release );
		report( message );
		getchar();
	}
	LEAVE( "RXTXPort:Initialize" );
#endif /* DEBUG && __linux__ */
}

/*----------------------------------------------------------
RXTXPort.find_preopened_ports
   accept:      The device to find if preopened.  ie "/dev/ttyS0"
   perform:     find the filedescriptor if preopened
   return:      fd
   exceptions:  none
   comments:    see
			RXTXPort.nativeStaticSetDSR
			RXTXPort.nativeStaticSetDTR
			RXTXPort.nativeStaticSetRTS
			RXTXPort.nativeStaticSetSerialPortParams
		This is used so people can setDTR low before calling the
		Java open().
----------------------------------------------------------*/
int find_preopened_ports( const char *filename )
{
	int fd;
	struct preopened *p = preopened_port;

	if( !p )
	{
		return(0);
	}
	for(;;)
	{
		if( !strcmp( p->filename, filename) )
		{
			fd = p->fd;
			if( p->prev && p->next )
			{
				p->prev->next = p->next;
				p->next->prev = p->prev;
			}
			else if ( p->prev )
			{
				p->prev->next = NULL;
			}
			else if ( p->next )
			{
				p->next->prev = NULL;
			}
			else
			{
				free( p );
				preopened_port = NULL;
				return( fd );
			}
			free( p );
			return( fd );
		}
		if( p->next )
		{
			p = p->next;
		}
		else
		{
			return(0);
		}
	}
}

/*----------------------------------------------------------
configure_port

   accept:      env, file descriptor
   perform:     set the termios struct to sane settings and
   return:      0 on success
   exceptions:  IOExcepiton
   comments:    Very often people complain about not being able to get past
                this function and it turns out to be permissions on the
                device file or bios has the device disabled.
----------------------------------------------------------*/

int configure_port( int fd )
{
	struct termios ttyset;

	if( fd < 0 ) goto fail;

	if( tcgetattr( fd, &ttyset ) < 0 ) goto fail;
	ttyset.c_iflag = INPCK;
	ttyset.c_lflag = 0;
	ttyset.c_oflag = 0;
	ttyset.c_cflag = CREAD | CS8 | CLOCAL;
	ttyset.c_cc[ VMIN ] = 0;
	ttyset.c_cc[ VTIME ] = 0;

#ifdef __FreeBSD__
	if( cfsetspeed( &ttyset, B9600 ) < 0 ) goto fail;
#else
	if( cfsetispeed( &ttyset, B9600 ) < 0 ) goto fail;
	if( cfsetospeed( &ttyset, B9600 ) < 0 ) goto fail;
#endif
	if( tcsetattr( fd, TCSANOW, &ttyset ) < 0 ) goto fail;
#ifndef WIN32
	fcntl( fd, F_SETOWN, getpid() );
#endif /* WIN32 */
#ifdef FASYNC
	fcntl( fd, F_SETFL, FASYNC );
#endif /* FASYNC */

	return 0;

fail:
	return 1;
}


/*----------------------------------------------------------
get_java_baudrate

   accept:      the native speed setting
   perform:     translate the native speed to a Java speed
   return:      the Java speed
   exceptions:  none
   comments:    This is used by open() (indirectly) and
		nativeStaticGetBaudRate()
----------------------------------------------------------*/
int get_java_baudrate( int native_speed )
{
	switch( native_speed )
	{
		case B0:     return 0;
		case B50:    return 50;
		case B75:    return 75;
		case B110:   return 110;
		case B134:   return 134;
		case B150:   return 150;
		case B200:   return 200;
		case B300:   return 300;
		case B600:   return 600;
		case B1200:  return 1200;
		case B1800:  return 1800;
		case B2400:  return 2400;
		case B4800:  return 4800;
		case B9600:  return 9600;
		case B19200: return 19200;
		case B38400: return 38400;
		case B57600: return 57600;
		default: return -1;
	}
}

/*----------------------------------------------------------
set_java_vars

   accept:      fd of the preopened device
   perform:     Now that the object is instatiated, set the Java variables
		to the preopened states.
   return:      none
   exceptions:  none
   comments:    preopened refers to the fact that the serial port has
		been configured before the Java open() has been called.
----------------------------------------------------------*/
		
void set_java_vars( gnu::io::RXTXPort *p,
	int fd
)
{
	struct termios ttyset;
	int databits = -1;
	int jparity = -1;
	int stop_bits = STOPBITS_1_5;
	int baudrate;

	if( tcgetattr( fd, &ttyset ) < 0 )
	{
		report( "Cannot Get Serial Port Settings\n" );
		return;
	}

	
	switch( ttyset.c_cflag&CSIZE ) {
		case CS5:  databits = JDATABITS_5; break;
		case CS6:  databits = JDATABITS_6; break;
		case CS7:  databits = JDATABITS_7; break;
		case CS8:  databits = JDATABITS_8; break;
	}
#ifdef CMSPAR
	switch( ttyset.c_cflag&(PARENB|PARODD|CMSPAR ) ) {
#else
	switch( ttyset.c_cflag&(PARENB|PARODD) ) {
#endif /* CMSPAR */
		case 0: jparity = JPARITY_NONE; break;
		case PARENB: jparity = JPARITY_EVEN; break;
		case PARENB | PARODD: jparity = JPARITY_ODD; break;
#ifdef CMSPAR
		case PARENB | PARODD | CMSPAR: jparity = JPARITY_MARK; break;
		case PARENB | CMSPAR: jparity = JPARITY_SPACE; break;
#endif /* CMSPAR */
	}
        switch( ttyset.c_cflag&(CSTOPB) ) {
                case 0: stop_bits = STOPBITS_1; break;
                case CSTOPB:  stop_bits = STOPBITS_2; break;
        }
/*
dima writes:

Trent, here is something I found with google:
(freebsd list freebsd-current@freebsd.org)

Andrzej Bialecki <abial@korin.warman.org.pl> asked:
I tried to compile a piece of software, probably for Linux, and I noticed
that we don't define CBAUD constant. I'm not sure, but I think POSIX
defines and uses it. Should(n't) we?

Bruce Evans <bde@zeta.org.au> answered:
CBAUD is for SYSV compatibility.  It is considerably inferior to POSIX's
cf{get,set}{i,o}speed and shouldn't be provided or used.

*/
#if defined(CBAUD)/* dima */
    	baudrate = ttyset.c_cflag&CBAUD;
#else
    	baudrate = cfgetispeed(&ttyset);
#endif
	p->speed = ( jint ) get_java_baudrate(baudrate) ;
	p->dataBits = ( jint ) databits ;
	p->stopBits = ( jint ) stop_bits ;
	p->parity = ( jint ) jparity ;
}
/*----------------------------------------------------------
RXTXPort.open

   accept:      The device to open.  ie "/dev/ttyS0"
   perform:     open the device, set the termios struct to sane settings and
                return the filedescriptor
   return:      fd
   exceptions:  IOExcepiton
   comments:    Very often people complain about not being able to get past
                this function and it turns out to be permissions on the
                device file or bios has the device disabled.
----------------------------------------------------------*/
jint gnu::io::RXTXPort::open(
	java::lang::String *jstr
)
{
	int fd =0, i;
	int  pid = -1;
	char message[80];
	report_time_start( );
	char filename[80];

	for( i=0;i<jstr->length();i++)
		filename[i] = jstr->charAt(i);
	filename[i] = '\0';

#ifndef WIN32
	pid = getpid();
#endif /* WIN32 */

	this->pid = pid;
	/* 
		LOCK is one of three functions defined in SerialImp.h

			uucp_lock		Solaris
			fhs_lock		Linux
			system_does_not_lock	Win32
	*/
			
	ENTER( "RXTXPort:open" );
	if ( LOCK( filename, pid ) )
	{
		sprintf( message, "open: locking has failed for %s\n",
			filename );
		report( message );
		goto fail;
	}
	else
	{
		sprintf( message, "open: locking worked for %s\n", filename );
		report( message );
	}
	/* This is used so DTR can remain low on 'open()' */
	if( fd )
	{
		return (jint)fd;
	}

	do {
		fd=::OPEN (filename, O_RDWR | O_NOCTTY | O_NONBLOCK );
	}  while (fd < 0 && errno==EINTR);

	if( configure_port( fd ) ) goto fail;
	sprintf( message, "open: fd returned is %i\n", fd );
	report( message );
	LEAVE( "RXTXPort:open" );
	report_time_end( );
	return (jint)fd;

fail:
	LEAVE( "RXTXPort:open" );
	snprintf(message, 79, "%s %s",strerror(errno)," in open");
	throw new gnu::io::PortInUseException( JvNewStringUTF( message ) );
	//throw new PortInUseException;
	return -1;
}

/*----------------------------------------------------------
RXTXPort.nativeClose

   accept:      none
   perform:     get the fd from the java end and close it
   return:      none
   exceptions:  none
----------------------------------------------------------*/
void gnu::io::RXTXPort::nativeClose(
	java::lang::String *jstr
)
{
	int result = -1, i;
	char filename[80];

	for( i=0;i<jstr->length();i++)
		filename[i] = jstr->charAt(i);
	filename[i] = '\0';

	report_time_start( );
#ifndef WIN32
#endif /* WIN32 */
	/* 
		UNLOCK is one of three functions defined in SerialImp.h

			uucp_unlock		Solaris
			fhs_unlock		Linux
			system_does_not_unlock	Win32
	*/
	ENTER( "RXTXPort:nativeClose" );
	if (this->fd > 0)
	{
		do {
			result=::CLOSE (this->fd);
		}  while ( result < 0 && errno == EINTR );
		UNLOCK( filename, this->pid );
	}
	LEAVE( "RXTXPort:nativeClose" );
	report_time_end( );
	return;
}

/*----------------------------------------------------------
 RXTXPort.set_port_params

   accept:     env, fd, speed, data bits, stop bits, parity
   perform:    set the serial port parameters
   return:     1 on error
   exceptions: UnsupportedCommOperationException
   comments:   There is a static method and an instance method that use this
		function.  The static method gets a fd first.  The instance
		method can get the fd from the object.

		see:  nativeSetSerialPortParams & nativeStaticSerialPortParams
----------------------------------------------------------*/
int set_port_params(
	int fd,
	int cspeed,
	int dataBits,
	int stopBits,
	int parity
)
{
	struct termios ttyset;
	int result = 0;
#if defined(TIOCGSERIAL)
	struct serial_struct sstruct;
#endif /* TIOCGSERIAL */

	if( tcgetattr( fd, &ttyset ) < 0 )
	{
		report( "set_port_params: Cannot Get Serial Port Settings\n" );
		return(1);
	}

	if( translate_data_bits( &(ttyset.c_cflag), dataBits ) )
	{
		report( "set_port_params: Invalid Data Bits Selected\n" );
		return(1);
	}

	if( translate_stop_bits( &(ttyset.c_cflag), stopBits ) )
	{
		report( "set_port_params: Invalid Stop Bits Selected\n" );
		return(1);
	}

	if( translate_parity( &(ttyset.c_cflag), parity ) )
	{
		report( "set_port_params: Invalid Parity Selected\n" );
		return(1);
	}

#ifdef __FreeBSD__
	if( cfsetspeed( &ttyset, cspeed ) < 0 )
	{
		report( "set_port_params: Cannot Set Speed\n" );
		return( 1 );
	}
#endif  /* __FreeBSD__ */
	if( !cspeed )
	{
		/* hang up the modem aka drop DTR  */
		/* Unix should handle this */
		/*
		mexPrintf("dropping DTR\n");
		printf("dropping DTR\n");
		*/
		ioctl( fd, TIOCMGET, &result );
		result &= ~TIOCM_DTR;
		ioctl( fd, TIOCMSET, &result );
	}
	if(     cfsetispeed( &ttyset, cspeed ) < 0 ||
		cfsetospeed( &ttyset, cspeed ) < 0 )
	{
		/*
		    Some people need to set the baud rate to ones not defined
		    in termios.h

		    This includes some baud rates which are supported by CommAPI
		    in Unix ( 14400, 28800, 128000, 256000 )

		    If the above fails, we assume this is not a defined
		    baud rate on Unix.  With Win32, It is assumed the kernel
		    will do this for us.

		    The baud_base and desired speed are used to
		    calculate a custom divisor.

		    On linux the setserial man page covers this.
		*/

#if defined(TIOCGSERIAL)
		sstruct.custom_divisor = ( sstruct.baud_base/cspeed );
		cspeed = B38400;
#endif /* TIOCGSERIAL */
		if(     cfsetispeed( &ttyset, cspeed ) < 0 ||
			cfsetospeed( &ttyset, cspeed ) < 0 )
		{
			/* OK, we tried everything */
			report( "nativeSetSerialPortParams: Cannot Set Speed\n" );
			return( 1 );
		}
#if defined(TIOCSSERIAL)
		/*  It is assumed Win32 does this for us */
		if (	sstruct.baud_base < 1 ||
			ioctl( fd, TIOCSSERIAL, &sstruct ) < 0 )
		{
			return( 1 );
		}
#endif /* TIOCSSERIAL */
	}

	if( tcsetattr( fd, TCSANOW, &ttyset ) < 0 )
	{
		report("tcsetattr returns nonzero value!\n");
		return( 1 );
	}
	return(0);
}

/*----------------------------------------------------------
 RXTXPort.nativeSetSerialPortParams

   accept:     speed, data bits, stop bits, parity
   perform:    set the serial port parameters
   return:     jboolean 1 on error
   exceptions: UnsupportedCommOperationException
----------------------------------------------------------*/
jboolean gnu::io::RXTXPort::nativeSetSerialPortParams(
	jint speed,
	jint dataBits,
	jint stopBits,
	jint parity
)
{
	int fd = (int) this->fd;
	int cspeed = translate_speed( speed );

	ENTER( "RXTXPort:nativeSetSerialPortParams" );
	report_time_start( );

	if (cspeed < 0 )
	{
		report(" invalid cspeed\n");
/*
    For some reason the native exceptions are not being caught.  Moving this
    to the Java side fixed the issue.  taj.
		throw_java_exception( env, UNSUPPORTED_COMM_OPERATION,
			"", "BaudRate could not be set to the specified value" );
*/
		return(1);
	}


	if( set_port_params( fd, cspeed, dataBits, stopBits, parity ) )
	{
		report("set_port_params failed\n");
		LEAVE( "RXTXPort:nativeSetSerialPortParams" );
/*
    For some reason the native exceptions are not being caught.  Moving this
    to the Java side fixed the issue.  taj.
		throw_java_exception( env, UNSUPPORTED_COMM_OPERATION,
			"nativeSetSerialPortParams", strerror( errno ) );
*/
		return(1);
	}

	LEAVE( "RXTXPort:nativeSetSerialPortParams" );
	report_time_end( );
	return(0);
}

/*----------------------------------------------------------
 translate_speed

   accept:     speed in bits-per-second
   perform:    convert bits-per-second to a speed_t constant
   return:     speed_t constant
   exceptions: returns -1 and the calling method throws the exception so
	       it may be caught in java.
   comments:   Only the lowest level code should know about
               the magic constants.
----------------------------------------------------------*/
int translate_speed(
	jint speed
)
{
	LEAVE( "RXTXPort:translate_speed" );
	switch( speed ) {
		case 0:		return B0;
		case 50:	return B50;
		case 75:	return B75;
		case 110:	return B110;
		case 134:	return B134;
		case 150:	return B150;
		case 200:	return B200;
		case 300:	return B300;
		case 600:	return B600;
		case 1200:	return B1200;
		case 1800:	return B1800;
		case 2400:	return B2400;
		case 4800:	return B4800;
		case 9600:	return B9600;
		case 19200:	return B19200;
		case 38400:	return B38400;
#ifdef B57600
		case 57600:	return B57600;
#endif /* B57600 */
#ifdef B115200
		case 115200:	return B115200;
#endif /* B115200 */
#ifdef B230400
		case 230400:	return B230400;
#endif /* B230400 */
#ifdef B460800
		case 460800:	return B460800;
#endif /* B460800 */
#ifdef B14400
		case 14400:	return B14400;
#endif /* B14400 */
#ifdef B28800
		case 28800:	return B28800;
#endif /* B28800 */
#ifdef B128000  /* dima */
		case 128000:	return B128000;
#endif  /* dima */
#ifdef B256000  /* dima */
		case 256000:	return B256000;
#endif  /* dima */
	}

	/* Handle custom speeds */
	if( speed >= 0 ) return speed;
	else
	{
		LEAVE( "RXTXPort:translate_speed: Error condition" );
		return -1;
	}
}

/*----------------------------------------------------------
 translate_data_bits

   accept:     gnu.io.SerialPort.DATABITS_* constant
   perform:    set proper termios c_cflag bits
   return:     1 on error
   exceptions: UnsupportedCommOperationException
----------------------------------------------------------*/
int translate_data_bits(
	tcflag_t *cflag,
	jint dataBits
)
{
	int temp = (*cflag) & ~CSIZE;

	ENTER( "translate_date_bits" );
	switch( dataBits ) {
		case JDATABITS_5:
			(*cflag) = temp | CS5;
			return 0;
		case JDATABITS_6:
			(*cflag) = temp | CS6;
			return 0;
		case JDATABITS_7:
			(*cflag) = temp | CS7;
			return 0;
		case JDATABITS_8:
			(*cflag) = temp | CS8;
			return 0;
	}

	LEAVE( "RXTXPort:translate_date_bits" );
/*
    For some reason the native exceptions are not being caught.  Moving this
    to the Java side fixed the issue.  taj.
	throw_java_exception( env, UNSUPPORTED_COMM_OPERATION,
		"", "databit value not supported" );
*/
	return 1;
}

/*----------------------------------------------------------
 translate_stop_bits

   accept:     gnu.io.SerialPort.STOPBITS_* constant
   perform:    set proper termios c_cflag bits
   return:     1 on error
   exceptions: UnsupportedCommOperationException
   comments:   If you specify 5 data bits and 2 stop bits, the port will
               allegedly use 1.5 stop bits.  Does anyone care?
----------------------------------------------------------*/
int translate_stop_bits(
	tcflag_t *cflag,
	jint stopBits
)
{
	ENTER( "translate_stop_bits" );
	switch( stopBits ) {
		case STOPBITS_1:
			(*cflag) &= ~CSTOPB;
			LEAVE( "RXTXPort:translate_stop_bits" );
			return 0;
		/*  ok.. lets try putting it in and see if anyone notices */
		case STOPBITS_1_5:
			if ( translate_data_bits( cflag, JDATABITS_5 ) )
				return( 1 );
			return 0;
		case STOPBITS_2:
			(*cflag) |= CSTOPB;
			LEAVE( "RXTXPort:translate_stop_bits" );
			return 0;
	}

	LEAVE( "RXTXPort:translate_stop_bits" );
/*
    For some reason the native exceptions are not being caught.  Moving this
    to the Java side fixed the issue.  taj.
	throw_java_exception( env, UNSUPPORTED_COMM_OPERATION,
		"", "stopbit value not supported" );
*/
	return 1;
}
jint gnu::io::RXTXPort::nativeGetFlowControlMode(
      	jint fd
)
{
	struct termios ttyset;
	int ret = 0;

	tcgetattr( fd, &ttyset );
	
	if( ttyset.c_cflag & HARDWARE_FLOW_CONTROL )
	{
		ret |= ( FLOWCONTROL_RTSCTS_IN | FLOWCONTROL_RTSCTS_OUT );
	}
	if ( ttyset.c_iflag & IXOFF )
	{
		ret |= FLOWCONTROL_XONXOFF_IN;
	}
	if ( ttyset.c_iflag & IXON )
	{
		ret |= FLOWCONTROL_XONXOFF_OUT;
	}
	return( (jint) ret );
}
jint gnu::io::RXTXPort::nativeGetParity(
	jint fd
)
{
	struct termios ttyset;

	if( tcgetattr( fd, &ttyset ) < 0 )
	{
		report("nativeGetParity:  tcgetattr failed\n");
		return( -1 );
	}
#ifdef  CMSPAR 
	if ( ( ttyset.c_cflag & PARENB ) &&
		( ttyset.c_cflag & PARODD ) &&
		( ttyset.c_cflag & CMSPAR ) )
	{
		return( JPARITY_MARK );
	}
	else if ( ttyset.c_cflag & ( PARENB &&
		ttyset.c_cflag & CMSPAR ) )
	{
		return( JPARITY_SPACE );
	}
#endif /* CMSPAR */
	if ( ttyset.c_cflag & PARENB &&
		ttyset.c_cflag & PARODD )
	{
		return( JPARITY_ODD );
	}
	else if ( ttyset.c_cflag & PARENB )
	{
		return( JPARITY_EVEN );
	}
	else
	{
		return( JPARITY_NONE );
	}
}

/*----------------------------------------------------------
 translate_parity

   accept:     javax.comm.SerialPort.PARITY_* constant
   perform:    set proper termios c_cflag bits
   return:     1 on error
   exceptions: UnsupportedCommOperationException
   comments:   The CMSPAR bit should be used for 'mark' and 'space' parity,
               but it's not in glibc's includes.  Oh well, rarely used anyway.
----------------------------------------------------------*/
int translate_parity(
	tcflag_t *cflag,
	jint parity
)
{
	ENTER( "translate_parity" );
#ifdef CMSPAR
	(*cflag) &= ~(PARENB | PARODD | CMSPAR );
#endif /* CMSPAR */
	switch( parity ) {
		case JPARITY_NONE:
			LEAVE( "translate_parity" );
			return 0;
		case JPARITY_EVEN:
			(*cflag) |= PARENB;
			LEAVE( "translate_parity" );
			return 0;
		case JPARITY_ODD:
			(*cflag) |= PARENB | PARODD;
			LEAVE( "translate_parity" );
			return 0;
#ifdef CMSPAR
		case JPARITY_MARK:
			(*cflag) |= PARENB | PARODD | CMSPAR;
			LEAVE( "translate_parity" );
			return 0;
		case JPARITY_SPACE:
			(*cflag) |= PARENB | CMSPAR;
			LEAVE( "translate_parity" );
			return 0;
#endif /* CMSPAR */
		default:
			printf("Parity missed %i\n", (int) parity );
	}

	LEAVE( "translate_parity" );
/*
    For some reason the native exceptions are not being caught.  Moving this
    to the Java side fixed the issue.  taj.
	throw_java_exception( env, UNSUPPORTED_COMM_OPERATION,
		"", "parity value not supported" );
*/
	return 1;
}
#if !defined(TIOCSERGETLSR) && !defined(WIN32)
/*----------------------------------------------------------
drain_loop()

   accept:      
   perform:	call tcdrain() and report an event when it succeds  
   return:      none
   exceptions:  
   comments:  	
----------------------------------------------------------*/
void *drain_loop( void *arg )
{
	struct event_info_struct *eis = ( struct event_info_struct * ) arg;
	/* char msg[80]; */
	int i;
	pthread_detach( pthread_self() );

	for(i=0;;i++)
	{
		report_verbose("drain_loop:  looping\n");
#if defined(__sun__)
	/* FIXME: No time to test on all OS's for production */
		usleep(5000);
#else
		usleep(1000000);
#endif /* __sun__ */
		/*
		system_wait();
		*/
		if( eis->eventloop_interrupted )
		{
			goto end;
		}
		if( tcdrain( eis->fd ) == 0 )
		{
			if( eis && eis->writing )
			{
				/*
				sprintf(msg, "drain_loop: setting OUTPUT_BUFFER_EMPTY\n" );
				report( msg );
				*/
				eis->output_buffer_empty_flag = 1;
				eis->writing=false;
			}
			else
			{
				if( !eis )
				{
					goto end;
				}
				report_verbose("drain_loop:  writing not set\n");
			}
		}
		else
		{
			report("drain_loop:  tcdrain bad fd\n");
			goto end;
		}
	}
end:
	report("------------------ drain_loop exiting ---------------------\n");
	eis->closing = 1;
	pthread_exit( NULL );
	return( NULL );
}
#endif /* !defined(TIOCSERGETLSR) && !defined(WIN32) */
/*----------------------------------------------------------
finalize_threads( )

   accept:      event_info_struct used to access java and communicate with
	        eventLoop().
   perform:     see comments
   return:      none
   exceptions:  none
   comments:	
	The is the pthread spawned on systems that can't access the
	LSR (Line Status Register).  Without access to the LSR rxtx
	cannot detect when the output buffer is empty in the Monitor
	Thread.  The solution is to return the value of write's return
	but hang around in this thread waiting for tcdrain to finish.
	
	once the drain has finished, we let the eventLoop know that the
	output buffer is empty and the Signal is sent.
----------------------------------------------------------*/
void finalize_threads( struct event_info_struct *eis )
{
#if     !defined(TIOCSERGETLSR) && !defined( WIN32 )
	/* used to shut down any remaining write threads */

	eis->output_buffer_empty_flag = 0;
	ENTER("finalize_threads\n");

	/* need to clean up again after working events */
	LEAVE("---------------- finalize_threads ---------------");
#endif /* TIOCSERGETLSR & !WIN32 */
}

#if !defined(TIOCSERGETLSR) && !defined( WIN32 )
static void warn_sig_abort( int signo )
{
	char msg[80];
	sprintf( msg, "RXTX Recieved Signal %i\n", signo );
	/*
	report_error( msg );
	*/
}
#endif /* TIOCSERGETLSR */

/*----------------------------------------------------------
init_threads( )

   accept:      none
   perform:     
   return:      none
   exceptions:  none
   comments:	
----------------------------------------------------------*/
int init_threads( gnu::io::RXTXPort *p, struct event_info_struct *eis )
{
#if !defined(TIOCSERGETLSR) & !defined(WIN32)
	sigset_t newmask, oldmask;
	struct sigaction newaction, oldaction;
	pthread_t tid;

	report_time_start( );
	report("init_threads:  start\n");
	sigemptyset(&newmask);
	sigaddset(&newmask, SIGCHLD);
	newaction.sa_handler = warn_sig_abort;
	sigemptyset( &newaction.sa_mask );
#ifdef SA_INTERRUPT
	newaction.sa_flags = SA_INTERRUPT;
#endif /* SA_INTERRUPT */
#ifdef SA_RESTART
	newaction.sa_flags = SA_RESTART;
#endif /* SA_RESTART */

	sigaction(SIGABRT, &newaction, &oldaction);
	sigaction(SIGCHLD, &newaction, &oldaction);
	sigaction(SIGALRM, &newaction, &oldaction);
	sigaction(SIGCONT, &newaction, &oldaction);
/*
	sigaction(SIGPOLL, &newaction, &oldaction);
	sigaction(SIGTRAP, &newaction, &oldaction);
	sigaction(SIGBUS, &newaction, &oldaction);
	sigaction(SIGSEGV, &newaction, &oldaction);
	sigaction(SIGFPE, &newaction, &oldaction);
	sigaction(SIGILL, &newaction, &oldaction);

	sigfillset(&newmask);
	sigprocmask( SIG_SETMASK, &newmask, &oldmask );
	pthread_sigmask( SIG_BLOCK, &newmask, &oldmask );
*/
	sigprocmask( SIG_SETMASK, &newmask, &oldmask );

	report("init_threads: creating drain_loop\n");
	pthread_create( &tid, NULL, drain_loop, (void *) eis );
	pthread_detach( tid );
#endif /* TIOCSERGETLSR */
	report("init_threads: set eis\n");
	p->eis = ( jint ) eis;
	report("init_threads:  stop\n");
	report_time_end( );
	return( 1 );
}

/*----------------------------------------------------------
RXTXPort.writeByte

   accept:      byte to write (passed as int)
                jboolean interrupted (no events if true)
   perform:     write a single byte to the port
   return:      none
   exceptions:  IOException
----------------------------------------------------------*/
void gnu::io::RXTXPort::writeByte(
	jint ji,
	jboolean interrupted
)
{
#ifndef TIOCSERGETLSR
	struct event_info_struct *index = master_index;
#endif
	unsigned char byte = (unsigned char)ji;
	int fd = ( int ) this->fd;
	int result;
	char msg[80];
#if defined ( __sun__ )
	int count;
#endif /* __sun__ */

	report_time_start();
	ENTER( "RXTXPort:writeByte" );
	do {
		sprintf( msg, "writeByte %c>>\n", byte );
		report( msg );
		result=WRITE (fd, (void * ) &byte, sizeof(unsigned char));
	}  while (result < 0 && errno==EINTR);
	if ( result < 0 && errno != EINTR )
	{
		snprintf( msg, 79, "%s%s", strerror( errno ), " in writeByte" );
		throw new java::io::IOException( JvNewStringUTF( msg ) );
		/* On win32 I'm not sure how many bytes made it out */
		return;
	}
/*
	This makes write for win32, glinux and Sol behave the same
#if defined ( __sun__ )
	do {
		report_verbose( "nativeDrain: trying tcdrain\n" );
		result=tcdrain(fd);
		count++;
	}  while (result && errno==EINTR && count <3);
#endif *//* __sun __ */
#ifndef TIOCSERGETLSR
	if( ! interrupted )
	{
		index = master_index;
		if( index )
		{
			while( index->fd != fd &&
				index->next ) index = index->next;
		}
		index->writing = 1;
		report( "writeByte:  index->writing = 1" );
	}
#endif
	sprintf( msg, "RXTXPort:writeByte %i\n", result );
	report( msg );
	LEAVE( "RXTXPort:writeByte" );
	if(result >= 0)
	{
		report_time_end();
		return;
	}
	snprintf( msg, 79, "%s%s", strerror( errno ), " in writeByte" );
	throw new java::io::IOException( JvNewStringUTF( msg ) );
}

/*----------------------------------------------------------
RXTXPort.writeArray

   accept:      jbarray: bytes used for writing
                offset: offset in array to start writing
                count: Number of bytes to write
                jboolean interrupted (no events if true)
   perform:     write length bytes of jbarray
   return:      none
   exceptions:  IOException
----------------------------------------------------------*/
void gnu::io::RXTXPort::writeArray(
	jbyteArray jbarray,
	jint offset,
	jint count,
	jboolean interrupted
)
{
#ifndef TIOCSERGETLSR
	struct event_info_struct *index = master_index;
#endif /* TIOCSERGETLSR */
	int fd;
	int result=0,total=0;
	jbyte *out = elements( jbarray );
	char msg[80];
#if defined ( __sun__ )
	int icount;
#endif /* __sun__ */
#if defined ( __sun__ )
	struct timespec retspec;

	retspec.tv_sec = 0;
	retspec.tv_nsec = 50000;
#endif /* __sun__ */
	fd = ( int ) this->fd;

	report_time_start();
	ENTER( "writeArray" );
	/* warning Roy Rogers */
	/*
	sprintf( message, "::::RXTXPort:writeArray(%s);\n", (char *) body );
	report_verbose( message );
	*/

	do {
		result=WRITE (fd, (void * ) ((char *) out + total + offset), count - total); /* dima */
		if(result >0){
			total += result;
		}
		report("writeArray()\n");
	}  while ( ( total < count ) && ( (result < 0 && errno==EINTR ) || ( result >= 0 ) ) ) ;
	if ( result < 0 && errno != EINTR )
	{
		snprintf( msg, 79, "%s%s", strerror( errno ), " in writeArray" );
		throw new java::io::IOException( JvNewStringUTF( msg ) );
		/* On win32 I'm not sure how many bytes made it out */
		return;
	}
/*
	This makes write for win32, glinux and Sol behave the same
#if defined ( __sun__ )
	do {
		report_verbose( "nativeDrain: trying tcdrain\n" );
		result=tcdrain(fd);
		icount++;
	}  while (result && errno==EINTR && icount <3);
#endif *//* __sun__ */
#ifndef TIOCSERGETLSR
	if( !interrupted )
	{
		if( index )
		{
			while( index->fd != fd &&
				index->next ) index = index->next;
		}
		index->writing = 1;
		report( "writeArray:  index->writing = 1" );
	}
#endif /* TIOCSERGETLSR */
	/*
		50 ms sleep to make sure read can get in

		what I think is happening here is the data writen is causing
		signals, the event loop can't select with data available

		I think things like BlackBox with 2 ports open are getting
		signals for both the reciever and transmitter since they
		are the same PID.

		Things just start spinning out of control after that.
	*/
	LEAVE( "RXTXPort:writeArray" );
	report_time_end();
	return;
fail:
	snprintf( msg, 79, "%s%s", strerror( errno ), " in writeArray" );
	throw new java::io::IOException( JvNewStringUTF( msg ) );
}

/*----------------------------------------------------------
RXTXPort.nativeDrain

   accept:      jboolean interrupted (no events if true)
   perform:     wait until all data is transmitted
   return:      none
   exceptions:  IOException
   comments:    java.io.OutputStream.flush() is equivalent to tcdrain,
                not tcflush, which throws away unsent bytes

                count logic added to avoid infinite loops when EINTR is
                true...  Thread.yeild() was suggested.
----------------------------------------------------------*/
jboolean gnu::io::RXTXPort::nativeDrain(
	jboolean interrupted
)
{
	int fd = ( int ) this->fd;
	struct event_info_struct *eis = ( struct event_info_struct * ) this->eis;
	int result, count=0;

	char message[80];

	ENTER( "SerialImp.c:drain()" );
	report_time_start( );
	do {
		report_verbose( "nativeDrain: trying tcdrain\n" );
		result=tcdrain(fd);
		count++;
	}  while (result && errno==EINTR && count <3);

	sprintf( message, "RXTXPort:drain() returns: %i\n", result ); 
	report_verbose( message );
#if defined(__sun__)
	/* FIXME: No time to test on all OS's for production */
	return( true );
#endif /* __sun__ */
	LEAVE( "RXTXPort:drain()" );
	if( result )
	{
		snprintf( message, 79, "%s%s", strerror( errno ), " in nativeDrain" );
		throw new java::io::IOException( JvNewStringUTF( message ) );
	}
	if( interrupted ) return( false );
#if !defined(TIOCSERGETLSR) && !defined(WIN32)
	if( eis && eis->writing )
	{
		eis->writing=false;
		eis->output_buffer_empty_flag = 0;
	}
#endif /* !TIOCSERGETLSR !WIN32 */
	if( eis && eis->eventflags[SPE_OUTPUT_BUFFER_EMPTY] )
	{
		send_event( this, eis, SPE_OUTPUT_BUFFER_EMPTY, 1 );
	}
	report_time_end( );
	return( false );
}

/*----------------------------------------------------------
RXTXPort.sendBreak

   accept:     duration in milliseconds.
   perform:    send break for actual time.  not less than 0.25 seconds.
   exceptions: none
   comments:   not very precise
----------------------------------------------------------*/
void gnu::io::RXTXPort::sendBreak(
	jint duration
)
{
	int fd = ( int ) this->fd;
	report_time_start( );
	ENTER( "RXTXPort:sendBreak()" );
	tcsendbreak( fd, (int)( duration / 250 ) );
	report_time_end( );
	LEAVE( "RXTXPort:sendBreak()" );
}

/*----------------------------------------------------------
RXTXPort.NativegetReceiveTimeout

   accept:     none
   perform:    get termios.c_cc[VTIME]
   return:     VTIME
   comments:   see  NativeEnableReceiveTimeoutThreshold
----------------------------------------------------------*/
jint gnu::io::RXTXPort::NativegetReceiveTimeout(
)
{
	int fd = this->fd;
	struct termios ttyset;
	char msg[80];

	ENTER( "RXTXPort:nativegetRecieveTimeout()" );
	if( tcgetattr( fd, &ttyset ) < 0 ) goto fail;
	LEAVE( "RXTXPort:nativegetRecieveTimeout()" );
	return(ttyset.c_cc[ VTIME ] * 100);
fail:
	LEAVE( "RXTXPort:nativegetRecieveTimeout()" );
	snprintf( msg, 79, "%s%s", strerror( errno ), " in nativeGetRecieveTimeout()" );
	throw new java::io::IOException( JvNewStringUTF( msg ) );
	return -1;
}

/*----------------------------------------------------------
RXTXPort.NativeisReceiveTimeoutEnabled

   accept:     none
   perform:    determine if VTIME is none 0
   return:     true if VTIME > 0 else false
   comments:   see  NativeEnableReceiveTimeoutThreshold
----------------------------------------------------------*/
jboolean gnu::io::RXTXPort::NativeisReceiveTimeoutEnabled(
)
{
	int fd = this->fd;
	struct termios ttyset;
	char msg[80];
	ENTER( "RXTXPort:NativeisRecieveTimeoutEnabled()" );
	if( tcgetattr( fd, &ttyset ) < 0 ) goto fail;
	LEAVE( "RXTXPort:NativeisRecieveTimeoutEnabled()" );
	return(ttyset.c_cc[ VTIME ] > 0 ? true:false);
fail:
	LEAVE( "RXTXPort:NativeisRecieveTimeoutEnabled()" );
	snprintf( msg, 79, "%s%s", strerror( errno ), " in isRecieveTimeoutEnabled()" );
	throw new java::io::IOException( JvNewStringUTF( msg ) );
	return false;
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
jboolean gnu::io::RXTXPort::isDSR(
)
{
	unsigned int result = 0;
	int fd = this->fd;
	char message[80];

	ENTER( "RXTXPort:isDSR" );
	ioctl( fd, TIOCMGET, &result );
	sprintf( message, "RXTXPort:isDSR returns %i\n", result & TIOCM_DSR );
	report( message );
	LEAVE( "RXTXPort:isDSR" );
	if( result & TIOCM_DSR ) return true;
	else return false;
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
jboolean gnu::io::RXTXPort::isCD(
)
{
	unsigned int result = 0;
	int fd =  ( int ) this->fd;
	char message[80];

	ENTER( "RXTXPort:isCD" );
	ioctl( fd, TIOCMGET, &result );
	sprintf( message, "RXTXPort:isCD returns %i\n", result & TIOCM_CD );
	LEAVE( "RXTXPort:isCD" );
	if( result & TIOCM_CD ) return true;
	else return false;
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
jboolean gnu::io::RXTXPort::isCTS(
)
{
	unsigned int result = 0;
	int fd = ( int ) this->fd;
	char message[80];

	ENTER( "RXTXPort:isCTS" );
	ioctl( fd, TIOCMGET, &result );
	sprintf( message, "RXTXPort:isCTS returns %i\n", result & TIOCM_CTS );
	report( message );
	LEAVE( "RXTXPort:isCTS" );
	if( result & TIOCM_CTS ) return true;
	else return false;
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
jboolean gnu::io::RXTXPort::isRI(
)
{
	unsigned int result = 0;
	int fd = ( int ) this->fd;
	char message[80];

	ENTER( "RXTXPort:isRI" );
	ioctl( fd, TIOCMGET, &result );
	sprintf( message, "RXTXPort:isRI returns %i\n", result & TIOCM_RI );
	report( message );
	LEAVE( "RXTXPort:isRI" );
	if( result & TIOCM_RI ) return true;
	else return false;
}

/*----------------------------------------------------------
RXTXPort.isRTS

   accept:      none
   perform:     check status of RTS
   return:      true if TIOCM_RTS is set
                false if TIOCM_RTS is not set
   exceptions:  none
   comments:    tcgetattr with c_cflag CRTS_IFLOW
----------------------------------------------------------*/
jboolean gnu::io::RXTXPort::isRTS(
)
{
	unsigned int result = 0;
	int fd = ( int ) this->fd;
	char message[80];

	ENTER( "RXTXPort:isRTS" );
	ioctl( fd, TIOCMGET, &result );
	sprintf( message, "RXTXPort:isRTS returns %i\n", result & TIOCM_RTS );
	report( message );
	LEAVE( "RXTXPort:isRTS" );
	if( result & TIOCM_RTS ) return true;
	else return false;
}

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
void gnu::io::RXTXPort::setRTS(
	jboolean state
)
{
	unsigned int result = 0;
	int fd = ( int ) this->fd;
	char message[80];

	ENTER( "RXTXPort:setRTS" );
	ioctl( fd, TIOCMGET, &result );
	if( state == true ) result |= TIOCM_RTS;
	else result &= ~TIOCM_RTS;
	ioctl( fd, TIOCMSET, &result );
	sprintf( message, "setRTS( %i )\n", state );
	report( message );
	LEAVE( "RXTXPort:setRTS" );
	return;
}

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
void gnu::io::RXTXPort::setDSR(
	jboolean state
)
{
	unsigned int result = 0;
	int fd = ( int ) this->fd;
	char message[80];

	ENTER( "RXTXPort:setDSR()" );
	ioctl( fd, TIOCMGET, &result );
	
	sprintf( message, "setDSR( %i )\n", state );
	if( state == true ) result |= TIOCM_DSR;
	else result &= ~TIOCM_DSR;
	ioctl( fd, TIOCMSET, &result );
	sprintf( message, "setDSR( %i )\n", state );
	report( message );
	LEAVE( "RXTXPort:setDSR()" );
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
jboolean gnu::io::RXTXPort::isDTR(
)
{
	unsigned int result = 0;
	int fd = ( int ) this->fd;
	char message[80];

	ENTER( "RXTXPort:isDTR" );
	ioctl( fd, TIOCMGET, &result );
	sprintf( message, "isDTR( ) returns %i\n", result& TIOCM_DTR );
	report( message );
	LEAVE( "RXTXPort:isDTR" );
	if( result & TIOCM_DTR ) return true;
	else return false;
}

/*----------------------------------------------------------
RXTXPort.setDTR

   accept:      new DTR state
   perform:     if state is true, TIOCM_DTR is set
                if state is false, TIOCM_DTR is unset
   return:      none
   exceptions:  none
   comments:    DTR stands for Data Terminal Ready
----------------------------------------------------------*/
void gnu::io::RXTXPort::setDTR(
	jboolean state
)
{
	unsigned int result = 0;
	int fd = ( int ) this->fd;
	char message[80];

	ENTER( "RXTXPort:setDTR" );
	ioctl( fd, TIOCMGET, &result );
	if( state == true ) result |= TIOCM_DTR;
	else result &= ~TIOCM_DTR;
	ioctl( fd, TIOCMSET, &result );
	sprintf( message, "setDTR( %i )\n", state );
	report( message );
	LEAVE( "RXTXPort:setDTR" );
	return;
}
/*----------------------------------------------------------
RXTXPort.static_add_filename

   accept:      filename and fd to save
   perform:     add a struct holding the info to a linked list
   return:      none
   exceptions:  none
   comments:    the info is checked on open() if its in the list no
		changes are performed on the file on open()

   comments:    see
			RXTXPort.nativeStaticSetDSR
			RXTXPort.nativeStaticSetDTR
			RXTXPort.nativeStaticSetRTS
			RXTXPort.nativeStaticSetSerialPortParams
		This is used so people can setDTR low before calling the
-----------------------------------------------------------*/

void static_add_filename( const char *filename, int fd)
{
	struct preopened *newp, *p = preopened_port;

	newp = ( struct preopened *) malloc( sizeof( struct preopened ) );
	strcpy( newp->filename, filename );
	newp->fd = fd;

	if( !p )
	{
		newp->next = NULL;
		newp->prev = NULL;
		preopened_port = newp;
		return;
	}
	for(;;)
	{
		if( !strcmp( p->filename, filename) )
		{
			/* already open */
			return;
		}
		if( p->next )
		{
			p = p->next;
		}
		else
		{
			/* end of list */
			newp->next = NULL;
			newp->prev = p;
			p->next = newp;
			preopened_port = p;
			return;
		}
	}
}
/*----------------------------------------------------------
RXTXPort.nativeSetBaudBase

   accept:      The Baud Base for custom speeds
   perform:     set the Baud Base
   return:      0 on success
   exceptions:  Unsupported Comm Operation on systems not supporting 
                TIOCGSERIAL
   comments:    
		Set baud rate to 38400 before using this
		First introduced in rxtx-2.1-3
----------------------------------------------------------*/
jboolean gnu::io::RXTXPort::nativeSetBaudBase(
	jint BaudBase
)
{

#if defined(TIOCGSERIAL)

	int fd = ( int ) this->fd;
	struct serial_struct sstruct;

	if (	sstruct.baud_base < 1 ||
		ioctl( fd, TIOCSSERIAL, &sstruct ) < 0 )
	{
		return( true );
	}
	return( false );
#else
	snprintf( msg, 79, "%s%s", strerror( errno ), " in nativeSetBaudBase" );
	throw new gnu::io::UnsupportedCommOperationException( JvNewStringUTF( msg ) );
	return( true );
#endif /* TIOCGSERIAL */
}

/*----------------------------------------------------------
RXTXPort.nativeGetBaudBase

   accept:      the Baud Base used for custom speeds
   perform:     
   return:      Baud Base
   exceptions:  Unsupported Comm Operation on systems not supporting 
                TIOCGSERIAL
   comments:    
		First introduced in rxtx-2.1-3
----------------------------------------------------------*/
jint gnu::io::RXTXPort::nativeGetBaudBase(
)
{

#if defined(TIOCGSERIAL)

	int fd = ( int ) this->fd;
	struct serial_struct sstruct;

	if ( ioctl( fd, TIOCGSERIAL, &sstruct ) < 0 )
	{
		return( ( jint ) -1 );
	}
	return( ( jint ) ( sstruct.baud_base ) );
#else
	snprintf( msg, 79, "%s%s", strerror( errno ), " in nativeGetBaudBase" );
	throw new gnu::io::UnsupportedCommOperationException( JvNewStringUTF( msg ) );
	return( ( jint ) -1 );
#endif /* TIOCGSERIAL */
}

/*----------------------------------------------------------
RXTXPort.nativeSetDivisor

   accept:      Divisor for custom speeds
   perform:     set the Divisor for custom speeds
   return:      0 on success
   exceptions:  Unsupported Comm Operation on systems not supporting 
                TIOCGSERIAL
   comments:    
		Set baud rate to 38400 before using this
		First introduced in rxtx-2.1-3
----------------------------------------------------------*/
jboolean gnu::io::RXTXPort::nativeSetDivisor(
	jint Divisor
)
{

#if defined(TIOCGSERIAL)

	int fd = ( int ) this->fd;
	struct serial_struct sstruct;

	if ( ioctl( fd, TIOCGSERIAL, &sstruct ) < 0 )
	{
		return( true );
	}

	if (	sstruct.custom_divisor < 1 ||
		ioctl( fd, TIOCSSERIAL, &sstruct ) < 0 )
	{
		return( true );
	}
	return( false );
#else
	snprintf( msg, 79, "%s%s", strerror( errno ), " in nativeSetDivisor" );
	throw new gnu::io::UnsupportedCommOperationException( JvNewStringUTF( msg ) );
	return( true );
#endif /* TIOCGSERIAL */
}

/*----------------------------------------------------------
RXTXPort.nativeGetDivisor

   accept:      none
   perform:     Find the Divisor used for custom speeds
   return:      Divisor
   exceptions:  Unsupported Comm Operation on systems not supporting
	        TIOCGSERIAL
   comments:    
		First introduced in rxtx-2.1-3
----------------------------------------------------------*/
jint gnu::io::RXTXPort::nativeGetDivisor(
)
{

#if defined(TIOCGSERIAL)

	int fd = ( int ) this->fd;
	struct serial_struct sstruct;

	if ( ioctl( fd, TIOCGSERIAL, &sstruct ) < 0 )
	{
		return( ( jint ) -1 );
	}

	return( ( jint ) sstruct.custom_divisor );
#else
	snprintf( msg, 79, "%s%s", strerror( errno ), " in nativeGetDivisor" );
	throw new gnu::io::UnsupportedCommOperationException( JvNewStringUTF( msg ) );
	return( ( jint ) -1 );
#endif /* TIOCGSERIAL */
}

/*----------------------------------------------------------
RXTXPort.nativeStaticSetDSR

   accept:      new RTS state
   perform:     if flag is true, TIOCM_DSR is set
                if flag is false, TIOCM_DSR is unset
   return:      none
   exceptions:  none
   comments:    Set the DSR so it does not raise on the next open
		needed for some funky test boards?

		This is static so we can not call the open() setDSR()
		we dont have the jobject.

		First introduced in rxtx-1.5-9
----------------------------------------------------------*/
jboolean gnu::io::RXTXPort::nativeStaticSetDSR (
	jstring jstr,
	jboolean flag
)
{
	int fd, i, pid = -1, result;
	char filename[80];

	for( i=0;i<jstr->length();i++)
		filename[i] = jstr->charAt(i);
	filename[i] = '\0';

	ENTER( "RXTXPort:nativeStaticSetDSR" );
#ifndef WIN32
	pid = getpid();
#endif /* WIN32 */

	/* Open and lock the port so nothing else changes the setting */

	if ( LOCK( filename, pid ) ) goto fail;;
	
	fd = find_preopened_ports( filename );
	if( !fd )
	{
		do {
			fd = ::OPEN (filename, O_RDWR | O_NOCTTY | O_NONBLOCK );
		}  while (fd < 0 && errno==EINTR);
		if ( configure_port( fd ) ) goto fail;
	}
	if ( fd < 0 ) goto fail;

	/* raise the DSR */

	ioctl( fd, TIOCMGET, &result );
	if( flag == true ) result |= TIOCM_DSR;
	else result &= ~TIOCM_DSR;
	ioctl( fd, TIOCMSET, &result );

	/* Unlock the port.  Good luck! :) */

	UNLOCK( filename,  pid );

	static_add_filename( filename, fd );
	
	/* dont close the port.  Its not clear if the DSR would remain high */
	LEAVE( "RXTXPort:nativeStaticSetDSR" );
	return( true );
fail:
	LEAVE( "RXTXPort:nativeStaticSetDSR" );
	return( false );
}

/*----------------------------------------------------------
RXTXPort.nativeStaticSetRTS

   accept:      new RTS state
   perform:     if flag is true, TIOCM_RTS is set
                if flag is false, TIOCM_RTS is unset
   return:      none
   exceptions:  none
   comments:    Set the RTS so it does not raise on the next open
		needed for some funky test boards?

		This is static so we can not call the open() setDTR()
		we dont have the jobject.

		First introduced in rxtx-1.5-9
----------------------------------------------------------*/
jboolean gnu::io::RXTXPort::nativeStaticSetRTS (
	jstring jstr,
	jboolean flag
)
{
	int fd, i, pid = -1, result;
	char filename[80];

	for( i=0;i<jstr->length();i++)
		filename[i] = jstr->charAt(i);
	filename[i] = '\0';

	ENTER( "RXTXPort:nativeStaticSetRTS" );
#ifndef WIN32
	pid = getpid();
#endif /* WIN32 */

	/* Open and lock the port so nothing else changes the setting */

	if ( LOCK( filename, pid ) ) goto fail;;
	
	fd = find_preopened_ports( filename );
	if( !fd )
	{
		do {
			fd = ::OPEN (filename, O_RDWR | O_NOCTTY | O_NONBLOCK );
		}  while (fd < 0 && errno==EINTR);
		if ( configure_port( fd ) ) goto fail;
	}
	if ( fd < 0 ) goto fail;

	/* raise the RTS */

	ioctl( fd, TIOCMGET, &result );
	if( flag == true ) result |= TIOCM_RTS;
	else result &= ~TIOCM_RTS;
	ioctl( fd, TIOCMSET, &result );

	/* Unlock the port.  Good luck! :) */

	UNLOCK( filename,  pid );

	static_add_filename( filename, fd );
	
	/* dont close the port.  Its not clear if the RTS would remain high */
	LEAVE( "RXTXPort:nativeStaticSetRTS" );
	return( true );
fail:
	LEAVE( "RXTXPort:nativeStaticSetRTS" );
	return( false );
}

/*----------------------------------------------------------
RXTXPort.nativeStaticSetSerialPortParams

   accept:      string for the filename, int baudrate, int databits,
		int stopbits, int parity
   perform:     set the serial port, set the params, save the fd in a linked
		list.
   return:      none
   exceptions:  none
   comments:    Not set the speed on the next 'open'

		This is static so we can not call the open() setDTR()
		we dont have the jobject.

		First introduced in rxtx-1.5-9
----------------------------------------------------------*/
void gnu::io::RXTXPort::nativeStaticSetSerialPortParams (
	jstring jstr,
	jint baudrate,
	jint dataBits,
	jint stopBits,
	jint parity
)
{
	int fd, i, pid = -1, cspeed = translate_speed( baudrate );
	char msg[80];
	char filename[80];

	for( i=0;i<jstr->length();i++)
		filename[i] = jstr->charAt(i);
	filename[i] = '\0';


	ENTER( "RXTXPort:nativeStaticSetSerialPortParams" );
#ifndef WIN32
	pid = getpid();
#endif /* WIN32 */
	/* Open and lock the port so nothing else changes the setting */

	if ( LOCK( filename, pid ) ) goto fail;
	
	fd = find_preopened_ports( filename );
	if( !fd )
	{
		do {
			fd = ::OPEN (filename, O_RDWR | O_NOCTTY | O_NONBLOCK );
		}  while (fd < 0 && errno==EINTR);
		if ( configure_port( fd ) ) goto fail;
	}

	if ( fd < 0 )
	{
		LEAVE( "RXTXPort:nativeStaticSetSerialPortParams" );
		snprintf( msg, 79, "%s%s", strerror( errno ), " in nativeStaticSetSerialPortParams" );
		throw new gnu::io::UnsupportedCommOperationException( JvNewStringUTF( msg ) );
		return;
	}

	if (cspeed == -1)
	{
		LEAVE( "RXTXPort:nativeStaticSetSerialPortParams" );
		snprintf( msg, 79, "%s%s", "BaudRate could not be set to the specified value", " in nativeStaticSetSerialPortParams" );
		throw new gnu::io::UnsupportedCommOperationException( JvNewStringUTF( msg ) );
		return;
	}

	if( set_port_params( fd, cspeed, dataBits, stopBits, parity ) )
	{
		LEAVE( "RXTXPort:nativeStaticSetSerialPortParams" );
		snprintf( msg, 79, "%s%s", strerror( errno ), " in nativeStaticSetSerialPortParams" );
		return;
	}

	/* Unlock the port.  Good luck! :) */

	UNLOCK( filename,  pid );

	static_add_filename( filename, fd );
	/* dont close the port. */

	LEAVE( "RXTXPort:nativeStaticSetSerialPortParams" );
	return;
fail:
	LEAVE( "RXTXPort:nativeStaticSetSerialPortParams" );
	return;
}

/*----------------------------------------------------------
RXTXPort.nativeStaticSetDTR

   accept:      new DTR state
   perform:     if flag is true, TIOCM_DTR is set
                if flag is false, TIOCM_DTR is unset
   return:      none
   exceptions:  none
   comments:    Set the DTR so it does not raise on the next open
		needed for some funky test boards?

		This is static so we can not call the open() setDTR()
		we dont have the jobject.

		First introduced in rxtx-1.5-9
----------------------------------------------------------*/
jboolean gnu::io::RXTXPort::nativeStaticSetDTR (
	jstring jstr,
	jboolean flag
)
{
	int fd, i, pid = -1, result;
	char filename[80];

	for( i=0;i<jstr->length();i++)
		filename[i] = jstr->charAt(i);
	filename[i] = '\0';

	ENTER( "RXTXPort:nativeStaticSetDTR" );
#ifndef WIN32
	pid = getpid();
#endif /* WIN32 */

	/* Open and lock the port so nothing else changes the setting */

	if ( LOCK( filename, pid ) ) goto fail;;
	
	fd = find_preopened_ports( filename );
	if( !fd )
	{
		do {
			fd = ::OPEN (filename, O_RDWR | O_NOCTTY | O_NONBLOCK );
		}  while (fd < 0 && errno==EINTR);
		if ( configure_port( fd ) ) goto fail;
	}
	if ( fd < 0 ) goto fail;

	/* raise the DTR */

	ioctl( fd, TIOCMGET, &result );
	if( flag == true ) result |= TIOCM_DTR;
	else result &= ~TIOCM_DTR;
	ioctl( fd, TIOCMSET, &result );

	/* Unlock the port.  Good luck! :) */

	UNLOCK( filename,  pid );

	static_add_filename( filename, fd );
	/* dont close the port.  Its not clear if the DTR would remain high */

	LEAVE( "RXTXPort:nativeStaticSetDTR" );
	return( true );
fail:
	LEAVE( "RXTXPort:nativeStaticSetDTR" );
	return( false );
}

/*----------------------------------------------------------
RXTXPort.nativeStaticIsRTS

   accept:      filename
   perform:     check status of RTS of preopened ports (setting lines/params
		before calling the Java open()
   return:      true if TIOCM_RTS is set
                false if TIOCM_RTS is not set
   exceptions:  none
   comments:    RTS stands for Request to Send
----------------------------------------------------------*/
jboolean gnu::io::RXTXPort::nativeStaticIsRTS(
	jstring jstr
)
{
	unsigned int result = 0;
	int i, fd;
	char message[80];
	char filename[80];

	for( i=0;i<jstr->length();i++)
		filename[i] = jstr->charAt(i);
	filename[i] = '\0';
	fd = find_preopened_ports( filename );

	ENTER( "RXTXPort:nativeStaticIsRTS" );
	if( !fd )
	{
		/* Exception? FIXME */
		return false;
	}
	ioctl( fd, TIOCMGET, &result );
	sprintf( message, "nativeStaticIsRTS( ) returns %i\n", result& TIOCM_RTS );
	report( message );
	LEAVE( "RXTXPort:nativeStaticIsRTS" );
	if( result & TIOCM_RTS ) return true;
	else return false;
}
/*----------------------------------------------------------
RXTXPort.nativeStaticIsDSR

   accept:      filename
   perform:     check status of DSR of preopened ports (setting lines/params
		before calling the Java open()
   return:      true if TIOCM_DSR is set
                false if TIOCM_DSR is not set
   exceptions:  none
   comments:    
----------------------------------------------------------*/
jboolean gnu::io::RXTXPort::nativeStaticIsDSR(
	jstring jstr
)
{
	unsigned int result = 0;
	int i, fd;
	char message[80];
	char filename[80];

	for( i=0;i<jstr->length();i++)
		filename[i] = jstr->charAt(i);
	filename[i] = '\0';
	fd = find_preopened_ports( filename );

	ENTER( "RXTXPort:nativeStaticIsDSR" );
	if( !fd )
	{
		/* Exception? FIXME */
		return false;
	}
	ioctl( fd, TIOCMGET, &result );
	sprintf( message, "nativeStaticIsDSR( ) returns %i\n", result& TIOCM_DSR );
	report( message );
	LEAVE( "RXTXPort:nativeStaticIsDSR" );
	if( result & TIOCM_DSR ) return true;
	else return false;
}
/*----------------------------------------------------------
RXTXPort.nativeStaticIsDTR

   accept:      filename
   perform:     check status of DTR of preopened ports (setting lines/params
		before calling the Java open()
   return:      true if TIOCM_DTR is set
                false if TIOCM_DTR is not set
   exceptions:  none
   comments:    DTR stands for Data Terminal Ready
----------------------------------------------------------*/
jboolean gnu::io::RXTXPort::nativeStaticIsDTR(
	jstring jstr
)
{
	unsigned int result = 0;
	int i, fd;
	char message[80];
	char filename[80];

	for( i=0;i<jstr->length();i++)
		filename[i] = jstr->charAt(i);
	filename[i] = '\0';
	fd = find_preopened_ports( filename );

	ENTER( "RXTXPort:nativeStaticIsDTR" );
	if( !fd )
	{
		/* Exception? FIXME */
		return false;
	}
	ioctl( fd, TIOCMGET, &result );
	sprintf( message, "nativeStaticIsDTR( ) returns %i\n", result& TIOCM_DTR );
	report( message );
	LEAVE( "RXTXPort:nativeStaticIsDTR" );
	if( result & TIOCM_DTR ) return true;
	else return false;
}
/*----------------------------------------------------------
RXTXPort.nativeStaticIsCD

   accept:      filename
   perform:     check status of CD of preopened ports (setting lines/params
		before calling the Java open()
   return:      true if TIOCM_CD is set
                false if TIOCM_CD is not set
   exceptions:  none
   comments:    CD stands for carrier detect
----------------------------------------------------------*/
jboolean gnu::io::RXTXPort::nativeStaticIsCD(
	jstring jstr
)
{
	unsigned int result = 0;
	int i, fd;
	char message[80];
	char filename[80];

	for( i=0;i<jstr->length();i++)
		filename[i] = jstr->charAt(i);
	filename[i] = '\0';
	fd = find_preopened_ports( filename );

	ENTER( "RXTXPort:nativeStaticIsCD" );
	if( !fd )
	{
		/* Exception? FIXME */
		return false;
	}
	ioctl( fd, TIOCMGET, &result );
	sprintf( message, "nativeStaticIsCD( ) returns %i\n", result& TIOCM_CD );
	report( message );
	LEAVE( "RXTXPort:nativeStaticIsCD" );
	if( result & TIOCM_CD ) return true;
	else return false;
}
/*----------------------------------------------------------
RXTXPort.nativeStaticIsCTS

   accept:      filename
   perform:     check status of CTS of preopened ports (setting lines/params
		before calling the Java open()
   return:      true if TIOCM_CTS is set
                false if TIOCM_CTS is not set
   exceptions:  none
   comments:    CTS stands for Clear To Send
----------------------------------------------------------*/
jboolean gnu::io::RXTXPort::nativeStaticIsCTS(
	jstring jstr
)
{
	unsigned int result = 0;
	int i, fd;
	char message[80];
	char filename[80];

	for( i=0;i<jstr->length();i++)
		filename[i] = jstr->charAt(i);
	filename[i] = '\0';
	fd = find_preopened_ports( filename );

	ENTER( "RXTXPort:nativeStaticIsCTS" );
	if( !fd )
	{
		/* Exception? FIXME */
		return false;
	}
	ioctl( fd, TIOCMGET, &result );
	sprintf( message, "nativeStaticIsCTS( ) returns %i\n", result& TIOCM_CTS );
	report( message );
	LEAVE( "RXTXPort:nativeStaticIsCTS" );
	if( result & TIOCM_CTS ) return true;
	else return false;
}
/*----------------------------------------------------------
RXTXPort.nativeStaticIsRI

   accept:      filename
   perform:     check status of RI of preopened ports (setting lines/params
		before calling the Java open()
   return:      true if TIOCM_RI is set
                false if TIOCM_RI is not set
   exceptions:  none
   comments:    RI stands for carrier detect
----------------------------------------------------------*/
jboolean gnu::io::RXTXPort::nativeStaticIsRI(
	jstring jstr
)
{
	unsigned int result = 0;
	int i, fd;
	char message[80];
	char filename[80];

	for( i=0;i<jstr->length();i++)
		filename[i] = jstr->charAt(i);
	filename[i] = '\0';
	fd = find_preopened_ports( filename );

	ENTER( "RXTXPort:nativeStaticIsRI" );
	if( !fd )
	{
		/* Exception? FIXME */
		return false;
	}
	ioctl( fd, TIOCMGET, &result );
	sprintf( message, "nativeStaticRI( ) returns %i\n", result& TIOCM_RI );
	report( message );
	LEAVE( "RXTXPort:nativeStaticIsRI" );
	if( result & TIOCM_RI ) return true;
	else return false;
}

/*----------------------------------------------------------
RXTXPort.nativeStaticGetBaudRate

   accept:      filename
   perform:     find the baud rate (not all buads are handled yet)
   return:      return the baud rate or -1 if not supported yet.
   exceptions:  
   comments:    simple test for preopened ports
----------------------------------------------------------*/
jint gnu::io::RXTXPort::nativeStaticGetBaudRate(
	jstring jstr
)
{
	int i, fd;
	struct termios ttyset;
	int baudrate;
	char filename[80];

	for( i=0;i<jstr->length();i++)
		filename[i] = jstr->charAt(i);
	filename[i] = '\0';
	fd = find_preopened_ports( filename );

	ENTER( "RXTXPort:nativeStaticGetBaudRate" );
	if( !fd )
	{
		/* Exception? FIXME */
		return -1;
	}
	if( tcgetattr( fd, &ttyset ) < 0 )
	{
		report( "nativeStaticGetBaudRate: Cannot Get Serial Port Settings\n" );
		return(-1);
	}
/*
dima writes:

Trent, here is something I found with google:
(freebsd list freebsd-current@freebsd.org)

Andrzej Bialecki <abial@korin.warman.org.pl> asked:
I tried to compile a piece of software, probably for Linux, and I noticed
that we don't define CBAUD constant. I'm not sure, but I think POSIX
defines and uses it. Should(n't) we?

Bruce Evans <bde@zeta.org.au> answered:
CBAUD is for SYSV compatibility.  It is considerably inferior to POSIX's
cf{get,set}{i,o}speed and shouldn't be provided or used.

*/
#if defined(CBAUD)/* dima */
    	baudrate = ttyset.c_cflag&CBAUD;
#else
    	if(cfgetispeed(&ttyset) != cfgetospeed(&ttyset)) return -1;
    	baudrate = cfgetispeed(&ttyset);
#endif
	return( get_java_baudrate(baudrate) );
}
/*----------------------------------------------------------
RXTXPort.nativeStaticGetDataBits

   accept:      filename
   perform:     find the data bits (not all buads are handled yet)
   return:      return the data bits
   exceptions:  
   comments:    simple test for preopened ports
----------------------------------------------------------*/
jint gnu::io::RXTXPort::nativeStaticGetDataBits(
	jstring jstr
)
{
	int i, fd;
	struct termios ttyset;
	char filename[80];

	for( i=0;i<jstr->length();i++)
		filename[i] = jstr->charAt(i);
	filename[i] = '\0';
	fd = find_preopened_ports( filename );

	ENTER( "RXTXPort:nativeStaticGetDataBits" );
	if( !fd )
	{
		/* Exception? FIXME */
		return -1;
	}
	if( tcgetattr( fd, &ttyset ) < 0 )
	{
		report( "nativeStaticGetDataBits: Cannot Get Serial Port Settings\n" );
		return(-1);
	}
	switch( ttyset.c_cflag&CSIZE ) {
		case CS5:  return JDATABITS_5;
		case CS6:  return JDATABITS_6;
		case CS7:  return JDATABITS_7;
		case CS8:  return JDATABITS_8;
		default:  return(-1);
	}
}
/*----------------------------------------------------------
RXTXPort.nativeStaticGetParity

   accept:      filename
   perform:     find the parity
   return:      return the parity
   exceptions:  
   comments:    simple test for preopened ports
----------------------------------------------------------*/
jint gnu::io::RXTXPort::nativeStaticGetParity(
	jstring jstr
)
{
	int i, fd;
	struct termios ttyset;
	char filename[80];

	for( i=0;i<jstr->length();i++)
		filename[i] = jstr->charAt(i);
	filename[i] = '\0';
	fd = find_preopened_ports( filename );

	ENTER( "RXTXPort:nativeStaticGetParity" );
	if( !fd )
	{
		/* Exception? FIXME */
		return -1;
	}
	if( tcgetattr( fd, &ttyset ) < 0 )
	{
		report( "nativeStaticGetParity: Cannot Get Serial Port Settings\n" );
		return(-1);
	}
#ifdef CMSPAR
	switch( ttyset.c_cflag&(PARENB|PARODD|CMSPAR ) ) {
#else
	switch( ttyset.c_cflag&(PARENB|PARODD) ) {
#endif /* CMSPAR */
		case 0: return JPARITY_NONE;
		case PARENB: return JPARITY_EVEN;
		case PARENB | PARODD: return JPARITY_ODD;
#ifdef CMSPAR
		case PARENB | PARODD | CMSPAR: return JPARITY_MARK;
		case PARENB | CMSPAR: return JPARITY_SPACE;
#endif /* CMSPAR */
		default:  return(-1);
	}
}
/*----------------------------------------------------------
RXTXPort.nativeStaticGetStopBits

   accept:      filename
   perform:     find the stop bits 
   return:      return the stop bits
   exceptions:  
   comments:    simple test for preopened ports
----------------------------------------------------------*/
jint gnu::io::RXTXPort::nativeStaticGetStopBits(
	jstring jstr
)
{
	int i, fd;
	struct termios ttyset;
	char filename[80];
	fd = find_preopened_ports( filename );

	for( i=0;i<jstr->length();i++)
		filename[i] = jstr->charAt(i);
	filename[i] = '\0';

	ENTER( "RXTXPort:nativeStaticGetStopBits" );
	if( !fd )
	{
		/* Exception? FIXME */
		return -1;
	}
	if( tcgetattr( fd, &ttyset ) < 0 )
	{
		report( "nativeStaticGetStopBits: Cannot Get Serial Port Settings\n" );
		return(-1);
	}
	switch( ttyset.c_cflag&(CSTOPB) ) {
		case 0: return STOPBITS_1;
		case CSTOPB:  return STOPBITS_2;
		default:  return  STOPBITS_1_5;
	}
}

/*----------------------------------------------------------
RXTXPort.nativeGetParityErrorChar

   accept:      -
   perform:     check the ParityErrorChar
   return:      The ParityErrorChar as an jbyte.
   exceptions:  UnsupportedCommOperationException if not implemented
   comments:    It appears the Parity char is usually \0.  The windows
		API allows for this to be changed.  I cant find may
		examples of this being done.  Maybe for a reason.

		Use a direct call to the termios file until we find a
		solution.
----------------------------------------------------------*/
jbyte gnu::io::RXTXPort::nativeGetParityErrorChar(
)
{
	unsigned int result = 0;

	ENTER( "nativeGetParityErrorChar" );
#ifdef WIN32
	result = ( jbyte ) termiosGetParityErrorChar(
			this->fd );
#else
	/*
	   arg!  I cant find a way to change it from \0 in Linux.  I think
		   the frame and parity error characters are hardcoded.
	*/
		result = ( jint ) '\0';

#endif /* WIN32 */
	LEAVE( "nativeGetParityErrorChar" );
	return( ( jbyte ) result );
}

/*----------------------------------------------------------
RXTXPort.nativeGetEndOfInputChar

   accept:      -
   perform:     check the EndOf InputChar
   return:      the EndOfInputChar as an jbyte.  -1 on error
   exceptions:  UnsupportedCommOperationException if not implemented
   comments:    
----------------------------------------------------------*/
jbyte gnu::io::RXTXPort::nativeGetEndOfInputChar(
)
{
	int fd = ( int ) this->fd;
	struct termios ttyset;

	ENTER( "nativeGetEndOfInputChar" );
	if( tcgetattr( fd, &ttyset ) < 0 ) goto fail;
	LEAVE( "nativeGetEndOfInputChar" );
	return( (jbyte) ttyset.c_cc[VEOF] );
fail:
	LEAVE( "nativeGetEndOfInputChar" );
	report( "nativeGetEndOfInputChar failed\n" );
	return( ( jbyte ) -1 );
}

/*----------------------------------------------------------
RXTXPort.nativeSetParityErrorChar

   accept:      the ParityArrorCharacter as an int.
   perform:     Set the ParityErrorChar
   return:      true on success
   exceptions:  UnsupportedCommOperationException if not implemented
   comments:    It appears the Parity char is usually \0.  The windows
		API allows for this to be changed.  I cant find may
		examples of this being done.  Maybe for a reason.

		Use a direct call to the termios file until we find a
		solution.
----------------------------------------------------------*/
jboolean gnu::io::RXTXPort::nativeSetParityErrorChar(
	jbyte value
)
{

	char msg[80];
#ifdef WIN32
	int fd = ( int ) this->fd;
	ENTER( "nativeSetParityErrorChar" );
	termiosSetParityError( fd, ( char ) value );
	LEAVE( "nativeSetParityErrorChar" );
	return( true );
#else
	ENTER( "nativeSetParityErrorChar" );
	/*
	   arg!  I cant find a way to change it from \0 in Linux.  I think
	   the frame and parity error characters are hardcoded.
	*/

	snprintf( msg, 79, "%s%s", strerror( errno ), " in setParityErrorChar()" );
	throw new gnu::io::UnsupportedCommOperationException( JvNewStringUTF( msg ) );
	LEAVE( "nativeSetParityErrorChar" );
	return( false );
#endif /* WIN32 */
}

/*----------------------------------------------------------
RXTXPort.nativeSetEndOfInputChar

   accept:      The EndOfInputChar as an int
   perform:     set the EndOfInputChar
   return:      true on success
   exceptions:  UnsupportedCommOperationException if not implemented
   comments:    This may cause troubles on Windows.
		Lets give it a shot and see what happens.

		See termios.c for the windows bits.

		EofChar = val;
		fBinary = false;  winapi docs say always use true. ?
----------------------------------------------------------*/
jboolean gnu::io::RXTXPort::nativeSetEndOfInputChar(
	jbyte value
)
{
	int fd = ( int ) this->fd;
	struct termios ttyset;
	char msg[80];

	ENTER( "nativeSetEndOfInputChar" );
	if( tcgetattr( fd, &ttyset ) < 0 ) goto fail;
	ttyset.c_cc[VEOF] = ( char ) value;
	if( tcsetattr( fd, TCSANOW, &ttyset ) < 0 ) goto fail;
	LEAVE( "nativeSetEndOfInputChar" );
	return( true );
fail:
	snprintf( msg, 79, "%s%s", strerror( errno ), " in nativeSetEndOfInputChar" );
	throw new gnu::io::UnsupportedCommOperationException( JvNewStringUTF( msg ) );
	report( "nativeSetEndOfInputChar failed\n" );
	LEAVE( "nativeSetEndOfInputChar" );
	return( false );
}

#ifndef WIN32
long
GetTickCount()
{
	/* return milliseconds */
	struct timeval now;

	gettimeofday(&now, NULL);
	report_verbose("gettimeofday\n");

#ifdef __QNX__
	return ( ( now.tv_sec * 1000 + now.tv_usec / 1000 ) );
#else
	return ( ( now.tv_sec * 1000) + ( long ) ceil( ( double ) now.tv_usec / 1000 ) );
#endif /* __QNX__ */
}

#endif /* !WIN32 */

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

int read_byte_array(
	int fd,
	unsigned char *buffer,
	int length,
	int timeout
)
{
	int ret, left, bytes = 0;
	long timeLeft, now = 0, start = 0;
	char msg[80];
	struct timeval tv, *tvP;
	fd_set rset;
	/* TRENT */
	int count = 0;
        sigset_t sigpwr_mask;

	// init the sigset for blocking SIGPWR + SIGXCPU
	sigemptyset(&sigpwr_mask);
	sigaddset(&sigpwr_mask, SIGPWR);
	sigaddset(&sigpwr_mask, SIGXCPU);
	
	report_time_start();
	ENTER( "read_byte_array" );
	sprintf(msg, "read_byte_array requests %i\n", length);
	report( msg );
	left = length;
	if (timeout >= 0)
		start = GetTickCount();
	while( bytes < length &&  count++ < 20 );
	{
		if (timeout >= 0) {
			now = GetTickCount();
			if ( now-start >= timeout )
				return bytes;
		}

		FD_ZERO(&rset);
		FD_SET(fd, &rset);

		if (timeout >= 0){
			timeLeft = timeout - (now - start);
			tv.tv_sec = timeLeft / 1000;
			tv.tv_usec = 1000 * ( timeLeft % 1000 );
			tvP = &tv;
		}
		else{
			tvP = NULL;
		}
		
		// ignore SIGPWR + SIGXCPU during SELECT as gcj GC uses these signals
		sigprocmask(SIG_BLOCK, &sigpwr_mask, NULL);
		do {
			ret = SELECT(fd + 1, &rset, NULL, NULL, tvP);
		} while ( ret < 0 && errno == EINTR );
		sigprocmask(SIG_UNBLOCK, &sigpwr_mask, NULL);
		if (ret == -1){
			report( "read_byte_array: select returned -1\n" );
			LEAVE( "read_byte_array" );
			return -1;
		}
		else if (ret > 0){
			if ((ret = READ( fd, buffer + bytes, left )) < 0 ){
				if (errno != EINTR && errno != EAGAIN){
					report( "read_byte_array: read returned -1\n" );
					LEAVE( "read_byte_array" );
					return -1;
				}
			}
			else if ( ret ) {
				bytes += ret;
				left -= ret;
			}
		/*
		The only thing that is bugging me with the new 
		version is the CPU usage when reading on the serial port.  I
		looked at it today and find a quick fix.  It doesn't seems to
		affect the performance for our apps (I mean in a negative way,
		cause the CPU is back to normal, near 0-5%).  All I did is add
		a usleep in the reading function.

		Nicolas <ripley@8d.com>
		*/
			else {
				//usleep(10);
				usleep(1000);
			}
		}
	}
/*
	if( count > 19 )
	{
		throw_java_exception( env, IO_EXCEPTION, "read_byte_array",
			"No data available" );
	}
*/

	//*(buffer+bytes) = 0;
	sprintf(msg, "read_byte_array returns %i\n", bytes);
	report( msg );
	LEAVE( "read_byte_array" );
	report_time_end();
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
void gnu::io::RXTXPort::NativeEnableReceiveTimeoutThreshold(
	jint vtime,
	jint threshold,
	jint buffer
)
{
	int fd = ( int ) this->fd;
	struct termios ttyset;
	int timeout;
	char msg[80];

	if (vtime < 0){
		timeout = 0; 
	}
	else if (vtime == 0){
		timeout = 1;
	}
	else{
		timeout = vtime;
	}

	ENTER( "RXTXPort:NativeEnableRecieveTimeoutThreshold" );
	if( tcgetattr( fd, &ttyset ) < 0 ) goto fail;
	/* TESTING ttyset.c_cc[ VMIN ] = threshold; */
	ttyset.c_cc[ VMIN ] = 0;
	ttyset.c_cc[ VTIME ] = timeout/100;
	if( tcsetattr( fd, TCSANOW, &ttyset ) < 0 ) goto fail;

	LEAVE( "RXTXPort:NativeEnableRecieveTimeoutThreshold" );
	return;
fail:
	LEAVE( "RXTXPort:NativeEnableRecieveTimeoutThreshold" );
	snprintf( msg, 79, "%s%s", strerror( errno ), " in TimeoutThreshold" );
	throw new java::io::IOException( JvNewStringUTF( msg ) );
	return;
}

/*----------------------------------------------------------
RXTXPort.readByte

   accept:      none
   perform:     Read a single byte from the port.  Block unless an exeption
	        is thrown, or end of stream.
   return:      The byte read
   exceptions:  IOException
   comments:

On Fri, 30 Aug 2002, Bill Smith wrote:

I agree, the documentation isn't the best. No surprises there. 

I did do a test using the sun/win32 comm driver with read() and retrieve 
timeout enabled. It blocked until the timeout expired, then returned a -1. 
This seems to jive with the way I'm reading it which is the javax.comm 
comments regarding read (in the CommPort.getInputStream stuff)
extends/overrides 
the documentation for java.io.InputStream.

This is the same behavior that the Windriver driver for vxworks exhibits.

On Fri, 30 Aug 2002, Bill Smith wrote:

> Hi Trent,
> 
> I have a couple of questions/comments.
> 
> 1) I noticed in the thread last night and in the code changes this morning that you
>    now have readByte() (which is called from the input stream read(), to block
>    forever. I pulled the following info from the javax.comm doc for the CommPort class in
>    getInputStream().
> 
>    The way I interpret that is that read() just like read(byte[]), and read(byte[], int, int),
>    show only block indefinitely if timeout is disabled. The sun implementation for win32 (as 
>    well as the one we have for vxworks) returns a -1 when it times out.
> 

Doing what Sun does is going to the least hassle.  The documentation was a 
little unclear to me.  I assume this is the CommPort.getInputStream 
comment that you mention 

        The read behaviour of the input stream returned by getInputStream
        depends on combination of the threshold and timeout values. The
        possible behaviours are described in the table below: ... 

But InputStream is where read(byte) is documented
http://java.sun.com/j2se/1.3/docs/api/java/io/InputStream.html#read()

        Reads the next byte of data from the input stream. The value byte
        is returned as an int in the range 0 to 255. If no byte is
        available because the end of the stream has been reached, the value 
        -1 is returned. This method blocks until input data is
        available, the end of the stream is detected, or an exception is 
        thrown

If you are sure commapi is doing a timeout and returning -1, I can change 
it back and document the issue.

Because I often grep my own mailbox for details, I'm going to add
these two comments also:

        public int read(byte[] b)
)
http://java.sun.com/j2se/1.3/docs/api/java/io/InputStream.html#read(byte[])

        Reads some number of bytes from the input stream and stores them 
        into the buffer array b. The number of bytes actually read is 
        returned as an integer. This method blocks until input data is
        available, end of file is detected, or an exception is thrown. 

        If b is null, a NullPointerException is thrown. If the length of b 
        is zero, then no bytes are read and 0 is returned; otherwise, 
        there is an attempt to read at least one byte. If no byte is 
        available because the stream is at end of file, the value -1 is 
        returned; otherwise, at least one byte is read and stored into b. 

So read(byte[] b) is documented as blocking for the first byte.

public int read(byte[] b,int off,int len)
http://java.sun.com/j2se/1.3/docs/api/java/io/InputStream.html#read(byte[],
int, int)

        Reads up to len bytes of data from the input stream into an array of 
        bytes. An attempt is made to read as many as len bytes, but a 
        smaller number may be read, possibly zero. The number of bytes 
        actually read is returned as an integer. 

Which makes sense with the timeout documentation.

<snip>threshold comment  I'll look at that next.  I thought those changes 
where in the ifdefed code.  I'll take a second look and reply.

> 
> Thoughts? Comments?
> 
> Bill
> 
> ----------------------
> 
> public abstract InputStream getInputStream() throws IOException
> 
> 
> Returns an input stream. This is the only way to receive data from the
communications
> port. If the port is unidirectional and doesn't support receiving data, then 
> getInputStream returns null.
> 
> The read behaviour of the input stream returned by getInputStream depends on 
> combination of the threshold and timeout values. The possible behaviours are 
> described in the table below: 
> 
> 
>    Threshold             Timeout        Read Buffer    Read Behaviour
> State     Value       State     Value       Size   
>
-----------------------------------------------------------------------------------
> disabled    -         disabled    -       n bytes      block until any data is available
> 
> enabled   m bytes     disabled    -       n bytes      block until min(m,n) bytes are available
> 
> disabled    -         enabled   x ms      n bytes      block for x ms or
until any data is available
> 
> enabled   m bytes     enabled   x ms      n bytes      block for x ms or
until min(m,n) bytes are available
> 
> Returns: InputStream object that can be used to read from the port 
> 
> Throws: IOException if an I/O error occurred


----------------------------------------------------------*/
jint gnu::io::RXTXPort::readByte(
)
{
	int bytes;
	unsigned char buffer[ 1 ];
	int fd = ( int ) this->fd;
	int timeout = ( int ) this->timeout;
	char msg[80];

	ENTER( "RXTXPort:readByte" );
	report_time_start( );
	bytes = read_byte_array( fd, buffer, 1, timeout );
	if( bytes < 0 ) {
		LEAVE( "RXTXPort:readByte" );
		snprintf( msg, 79, "%s%s", strerror( errno ), " in readByte" );
		throw new java::io::IOException( JvNewStringUTF( msg ) );
		return -1;
	}
	LEAVE( "RXTXPort:readByte" );
	sprintf( msg, "readByte return(%i)\n", bytes ? buffer[ 0 ] : -1 );
	report( msg );
	report_time_end( );
	return (bytes ? (jint)buffer[ 0 ] : -1);
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
jint gnu::io::RXTXPort::readArray(
	jbyteArray jbarray,
	jint offset,
	jint length
)
{
	int bytes;
	char msg[80];
	int fd = ( int ) this->fd;
	int timeout = ( int ) this->timeout;
	jbyte *in = elements( jbarray ) + offset;

	ENTER( "readArray" );
	report_time_start( );
	if( length > SSIZE_MAX || length < 0 ) {
		report( "RXTXPort:readArray length > SSIZE_MAX" );
		LEAVE( "RXTXPort:readArray" );
		snprintf( msg, 79, "%s%s", "Invalid length", " in readArray" );
		throw new java::lang::ArrayIndexOutOfBoundsException( JvNewStringUTF( msg ) );
		return -1;
	}
	bytes = read_byte_array( fd, (unsigned char *) in, length, timeout );/* dima */
	if( bytes < 0 ) {
		report( "RXTXPort:readArray bytes < 0" );
		LEAVE( "RXTXPort:readArray" );
		snprintf( msg, 79, "%s%s", strerror( errno ), " in readArray" );
		throw new java::io::IOException( JvNewStringUTF( msg ) );
		return -1;
	}
	sprintf( msg, "RXTXPort:readArray: %i %i\n", (int) length, bytes);
	report( msg );
	report_time_end( );

	LEAVE( "RXTXPort:readArray" );
	return (bytes);
}

/*----------------------------------------------------------
RXTXPort.nativeavailable

   accept:      none
   perform:     find out the number of bytes available for reading
   return:      available bytes
                -1 on error
   exceptions:  none
----------------------------------------------------------*/
jint gnu::io::RXTXPort::nativeavailable(
)
{
	int fd = ( int ) this->fd;
	int result;
	char msg[80];

/*
	ENTER( "RXTXPort:nativeavailable" );

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
	sprintf(msg, "    nativeavailable: FIORDCHK result %d, \
		errno %d\n", result , result == -1 ? errno : 0);
	report_verbose( msg );
	if (result == -1) {
		goto fail;
	}
	if( result )
	{
		sprintf(msg, "    nativeavailable: FIORDCHK result %d, \
				errno %d\n", result , result == -1 ? errno : 0);
		report( msg );
	}
/*
	LEAVE( "RXTXPort:nativeavailable" );
*/
	return (jint)result;
fail:
	report("RXTXPort:nativeavailable:  ioctl() failed\n");
/*
	LEAVE( "RXTXPort:nativeavailable" );
*/
	snprintf( msg, 79, "%s%s", strerror( errno ), " in nativeavailable" );
	throw new java::io::IOException( JvNewStringUTF( msg ) );
	return (jint)result;
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
   exceptions:  UnsupportedCommOperationException
   comments:  there is no differentiation between input and output hardware
              flow control
----------------------------------------------------------*/
void gnu::io::RXTXPort::setflowcontrol(
	jint flowmode
)
{
	struct termios ttyset;
	int fd = ( int ) this->fd;
	char msg[80];

	ENTER( "RXTXPort:setflowcontrol" );
	if( tcgetattr( fd, &ttyset ) ) goto fail;
	
	if ( flowmode & ( FLOWCONTROL_RTSCTS_IN | FLOWCONTROL_RTSCTS_OUT ) )
	{
		ttyset.c_cflag |= HARDWARE_FLOW_CONTROL;
	}
	else ttyset.c_cflag &= ~HARDWARE_FLOW_CONTROL;

	ttyset.c_iflag &= ~IXANY;

	if ( flowmode & FLOWCONTROL_XONXOFF_IN )
	{
		ttyset.c_iflag |= IXOFF;
	}
	else ttyset.c_iflag &= ~IXOFF;

	if ( flowmode & FLOWCONTROL_XONXOFF_OUT )
	{
		
		ttyset.c_iflag |= IXON;
	}
	else ttyset.c_iflag &= ~IXON;
/* TRENT */
	if( tcsetattr( fd, TCSANOW, &ttyset ) ) goto fail;
	LEAVE( "RXTXPort:setflowcontrol" );
	return;
fail:
	LEAVE( "RXTXPort:setflowcontrol" );
	snprintf( msg, 79, "%s%s", "flow control type not supported", " in setflowcontrol" );
	throw new java::io::IOException( JvNewStringUTF( msg ) );
	return;
}

/*----------------------------------------------------------
unlock_monitor_thread

   accept:      event_info_struct
   perform:     unlock the monitor thread so event notification can start.
   return:      none
   exceptions:  none
   comments:    Events can be missed otherwise.
----------------------------------------------------------*/

void unlock_monitor_thread(  gnu::io::RXTXPort *p, struct event_info_struct *eis )
{
	p->MonitorThreadLock = false;
}

/*----------------------------------------------------------
check_line_status_register

   accept:      event_info_struct
   perform:     check for changes on the LSR
   return:      0 on success
   exceptions:  none
   comments:    not supported on all devices/drivers.
----------------------------------------------------------*/
int check_line_status_register( gnu::io::RXTXPort *p, struct event_info_struct *eis )
{
#ifdef TIOCSERGETLSR
	struct stat fstatbuf;

	if( ! eis->eventflags[SPE_OUTPUT_BUFFER_EMPTY] )
	{
		report( "check_line_status_registe OUPUT_BUFFER_EMPTY not set\n" );
		return 0;
	}
	if ( fstat( eis->fd, &fstatbuf ) )
	{
		report( "check_line_status_register: fstat\n" );
		return( 1 );
	}
	if( ioctl( eis->fd, TIOCSERGETLSR, &eis->change ) )
	{
		report( "check_line_status_register: TIOCSERGETLSR\n is nonnull\n" );
		return( 1 );
	}
	else if( eis && eis->change )
	{
		report_verbose( "check_line_status_register: sending OUTPUT_BUFFER_EMPTY\n" );
		send_event( p, eis, SPE_OUTPUT_BUFFER_EMPTY, 1 );
	}
#else
/*
	printf("test %i\n",  eis->output_buffer_empty_flag );
*/
	if( eis && eis->output_buffer_empty_flag == 1 && 
		eis->eventflags[SPE_OUTPUT_BUFFER_EMPTY] )
	{
		report_verbose("check_line_status_register: sending SPE_OUTPUT_BUFFER_EMPTY\n");
		send_event( p, eis, SPE_OUTPUT_BUFFER_EMPTY, 1 );
/*
		send_event( this, eis, SPE_DATA_AVAILABLE, 1 );
*/
		eis->output_buffer_empty_flag = 0;
	}
#endif /* TIOCSERGETLSR */
	return( 0 );
}

/*----------------------------------------------------------
has_line_status_register_access

   accept:      fd of interest
   perform:     check for access to the LSR
   return:      0 if not available
   exceptions:  none
   comments:    not supported on all devices/drivers.
		JK00: work around for multiport cards without TIOCSERGETLSR 
		Cyclades is one of those :-(
----------------------------------------------------------*/
int has_line_status_register_access( int fd )
{
#if defined(TIOCSERGETLSR)
	int change;

	if( !ioctl( fd, TIOCSERGETLSR, &change ) ) {
		return(1);
	}
#endif /* TIOCSERGETLSR */
	report( "has_line_status_register_acess: Port does not support TIOCSERGETLSR\n" );
	return( 0 );
}

/*----------------------------------------------------------
check_cgi_count

   accept:      fd of interest
   perform:     check for access to TIOCGICOUNT
   return:      0 if not available
   exceptions:  none
   comments:    not supported on all devices/drivers.
	 *	wait for RNG, DSR, CD or CTS  but not DataAvailable
	 *      The drawback here is it never times out so if someone
	 *      reads there will be no chance to try again.
	 *      This may make sense if the program does not want to
	 *      be notified of data available or errors.
	 *	ret=ioctl(fd,TIOCMIWAIT);
----------------------------------------------------------*/
void check_cgi_count( gnu::io::RXTXPort *p, struct event_info_struct *eis )
{
#if defined(TIOCGICOUNT)

	/* JK00: only use it if supported by this port */

	struct serial_icounter_struct sis;
	memcpy( &sis, &eis->osis, sizeof( struct serial_icounter_struct ) );

	if( ioctl( eis->fd, TIOCGICOUNT, &sis ) )
	{
		report( "check_cgi_count: TIOCGICOUNT\n is not 0\n" );
		return;
	}
	while( eis && sis.frame != eis->osis.frame ) {
		send_event( p, eis, SPE_FE, 1);
		eis->osis.frame++;
	}
	while( eis && sis.overrun != eis->osis.overrun ) {
		send_event( p, eis, SPE_OE, 1);
		eis->osis.overrun++;
	}
	while( eis && sis.parity != eis->osis.parity ) {
		send_event( p, eis, SPE_PE, 1);
		eis->osis.parity++;
	}
	while( eis && sis.brk != eis->osis.brk ) {
		send_event( p, eis, SPE_BI, 1);
		eis->osis.brk++;
	}
	if( eis )
		memcpy( &eis->osis, &sis, sizeof( struct serial_icounter_struct ) );
#endif /*  TIOCGICOUNT */
}

/*----------------------------------------------------------
port_has_changed_fionread

   accept:      fd of interest
   perform:     check if FIONREAD has changed
   return:      0 if no data available
   exceptions:  none
   comments:    
----------------------------------------------------------*/
int port_has_changed_fionread( struct event_info_struct *eis )
{
	int change, rc;
	char message[80];

	rc = ioctl( eis->fd, FIONREAD, &change );
	sprintf( message, "port_has_changed_fionread: change is %i ret is %i\n", change, eis->ret );
#if defined(__unixware__) || defined(__sun__)
	/*
	   On SCO OpenServer FIONREAD always fails for serial devices,
	   so rely upon select() result to know whether data available.

	   This is true for Solaris, also.  taj.
	*/
	if( (rc != -1 && change) || (rc == -1 && eis->ret > 0) ) 
		return( 1 );
#else
	sprintf( message, "port_has_changed_fionread: change is %i\n", change );
	report_verbose( message );
	if( change )
		return( 1 );
#endif /* __unixware__  || __sun__ */
	return( 0 );
}
		
/*----------------------------------------------------------
check_tiocmget_changes

   accept:      event_info_struct
   perform:     use TIOCMGET to report events
   return:      none
   exceptions:  none
   comments:    not supported on all devices/drivers.
----------------------------------------------------------*/
void check_tiocmget_changes( gnu::io::RXTXPort *p, struct event_info_struct * eis )
{
	unsigned int mflags = 0;
	int change;

	/* DORITO */
	if( !eis ) return;
	change  = eis->change;

	report_verbose("entering check_tiocmget_changes\n");
	if( ioctl( eis->fd, TIOCMGET, &mflags ) )
	{
		report( "=======================================\n");
		report( "check_tiocmget_changes: ioctl(TIOCMGET)\n" );
		return;
	}

	change = (mflags&TIOCM_CTS) - (eis->omflags&TIOCM_CTS);
	if( eis && change ) send_event( p, eis, SPE_CTS, change );

	change = (mflags&TIOCM_DSR) - (eis->omflags&TIOCM_DSR);
	if( eis && change )
	{
		report( "sending DSR ===========================\n");
		send_event( p, eis, SPE_DSR, change );
	}

	change = (mflags&TIOCM_RNG) - (eis->omflags&TIOCM_RNG);
	if( eis && change ) send_event( p, eis, SPE_RI, change );

	change = (mflags&TIOCM_CD) - (eis->omflags&TIOCM_CD);
	if( eis && change ) send_event( p, eis, SPE_CD, change );

	if( eis )
		eis->omflags = mflags;
	report_verbose("leaving check_tiocmget_changes\n");
}

/*----------------------------------------------------------
system_wait

   accept:      
   perform:     
   return:      
   exceptions:  none
   comments:    
----------------------------------------------------------*/
void system_wait()
{
#if defined (__sun__ )
	struct timespec retspec, tspec;
	retspec.tv_sec = 0;
	retspec.tv_nsec = 100000000;
	do {
		tspec = retspec;
		nanosleep( &tspec, &retspec );
	} while( tspec.tv_nsec != 0 );
/* Trent
*/
#else
#ifdef TRENT_IS_HERE_DEBUGGING_THREADS
	/* On NT4 The following was observed in a intense test:
		50000   95%   179 sec
		200000  95%   193 sec
		1000000 95%   203 sec	some callback failures sometimes.
		2000000 0-95% 		callback failures.
	*/
#endif /* TRENT_IS_HERE_DEBUGGING_THREADS */
#endif /* __sun__ */
}

/*----------------------------------------------------------
driver_has_tiocgicount

   accept:      fd of interest
   perform:     check for access to TIOCGICOUNT
   return:      0 if not available
   exceptions:  none
   comments:    not supported on all devices/drivers.
		Some multiport serial cards do not implement TIOCGICOUNT ... 
		So use the 'dumb' mode to enable using them after all! JK00
----------------------------------------------------------*/
int driver_has_tiocgicount( struct event_info_struct * eis )
{
#if defined(TIOCGICOUNT)

	/* Some multiport serial cards do not implement TIOCGICOUNT ... */
	/* So use the 'dumb' mode to enable using them after all! JK00 */

	if( ioctl( eis->fd, TIOCGICOUNT, &eis->osis ) < 0 ) {
		report_verbose( " driver_has_tiocgicount:  Port does not support TIOCGICOUNT events\n" );
		return(0);
	}
	else
		return(1);
#endif /*  TIOCGICOUNT */
	return(0);
	
}

/*----------------------------------------------------------
report_serial_events

   accept:      event_info_struct
   perform:     send events if they occured
   return:      0 if not available
   exceptions:  none
   comments:    not supported on all devices/drivers.
----------------------------------------------------------*/
void report_serial_events( gnu::io::RXTXPort *p, struct event_info_struct *eis )
{
	/* JK00: work around for Multi IO cards without TIOCSERGETLSR */
	/* if( eis->has_tiocsergetlsr ) we have a fix for output empty */
		if( check_line_status_register( p, eis ) )
			return;

#ifndef WIN32 /* something is wrong here */
	if ( eis && eis->has_tiocgicount )
		check_cgi_count( p, eis );
#endif /* WIN32 */

	check_tiocmget_changes( p, eis );

	if( eis && port_has_changed_fionread( eis ) )
	{
		if(!eis->eventflags[SPE_DATA_AVAILABLE] )
		{
			report_verbose("report_serial_events: ignoring DATA_AVAILABLE\n");
/*
			report(".");
*/
#if !defined(__sun__)
	/* FIXME: No time to test on all OS's for production */
			usleep(20000);
#endif /* !__sun__ */
			return;
		}
		report("report_serial_events: sending DATA_AVAILABLE\n");
		if(!send_event( p, eis, SPE_DATA_AVAILABLE, 1 ))
		{
			/* select wont block */
#if !defined(__sun__)
	/* FIXME: No time to test on all OS's for production */
			usleep(20000);
#endif /* !__sun__ */
/*
			system_wait();
*/
		}
	}
}

/*----------------------------------------------------------
initialise_event_info_struct

   accept:      event_info_struct for this thread.
   perform:     initialise or reset the event_info_struct
   return:      1 on success
   exceptions:  none
   comments:    
----------------------------------------------------------*/
int initialise_event_info_struct(  gnu::io::RXTXPort *p, struct event_info_struct *eis )
{
	int i;
	struct event_info_struct *index = master_index;

	if ( eis->initialised == 1 )
		goto end;

#ifdef TIOCGICOUNT
	memset(&eis->osis,0,sizeof(eis->osis));
#endif /* TIOCGICOUNT */

	if( index )
	{
		while( index->next )
		{
			index = index->next;
		}
		index->next = eis;
		eis->prev = index;
		eis->next = NULL;
	}
	else
	{
		master_index = eis;
		master_index->next = NULL;
		master_index->prev = NULL;
	}

	for( i = 0; i < 11; i++ ) eis->eventflags[i] = 0;
#if !defined(TIOCSERGETLSR) && !defined(WIN32)
	eis->output_buffer_empty_flag = 0;
	eis->writing = 0;
#endif /* TIOCSERGETLSR */
	eis->eventloop_interrupted = 0;
	eis->closing = 0;

	eis->fd = ( int ) p->fd;
	eis->has_tiocsergetlsr = has_line_status_register_access( eis->fd );
	eis->has_tiocgicount = driver_has_tiocgicount( eis );

	if( ioctl( eis->fd, TIOCMGET, &eis->omflags) < 0 ) {
		report( "initialise_event_info_struct: Port does not support events\n" );
	}

end:
	FD_ZERO( &eis->rfds );
	FD_SET( eis->fd, &eis->rfds );
	eis->tv_sleep.tv_sec = 0;
	eis->tv_sleep.tv_usec = 1000;
	eis->initialised = 1;
	return( 1 );
}

/*----------------------------------------------------------
finalize_event_info_struct

   accept:      event_info_struct for this thread.
   perform:     free resources
   return:      none
   exceptions:  none
   comments:    
----------------------------------------------------------*/
void finalize_event_info_struct( struct event_info_struct *eis )
{
	if( eis->next && eis->prev ) 
	{
		eis->prev->next = eis->next;
		eis->next->prev = eis->prev;
	}
	else if( eis->next )
	{
		eis->next->prev = NULL;
		master_index = eis->next;
	}
	else if( eis->prev )
		eis->prev->next = NULL;
	else master_index = NULL;
}

/*----------------------------------------------------------
RXTXPort.eventLoop

   accept:      none
   perform:     periodically check for SerialPortEvents
   return:      none
   exceptions:  none
   comments:	please keep this function clean.
----------------------------------------------------------*/
void gnu::io::RXTXPort::eventLoop(
)
{
	struct event_info_struct eis;
	eis.initialised = 0;

	ENTER( "eventLoop\n" );
	if ( !initialise_event_info_struct( this, &eis ) ) goto end;
	if ( !init_threads( this, &eis ) ) goto end;
	unlock_monitor_thread( this, &eis );
	do{
		report_time_eventLoop( );
		do {
			/* report( "." ); */
			eis.ret = SELECT( eis.fd + 1, &eis.rfds, NULL, NULL,
					&eis.tv_sleep );
			/* nothing goes between this call and select */
			if( eis.closing )
			{
				report("eventLoop: got interrupt\n");
				finalize_threads( &eis );
				finalize_event_info_struct( &eis );
				this->MonitorThreadCloseLock = false;
				LEAVE("eventLoop");
				return;
			}
			usleep(20000);
/*
			 Trent system_wait();
*/
		}  while ( eis.ret < 0 && errno == EINTR );
		if( eis.ret >= 0 )
		{
			report_serial_events( this, &eis );
		}
		initialise_event_info_struct( this, &eis );
	} while( 1 );
end:
	LEAVE( "eventLoop:  Bailing!\n" );
}

/*----------------------------------------------------------
RXTXCommDriver.nativeGetVersion

   accept:      none
   perform:     return the current version 
   return:      version
   exceptions:  none
   comments:    This is used to avoid mixing versions of the .jar and
		native library.
		First introduced in rxtx-1.5-9
		
----------------------------------------------------------*/
jstring gnu::io::RXTXCommDriver::nativeGetVersion (
)
{
	return JvNewStringUTF( "RXTX-2.1-7" );
}

/*----------------------------------------------------------
RXTXCommDriver.testRead

   accept:      jstr The device to be tested
   perform:     test if the device can be read from
   return:      true if the device can be read from
   exceptions:  none
   comments:    From Wayne Roberts wroberts1@home.com
   		check tcget/setattr returns.
		support for non serial ports Trent
----------------------------------------------------------*/

jboolean  gnu::io::RXTXCommDriver::testRead(
	jstring jstr,
	jint port_type
)
{
	struct termios ttyset;
	char c;
	int i, fd, ret = true, pid = -1;
	//char filename[80];
	char filename[80];
#ifdef TRENT_IS_HERE_DEBUGGING_ENUMERATION
	char message[80];
#endif /* TRENT_IS_HERE_DEBUGGING_ENUMERATION */

	for( i=0;i<jstr->length();i++)
		filename[i] = jstr->charAt(i);
	filename[i] = '\0';
   	//java::lang::System::out->println( filename );
	//const char *filename = (const char *) jstr->toCharArray();

	/* We opened the file in this thread, use this pid to unlock */
   	//java::lang::System::out->println( jstr );
#ifndef WIN32
	pid = getpid();
#else
	char full_windows_name[80];
#endif /* WIN32 */

	ENTER( "RXTXPort:testRead" );
	//printf( "RXTXCommDriver::testRead( %s )\n", filename );
#ifdef TRENT_IS_HERE_DEBUGGING_ENUMERATION
	/* vmware lies about which ports are there causing irq conflicts */
	/* this is for testing only */
	if( !strcmp( filename, "COM1" ) || !strcmp( filename, "COM2") )
	{
		printf("%s is good\n",filename);
		sprintf( message, "testRead: %s is good!\n", filename );
		report( message );
		return( true );
	}
	return( false );
#endif /* TRENT_IS_HERE_DEBUGGING_ENUMERATION */
#ifdef WIN32
	strcpy( full_windows_name, DEVICEDIR );
	strcat( full_windows_name, filename );
	ret = serial_test((char *) full_windows_name );
	return(ret);
#endif /* WIN32 */

	/* 
		LOCK is one of three functions defined in SerialImp.h

			uucp_lock		Solaris
			fhs_lock		Linux
			system_does_not_lock	Win32
	*/
	//printf( "RXTXCommDriver::testRead( %s )\n", filename );

	if ( LOCK( filename, pid ) )
	{
		LEAVE( "RXTXPort:testRead no lock" );
		return false;
	}

	/*
           CLOCAL eliminates open blocking on modem status lines
           -- changed to O_NONBLOCK
	*/
	do {
		fd=::OPEN ( filename, O_RDWR | O_NOCTTY | O_NONBLOCK );
	}  while ( fd < 0 && errno==EINTR );

	if( fd < 0 )
	{
		report_verbose( "testRead() open failed\n" );
		ret = false;
		goto END;
	}

	if ( port_type == PORT_SERIAL )
	{
		int saved_flags;
		struct termios saved_termios;

		if (tcgetattr(fd, &ttyset) < 0) {
			ret = false;
			goto END;
		}

		/* save, restore later */
		if ( ( saved_flags = fcntl(fd, F_GETFL ) ) < 0 )
		{
			report( "testRead() fcntl(F_GETFL) failed\n" );
			ret = false;
			goto END;
		}

		memcpy( &saved_termios, &ttyset, sizeof( struct termios ) );

		if ( fcntl( fd, F_SETFL, O_NONBLOCK ) < 0 )
		{
			report( "testRead() fcntl(F_SETFL) failed\n" );
			ret = false;
			goto END;
		}

		cfmakeraw(&ttyset);
		ttyset.c_cc[VMIN] = ttyset.c_cc[VTIME] = 0;

		if ( tcsetattr( fd, TCSANOW, &ttyset) < 0 )
		{
			report( "testRead() tcsetattr failed\n" );
			ret = false;
			tcsetattr( fd, TCSANOW, &saved_termios );
			goto END;
		}

/*

              The following may mess up if both EAGAIN and EWOULDBLOCK
              are defined but only EWOULDBLOCK is used

              Linux:

              man 2 open
              O_NONBLOCK or O_NDELAY
              When  possible,  the file is opened in non-blocking
              mode. Neither the open nor  any  subsequent  opera­
              tions on the file descriptor which is returned will
              cause the calling process to wait.   For  the  han­
              dling  of  FIFOs  (named  pipes), see also fifo(4).
              This mode need not have any effect on  files  other
              than FIFOs.

	      man 2 read
              EAGAIN
              Non-blocking I/O has been selected using O_NONBLOCK
              and no data was immediately available for  reading.


              /usr/include/asm/error.h:
              #define EAGAIN          11      / Try again /
              #define EWOULDBLOCK     EAGAIN  / Operation would block /

              looks like the kernel is using EAGAIN

              -- should be OK

              Solaris:

              man 2 open
              EAGAIN    The path  argument  names  the  slave  side  of  a
              pseudo-terminal device that is locked.

              man 2 read
              If O_NONBLOCK is set, read() returns -1 and sets errno
              to EAGAIN.

              -- should be OK.

              HP-UX

              both are defined but EAGAIN is used.

              -- should be OK.

              Win32

              neither errno is currently set.  Comment added to termios.c
              serial_open().

              -- should be OK

Steven's book.  Advanced programming in the Unix Environment pg 364

"A common use for nonblocking I/O is for dealing with a terminal device 
for a network connection and these devices are normally used by one process
at a time.  This means that the change in the BSD semantics normally does 't 
effect us.  The different error return, EWOULDBLOCK, instead of POSIX.1 
EAGAIN, continues to be a portability difference that we must deal with."

*/

		if ( READ( fd, &c, 1 ) < 0 )
		{
#ifdef EAGAIN
			if ( errno != EAGAIN ) {
				report( "testRead() read failed\n" );
				ret = false;
			}
#else                      
#ifdef EWOULDBLOCK
			if ( errno != EWOULDBLOCK )
			{
				report( "testRead() read failed\n" );
				ret = false;
			}
#else
			ret = false;
#endif /* EWOULDBLOCK */
#endif /* EAGAIN */
		}

		/* dont walk over unlocked open devices */
		tcsetattr( fd, TCSANOW, &saved_termios );
		fcntl( fd, F_SETFL, saved_flags );
	}

	/* 
		UNLOCK is one of three functions defined in SerialImp.h

			uucp_unlock		Solaris
			fhs_unlock		Linux
			system_does_not_unlock	Win32
	*/

END:
	UNLOCK(filename, pid );
	::CLOSE( fd );
	LEAVE( "RXTXPort:testRead" );
	return ret;
}

#if defined(__APPLE__)
/*----------------------------------------------------------
 createSerialIterator()
   accept:      
   perform:     
   return:      
   exceptions:  
   comments:
		Code courtesy of Eric Welch at Keyspan, except for the bugs
		which are courtesy of Joseph Goldstone (joseph@lp.com)
----------------------------------------------------------*/

kern_return_t
createSerialIterator(io_iterator_t *serialIterator)
{
    kern_return_t    kernResult;
    mach_port_t        masterPort;
    CFMutableDictionaryRef    classesToMatch;
    if ((kernResult=IOMasterPort(NULL, &masterPort)) != KERN_SUCCESS)
    {
	printf( "IOMasterPort returned %d\n", kernResult);
	return kernResult;
    }
    if ((classesToMatch = IOServiceMatching(kIOSerialBSDServiceValue)) == NULL)
    {
	printf( "IOServiceMatching returned NULL\n" );
	return kernResult;
    }
    CFDictionarySetValue(classesToMatch, CFSTR(kIOSerialBSDTypeKey), CFSTR(kIOSerialBSDAllTypes));
    kernResult = IOServiceGetMatchingServices(masterPort, classesToMatch, serialIterator);
    if (kernResult != KERN_SUCCESS)
    {
	printf( "IOServiceGetMatchingServices returned %d\n", kernResult);
    }
    return kernResult;
}

/*----------------------------------------------------------
 getRegistryString()

   accept:      
   perform:     
   return:      
   exceptions:  
   comments:
		Code courtesy of Eric Welch at Keyspan, except for the bugs
		which are courtesy of Joseph Goldstone (joseph@lp.com)
----------------------------------------------------------*/
char *
getRegistryString(io_object_t sObj, char *propName)
{
    static char resultStr[256];
    CFTypeRef   nameCFstring;
    resultStr[0] = 0;
    nameCFstring = IORegistryEntryCreateCFProperty(sObj,
            CFStringCreateWithCString(kCFAllocatorDefault, propName, kCFStringEncodingASCII),
                                                   kCFAllocatorDefault, 0);
    if (nameCFstring)
    {
        CFStringGetCString(nameCFstring, resultStr, sizeof(resultStr), kCFStringEncodingASCII);
        CFRelease(nameCFstring);
    }
    return resultStr;
}

/*----------------------------------------------------------
 registerKnownSerialPorts()
   accept:      
   perform:     
   return:      
   exceptions:  
   comments:
----------------------------------------------------------*/
int
registerKnownSerialPorts(
	jint portType
) /* dima */
{
    io_iterator_t    theSerialIterator;
    io_object_t      theObject;
    int              numPorts = 0;/* dima it should initiated */
    if (createSerialIterator(&theSerialIterator) != KERN_SUCCESS)
    {
        printf( "createSerialIterator failed\n" );
    } else {
            while (theObject = IOIteratorNext(theSerialIterator))
            {
 /* begin dima  taj got this close for gcj */
            	jstring	tempJstring;
		tempJstring = JvNewStringUTF(getRegistryString(theObject, kIODialinDeviceKey));
		gnu::io::ComPortIdentifier->addPortName(tempJstring,portType,this);/* dima */
                numPorts++;

 		tempJstring = JvNewStringUTF(getRegistryString(theObject, kIOCalloutDeviceKey));
		gnu::io::ComPortIdentifier->addPortName(tempJstring,portType,this);/* dima */
                numPorts++;
/* end dima  taj got this close for gcj */
            }
        }
    }
    return numPorts;
}
#endif /* __APPLE__ */


/*----------------------------------------------------------
 registerKnownPorts

   accept:      the type of port
   perform:     register any ports of the desired type a priori known to this OS
   return:      true if any such ports were registered otherwise false
   exceptions:  none
   comments:
----------------------------------------------------------*/
jboolean gnu::io::RXTXCommDriver::registerKnownPorts(
	jint portType
)
{
	enum {PORT_TYPE_SERIAL = 1,
		PORT_TYPE_PARALLEL,
		PORT_TYPE_I2C,
		PORT_TYPE_RS485,
		PORT_TYPE_RAW};
	jboolean result = false;
	char message[80];

	switch(portType) {
		case PORT_TYPE_SERIAL:
#if defined(__APPLE__)
			if (registerKnownSerialPorts( PORT_TYPE_SERIAL) > 0)
			{/* dima */
				result = true;
			}
#endif
           		 break;
		case PORT_TYPE_PARALLEL: break;
		case PORT_TYPE_I2C:      break;
		case PORT_TYPE_RS485:    break;
		case PORT_TYPE_RAW:      break;
		default:
			sprintf( message, "unknown portType %d handed to \
				native RXTXCommDriver.registerKnownPorts() \
				 method.\n",
				(int) portType
			);
			report( message );
	}
	return result;
}
    
/*----------------------------------------------------------
 isPortPrefixValid

   accept:      a port prefix
   perform:     see if the port prefix matches a port that is valid on this OS.
   return:      true if it exists otherwise false
   exceptions:  none
   comments:
----------------------------------------------------------*/
jboolean  gnu::io::RXTXCommDriver::isPortPrefixValid(
	jstring jstr
)
{
	jboolean result;
	static struct stat mystat;
	char teststring[256];
	int fd,i;
	char filename[80];

	for( i=0;i<jstr->length();i++)
		filename[i] = jstr->charAt(i);
	filename[i] = '\0';

	ENTER( "RXTXCommDriver:isPortPrefixValid" );
	for(i=0;i<64;i++){
#if defined(__sun__)
		/* Solaris uses /dev/cua/a instead of /dev/cua0 */
		if( i > 25 ) break;
		sprintf(teststring,"%s%s%c",DEVICEDIR, name, i + 97 );
		fprintf(stderr, "testing: %s\n", teststring);
#else 
#if defined(_GNU_SOURCE)
		snprintf(teststring, 256, "%s%s%i",DEVICEDIR,filename, i);
#else
		sprintf(teststring,"%s%s%i",DEVICEDIR,filename, i);
#endif /* _GNU_SOURCE */
		stat(teststring,&mystat);
#endif /* __sun__ */
/* XXX the following hoses freebsd when it tries to open the port later on */
#ifndef __FreeBSD__
		if(S_ISCHR(mystat.st_mode)){
			fd=::OPEN(teststring,O_RDONLY|O_NONBLOCK);
			if (fd>0){
				::CLOSE(fd);
				result=true;
				break;
			}
			else
				result=false;
		}
		else
			result=false;
#else
		result=true;
#endif  /* __FreeBSD __ */
	}
#if defined(_GNU_SOURCE)
	snprintf(teststring, 256, "%s%s",DEVICEDIR,filename);
#else
	sprintf(teststring,"%s%s",DEVICEDIR,filename);
#endif /* _GNU_SOURCE */
	stat(teststring,&mystat);
	if(S_ISCHR(mystat.st_mode)){
		fd=::OPEN(teststring,O_RDONLY|O_NONBLOCK);
		if (fd>0){
			::CLOSE(fd);
			result=true;
		}
	}
	LEAVE( "RXTXCommDriver:isPortPrefixValid" );
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

jstring gnu::io::RXTXCommDriver::getDeviceDirectory(
)
{
	ENTER( "RXTXCommDriver:getDeviceDirectory" );
	return JvNewStringUTF(DEVICEDIR);
	LEAVE( "RXTXCommDriver:getDeviceDirectory" );
}

#ifdef GOING_TO_MESS_WITH_BUFFERS
/*----------------------------------------------------------
 setInputBufferSize

   accept:      
   perform:     
   return:      none
   exceptions:  none
   comments:    see fopen/fclose/fwrite/fread man pages.
----------------------------------------------------------*/
void gnu::io::RXTXPort::setInputBufferSize(
	jint size
)
{
	report( "setInputBufferSize is not implemented\n" );
}

/*----------------------------------------------------------
 getIputBufferSize

   accept:      
   perform:     
   return:      none
   exceptions:  none
   comments:    see fopen/fclose/fwrite/fread man pages.
----------------------------------------------------------*/
jint gnu::io::RXTXPort::getInputBufferSize(
)
{
	report( "getInputBufferSize is not implemented\n" );
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
void gnu::io::RXTXPort::setOutputBufferSize(
	jint size
)
{
	report( "setOutputBufferSize is not implemented\n" );
}

/*----------------------------------------------------------
 getOutputBufferSize

   accept:      
   perform:     
   return:      none
   exceptions:  none
   comments:    see fopen/fclose/fwrite/fread man pages.
----------------------------------------------------------*/
jint gnu::io::RXTXPort::getOutputBufferSize()
{
	report( "getOutputBufferSize is not implemented\n" );
	return(1);
}

#endif /* GOING_TO_MESS_WITH_BUFFERS */

/*----------------------------------------------------------
 interruptEventLoop

   accept:      nothing
   perform:     increment eventloop_interrupted 
   return:      nothing
   exceptions:  none
   comments:    all eventloops in this PID will check if their thread
		is interrupted.  When all the interrupted threads exit
		they will decrement the var leaving it 0.
		the remaining threads will continue.
----------------------------------------------------------*/
void gnu::io::RXTXPort::interruptEventLoop(
)
{
	struct event_info_struct *index = master_index;
	int fd = ( int ) this->fd;
	int searching = 1;


	while( searching )
	{
		index = master_index;
		if( index )
		{
			while( index->fd != fd &&
				index->next ) index = index->next;
			if ( index->fd == fd ) searching = 0;
		}
		else
			report("x");
		if( searching )
		{
			report("@");
			usleep(1000);
		}
	}
	index->eventloop_interrupted = 1;
	/*
	Many OS's need a thread running to determine if output buffer is
	empty.  For Linux and Win32 it is not needed.  So closing is used to
	shut down the thread in the write order on OS's that don't have
	kernel support for output buffer empty.

	In rxtx TIOCSERGETLSR is defined for win32 and Linux
	*/
#ifdef TIOCSERGETLSR
	index->closing=1;
#endif /* TIOCSERGETLSR */
#ifdef WIN32
	termios_interrupt_event_loop( index->fd, 1 );
#endif /* WIN32 */
	report("interruptEventLoop: interrupted\n");
}

/*----------------------------------------------------------
 is_interrupted

   accept:      event_info_struct
   perform:     see if the port is being closed.
   return:      a positive value if the port is being closed.
   exceptions:  none
   comments:
----------------------------------------------------------*/
jboolean is_interrupted( gnu::io::RXTXPort *p, struct event_info_struct *eis )
{
	int result;

	ENTER( "is_interrupted" );
	result = p->checkMonitorThread( );
#ifdef DEBUG
	if((*env)->ExceptionOccurred(env)) {
		report ( "is_interrupted: an error occured calling sendEvent()\n" );
		(*env)->ExceptionDescribe(env);
		(*env)->ExceptionClear(env);
	}
#endif /* DEBUG */
	LEAVE( "RXTXCommDriver:is_interrupted" );
	return(result);
}

/*----------------------------------------------------------
 nativeSetEventFlag

   accept:      fd for finding the struct, event to flag, flag.
   perform:     toggle the flag
   return:      none
   exceptions:  none
   comments:	all the logic used to be done in Java but its too noisy
----------------------------------------------------------*/
void gnu::io::RXTXPort::nativeSetEventFlag(
	jint fd,
	jint event,
	jboolean flag
)
{
	struct event_info_struct *index = master_index;

	if( !index )
	{
		report_error("nativeSetEventFlag !index\n");
		return;
	}
	while( index->fd != fd && index->next )
	{
		index = index->next;
	}
	if( index->fd != fd )
	{
		report_error("nativeSetEventFlag !fd\n");
		return;
	}
	index->eventflags[event] = (int) flag;
#ifdef WIN32
	termios_setflags( fd, index->eventflags );
#endif /* win32 */
	
}

/*----------------------------------------------------------
 send_event

   accept:      event_info_structure, event type and true/false
   perform:     if state is > 0 send a true event otherwise send false
   return:      a positive value if the port is being closed.
   exceptions:  none
   comments:
----------------------------------------------------------*/
int send_event( gnu::io::RXTXPort *p, struct event_info_struct *eis, jint type, int flag )
{
	int result;

	ENTER( "send_event" );
	if( !eis || eis->eventloop_interrupted > 1 )
	{
		report("event loop interrupted\n");
		return true;
	}
	report_verbose("send_event: !eventloop_interupted\n");

#ifdef TODO /* FIXME jcj hack */
	(*env)->ExceptionClear(env);
#endif /* FIXME jcj hack */

	report_verbose("send_event: calling\n");
	result = p->sendEvent( type, flag > 0 ? true : false );
	report_verbose("send_event: called\n");

#ifdef asdf
	if(!eis || (*eis->env)->ExceptionOccurred(eis->env)) {
		report ( "send_event: an error occured calling sendEvent()\n" );
		(*eis->env)->ExceptionDescribe(eis->env);
		(*eis->env)->ExceptionClear(eis->env);
	}
#endif /* asdf */
	/* report("e"); */
	LEAVE( "send_event" );
	return(result);
}

/*----------------------------------------------------------
 report_warning

   accept:      string to send to report as an message
   perform:     send the string to stderr or however it needs to be reported.
   return:      none
   exceptions:  none
   comments:
----------------------------------------------------------*/
void report_warning(char *msg)
{
#ifndef DEBUG_MW
	fprintf(stderr, msg);
#else
	mexWarnMsgTxt( (const char *) msg );
#endif /* DEBUG_MW */
}

/*----------------------------------------------------------
 report_verbose

   accept:      string to send to report as an verbose message
   perform:     send the string to stderr or however it needs to be reported.
   return:      none
   exceptions:  none
   comments:
----------------------------------------------------------*/
void report_verbose(char *msg)
{
#ifdef DEBUG_VERBOSE
#ifdef DEBUG_MW
	mexErrMsgTxt( msg );
#else
	fprintf(stderr, msg);
#endif /* DEBUG_MW */
#endif /* DEBUG_VERBOSE */
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
#ifndef DEBUG_MW
	fprintf(stderr, msg);
#else
	mexWarnMsgTxt( msg );
#endif /* DEBUG_MW */
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
#	ifndef DEBUG_MW
		fprintf(stderr, msg);
#	else
		mexPrintf( msg );
#	endif /* DEBUG_MW */
#endif /* DEBUG */
}

#ifndef WIN32
#ifdef LFS
/*----------------------------------------------------------
 lfs_lock

   accept:      The name of the device to try to lock
   perform:     Create a lock file if there is not one already using a
                lock file server.
   return:      1 on failure 0 on success
   exceptions:  none
   comments:    

----------------------------------------------------------*/
int lfs_lock( const char *filename, int pid )
{
	int s;
	int ret;
	int size = 1024;
	char *buffer = malloc(size);
	struct sockaddr_in addr;

	if ( !( s = socket( AF_INET, SOCK_STREAM, 0 ) ) > 0 )
		return 1;
	addr.sin_family = AF_INET;
	addr.sin_port = htons( 50001 );
	addr.sin_addr.s_addr = inet_addr( "127.0.0.1" );
   
	if ( !connect( s, ( struct sockaddr * ) &addr, sizeof( addr ) ) == 0 )
		return 1;
	ret=recv( s, buffer, size, 0 );
	sprintf( buffer, "lock %s %i\n", filename, pid );
	/* printf( "%s", buffer ); */
	send( s, buffer, strlen(buffer), 0 );
	ret=recv( s, buffer, size, 0 );
	if ( ret > 0 )
	{
		buffer[ret] = '\0';
		/* printf( "Message recieved: %s", buffer ); */
	}
	send( s, "quit\n", strlen( "quit\n" ), 0 );
	close(s);
	/* printf("%s\n", buffer); */
	if( buffer[0] == '2' ) return 0;
	return 1;
}

/*----------------------------------------------------------
 lfs_unlock

   accept:      The name of the device to try to unlock
   perform:     Remove a lock file if there is one using a
                lock file server.
   return:      1 on failure 0 on success
   exceptions:  none
   comments:    

----------------------------------------------------------*/
int lfs_unlock( const char *filename, int pid )
{
	int s;
	int ret;
	int size = 1024;
	char *buffer = malloc(size);
	struct sockaddr_in addr;

	if ( !( s = socket( AF_INET, SOCK_STREAM, 0 ) ) > 0 )
		return 1;
	addr.sin_family = AF_INET;
	addr.sin_port = htons( 50001 );
	addr.sin_addr.s_addr = inet_addr( "127.0.0.1" );
   
	if ( !connect( s, ( struct sockaddr * ) &addr, sizeof( addr ) ) == 0 )
		return 1;
	sprintf( buffer, "unlock %s %i\n", filename, pid );
	/* printf( "%s", buffer ); */
	send( s, buffer, strlen(buffer), 0 );
	ret = recv( s, buffer, size, 0 );
	if ( ret > 0 )
	{
		buffer[ret] = '\0';
		/* printf( "Message recieved: %s", buffer ); */
	}
	send( s, "quit\n", strlen( "quit\n" ), 0 );
	close(s);
	if( buffer[0] == '2' ) return 0;
	return 1;
}
#endif /* LFS */

/*----------------------------------------------------------
 fhs_lock

   accept:      The name of the device to try to lock
                termios struct
   perform:     Create a lock file if there is not one already.
   return:      1 on failure 0 on success
   exceptions:  none
   comments:    This is for linux and freebsd only currently.  I see SVR4 does
                this differently and there are other proposed changes to the
		Filesystem Hierachy Standard

		more reading:

----------------------------------------------------------*/
int fhs_lock( const char *filename, int pid )
{
	/*
	 * There is a zoo of lockdir possibilities
	 * Its possible to check for stale processes with most of them.
	 * for now we will just check for the lockfile on most
	 * Problem lockfiles will be dealt with.  Some may not even be in use.
	 *
	 */
	int fd,j;
	char lockinfo[12], message[80];
	char file[80], *p;

	j = strlen( filename );
	p = ( char * ) filename + j;
	/*  FIXME  need to handle subdirectories /dev/cua/... 
	    SCO Unix use lowercase all the time
			taj
	*/
	//printf("lock file is %s\n",filename);
	while( *( p - 1 ) != '/' && j-- != 1 )
	{
#if defined ( __unixware__ )
		*p = tolower( *p );
#endif /* __unixware__ */
		p--;
	}
	sprintf( file, "%s/LCK..%s", LOCKDIR, p );
	if ( check_lock_status( filename ) )
	{
		report( "fhs_lock() lockstatus fail\n" );
		return 1;
	}
	fd = open( file, O_CREAT | O_WRONLY | O_EXCL, 0444 );
	if( fd < 0 )
	{
		sprintf( message,
			"RXTX fhs_lock() Error: creating lock file: %s: %s\n",
			file, strerror(errno) );
		report_error( message );
		return 1;
	}
	sprintf( lockinfo, "%10d\n",(int) getpid() );
	sprintf( message, "fhs_lock: creating lockfile: %s\n", lockinfo );
	report( message );
	write( fd, lockinfo, 11 );
	close( fd );
	return 0;
}

/*----------------------------------------------------------
 uucp_lock

   accept:     char * filename.  Device to be locked 
   perform:    Try to get a uucp_lock
   return:     int 0 on success
   exceptions: none 
   comments: 
		The File System Hierarchy Standard
		http://www.pathname.com/fhs/

		UUCP Lock Files
		http://docs.freebsd.org/info/uucp/uucp.info.UUCP_Lock_Files.html

		FSSTND
		ftp://tsx-11.mit.edu/pub/linux/docs/linux-standards/fsstnd/

		Proposed Changes to the File System Hierarchy Standard
		ftp://scicom.alphacdc.com/pub/linux/devlock-0.X.tgz

		"UNIX Network Programming", W. Richard Stevens,
		Prentice-Hall, 1990, pages 96-101.

		There is much to do here.

		1) UUCP style locks (done)
			/var/spool/uucp
		2) SVR4 locks
			/var/spool/locks
		3) FSSTND locks (done)
			/var/lock
		4) handle stale locks  (done except kermit locks)
		5) handle minicom lockfile contents (FSSTND?)
			"     16929 minicom root\n"  (done)
		6) there are other Lock conventions that use Major and Minor
		   numbers...
		7) Stevens recommends LCK..<pid>

		most are caught above.  If they turn out to be problematic
		rather than an exercise, we will handle them.

----------------------------------------------------------*/
int uucp_lock( const char *filename, int pid )
{
	char lockfilename[80], lockinfo[12], message[80];
	char name[80];
	int fd;
	struct stat buf;

	sprintf( message, "uucp_lock( %s );\n", filename ); 
	report( message );

	if ( check_lock_status( filename ) )
	{
		report( "RXTX uucp check_lock_status true\n" );
		return 1;
	}
	if ( stat( LOCKDIR, &buf ) != 0 )
	{
		report( "RXTX uucp_lock() could not find lock directory.\n" );
		return 1;
	}
	if ( stat( filename, &buf ) != 0 )
	{
		report( "RXTX uucp_lock() could not find device.\n" );
		sprintf( message, "uucp_lock: device was %s\n", name );
		report( message );
		return 1;
	}
	sprintf( lockfilename, "%s/LK.%03d.%03d.%03d",
		LOCKDIR,
		(int) major( buf.st_dev ),
	 	(int) major( buf.st_rdev ),
		(int) minor( buf.st_rdev )
	);
	sprintf( lockinfo, "%10d\n", (int) getpid() );
	if ( stat( lockfilename, &buf ) == 0 )
	{
		sprintf( message, "RXTX uucp_lock() %s is there\n",
			lockfilename );
		report( message );
		report_error( message );
		return 1;
	}
	fd = open( lockfilename, O_CREAT | O_WRONLY | O_EXCL, 0444 );
	if( fd < 0 )
	{
		sprintf( message,
			"RXTX uucp_lock() Error: creating lock file: %s\n",
			lockfilename );
		report_error( message );
		return 1;
	}
	write( fd, lockinfo,11 );
	close( fd );
	return 0;
}

/*----------------------------------------------------------
 check_lock_status

   accept:      the lock name in question
   perform:     Make sure everything is sane
   return:      0 on success
   exceptions:  none
   comments:    
----------------------------------------------------------*/
int check_lock_status( const char *filename )
{
	struct stat buf;
	/*  First, can we find the directory? */

	if ( stat( LOCKDIR, &buf ) != 0 )
	{
		report( "check_lock_status: could not find lock directory.\n" );
		return 1;
	}

	/*  OK.  Are we able to write to it?  If not lets bail */

	if ( check_group_uucp() )
	{
		report_error( "check_lock_status: No permission to create lock file.\nplease see: How can I use Lock Files with rxtx? in INSTALL\n" );
		return(1);
	}

	/* is the device alread locked */

	if ( is_device_locked( filename ) )
	{
		report( "check_lock_status: device is locked by another application\n" );
		return 1;	
	}
	return 0;
	
}

/*----------------------------------------------------------
 fhs_unlock

   accept:      The name of the device to unlock
   perform:     delete the lock file
   return:      none
   exceptions:  none
   comments:    This is for linux only currently.  I see SVR4 does this
                differently and there are other proposed changes to the
		Filesystem Hierachy Standard
----------------------------------------------------------*/
void fhs_unlock( const char *filename, int openpid )
{
	char file[80],*p;
	int i;

	i = strlen( filename );
	p = ( char * ) filename + i;
	/*  FIXME  need to handle subdirectories /dev/cua/... */
	while( *( p - 1 ) != '/' && i-- != 1 ) p--;
	sprintf( file, "%s/LCK..%s", LOCKDIR, p );

	if( !check_lock_pid( file, openpid ) )
	{
		unlink(file);
		report("fhs_unlock: Removing LockFile\n");
	}
	else
	{
		report("fhs_unlock: Unable to remove LockFile\n");
	}
}

/*----------------------------------------------------------
 uucp_unlock

   accept:     char *filename the device that is locked      
   perform:    remove the uucp lockfile if it exists 
   return:     none 
   exceptions: none 
   comments:   http://docs.freebsd.org/info/uucp/uucp.info.UUCP_Lock_Files.html 
----------------------------------------------------------*/
void uucp_unlock( const char *filename, int openpid )
{
	struct stat buf;
	char file[80], message[80];
	/* FIXME */

	sprintf( message, "uucp_unlock( %s );\n", filename );
	report( message );

	if ( stat( filename, &buf ) != 0 ) 
	{
		/* hmm the file is not there? */
		report( "uucp_unlock() no such device\n" );
		return;
	}
	sprintf( file, LOCKDIR"/LK.%03d.%03d.%03d",
		(int) major( buf.st_dev ),
	 	(int) major( buf.st_rdev ),
		(int) minor( buf.st_rdev )
	);
	if ( stat( file, &buf ) != 0 ) 
	{
		/* hmm the file is not there? */
		report( "uucp_unlock no such lockfile\n" );
		return;
	}
	if( !check_lock_pid( file, openpid ) )
	{ 
		sprintf( message, "uucp_unlock: unlinking %s\n", file );
		report( message );
		unlink(file);
	}
	else
	{
		sprintf( message, "uucp_unlock: unlinking failed %s\n", file );
		report( message );
	}
}

/*----------------------------------------------------------
 check_lock_pid

   accept:     the name of the lockfile 
   perform:    make sure the lock file is ours.
   return:     0 on success
   exceptions: none
   comments:   
----------------------------------------------------------*/
int check_lock_pid( const char *file, int openpid )
{
	int fd, lockpid;
	char pid_buffer[12];
	char message[80];

	fd=open( file, O_RDONLY );
	if ( fd < 0 )
	{
		return( 1 );
	}
	if ( read( fd, pid_buffer, 11 ) < 0 )
	{
		close( fd );
		return( 1 );
	}
	close( fd );
	pid_buffer[11] = '\0';
	lockpid = atol( pid_buffer );
	/* Native threads JVM's have multiple pids */
	if ( lockpid != getpid() && lockpid != getppid() && lockpid != openpid )
	{
		sprintf(message, "check_lock_pid: lock = %s pid = %i gpid=%i openpid=%i\n",
			pid_buffer, (int) getpid(), (int) getppid(), openpid );
		report( message );
		return( 1 );
	}
	return( 0 );
}

/*----------------------------------------------------------
 check_group_uucp

   accept:     none
   perform:    check if the user is root or in group uucp
   return:     0 on success 
   exceptions: none 
   comments:   
		This checks if the effective user is in group uucp so we can
		create lock files.  If not we give them a warning and bail.
		If its root we just skip the test.

		if someone really wants to override this they can use the			USER_LOCK_DIRECTORY --not recommended.

		In a recent change RedHat 7.2 decided to use group lock.
		In order to get around this we just check the group id
		of the lock directory.

		* Modified to support Debian *

		The problem was that checking the ownership of the lock file
		dir is not enough, in the sense that even if the current user
		is not in the group of the lock directory if the lock
		directory has 777 permissions the lock file can be anyway
		created.  My solution is simply to try to create a tmp file
		there and if it works then we can go on.  Here is my code that
		I tried and seems to work.

		Villa Valerio <valerio.villa@siemens.com>
----------------------------------------------------------*/
int check_group_uucp()
{
#ifndef USER_LOCK_DIRECTORY
	FILE *testLockFile ;
	char testLockFileDirName[] = LOCKDIR;
	char testLockFileName[] = "tmpXXXXXX";
	char *testLockAbsFileName;

	testLockAbsFileName = ( char * ) calloc(strlen(testLockFileDirName)
			+ strlen(testLockFileName) + 2, sizeof(char));
	if ( NULL == testLockAbsFileName )
	{
		report_error("check_group_uucp(): Insufficient memory");
		return 1;  
	}
	strcat(testLockAbsFileName, testLockFileDirName);
	strcat(testLockAbsFileName, "/");
	strcat(testLockAbsFileName, testLockFileName);
	if ( NULL == mkstemp(testLockAbsFileName) )
	{
		free(testLockAbsFileName);
		report_error("check_group_uucp(): mktemp malformed string - \
			should not happen");

		return 1;
	}
	testLockFile = fopen (testLockAbsFileName, "w+");
	if (NULL == testLockFile)
	{
		report_error("check_group_uucp(): error testing lock file \
			creation Error details: ");
		report_error(strerror(errno));

		free(testLockAbsFileName);
		return 1;
	}

	fclose (testLockFile);
	unlink (testLockAbsFileName);           
	free(testLockAbsFileName);

#endif /* USER_LOCK_DIRECTORY */
	return 0;

#ifdef USE_OLD_CHECK_GROUP_UUCP
int check_group_uucp()
{
#ifndef USER_LOCK_DIRECTORY
	int group_count;
	struct passwd *user = getpwuid( geteuid() );
	struct stat buf;
	char msg[80];
	gid_t list[ NGROUPS_MAX ];

	if( stat( LOCKDIR, &buf) )
	{
		sprintf( msg, "check_group_uucp:  Can not find Lock Directory: %s\n", LOCKDIR );
		report_error( msg );
		return( 1 );
	}
	group_count = getgroups( NGROUPS_MAX, list );
	list[ group_count ] = geteuid();

	/* JJO changes start */
	if( user == NULL )
	{
		report_error( "Not able to get user groups.\n" );
		return 1;
	} else
	/* JJO changes stop */


	if( user->pw_gid )
	{
		while( group_count >= 0 && buf.st_gid != list[ group_count ] )
		{
  			group_count--; 
		}
		if( buf.st_gid == list[ group_count ] )
			return 0;
		sprintf( msg, "%i %i\n", buf.st_gid, list[ group_count ] );
		report_error( msg );
		report_error( UUCP_ERROR );
		return 1;
	}
	return 0;
/*
	if( strcmp( user->pw_name, "root" ) )
	{
		while( *g->gr_mem )
		{
			if( !strcmp( *g->gr_mem, user->pw_name ) )
			{
				break;
			}
			(void) *g->gr_mem++;
		}
		if( !*g->gr_mem )
		{
			report( UUCP_ERROR );
			return 1;
		}
	}
*/
#endif /* USER_LOCK_DIRECTORY */
	return 0;
#endif /* USE_OLD_CHECK_GROUP_UUCP */
}

/*----------------------------------------------------------
 The following should be able to follow symbolic links.  I think the stat
 method used below will work on more systems.  This was found while looking
 for information.

 * realpath() doesn't exist on all of the systems my code has to run
   on (HP-UX 9.x, specifically)
----------------------------------------------------------
int different_from_LOCKDIR(const char* ld)
{
	char real_ld[MAXPATHLEN];
	char real_LOCKDIR[MAXPATHLEN];
	if (strncmp(ld, LOCKDIR, strlen(ld)) == 0)
		return 0;
	if (realpath(ld, real_ld) == NULL)
		return 1;
	if (realpath(LOCKDIR, real_LOCKDIR) == NULL)
		return 1;
	if (strncmp(real_ld, real_LOCKDIR, strlen(real_ld)) == 0)
		return 0;
	else
		return 1;
}
*/

/*----------------------------------------------------------
 is_device_locked

   accept:      char * filename.  The device in question including the path.
   perform:     see if one of the many possible lock files is aready there
		if there is a stale lock, remove it.
   return:      1 if the device is locked or somethings wrong.
		0 if its possible to create our own lock file.
   exceptions:  none
   comments:    check if the device is already locked
----------------------------------------------------------*/
int is_device_locked( const char *port_filename )
{
	const char *lockdirs[] = { "/etc/locks", "/usr/spool/kermit",
		"/usr/spool/locks", "/usr/spool/uucp", "/usr/spool/uucp/",
		"/usr/spool/uucp/LCK", "/var/lock", "/var/lock/modem",
		"/var/spool/lock", "/var/spool/locks", "/var/spool/uucp",
		LOCKDIR, NULL
	};
	const char *lockprefixes[] = { "LCK..", "lk..", "LK.", NULL }; 
	char *p, file[80], pid_buffer[20], message[80];
	int i = 0, j, k, fd , pid;
	struct stat buf;
	struct stat buf2;

	j = strlen( port_filename );
	p = ( char * ) port_filename+j;
	while( *( p-1 ) != '/' && j-- !=1 ) p--;

	while( lockdirs[i] )
	{
		/*
		   Look for lockfiles in all known places other than the
		   defined lock directory for this system
		   report any unexpected lockfiles.

		   Is the suspect lockdir there?
		   if it is there is it not the expected lock dir?
		*/
		if( !stat( lockdirs[i], &buf2 ) &&
			strncmp( lockdirs[i], LOCKDIR, strlen( lockdirs[i] ) ) )
		{
			j = strlen( port_filename );
			p = ( char *  ) port_filename + j;
		/*
		   SCO Unix use lowercase all the time
			taj
		*/
			while( *( p - 1 ) != '/' && j-- != 1 )
			{
#if defined ( __unixware__ )
				*p = tolower( *p );
#endif /* __unixware__ */
				p--;
			}
			k=0;
			while ( lockprefixes[k] )
			{
				/* FHS style */
				sprintf( file, "%s/%s%s", lockdirs[i],
					lockprefixes[k], p );
				if( stat( file, &buf ) == 0 )
				{
					sprintf( message, UNEXPECTED_LOCK_FILE,
						file );
					report_warning( message );
					return 1;
				}

				/* UUCP style */
				stat(port_filename , &buf );
				sprintf( file, "%s/%s%03d.%03d.%03d",
					lockdirs[i],
					lockprefixes[k],
					(int) major( buf.st_dev ),
					(int) major( buf.st_rdev ),
					(int) minor( buf.st_rdev )
				);
				if( stat( file, &buf ) == 0 )
				{
					sprintf( message, UNEXPECTED_LOCK_FILE,
						file );
					report_warning( message );
					return 1;
				}
				k++;
			}
		}
		i++;
	}

	/*
		OK.  We think there are no unexpect lock files for this device
		Lets see if there any stale lock files that need to be
		removed.
	*/
		 
#ifdef FHS
	/*  FHS standard locks */
	i = strlen( port_filename );
	p = ( char * ) port_filename + i;
	while( *(p-1) != '/' && i-- != 1)
	{
#if defined ( __unixware__ )
		*p = tolower( *p );
#endif /* __unixware__ */
		p--;
	}
	sprintf( file, "%s/%s%s", LOCKDIR, LOCKFILEPREFIX, p );
#else 
	/*  UUCP standard locks */
	if ( stat( port_filename, &buf ) != 0 )
	{
		report( "RXTX is_device_locked() could not find device.\n" );
			return 1;
	}
	sprintf( file, "%s/LK.%03d.%03d.%03d",
		LOCKDIR,
		(int) major( buf.st_dev ),
 		(int) major( buf.st_rdev ),
		(int) minor( buf.st_rdev )
	);

#endif /* FHS */

	if( stat( file, &buf ) == 0 )
	{

		/* check if its a stale lock */
		fd=open( file, O_RDONLY );
		read( fd, pid_buffer, 11 );
		/* FIXME null terminiate pid_buffer? need to check in Solaris */
		close( fd );
		sscanf( pid_buffer, "%d", &pid );

		if( kill( (pid_t) pid, 0 ) && errno==ESRCH )
		{
			sprintf( message,
				"RXTX Warning:  Removing stale lock file. %s\n",
				file );
			report_warning( message );
			if( unlink( file ) != 0 )
			{
				snprintf( message, 80, "RXTX Error:  Unable to \
					remove stale lock file: %s\n",
					file
				);
				report_warning( message );
				return 1;
			}
		}
	}
	return 0;
}
#endif /* WIN32 */

/*----------------------------------------------------------
 system_does_not_lock

   accept:      the filename the system thinks should be locked.
   perform:     avoid trying to create lock files on systems that dont use them
   return:      0 for success ;)
   exceptions:  none
   comments:    OS's like Win32 may not have lock files.
----------------------------------------------------------*/
int system_does_not_lock( const char * filename, int pid )
{
	return 0;
}

/*----------------------------------------------------------
 system_does_not_unlock

   accept:      the filename the system thinks should be locked.
   perform:     avoid trying to create lock files on systems that dont use them
   return:      none
   exceptions:  none
   comments:    OS's like Win32 may not have lock files.
----------------------------------------------------------*/
void system_does_not_unlock( const char * filename, int openpid )
{
	return;
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
#ifdef DEBUG
	int i;

	fprintf(stderr, "%s c_iflag=%#x\n", foo, ttyset->c_iflag);
	fprintf(stderr, "%s c_lflag=%#x\n", foo, ttyset->c_lflag);
	fprintf(stderr, "%s c_oflag=%#x\n", foo, ttyset->c_oflag);
	fprintf(stderr, "%s c_cflag=%#x\n", foo, ttyset->c_cflag);
	fprintf(stderr, "%s c_cc[]: ", foo);
	for(i=0; i<NCCS; i++)
	{
		fprintf(stderr,"%d=%x ", i, ttyset->c_cc[i]);
	}
	fprintf(stderr,"\n" );
#endif /* DEBUG */
}
/*----------------------------------------------------------
printj

   accept:      like vwprintf()
   return:      number of jchars written or -1
   exceptions:  none
   comments:    prints data using System.out.print()

   for gcj + CNI
   Just use java::lang::System::out->println(s);
----------------------------------------------------------*/
int printj(
	wchar_t *fmt,
	...
)
{
	return(0);
}
/* for gcj compiles */

jboolean gnu::io::RXTXPort::nativeSetCallOutHangup( jboolean NoHup )
{
	return( true );
}
jboolean gnu::io::RXTXPort::nativeGetCallOutHangup( )
{
	return( true );
}
jboolean gnu::io::RXTXPort::nativeSetLowLatency( )
{
	return( true );
}
jboolean gnu::io::RXTXPort::nativeGetLowLatency( )
{
	return( true );
}
jboolean gnu::io::RXTXPort::nativeSetUartType( jstring ty, jboolean te )
{
	return( true );
}
jstring  gnu::io::RXTXPort::nativeGetUartType( )
{
	return JvNewStringUTF( "RXTX-2.1-7pre13 SAM I AM" );
}
