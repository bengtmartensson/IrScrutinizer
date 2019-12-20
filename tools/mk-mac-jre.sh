#! /bin/sh

if [ $# -lt 1 ] ; then
    echo -e "Usage:\n$0 <JDK-file>" >&2
    exit 1
fi

if [ "`uname -m`" != "x86_64" -o "`uname -s`" != "Darwin" ] ; then
    echo "Presently, this runs only on MacOS x86_64, quitting." >&2
    exit 1
fi

JAVA_TAR_GZ=$1

JAVA_MODULES=java.base,java.datatransfer,java.desktop,java.logging,java.xml
JRE_DIR=jre-x64-macOS

rm -rf ${JRE_DIR}

#tar zxf ${JAVA_TAR_GZ}
JAVA_ROOT=`echo jdk*`

${JAVA_ROOT}/Contents/Home/bin/jlink --no-header-files --no-man-pages --compress=2 --strip-debug --add-modules ${JAVA_MODULES} --output ${JRE_DIR}
