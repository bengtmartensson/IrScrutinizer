#!/bin/sh

# Installs IrScrutinizer on the local machine.

# This script assumes that the binary distribution has been unpacked in its
# final destination, for example /usr/local/share/irscrutinizer.
# It must reside within that directory.
# It should be run with the rights required to write
# at the desired places. I.e. root a priori not necessary.

mklink()
{
    if [ $(readlink -f -- "${PREFIX}/bin/$1") != "${IRSCRUTINIZERHOME}/${MYPROG_LOWER}.sh" ] ; then
        if ln -sf ${IRSCRUTINIZERHOME}/${MYPROG_LOWER}.sh   ${PREFIX}/bin/${1} >/dev/null 2>&1 ; then
            echo "Creating link ${PREFIX}/bin/${1} succeded"
        else
            echo "Creating link ${PREFIX}/bin/${1} failed, redo with sudo?"
        fi
#    else
#        echo "Link ${PREFIX}/bin/$1 -> ${IRSCRUTINIZERHOME}/${MYPROG_LOWER}.sh already present and OK"
    fi
}

MYPROG=IrScrutinizer

IRSCRUTINIZERHOME="$(dirname -- "$(readlink -f -- "${0}")" )"

if [ $# = 1 ] ; then
    PREFIX=$1
else
    PREFIX=$(dirname "$(dirname "${IRSCRUTINIZERHOME}" )" )
fi

MYPROG_LOWER=$(echo ${MYPROG} | tr A-Z a-z)
DESKTOP_FILE=${PREFIX}/share/applications/${MYPROG_LOWER}.desktop
GIRR_XML=${PREFIX}/share/applications/girr.xml

mkdir -p ${PREFIX}/bin
mklink ${MYPROG_LOWER}
mklink irptransmogrifier
mklink harchardware
mklink AmxBeaconListenerPanel
mklink HexCalculator
mklink TimeFrequencyCalculator

# Install desktop file
install -d ${PREFIX}/share/applications
if [ -f ${PREFIX}/share/applications/${MYPROG_LOWER}.desktop ] ; then
    rm -f ${PREFIX}/share/applications/${MYPROG_LOWER}.desktop > /dev/null 2>&1
fi
if sed -e "s|Exec=.*|Exec=/bin/sh \"${PREFIX}/bin/${MYPROG_LOWER}\"|" \
    -e "s|Icon=.*|Icon=${IRSCRUTINIZERHOME}/${MYPROG_LOWER}.png|" ${IRSCRUTINIZERHOME}/${MYPROG_LOWER}.desktop \
  > ${DESKTOP_FILE} ; then
    echo "Creating ${DESKTOP_FILE} succeded."
else
    echo "Creating ${DESKTOP_FILE} failed, redo with sudo?"
fi

# Install mime type file for girr
if [ -f ${PREFIX}/share/applications/girr.xml ] ; then
    rm -f ${PREFIX}/share/applications/girr.xml >/dev/null 2>&1
fi

if install --mode 444 ${IRSCRUTINIZERHOME}/girr.xml ${GIRR_XML} >/dev/null 2>&1 ; then
    echo "Creating ${GIRR_XML} succeded."
else
    echo "Creating ${GIRR_XML} failed, redo with sudo?"
fi

echo "Consider deleting old properties with the command "
echo "    irscrutinizer --nuke-properties"
