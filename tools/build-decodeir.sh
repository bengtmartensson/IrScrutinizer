#!/bin/sh

# svn export -q svn://svn.code.sf.net/p/controlremote/code/trunk/decodeir
wget -c http://www.harctoolbox.org/downloads/decodeir.tar.gz
tar xfvz decodeir.tar.gz
cd decodeir
mvn install

# end here if compilation of the shared library is not wanted

autoreconf -fi
./configure
make
sudo make install
