#!/bin/sh

# Generate the pom in package.
# It contains so many version numbers that are impossible to keep consistent otherwise.

cd $(dirname $0)/..
xsltproc --output pom.xml tools/generate-pom.xsl pom-template.xml
