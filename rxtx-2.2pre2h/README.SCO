This is README.SCO Last Modified Thu Oct 18 13:58:28 MDT 2001
UnixWare and OpenUNIX related Documentation

This document is the release notes From Caldera before the merge see notes.

Java Communications API Implementation for Caldera UNIX Operating Systems
Version 2.0 Beta-3
October 2, 2001

-------------------------------------------------------------------------------

notes:

This was Version 2.0 Beta-3 notes.  The work is being merged into the rxtx CVS.
Some minor changes will happen and will be noted here.

Thanks to Jonathan Schilling <jls@caldera.com> and Caldera for the SCO port!

1)  RXTX does not ship with Sun's comm.jar.  It can be obtained from Sun.
2)  The Lockfile could use some work.  It possible to fix it at compile time
    but a run time solution would allow.  The current behavior is to use FHS
    locks.  This should be the same behavior as rxtx-1.4-8 as used in the
    original SCO port.
3)  in configure.in you will want to comment out 2 lines and run autoconf in
    the top rxtx directory before building.  The lines are:

	# Comment these two lines out when compiling on SCO
	AC_DISABLE_STATIC
	AM_PROG_LIBTOOL
	# end of SCO fixes

------------------------------------------------------------------------------- 

These are the Release Notes for the beta release of an implementation of
the Java Communications API (also known as javax.comm) for Caldera UNIX
operating systems.  

The Java Communications API is described at 
http://java.sun.com/products/javacomm/index.html.

This implementation is available for the following Caldera UNIX operating
systems:

	Open UNIX 8.00
	UnixWare 7.1.1
	OpenServer 5.0.6 with rs506a supplement

A prerequisite is that Java 2 Standard Edition v. 1.3.0 for Caldera UNIX
Operating Systems must already have been installed (including any 
packages or ptfs required by J2SE 1.3.0).  This requirement may be met
by either the J2SE SDK (package java2sdk3) or the J2SE Runtime Environment 
(package java2jre3).  

While J2SE 1.3.0 for OpenServer does not require the rs506a release 
supplement be installed, this javax.comm implementation does.

This implementation is a port of the open source RXTX implementation of
the Java Communications API.  Information about RXTX may be found at
http://www.rxtx.org.  This port has been done off of RXTX version 1.4-8.

RXTX includes part of the Sun Java Communications API implementation 
for Solaris/x86, version 2.0.  This part (the comm.jar file) is included 
in this distribution; you do not need to download it from the Sun web page.

To install this beta implementation, place the javaxcomm.ds file somewhere.
Go there, become root, and then do:

	# pkgadd -d 'pwd`/javaxcomm.ds

This will install package javaxcomm, which places the following files into 
the J2SE 1.3.0 directory structure:

	/usr/java2/jre/lib/ext/comm.jar 
	/usr/java2/jre/lib/ext/jcl.jar
	/usr/java2/jre/lib/x86at/libSerial.so
	/usr/java2/jre/lib/x86at/libParallel.so
	/usr/java2/jre/lib/javax.comm.properties
	/usr/java2/javax.comm-ReleaseNotes.txt [this file]

Alternatively, if you are installing the Java Communications API in 
conjunction with the J2SE Runtime Environment rather than the J2SE SDK, 
then use the jrexcomm.ds file instead:

	# pkgadd -d 'pwd`/jrexcomm.ds

which will install the following files as part of package jrexcomm:

	/usr/jre2/lib/ext/comm.jar 
	/usr/jre2/lib/ext/jcl.jar
	/usr/jre2/lib/x86at/libSerial.so
	/usr/jre2/lib/x86at/libParallel.so
	/usr/jre2/lib/javax.comm.properties
	/usr/jre2/javax.comm-ReleaseNotes.txt [this file]

You can install both the javaxcomm and jrexcomm packages if you desire,
as long as you have both java2sdk3 and java2jre3 installed.

The installed binaries of either Java Communications API package are used 
for all three Caldera UNIX platforms.  (On OpenServer, the native code 
libraries are executed via the OSRcompat binary compatibility module, 
just as the native code layers of J2SE 1.3.0 are.)

To use this implementation of Java Communications API, just compile and
run Java as you normally would.  Because the above files are installed
into the standard J2SE 1.3.0 extension space, there is no need to set
CLASSPATH or LD_LIBRARY_PATH to access the Java Communications API.

A simple test of the serial I/O part of this implementation can be
done as follows.  Set up a null modem RS-232 cable between two machines.  
Become root.  Run the open source "minicom" tool 
(see http://www.pp.clinet.fi/~walker/minicom.html) on each machine 
and verify that the connection is working, by typing characters back
and forth between the two machines.  Caldera can make available
minicom-ou8-uw7 and minicom-osr5, ported versions of minicom for
the Caldera UNIX platforms.  (Note that you need to first do 
"minicom -s" to tell it about the names and speeds of your serial ports.  
When you then run "minicom", use the ctrl-A E and ctrl-A A commands to 
turn on local echo and linefeed add.)

Once working serial communication has been verified, shut down minicom
on the machine that Java Communications API is installed on.  Untar the 
Sun SerialDemo demo program, which is SerialDemo.tar and can be made
available by Caldera.  Change into the SerialDemo directory, and run

	# /usr/java2/bin/java SerialDemo
or
	# /usr/jre2/bin/java SerialDemo

This should bring up a GUI.  Change the serial device port name and speed
in the GUI if necessary, then click "Open Port".  (Contact Caldera if the 
port name you are interested in does not appear in the GUI.)  Type into the 
top pane and text should appear in the minicom window on the other machine.  
Type into the minicom window on the other machine and text should appear 
in the middle pane of the SerialDemo GUI.  Click "Close Port" to finish.

This implementation uses the normal UNIX "lock file" protocol to protect
serial ports from concurrent access.  This means that javax.comm will
not be able to access the port if it is already locked by another
serial application (whether javax.comm, non-Java, or a system command),
and vice versa.  In the case where javax.comm is locked out, you may get 
a message containing the phrase "Port currently owned by unknown Solaris 
Application"; for "Solaris" read the Caldera UNIX operating system you 
are on.

Additional sample programs using the Java Communications API are available
as part of the Sun Solaris/x86 implementation and can be downloaded from
http://java.sun.com/products/javacomm/index.html.  (Do not use the
SimpleRead and SimpleWrite programs without modification, since they fail
to properly close the serial port at program completion.)

Known problems:

The lock files in Open UNIX 8 and UnixWare 7 will protect against
concurrent port access by multiple javax.comm applications, but will not
protect against concurrent access by other applications or system commands
that use the SVR4 serial lock file protocol (/var/spool/locks/LK.m.m.m).  
This will be corrected in the final release.  On OpenServer, the lock files 
protect against all concurrent access, assuming other applications are
using the standard protocol there (/usr/spool/uucp/LCK..tty?A).

The parallel I/O part of this implementation has not been tested at all 
on any Caldera UNIX platforms.

RXTX is available under the GNU Library General Public License (LGPL).
Because this is still just an beta level implementation, the ported
source code for RXTX is not included with this distribution.  Once this
implementation is final, source code will be included.  If you would
like the source code for this beta implementation, please contact 
Caldera and it will be sent to you.

