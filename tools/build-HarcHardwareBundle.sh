#!/bin/sh
git clone https://github.com/bengtmartensson/HarcHardwareBundle.git
cd HarcHardwareBundle
git checkout IrpMaster

mvn install -Dmaven.test.skip=true
