#!/bin/sh
git clone https://github.com/bengtmartensson/RXTX.git
cd RXTX

#git checkout Version-2.0.1
mvn install -Dmaven.test.skip=true
