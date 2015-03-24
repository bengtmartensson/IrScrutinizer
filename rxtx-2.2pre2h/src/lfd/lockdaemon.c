/*-------------------------------------------------------------------------
|   RXTX License v 2.1 - LGPL v 2.1 + Linking Over Controlled Interface.
|   RXTX is a native interface to serial ports in java.
|   Copyright 2002-2007 by Trent Jarvi tjarvi@qbang.org and others who
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
#include <sys/types.h>
#include <unistd.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <syslog.h>
#include <sys/socket.h>
#include <netdb.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <arpa/inet.h>
#include <errno.h>
#include <signal.h>
#include <pwd.h>

#define FHS
#define LOCKFILEPREFIX "LCK.."
#define LOCK fhs_lock
#define UNLOCK fhs_unlock
#define LOCKDIR "/var/lock"

char hostname[256];
#define M200 "200 Command okay.\n"
#define M202 "202 Command not implemented\n"
#define M220 "220 %s Lock File Server (Version rxtx-1.5-9) ready\n", hostname
#define M221 "221 Thank you for using the Lock File service on %s\n", hostname
#define M450 "450 : File busy.\n"
#define M500 "500 '%s': command not understood\n", str
#define M550 "550 : Permission denied.\n"

int is_device_locked( const char * );
int check_group_uucp();
int check_lock_status( const char * );
int check_lock_pid( const char *, int  );

#define UNEXPECTED_LOCK_FILE "RXTX Error:  Unexpected lock file: %s\n Please report to the RXTX developers\n"
#define UUCP_ERROR "\n\n\nRXTX WARNING:  This library requires the user running applications to be in\ngroup uucp.  Please consult the INSTALL documentation.  More information is\navaiable under the topic 'How can I use Lock Files with rxtx?'\n" 

extern int errno;

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
		/* syslog( LOG_INFO, "fhs_lock() lockstatus fail\n" ); */
		return 1;
	}
	fd = open( file, O_CREAT | O_WRONLY | O_EXCL, 0444 );
	if( fd < 0 )
	{
		sprintf( message,
			"RXTX fhs_lock() Error: creating lock file: %s: %s\n",
			file, strerror(errno) );
		syslog( LOG_INFO, message );
		return 1;
	}
	sprintf( lockinfo, "%10d\n", pid );
	sprintf( message, "fhs_lock: creating lockfile: %s\n", lockinfo );
	//syslog( LOG_INFO, message );
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
	syslog( LOG_INFO, message );

	if ( check_lock_status( filename ) )
	{
		syslog( LOG_INFO, "RXTX uucp check_lock_status true\n" );
		return 1;
	}
	if ( stat( LOCKDIR, &buf ) != 0 )
	{
		syslog( LOG_INFO, "RXTX uucp_lock() could not find lock directory.\n" );
		return 1;
	}
	if ( stat( filename, &buf ) != 0 )
	{
		syslog( LOG_INFO, "RXTX uucp_lock() could not find device.\n" );
		sprintf( message, "uucp_lock: device was %s\n", name );
		syslog( LOG_INFO, message );
		sprintf( message, "Filename is : %s \n", filename );
		syslog( LOG_INFO, message );
		return 1;
	}
	sprintf( lockfilename, "%s/LK.%03d.%03d.%03d",
		LOCKDIR,
		(int) major( buf.st_dev ),
	 	(int) major( buf.st_rdev ),
		(int) minor( buf.st_rdev )
	);
	sprintf( lockinfo, "%10d\n", pid );
	if ( stat( lockfilename, &buf ) == 0 )
	{
		sprintf( message, "RXTX uucp_lock() %s is there\n",
			lockfilename );
		syslog( LOG_INFO, message );
		syslog( LOG_INFO, message );
		return 1;
	}
	fd = open( lockfilename, O_CREAT | O_WRONLY | O_EXCL, 0444 );
	if( fd < 0 )
	{
		sprintf( message,
			"RXTX uucp_lock() Error: creating lock file: %s\n",
			lockfilename );
		syslog( LOG_INFO, message );
		return 1;
	}
	write( fd, lockinfo,11 );
	close( fd );
/*
	setgid( nobody );
	setuid( nobody );
*/
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
		syslog( LOG_INFO, "check_lock_status: could not find lock directory.\n" );
		return 1;
	}

	/*  OK.  Are we able to write to it?  If not lets bail */

	if ( check_group_uucp() )
	{
		syslog( LOG_INFO, "check_lock_status: No permission to create lock file.

		please see: How can I use Lock Files with rxtx? in INSTALL\n" );
		return 1;
	}

	/* is the device alread locked */

	if ( is_device_locked( filename ) )
	{
		/* syslog( LOG_INFO, "check_lock_status: device is locked by another application\n" ); */
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
int fhs_unlock( const char *filename, int openpid )
{
	char file[80],*p, msg[80];
	struct stat buf;
	int i;

	i = strlen( filename );
	p = ( char * ) filename + i;
	sprintf( msg, "fhs_unlock %s\n", filename );
	/* syslog( LOG_INFO, msg ); */
	/*  FIXME  need to handle subdirectories /dev/cua/... */
	while( *( p - 1 ) != '/' && i-- != 1 ) p--;
	sprintf( file, "%s/LCK..%s", LOCKDIR, p );
#ifdef asdf
	if ( ! check_lock_status( p ) )
	{
		sprintf( msg, "fhs_unlock %s check_lock_status\n", filename );
		syslog( LOG_INFO, msg );
		return 0;
	}
#endif
	if ( stat( filename, &buf ) != 0 ) 
	{
		/* hmm the file is not there? */
		syslog( LOG_INFO, "uucp_unlock() no such device\n" );
		return(0);
	}

	if( !check_lock_pid( file, openpid ) )
	{
		unlink(file);
		//syslog( LOG_INFO,"fhs_unlock: Removing LockFile\n");
		return( 0 );
	}
	else
	{
		syslog( LOG_INFO,"fhs_unlock: Unable to remove LockFile\n");
		return( 1 );
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
	syslog( LOG_INFO, message );

	if ( stat( filename, &buf ) != 0 ) 
	{
		/* hmm the file is not there? */
		syslog( LOG_INFO, "uucp_unlock() no such device\n" );
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
		syslog( LOG_INFO, "uucp_unlock no such lockfile\n" );
		return;
	}
	if( !check_lock_pid( file, openpid ) )
	{ 
		sprintf( message, "uucp_unlock: unlinking %s\n", file );
		syslog( LOG_INFO, message );
		unlink(file);
	}
	else
	{
		sprintf( message, "uucp_unlock: unlinking failed %s\n", file );
		syslog( LOG_INFO, message );
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
	if ( lockpid != openpid )
	{
		if( kill( (pid_t) lockpid, 0 ) && errno==ESRCH )
		{
			return( 0 );
		}
		sprintf(message, "check_lock_pid: lock = %s pid = %i gpid=%i openpid=%i\n",
			pid_buffer, (int) getpid(), (int) getppid(), openpid );
		syslog( LOG_INFO, message );
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
----------------------------------------------------------*/
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
		syslog( LOG_INFO, msg );
		return( 1 );
	}
	group_count = getgroups( NGROUPS_MAX, list );
	list[ group_count ] = geteuid();

	if( user->pw_gid )
	{
		while( group_count >= 0 && buf.st_gid != list[ group_count ] )
		{
  			group_count--; 
		}
		if( buf.st_gid == list[ group_count ] )
			return 0;
		sprintf( msg, "%i %i\n", buf.st_gid, list[ group_count ] );
		syslog( LOG_INFO, msg );
		syslog( LOG_INFO, UUCP_ERROR );
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
			syslog( LOG_INFO, UUCP_ERROR );
			return 1;
		}
	}
*/
#endif /* USER_LOCK_DIRECTORY */
	return 0;
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
					syslog( LOG_INFO, message );
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
					syslog( LOG_INFO, message );
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
		sprintf( message, "found lock for %s with pid %i\n", file, pid );
		/* syslog( LOG_INFO, message ); */

		if( kill( (pid_t) pid, 0 ) && errno==ESRCH )
		{
			sprintf( message,
				"RXTX Warning:  Removing stale lock file. %s\n",
				file );
			syslog( LOG_INFO, message );
			if( unlink( file ) != 0 )
			{
				snprintf( message, 80, "RXTX Error:  Unable to \
					remove stale lock file: %s\n",
					file
				);
				syslog( LOG_INFO, message );
				return 0;
			}
		}
		else
		{
			sprintf( message, "could not kill %i\n", pid );
			/* syslog( LOG_INFO, message ); */
			return 1;
		}
	}
	return 0;
}
int init( void )
{
	pid_t pid;

	if(  ( pid = fork() ) < 0 )
	{
		return(-1);
	} 
	else if ( pid != 0 )
	{
		exit( 0 );
	}
	setsid();
	chdir("/");
	umask( 0 );
	return( 0 );
}
int process_requests( )
{
	for(;;)
	{
		char str[80];
		char str2[80];
		char *p;
		int ret;

		ret = read( 1, str, 80 );
		
		if( ret < 80 && ret > 1 )
			str[ret] = '\0';
		else
			str[79] = '\0';
		if ( !strncasecmp( str, "quit", 4 ) )
		{
			sprintf( str, M221 );
			write( 0, str, strlen( str ) );
			return( 1 );
		}
		else if( !strncasecmp( str, "lock ", 4 ) )
		{
			char *q,*r;
			p = str + 5;
			q=p;
			while( *q != ' ' && *q != '\0' )
				q++;
			if ( *q == '\0' )
			{
				write( 0, M450, strlen( M450 ) );
				return(0);
			}
			*q = '\0';
			q++;
			r=q; 
			while( *r != '\n' && *r != '\0' )
				r++;
			if( *r == '\n' )
				*r = '\0';
			if ( LOCK( p, atoi( q ) ) )
			{
				write( 0, M450, strlen( M450 ) );
			}
			else
			{
				write( 0, M200, strlen( M200 ) );
			}
		}
		else if( !strncasecmp( str, "unlock ", 6 ) )
		{
			char *q,*r;
			p = str + 7;
			q=p;
			while( *q != ' ' && *q != '\0' )
				q++;
			if ( *q == '\0' )
			{
				write( 0, "q=0\n", strlen( "q=0\n" ) );
				write( 0, M450, strlen( M450 ) );
				return(0);
			}
			*q = '\0';
			q++;
			r=q; 
			while( *r != '\n' && *r != '\0' )
				r++;
			if( *r == '\n' )
				*r = '\0';
			if ( UNLOCK( (const char *) p, atoi( q ) ) )
			{
				write( 0, M450, strlen( M450 ) );
			}
			else
			{
				write( 0, M200, strlen( M200 ) );
			}
		}
		else
		{
			str[ret-2]='\0';
			sprintf( str2, M500 );
			write( 0, str2, strlen(str2));
		}
		return( 0 );
	}
}
int main( int argc, char **argv )
{
	char str[128];
	char portstr[7];
	struct sockaddr *cliaddr;
	struct sockaddr_in *sin;
	socklen_t len;

	openlog( argv[0], LOG_PID, 0 );
	cliaddr= malloc( 128 );
	len = 128;
	sin = ( struct sockaddr_in * ) cliaddr;
	cliaddr= malloc( 128 );
	gethostname( hostname, 255 );
	if ( !inet_ntop( AF_INET, &(sin->sin_addr), str, sizeof(str) ) )
		str[0] = '\0';
	if ( !ntohs( sin->sin_port ) )
	{
		snprintf( portstr, sizeof( portstr ), ".%d", ntohs( sin->sin_port) );
		strcat( str, portstr);
	}
	sprintf( str,  M220 );
	//syslog( LOG_INFO, str );
	write( 0, str, strlen(str) );
	for(;;)
	{
		if (process_requests( ))
		{
			goto exit;
		}
	}
	syslog( LOG_INFO, "Lock Daemon Shutting down" );
exit:
	closelog();
	exit(0);
}
