#! /bin/sh

if [ $# -lt 1 ] ; then
    echo -e "Usage:\n$0 <JDK-file>" >&2
    exit 1
fi

JAVA_ZIP=$1
JAVA_MODULES=java.base,java.datatransfer,java.desktop,java.logging,java.xml,jdk.crypto.ec
JRE_DIR=jre-x86-windows

rm -rf ${JRE_DIR}

jar xf ${JAVA_ZIP}
JAVA_ROOT=`echo jdk*`

WINEDEBUG=-all; export WINEDEBUG
wine ${JAVA_ROOT}/bin/jlink --no-header-files --no-man-pages --compress=2 --strip-debug --add-modules ${JAVA_MODULES} --output jre-x86-windows
rm -rf ${JAVA_ROOT}
