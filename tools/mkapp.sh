#!/bin/sh
#
# Create a MacOS app in the form of a zip file
#
#

APPNAME=$1
VERSION=$2

TOPDIR=`pwd`
TARGETDIR=${TOPDIR}/target
WORKDIR=${TARGETDIR}
APPDIR=${WORKDIR}/${APPNAME}-${VERSION}
REPODIR=${APPDIR}/${APPNAME}.app/Contents/Resources/Java/repo

rm -rf ${APPDIR}/${APPNAME}.app
mkdir -p ${REPODIR}
mkdir -p ${APPDIR}/${APPNAME}.app/Contents/MacOS
cp ${TARGETDIR}/doc/README* ${APPDIR}
cp ${TARGETDIR}/doc/LICENSE* ${APPDIR}
cp ${TARGETDIR}/doc/${APPNAME}* ${APPDIR}

cp ${TARGETDIR}/Info.plist ${APPDIR}/${APPNAME}.app/Contents
cp ${TARGETDIR}/Launcher ${APPDIR}/${APPNAME}.app/Contents/MacOS
chmod +x ${APPDIR}/${APPNAME}.app/Contents/MacOS/Launcher
cp ${TOPDIR}/src/main/resources/${APPNAME}.icns ${APPDIR}/${APPNAME}.app/Contents/Resources

( cd ${APPDIR}/${APPNAME}.app/Contents/Resources/Java/repo; \
  unzip -q ${TARGETDIR}/${APPNAME}-bin.zip )

# These may have been copied already by the unzip. But who cares? :-).
cp -r "${TOPDIR}/native/Mac OS X-x86_64" ${REPODIR}
cp -r "${TOPDIR}/native/Mac OS X-i386"   ${REPODIR}

# Delete some files that are not relevant in the MacOs environment
rm ${REPODIR}/doc/INSTALL-binary* ${REPODIR}/INSTALL-binary*
rm ${REPODIR}/irscrutinizer.bat ${REPODIR}/irscrutinizer.desktop
rm ${REPODIR}/*_install.txt
rm -rf ${REPODIR}/Linux* ${REPODIR}/Windows*

(cd ${WORKDIR}; zip -q -r ${TARGETDIR}/${APPNAME}-${VERSION}-app.zip ${APPNAME}-${VERSION})
