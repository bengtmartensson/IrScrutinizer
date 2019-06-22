#!/bin/sh

# Installs IrScrutinizer on the local machine.

# This script assumes that the binary distribution has been unpacked in its
# final destination, for example /usr/local/share/irscrutinizer.
# It must reside within that directory.
# It should be run with the rights required to write
# at the desired places. I.e. root a priori not necessary.

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

mkdir -p ${PREFIX}/bin
mklink ${MYPROG_LOWER}
mklink irptransmogrifier

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

# Install desktop file
install -d ${PREFIX}/share/applications
if [ -f ${PREFIX}/share/applications/${MYPROG_LOWER}.desktop ] ; then
    rm -f ${PREFIX}/share/applications/${MYPROG_LOWER}.desktop
fi
sed -e "s|Exec=.*|Exec=/bin/sh \"${PREFIX}/bin/${MYPROG_LOWER}\"|" \
    -e "s|Icon=.*|Icon=${IRSCRUTINIZERHOME}/${MYPROG_LOWER}.png|" ${IRSCRUTINIZERHOME}/${MYPROG_LOWER}.desktop \
  > ${PREFIX}/share/applications/${MYPROG_LOWER}.desktop

# Install mime type file for girr
if [ -f ${PREFIX}/share/applications/girr.xml ] ; then
    rm -f ${PREFIX}/share/applications/girr.xml
fi
install --mode 444 ${IRSCRUTINIZERHOME}/girr.xml ${PREFIX}/share/applications

echo "Consider deleting old properties with the command "
echo "irscrutinizer --nuke-properties"
