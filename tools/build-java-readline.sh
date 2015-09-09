#!/bin/sh
git clone https://github.com/bengtmartensson/java-readline
cd java-readline
git checkout autotools
mvn install

# end here if compilation of the shared library is not wanted

# Need to have readline including its includes.
# on Fedora, this is the packages readline AND readline-devel.
# If not, configure will barf.

./autogen.sh
./configure
make
sudo make install
