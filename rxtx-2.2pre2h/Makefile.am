###########################################################################
#   RXTX License v 2.1 - LGPL v 2.1 + Linking Over Controlled Interface.
#   RXTX is a native interface to serial ports in java.
#   Copyright 1997-2007 by Trent Jarvi tjarvi@qbang.org and others who
#   actually wrote it.  See individual source files for more information.
#
#   A copy of the LGPL v 2.1 may be found at
#   http://www.gnu.org/licenses/lgpl.txt on March 4th 2007.  A copy is
#   here for your convenience.
#
#   This library is free software; you can redistribute it and/or
#   modify it under the terms of the GNU Lesser General Public
#   License as published by the Free Software Foundation; either
#   version 2.1 of the License, or (at your option) any later version.
#
#   This library is distributed in the hope that it will be useful,
#   but WITHOUT ANY WARRANTY; without even the implied warranty of
#   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
#   Lesser General Public License for more details.
#
#   An executable that contains no derivative of any portion of RXTX, but
#   is designed to work with RXTX by being dynamically linked with it,
#   is considered a "work that uses the Library" subject to the terms and
#   conditions of the GNU Lesser General Public License.
#
#   The following has been added to the RXTX License to remove
#   any confusion about linking to RXTX.   We want to allow in part what
#   section 5, paragraph 2 of the LGPL does not permit in the special
#   case of linking over a controlled interface.  The intent is to add a
#   Java Specification Request or standards body defined interface in the 
#   future as another exception but one is not currently available.
#
#   http://www.fsf.org/licenses/gpl-faq.html#LinkingOverControlledInterface
#
#   As a special exception, the copyright holders of RXTX give you
#   permission to link RXTX with independent modules that communicate with
#   RXTX solely through the Sun Microsytems CommAPI interface version 2,
#   regardless of the license terms of these independent modules, and to copy
#   and distribute the resulting combined work under terms of your choice,
#   provided that every copy of the combined work is accompanied by a complete
#   copy of the source code of RXTX (the version of RXTX used to produce the
#   combined work), being distributed under the terms of the GNU Lesser General
#   Public License plus this exception.  An independent module is a
#   module which is not derived from or based on RXTX.
#
#   Note that people who make modified versions of RXTX are not obligated
#   to grant this special exception for their modified versions; it is
#   their choice whether to do so.  The GNU Lesser General Public License
#   gives permission to release a modified version without this exception; this
#   exception also makes it possible to release a modified version which
#   carries forward this exception.
#
#   You should have received a copy of the GNU Lesser General Public
#   License along with this library; if not, write to the Free
#   Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
#   All trademarks belong to their respective owners.
############################################################################
srcdir = @srcdir@
abs_srcdir=$(TOP)/@srcdir@
VPATH = @srcdir@

AUTOMAKE_OPTIONS = foreign
INCLUDES= -I$(TOP) -I$(DEST) -I. $(JAVAINCLUDE) $(JAVANATINC)
####################### vars for rxtx-1.4->1.5 transition
JARTARGET=RXTXcomm.jar
CLASSDIR=gnu.io
CLASSTOP=gnu
CLASSDEST=gnu/io
####################### tools
LIBTOOLC=$(GLIBTOOL) --mode=compile $(CC) $(INCLUDES) $(CPPFLAGS) $(CFLAGS) $(VERBOSE_IOEXCEPTIONS) -c
LIBTOOL_INST=$(GLIBTOOL) --mode=install $(INSTALL_PROGRAM)
####################### dirs
SPATH = $(abs_srcdir)/src
DEST=$(target_triplet)
####################### rules
CLASSES = $(patsubst $(SPATH)/gnu/io/%.java,$(TOP)/$(CLASSDEST)/%.class,$(wildcard $(SPATH)/gnu/io/*.java))
javafiles = $(wildcard $(SPATH)/gnu/io/*.java)
javahfiles = $(patsubst $(SPATH)/gnu/io/%.java,%,$(wildcard $(SPATH)/gnu/io/*.java))

#######################
CLEANFILES = $(TOP)/*.class $(TOP)/$(CLASSDEST)/*.class $(TOP)/$(JARTARGET)

# for SCO builds everything else is currently using lib%.la below
# I'm thinking about taking libtool out completely.  See also Mac OS X builds.
$(target_triplet)/lib%.so: $(SPATH)/%Imp.@EXTENSION@ $(CLASSES)
	$(COMPILE) $(SPATH)/$*Imp.@EXTENSION@ -o $(TOP)/$(DEST)/$*Imp.o
	$(LINK) -G -o lib$*.so $(TOP)/$(DEST)/$*Imp.o

#  compile src/SerialImp.c and similar files into libraries.
#  LIBTOOL_FIX handles problems encountered on some OS's.  See configure.in
#  If a LIBTOOL_FIX is added be sure to provide a trailing ';' in configure.in

$(target_triplet)/librxtx%.@LIB_EXTENSION@: $(SPATH)/%Imp.c $(CLASSES) $(SPATH)/%Imp.h
	(								\
	$(LIBTOOL_FIX)							\
	cd $(DEST); 							\
	$(LIBTOOLC) $(SPATH)/$*Imp.@EXTENSION@ -o			\
		$(TOP)/$(DEST)/$*Imp.@OBJECT_EXTENSION@;		\
	if [ "$*" = "Serial" ]; then 					\
		$(LIBTOOLC) $(SPATH)/fuserImp.@EXTENSION@ -o		\
			$(TOP)/$(DEST)/fuserImp.@OBJECT_EXTENSION@;	\
		@LINK@ $(TOP)/$(DEST)/$*Imp.@OBJECT_EXTENSION@  	\
			$(TOP)/$(DEST)/fuserImp.@OBJECT_EXTENSION@; 	\
	else								\
		@LINK@ $(TOP)/$(DEST)/$*Imp.@OBJECT_EXTENSION@; 	\
	fi;								\
	)

#  compile src/RXTXPort.java and similar files into classes
#  then create the .jar file
$(CLASSES): $(javafiles) $(TOP)/Makefile
	$(JAVAH_FIX)
	$(JAVAC) $(javafiles)
	$(JAVAH) `(for i in $(javahfiles);do \
		if grep > /dev/null native.*\( $(SPATH)/gnu/io/$$i.java; then \
			echo $(CLASSDIR).$$i; \
		fi; \
	done)`
	$(JAR) cf $(JARTARGET) $(CLASSTOP)/*

#  install librxtxSerial.so into the proper directory and copy $(JARTARGET) to its
#  proper location
install: all
	@$(LIBTOOL_INST) \
	`for i in $(TARGETLIB);do \
		if [ -f $$i ];then \
			echo $$i; \
		fi; \
	done` $(RXTX_PATH)
	$(INSTALL_PROGRAM) $(JARTARGET) $(JHOME)/

clean-generic:
	rm -rf $(DEST) $(TOP)/$(CLASSTOP)
	-test -z "$(CLEANFILES)" || rm -f $(CLEANFILES)

all: $(TARGETLIB)

#  create the javadoc files.
docs: $(CLASSES)
	$(JAVADOC) $(javafiles)

################  WIN32 CrossCompiling from here down #######################

$(DEST)/rxtxSerial.dll: $(CLASSES) $(TOP)/Makefile
	gcc -DWIN32 -D __int64="long long" -mno-fp-ret-in-387 -b$(target_alias) -I $(TOP) -I $(DEST) -I . $(WINDOWS_JAVA_INCLUDE) -I $(SPATH) -Wall -c $(SPATH)/SerialImp.c -o $(DEST)/SerialImp.o
	gcc -DWIN32 -D __int64="long long" -mno-fp-ret-in-387 -b$(target_alias) -I $(TOP) -I $(DEST) -I . $(WINDOWS_JAVA_INCLUDE) -I $(SPATH) -Wall -c $(SPATH)/termios.c -o $(DEST)/termios.o
	gcc -DWIN32 -D __int64="long long" -mno-fp-ret-in-387 -b$(target_alias) -I $(TOP) -I $(DEST) -I . $(WINDOWS_JAVA_INCLUDE) -I $(SPATH) -Wall -c $(SPATH)/fixup.c -o $(DEST)/fixup.o
	gcc -DWIN32 -D __int64="long long" -mno-fp-ret-in-387 -b$(target_alias) -I $(TOP) -I $(DEST) -I . $(WINDOWS_JAVA_INCLUDE) -I $(SPATH) -Wall -c $(SPATH)/init.cc -o $(DEST)/init.o
	ld --base-file $(DEST)/Serial.base --dll -o $(DEST)/rxtxSerial.dll $(DEST)/termios.o $(DEST)/SerialImp.o $(DEST)/fixup.o $(DEST)/init.o -luser32 -lgdi32 -lcomdlg32 -lkernel32 -ladvapi32 -lmingw32 -lmoldname -lcrtdll -e _dll_entry@12 --base-file $(DEST)/Serial.base
	 echo EXPORTS >$(DEST)/Serial.def;for i in `nm $(DEST)/rxtxSerial.dll | grep "T _Java"|cut -b 13-`;do echo -n $$i|sed s#@.*##;echo "="$$i;done >> $(DEST)/Serial.def
	dlltool -b$(DEST) --as=as --dllname $(DEST)/rxtxSerial.dll --def $(DEST)/Serial.def --base-file $(DEST)/Serial.base --output-exp $(DEST)/Serial.exp
	ld --base-file $(DEST)/Serial.base $(DEST)/Serial.exp -dll -o $(DEST)/rxtxSerial.dll $(DEST)/termios.o $(DEST)/SerialImp.o $(DEST)/fixup.o $(DEST)/init.o -luser32 -lgdi32 -lcomdlg32 -lkernel32 -ladvapi32 -lmingw32 -lmoldname -lcrtdll -e _dll_entry@12 --base-file $(DEST)/Serial.base
	dlltool -b$(DEST) --as=as --dllname $(DEST)/rxtxSerial.dll --def $(DEST)/Serial.def --base-file $(DEST)/Serial.base --output-exp $(DEST)/Serial.exp
	ld $(DEST)/Serial.exp -dll -o $(DEST)/rxtxSerial.dll $(DEST)/termios.o $(DEST)/SerialImp.o $(DEST)/fixup.o $(DEST)/init.o -luser32 -lgdi32 -lcomdlg32 -lkernel32 -ladvapi32 -lmingw32 -lmoldname -lcrtdll -e _dll_entry@12 --base-file $(DEST)/Serial.base

$(DEST)/rxtxParallel.dll: $(CLASSES) $(TOP)/Makefile
	gcc -DWIN32 -D __int64="long long" -mno-fp-ret-in-387 -b$(target_alias) -I $(TOP) -I $(DEST) -I . $(WINDOWS_JAVA_INCLUDE) -I $(SPATH) -Wall -c $(SPATH)/ParallelImp.c -o $(DEST)/ParallelImp.o
	gcc -DWIN32 -D __int64="long long" -mno-fp-ret-in-387 -b$(target_alias) -I $(TOP) -I $(DEST) -I . $(WINDOWS_JAVA_INCLUDE) -I $(SPATH) -Wall -c $(SPATH)/termios.c -o $(DEST)/termios.o
	gcc -DWIN32 -D __int64="long long" -mno-fp-ret-in-387 -b$(target_alias) -I $(TOP) -I $(DEST) -I . $(WINDOWS_JAVA_INCLUDE) -I $(SPATH) -Wall -c $(SPATH)/fixup.c -o $(DEST)/fixup.o
	gcc -DWIN32 -D __int64="long long" -mno-fp-ret-in-387 -b$(target_alias) -I $(TOP) -I $(DEST) -I . $(WINDOWS_JAVA_INCLUDE) -I $(SPATH) -Wall -c $(SPATH)/init.cc -o $(DEST)/init.o
	ld --base-file $(DEST)/Parallel.base --dll -o $(DEST)/rxtxParallel.dll $(DEST)/termios.o $(DEST)/ParallelImp.o $(DEST)/fixup.o $(DEST)/init.o -luser32 -lgdi32 -lcomdlg32 -lkernel32 -ladvapi32 -lmingw32 -lmoldname -lcrtdll -e _dll_entry@12 --base-file $(DEST)/Parallel.base
	 echo EXPORTS >$(DEST)/Parallel.def;for i in `nm $(DEST)/rxtxParallel.dll | grep "T _Java"|cut -b 13-`;do echo -n $$i|sed s#@.*##;echo "="$$i;done >> $(DEST)/Parallel.def
	dlltool -b$(DEST) --as=as --dllname $(DEST)/rxtxParallel.dll --def $(DEST)/Parallel.def --base-file $(DEST)/Parallel.base --output-exp $(DEST)/Parallel.exp
	ld --base-file $(DEST)/Parallel.base $(DEST)/Parallel.exp -dll -o $(DEST)/rxtxParallel.dll $(DEST)/termios.o $(DEST)/ParallelImp.o $(DEST)/fixup.o $(DEST)/init.o -luser32 -lgdi32 -lcomdlg32 -lkernel32 -ladvapi32 -lmingw32 -lmoldname -lcrtdll -e _dll_entry@12 --base-file $(DEST)/Parallel.base
	dlltool -b$(DEST) --as=as --dllname $(DEST)/rxtxParallel.dll --def $(DEST)/Parallel.def --base-file $(DEST)/Parallel.base --output-exp $(DEST)/Parallel.exp
	ld $(DEST)/Parallel.exp -dll -o $(DEST)/rxtxParallel.dll $(DEST)/termios.o $(DEST)/ParallelImp.o $(DEST)/fixup.o $(DEST)/init.o -luser32 -lgdi32 -lcomdlg32 -lkernel32 -ladvapi32 -lmingw32 -lmoldname -lcrtdll -e _dll_entry@12 --base-file $(DEST)/Parallel.base

