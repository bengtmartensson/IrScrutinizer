#!/bin/sh
git clone https://github.com/bengtmartensson/HarcHardware.git
cd HarcHardware

#git checkout Version-2.0.1
mvn install -Dmaven.test.skip=true
