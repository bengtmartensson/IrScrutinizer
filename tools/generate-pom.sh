#!/bin/sh

# Generate the pom in package.
# It contains so many version numbers that are impossible to keep consistent otherwise.

xsltproc --output ../pom.xml generate-pom.xsl ../pom-template.xml
