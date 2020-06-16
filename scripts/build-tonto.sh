#!/bin/sh
git clone https://github.com/stewartoallen/tonto
cd tonto
git checkout be1657a
sed -i -e '/signjar/d' -e 's/<javac/<javac source="1.6" target="1.6"/' build.xml
ant all
mvn install:install-file \
    -DgroupId=com.mrallen \
    -DartifactId=tonto \
    -Dversion=1.44 \
    -Dpackaging=jar \
    -Dfile=jars/tonto.jar

# End here if the serial communication library is not wanted
if [ "$1" = "-n" ] ; then
    echo "Not building the shared library"
    exit 0
fi

cd libs
sh ./build-fedora.sh
sudo install -D libjnijcomm.so /usr/local/lib/tonto/libjnijcomm.so
