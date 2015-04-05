#!/bin/sh
git clone https://github.com/stewartoallen/tonto
cd tonto
git checkout be1657a
sed -i '/signjar/d' build.xml
ant all
mvn install:install-file \
    -DgroupId=com.mrallen \
    -DartifactId=tonto \
    -Dversion=1.44 \
    -Dpackaging=jar \
    -Dfile=jars/tonto.jar

# End here if the serial communication library is not wanted
cd libs
sh ./build-fedora.sh
sudo install -D libjnijcomm.so /usr/local/lib/tonto/libjnijcomm.so
