#!/bin/sh
git clone https://github.com/bengtmartensson/IrpMaster.git
cd IrpMaster

mvn install -Dmaven.test.skip=true
