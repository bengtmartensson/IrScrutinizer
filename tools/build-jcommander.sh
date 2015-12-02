#!/bin/sh
git clone https://github.com/cbeust/jcommander
cd jcommander
git checkout tags/jcommander-1.35
mvn install -DskipTests=true -Dmaven.javadoc.skip=true
