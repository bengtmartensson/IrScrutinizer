#!/bin/sh
#
# Create a MacOS app in the form of a zip file
#
#

APPNAME=$1
VERSION=$2

#APPNAME=IrScrutinizer
#VERSION=$(
#xsltproc - ${APPNAME}/pom.xml <<EOF
#<xsl:stylesheet xmlns:pom="http://maven.apache.org/POM/4.0.0"
#                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
#version="1.0">
#   <xsl:output method="text"/>
#   <xsl:template match="/">
#       <xsl:apply-templates select="/pom:project/pom:version/text()"/>
#   </xsl:template>
#</xsl:stylesheet>
#EOF
#)


TOPDIR=`pwd`
TARGETDIR=${TOPDIR}/${APPNAME}/target
WORKDIR=${TARGETDIR}
APPDIR=${WORKDIR}/${APPNAME}-${VERSION}
REPODIR=${APPDIR}/${APPNAME}.app/Contents/Resources/Java/repo

mkdir -p ${REPODIR}
mkdir -p ${APPDIR}/${APPNAME}.app/Contents/MacOS
cp ${TARGETDIR}/doc/README* ${APPDIR}
cp ${TARGETDIR}/doc/LICENSE* ${APPDIR}
cp ${TARGETDIR}/doc/${APPNAME}* ${APPDIR}

cp ${TARGETDIR}/Info.plist ${APPDIR}/${APPNAME}.app/Contents
cp ${TARGETDIR}/Launcher ${APPDIR}/${APPNAME}.app/Contents/MacOS
chmod +x ${APPDIR}/${APPNAME}.app/Contents/MacOS/Launcher
cp ${TOPDIR}/${APPNAME}/${APPNAME}.icns ${APPDIR}/${APPNAME}.app/Contents/Resources
cp -r "${TOPDIR}/native/Mac OS X-x86_64" ${REPODIR}
cp -r "${TOPDIR}/native/Mac OS X-i386"   ${REPODIR}

( cd ${APPDIR}/${APPNAME}.app/Contents/Resources/Java/repo; \
  unzip -q ${TARGETDIR}/${APPNAME}-bin.zip )

# Delete some files that are not relevant in the MacOs environment
rm ${REPODIR}/doc/INSTALL-binary* ${REPODIR}/INSTALL-binary*
rm ${REPODIR}/irscrutinizer.bat ${REPODIR}/irscrutinizer.desktop
rm ${REPODIR}/*_install.txt

(cd ${WORKDIR}; zip -q -r ${TARGETDIR}/${APPNAME}-${VERSION}-app.zip ${APPNAME}-${VERSION})
