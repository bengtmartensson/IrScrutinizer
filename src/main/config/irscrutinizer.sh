#!/bin/sh

# This wrapper is used to start both IrScrutinizer and IrpMaster,
# depending on what name it is called.

# Intended for Unix-like systems (like Linux and MacOsX).
# May need to be locally adapted.

# When changing this file, or updating the programs, it may be a good idea to
# delete the property file, normally ~/.config/IrScrutinizer/properties.xml.

# Set to the preferred Java VM, with or without directory.
#JAVA=/opt/jdk1.7.0_65/bin/java
JAVA=java

# Where the programs are installed, adjust if required
#IRSCRUTINIZERHOME=/usr/local/irscrutinizer
#IRSCRUTINIZERHOME="$( dirname "${BASH_SOURCE[0]}" )"
export IRSCRUTINIZERHOME="$(dirname -- "$(readlink -f -- "${0}")" )"

if [ `basename "$0"` = "irptransmogrifier" ] ; then
    exec "${JAVA}" \
         -cp "${IRSCRUTINIZERHOME}/IrScrutinizer-jar-with-dependencies.jar" \
         org.harctoolbox.irp.IrpTransmogrifier "$@"
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
# Fedora: dnf install rxtx
# Ubunto >= 16: apt-get install librxtx-java
if [ -f /usr/lib64/rxtx/librxtxSerial.so ] ; then
    LOAD_RXTX_PATH=-Djava.library.path=/usr/lib64/rxtx
fi
if [ -f /usr/lib/rxtx/librxtxSerial.so ] ; then
    LOAD_RXTX_PATH=-Djava.library.path=/usr/lib/rxtx
fi
#LIBRARY_PATH=${RXTXLIB_PATH}${IRSCRUTINIZERHOME}/`uname -s`-${ARCH}
#LIBRARY_PATH=/usr/lib64/rxtx

# Use if you need /dev/ttyACM* (IrToy, many Arduino types) and your rxtx does not support it
#RXTX_SERIAL_PORTS=-Dgnu.io.rxtx.SerialPorts=/dev/ttyS0:/dev/ttyUSB0:/dev/ttyUSB1:/dev/ttyACM0:/dev/ttyACM1

if grep dialout /etc/group > /dev/null ; then
    if ! groups | grep dialout > /dev/null ; then
        needs_dialout=t
        MESSAGE="dialout"
    fi
fi

if grep lock /etc/group > /dev/null ; then
    if ! groups | grep lock > /dev/null ; then
        needs_lock=t
        MESSAGE="lock"
    fi
fi

if [ "x$needs_dialout" != "x" -a "x$needs_lock" != "x" ] ; then
    MESSAGE="dialout,lock"
fi

MESSAGEPRE="You are not a member of the group(s) "
MESSAGETAIL=", so you will probably not have access to the USB serial devices.\nYou probably want to correct this. Otherwise, functionality will be limited.\n\nProceed anyhow?"

if [ "x$MESSAGE" != "x" ] ; then
    if ! "${JAVA}" ${LOAD_RXTX_PATH} ${RXTX_SERIAL_PORTS} -classpath "${IRSCRUTINIZERHOME}/IrScrutinizer-jar-with-dependencies.jar" \
           org.harctoolbox.guicomponents.StandalonePopupAnnoyer "${MESSAGEPRE}${MESSAGE}${MESSAGETAIL}" "$@" ; then
        exit 1
    fi
fi

exec "${JAVA}" ${LOAD_RXTX_PATH} ${RXTX_SERIAL_PORTS} -jar "${IRSCRUTINIZERHOME}/IrScrutinizer-jar-with-dependencies.jar" ${IRSCRUTINIZER} "$@"
