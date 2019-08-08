#!/bin/sh
git clone https://github.com/bengtmartensson/IrpTransmogrifier.git
cd IrpTransmogrifier

mvn install -Dmaven.test.skip=true

git checkout Version-1.0.0
mvn install -Dmaven.test.skip=true
