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
OK.  Finally found the missing bits to get rxtx lp support up to speed.
http://people.redhat.com/twaugh/parport/html/parportguide.html
Patches available for linux 2.2 and 2.4.

This is just a quick test to see if the stuff is working.

*/

#include <sys/io.h>
#include <linux/ppdev.h>
#include <sys/ioctl.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <stdio.h>
#include <stdlib.h>


int main(int argc, char **args)
{
	int compat=IEEE1284_MODE_COMPAT;
	int nibble=IEEE1284_MODE_NIBBLE;
	int byte=IEEE1284_MODE_BYTE;
	int epp=IEEE1284_MODE_EPP;
	int ecp=IEEE1284_MODE_ECP;

	
	int fd=open("/dev/lp0",O_NONBLOCK);
	if(!ioctl(fd,PPCLAIM))
		printf("PPCLAIM Failed\n");
	else
		printf("PPCLAIM works!\n");
	if(!ioctl(fd,PPNEGOT,&compat))
		printf("PPNEGOT compat Failed\n");
	else
		printf("PPNEGOT compat works!\n");
	if(!ioctl(fd,PPNEGOT,nibble))
		printf("PPNEGOT nibble Failed\n");
	else
		printf("PPNEGOT nibble works!\n");
	if(!ioctl(fd,PPNEGOT,byte))
		printf("PPNEGOT byte Failed\n");
	else
		printf("PPNEGOT byte works!\n");
	if(!ioctl(fd,PPNEGOT,epp))
		printf("PPNEGOT epp Failed\n");
	else
		printf("PPNEGOT epp works!\n");
	if(!ioctl(fd,PPNEGOT,ecp))
		printf("PPNEGO ecpT Failed\n");
	else
		printf("PPNEGOT ecp works!\n");
	if(!ioctl(fd, PPSETMODE,&compat))
		printf("PPSETMODE compat Failed\n");
	else
		printf("PPSETMODE compat works!\n");
	if(!ioctl(fd, PPSETMODE,&nibble))
		printf("PPSETMODE nibble  Failed\n");
	else
		printf("PPSETMODE nibble  works!\n");
	if(!ioctl(fd, PPSETMODE,&byte))
		printf("PPSETMODE byte Failed\n");
	else
		printf("PPSETMODE byte works!\n");
	if(!ioctl(fd, PPSETMODE,&epp))
		printf("PPSETMODE epp Failed\n");
	else
		printf("PPSETMODE epp works!\n");
	if(!ioctl(fd, PPSETMODE,&ecp))
		printf("PPSETMODE ecp Failed\n");
	else
		printf("PPSETMODE ecp works!\n");
	if(!ioctl(fd,PPRELEASE))
		printf("PPRELEASE Failed\n");
	else
		printf("PPRELEASE works!\n");
	exit(0);
}
