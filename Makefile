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
MSCVER = $(shell cat src/com/cisco/mscviewer/Main.java |\
           grep VERSION | cut -d\" -f2)

ifeq (,$(findstring CYGWIN,$(UNAME)))
  P:=:
else
  P:=;
endif
TEMPFILE1 := $(shell mktemp)
THIRD_PARTIES_PATH=$(INSTALL_DIR_N_FS)/third-parties

XJARS := $(THIRD_PARTIES_PATH)/$(JYTHON_JAR)$P$(THIRD_PARTIES_PATH)/$(SWINGX_JAR)
VERSIONED_NAME := mscviewer_$(MSCVER)
INSTALL_PREFIX := $(shell mktemp -d)
INSTALL_DIR := $(INSTALL_PREFIX)/$(VERSIONED_NAME)
ifeq (,$(findstring CYGWIN,$(UNAME)))
  INSTALL_DIR_N := $(INSTALL_DIR)
  INSTALL_DIR_N_FS := $(INSTALL_DIR)
  TEMPFILE := $(TEMPFILE1)
else
  INSTALL_DIR_N := $(shell cygpath -w $(INSTALL_DIR))
  INSTALL_DIR_N_FS := $(subst \,/,$(INSTALL_DIR_N))
  TEMPFILE := $(shell cygpath -m $(TEMPFILE1))
endif


.PHONY: all clean install jar distrib

distrib: jar
	$(eval $(call setup_install_vars))
	@echo "Installing mscviewer_$(MSCVER) in $(INSTALL_PREFIX)"
	@mkdir -p $(INSTALL_DIR)
	@cp -rf bin batch examples doc third-parties $(INSTALL_DIR)
	@cp -rf mscviewer.jar resources $(INSTALL_DIR)
	@echo $(JAVA)\
	  -cp "$(INSTALL_DIR_N_FS)/mscviewer.jar" \
          com.cisco.mscviewer.io.ConvertFormat \
	  '$$@' >$(INSTALL_DIR)/bin/mscupgrade

	@echo $(JAVA)\
	  -cp "$(INSTALL_DIR_N_FS)/mscviewer.jar" \
          com.cisco.mscviewer.io.ConvertFormat \
          '%*' >$(INSTALL_DIR)/bin/mscupgrade.bat
                        
	@chmod 755 $(INSTALL_DIR)/bin/*
	@rm -rf $(INSTALL_DIR)/.[a-z]*
	@rm -f .gitignore .srclist .texlipse
	tar cfz $(VERSIONED_NAME).tgz -C $(INSTALL_PREFIX) $(VERSIONED_NAME)
	@echo "removing temporary dir $(INSTALL_DIR)"
	@rm -rf $(INSTALL_DIR)
    
all:  
	@echo "Building mscviewer java code..."
	@find  src -name *.java >.srclist
	@mkdir -p classes
	@$(JAVAC) -g -Xlint -classpath "src$Pthird-parties/$(SWINGX_JAR)$Pthird-parties/$(JYTHON_JAR)" -d classes @.srclist


clean:
	-@rm -rf classes bin .srclist
	-@rm -rf $(INSTALL_DIR)
	-@rm -f $(WS_TOOLS_DIR)/host_tools.$(TARGET).sentinel 

jar: all
	@echo "Packaging classes to jar file..."
	$(eval $(call create_temp_file))
	@echo "Manifest-Version: 1.1" >$(TEMPFILE)
	@echo "Created-By: Rattias" >>$(TEMPFILE)
	@echo "Class-Path: third-parties/$(JYTHON_JAR) third-parties/$(SWINGX_JAR)" >>$(TEMPFILE)
	@echo "Main-Class: com.cisco.mscviewer.Main" >>$(TEMPFILE1)
	@echo >>$(TEMPFILE)
	@$(JAR) cmf $(TEMPFILE) mscviewer.jar -C classes .
	@rm $(TEMPFILE)
    

		
