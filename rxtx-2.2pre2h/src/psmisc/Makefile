CC=cc
CFLAGS=-O -D_GNU_SOURCE -Wall -Wno-parentheses -Wwrite-strings -Wpointer-arith 
REAL_CPP=/lib/cpp
LIBDIR=/usr/lib

all:	libpsmisc.la testing

testing:	testing.c libpsmisc.la
	cp .libs/*so* .
	$(CC) $(CFLAGS)  -L .libs -lpsmisc -o testing testing.c 
libpsmisc.la:	fuser.c Makefile 
	libtool --mode=compile $(CC)  $(CFLAGS)  -c fuser.c
	libtool --mode=link $(CC) $(CFLAGS) --version-info 1:0:1 -o libpsmisc.la -rpath $(LIBDIR) fuser.lo 

clean:
		rm -rf *.o .libs *~ core testing *.lo libps*
