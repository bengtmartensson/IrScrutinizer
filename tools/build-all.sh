#!/bin/sh

# This tool is intended for the maintainer only.

SUBPROJECTS="IrpTransmogrifier Girr Jirc RemoteLocator HarcHardware IrScrutinizer"

for proj in ${SUBPROJECTS} ; do
    echo Building $proj
    ( cd $proj ; \
    mvn clean; \
    mvn install )
done

for proj in ${SUBPROJECTS} ; do
    (cd $proj ; \
    git status )
done

for proj in ${SUBPROJECTS} ; do
    (cd $proj ; \
    xpath -e "/project/version/text()" -q  pom.xml )
done
