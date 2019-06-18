#!/bin/sh
git clone https://github.com/bengtmartensson/Girr.git
cd Girr
#git checkout IrpMaster

mvn install -Dmaven.test.skip=true
