#!/bin/sh
git clone https://github.com/bengtmartensson/HarcHardwareBundle.git
cd HarcHardwareBundle

git checkout Version-2.0.1
mvn install -Dmaven.test.skip=true
