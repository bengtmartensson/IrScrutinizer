#!/bin/sh
git clone https://github.com/bengtmartensson/Jirc.git
cd Jirc

mvn install -Dmaven.test.skip=true
