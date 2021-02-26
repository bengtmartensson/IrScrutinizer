#! /bin/bash

# Script for downloading and installing of IrScrutinizer on Linux and *ix systems
#
# Author: Bengt Martensson
# License: public domain

if [ $(id -u) -eq 0 ] ; then
    PREFIX=/usr/local
else
    PREFIX=${HOME}
fi

# Where the files are installed, modify if desired.
# Can be overridden from the command line.
APPHOME=${PREFIX}/share/irscrutinizer

# Where the executable links go-
# Can be overridden from the command line.
LINKDIR=${PREFIX}/bin

# Where the desktop files go, change only if you know what you are doing
if [ $(id -u) -eq 0 ] ; then
    DESKTOPDIR=${PREFIX}/share/applications
else
    DESKTOPDIR=${HOME}/.local/share/applications
fi

## Command to invoke the Java JVM. Can be an absolute or relative file name,
## or a command sought in the PATH.
## Can be overridden from the command line.
#JAVA=java
#
## Scaling factor for the GUI. Does not work with all JVMs;
## some JVMs accept only integer arguments.
## Can be overridden from the command line.
#SCALE_FACTOR=1

# Should probably not change
URL_VERSION=http://harctoolbox.org/downloads/IrScrutinizer.version

TMP_DIR=/tmp/downloader$$

usage()
{
    echo "Usage: $0 [OPTIONS] [zip-file]"
    echo ""
    echo "Installs IrScrutinizer and its tools in the system, compatible with the"
    echo "Freedesktop standard (https://www.freedesktop.org)."
    echo "If a zip-file is not given as argument, the official latest version is downloaded."
    echo ""
    echo "Options:"
    echo "    -?, --help                        Display this help and exit."
#    echo "    -j, --java command-for-java       Command to invoke Java, default \"${JAVA}\"."
#    echo "    -s, --scale scale-factor          scale factor for the GUI, default ${SCALE_FACTOR}. Not supported by all JVMs."
    echo "    -h, --home install-dir       Directory in which to install, default ${APPHOME}."
    echo "    -l, --link directory-for-links    Directory in which to create start links, default ${LINKDIR}."
    echo ""
    echo "This script should be run with the privileges necessary for writing"
    echo "to the locations selected."
}

while [ -n "$1" ] ; do
    case $1 in
        -\? | --help )          usage
                                exit 0
                                ;;
#        -j | --java )           shift
#                                JAVA="$1"
#                                ;;
#        -s | --scale )          shift
#                                SCALE_FACTOR="$1"
#                                ;;
        -l | --linkdir )        shift
                                LINKDIR="$1"
                                ;;
        -h | --home | --rmhome ) shift
                                APPHOME="$1"
                                ;;
        * )                     ZIP="$1"
                                ;;
    esac
    shift
done

if [ -z ${ZIP} ] ; then
    if [ ! -d ${TMP_DIR} ] ; then
        mkdir -p ${TMP_DIR} || exit 1
    fi
    cd ${TMP_DIR} || exit 1
    wget ${URL_VERSION}
    VERSION=$(sed -e 's/IrScrutinizer version //' IrScrutinizer.version)
    FILE=IrScrutinizer-${VERSION}-bin.zip
    wget https://github.com/bengtmartensson/IrScrutinizer/releases/download/Version-${VERSION}/${FILE}
    ZIP=${TMP_DIR}/${FILE}
fi

if [ ! -d "${APPHOME}" ] ; then
    mkdir -p "${APPHOME}" || exit 1
fi

cd "${APPHOME}" || exit 1
rm -rf * || exit 1
unzip -q "${ZIP}" || exit 1
sh ./setup-irscrutinizer.sh || exit 1
