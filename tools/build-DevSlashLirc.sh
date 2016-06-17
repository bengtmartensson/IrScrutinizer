#!/bin/sh
git clone https://github.com/bengtmartensson/DevSlashLirc.git
cd DevSlashLirc
mvn install -DskipTests=true -Dmaven.javadoc.skip=true
