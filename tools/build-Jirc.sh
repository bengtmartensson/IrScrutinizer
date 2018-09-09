#!/bin/sh
git clone https://github.com/bengtmartensson/Jirc.git
cd Jirc
#git checkout IrpMaster

mvn install -Dmaven.test.skip=true
