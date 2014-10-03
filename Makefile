#
# Makefile for mscviewer
#
# Oct 2011, Roberto Attias
#
# Copyright (c) 2010-2012 by Cisco Systems, Inc.
# All rights reserved.

#------------------------------------------------------------
# Parameters:
 
# Change JAVA_BIN var if your $JDK/bin is not in the path
JAVA_BIN = ""

# Change INSTALL_PREFIX if you don't want to install in $HOME
INSTALL_PREFIX := $(shell echo $$HOME)
#------------------------------------------------------------

ifeq ($(JAVA_BIN),"")
JAVAC = javac
JAVA = java
JAR = jar
else
JAVAC = $(JAVA_BIN)/javac
JAVA = $(JAVA_BIN)/java
JAR  = $(JAVA_BIN)/jar
endif

JYTHON_JAR:=jython-standalone-2.5.4-rc1.jar
SWINGX_JAR:=swingx-all-1.6.4.jar
UNAME:=$(shell uname -s)
TEMPFILE1:=$(shell mktemp)
MSCVER = $(shell  cat src/com/cisco/mscviewer/Main.java | grep VERSION | cut -d\" -f2)

INSTALL_DIR := $(INSTALL_PREFIX)/mscviewer_$(MSCVER)
ifeq (,$(findstring CYGWIN,$(UNAME)))
  P:=:
  TEMPFILE := $(TEMPFILE1)
  INSTALL_DIR_N :=$(INSTALL_DIR)
  INSTALL_DIR_N_QB := $(INSTALL_DIR)
else
  P:=;
  TEMPFILE := $(shell cygpath -m $(TEMPFILE1))
  INSTALL_DIR_N := $(shell cygpath -w $(INSTALL_DIR))
  INSTALL_DIR_N_FS := $(subst \,/,$(INSTALL_DIR_N))
endif
THIRD_PARTIES_PATH=$(INSTALL_DIR_N_FS)/third-parties

XJARS := $(THIRD_PARTIES_PATH)/$(JYTHON_JAR)$P$(THIRD_PARTIES_PATH)/$(SWINGX_JAR)

.PHONY: all clean install jar
install: jar
	@echo "Installing mscviewer_$(MSCVER) in $(INSTALL_PREFIX)"
	@mkdir -p $(INSTALL_DIR)
	@cp -rf bin batch examples doc third-parties $(INSTALL_DIR)
	@cp -rf mscviewer.jar plugins $(INSTALL_DIR)
	@echo $(JAVA) -jar\
          "\"-Dpypath=$(INSTALL_DIR_N_FS)/examples$P" \
                     "$(INSTALL_DIR_N_FS)/plugins\"" \
                     "$(INSTALL_DIR_N_FS)/mscviewer.jar" \
          '$$@' >$(INSTALL_DIR)/bin/mscviewer
	@echo $(JAVA)\
          -jar \
          "\"-Dpypath=$(INSTALL_DIR_N_FS)/examples$P" \
                     "$(INSTALL_DIR_N_FS)/plugins\"" \
                     "$(INSTALL_DIR_N_FS)/mscviewer.jar" \
          '%*' >$(INSTALL_DIR)/bin/mscviewer.bat

	@chmod 755 $(INSTALL_DIR)/bin/*
	@rm -rf $(shell find $(INSTALL_DIR) -name '.*')
	@echo "Install competed"
 
all:  
	@echo "Building mscviewer code..."
	@find  src -name *.java >.srclist
	@mkdir -p classes
	@$(JAVAC) -g -Xlint -classpath "src$Pthird-parties/$(SWINGX_JAR)$Pthird-parties/$(JYTHON_JAR)" -d classes @.srclist
	@mkdir -p bin
	@echo "$(JAVA) -classpath $(CURDIR)$P$(CURDIR)/classes$P$(XJARS) -Dpypath=$(CURDIR)/plugin:$(CURDIR)/python com.cisco.mscviewer.Main '$$@' >$(CURDIR)/bin/mscviewer"i >bin/mscviewer
	@chmod +x bin/mscviewer


clean:
	-@rm -rf classes bin .srclist
	-@rm -rf $(INSTALL_DIR)
	-@rm -f $(WS_TOOLS_DIR)/host_tools.$(TARGET).sentinel 

jar: all
	@echo "Manifest-Version: 1.1" >$(TEMPFILE1)
	@echo "Created-By: Rattias" >>$(TEMPFILE1)
	@echo "Class-Path: third-parties/$(JYTHON_JAR) third-parties/$(SWINGX_JAR)" >>$(TEMPFILE1)
	@echo "Main-Class: com.cisco.mscviewer.Main" >>$(TEMPFILE1)
	@echo >>$(TEMPFILE1)
        
	@echo "Packaging jar file..."
	@$(JAR) cmf $(TEMPFILE) mscviewer.jar -C classes .
    

		
