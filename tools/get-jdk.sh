#!/bin/sh
# Downloads the JDK distro defined in the pom.xml

DIR="$(dirname -- "$(readlink -f -- "${0}")" )"

# Get the URL from XML
URL="$(xsltproc --stringparam SYSTEM $1 ${DIR}/extract_jdk_url.xsl ${DIR}/../pom.xml)"

# and get it
echo -n "Downloading ${URL} ..."
wget --quiet ${URL}
echo " done"
