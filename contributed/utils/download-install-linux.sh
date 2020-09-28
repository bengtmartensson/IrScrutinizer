#! /bin/sh

# Script for downloading and installing of IrScrutinizer on Linux and *ix systems
#
# Author: Bengt Martensson
# License: public domain

# Where the files go, modify if desired
APPHOME=/usr/local/share/irscrutinizer

# Should probably not change
URL_VERSION=http://harctoolbox.org/downloads/IrScrutinizer.version

TMP_DIR=/tmp/downloader$$

if [ $# -eq 0 ] ; then
    if [ ! -d ${TMP_DIR} ] ; then
        mkdir ${TMP_DIR}
    fi
    cd ${TMP_DIR}
    wget ${URL_VERSION}
    VERSION=$(sed -e 's/IrScrutinizer version //' IrScrutinizer.version)
    FILE=IrScrutinizer-${VERSION}-bin.zip
    wget https://github.com/bengtmartensson/IrScrutinizer/releases/download/Version-${VERSION}/${FILE}
    ZIP=${TMP_DIR}/${FILE}
fi

if [ -f "$1" ] ; then
    ZIP=$(readlink -f -- "$1")
fi

if [ ! -d "${APPHOME}" ] ; then
    mkdir "${APPHOME}"
fi

cd "${APPHOME}"
rm -rf *
unzip -q "${ZIP}"
sh ./setup-irscrutinizer.sh
