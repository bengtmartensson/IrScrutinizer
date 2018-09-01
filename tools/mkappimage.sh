#!/bin/sh
#
# Create a directory for AppImage, and call AppImageAssistant.
# Should be called from the IrScrutinizer directory.
#
# TODO: should check membership in groups dialout, lock, and lirc,
# as in irscrutinizer.sh

# Since the lock directory is sometimes different from /var/lock,
# we prefer the system's rxtx (if existing) over our own.
JAVA_LIBRARY_PATH=/usr/lib64/rxtx:/usr/lib64:/usr/lib/rxtx:/usr/lib

APPNAME=$1
VERSION=$2
APPIMAGEKITDIR=$3
MYPROG_LOWER=$(echo ${APPNAME} | tr A-Z a-z)

APPIMAGEDIR=target/AppImage
MYPROG_HOME=${APPIMAGEDIR}/usr/share/${MYPROG_LOWER}
USR_BIN=${APPIMAGEDIR}/usr/bin
WRAPPER=${USR_BIN}/${MYPROG_LOWER}
APPIMAGE=target/${APPNAME}-${VERSION}-x86_64.AppImage

rm -rf ${APPIMAGEDIR}

# Copy stuff to MYPROG_HOME
install -d ${MYPROG_HOME}
install --mode=444 target/${APPNAME}-jar-with-dependencies.jar ${MYPROG_HOME}
install --mode=444 target/maven-shared-archive-resources/*.ini ${MYPROG_HOME}
install --mode=444 target/protocols.ini ${MYPROG_HOME}
#install --mode=444 target/maven-shared-archive-resources/girr.xml ${MYPROG_HOME}
install -d ${MYPROG_HOME}/exportformats.d
install --mode=444 target/exportformats.d/* ${MYPROG_HOME}/exportformats.d
#install -d ${MYPROG_HOME}/contributed
#install --mode=444 target/contributed/* ${MYPROG_HOME}/contributed
install -d ${MYPROG_HOME}/doc
install --mode=444 target/doc/* ${MYPROG_HOME}/doc
install -d ${MYPROG_HOME}/schemas
install --mode=444 ../schemas/*.xsd ${MYPROG_HOME}/schemas
#install -d ${MYPROG_HOME}/Linux-i386
#install --mode=444 ../native/Linux-i386/* ${MYPROG_HOME}/Linux-i386
install -d ${MYPROG_HOME}/Linux-amd64
install --mode=444 ../native/Linux-amd64/* ${MYPROG_HOME}/Linux-amd64
install -d ${MYPROG_HOME}/contributed
install --mode=444 target/contributed/* ${MYPROG_HOME}/contributed
install -d ${MYPROG_HOME}/contributed/import
install --mode=666 target/contributed/import/*.sh ${MYPROG_HOME}/contributed/import
install --mode=444 target/contributed/import/*.xsl ${MYPROG_HOME}/contributed/import

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
echo "unset XDG_DATA_DIRS"                                                          >> ${WRAPPER}
echo "IRSCRUTINIZERHOME=\${APPDIR}/usr/share/${MYPROG_LOWER}"                       >> ${WRAPPER}
echo 'cd ${IRSCRUTINIZERHOME}'                                                      >> ${WRAPPER}
echo "java -Djava.library.path=${JAVA_LIBRARY_PATH} -jar \${IRSCRUTINIZERHOME}/${APPNAME}-jar-with-dependencies.jar \"\$@\"" >> ${WRAPPER}
chmod 555 ${WRAPPER}
install --mode=555 ${APPIMAGEKITDIR}/desktopintegration ${USR_BIN}/${MYPROG_LOWER}.wrapper

# Invoke the builder
rm -f ${APPIMAGE}
${APPIMAGEKITDIR}/AppImageAssistant ${APPIMAGEDIR} ${APPIMAGE}
