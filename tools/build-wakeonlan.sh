#!/bin/sh

wget http://www.moldaner.de/wakeonlan/download/wakeonlan-1.0.0.zip
unzip wakeonlan-1.0.0.zip
cd wakeonlan-1.0.0
ant clean
ant
mvn install:install-file \
    -DgroupId=de.moldaner \
    -DartifactId=wakeonlan \
    -Dversion=1.0.0 \
    -Dpackaging=jar \
    -Dfile=deploy/wakeonlan.jar
