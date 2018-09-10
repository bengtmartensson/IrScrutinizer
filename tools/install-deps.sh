#!/bin/sh
#
#  Install external dependencies using build-classpath from jpackage-utils
#
# Author: Alec Leamas
#

mvn install:install-file \
    -DgroupId=org.rxtx \
    -DartifactId=rxtx \
    -Dversion=2.2 \
    -Dpackaging=jar \
    -Dfile=$( build-classpath org.rxtx:rxtx )

mvn install:install-file \
    -DgroupId=com.mrallen \
    -DartifactId=tonto \
    -Dversion=1.44 \
    -Dpackaging=jar \
    -Dfile=$( build-classpath com.mrallen:tonto )
