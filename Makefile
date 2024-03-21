# Main target and filename of the executable
JARFILE := main.jar
OPENCARD=opencard.properties
OUT_DIR :=out

BUILD_DIR := build
SRC_DIR := src
LOG_DIR := logs
LIB_DIR := lib

MANIFEST_DIR:= META-INF
MANIFEST := $(MANIFEST_DIR)/MANIFEST.MF
MAIN_CLASS:= javachatnt.Main


#commands
ifeq ($(OS),Windows_NT)
	JAVADIR =C:\Program Files (x86)\Java\jdk1.8.0_202\bin
	JAVAC = "$(JAVADIR)\javac"
	JAR = "$(JAVADIR)\jar"
	JAVA = "$(JAVADIR)\java"
	DEVNULL = NUL

	MKDIR = md
	RM = rmdir /s /q
	CPR = xcopy /E /i
	CP = copy
	RUNNER= start.bat

	separator=;
else
#	JAVADIR := /usr/lib/jvm/java-8-openjdk/bin
	JAVADIR := /usr/bin
	JAVAC = $(JAVADIR)/javac
	JAR= $(JAVADIR)/jar
	JAVA= $(JAVADIR)/java
	DEVNULL = /dev/null

	MKDIR=mkdir -p
	RM=rm -rf	
	CPR=cp -r
	CP = cp
	RUNNER = start.sh

	separator=:
endif

# This makefile allows to compile automatically java files and produce a jar
rwildcard=$(foreach d,$(wildcard $1*),$(call rwildcard,$d/,$2)$(filter $(subst *,%,$2),$d))
# List of all the .java source files to compile
SRC := $(call rwildcard,$(SRC_DIR),*.java)
# List of all the .class object files to produce
OBJ := $(patsubst $(SRC_DIR)/%.java,$(BUILD_DIR)/%.class,$(SRC))

# List of jar files inside lib folder
JAR_LIB := $(call rwildcard,$(LIB_DIR),*.jar)
# List of jar file in manifest
JAR_NAME := $(patsubst $(LIB_DIR)/%.jar,%.jar,$(JAR_LIB))

noop=
space = $(noop) $(noop)
CLASSES_PATH:=$(subst $(space),$(separator),$(strip $(BUILD_DIR) $(JAR_LIB)))

# path of jar
OUT=$(OUT_DIR)/$(JARFILE)


all: $(BUILD_DIR) $(OBJ)


$(BUILD_DIR) $(MANIFEST_DIR):
	@echo Creating folder $@...
	@$(MKDIR) ${@:/=}

$(BUILD_DIR)/%.class: $(SRC_DIR)/%.java
	@echo Compiling $<...
	@$(JAVAC) -cp "$(CLASSES_PATH)" $< -d $(BUILD_DIR) -sourcepath $(SRC_DIR)


$(OUT): $(BUILD_DIR) $(OBJ) $(MANIFEST) $(OUT_DIR) $(OUT_DIR)/$(RUNNER)
	@echo Creating jar $@...
	@$(JAR) cvfm $(OUT) $(MANIFEST) -C $(BUILD_DIR) . > $(DEVNULL)
	@$(RM) $(MANIFEST_DIR)

$(MANIFEST): $(MANIFEST_DIR)
	@echo Generating manifest...
	@echo Manifest-Version: 1.0 >> $(MANIFEST)
	@echo Main-Class: $(MAIN_CLASS) >> $(MANIFEST)
	@echo Class-Path: $(JAR_NAME) >> $(MANIFEST)

$(OUT_DIR): $(LIB_DIR)
	@$(CPR) $(LIB_DIR) $(OUT_DIR)
	@$(CP) $(OPENCARD) $(OUT_DIR)

$(OUT_DIR)/$(RUNNER): $(OUT_DIR)
ifeq ($(OS),Windows_NT)
	@echo @echo off >> $(OUT_DIR)/$(RUNNER)
	@echo echo running $(JARFILE)... >> $(OUT_DIR)/$(RUNNER)
	@echo $(JAVA) -Djava.library.path=. -jar $(JARFILE) %%* >> $(OUT_DIR)/$(RUNNER)
	@echo pause >> $(OUT_DIR)/$(RUNNER)
else
	@echo "#!/bin/bash" >> $(OUT_DIR)/$(RUNNER)
	@echo "echo running $(JARFILE)..." >> $(OUT_DIR)/$(RUNNER)
	@echo "$(JAVA) -Djava.library.path=. -jar $(JARFILE) \$$@" >> $(OUT_DIR)/$(RUNNER)
	@chmod +x $(OUT_DIR)/$(RUNNER)
endif

clean: $(BUILD_DIR)
	@echo Cleaning build...
	@$(RM) $(BUILD_DIR)

cleanlogs:
	@echo Removing logs...
	@$(RM) $(LOG_DIR)

release: $(OUT)

unrelease: $(OUT_DIR)
	@echo Cleaning release...
	@$(RM) $(OUT_DIR)

server:  $(BUILD_DIR) $(OBJ)
	@$(JAVA) -cp "$(CLASSES_PATH)" $(MAIN_CLASS) serve 2000 2001:ADMIN

generator:  $(BUILD_DIR) $(OBJ)
	@$(JAVA) -cp "$(CLASSES_PATH)" $(MAIN_CLASS) generate $(USER)

client:  $(BUILD_DIR) $(OBJ)
	@$(JAVA) -cp "$(CLASSES_PATH)" $(MAIN_CLASS) connect 2000

card-client:  $(BUILD_DIR) $(OBJ)
	@$(JAVA) -Djava.library.path=$(LIB_DIR) -cp "$(CLASSES_PATH)" $(MAIN_CLASS) card-connect 2000
