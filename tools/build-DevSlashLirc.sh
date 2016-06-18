#!/bin/sh
git clone https://github.com/bengtmartensson/DevSlashLirc.git
cd DevSlashLirc

# Fake media/lirc.h, normally in /usr/local/include/lirc/include/media/lirc.h
#mkdir -p target/generated-sources/c++/org/harctoolbox/devslashlirc/media
#wget -O  target/generated-sources/c++/org/harctoolbox/devslashlirc/media/lirc.h https://sourceforge.net/p/lirc/git/ci/master/tree/include/media/lirc.h?format=raw

#make lib
mvn package -Dmaven.test.skip=true