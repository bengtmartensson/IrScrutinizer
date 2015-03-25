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
#include "StdAfx.h"
#include "rxtxHelpers.h"

/*
Initialize

   accept:      none
   perform:     Initialize the native library
   return:      none
   exceptions:  none
   comments:
 * Class:     gnu_io_RXTXPort
 * Method:    Initialize
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_gnu_io_RXTXPort_Initialize(JNIEnv *env, jclass cls)
{
}

/*
open

   accept:      The device to open. ie "COM1:"
   perform:     open the device
   return:      handle
   exceptions:  IOExcepiton
   comments:    Very often people complain about not being able to get past
                this function and it turns out to be permissions on the
                device file or bios has the device disabled.
 * Class:     gnu_io_RXTXPort
 * Method:    open
 * Signature: (Ljava/lang/String{ })I
 */
JNIEXPORT jint JNICALL Java_gnu_io_RXTXPort_open(JNIEnv *env, jobject jobj, jstring name)
{
  DCB PortDCB;
  COMMTIMEOUTS CommTimeouts;
  LPCWSTR lpMsgBuf;
  EventInfoStruct *EventInfo;
  DWORD dwErr;

  LPCWSTR wszName = (LPCWSTR)env->GetStringChars(name, NULL);
  HANDLE hPort = CreateFileW(wszName,      // Pointer to the name of the port
                             GENERIC_READ | GENERIC_WRITE,// Access (read-write) mode
                             0,            // Share mode
                             NULL,         // Pointer to the security attribute
                             OPEN_EXISTING,// How to open the serial port
                             0,            // Port attributes
                             NULL);        // Handle to port with attribute to copy
  // If it fails to open the port, return FALSE.
  if ( hPort == INVALID_HANDLE_VALUE )
  { // Could not open the port.
    CreateErrorMsg(GetLastError(), lpMsgBuf);
    throw_java_exceptionW(env, PORT_IN_USE_EXCEPTION, L"open - CreateFile", lpMsgBuf );
    // Free the buffers.
    ReleaseErrorMsg(lpMsgBuf);
    env->ReleaseStringChars(name, (const jchar*)wszName);
    return (jint)INVALID_HANDLE_VALUE;
  }


  PortDCB.DCBlength = sizeof (DCB);

  // Get the default port setting information.
  GetCommState(hPort, &PortDCB);

  // Change the DCB structure settings.
  PortDCB.fBinary = TRUE;               // Binary mode; no EOF check
  PortDCB.fParity = TRUE;               // Enable parity checking
  PortDCB.fOutxDsrFlow = FALSE;         // No DSR output flow control
  PortDCB.fDtrControl = DTR_CONTROL_HANDSHAKE;// DTR flow control type
  PortDCB.fDsrSensitivity = FALSE;      // DSR sensitivity
  PortDCB.fTXContinueOnXoff = TRUE;     // XOFF continues Tx
  PortDCB.fErrorChar = FALSE;           // Disable error replacement
  PortDCB.fNull = FALSE;                // Disable null stripping
  PortDCB.fRtsControl = RTS_CONTROL_ENABLE;// RTS flow control
  PortDCB.fAbortOnError = FALSE;        // Do not abort reads/writes on error

  // Configure the port according to the specifications of the DCB structure.
  if (!SetCommState(hPort, &PortDCB))
  { // Could not set comm port state
    CreateErrorMsg(GetLastError(), lpMsgBuf);
    throw_java_exceptionW(env, PORT_IN_USE_EXCEPTION, L"open - SetCommState", lpMsgBuf);
    // Free the buffers.
    ReleaseErrorMsg(lpMsgBuf);
    env->ReleaseStringChars(name, (const jchar*)wszName);
    return (jint)INVALID_HANDLE_VALUE;
  }

  // Retrieve the time-out parameters for all read and write operations
  // on the port.
  GetCommTimeouts(hPort, &CommTimeouts);

  // Change the COMMTIMEOUTS structure settings.
  CommTimeouts.ReadIntervalTimeout = 0;
  CommTimeouts.ReadTotalTimeoutMultiplier = 0;
  CommTimeouts.ReadTotalTimeoutConstant = MAXDWORD;
  CommTimeouts.WriteTotalTimeoutMultiplier = 10;
  CommTimeouts.WriteTotalTimeoutConstant = 1000;

  // Set the time-out parameters for all read and write operations
  // on the port.
  if (!SetCommTimeouts(hPort, &CommTimeouts))
  { // Unable to set the time-out parameters
    CreateErrorMsg(GetLastError(), lpMsgBuf);
    throw_java_exceptionW(env, PORT_IN_USE_EXCEPTION, L"open - SetCommTimeouts", lpMsgBuf);
    // Free the buffers.
    ReleaseErrorMsg(lpMsgBuf);
    env->ReleaseStringChars(name, (const jchar*)wszName);
    return (jint)INVALID_HANDLE_VALUE;
  }

  // SETRTS: Sends the RTS (request-to-send) signal.
  EscapeCommFunction(hPort, SETRTS);


  if((dwErr = InitialiseEventInfoStruct(hPort, &EventInfo)) != 0)
  { // Unable to set up EventInfo structure for event processing
    CreateErrorMsg(dwErr, lpMsgBuf);
    throw_java_exceptionW(env, IO_EXCEPTION, L"open - InitialiseEventInfoStruct", lpMsgBuf);
    // Free the buffers.
    ReleaseErrorMsg(lpMsgBuf);
    env->ReleaseStringChars(name, (const jchar*)wszName);
    return (jint)INVALID_HANDLE_VALUE;
  }

  jclass cls = env->GetObjectClass(jobj);
  jfieldID jfEis = env->GetFieldID(cls, "eis", "J");
	if( !jfEis ) {
    IF_DEBUG
    (
		  env->ExceptionDescribe();
    )
		env->ExceptionClear();
    // Free the buffers.
    env->ReleaseStringChars(name, (const jchar*)wszName);
		return (jint)INVALID_HANDLE_VALUE;
	}
	env->SetIntField(jobj, jfEis, (jint)EventInfo);

  env->ReleaseStringChars(name, (const jchar*)wszName);
  // Returning HANDLE (which is a pointer) as file descriptor... Anyway, 32 bits are 32 bits
  return (jint)hPort;
}


/*
 nativeSetSerialPortParams

   accept:     speed, data bits, stop bits, parity
   perform:    set the serial port parameters
   return:     true on error
   exceptions: UnsupportedCommOperationException
 * Class:     gnu_io_RXTXPort
 * Method:    nativeSetSerialPortParams
 * Signature: (IIII)V
 */
JNIEXPORT jboolean JNICALL Java_gnu_io_RXTXPort_nativeSetSerialPortParams(
  JNIEnv *env, jobject jobj, jint speed, jint dataBits, jint stopBits,
  jint parity)
{
  DCB PortDCB;
  LPCWSTR lpMsgBuf;

  HANDLE hPort = get_fd(env, jobj);
  PortDCB.DCBlength = sizeof(DCB);

  IF_DEBUG
  (
    printj(env, L"--- RXTXPort.nativeSetSerialPortParams(%ld, %ld, %ld, %ld) called\n", speed, dataBits, stopBits, parity);
  )

  // Get the default port setting information.
  if(!GetCommState(hPort, &PortDCB))
  { //Unable to get configuration of the serial port
    CreateErrorMsg(GetLastError(), lpMsgBuf);
    throw_java_exceptionW(env, UNSUPPORTED_COMM_OPERATION, L"nativeSetSerialPortParams - GetCommState", lpMsgBuf);
    // Free the buffers.
    ReleaseErrorMsg(lpMsgBuf);
    return JNI_TRUE;
  }

  // Change the DCB structure settings.
  PortDCB.BaudRate = speed;             // Current baud
  PortDCB.ByteSize = (BYTE)dataBits;    // Number of bits/byte, 4-8
  PortDCB.Parity = (BYTE)parity;        // 0-4=no,odd,even,mark,space
  switch(stopBits)
  {
    case gnu_io_RXTXPort_STOPBITS_1:
      PortDCB.StopBits = ONESTOPBIT;
      break;

    case gnu_io_RXTXPort_STOPBITS_2:
      PortDCB.StopBits = TWOSTOPBITS;
      break;

    case gnu_io_RXTXPort_STOPBITS_1_5:
      PortDCB.StopBits = ONE5STOPBITS;
      break;

    default:
      throw_java_exceptionW(env, UNSUPPORTED_COMM_OPERATION, L"nativeSetSerialPortParams", L"Incorrect stopBits");

  }

  // Configure the port according to the specifications of the DCB structure.
  if (!SetCommState (hPort, &PortDCB))
  { //Unable to configure the serial port
    CreateErrorMsg(GetLastError(), lpMsgBuf);
    throw_java_exceptionW(env, UNSUPPORTED_COMM_OPERATION, L"nativeSetSerialPortParams - SetCommState", lpMsgBuf);
    // Free the buffers.
    ReleaseErrorMsg(lpMsgBuf);
    return JNI_TRUE;
  }
  return JNI_FALSE;
}

/*
setflowcontrol

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
 * Class:     gnu_io_RXTXPort
 * Method:    setflowcontrol
 * Signature: (I)V
 */
JNIEXPORT void JNICALL Java_gnu_io_RXTXPort_setflowcontrol(JNIEnv *env, jobject jobj, jint flowcontrol)
{
  DCB PortDCB;
  LPCWSTR lpMsgBuf;

  IF_DEBUG
  (
    printj(env, L"--- RXTXPort.setflowcontrol(%ld) called\n", flowcontrol);
  )

  HANDLE hPort = get_fd(env, jobj);

  PortDCB.DCBlength = sizeof(DCB);
  if(!GetCommState (hPort, &PortDCB))
  { //Unable to get configuration of the serial port
    CreateErrorMsg(GetLastError(), lpMsgBuf);
    throw_java_exceptionW(env, UNSUPPORTED_COMM_OPERATION, L"setflowcontrol - GetCommState", lpMsgBuf);
    // Free the buffers.
    ReleaseErrorMsg(lpMsgBuf);
    return;
  }

  if(flowcontrol & (gnu_io_RXTXPort_FLOWCONTROL_RTSCTS_IN | gnu_io_RXTXPort_FLOWCONTROL_RTSCTS_OUT))
    PortDCB.fRtsControl = RTS_CONTROL_HANDSHAKE;
  else
    PortDCB.fRtsControl = RTS_CONTROL_ENABLE;

  if(flowcontrol & gnu_io_RXTXPort_FLOWCONTROL_RTSCTS_OUT)
    PortDCB.fOutxCtsFlow = TRUE;
  else
    PortDCB.fOutxCtsFlow = FALSE;

  if(flowcontrol & gnu_io_RXTXPort_FLOWCONTROL_XONXOFF_IN)
    PortDCB.fInX = TRUE;
  else
    PortDCB.fInX = FALSE;

  if(flowcontrol & gnu_io_RXTXPort_FLOWCONTROL_XONXOFF_OUT)
    PortDCB.fOutX = TRUE;
  else
    PortDCB.fOutX = FALSE;

  // Configure the port according to the specifications of the DCB structure.
  if (!SetCommState (hPort, &PortDCB))
  { // Could not set params
    CreateErrorMsg(GetLastError(), lpMsgBuf);
    throw_java_exceptionW(env, UNSUPPORTED_COMM_OPERATION, L"setflowcontrol - SetCommState", lpMsgBuf);
    // Free the buffers.
    ReleaseErrorMsg(lpMsgBuf);
    return;
  }
}

/*
NativegetReceiveTimeout

   accept:     none
   perform:    get termios.c_cc[VTIME]
   return:     VTIME
   comments:   see  NativeEnableReceiveTimeoutThreshold
 * Class:     gnu_io_RXTXPort
 * Method:    NativegetReceiveTimeout
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_gnu_io_RXTXPort_NativegetReceiveTimeout(JNIEnv *env, jobject jobj)
{
  COMMTIMEOUTS CommTimeouts;
  LPCWSTR lpMsgBuf;
  HANDLE hPort = get_fd(env, jobj);

  IF_DEBUG
  (
    printj(env, L"--- RXTXPort.NativegetReceiveTimeout() called\n");
  )

  // Retrieve the time-out parameters for all read and write operations on the port.
  if (!GetCommTimeouts(hPort, &CommTimeouts))
  { // Unable to get the time-out parameters
    CreateErrorMsg(GetLastError(), lpMsgBuf);
    throw_java_exceptionW(env, IO_EXCEPTION, L"NativegetReceiveTimeout - GetCommTimeouts", lpMsgBuf);
    // Free the buffers.
    ReleaseErrorMsg(lpMsgBuf);
    return -1;
  }

  // As far as I know c_cc is byte table
  return CommTimeouts.ReadIntervalTimeout/100 <= 255 ? CommTimeouts.ReadIntervalTimeout / 100 : 255;
}

/*
NativeisReceiveTimeoutEnabled

   accept:     none
   perform:    determine if VTIME is none 0
   return:     JNI_TRUE if VTIME > 0 else JNI_FALSE
   comments:   see  NativeEnableReceiveTimeoutThreshold
 * Class:     gnu_io_RXTXPort
 * Method:    NativeisReceiveTimeoutEnabled
 * Signature: ()Z
 */
JNIEXPORT jboolean JNICALL Java_gnu_io_RXTXPort_NativeisReceiveTimeoutEnabled(JNIEnv *env, jobject jobj)
{
  COMMTIMEOUTS CommTimeouts;
  LPCWSTR lpMsgBuf;
  HANDLE hPort = get_fd(env, jobj);

  IF_DEBUG
  (
    printj(env, L"--- RXTXPort.NativeisReceiveTimeoutEnabled() called\n");
  )

  // Retrieve the time-out parameters for all read and write operations
  // on the port.
  if (!GetCommTimeouts(hPort, &CommTimeouts))
  { // Unable to get the time-out parameters
    CreateErrorMsg(GetLastError(), lpMsgBuf);
    throw_java_exceptionW(env, IO_EXCEPTION, L"NativeisReceiveTimeoutEnabled - GetCommTimeouts", lpMsgBuf);
    // Free the buffers.
    ReleaseErrorMsg(lpMsgBuf);
    return -1;
  }

  return CommTimeouts.ReadIntervalTimeout || CommTimeouts.ReadTotalTimeoutConstant || CommTimeouts.ReadTotalTimeoutMultiplier ? JNI_TRUE : JNI_FALSE;
}

/*
NativeEnableReceiveTimeoutThreshold
   accept:      int  threshold, int vtime,int buffer
   perform:     Set c_cc->VMIN to threshold and c_cc=>VTIME to vtime
   return:      void
   exceptions:  IOException
   comments:    This is actually all handled in read with select in
                canonical input mode.
 * Class:     gnu_io_RXTXPort
 * Method:    NativeEnableReceiveTimeoutThreshold
 * Signature: (III)V
 */
JNIEXPORT void JNICALL Java_gnu_io_RXTXPort_NativeEnableReceiveTimeoutThreshold(JNIEnv *env, jobject jobj, jint time, jint threshold, jint InputBuffer)
{
  COMMTIMEOUTS CommTimeouts;
  LPCWSTR lpMsgBuf;
  HANDLE hPort = get_fd(env, jobj);

  IF_DEBUG
  (
    printj(env, L"--- RXTXPort.NativeEnableReceiveTimeoutThreshold(%ld, %ld) called\n", time, threshold);
  )

  // Retrieve the time-out parameters for all read and write operations
  // on the port.
  if (!GetCommTimeouts(hPort, &CommTimeouts))
  { // Unable to get the time-out parameters
    CreateErrorMsg(GetLastError(), lpMsgBuf);
    throw_java_exceptionW(env, IO_EXCEPTION, L"NativeEnableReceiveTimeoutThreshold - GetCommTimeouts", lpMsgBuf);
    // Free the buffers.
    ReleaseErrorMsg(lpMsgBuf);
    return;
  }

  /* ------ from javax.comm.CommPort javadoc -------------------------------------------------------------
  |    Threshold   |    Timeout   |Read Buffer Size | Read Behaviour
  |State   |Value  |State   |Value|                 |
  |disabled|  -    |disabled| -   |    n bytes      | block until any data is available
  |enabled |m bytes|disabled| -   |    n bytes      | block until min(m,n) bytes are available
  |disabled|  -    |enabled |x ms |    n bytes      | block for x ms or until any data is available
  |enabled |m bytes|enabled |x ms |    n bytes      | block for x ms or until min(m,n) bytes are available
  --------------------------------------------------------------------------------------------------------

  Enabling the Timeout OR Threshold with a value of zero is a special case.
  This causes the underlying driver to poll for incoming data instead being event driven.
  Otherwise, the behaviour is identical to having both the Timeout and Threshold disabled.
  */

  // Following is based on my understanding of timeout parameters meaning.
  // Not completely precise (threshold?!)

  if(time == 0)
  { // polling mode - return if no data
    CommTimeouts.ReadIntervalTimeout = MAXDWORD;
    CommTimeouts.ReadTotalTimeoutMultiplier = MAXDWORD;
    CommTimeouts.ReadTotalTimeoutConstant = 1;
  }
  else if(time == -1)
  { // disable timeout
    CommTimeouts.ReadIntervalTimeout = 0;
    CommTimeouts.ReadTotalTimeoutMultiplier = 0;
    CommTimeouts.ReadTotalTimeoutConstant = 0;
  }
  else
  { // set timeout
    CommTimeouts.ReadIntervalTimeout = 0;
    CommTimeouts.ReadTotalTimeoutMultiplier = 0;
    CommTimeouts.ReadTotalTimeoutConstant = time;
  }

  // Set the time-out parameters for all read and write operations
  // on the port.
  if (!SetCommTimeouts (hPort, &CommTimeouts))
  { // Unable to set the time-out parameters
    CreateErrorMsg(GetLastError(), lpMsgBuf);
    throw_java_exceptionW(env, IO_EXCEPTION, L"NativeEnableReceiveTimeoutThreshold - SetCommTimeouts", lpMsgBuf);
    // Free the buffers.
    ReleaseErrorMsg(lpMsgBuf);
    return;
  }
}

/*
isDTR

   accept:      none
   perform:     check status of DTR
   return:      true if TIOCM_DTR is set
                false if TIOCM_DTR is not set
   exceptions:  none
   comments:    DTR stands for Data Terminal Ready
 * Class:     gnu_io_RXTXPort
 * Method:    isDTR
 * Signature: ()Z
 */
JNIEXPORT jboolean JNICALL Java_gnu_io_RXTXPort_isDTR(JNIEnv *env, jobject jobj)
{
  DCB PortDCB;
  HANDLE hPort = get_fd(env, jobj);

  PortDCB.DCBlength = sizeof(DCB);

  // Get the default port setting information.
  if(!GetCommState(hPort, &PortDCB))
  {
    //GetLastError();
    return JNI_FALSE;
  }

  // This is not completely correct: I assume that nobody's playing with
  // manually setting/resetting DTR. Instead, DTR is in handshake mode (ON unless
  // port closed) or disable mode (OFF).
  // So, don't use EscapeCommFunction(SETDTR/CLRDTR) in this library!
  return PortDCB.fDtrControl != DTR_CONTROL_DISABLE ? JNI_TRUE : JNI_FALSE;
}

/*
setDTR

   accept:      new DTR state
   perform:     if state is true, TIOCM_DTR is set
                if state is false, TIOCM_DTR is unset
   return:      none
   exceptions:  none
   comments:    DTR stands for Data Terminal Ready
 * Class:     gnu_io_RXTXPort
 * Method:    setDTR
 * Signature: (Z)V
 */
JNIEXPORT void JNICALL Java_gnu_io_RXTXPort_setDTR(JNIEnv *env, jobject jobj, jboolean state)
{
  DCB PortDCB;

  HANDLE hPort = get_fd(env, jobj);
  PortDCB.DCBlength = sizeof(DCB);

  // Don't use EscapeCommFunction(SETDTR/CLRDTR) in this library!

  // Get the default port setting information.
  if(!GetCommState (hPort, &PortDCB))
  { //Unable to get configuration of the serial port
    //GetLastError();
    return;
  }

  if(state == JNI_TRUE)
    PortDCB.fDtrControl = DTR_CONTROL_HANDSHAKE;
  else
    PortDCB.fDtrControl = DTR_CONTROL_DISABLE;


  // Configure the port according to the specifications of the DCB structure.
  if (!SetCommState (hPort, &PortDCB))
  { //Unable to configure the serial port
    //GetLastError();
    return;
  }
}

/*
setRTS

   accept:      state  flag to set/unset.
   perform:     depends on the state flag
                if true TIOCM_RTS is set
                if false TIOCM_RTS is unset
   return:      none
   exceptions:  none
   comments:    tcsetattr with c_cflag CRTS_IFLOW
 * Class:     gnu_io_RXTXPort
 * Method:    setRTS
 * Signature: (Z)V
 */
JNIEXPORT void JNICALL Java_gnu_io_RXTXPort_setRTS(JNIEnv *env, jobject jobj, jboolean state)
{
  HANDLE hPort = get_fd(env, jobj);

  // EscapeCommFunction will fail if there's RTS/CTS flowcontrol. If it is incorrect,
  // we could turn flowcontrol off when getting to this function
  if(state == JNI_TRUE)
    EscapeCommFunction(hPort, SETRTS);
  else
    EscapeCommFunction(hPort, CLRRTS);
}

/*
setDSR

   accept:      state  flag to set/unset.
   perform:     depends on the state flag
                if true TIOCM_DSR is set
                if false TIOCM_DSR is unset
   return:      none
   exceptions:  none
   comments:    tcsetattr with c_cflag CRTS_IFLOW
 * Class:     gnu_io_RXTXPort
 * Method:    setDSR
 * Signature: (Z)V
 */
JNIEXPORT void JNICALL Java_gnu_io_RXTXPort_setDSR(JNIEnv *env, jobject jobj, jboolean state)
{
  DCB PortDCB;

  HANDLE hPort = get_fd(env, jobj);
  PortDCB.DCBlength = sizeof(DCB);

  // Get the default port setting information.
  if(!GetCommState (hPort, &PortDCB))
  { //Unable to get configuration of the serial port
    //GetLastError();
    return;
  }

  // Change the DCB structure settings.
  if(state != JNI_FALSE)
    PortDCB.fOutxDsrFlow = TRUE;
  else
    PortDCB.fOutxDsrFlow = FALSE;

  // Configure the port according to the specifications of the DCB
  // structure.
  if (!SetCommState (hPort, &PortDCB))
  { //Unable to configure the serial port
    //GetLastError();
    return;
  }
}

/*
isCTS

   accept:      none
   perform:     check status of CTS
   return:      true if TIOCM_CTS is set
                false if TIOCM_CTS is not set
   exceptions:  none
   comments:    CTS stands for Clear To Send.
 * Class:     gnu_io_RXTXPort
 * Method:    isCTS
 * Signature: ()Z
 */
JNIEXPORT jboolean JNICALL Java_gnu_io_RXTXPort_isCTS(JNIEnv *env, jobject jobj)
{
  DWORD ModemStat;
  HANDLE hPort = get_fd(env, jobj);

  if(!GetCommModemStatus(hPort, &ModemStat))
    return JNI_FALSE;

  return ModemStat|MS_CTS_ON ? JNI_TRUE : JNI_FALSE;
}

/*
isDSR

   accept:      none
   perform:     check status of DSR
   return:      true if TIOCM_DSR is set
                false if TIOCM_DSR is not set
   exceptions:  none
   comments:    DSR stands for Data Set Ready
 * Class:     gnu_io_RXTXPort
 * Method:    isDSR
 * Signature: ()Z
 */
JNIEXPORT jboolean JNICALL Java_gnu_io_RXTXPort_isDSR(JNIEnv *env, jobject jobj)
{
  DWORD ModemStat;
  HANDLE hPort = get_fd(env, jobj);

  if(!GetCommModemStatus(hPort, &ModemStat))
    return JNI_FALSE;

  return ModemStat|MS_DSR_ON ? JNI_TRUE : JNI_FALSE;
}

/*
isCD

   accept:      none
   perform:     check status of CD
   return:      true if TIOCM_CD is set
                false if TIOCM_CD is not set
   exceptions:  none
   comments:    CD stands for Carrier Detect
                The following comment has been made...
                "well, it works, there might ofcourse be a bug, but making DCD
                permanently on fixed it for me so I don't care"
 * Class:     gnu_io_RXTXPort
 * Method:    isCD
 * Signature: ()Z
 */
JNIEXPORT jboolean JNICALL Java_gnu_io_RXTXPort_isCD(JNIEnv *env, jobject jobj)
{
  DWORD ModemStat;
  HANDLE hPort = get_fd(env, jobj);

  if(!GetCommModemStatus(hPort, &ModemStat))
    return JNI_FALSE;

  return ModemStat|MS_RLSD_ON ? JNI_TRUE : JNI_FALSE;
}

/*
isRI

   accept:      none
   perform:     check status of RI
   return:      true if TIOCM_RI is set
                false if TIOCM_RI is not set
   exceptions:  none
   comments:    RI stands for Ring Indicator
 * Class:     gnu_io_RXTXPort
 * Method:    isRI
 * Signature: ()Z
 */
JNIEXPORT jboolean JNICALL Java_gnu_io_RXTXPort_isRI(JNIEnv *env, jobject jobj)
{
  DWORD ModemStat;
  HANDLE hPort = get_fd(env, jobj);

  if(!GetCommModemStatus(hPort, &ModemStat))
    return JNI_FALSE;

  return ModemStat|MS_RING_ON ? JNI_TRUE : JNI_FALSE;
}

/*
isRTS

   accept:      none
   perform:     check status of RTS
   return:      true if TIOCM_RTS is set
                false if TIOCM_RTS is not set
   exceptions:  none
   comments:    tcgetattr with c_cflag CRTS_IFLOW
 * Class:     gnu_io_RXTXPort
 * Method:    isRTS
 * Signature: ()Z
 */
JNIEXPORT jboolean JNICALL Java_gnu_io_RXTXPort_isRTS(JNIEnv *env, jobject jobj)
{
  DCB PortDCB;
  HANDLE hPort = get_fd(env, jobj);

  PortDCB.DCBlength = sizeof(DCB);
  if(!GetCommState (hPort, &PortDCB))
  { //Unable to get configuration of the serial port
    //GetLastError();
    return JNI_FALSE;
  }

  if(PortDCB.fRtsControl == RTS_CONTROL_DISABLE)
    return JNI_FALSE;

  if(PortDCB.fRtsControl == RTS_CONTROL_ENABLE)
    return JNI_TRUE;

  if(PortDCB.fRtsControl == RTS_CONTROL_HANDSHAKE)
  {
    IF_DEBUG
    (
      //MessageBox(NULL, L"Undefined condition: isRTS() but control is RTS_CONTROL_HANDSHAKE\r\nReturning FALSE", L"Warning", MB_OK | MB_ICONWARNING | MB_SETFOREGROUND);
      printj(env,
        L"Undefined condition: isRTS() but control is RTS_CONTROL_HANDSHAKE. Returning FALSE\n");
    )
    return JNI_FALSE;
  }

  return JNI_FALSE;
}

/*
sendBreak

   accept:     duration in milliseconds.
   perform:    send break for actual time.  not less than 0.25 seconds.
   exceptions: none
   comments:   not very precise
 * Class:     gnu_io_RXTXPort
 * Method:    sendBreak
 * Signature: (I)V
 */
JNIEXPORT void JNICALL Java_gnu_io_RXTXPort_sendBreak(JNIEnv *env, jobject jobj, jint duration)
{
  HANDLE hPort = get_fd(env, jobj);

  SetCommBreak(hPort);
  Sleep(duration);
  ClearCommBreak(hPort);
}

/*
writeByte

   accept:      byte to write (passed as int)
   perform:     write a single byte to the port
   return:      none
   exceptions:  IOException
 * Class:     gnu_io_RXTXPort
 * Method:    writeByte
 * Signature: (IZ)V
 */
JNIEXPORT void JNICALL Java_gnu_io_RXTXPort_writeByte(JNIEnv *env, jobject jobj, jint b, jboolean i)
{
  DWORD dwNumBytesWritten;
  LPCWSTR lpMsgBuf;
  HANDLE hPort = get_fd(env, jobj);
  BYTE bb = (BYTE)b;

  do {
    if (!WriteFile(hPort,              // Port handle
                   &bb,                // Pointer to the data to write
                   1,                  // Number of bytes to write
                   &dwNumBytesWritten, // Pointer to the number of bytes written
                   NULL))              // Must be NULL for Windows CE
    { // WriteFile failed. Report error.
      CreateErrorMsg(GetLastError(), lpMsgBuf);
      throw_java_exceptionW(env, IO_EXCEPTION, L"writeByte - WriteFile", lpMsgBuf );
      // Free the buffers.
      ReleaseErrorMsg(lpMsgBuf);
      return;
    }
  } while(dwNumBytesWritten == 0);
}

/*
writeArray

   accept:      jbarray: bytes used for writing
                off: offset in array to start writing
                len: Number of bytes to write
   perform:     write length bytes of jbarray
   return:      none
   exceptions:  IOException
 * Class:     gnu_io_RXTXPort
 * Method:    writeArray
 * Signature: ([BIIZ)V
 */
JNIEXPORT void JNICALL Java_gnu_io_RXTXPort_writeArray(JNIEnv *env, jobject jobj, jbyteArray b, jint off, jint len, jboolean i)
{
  LPCWSTR lpMsgBuf;
	DWORD dwNumBytesWritten;
  jint total=0;
  HANDLE hPort = get_fd(env, jobj);

  jbyte *body = env->GetByteArrayElements(b, NULL);

	do {
    IF_DEBUG
    (
      printj(env, L"--- writeArray - %d bytes to write\n", len-total);
    )
    if (!WriteFile(hPort,              // Port handle
                   body+total+off,     // Pointer to the data to write
                   len-total,          // Number of bytes to write
                   &dwNumBytesWritten, // Pointer to the number of bytes written
                   NULL))              // Must be NULL for Windows CE
    { // WriteFile failed. Report error.
      CreateErrorMsg(GetLastError(), lpMsgBuf);
      throw_java_exceptionW(env, IO_EXCEPTION, L"writeArray - WriteFile", lpMsgBuf );
      // Free the buffers.
      ReleaseErrorMsg(lpMsgBuf);
      env->ReleaseByteArrayElements(b, body, JNI_ABORT);
      return;
    }
    total += dwNumBytesWritten;
	} while( total < len );

  env->ReleaseByteArrayElements(b, body, JNI_ABORT);
}

/*
nativeDrain

   accept:      none
   perform:     wait until all data is transmitted
   return:      none
   exceptions:  IOException
   comments:    java.io.OutputStream.flush() is equivalent to tcdrain,
                not tcflush, which throws away unsent bytes

                count logic added to avoid infinite loops when EINTR is
                true...  Thread.yeild() was suggested.
 * Class:     gnu_io_RXTXPort
 * Method:    nativeDrain
 * Signature: ()V
 */
JNIEXPORT jboolean JNICALL Java_gnu_io_RXTXPort_nativeDrain(JNIEnv *env, jobject jobj, jboolean i)
{
  //COMSTAT Stat;
  //DWORD dwErrors;
  //HANDLE hPort = get_fd(env, jobj);

  IF_DEBUG
  (
    printj(env, L"--- RXTXPort.nativeDrain() called\n");
  )
  /*
  if(!FlushFileBuffers(hPort))
  {
    IF_DEBUG
    (
      printj(env, L"!!! FlushFileBuffers() error %ld\n", GetLastError());
    )
    return JNI_FALSE;
  }
   */
  // Alternative implementation:
  /*
  do
  {
    if(!ClearCommError(hPort, &dwErrors, &Stat))
    {
      GetLastError();
      return;
    }
    Sleep(10);
  } while(Stat.cbOutQue > 0);*/

  return JNI_FALSE;
}

/*
nativeavailable

   accept:      none
   perform:     find out the number of bytes available for reading
   return:      available bytes
                -1 on error
   exceptions:  none /// should be IOException
 * Class:     gnu_io_RXTXPort
 * Method:    nativeavailable
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_gnu_io_RXTXPort_nativeavailable(JNIEnv *env, jobject jobj)
{
  DWORD dwErrors;
  COMSTAT Stat;
  LPCWSTR lpMsgBuf;
  HANDLE hPort = get_fd(env, jobj);

  IF_DEBUG
  (
    printj(env, L"--- RXTXPort.nativeavailable() called\n");
  )

  if(!ClearCommError(hPort, &dwErrors, &Stat))
  {
    CreateErrorMsg(GetLastError(), lpMsgBuf);
    throw_java_exceptionW(env, IO_EXCEPTION, L"nativeavailable - ClearCommError", lpMsgBuf );
    // Free the buffers.
    ReleaseErrorMsg(lpMsgBuf);
    return -1;
  }

  IF_DEBUG
  (
    printj(env, L" - returning %ld\n", Stat.cbInQue);
  )

  return Stat.cbInQue;
}

/*
readByte

   accept:      none
   perform:     Read a single byte from the port
   return:      The byte read
   exceptions:  IOException
 * Class:     gnu_io_RXTXPort
 * Method:    readByte
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_gnu_io_RXTXPort_readByte(JNIEnv *env, jobject jobj)
{
  BYTE bb;
  DWORD dwNumBytesRead;
  LPCWSTR lpMsgBuf;
  HANDLE hPort = get_fd(env, jobj);

  if (!ReadFile(hPort, &bb, 1, &dwNumBytesRead, NULL))
  { // ReadFile failed. Report error.
    CreateErrorMsg(GetLastError(), lpMsgBuf);
    throw_java_exceptionW(env, IO_EXCEPTION, L"readByte - ReadFile", lpMsgBuf);
    // Free the buffers.
    ReleaseErrorMsg(lpMsgBuf);
    return -1;
  }

  return dwNumBytesRead > 0 ? bb : -1;
}

/*
readArray

   accept:       offset (offset to start storing data in the jbarray) and
                 Length (bytes to read)
   perform:      read bytes from the port into a byte array
   return:       bytes read on success
                 0 on read timeout
   exceptions:   IOException
   comments:     throws ArrayIndexOutOfBoundsException if asked to
                 read more than SSIZE_MAX bytes
 * Class:     gnu_io_RXTXPort
 * Method:    readArray
 * Signature: ([BII)I
 */
JNIEXPORT jint JNICALL Java_gnu_io_RXTXPort_readArray(JNIEnv *env, jobject jobj, jbyteArray b, jint off, jint len)
{
  LPCWSTR lpMsgBuf;
	DWORD dwNumBytesRead, dwTotalRead = 0;
  jint threshold;
  HANDLE hPort = get_fd(env, jobj);

  if(len < 0)
  {
    throw_java_exceptionW(env, ARRAY_INDEX_OUT_OF_BOUNDS, L"readArray", L"Negative number of character to read");
    return -1;
  }

  jbyte *body = env->GetByteArrayElements(b, NULL);
  threshold = get_java_var_long(env, jobj, "threshold", "I");
  
  if (threshold > len)
    threshold = len;
  do
  {
    if (!ReadFile(hPort, body + off + dwTotalRead, len - dwTotalRead,
        &dwNumBytesRead, NULL))
    { // ReadFile failed. Report error.
      CreateErrorMsg(GetLastError(), lpMsgBuf);
      throw_java_exceptionW(env, IO_EXCEPTION, L"readArray - ReadFile",
                            lpMsgBuf);
      // Free the buffers.
      ReleaseErrorMsg(lpMsgBuf);
      env->ReleaseByteArrayElements(b, body, JNI_ABORT);
      return -1;
    }

    dwTotalRead += dwNumBytesRead;
  } while (dwNumBytesRead > 0 && (jint)dwTotalRead < threshold);

  env->ReleaseByteArrayElements(b, body, 0);
  return dwTotalRead;
}

/*
nativeClearCommInput

   accept:       none
   perform:      try to clear the input.
   return:       true on success, false on error
   exceptions:   none
   comments:     This is an extension to commapi.
*/
JNIEXPORT jboolean JNICALL Java_gnu_io_RXTXPort_nativeClearCommInput(
  JNIEnv *env, jobject jobj)
{
  throw_java_exceptionW(env, UNSUPPORTED_COMM_OPERATION,
    L"nativeClearCommInput", L"Operation not implemented");
  return( JNI_FALSE );
}

/*
readTerminatedArray

   accept:       offset (offset to start storing data in the jbarray) and
                 Length (bytes to read).  Terminator - 2 bytes that we
		 dont read past
   perform:      read bytes from the port into a byte array
   return:       bytes read on success
                 0 on read timeout
   exceptions:   IOException
   comments:     throws ArrayIndexOutOfBoundsException if asked to
                 read more than SSIZE_MAX bytes
		 timeout is not properly handled

		 This is an extension to commapi.
*/
JNIEXPORT jint JNICALL Java_gnu_io_RXTXPort_readTerminatedArray(JNIEnv *env,
  jobject jobj, jbyteArray jbarray, jint offset, jint length,
  jbyteArray jterminator)
{
  throw_java_exceptionW(env, UNSUPPORTED_COMM_OPERATION,
    L"readTerminatedArray", L"Operation not implemented");
  return 0;
}

/*
eventLoop

   accept:      none
   perform:     periodically check for SerialPortEvents
   return:      none
   exceptions:  none
   comments:	please keep this function clean.
 * Class:     gnu_io_RXTXPort
 * Method:    eventLoop
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_gnu_io_RXTXPort_eventLoop(JNIEnv *env, jobject jobj)
{
  jfieldID jfMonitorThreadLock, jfMonThreadisInterrupted;
  jmethodID jmSendEvent;
  HANDLE hCommEventThread;
  DWORD dwThreadID, dwWaitResult, dwEvent;
  int RetVal;
  //HANDLE hPort = get_fd(env, jobj);
  EventInfoStruct *EventInfo = get_eis(env, jobj);

  IF_DEBUG
  (
    printj(env, L"--- RXTXPort.eventLoop() start\n");
  )

  jclass cls = env->GetObjectClass(jobj);

  // Get pointers to some Java variables and methods:
  if( !(jfMonitorThreadLock = env->GetFieldID(cls, "MonitorThreadLock", "Z")) ||
      !(jfMonThreadisInterrupted = env->GetFieldID(cls, "monThreadisInterrupted", "Z")) ||
      !(jmSendEvent = env->GetMethodID(cls, "sendEvent", "(IZ)Z"))
    )
  {
    IF_DEBUG
    (
		  env->ExceptionDescribe();
    )
	  env->ExceptionClear();
	  return;
  }

  hCommEventThread = CreateThread(NULL, 0, CommEventThread, (LPVOID)EventInfo, 0, &dwThreadID);
  if(hCommEventThread == NULL)
  {
    IF_DEBUG
    (
      LPCWSTR lpMsgBuf;
      CreateErrorMsg(GetLastError(), lpMsgBuf);
      printj(env, L"!!! eventLoop - CreateThread() error: %s\n", lpMsgBuf);
      ReleaseErrorMsg(lpMsgBuf);
    )
    return;
  }

  CloseHandle(hCommEventThread);

  do
  {
    if(env->GetBooleanField(jobj, jfMonThreadisInterrupted) == JNI_TRUE)
    {
      IF_DEBUG
      (
        printj(env, L"--- RXTXPort.eventLoop() interrupted - exiting\n");
      )

      CloseHandle(EventInfo->eventHandle);
      return;
    }

    if(EventInfo->eventThreadReady)
    { // Thread is ready to work - pass signal to Java
      IF_DEBUG
      (
        printj(env, L"--- RXTXPort.eventLoop() - EventThread is ready\n");
      )
      env->SetBooleanField(jobj, jfMonitorThreadLock, JNI_FALSE);
      EventInfo->eventThreadReady = false;
    }

    if((dwWaitResult = WaitForSingleObject(EventInfo->eventHandle, 250)) == WAIT_FAILED)
    {
      IF_DEBUG
      (
        LPCWSTR lpMsgBuf;
        CreateErrorMsg(GetLastError(), lpMsgBuf);
        printj(env, L"!!! eventLoop - WaitForSingleObject() error: %s\n", lpMsgBuf);
        ReleaseErrorMsg(lpMsgBuf);
      )
      CloseHandle(EventInfo->eventHandle);
      return;
    }

    if(dwWaitResult == WAIT_OBJECT_0)
    {
      dwEvent = EventInfo->event;
      // Clearing event - event thread will continue
      EventInfo->event = 0;
      // Send events to Java
      if((RetVal = SendEvents(env, jobj, dwEvent, EventInfo, jmSendEvent)) != 0)
      {
        IF_DEBUG
        (
          printj(env, L"!!! eventLoop - SendEvents() result: %d\n", RetVal);
        )
        CloseHandle(EventInfo->eventHandle);
        return;
      }
    }
    else
    {
      IF_DEBUG
      (
        ;//printj(env, L"--- eventLoop() looping (wait event timeout)\n");
        //MessageBeep(MB_OK);
      )
    }
  } while(TRUE);
}


/*
interruptEventLoop

   accept:      nothing
   perform:     sets monThreadisInterrupted
   return:      nothing
   exceptions:  none
   comments:    real monitoring thread will stay until port is closed
 * Class:     gnu_io_RXTXPort
 * Method:    interruptEventLoop
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_gnu_io_RXTXPort_interruptEventLoop(JNIEnv *env, jobject jobj)
{
  jfieldID jfid;
  //HANDLE hPort = get_fd(env, jobj);
  jclass cls = env->GetObjectClass(jobj);

  IF_DEBUG
  (
    printj(env, L"--- interrupt eventLoop()\n");
  )
  
  jfid = env->GetFieldID(cls, "monThreadisInterrupted", "Z");
  env->SetBooleanField(jobj, jfid, JNI_TRUE);
}


/*
 nativeSetEventFlag

   accept:      fd for finding the struct, event to flag, flag.
   perform:     toggle the flag
   return:      none
   exceptions:  none
   comments:	all the logic used to be done in Java but its too noisy
 * Class:     gnu_io_RXTXPort
 * Method:    nativeSetEventFlag
 * Signature: (IIZ)V
 */
JNIEXPORT void JNICALL Java_gnu_io_RXTXPort_nativeSetEventFlag(JNIEnv *env, jobject jobj, jint fd, jint event, jboolean flag)
{
  DWORD NewFlag;
  EventInfoStruct *EventInfo = get_eis(env, jobj);
  HANDLE hPort = (HANDLE)fd;

  IF_DEBUG
  (
    printj(env, L"--- nativeSetEventFlag(%ld, %d) called\n", event, (int)flag);
  )

  switch(event)
  {
    case SPE_DATA_AVAILABLE:
      NewFlag = EV_RXCHAR;
    break;

    case SPE_OUTPUT_BUFFER_EMPTY:
      NewFlag = EV_TXEMPTY;
    break;

    case SPE_CTS:
      NewFlag = EV_CTS;
    break;

    case SPE_DSR:
      NewFlag = EV_DSR;
    break;

    case SPE_RI:
      NewFlag = EV_RING;
    break;

    case SPE_CD:
      NewFlag = EV_RLSD;
    break;

    case SPE_OE:
    case SPE_PE:
    case SPE_FE:
      NewFlag = EV_ERR;
    break;

    case SPE_BI:
      NewFlag = EV_BREAK;
    break;

    default:
      /* Shouldn't happen. */
      /* Return here to prevent warning about uninitialized NewFlag. */
      return;
  }

  if(flag == JNI_TRUE)
    EventInfo->ef = EventInfo->ef | NewFlag;
  else
    EventInfo->ef = EventInfo->ef & ~NewFlag;

  if(!SetCommMask(hPort, EventInfo->ef))
  {
    IF_DEBUG
    (
      DWORD dwErr = GetLastError();
      LPCWSTR lpMsgBuf;
      CreateErrorMsg(dwErr, lpMsgBuf);
      printj(env, L"!!! nativeSetEventFlag - SetCommMask() error: %ld %s\n", dwErr, lpMsgBuf );
      ReleaseErrorMsg(lpMsgBuf);
    )
    return;
  }

}

/*
nativeClose

   accept:      none
   perform:     get the fd from the java end and close it
   return:      none
   exceptions:  none
 * Class:     gnu_io_RXTXPort
 * Method:    nativeClose
 * Signature: (Ljava/lang/String{ })V
 */
JNIEXPORT void JNICALL Java_gnu_io_RXTXPort_nativeClose(JNIEnv *env, jobject jobj, jstring name)
{
  HANDLE hPort = get_fd(env, jobj);

  IF_DEBUG
  (
    LPCWSTR wszName = (LPCWSTR)env->GetStringChars(name, NULL);
    printj(env, L"--- RXTXPort.nativeClose(%s) called\n", wszName);
    env->ReleaseStringChars(name, (const jchar*)wszName);
  )

  (void)CloseHandle(hPort);
  //GetLastError();
}

/*
nativeGetParityErrorChar

   accept:      -
   perform:     check the ParityErrorChar
   return:      The ParityErrorChar as an jbyte.
   exceptions:  UnsupportedCommOperationException if not implemented
   comments:    It appears the Parity char is usually \0.  The windows
		API allows for this to be changed.  I cant find may
		examples of this being done.  Maybe for a reason.

		Use a direct call to the termios file until we find a
		solution.
 * Class:     gnu_io_RXTXPort
 * Method:    nativeGetParityErrorChar
 * Signature: ()I
 */
JNIEXPORT jbyte JNICALL Java_gnu_io_RXTXPort_nativeGetParityErrorChar(JNIEnv *env, jobject jobj)
{
  DCB PortDCB;
  LPCWSTR lpMsgBuf;

  HANDLE hPort = get_fd(env, jobj);
  PortDCB.DCBlength = sizeof(DCB);

  IF_DEBUG
  (
    printj(env, L"--- RXTXPort.nativeGetParityErrorChar() called\n");
  )

  // Get port setting information.
  if(!GetCommState(hPort, &PortDCB))
  { //Unable to get configuration of the serial port
    CreateErrorMsg(GetLastError(), lpMsgBuf);
    IF_DEBUG
    (
      printj(env, L"!!! nativeGetParityErrorChar - GetCommState() error: %s\n", lpMsgBuf);
    )
    ReleaseErrorMsg(lpMsgBuf);
    return -1;
  }

  return (jbyte)PortDCB.ErrorChar;
}

/*
nativeSetParityErrorChar

   accept:      the ParityArrorCharacter as an int.
   perform:     Set the ParityErrorChar
   return:      JNI_TRUE on success
   exceptions:  UnsupportedCommOperationException if not implemented
   comments:    It appears the Parity char is usually \0.  The windows
		API allows for this to be changed.  I cant find may
		examples of this being done.  Maybe for a reason.

		Use a direct call to the termios file until we find a
		solution.
 * Class:     gnu_io_RXTXPort
 * Method:    nativeSetParityErrorChar
 * Signature: (B)Z
 */
JNIEXPORT jboolean JNICALL Java_gnu_io_RXTXPort_nativeSetParityErrorChar(JNIEnv *env, jobject jobj, jbyte b)
{
  DCB PortDCB;
  LPCWSTR lpMsgBuf;

  HANDLE hPort = get_fd(env, jobj);
  PortDCB.DCBlength = sizeof(DCB);

  IF_DEBUG
  (
    printj(env, L"--- RXTXPort.nativeSetParityErrorChar(%#x) called\n", (int)b);
  )

  // Get port setting information.
  if(!GetCommState(hPort, &PortDCB))
  { //Unable to get configuration of the serial port
    CreateErrorMsg(GetLastError(), lpMsgBuf);
    IF_DEBUG
    (
      printj(env, L"!!! nativeSetParityErrorChar - GetCommState() error: %s\n", lpMsgBuf);
    )
    ReleaseErrorMsg(lpMsgBuf);
    return JNI_FALSE;
  }

  if(b != 0)
  {
    PortDCB.fErrorChar = TRUE;
    PortDCB.fParity = TRUE;
    PortDCB.ErrorChar = b;
  }
  else
  {
    PortDCB.fErrorChar = FALSE;
    PortDCB.fParity = FALSE;
    PortDCB.ErrorChar = 0;
  }

  if (!SetCommState(hPort, &PortDCB))
  {
    CreateErrorMsg(GetLastError(), lpMsgBuf);
    IF_DEBUG
    (
      printj(env, L"!!! nativeSetParityErrorChar - SetCommState() error: %s\n", lpMsgBuf);
    )
    ReleaseErrorMsg(lpMsgBuf);
    return JNI_FALSE;
  }

  return JNI_TRUE;
}

/*
nativeGetEndOfInputChar

   accept:      -
   perform:     check the EndOf InputChar
   return:      the EndOfInputChar as an jbyte.  -1 on error
   exceptions:  UnsupportedCommOperationException if not implemented
   comments:
 * Class:     gnu_io_RXTXPort
 * Method:    nativeGetEndOfInputChar
 * Signature: ()I
 */
JNIEXPORT jbyte JNICALL Java_gnu_io_RXTXPort_nativeGetEndOfInputChar(JNIEnv *env, jobject jobj)
{
  DCB PortDCB;
  LPCWSTR lpMsgBuf;

  HANDLE hPort = get_fd(env, jobj);
  PortDCB.DCBlength = sizeof(DCB);

  IF_DEBUG
  (
    printj(env, L"--- RXTXPort.nativeGetEndOfInputChar() called\n");
  )

  // Get port setting information.
  if(!GetCommState(hPort, &PortDCB))
  { //Unable to get configuration of the serial port
    CreateErrorMsg(GetLastError(), lpMsgBuf);
    IF_DEBUG
    (
      printj(env, L"!!! nativeGetEndOfInputChar - GetCommState() error: %s\n", lpMsgBuf);
    )
    ReleaseErrorMsg(lpMsgBuf);
    return -1;
  }

  return (jbyte)PortDCB.EofChar;
}


/*
nativeSetEndOfInputChar

   accept:      The EndOfInputChar as an int
   perform:     set the EndOfInputChar
   return:      JNI_TRUE on success
   exceptions:  UnsupportedCommOperationException if not implemented
   comments:    This may cause troubles on Windows.
		Lets give it a shot and see what happens.

		See termios.c for the windows bits.

		EofChar = val;
		fBinary = false;  //winapi docs say always use true. ?
 * Class:     gnu_io_RXTXPort
 * Method:    nativeSetEndOfInputChar
 * Signature: (B)Z
 */
JNIEXPORT jboolean JNICALL Java_gnu_io_RXTXPort_nativeSetEndOfInputChar(JNIEnv *env, jobject jobj, jbyte b)
{
  DCB PortDCB;
  LPCWSTR lpMsgBuf;

  HANDLE hPort = get_fd(env, jobj);
  PortDCB.DCBlength = sizeof(DCB);

  IF_DEBUG
  (
    printj(env, L"--- RXTXPort.nativeSetEndOfInputChar(%#x) called\n", (int)b);
  )

  // Get port setting information.
  if(!GetCommState(hPort, &PortDCB))
  { //Unable to get configuration of the serial port
    CreateErrorMsg(GetLastError(), lpMsgBuf);
    IF_DEBUG
    (
      printj(env, L"!!! nativeSetEndOfInputChar - GetCommState() error: %s\n", lpMsgBuf);
    )
    ReleaseErrorMsg(lpMsgBuf);
    return JNI_FALSE;
  }

  if( b != 0)
  {
    PortDCB.fBinary = FALSE;
    PortDCB.EofChar = b;
  }
  else
  {
    PortDCB.fBinary = TRUE;
    PortDCB.EofChar = 0;
  }

  if (!SetCommState(hPort, &PortDCB))
  {
    CreateErrorMsg(GetLastError(), lpMsgBuf);
    IF_DEBUG
    (
      printj(env, L"!!! nativeSetEndOfInputChar - SetCommState() error: %s\n", lpMsgBuf);
    )
    throw_java_exceptionW(env, UNSUPPORTED_COMM_OPERATION, L"nativeSetEndOfInputChar", lpMsgBuf);
    ReleaseErrorMsg(lpMsgBuf);
    return JNI_FALSE;
  }

  return JNI_TRUE;
}


/*
 * Class:     gnu_io_RXTXPort
 * Method:    nativeSetUartType
 * Signature: (Ljava/lang/String{ }Z)Z
 */
JNIEXPORT jboolean JNICALL Java_gnu_io_RXTXPort_nativeSetUartType(JNIEnv *env, jobject jobj, jstring type, jboolean test)
{
  throw_java_exceptionW(env, UNSUPPORTED_COMM_OPERATION, L"nativeSetUartType", L"Operation not implemented");
  return JNI_FALSE;
}

/*
 * Class:     gnu_io_RXTXPort
 * Method:    nativeGetUartType
 * Signature: ()Ljava/lang/String{ }
 */
JNIEXPORT jstring JNICALL Java_gnu_io_RXTXPort_nativeGetUartType(JNIEnv *env, jobject jobj)
{
  throw_java_exceptionW(env, UNSUPPORTED_COMM_OPERATION, L"nativeSetUartType", L"Operation not implemented");
  return env->NewStringUTF("Unknown");
}

/*
 * Class:     gnu_io_RXTXPort
 * Method:    nativeSetBaudBase
 * Signature: (I)Z
 */
JNIEXPORT jboolean JNICALL Java_gnu_io_RXTXPort_nativeSetBaudBase(JNIEnv *env, jobject jobj, jint baudBase)
{
  throw_java_exceptionW(env, UNSUPPORTED_COMM_OPERATION, L"nativeSetBaudBase", L"Operation not implemented");
  return JNI_FALSE;
}

/*
 * Class:     gnu_io_RXTXPort
 * Method:    nativeGetBaudBase
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_gnu_io_RXTXPort_nativeGetBaudBase(JNIEnv *env, jobject jobj)
{
  throw_java_exceptionW(env, UNSUPPORTED_COMM_OPERATION, L"nativeGetBaudBase", L"Operation not implemented");
  return -1;
}

/*
 * Class:     gnu_io_RXTXPort
 * Method:    nativeSetDivisor
 * Signature: (I)Z
 */
JNIEXPORT jboolean JNICALL Java_gnu_io_RXTXPort_nativeSetDivisor(JNIEnv *env, jobject jobj, jint divisor)
{
  throw_java_exceptionW(env, UNSUPPORTED_COMM_OPERATION, L"nativeSetDivisor", L"Operation not implemented");
  return JNI_FALSE;
}

/*
 * Class:     gnu_io_RXTXPort
 * Method:    nativeGetDivisor
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_gnu_io_RXTXPort_nativeGetDivisor(JNIEnv *env, jobject jobj)
{
  throw_java_exceptionW(env, UNSUPPORTED_COMM_OPERATION, L"nativeGetDivisor", L"Operation not implemented");
  return -1;
}

/*
 * Class:     gnu_io_RXTXPort
 * Method:    nativeSetLowLatency
 * Signature: ()Z
 */
JNIEXPORT jboolean JNICALL Java_gnu_io_RXTXPort_nativeSetLowLatency(JNIEnv *env, jobject jobj)
{
  throw_java_exceptionW(env, UNSUPPORTED_COMM_OPERATION, L"nativeSetLowLatency", L"Operation not implemented");
  return JNI_FALSE;
}

/*
 * Class:     gnu_io_RXTXPort
 * Method:    nativeGetLowLatency
 * Signature: ()Z
 */
JNIEXPORT jboolean JNICALL Java_gnu_io_RXTXPort_nativeGetLowLatency(JNIEnv *env, jobject jobj)
{
  throw_java_exceptionW(env, UNSUPPORTED_COMM_OPERATION, L"nativeGetLowLatency", L"Operation not implemented");
  return JNI_FALSE;
}

/*
 * Class:     gnu_io_RXTXPort
 * Method:    nativeSetCallOutHangup
 * Signature: (Z)Z
 */
JNIEXPORT jboolean JNICALL Java_gnu_io_RXTXPort_nativeSetCallOutHangup(JNIEnv *env, jobject jobj, jboolean noHup)
{
  throw_java_exceptionW(env, UNSUPPORTED_COMM_OPERATION, L"nativeSetCallOutHangup", L"Operation not implemented");
  return JNI_FALSE;
}

/*
 * Class:     gnu_io_RXTXPort
 * Method:    nativeGetCallOutHangup
 * Signature: ()Z
 */
JNIEXPORT jboolean JNICALL Java_gnu_io_RXTXPort_nativeGetCallOutHangup(JNIEnv *env, jobject jobj)
{
  throw_java_exceptionW(env, UNSUPPORTED_COMM_OPERATION, L"nativeGetCallOutHangup", L"Operation not implemented");
  return JNI_FALSE;
}

