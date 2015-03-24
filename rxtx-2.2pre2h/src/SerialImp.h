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
/* gnu.io.SerialPort constants */
#ifdef WIN32
#include "win32termios.h"
#endif /* WIN32 */
#define JDATABITS_5		5
#define JDATABITS_6		6
#define JDATABITS_7		7
#define JDATABITS_8		8
#define JPARITY_NONE		0
#define JPARITY_ODD		1
#define JPARITY_EVEN		2
#define JPARITY_MARK		3
#define JPARITY_SPACE		4
#define STOPBITS_1		1
#define STOPBITS_2		2
#define STOPBITS_1_5		3
#define FLOWCONTROL_NONE	0
#define FLOWCONTROL_RTSCTS_IN	1
#define FLOWCONTROL_RTSCTS_OUT	2
#define FLOWCONTROL_XONXOFF_IN	4
#define FLOWCONTROL_XONXOFF_OUT	8

/* gnu.io.SerialPortEvent constants */
#define SPE_DATA_AVAILABLE       1
#define SPE_OUTPUT_BUFFER_EMPTY  2
#define SPE_CTS                  3
#define SPE_DSR                  4
#define SPE_RI                   5
#define SPE_CD                   6
#define SPE_OE                   7
#define SPE_PE                   8
#define SPE_FE                   9
#define SPE_BI                  10

#define PORT_SERIAL		 1
#define PORT_PARALLEL		 2
#define PORT_I2C		 3
#define PORT_RS485		 4
#define PORT_RAW		 5

/* glue for unsupported linux speeds see also win32termios.h */

#if !defined(__APPLE__) && !defined(__FreeBSD__) /* dima */
/* this is now handled in SerialImp.c
#define B14400		1010001
#define B28800		1010002
#define B128000		1010003
#define B256000		1010004
*/
#endif /* dima */

struct preopened
{
	char filename[40];
	int fd;
	struct preopened *next;
	struct preopened *prev;
};

struct event_info_struct
{
	int fd;
	/* flags for events */
	int eventflags[11];
	
	int initialised;
	int ret, change;
	unsigned int omflags;
	char message[80];
	int has_tiocsergetlsr;
	int has_tiocgicount;
	int eventloop_interrupted;
	JNIEnv *env;
	jobject *jobj;
	jclass jclazz;
	jmethodID send_event;
	jmethodID checkMonitorThread;
	struct event_info_struct *next, *prev;
	fd_set rfds;
	struct timeval tv_sleep;
	int closing;
#if !defined(TIOCSERGETLSR) && !defined(WIN32)
	int writing;
	int output_buffer_empty_flag;
	int drain_loop_running;
	pthread_t drain_tid;
#endif /* !TIOCSERGETLSR !WIN32 */
#	if defined(TIOCGICOUNT)
	struct serial_icounter_struct osis;
#endif /* TIOCGICOUNT */
};

/*  Ports known on the OS */
#if defined(__linux__)
/*
	This is a small hack to get mark and space parity working on older systems
	https://bugzilla.redhat.com/bugzilla/show_bug.cgi?id=147533
*/
#	if !defined(CMSPAR)
#		define CMSPAR 010000000000
#	endif /* CMSPAR */
#	
#	define DEVICEDIR "/dev/"
#	define LOCKDIR "/var/lock"
#	define LOCKFILEPREFIX "LCK.."
#	define FHS
#endif /* __linux__ */
#if defined(__QNX__)
#	define DEVICEDIR "/dev/"
#	define LOCKDIR ""
#	define LOCKFILEPREFIX ""
#endif /* qnx */
#if defined(__sgi__) || defined(sgi)
#	define DEVICEDIR "/dev/"
#	define LOCKDIR "/usr/spool/uucp"
#	define LOCKFILEPREFIX "LK."
#	define UUCP
#endif /* __sgi__ || sgi */
#if defined(__FreeBSD__)
#	define DEVICEDIR "/dev/"
#	define LOCKDIR "/var/spool/lock"
#	define LOCKFILEPREFIX "LK.."
#	define UUCP
#endif /* __FreeBSD__ */
#if defined(__APPLE__)
#	define DEVICEDIR "/dev/"
/*#	define LOCKDIR "/var/spool/uucp"*/
#	define LOCKDIR "/var/lock"
#	define LOCKFILEPREFIX "LK."
/*#	define UUCP*/
#	define OPEN_EXCL
#endif /* __APPLE__ */
#if defined(__NetBSD__)
#	define DEVICEDIR "/dev/"
#	define LOCKDIR "/var/lock"
/*#	define LOCKDIR "/usr/spool/uucp"*/
#	define LOCKFILEPREFIX "LK."
#	define UUCP
#endif /* __NetBSD__ */
#if defined(__unixware__)
#	define DEVICEDIR "/dev/"
/* really this only fully works for OpenServer */
#	define LOCKDIR "/var/spool/uucp/"
#	define LOCKFILEPREFIX "LK."
/* 
this needs work....

Jonathan Schilling <jls@caldera.com> writes:

This is complicated because as I said in my previous mail, there are
two kinds of SCO operating systems.

The one that most people want gnu.io for, including the guy who
asked the mailing list about SCO support a few days ago, is Open Server
(a/k/a "SCO UNIX"), which is SVR3-based.  This uses old-style uucp locks, 
of the form LCK..tty0a.  That's what I implemented in the RXTX port I did,
and it works correctly.

The other SCO/Caldera OS, UnixWare/Open UNIX, uses the new-style
SVR4 locks, of the form LK.123.123.123.  These OSes are a lot like
Solaris (UnixWare/Open UNIX come from AT&T SVR4 which had a joint

The other SCO/Caldera OS, UnixWare/Open UNIX, uses the new-style
SVR4 locks, of the form LK.123.123.123.  These OSes are a lot like
Solaris (UnixWare/Open UNIX come from AT&T SVR4 which had a joint
heritage with Sun way back when).  I saw that you added support
for this form of lock by RXTX 1.4-10 ... but it gets messy because,
as I said before, we use the same binary gnu.io files for both
UnixWare/Open UNIX and OpenServer.  Thus we can't #ifdef one or the
other; it would have to be a runtime test.  Your code and your macros
aren't set up for doing this (understandably!).  So I didn't implement
these; the gnu.io locks won't fully work on UnixWare/Open UNIX
as a result, which I mentioned in the Release Notes.


What I would suggest is that you merge in the old-style LCK..tty0a lock
code that I used, since this will satisfy 90% of the SCO users.  Then
I'll work on some way of getting UnixWare/Open UNIX locking to work
correctly, and give you those changes at a later date.

Jonathan

FIXME  The lock type could be passed with -DOLDUUCP or -DUUCP based on
os.name in configure.in or perhaps system defines could determine the lock
type.

Trent
*/
#	define OLDUUCP
#endif
#if defined(__hpux__)
/* modif cath */
#	define DEVICEDIR "/dev/"
#	define LOCKDIR "/var/spool/uucp"
#	define LOCKFILEPREFIX "LCK."
#	define UUCP
#endif /* __hpux__ */
#if defined(__osf__)  /* Digital Unix */
#	define DEVICEDIR "/dev/"
#	define LOCKDIR ""
#	define LOCKFILEPREFIX "LK."
#	define UUCP
#endif /* __osf__ */
#if defined(__sun__) /* Solaris */
#	define DEVICEDIR "/dev/"
#	define LOCKDIR "/var/spool/locks"
#	define LOCKFILEPREFIX "LK."
/*
#	define UUCP
*/
#endif /* __sun__ */
#if defined(__BEOS__)
#	define DEVICEDIR "/dev/ports/"
#	define LOCKDIR ""
#	define LOCKFILEPREFIX ""
#	define UUCP
#endif /* __BEOS__ */
#if defined(WIN32)
#	define DEVICEDIR "//./"
#	define LOCKDIR ""
#	define LOCKFILEPREFIX ""
#	define OPEN serial_open
#	define CLOSE serial_close
#	define WRITE serial_write
#	define READ serial_read
#	define SELECT serial_select
#else /* use the system calls for Unix */
#	define OPEN open
#	define CLOSE close
#	define WRITE write
#	define READ read
#	define SELECT select
#endif /* WIN32 */

#if defined(DEBUG_TIMING) && ! defined(WIN32) /* WIN32 does not have gettimeofday() */
struct timeval snow, enow, seloop, eeloop;
#ifdef __APPLE__
#define report_time_eventLoop( ) { \
	if ( seloop.tv_sec == eeloop.tv_sec && seloop.tv_usec == eeloop.tv_usec ) \
	{ \
		gettimeofday(&eeloop, NULL); \
		seloop.tv_sec = eeloop.tv_sec; \
		seloop.tv_usec = eeloop.tv_usec; \
		printf("%8li sec : %8i usec\n", eeloop.tv_sec - seloop.tv_sec, eeloop.tv_usec - seloop.tv_usec); \
	} \
}
#define report_time( ) \
{ \
	struct timeval now; \
	gettimeofday(&now, NULL); \
	printf("%8s : %5i : %8li sec : %8i usec\n", __TIME__, __LINE__, now.tv_sec, now.tv_usec); \
}
#define report_time_start( ) \
{ \
	gettimeofday(&snow, NULL); \
	printf("%8s : %5i : %8li sec : %8i usec", __TIME__, __LINE__, snow.tv_sec, snow.tv_usec); \
}
#define report_time_end( ) \
{ \
	gettimeofday(&enow, NULL); \
	printf("%8li sec : %8i usec\n", enow.tv_sec - snow.tv_sec, enow.tv_sec - snow.tv_sec?snow.tv_usec-enow.tv_usec:enow.tv_usec - snow.tv_usec); \
}
#else /* ! __APPLE__ */
#define report_time_eventLoop( ) { \
if ( seloop.tv_sec == eeloop.tv_sec && seloop.tv_usec == eeloop.tv_usec ) \
{ \
gettimeofday(&eeloop, NULL); \
seloop.tv_sec = eeloop.tv_sec; \
seloop.tv_usec = eeloop.tv_usec; \
printf("%8li sec : %8li usec\n", eeloop.tv_sec - seloop.tv_sec, eeloop.tv_usec - seloop.tv_usec); \
} \
}
#define report_time( ) \
{ \
struct timeval now; \
gettimeofday(&now, NULL); \
printf("%8s : %5i : %8li sec : %8li usec\n", __TIME__, __LINE__, now.tv_sec, now.tv_usec); \
}
#define report_time_start( ) \
{ \
gettimeofday(&snow, NULL); \
printf("%8s : %5i : %8li sec : %8li usec", __TIME__, __LINE__, snow.tv_sec, snow.tv_usec); \
}
#define report_time_end( ) \
{ \
gettimeofday(&enow, NULL); \
printf("%8li sec : %8li usec\n", enow.tv_sec - snow.tv_sec, enow.tv_sec - snow.tv_sec?snow.tv_usec-enow.tv_usec:enow.tv_usec - snow.tv_usec); \
}
#endif  /* __APPLE__ */
#else /* ! DEBUG_TIMING || WIN32 */
#define report_time_eventLoop( ){};
#define report_time( ) {}
#define report_time_start( ) {}
#define report_time_end( ) {}
#endif /* DEBUG_TIMING && ! WIN32 */

/* #define TRACE */
#ifdef TRACE
#define ENTER(x) report("entering "x" \n");
#define LEAVE(x) report("leaving "x" \n");
#else
#define ENTER(x)
#define LEAVE(x)
#endif /* TRACE */

/* allow people to override the directories */
/* #define USER_LOCK_DIRECTORY "/home/tjarvi/1.5/build" */
#ifdef USER_LOCK_DIRECTORY
#	define LOCKDIR USER_LOCK_DIRECTORY
#endif /* USER_LOCK_DIRECTORY */

#ifdef DISABLE_LOCKFILES
#undef UUCP
#undef FHS
#undef OLDUUCP
#endif /* DISABLE_LOCKFILES */

/*  That should be all you need to look at in this file for porting */
#ifdef LFS  /*  Use a Lock File Server */
#	define LOCK lfs_lock
#	define UNLOCK lfs_unlock
#elif defined(UUCP)
#	define LOCK uucp_lock
#	define UNLOCK uucp_unlock
#elif defined(OLDUUCP)
/*
   We can handle the old style if needed here see __unixware__ above.
   defaulting to rxtx-1.4-8 behavior for now.

   see also __sco__ in SerialImp.c when changing this for a possible
   bug

   FIXME
*/
#	define LOCK fhs_lock
#	define UNLOCK fhs_unlock
#elif defined(FHS)
#ifdef LIBLOCKDEV
#	define LOCK lib_lock_dev_lock
#	define UNLOCK lib_lock_dev_unlock
#else
#	define LOCK fhs_lock
#	define UNLOCK fhs_unlock
#endif /* LIBLOCKDEV */
#else 
#	define LOCK system_does_not_lock
#	define UNLOCK system_does_not_unlock
#endif /* UUCP */

/* java exception class names */
/* define UNSUPPORTED_COMM_OPERATION "gnu/io/UnsupportedCommOperationException" */
#define UNSUPPORTED_COMM_OPERATION "UnsupportedCommOperationException"
#define ARRAY_INDEX_OUT_OF_BOUNDS "java/lang/ArrayIndexOutOfBoundsException"
#define OUT_OF_MEMORY "java/lang/OutOfMemoryError"
#define IO_EXCEPTION "java/io/IOException"
#define PORT_IN_USE_EXCEPTION "gnu/io/PortInUseException"

/* some popular releases of Slackware do not have SSIZE_MAX */

#ifndef SSIZE_MAX
#	if defined(INT_MAX)
#		define SSIZE_MAX  INT_MAX
#	elif defined(MAXINT)
#		define SSIZE_MAX MAXINT
#	else
#		define SSIZE_MAX 2147483647 /* ugh */
#	endif
#endif

/*
Flow Control defines inspired by reading how mgetty by Gert Doering does it
*/

#ifdef CRTSCTS
#define HARDWARE_FLOW_CONTROL CRTSCTS
#else
#	ifdef CCTS_OFLOW
#	define HARDWARE_FLOW_CONTROL CCTS_OFLOW|CRST_IFLOW
#	else
#		ifdef RTSFLOW
#		define HARDWARE_FLOW_CONTROL RTSFLOW|CTSFLOW
#		else
#			ifdef CRTSFL
#			define HARDWARE_FLOW_CONTROL CRTSFL
#			else
#				ifdef CTSCD
#				define HARDWARE_FLOW_CONTROL CTSCD
#				else
#					define HARDWARE_FLOW_CONTROL 0
#				endif
#			endif
#		endif
#	endif
#endif

/* PROTOTYPES */
#ifdef __BEOS__
struct tpid_info_struct *add_tpid( struct tpid_info_struct * );
data_rate translate_speed( JNIEnv*, jint  );
int translate_data_bits( JNIEnv *, data_bits *, jint );
int translate_stop_bits( JNIEnv *, stop_bits *, jint );
int translate_parity( JNIEnv *, parity_mode *, jint );
#else
int spawn_write_thread( int, char *, int, JNIEnv *, jobject *);
int translate_speed( JNIEnv*, jint  );
int translate_data_bits( JNIEnv *, tcflag_t *, jint );
int translate_stop_bits( JNIEnv *, tcflag_t *, jint );
int translate_parity( JNIEnv *, tcflag_t *, jint );
#endif
void system_wait();
void finalize_event_info_struct( struct event_info_struct * );
int read_byte_array( JNIEnv *, jobject *, int, unsigned char *, int, int );
long get_java_var_long( JNIEnv *, jobject, char *, char * );
size_t get_java_var( JNIEnv *, jobject, char *, char * );
jboolean is_interrupted( struct event_info_struct * );
int send_event(struct event_info_struct *, jint, int );
void dump_termios(char *,struct termios *);
void report_verbose(char *);
void report_error(char *);
void report_warning(char *);
void report(char *);
void throw_java_exception( JNIEnv *, char *, char *, char * );
int lock_device( const char * );
void unlock_device( const char * );
int is_device_locked( const char * );
int check_lock_status( const char * );
int lfs_unlock(const char *, int );
int lfs_lock( const char *, int);
int lib_lock_dev_unlock(const char *, int );
int lib_lock_dev_lock( const char *, int);
void fhs_unlock(const char *, int );
int fhs_lock( const char *, int);
void uucp_unlock( const char *, int );
int uucp_lock( const char *, int );
int system_does_not_lock( const char *, int );
void system_does_not_unlock( const char *, int );
int check_group_uucp();
int check_lock_pid( const char *, int );
int printj(JNIEnv *env, wchar_t *fmt, ...);

#define UNEXPECTED_LOCK_FILE "RXTX Error:  Unexpected lock file: %s\n Please report to the RXTX developers\n"
#define LINUX_KERNEL_VERSION_ERROR "\n\n\nRXTX WARNING:  This library was compiled to run with OS release %s and you are currently running OS release %s.  In some cases this can be a problem.  Try recompiling RXTX if you notice strange behavior.  If you just compiled RXTX make sure /usr/include/linux is a symbolic link to the include files that came with the kernel source and not an older copy.\n\n\npress enter to continue\n"
#define UUCP_ERROR "\n\n\nRXTX WARNING:  This library requires the user running applications to be in\ngroup uucp.  Please consult the INSTALL documentation.  More information is\navaiable under the topic 'How can I use Lock Files with rxtx?'\n" 
