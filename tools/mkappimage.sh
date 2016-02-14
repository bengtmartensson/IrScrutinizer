#!/bin/sh
#
# Create a directory for AppImage, and call AppImageAssistant.
# Should be called from the IrScrutinizer directory.
#

APPNAME=$1
VERSION=$2
MYPROG_LOWER=$(echo ${APPNAME} | tr A-Z a-z)

APPIMAGEDIR=target/AppImage
MYPROG_HOME=${APPIMAGEDIR}/usr/share/${MYPROG_LOWER}
USR_BIN=${APPIMAGEDIR}/usr/bin
WRAPPER=${USR_BIN}/${MYPROG_LOWER}
APPIMAGEKITDIR=../../AppImageKit
APPIMAGE=target/${APPNAME}-${VERSION}-x86_32+64.AppImage

rm -rf ${APPIMAGEDIR}

# Copy stuff to MYPROG_HOME
install -d ${MYPROG_HOME}
install --mode=444 target/${APPNAME}-jar-with-dependencies.jar ${MYPROG_HOME}

install --mode=444 target/IrpProtocols.ini target/protocols.ini ${MYPROG_HOME}
install --mode=444 target/exportformats.xml ${MYPROG_HOME}
install -d ${MYPROG_HOME}/doc
install --mode=444 target/doc/* ${MYPROG_HOME}/doc
install -d ${MYPROG_HOME}/schemas
install --mode=444 ../schemas/*.xsd ${MYPROG_HOME}/schemas
install -d ${MYPROG_HOME}/Linux-i386 ${MYPROG_HOME}/Linux-amd64
install --mode=444 ../native/Linux-i386/* ${MYPROG_HOME}/Linux-i386
install --mode=444 ../native/Linux-amd64/* ${MYPROG_HOME}/Linux-amd64

# Top level
install --mode=555 ${APPIMAGEKITDIR}/AppRun ${APPIMAGEDIR}
install --mode=444 target/${APPNAME}.png ${APPIMAGEDIR}/${MYPROG_LOWER}.png
sed -e "s|Exec=.*|Exec=${MYPROG_LOWER}.wrapper|" \
    -e "s|Icon=.*|Icon=${MYPROG_LOWER}|" \
    -e "s|Name=.*|Name=${APPNAME}|" < target/${MYPROG_LOWER}.desktop > ${APPIMAGEDIR}/${MYPROG_LOWER}.desktop
chmod 444 ${APPIMAGEDIR}/${MYPROG_LOWER}.desktop

# Fill "usr/bin"
install -d ${USR_BIN}
echo "#!/bin/sh"                                                                    >  ${WRAPPER}
echo "IRSCRUTINIZERHOME=\$(readlink -f \$(dirname \"\${0}\")/../share/${MYPROG_LOWER})" >> ${WRAPPER}
echo 'cd ${IRSCRUTINIZERHOME}'                                                      >> ${WRAPPER}
echo "java -jar \${IRSCRUTINIZERHOME}/${APPNAME}-jar-with-dependencies.jar \"\$@\"" >> ${WRAPPER}
chmod 555 ${WRAPPER}
install --mode=555 ${APPIMAGEKITDIR}/desktopintegration ${USR_BIN}/${MYPROG_LOWER}.wrapper

# Invoke the builder
rm -f ${APPIMAGE}
${APPIMAGEKITDIR}/AppImageAssistant ${APPIMAGEDIR} ${APPIMAGE}
