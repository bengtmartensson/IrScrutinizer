#!/bin/sh

# Wrapper for lirc2xml.
# Adjust as you see fit.

# For a user friendly GUI alternative, check out IrScrutinizer,
# http://www.harctoolbox.org/IrScrutinizer.html

# Command, absolute or in the path, of java command
JAVA=java

# Directory (or search path) where DecodeIR.so is found (optional, but desired).
DECODEIRLIB=/usr/local/lib64:/usr/local/lib:/usr/lib64/DecodeIR:/usr/lib64:/usr/lib:/usr/lib/DecodeIR

# jar file, including dependencies
JAR=target/Jirc-1.2-jar-with-dependencies.jar

"$JAVA" -Djava.library.path="$DECODEIRLIB" -jar "$JAR" "$@"
