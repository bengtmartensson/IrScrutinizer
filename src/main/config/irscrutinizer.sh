#!/bin/sh

# This wrapper is used to start IrScrutinizer.

# Intended for Unix-like systems (like Linux and MacOsX).
# May need to be locally adapted.

# When changing this file, or updating the programs, it may be a good idea to
# delete the property file, normally ~/.config/IrScrutinizer/properties.xml.

# Set to the preferred Java VM, with or without directory.
#JAVA=/opt/jdk1.7.0_65/bin/java
JAVA=${JAVA:-java}

# Uncomment and/or modify for Hight DPI usage
#JVM_ARGS=-Dsun.java2d.uiScale=2

# Where the programs are installed, adjust if required
#IRSCRUTINIZERHOME=/usr/local/irscrutinizer
#IRSCRUTINIZERHOME="$( dirname "${BASH_SOURCE[0]}" )"
export IRSCRUTINIZERHOME="$(dirname -- "$(readlink -f -- "${0}")" )"
FATJAT=${IRSCRUTINIZERHOME}/IrScrutinizer-jar-with-dependencies.jar

checkgroup()
{
    if grep $1 /etc/group > /dev/null ; then
	if ! groups | grep $1 > /dev/null ; then
            MESSAGE=${MESSAGE}$1,
	fi
    fi
}

# If called using the name irptransmogrifier, invoke that "program".
# Recall: exec does not return.
if [ $(basename "$0" ) = "irptransmogrifier" ] ; then
    exec "${JAVA}" -classpath "${FATJAT}" org.harctoolbox.irp.IrpTransmogrifier "$@"
fi

# If called using a name of one of the tools, invoke that "program".
if [ $(basename "$0" ) != "irscrutinizer" -a $(basename "$0" ) != "harchardware" ] ; then
    exec "${JAVA}" -classpath "${FATJAT}" org.harctoolbox.guicomponents.$(basename "$0") "$@"
fi

# Path to DecodeIR and RXTX
# If the code below does not work, just set LIBRARY_PATH to the directory
# containing the shared lib to use, like in the commented-out example lines.
#if [ `uname -m` = "armv6l" ] ; then
#    ARCH=arml
#elif [ `uname -m` = "x86_64" ] ; then
#    ARCH=amd64
#else
#    ARCH=i386
#fi

# Use a system supplied librxtxSerial.so if present.
# Fedora: dnf install rxtx (presently broken: https://bugzilla.redhat.com/show_bug.cgi?id=1645856 )
# Ubunto >= 16: apt-get install librxtx-java
#if [ -f /usr/lib64/rxtx/librxtxSerial.so ] ; then
#    LOAD_RXTX_PATH=-Djava.library.path=/usr/local/lib:/usr/lib64/rxtx
#fi
#if [ -f /usr/lib/rxtx/librxtxSerial.so ] ; then
#    LOAD_RXTX_PATH=-Djava.library.path=/usr/local/lib:/usr/lib/rxtx
#fi
#LIBRARY_PATH=${RXTXLIB_PATH}${IRSCRUTINIZERHOME}/`uname -s`-${ARCH}
#LIBRARY_PATH=/usr/lib64/rxtx

# Use if you need /dev/ttyACM* (IrToy, many Arduino types) and your rxtx does not support it
#RXTX_SERIAL_PORTS=-Dgnu.io.rxtx.SerialPorts=/dev/ttyS0:/dev/ttyUSB0:/dev/ttyUSB1:/dev/ttyACM0:/dev/ttyACM1

# Check that the use is a member of some groups ...
checkgroup dialout
checkgroup lock
checkgroup lirc

# ... if not, barf, and potentially bail out.
if [ "x${MESSAGE}" != "x" ] ; then
     # Remove last , of $MESSAGE
    MESSAGE=$(echo ${MESSAGE} | sed -e "s/,$//")
    MESSAGEPRE="You are not a member of the group(s) "
    MESSAGETAIL=", so you will probably not have access to some devices.\nYou probably want to correct this. Otherwise, functionality will be limited.\n\nDepending on your operating system, the command for fixing this may be \"sudo usermod -aG $MESSAGE $USER\".\n\nProceed anyhow?"
    if ! "${JAVA}" ${JVM_ARGS} -classpath "${FATJAT}" \
        org.harctoolbox.guicomponents.StandalonePopupAnnoyer "${MESSAGEPRE}${MESSAGE}${MESSAGETAIL}" "$@" ; then
        exit 1
    fi
fi

if [ $(basename "$0" ) = "harchardware" ] ; then
    exec "${JAVA}" -classpath "${FATJAT}" org.harctoolbox.harchardware.Main --apphome "${IRSCRUTINIZERHOME}" "$@"
fi

#exec "${JAVA}" ${LOAD_RXTX_PATH} ${RXTX_SERIAL_PORTS} -jar "${FATJAT}" --apphome "${IRSCRUTINIZERHOME}" "$@"
exec "${JAVA}" ${JVM_ARGS} -jar "${FATJAT}" --apphome "${IRSCRUTINIZERHOME}" "$@"
