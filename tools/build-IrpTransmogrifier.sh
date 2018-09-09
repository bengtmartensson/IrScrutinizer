#!/bin/sh
git clone https://github.com/bengtmartensson/IrpTransmogrifier.git
cd IrpTransmogrifier

mvn install -Dmaven.test.skip=true
