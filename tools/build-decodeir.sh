#!/bin/sh

PROGNAME=decodeir
svn export -r 1358 -q svn://svn.code.sf.net/p/controlremote/code/trunk/${PROGNAME}
cd ${PROGNAME}
mvn install

# end here if compilation of the shared library is not wanted

autoreconf -fi
./configure
make
sudo make install 
