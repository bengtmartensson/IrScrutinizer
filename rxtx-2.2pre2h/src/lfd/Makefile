CC=gcc
CFLAGS= -Wall -g

all:
	$(CC) $(CFLAGS) lockdaemon.c -o lfd
	chown root:uucp lfd
	chmod 2755 lfd
