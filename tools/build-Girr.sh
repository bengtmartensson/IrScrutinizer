#!/bin/sh
git clone https://github.com/bengtmartensson/Girr.git
cd Girr
git checkout $1
mvn install -Dmaven.test.skip=true

#git checkout Version-2.0.1
#mvn install -Dmaven.test.skip=true
