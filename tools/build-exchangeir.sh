#!/bin/sh

# svn export -q svn://svn.code.sf.net/p/controlremote/code/tags/exchangeir-0.0.8.2 exchangeir
wget -c http://www.harctoolbox.org/downloads/exchangeir.tar.gz
tar xfvz exchangeir.tar.gz
cd exchangeir
mvn install
