#!/bin/sh

# Generate the pom in package. It picks up the version numbers in <subpackage>/pom.xml
# and puts them in the main pom.xml.

cd $(dirname $0)/..
xsltproc --output pom.xml tools/generate-pom.xsl pom-template.xml
