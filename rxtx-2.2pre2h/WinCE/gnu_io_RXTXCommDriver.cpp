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

extern "C" {
  /* Instead of these declarations, we could just include the auto-generated
     gnu_io_CommPortIdentifier.h and gnu_io_RXTXVersion.h. */
  JNIEXPORT jstring JNICALL Java_gnu_io_CommPortIdentifier_native_1psmisc_1report_1owner(
    JNIEnv *, jobject, jstring);
  JNIEXPORT jstring JNICALL Java_gnu_io_RXTXVersion_nativeGetVersion(
    JNIEnv *, jclass);
}

/*
native_psmisc_report_owner

 * Class:     CommPortIdentifier
 * Method:    native_psmisc_report_owner
 * Signature: ()Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL
Java_gnu_io_CommPortIdentifier_native_1psmisc_1report_1owner(JNIEnv *env,
  jobject obj, jstring arg)
{
  return env->NewStringUTF("Unknown Application");
}

/*
nativeGetVersion

   accept:      none
   perform:     return the current version 
   return:      version
   exceptions:  none
   comments:    This is used to avoid mixing versions of the .jar and
		native library.
		First introduced in rxtx-1.5-9
                Moved from RXTXCommDriver to RXTXVersion in rxtx-2.1-7
 * Class:     RXTXVersion
 * Method:    nativeGetVersion
 * Signature: ()Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_gnu_io_RXTXVersion_nativeGetVersion(
  JNIEnv *env, jclass cls)
{
  return env->NewStringUTF("RXTX-2.2pre2");
}

/*
 registerKnownPorts

   accept:      the type of port
   perform:     register any ports of the desired type a priori known to this OS
   return:      JNI_TRUE if any such ports were registered otherwise JNI_FALSE
   exceptions:  none
   comments:
 * Class:     gnu_io_RXTXCommDriver
 * Method:    registerKnownPorts
 * Signature: (I)Z
 */
JNIEXPORT jboolean JNICALL Java_gnu_io_RXTXCommDriver_registerKnownPorts(JNIEnv *env, jobject jobj, jint portType)
{
	enum {PORT_TYPE_SERIAL = 1,
		PORT_TYPE_PARALLEL,
		PORT_TYPE_I2C,
		PORT_TYPE_RS485,
		PORT_TYPE_RAW};
	jboolean result = JNI_FALSE;

	switch(portType) {
		case PORT_TYPE_SERIAL:
      // We could check here what COMs are on system and register them.
      break;
		
    case PORT_TYPE_PARALLEL:
		case PORT_TYPE_I2C:
		case PORT_TYPE_RS485:
		case PORT_TYPE_RAW:      
      break;
		
    default:
      // Wrong port type - I'd raise exception, but there's no defined for Java
      IF_DEBUG
      (
        printj(env, L"!!! RXTXCommDriver.registerKnownPorts(%ld): Wrong port type\n", portType);
        //MessageBox(NULL, TEXT("RXTXCommDriver.registerKnownPorts(): Wrong port type"), TEXT("Error"), MB_OK);
      )
      break;
	}
	return result;
}

/*
 isPortPrefixValid

   accept:      a port prefix
   perform:     see if the port prefix matches a port that is valid on this OS.
   return:      JNI_TRUE if it exists otherwise JNI_FALSE
   exceptions:  none
   comments:
 * Class:     gnu_io_RXTXCommDriver
 * Method:    isPortPrefixValid
 * Signature: (Ljava/lang/String;)Z
 */
JNIEXPORT jboolean JNICALL Java_gnu_io_RXTXCommDriver_isPortPrefixValid(JNIEnv *env, jobject jobj, jstring dev)
{
	jboolean retVal;
  const char *szDev = env->GetStringUTFChars(dev, NULL);
  if( strncmp(szDev, "COM", 3) == 0 || strncmp(szDev, "LPT", 3) == 0)  // is first 3 chars OK?
    retVal = JNI_TRUE;
  else
    retVal = JNI_FALSE;
  
  env->ReleaseStringUTFChars(dev, szDev);

  return retVal;

}

/*
 testRead

   accept:      dev The device to be tested
   perform:     test if the device can be read from
   return:      JNI_TRUE if the device can be read from
   exceptions:  none
   comments:    From Wayne Roberts wroberts1@home.com
   		check tcget/setattr returns.
		support for non serial ports Trent
 * Class:     gnu_io_RXTXCommDriver
 * Method:    testRead
 * Signature: (Ljava/lang/String;I)Z
 */
JNIEXPORT jboolean JNICALL Java_gnu_io_RXTXCommDriver_testRead(JNIEnv *env, jobject jobj, jstring dev, jint type)
{
	jboolean retVal;

  if ( type == PORT_SERIAL )
  {
    const WCHAR *wszDev = (const WCHAR *)env->GetStringChars(dev, NULL); 
    HANDLE hPort = CreateFileW(wszDev,       // Pointer to the name of the port
                               GENERIC_READ | GENERIC_WRITE,
                                             // Access (read-write) mode
                               0,            // Share mode
                               NULL,         // Pointer to the security attribute
                               OPEN_EXISTING,// How to open the serial port
                               0,            // Port attributes
                               NULL);        // Handle to port with attribute
                                             // to copy
    // If it fails to open the port, return FALSE.
    if ( hPort == INVALID_HANDLE_VALUE ) 
    { // Could not open the port.
      IF_DEBUG
      (
        printj(env,
         L"!!! RXTXCommDriver.testRead(%s): cannot open port, error %ld\n",
         wszDev, GetLastError());
        //MessageBox(NULL, TEXT("RXTXCommDriver.testRead(): cannot open port"), wszDev /*TEXT("Error")*/, MB_OK | MB_SETFOREGROUND);
      )
      retVal = JNI_FALSE;
    }
    else
    { // Port open OK - let's close it and return TRUE
      (void)CloseHandle(hPort);
      IF_DEBUG
      (
        ;//printj(env, TEXT("--- RXTXCommDriver.testRead(%s, %ld): port open OK\n"), wszDev, type);
        //MessageBox(NULL, TEXT("RXTXCommDriver.testRead(): port open OK"), wszDev /*TEXT("Success")*/, MB_OK | MB_SETFOREGROUND);
      )
      retVal = JNI_TRUE;
    }
    env->ReleaseStringChars(dev, (const jchar*)wszDev);
  }
  else
    retVal = JNI_FALSE;

  return retVal;
}

/*
 getDeviceDirectory

   accept:      
   perform:     
   return:      the directory containing the device files
   exceptions:  
   comments:    we need it only for Unix
 * Class:     gnu_io_RXTXCommDriver
 * Method:    getDeviceDirectory
 * Signature: ()Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_gnu_io_RXTXCommDriver_getDeviceDirectory(JNIEnv *env, jobject jobj)
{
  return env->NewStringUTF("");
}
