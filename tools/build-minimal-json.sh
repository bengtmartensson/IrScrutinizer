#!/bin/sh
git clone https://github.com/ralfstx/minimal-json
cd minimal-json
git checkout tags/0.9.2
mvn install -DskipTests=true -Dmaven.javadoc.skip=true
