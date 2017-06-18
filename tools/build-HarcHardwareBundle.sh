#!/bin/sh
git clone https://github.com/bengtmartensson/HarcHardwareBundle.git
cd HarcHardwareBundle

mvn install -Dmaven.test.skip=true
