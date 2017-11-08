#!/bin/sh
git clone https://github.com/bengtmartensson/Girr.git
cd Girr

mvn install -Dmaven.test.skip=true
