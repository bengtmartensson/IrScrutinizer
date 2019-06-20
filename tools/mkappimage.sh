#!/bin/sh
#
# Create a directory for AppImage, and call appimagetool.
# Should be called from the IrScrutinizer top directory.

if [ "`uname -m`" != "x86_64" ] ; then
    echo "Presently, AppImages can only be built on x86_64, quitting."
    exit 0
fi

APPNAME=$1
VERSION=$2
JAVA_TAR_GZ=$3

MYPROG_LOWER=$(echo ${APPNAME} | tr A-Z a-z)
APPIMAGE=target/${APPNAME}-${VERSION}-x86_64.AppImage
APPDIR=target/AppDir
MYPROG_HOME=${APPDIR}/usr/share/${MYPROG_LOWER}
USR=${APPDIR}/usr
USR_BIN=${USR}/bin
WRAPPER=${USR_BIN}/${MYPROG_LOWER}
APPRUN=tools/AppRun
JAVA_MODULES=java.base,java.datatransfer,java.desktop,java.logging,java.xml

rm -rf ${APPDIR}

# If possible, bundle a subset of Java
if [ "x${JAVA_TAR_GZ}y" != "xy" -a -f "${JAVA_TAR_GZ}" ] ; then
    #wget -c https://github.com/AdoptOpenJDK/openjdk11-binaries/releases/download/jdk-11.0.3%2B7_openj9-0.14.3/OpenJDK11U-jdk_x64_linux_openj9_11.0.3_7_openj9-0.14.3.tar.gz
    tar xf ${JAVA_TAR_GZ}
    JAVA_ROOT=`echo jdk*`
    ${JAVA_ROOT}/bin/jlink --no-header-files --no-man-pages --compress=2 --strip-debug --add-modules ${JAVA_MODULES} --output ${USR}
    rm -rf ${JAVA_ROOT}
    JAVA_PATH=\${APP_ROOT}/usr/bin/
    JAVA_QUICKSTART=-Xquickstart
fi

# Copy stuff to MYPROG_HOME
install -d ${MYPROG_HOME}
install --mode=444 target/${APPNAME}-jar-with-dependencies.jar ${MYPROG_HOME}
install --mode=444 target/maven-shared-archive-resources/*.xml ${MYPROG_HOME}
install --mode=444 target/protocols.ini ${MYPROG_HOME}
#install --mode=444 target/maven-shared-archive-resources/girr.xml ${MYPROG_HOME}
install -d ${MYPROG_HOME}/exportformats.d
install --mode=444 target/exportformats.d/* ${MYPROG_HOME}/exportformats.d
install -d ${MYPROG_HOME}/doc
install --mode=444 target/doc/* ${MYPROG_HOME}/doc
install -d ${MYPROG_HOME}/schemas
install --mode=444 schemas/*.xsd ${MYPROG_HOME}/schemas
install -d ${MYPROG_HOME}/Linux-amd64
install --mode=444 native/Linux-amd64/* ${MYPROG_HOME}/Linux-amd64
install -d ${MYPROG_HOME}/contributed
install --mode=444 target/contributed/* ${MYPROG_HOME}/contributed
install -d ${MYPROG_HOME}/contributed/import
install --mode=555 target/contributed/import/*.sh ${MYPROG_HOME}/contributed/import
install --mode=444 target/contributed/import/*.xsl ${MYPROG_HOME}/contributed/import

# Top level
install --mode=555 ${APPRUN} ${APPDIR}
install --mode=444 target/${APPNAME}.png ${APPDIR}/${MYPROG_LOWER}.png
sed -e "s|Exec=.*|Exec=${MYPROG_LOWER}|" \
    -e "s|Icon=.*|Icon=${MYPROG_LOWER}|" \
    -e "s|Name=.*|Name=${APPNAME}|" < target/${MYPROG_LOWER}.desktop > ${APPDIR}/${MYPROG_LOWER}.desktop
chmod 444 ${APPDIR}/${MYPROG_LOWER}.desktop
install -d ${USR}/share/metainfo
install --mode=444 src/main/resources/${MYPROG_LOWER}.appdata.xml ${USR}/share/metainfo/${MYPROG_LOWER}.appdata.xml

# Fill "usr/bin"
install -d ${USR_BIN}

# Create wrapper
cat > ${WRAPPER} <<EOF
#!/bin/sh

unset XDG_DATA_DIRS
IRSCRUTINIZERHOME=\${APP_ROOT}/usr/share/${MYPROG_LOWER}

checkgroup()
{
    if grep \$1 /etc/group > /dev/null ; then
	if ! groups | grep \$1 > /dev/null ; then
            MESSAGE=\${MESSAGE}\$1,
	fi
    fi
}

# Check that the use is a member of some groups ...
checkgroup dialout
checkgroup lock
checkgroup lirc

# ... if not, barf, and potentially bail out.
if [ "x\${MESSAGE}" != "x" ] ; then
     # Remove last , of $MESSAGE
    MESSAGE=\$(echo \${MESSAGE} | sed -e "s/,\$//")
    MESSAGEPRE="You are not a member of the group(s) "
    MESSAGETAIL=", so you will probably not have access to some devices.\nYou probably want to correct this. Otherwise, functionality will be limited.\n\nDepending on your operating system, the command for fixing this may be \"sudo usermod -aG \${MESSAGE} \${USER}\".\n\nProceed anyhow?"
    if ! "${JAVA_PATH}java" -classpath "\${IRSCRUTINIZERHOME}/IrScrutinizer-jar-with-dependencies.jar" \
           org.harctoolbox.guicomponents.StandalonePopupAnnoyer "\${MESSAGEPRE}\${MESSAGE}\${MESSAGETAIL}" "\$@" ; then
        exit 1
    fi
fi

cd \${IRSCRUTINIZERHOME}

# Since the lock directory is sometimes different from /var/lock,
# we prefer the system's rxtx (if existing) over our own,
# which is why we put our own last in the path.
"${JAVA_PATH}java" -Djava.library.path=/usr/lib64/rxtx:/usr/lib64:/usr/lib/rxtx:/usr/lib:\${IRSCRUTINIZERHOME}/Linux-amd64 ${JAVA_QUICKSTART} -jar \${IRSCRUTINIZERHOME}/${APPNAME}-jar-with-dependencies.jar "\$@"
EOF

chmod 555 ${WRAPPER}

# Invoke the builder
#wget -c "https://github.com/AppImage/AppImageKit/releases/download/12/appimagetool-x86_64.AppImage"
#chmod +x ./appimagetool-x86_64.AppImage
tools/appimagetool-x86_64.AppImage -g ${APPDIR} && ls -lh ${APPNAME}*.AppImage*
mv ${APPNAME}*.AppImage* target/
