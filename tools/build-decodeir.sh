#!/bin/sh

svn export -q svn://svn.code.sf.net/p/controlremote/code/trunk/decodeir
cd decodeir
sed  -i -e 's|<version>0.1</version>|<version>2.44</version>|' pom.xml
mvn install

# end here if compilating the shared library is not needed
g++ -O2 -shared -fPIC \
    -I/usr/lib/jvm/java/include -I/usr/lib/jvm/java/include/linux \
    -o libDecodeIR.so DecodeIR.cpp
sudo install /usr/local/lib64

