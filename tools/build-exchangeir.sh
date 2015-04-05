#!/bin/sh

svn export -q svn://svn.code.sf.net/p/controlremote/code/tags/exchangeir-0.0.8.2 exchangeir
cd exchangeir
mvn install
