#!/bin/sh

# Since the Maven plugin osxappbundle-maven-plugin has some problems,
# we have to help it here.

# To be run from the directory it resides in.

version=1.1.2 # FIXME
targetdir=../IrScrutinizer/target/IrScrutinizer-$version/IrScrutinizer.app/Contents/Resources/Java/repo/org/harctoolbox/IrScrutinizer/$version
zipfile=../IrScrutinizer/target/IrScrutinizer-$version-app.zip

cp -r ../IrScrutinizer/target/doc            $targetdir
cp ../IrScrutinizer/target/IrpProtocols.ini  $targetdir
cp ../IrScrutinizer/target/protocols.ini     $targetdir
cp ../IrScrutinizer/target/exportformats.xml $targetdir
cp -r '../native/Mac OS X-i386'              $targetdir
cp -r '../native/Mac OS X-x86_64'            $targetdir

rm -f $zipfile
(cd ../IrScrutinizer/target; zip -r IrScrutinizer-$version-app.zip IrScrutinizer-$version)
