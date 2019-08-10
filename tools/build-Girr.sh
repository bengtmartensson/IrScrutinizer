#!/bin/sh
git clone https://github.com/bengtmartensson/Girr.git
cd Girr
#git checkout IrpMaster

mvn install -Dmaven.test.skip=true

git checkout Version-2.0.0
mvn install -Dmaven.test.skip=true