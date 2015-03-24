/*-------------------------------------------------------------------------
|   RXTX License v 2.1 - LGPL v 2.1 + Linking Over Controlled Interface.
|   RXTX is a native interface to serial ports in java.
|   Copyright 2002-2004 Michal Hobot MichalHobot@netscape.net
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
#if !defined(Included_RXTXHELPERS_H)
#define Included_RXTXSERIAL_H

/* SerialPortEvent constants */
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

#define PORT_SERIAL		1
#define PORT_PARALLEL	2
#define PORT_I2C		  3
#define PORT_RS485		4
#define PORT_RAW		  5

#define CreateErrorMsg(dwError, lpMsgBuf)          \
          FormatMessageW(                          \
                  FORMAT_MESSAGE_ALLOCATE_BUFFER | \
                  FORMAT_MESSAGE_FROM_SYSTEM |     \
                  FORMAT_MESSAGE_IGNORE_INSERTS,   \
                  NULL,                            \
                  dwError,                         \
                  0,                               \
                  (LPWSTR) & (lpMsgBuf),           \
                  0,                               \
                  NULL                             \
                 ),                                \
          ((WCHAR *)lpMsgBuf)[wcslen((WCHAR *)lpMsgBuf)-2] = '\0'

#define ReleaseErrorMsg(lpMsgBuf) LocalFree((LPVOID)(lpMsgBuf))

#if defined(DEBUG)
#  define IF_DEBUG(x) {x}
#else
#  define IF_DEBUG(x)  
#endif

/* java exception class names */
#define ARRAY_INDEX_OUT_OF_BOUNDS "java/lang/ArrayIndexOutOfBoundsException"
#define IO_EXCEPTION "java/io/IOException"

#ifdef JAVAXCOMM_FRONTEND
#define UNSUPPORTED_COMM_OPERATION "javax/comm/UnsupportedCommOperationException"
#define PORT_IN_USE_EXCEPTION "javax/comm/PortInUseException"
#else
#define UNSUPPORTED_COMM_OPERATION "gnu/io/UnsupportedCommOperationException"
#define PORT_IN_USE_EXCEPTION "gnu/io/PortInUseException"
#endif

typedef struct
{
  /* Port handle */
  HANDLE fd;
  /* flags for events */
  DWORD ef;
  /* event handle for Monitor interthread signalling*/
  HANDLE eventHandle;
  /* current serial event */
  DWORD event;
  /* EventThread sets this flag to TRUE when it's ready */
  bool eventThreadReady;
} EventInfoStruct;


long get_java_var_long( JNIEnv *, jobject, const char *, const char * );
//bool get_java_boolean_var2(JNIEnv *, jobject, jclass, const char *);
//void throw_java_exception(JNIEnv *, const char *, const char *, const char *);
void throw_java_exceptionW(JNIEnv *, const char *, const wchar_t *, const wchar_t *);
HANDLE get_fd(JNIEnv *, jobject);
EventInfoStruct *get_eis(JNIEnv *, jobject);
int printj(JNIEnv *, const wchar_t *, ...);
DWORD __stdcall CommEventThread(LPVOID);
//void setEventFlags(JNIEnv *env, jobject jobj, bool ef[]);
int InitialiseEventInfoStruct(HANDLE, EventInfoStruct **);
int SendEvents(JNIEnv *, jobject, DWORD, EventInfoStruct *, jmethodID);

#endif //Included_RXTXHELPERS_H
