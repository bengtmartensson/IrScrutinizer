#!/bin/sh
# Downloads the JDK distro defined in the pom.xml

DIR="$(dirname -- "$(readlink -f -- "${0}")" )"

# Get the URL from XML
URL="$(xsltproc ${DIR}/extract_jdk_url.xsl ${DIR}/../pom.xml)"

# and get it
echo -n "Downloading ${URL} ..."
wget --quiet --continue ${URL}
echo " done"
