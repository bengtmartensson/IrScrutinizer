#!/bin/sh
git clone https://github.com/bengtmartensson/$1.git
cd $1
if [ -n "$2" ] ; then
    git checkout $2
fi

mvn install -Dmaven.test.skip=true
