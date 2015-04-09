#!/bin/bash

# This script generates a simple html version of the IrScrutinizer manual
# in the current directory,
# without requiring any other prerequisites than (Java) Xalan.
# On Fedora, this is installed by
#
# sudo yum install xalan-j2

# This script runs as such on Fedora 21. On other systems, the paths
# and the jars may need some tweaking.

JAR_DIR=/usr/share/java
XALAN_JAR=${JAR_DIR}/xalan-j2.jar
XALAN_SERIALIZER_JAR=${JAR_DIR}/xalan-j2-serializer.jar
JAVA=java

TOOLS=$(dirname ${BASH_SOURCE[0]})
DIR=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )

${JAVA} -cp ${XALAN_JAR}:${XALAN_SERIALIZER_JAR} org.apache.xalan.xslt.Process \
	-XSL ${TOOLS}/xdoc2html.xsl \
	-IN  ${TOOLS}/../IrScrutinizer/src/main/xdocs/IrScrutinizer.xml \
	-OUT IrScrutinizer.html

# Just for the fun of it. Do not worry if Firefox is not installed
firefox IrScrutinizer.html
