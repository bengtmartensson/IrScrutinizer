#!/bin/sh
#
# Create a directory for AppImage, populate it, and call appimagetool.
# Should be called from the IrScrutinizer top directory.

if [ $# -lt 2 ] ; then
    echo -e "Usage:\n$0 [-v] <appname> <version> [<JDK-file>]" >&2
    exit 1
fi

if [ "`uname -m`" != "x86_64" ] ; then
    echo "Presently, AppImages can only be built on x86_64, quitting." >&2
    exit 1
fi

if [ "$1" = "-v" ] ; then
    echo Command line: $0 "$@"
    VERBOSE="-v"
    shift
fi

APPNAME=$1
export VERSION=$2
JAVA_TAR_GZ=$3

MYPROG_LOWER=$(echo ${APPNAME} | tr A-Z a-z)
APPIMAGE=target/${APPNAME}-${VERSION}-x86_64.AppImage
APPDIR=target/AppDir
MYPROG_HOME=${APPDIR}/usr/share/${MYPROG_LOWER}
USR=${APPDIR}/usr
USR_BIN=${USR}/bin
WRAPPER=${USR_BIN}/${MYPROG_LOWER}
APPRUN=tools/AppRun
FATJAR=${APPNAME}-${VERSION}-jar-with-dependencies.jar
JAVA_MODULES=java.base,java.datatransfer,java.desktop,java.logging,java.xml,jdk.crypto.ec
#,jdk.dynalink,jdk.editpad,jdk.hotspot.agent,jdk.httpserver,jdk.internal.ed,jdk.internal.jvmstat,jdk.internal.le,jdk.internal.opt,jdk.internal.vm.ci,jdk.internal.vm.compiler,jdk.internal.vm.compiler.management,jdk.jartool,jdk.javadoc,jdk.jcmd,jdk.jconsole,jdk.jdeps,jdk.jdi,jdk.jdwp.agent,jdk.jfr,jdk.jlink,jdk.jshell,jdk.jsobject,jdk.jstatd,jdk.localedata,jdk.management,jdk.management.agent,jdk.management.jfr,jdk.naming.dns,jdk.naming.rmi,jdk.net,jdk.pack,jdk.rmic,jdk.scripting.nashorn,jdk.scripting.nashorn.shell,jdk.sctp,jdk.security.auth,jdk.security.jgss,jdk.unsupported,jdk.unsupported.desktop,jdk.xml.dom,jdk.zipfs

rm -rf ${APPDIR}

# If possible, bundle a subset of Java
if [ "x${JAVA_TAR_GZ}y" != "xy" -a -f "${JAVA_TAR_GZ}" ] ; then
    tar xf ${JAVA_TAR_GZ}
    JAVA_ROOT=`echo jdk*`
    ${JAVA_ROOT}/bin/jlink --no-header-files --no-man-pages --strip-debug --add-modules ${JAVA_MODULES} --output ${USR}
    rm -rf ${JAVA_ROOT}
    JAVA_PATH=\${APP_ROOT}/usr/bin/
fi

# Copy stuff to MYPROG_HOME
install -d ${MYPROG_HOME}
install --mode=444 target/${FATJAR} ${MYPROG_HOME}
install --mode=444 target/maven-shared-archive-resources/*.xml ${MYPROG_HOME}
#install --mode=444 target/maven-shared-archive-resources/girr.xml ${MYPROG_HOME}
install -d ${MYPROG_HOME}/exportformats.d
install --mode=444 target/exportformats.d/* ${MYPROG_HOME}/exportformats.d
install -d ${MYPROG_HOME}/doc
install --mode=444 target/doc/* ${MYPROG_HOME}/doc
install -d ${MYPROG_HOME}/schemas
install --mode=444 target/*.xsd ${MYPROG_HOME}/schemas
install -d ${MYPROG_HOME}/Linux-amd64
install --mode=444 native/Linux-amd64/* ${MYPROG_HOME}/Linux-amd64
cp -a    target/contributed ${MYPROG_HOME}
chmod +x ${MYPROG_HOME}/contributed/*/*.sh

# Top level
install --mode=555 ${APPRUN} ${APPDIR}
install --mode=444 target/${APPNAME}.png ${APPDIR}/${MYPROG_LOWER}.png
sed -e "s|Exec=.*|Exec=${MYPROG_LOWER}|" \
    -e "s|Icon=.*|Icon=${MYPROG_LOWER}|" \
    -e "s|Name=.*|Name=${APPNAME}|" < target/${MYPROG_LOWER}.desktop > ${APPDIR}/${MYPROG_LOWER}.desktop
chmod 444 ${APPDIR}/${MYPROG_LOWER}.desktop
install -d ${USR}/share/metainfo
install --mode=444 src/main/config/${MYPROG_LOWER}.appdata.xml ${USR}/share/metainfo/${MYPROG_LOWER}.appdata.xml

# Fill "usr/bin"
install -d ${USR_BIN}

# Create wrapper
cat > ${WRAPPER} <<EOF
#!/bin/sh

unset XDG_DATA_DIRS
IRSCRUTINIZERHOME=\${APP_ROOT}/usr/share/${MYPROG_LOWER}

transmogrify()
{
    exec "${JAVA_PATH}java" -classpath "\${IRSCRUTINIZERHOME}/${FATJAR}" \
        ${JAVA_QUICKSTART} org.harctoolbox.irp.IrpTransmogrifier "\$@"
}

# If basename \$0 equals "irptransmogrifier" invoke that program instead.
if [ "\$(basename \$0)" = "irptransmogrifier" ] ; then
    transmogrify "\$@"
fi

# If \$1 equals "irptransmogrifier" invoke that program instead.
if [ "\$1" = "irptransmogrifier" ] ; then
    shift
    transmogrify "\$@"
fi

harchardware()
{
    exec "${JAVA_PATH}java" -classpath "\${IRSCRUTINIZERHOME}/${FATJAR}" \
        ${JAVA_QUICKSTART} org.harctoolbox.harchardware.Main "\$@"
}

# If basename \$0 equals "harchardware" invoke that program instead.
if [ "\$(basename \$0)" = "harchardware" ] ; then
    harchardware "\$@"
fi

# If \$1 equals "harchardware" invoke that program instead.
if [ "\$1" = "harchardware" ] ; then
    shift
    harchardware "\$@"
fi

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
    if ! "${JAVA_PATH}java" -classpath "\${IRSCRUTINIZERHOME}/${FATJAR}" \
           org.harctoolbox.guicomponents.StandalonePopupAnnoyer "\${MESSAGEPRE}\${MESSAGE}\${MESSAGETAIL}" "\$@" ; then
        exit 1
    fi
fi

cd \${IRSCRUTINIZERHOME}

exec "${JAVA_PATH}java" -Djava.library.path=/usr/local/lib:/usr/lib64:/usr/lib:\${IRSCRUTINIZERHOME}/Linux-amd64 \
     ${JAVA_QUICKSTART} -jar \${IRSCRUTINIZERHOME}/${FATJAR} "\$@"
EOF

chmod 555 ${WRAPPER}
ln -s ${MYPROG_LOWER} ${USR_BIN}/irptransmogrifier
ln -s ${MYPROG_LOWER} ${USR_BIN}/harchardware

# Invoke the builder
#wget -c "https://github.com/AppImage/AppImageKit/releases/download/12/appimagetool-x86_64.AppImage"
#chmod +x ./appimagetool-x86_64.AppImage

# Invocation of appstreamcli breaks on many systems, see
# https://github.com/AppImage/AppImageKit/issues/856
# Until that is fixed, must use --no-appstream

# Signing does not work at Travis, since it does not have access to the private key,
# but this is handled gracefully, so no need to handle special cases.

ARCH=x86_64; export ARCH
if [ -n "${VERBOSE}" ] ; then
    tools/appimagetool-x86_64.AppImage -v --sign --no-appstream ${APPDIR} ${APPIMAGE}
else
    tools/appimagetool-x86_64.AppImage --sign --no-appstream ${APPDIR} ${APPIMAGE} > /dev/null
fi
