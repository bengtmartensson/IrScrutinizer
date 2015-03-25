#!/bin/sh
# Run this to generate all the initial makefiles, etc.
# Taken from the GNOME package http://www.gnome.org

srcdir=.
PKG_NAME="rxtx library"

DIE=0

(autoconf --version) < /dev/null > /dev/null 2>&1 || {
    echo
    echo "**Error**: You must have "\`autoconf\'" installed to compile rxtx."
    echo "Download the appropriate package for your distribution,"
    echo "or get the source tarball at ftp://ftp.gnu.org/pub/gnu/"
    DIE=1
}

# avoid libtool on Mac OS X codename Darwin Dmitry


if test `uname` != "Darwin"; then
(libtool --version) < /dev/null > /dev/null 2>&1 || {
    echo
    echo "**Error**: You must have "\`libtool\'" installed to compile rxtx."
    echo "Get ftp://ftp.gnu.org/pub/gnu/libtool-1.2.tar.gz"
    echo "(or a newer version if it is available)"
    DIE=1
}
fi

(automake --version) < /dev/null > /dev/null 2>&1 || {
    echo
    echo "**Error**: You must have "\`automake\'" installed to compile rxtx."
    echo "Get ftp://ftp.gnu.org/pub/gnu/automake-1.3.tar.gz"
    echo "(or a newer version if it is available)"
    DIE=1
    NO_AUTOMAKE=yes
}


# if no automake, don't bother testing for aclocal
test -n "$NO_AUTOMAKE" || (aclocal --version) < /dev/null > /dev/null 2>&1 || {
    echo
    echo "**Error**: Missing "\`aclocal\'".  The version of "\`automake\'
    echo "installed doesn't appear recent enough."
    echo "Get ftp://ftp.gnu.org/pub/gnu/automake-1.3.tar.gz"
    echo "(or a newer version if it is available)"
    DIE=1
}

if test "$DIE" -eq 1; then
    exit 1
fi


autoreconf -fi
