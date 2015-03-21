#!/bin/sh

# This wrapper is used to start both IrScrutinizer, IrMaster, and IrpMaster,
# depending on what name it is called.

# Intended for Unix-like systems (like Linux and MacOsX).
# May need to be locally adapted.

# When changing this file, or updating the programs, it may be a good idea to
# delete ~/.IrMaster.properties.xml and ~/.config/IrScrutinizer/properties.xml.

# Set to the preferred Java VM, with or without directory.
#JAVA=/opt/jdk1.7.0_65/bin/java
JAVA=java

# Where the programs are installed
IRSCRUTINIZERHOME=/usr/local/irscrutinizer

# Path to DecodeIR and RXTX
# If the code below does not work, just set LIBRARY_PATH to the directory
# containing the shared lib to use, like in the commented-out example lines.
if [ `uname -m` = "armv6l" ] ; then
    ARCH=arml
elif [ `uname -m` = "x86_64" ] ; then
    ARCH=amd64
else
    ARCH=i386
fi
# Use a system supplied librxtxSerial.so if present (e.g. Fedora, "yum install rxtx")
if [ -f /usr/lib64/rxtx/librxtxSerial.so ] ; then
    RXTXLIB_PATH=/usr/lib64/rxtx:
fi
if [ -f /usr/lib/rxtx/librxtxSerial.so ] ; then
    RXTXLIB_PATH=/usr/lib/rxtx:
fi
LIBRARY_PATH=${RXTXLIB_PATH}${IRSCRUTINIZERHOME}/`uname -s`-${ARCH}
#LIBRARY_PATH=/usr/lib64/rxtx

# Use if you need /dev/ttyACM* (IrToy, many Arduino types) and your rxtx does not support it
#RXTX_SERIAL_PORTS=-Dgnu.io.rxtx.SerialPorts=/dev/ttyS0:/dev/ttyUSB0:/dev/ttyUSB1:/dev/ttyACM0:/dev/ttyACM1

if [ `basename "$0"` = "irpmaster" ] ; then
    # Run IrpMaster from the current directory
    exec "${JAVA}" -Djava.library.path="${LIBRARY_PATH}" -classpath "${IRSCRUTINIZERHOME}/IrScrutinizer.jar" org.harctoolbox.irscrutinizer.IrpMaster --config "${IRSCRUTINIZERHOME}/IrpProtocols.ini" "$@"
elif [ `basename "$0"` = "irscrutinizer" -o `basename "$0"` = "irscrutinizer.sh" ] ; then
    # cd to the installation director to get the relative path names in
    # the default properties to fit, can be omitted if making file names
    # in the properties absolute.

    cd "${IRSCRUTINIZERHOME}"
    exec "${JAVA}" ${RXTX_SERIAL_PORTS} -Djava.library.path="${LIBRARY_PATH}" -jar "${IRSCRUTINIZERHOME}/IrScrutinizer.jar" "$@"

elif [ `basename "$0"` = "irmaster" ] ; then
    # cd to the installation director to get the relative path names in
    # the default properties to fit, can be omitted if making file names
    # in the properties absolute.

    cd "${IRSCRUTINIZERHOME}"
    exec "${JAVA}" -Djava.library.path="${LIBRARY_PATH}" -jar "${IRSCRUTINIZERHOME}/IrMaster.jar" -p "${HOME}/.IrMaster.properties.xml" "$@"

else
    echo "Error, please investigate ${IRSCRUTINIZERHOME}/irscrutinizer.sh and the links to it."
    exit 1
fi
