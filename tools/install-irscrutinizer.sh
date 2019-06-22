#!/bin/sh

# Installs IrScrutinizer on the local machine.
# Should reside in the tools directory.
# Not to be distrubuted in user-setups.

# This script should be run with the rights required to write
# at the desired places. I.e. root a priori not necessary.
# It should be run from the top level directory.

MYPROG=IrScrutinizer
MYPROG_LOWER=$(echo ${MYPROG} | tr A-Z a-z)

if [ $# = 1 ] ; then
    INSTALLDIR=$1
else
    INSTALLDIR=/usr/local/share/${MYPROG_LOWER}
fi


DIR="$(dirname -- "$(readlink -f -- "${0}")" )"

mkdir -p ${INSTALLDIR}

cd ${INSTALLDIR}
unzip -o ${DIR}/../target/${MYPROG}-*-bin.zip

# Call the just unpacked setup script
./setup-irscrutinizer.sh
