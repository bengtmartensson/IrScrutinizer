ifndef VERSION

include version.mk

version.mk: $(VERSION_XML)
	$(XALAN) -XSL $(TOOLS)/mkVersionMkFile.xsl -IN $< -OUT $@

version: $(APPLICATION).version src/$(PROJECT_PREFIX)/$(PACKAGE)/Version.java

$(APPLICATION).version: $(VERSION_XML)
	$(XALAN) -XSL $(TOOLS)/mkVersionFile.xsl -IN $< -OUT $@

src/$(PROJECT_PREFIX)/$(PACKAGE)/Version.java: $(VERSION_XML)
	$(XALAN) -XSL $(TOOLS)/mkVersion.xsl -IN $< -OUT $@

else

version:
	@echo VERSION = $(VERSION), is defined in Makefile

endif

ifeq ($(wildcard programdata/$(PROJECT_PREFIX)/$(PACKAGE)/Props.xml),)
prop:
	@echo No props in $(APPLICATION)
else
props: src/$(PROJECT_PREFIX)/$(PACKAGE)/Props.java

src/$(PROJECT_PREFIX)/$(PACKAGE)/Props.java: programdata/$(PROJECT_PREFIX)/$(PACKAGE)/Props.xml
	$(XALAN) -XSL $(TOOLS)/mkProps.xsl -IN $< -OUT $@
endif

.PHONY: docu clean veryclean all ant src-dist bin-dist version

all: import version dist/$(APPLICATION).jar docu src-dist bin-dist inno

SRC_DIST = $(APPLICATION)-src-$(VERSION).zip
ifeq ($(BIN_DIST_FILES),)
BIN_DIST=
else
BIN_DIST = $(APPLICATION)-bin-$(VERSION).zip
endif

ant dist/$(APPLICATION).jar: import
	$(ANT)

import: $(foreach proj,$(IMPORT_PROJS),lib/$(proj).jar)

define template =
lib/$(1).jar: ../$(1)/dist/$(1).jar
	cp $$< $$@
endef

$(foreach proj,$(IMPORT_PROJS),$(eval $(call template,$(proj))))

ifeq ($(wildcard doc),)
docu:
	@echo No documentation in $(APPLICATION)
else
docu: doc/$(APPLICATION).html

doc/%.html: doc/%.xml $(TOOLS)/xdoc2html.xsl
	$(XALAN) -XSL $(TOOLS)/xdoc2html.xsl -IN $< -OUT $@

doc/%.pdf: $(PDF_DIR)/%.pdf
	cp $< $@
endif

src-dist: $(SRC_DIST)

$(SRC_DIST): $(SRC_DIST_FILES)
	$(RM) $@
	$(ZIP) $@ $(SRC_DIST_FILES)

ifneq ($(BIN_DIST_FILES),)
bin-dist: $(BIN_DIST)

$(BIN_DIST): dist/$(APPLICATION).jar $(BIN_DIST_FILES)
	$(RM) $@
	$(TAR) cf - --dereference --exclude=\.svn $(BIN_DIST_FILES) | (cd dist; $(TAR) xf -)
ifneq ($(wildcard native),)
	$(TAR) cf - -C native --exclude \.svn . | (cd dist; $(TAR) xf -)
endif
	(cd dist; $(ZIP) ../$@ $(APPLICATION).jar lib/* $(BIN_DIST_FILES) $(NATIVE_FILES))
else
bin-dist:
	@echo No bin-dist exists for $(APPLICATION)
endif

clean:
	$(ANT) clean
	$(RM) -r $(SRC_DIST) $(BIN_DIST) $(APPLICATION)-$(VERSION).exe dist doc/$(APPLICATION).html $(APPLICATION)_inno.iss run_inno.bat

veryclean: clean
	$(RM) $(APPLICATION).version version.mk

install-javadoc: ant
	$(RM) -r $(JAVADOC_INSTALLDIR)/$(APPLICATION)
	cp -a dist/javadoc $(JAVADOC_INSTALLDIR)/$(APPLICATION)

ifeq ($(wildcard $(APPLICATION)_inno.m4),)

inno:
	@echo No Inno installer in $(APPLICATION)

else

inno: dist/$(APPLICATION).jar $(BIN_DIST_FILES) $(APPLICATION)_inno.iss run_inno.bat

SETUP_EXE := $(APPLICATION)-$(VERSION).exe

$(APPLICATION)_inno.iss: $(APPLICATION)_inno.m4 $(APPLICATION).version
	m4 --define=VERSION=$(VERSION) $< > $@

run_inno.bat:
	echo del $(APPLICATION)-$(VERSION).exe > $@
	echo \"$(INNO_COMPILER)\" $(APPLICATION)_inno.iss >> $@
	echo $(APPLICATION)-$(VERSION) >> $@
	unix2dos $@

endif

export: $(SRC_DIST) $(BIN_DIST)
	cp $^ $(DISTDIR)
ifneq ($(wildcard $(APPLICATION).version),)
	cp $(APPLICATION).version $(DISTDIR)
endif
ifneq ($(wildcard $(SETUP_EXE)),)
	cp $(SETUP_EXE) $(DISTDIR)
endif
