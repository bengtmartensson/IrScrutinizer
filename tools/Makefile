# NOTE: This Makefile is not required to build the program, for which maven
# is used. Instead, it invokes the program for tests and for transforming the
# output, for example to the lirc.xml file.

MYPROG := IrScrutinizer
BROWSE := xdg-open
INSTALLDIR := /usr/local/share/irscrutinizer
MYDIR := $(dir $(firstword $(MAKEFILE_LIST)))
TOP := $(realpath $(MYDIR)..)
GH_PAGES := $(TOP)/gh-pages
ORIGINURL := $(shell git remote get-url origin)

VERSION=$(shell cat $(TOP)/target/$(MYPROG).version | cut  --delimiter=' ' -f 3)

IRSCRUTINIZER_JAR := target/IrScrutinizer-jar-with-dependencies.jar

default: $(IRSCRUTINIZER_JAR)

$(IRSCRUTINIZER_JAR):
	mvn package -Dmaven.test.skip=true

apidoc javadoc: target/site/apidocs/index.html
	$(BROWSE) $<

target/site/apidocs/index.html:
	mvn javadoc:javadoc

gh-pages: target/site/apidocs/index.html
	rm -rf $(GH_PAGES)
	git clone --depth 1 -b gh-pages ${ORIGINURL} ${GH_PAGES}
	( cd ${GH_PAGES};  \
	cp -r ../target/site/apidocs/* . ; \
	git add * ; \
	git commit -a -m "Update of API documentation" )
	echo Now perform \"git push\" from ${GH_PAGES}

# Only for Unix-like systems
install: $(IRP_TRANSMOGRIFIER_JAR) | $(INSTALLDIR)
	rm -rf $(INSTALLDIR)/*
	( cd $(INSTALLDIR); unzip $(TOP)/target/$(MYPROG)-$(VERSION)-bin.zip && ./setup-irscrutinizer.sh )

$(INSTALLDIR):
	mkdir -p $@

clean:
	mvn clean
	rm -rf $(GH_PAGES)

.PHONY: clean
