#!/bin/sh

# Wrapper for lirc2xml.

# For a user friendly GUI alternative, check out IrScrutinizer,
# http://www.harctoolbox.org/IrScrutinizer.html

JAVA=java
DECODEIRLIB=/usr/local/lib64
JAR=dist/Jirc.jar

"$JAVA" -Djava.library.path="$DECODEIRLIB" -jar "$JAR" "$@"
