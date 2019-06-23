# NOTE: This Makefile is not required to build the program, for which maven
# is used. Instead, it invokes the program for tests and for transforming the
# output, for example to the lirc.xml file.

MYPROG := IrScrutinizer
BROWSE := firefox
INSTALLDIR := /usr/local/share/irscrutinizer

MYDIR := $(dir $(firstword $(MAKEFILE_LIST)))
TOP := $(dir $(abspath $(MYDIR)))

VERSION=$(shell cat $(TOP)/target/$(MYPROG).version | cut  --delimiter=' ' -f 3)

IRSCRUTINIZER_JAR := target/IrScrutinizer-jar-with-dependencies.jar

default: $(IRSCRUTINIZER_JAR)

$(IRSCRUTINIZER_JAR):
	mvn package -Dmaven.test.skip=true

apidoc javadoc: target/site/apidocs/index.html

target/site/apidocs/index.html:
	mvn javadoc:javadoc
	$(BROWSE) target/site/apidocs/index.html

# Only for Unix-like systems
install: $(IRP_TRANSMOGRIFIER_JAR) | $(INSTALLDIR)
	rm -rf $(INSTALLDIR)/*
	( cd $(INSTALLDIR); unzip $(TOP)/target/$(MYPROG)-$(VERSION)-bin.zip && ./setup-irscrutinizer.sh )
	
$(INSTALLDIR):
	mkdir -p $@

clean:
	mvn clean

.PHONY: clean
