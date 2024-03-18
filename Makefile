# This makefile allows to compile automatically java files and produce a jar
rwildcard=$(foreach d,$(wildcard $1*),$(call rwildcard,$d/,$2)$(filter $(subst *,%,$2),$d))

# Main target and filename of the executable
JARFILE := main.jar
OUT_DIR :=out

BUILD_DIR := build
SRC_DIR := src
LOG_DIR := logs
LIB_DIR := lib

MANIFEST_DIR:= META-INF
MANIFEST := $(MANIFEST_DIR)/MANIFEST.MF
MAIN_CLASS:= Main

#commands
ifeq ($(OS),Windows_NT)
	JAVADIR :=C:\Program Files (x86)\Java\jdk1.8.0_202\bin
	JAVAC := '$(JAVADIR)\javac'
	JAR := '$(JAVADIR)\jar'
	JAVA := '$(JAVADIR)\java'

	MKDIR := md
	RMRF := rmdir /s /q

	separator=;
else
	JAVAC = javac
	JAR= jar
	JAVA=java

	MKDIR=mkdir -p
	RMRF=rm -rf	

	separator=:
endif


# List of all the .java source files to compile
SRC := $(call rwildcard,$(SRC_DIR),*.java)
# List of all the .class object files to produce
OBJ := $(patsubst $(SRC_DIR)/%.java,$(BUILD_DIR)/%.class,$(SRC))

# List of jar files inside lib folder
JAR_LIB := $(call rwildcard,$(LIB_DIR),*.jar)
# List of jar file in out folder
JAR_OUT := $(patsubst $(LIB_DIR)/%.jar,$(OUT_DIR)/%.jar,$(JAR_LIB))
JAR_CLASS_PATHS := $(patsubst $(LIB_DIR)/%.jar,%.jar,$(JAR_LIB))

noop=
space = $(noop) $(noop)
CLASSES_PATH:=$(subst $(space),$(separator),$(strip $(BUILD_DIR) $(JAR_LIB)))

# path of jar
OUT=$(OUT_DIR)/$(JARFILE)


all: $(OBJ)


$(BUILD_DIR) $(OUT_DIR) $(MANIFEST_DIR):
	@echo Creating folder $@...
	@$(MKDIR) ${@:/=}

$(BUILD_DIR)/%.class: $(SRC_DIR)/%.java
	@echo Compiling $<...
	@$(JAVAC) -cp "$(CLASSES_PATH)" $< -d $(BUILD_DIR) -sourcepath $(SRC_DIR)

$(OUT_DIR)/%.jar: $(LIB_DIR)/%.jar
	@echo Copying $< into $@...
	@cp $< $@

$(OUT): $(OUT_DIR) $(OBJ) $(JAR_OUT) $(MANIFEST)
	@echo Creating jar $@...
	@$(JAR) cvfm $(OUT) $(MANIFEST) -C $(BUILD_DIR) .

$(MANIFEST): $(MANIFEST_DIR)
	@echo Generating manifest...
	@echo Manifest-Version: 1.0 >> $(MANIFEST)
	@echo Main-Class: $(MAIN_CLASS) >> $(MANIFEST)
	@echo Class-Path: $(JAR_CLASS_PATHS) >> $(MANIFEST)

clean:
	@echo Cleaning build...
	@$(RMRF) $(BUILD_DIR) $(OUT_DIR) $(MANIFEST_DIR)

cleanlogs:
	@echo Removing logs...
	@$(RMRF) $(LOG_DIR)

jar: $(OUT)

server: $(OBJ)
	@$(JAVA) -cp "$(CLASSES_PATH)" Main serve 2000 2001:ADMIN

generator: $(OBJ)
	@$(JAVA) -cp "$(CLASSES_PATH)" Main generate $(USER)

client: $(OBJ)
	@$(JAVA) -cp "$(CLASSES_PATH)" Main connect 2000