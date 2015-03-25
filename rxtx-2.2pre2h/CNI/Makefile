
CFLAGS=-Wall -g -O2 -D_POSIX_SOURCE -D_BSD_SOURCE -D__need_timespec -O2 -D_POSIX_SOURCE -D_BSD_SOURCE -D__need_timespec

javafiles=$( wildcard *.java )
classfiles=$( patsubst %.java,gnu/io/%.class,$( wildcard *.java ) )
objects=$( patsubst %.java,gnu/io/%.o,$( wildcard *.java ) )

all:  Simple

Simple: librxtx.so librxtx.a
	#gcj *.o librxtx.a -static  -lgcj -lpthread -ldl -lz -o Simple --main=Simple
	ln -s Simple.Java Simple.java
	gcj --classpath RXTX.jar  -d . -C Simple.java
	rm Simple.java
	gcj -g -classpath RXTX.jar -d . -c Simple.class
	gcj -g *.o gnu/io/*.o -static  -lgcj -lpthread -ldl -lz -o Simple --main=Simple


librxtx.a:  gnu/io/SerialImp.o gnu/io/fuserImp.o objects
	ar rs librxtx.a  gnu/io/*.o *.o

librxtx.so:  gnu/io/SerialImp.o gnu/io/fuserImp.o objects
	g++ -g -shared gnu/io/*.o *.o -lpthread  -Wl,-soname -Wl,librxtx-0.0-1.so -o librxtx-0.0-1.so
	rm -f librxtx.so && ln -s librxtx-0.0-1.so librxtx.so

objects: classfiles
	(cd gnu/io && gcj -g -classpath ../.. -d gnu/io -c *.class )

gnu/io/SerialImp.o:  classfiles
	g++ $(CFLAGS) -I . -c SerialImp.c

gnu/io/fuserImp.o:  classfiles
	g++ $(CFLAGS) -I . -c fuserImp.c

classfiles:
	rm -f Simple.java
	gcj -d . -C *.java
	for i in gnu/io/*.class;do gcjh -d . `echo $$i|sed s/.class//g`;done
	jar -cf RXTX.jar gnu/io/*.class

clean:
	rm -f gnu/io/*
	rm -f *.a *.o *.so *.class Simple *.jar
	rmdir gnu/io
	rmdir gnu
