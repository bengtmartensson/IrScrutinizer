This directory contains a private version of the RXTX sources, with some minor
changes in the java code, including a Maven compatible directory re-organization
(motivating the version suffix "h").
The changed files are: CommPortIdentifier.java, RXTXCommDriver.java, RXTXVersion.java.
These changes are found within the Git history.

The sources for rxtx were originally obtained from the qbang CVS
repository using something like

    $ export CVSROOT=:pserver:anonymous@qbang.org:/var/cvs/cvsroot
    $ cvs login
    (Logging in to anonymous@qbang.org)
    CVS password:
    $ cvs co -r commapi-0-0-1 -D "2010-02-11" -d rxtx-13 rxtx-devel

However, at the time of writing this repository is not available, so
sources are obtained from one of the Fedora mirrors using:

    wget http://ftp.acc.umu.se/mirror/fedora/linux//releases/21/Everything/source/SRPMS/r/rxtx-2.2-0.10.20100211.fc21.src.rpm
    rpm2cpio rxtx-2.2-0.10.20100211.fc21.src.rpm | cpio -i
    rm *.patch *.spec *.src.rpm
    tar xjf rxtx-20100211.tar.bz2
    mv rxtx-20100211/* .
    rm -rf rxtx-20100211.tar.bz2 rxtx-20100211
