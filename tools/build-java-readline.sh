#!/bin/sh
git clone https://github.com/bengtmartensson/java-readline
cd java-readline
mvn install

# end here if compilation of the shared library is not wanted

make build-native
sudo make install
