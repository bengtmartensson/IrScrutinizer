# harctoolbox

Welcome to harctoolbox. More info on this project is at the
[website](http://www.harctoolbox.org/).

## Status

This is pre-alfa-testing state: don't even think about using it unless
you really know what you are doing. Latest stable versions of the harctoolbox
software is available at the
[download page](http://www.harctoolbox.org/downloads/index.html)

## Dependencies and bundled software

Despite of being a Java and Maven project, building is presently only
supported on Linux-like systems.

As a convenience, the following 3-rd party software is bundled by
harctoolbox:
  - **crystal project icons**, also packaged for most linux distributions
  - **wakeonlan** from  http://sourceforge.net/p/java-wakeonlan
  - **rxtx** which is JNI software using both native libraries
    (dll/so-files) and a java-generated jar file. Most (all?) linux
    distributions contains a rxtx package.
      - If you are on Fedora, use the system package.
      - If there is no rxtx package for your system: use the bundled one.
      - If you are using an Arduino or irToy device: use the bundled one.
      - Otherwise, use the system package.
    Build instructions for bundled package is in rxtx-2.2*/INSTALL.

harctoolbox has a few dependencies. These need to be installed also in
maven in order for the build to work. The dependencies are

  - **DecodeIr** from hifiremote http://sourceforge.net/p/controlremote
  - **ExchangeIR** also from hifiremote.
  - **tonto** from http://mrallen.com/tonto/
  - minimal-json from http://github.com/ralfstx/minimal-json.
  - The unix2dos and dos2unix utilities, sometimes in the dos2unix package.
  - The  icotool utility, sometimes in the icoutils package
  - For building the Windows setup.exe, the Inno Installer version 5
    (http://www.jrsoftware.org/download.php/is.exe) is needed.

Besides these, maven handled various dependencies  which are downloaded at
build time.

For Fedora users there are temporary RPM:s for the dependencies available at
https://copr.fedoraproject.org/coprs/leamas/harctoolbox/.

With the dependencies available you could run the script *install-deps.sh*
which installs them in you local maven repository before building.

## Building

Building is based on maven. `mvn install` creates some artifacts which can
be used to run IrScrutinizer in the ```IrScrutinizer/target```
directory.

To build the Windows setup.exe file, preferrably the work area should
be mounted from a Windows computer. Then open the generated file
IrScrutinizer/target/IrScrutinizer_inno.iss with the Inno installer,
and start the compile.

## Installation

As of today there is installation support like "make install". After
running *mvn package* in the top directory there are installations
artifacts (jars, documentation and configuration) in package/dist. For
installing the generic-binary package
(IrScrutinizer/target/IrScrutinizer-bin.tar.gz), see the instructions
in INSTALL-binary.txt, located in IrScrutinizer/src/main/doc, and also
in the mentioned tar file.


The rxtx package has support for installing the so/dll libraries, see
rxtx-2.2*/INSTALL.

## Running

### IrScrutinizer

Run irscrutinizer using
```
    $ cd IrScrutinizer
    $ java -jar target/IrScrutinizer-*-jar-with-dependencies.jar
```

### IrpMaster

Invoke IrpMaster with

```
    cd IrpMaster
    java -jar target/IrpMaster-*-jar-with-dependencies.jar  --help
```
