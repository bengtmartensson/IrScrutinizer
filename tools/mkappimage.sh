#!/bin/sh
#
# Create a directory for AppImage, and call appimagetool.
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

APPDIR=target/AppDir
MYPROG_HOME=${APPDIR}/usr/share/${MYPROG_LOWER}
USR_BIN=${APPDIR}/usr/bin
WRAPPER=${USR_BIN}/${MYPROG_LOWER}

rm -rf ${APPDIR}

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
install --mode=555 ${APPIMAGEKITDIR}/AppRun ${APPDIR}
install --mode=444 target/${APPNAME}.png ${APPDIR}/${MYPROG_LOWER}.png
sed -e "s|Exec=.*|Exec=${MYPROG_LOWER}.wrapper|" \
    -e "s|Icon=.*|Icon=${MYPROG_LOWER}|" \
    -e "s|Name=.*|Name=${APPNAME}|" < target/${MYPROG_LOWER}.desktop > ${APPDIR}/${MYPROG_LOWER}.desktop
chmod 444 ${APPDIR}/${MYPROG_LOWER}.desktop

# Fill "usr/bin"
install -d ${USR_BIN}
echo "#!/bin/sh"                                                                    >  ${WRAPPER}
echo "unset XDG_DATA_DIRS"                                                          >> ${WRAPPER}
echo "IRSCRUTINIZERHOME=\${APPDIR}/usr/share/${MYPROG_LOWER}"                       >> ${WRAPPER}
echo 'cd ${IRSCRUTINIZERHOME}'                                                      >> ${WRAPPER}
echo "java -Djava.library.path=${JAVA_LIBRARY_PATH} -Xquickstart -jar \${IRSCRUTINIZERHOME}/${APPNAME}-jar-with-dependencies.jar \"\$@\"" >> ${WRAPPER}
chmod 555 ${WRAPPER}
install --mode=555 ${APPIMAGEKITDIR}/desktopintegration ${USR_BIN}/${MYPROG_LOWER}.wrapper

# Bundle subset of Java
wget -c https://github.com/AdoptOpenJDK/openjdk11-binaries/releases/download/jdk-11.0.3%2B7_openj9-0.14.3/OpenJDK11U-jdk_x64_linux_openj9_11.0.3_7_openj9-0.14.3.tar.gz
tar xf OpenJDK11U-jdk_x64_linux_openj9_11.0.3_7_openj9-0.14.3.tar.gz
./jdk-11.0.3+7/bin/jlink --no-header-files --no-man-pages --compress=2 --strip-debug --add-modules java.base,java.datatransfer,java.desktop,java.logging,java.xml --output usr
cp -Rf usr ${APPDIR}/

# Invoke the builder
wget -c "https://github.com/AppImage/AppImageKit/releases/download/12/appimagetool-x86_64.AppImage"
chmod +x ./appimagetool-x86_64.AppImage
./appimagetool-x86_64.AppImage ${APPDIR}
