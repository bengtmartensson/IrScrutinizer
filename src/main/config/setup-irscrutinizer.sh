#!/bin/sh

# Installs IrScrutinizer on the local machine.

# This script assumes that the binary distribution has been unpacked in its
# final destination, for example /usr/local/share/irscrutinizer.
# It must reside within that directory.
# It should be run with the rights required to write
# at the desired places. I.e. root a priori not necessary.

# Just for this file, do not affect installed wrappers.
JAVA=java

mklink()
{
    if ln -sf ${IRSCRUTINIZERHOME}/${MYPROG_LOWER}.sh   ${PREFIX}/bin/${1} >/dev/null 2>&1 ; then
        echo "Creating link ${PREFIX}/bin/${1} succeded"
    else
        echo "Creating link ${PREFIX}/bin/${1} failed, redo with sudo?"
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

# Is it a good idea to install documentation and schemas as the commented-out
# code once did? If anybody thinks so, please let me know!

# Install documentation
#install -d ${PREFIX}/share/doc/${MYPROG_LOWER}
#install --mode=444 doc/* ${PREFIX}/share/doc/${MYPROG_LOWER}
#ln -sf ../doc/${MYPROG_LOWER} ${IRSCRUTINIZERHOME}/doc

# Install schemas
#install -d ${PREFIX}/share/xml/harctoolbox
#install --mode=444 ../schemas/*.xsd ${PREFIX}/share/xml/harctoolbox
#ln -sf ../xml/harctoolbox ${IRSCRUTINIZERHOME}/schemas

## Find best librxtxSerial.so, and make librxtxSerial.so a link to it.
## SUDO_USER is, if using sudo to run, the name of the invoking user (real uid).
#if [ "$(arch)" = "x86_64" ] ; then
#    cd ${IRSCRUTINIZERHOME}/Linux-amd64
#    for solib in librxtxSerial-var-lock.so librxtxSerial-var-lock-lockdev.so ; do
#        if [ x${SUDO_USER}y != "xy" ] ; then
#            su -c "${JAVA} -cp ${IRSCRUTINIZERHOME}/IrScrutinizer-jar-with-dependencies.jar org.harctoolbox.harchardware.comm.TestRxtx ${solib} 2>&1 | grep 'No permission to create lock file.' >/dev/null"  ${SUDO_USER}
#        else
#            ${JAVA} -cp ${IRSCRUTINIZERHOME}/IrScrutinizer-jar-with-dependencies.jar org.harctoolbox.harchardware.comm.TestRxtx ${solib} 2>&1 | grep 'No permission to create lock file.' >/dev/null
#        fi
#	if [ $? -ne 0 ] ; then
#	    ln -sf ${solib} librxtxSerial.so
#	    echo Made librxtxSerial.so link to ${solib}
#	    break
#	fi
#    done
#fi

# Install desktop file
install -d ${PREFIX}/share/applications
if [ -f ${PREFIX}/share/applications/${MYPROG_LOWER}.desktop ] ; then
    rm -f ${PREFIX}/share/applications/${MYPROG_LOWER}.desktop
fi
sed -e "s|Exec=.*|Exec=/bin/sh \"${PREFIX}/bin/${MYPROG_LOWER}\"|" \
    -e "s|Icon=.*|Icon=${IRSCRUTINIZERHOME}/${MYPROG_LOWER}.png|" ${IRSCRUTINIZERHOME}/${MYPROG_LOWER}.desktop \
  > ${DESKTOP_FILE} && echo "Creating ${DESKTOP_FILE} succeded."

# Install mime type file for girr
if [ -f ${PREFIX}/share/applications/girr.xml ] ; then
    rm -f ${PREFIX}/share/applications/girr.xml
fi

install --mode 444 ${IRSCRUTINIZERHOME}/girr.xml ${GIRR_XML} && echo "Creating ${GIRR_XML} succeded."

echo "Consider deleting old properties with the command "
echo "    irscrutinizer --nuke-properties"
