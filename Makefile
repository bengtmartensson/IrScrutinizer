# NOTE: This Makefile is not required to build the program, for which maven
# is used. Instead, it invokes the program for tests and for transforming the
# output, for example to the lirc.xml file.



MYPROG := IrScrutinizer
INSTALLDIR := /usr/local/share/irscrutinizer
MYDIR := $(dir $(firstword $(MAKEFILE_LIST)))
TOP := $(realpath $(MYDIR))

include $(MYDIR)/common/makefiles/paths.mk

# This file is not public ;-)
-include $(MYDIR)/../RemoteLocator/upload_location.mk

GH_PAGES := $(TOP)/gh-pages
ORIGINURL := $(shell git remote get-url origin)
PROJECT_NAME := $(MYPROG)
UPLOADDIR_DIR := ftp://bengt-martensson.de/harctoolbox/
VERSION=$(shell cat $(TOP)/target/$(MYPROG).version | cut  --delimiter=' ' -f 3)

IRSCRUTINIZER_JAR := target/$(MYPROG)-$(VERSION)-jar-with-dependencies.jar
EXPORT_FORMATS := src/main/config/exportformats.d
EXPORT_MAIN := org.harctoolbox.irscrutinizer.exporter.DynamicRemoteSetExportFormatMain
EXPORT_FORMATS_SCHEMA_LOCATION := http://www.harctoolbox.org/schemas/exportformats.xsd

default: $(IRSCRUTINIZER_JAR)

$(IRSCRUTINIZER_JAR):
	mvn package -Dmaven.test.skip=true

apidoc javadoc: target/site/apidocs/index.html
	$(BROWSE) $<

version:
	@echo $(VERSION)

push:
	git push

setversion:
	mvn versions:set -DnewVersion=$(NEWVERSION)
	git commit -S -m "Set version to $(NEWVERSION)" pom.xml src/main/doc/$(PROJECT_NAME).releasenotes.txt

target/site/apidocs/index.html:
	mvn javadoc:javadoc

gh-pages: target/site/apidocs/index.html
	rm -rf $(GH_PAGES)
	git clone --depth 1 -b gh-pages ${ORIGINURL} ${GH_PAGES}
	( cd ${GH_PAGES};  \
	cp -r ../target/site/apidocs/* . ; \
	git add * ; \
	git commit -a -m "Update of API documentation" ; \
	git push )

tag:
	git checkout master
	git status
	git tag -a Version-$(VERSION) -m "Tagging Version-$(VERSION)"
	git push origin Version-$(VERSION)

upload-version: target/$(MYPROG).version
	scp $< $(UPLOAD_LOCATION)

upload-harctoolbox:
	@(cd $(TOP)/../www.harctoolbox.org ; \
	make clean ; \
	make site ; \
	cd build/site/en ; \
	for file in $(MYPROG).html $(MYPROG).pdf $(MYPROG).releasenotes.txt wholesite.html wholesite.pdf ; do \
	echo Uploading $$file... ; \
		curl --netrc --upload-file $$file $(UPLOADDIR_DIR)/$$file;\
	done )

Arduino_Raw.ino: $(IRSCRUTINIZER_JAR)
	$(JAVA) -cp "$<" $(EXPORT_MAIN) -c "$(EXPORT_FORMATS)"  -f "Arduino Raw" -s "$(EXPORT_FORMATS_SCHEMA_LOCATION)" -o "$@" src/test/girr/nec1-test-fat.girr

# Only for Unix-like systems
install: $(IRP_TRANSMOGRIFIER_JAR) | $(INSTALLDIR)
	rm -rf $(INSTALLDIR)/*
	( cd $(INSTALLDIR); unzip -q $(TOP)/target/$(MYPROG)-$(VERSION)-bin.zip && ./setup-irscrutinizer.sh )

$(INSTALLDIR):
	mkdir -p $@

clean:
	mvn clean
	rm -rf $(GH_PAGES) pom.xml.versionsBackup Arduino_Raw.ino

.PHONY: clean
