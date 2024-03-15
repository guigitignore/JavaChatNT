# This makefile allows to compile automatically java files and produce a jar

#commands
	ifeq ($(OS),Windows_NT)
	JAVADIR=C:\Program Files (x86)\Java\jdk1.8.0_202\bin
	JAVAC="$(JAVADIR)\javac"
	JAR="$(JAVADIR)\jar"
	JAVA="$(JAVADIR)\java"

	MKDIR=md
	RMRF=rmdir /s /q

	BOUNCY_CASTLE=.\lib\bcprov-jdk18on-177.jar
else
	JAVAC = javac
	JAR= jar
	JAVA=java

	MKDIR=mkdir -p
	RMRF=rm -rf

	BOUNCY_CASTLE=lib/bcprov-jdk18on-177.jar
endif


MANIFEST= META-INF/MANIFEST.MF

# Main target and filename of the executable
JARFILE = main.jar
OUT_DIR=out

SRC_DIR=src
BUILD_DIR = build
LOG_DIR= logs


# Recursive Wildcard function
rwildcard=$(foreach d,$(wildcard $1*),$(call rwildcard,$d/,$2)$(filter $(subst *,%,$2),$d))
# Remove duplicate function
uniq = $(if $1,$(firstword $1) $(call uniq,$(filter-out $(firstword $1),$1))) 

# List of all the .java source files to compile
SRC = $(call rwildcard,$(SRC_DIR),*.java)

# List of all the .class object files to produce
OBJ = $(patsubst $(SRC_DIR)/%.java,$(BUILD_DIR)/%.class,$(SRC))
OBJ_DIRS = $(call uniq, $(dir $(OBJ)))

# path of jar
OUT=$(OUT_DIR)/$(JARFILE)


all: $(OBJ_DIRS) $(OBJ)


$(OBJ_DIRS) $(OUT_DIR):
	@echo "Creating folder $@..."
	$(MKDIR) ${@:/=}


$(BUILD_DIR)/%.class: $(SRC_DIR)/%.java
	@echo "Compiling $<..."
	@$(JAVAC) -cp $(BOUNCY_CASTLE) $< -d $(BUILD_DIR) -sourcepath $(SRC_DIR)

$(OUT): $(OBJ) $(OUT_DIR)
	@echo "Creating jar $@..."
	@cd $(BUILD_DIR) && $(JAR) cvfm ../$(OUT) ../$(MANIFEST) *.class
	@cp $(BOUNCY_CASTLE) $(OUT_DIR)

clean:
	@echo "Cleaning build"
	@$(RMRF) $(BUILD_DIR) $(OUT_DIR)

cleanlogs:
	@echo "Removing logs..."
	@$(RMRF) $(LOG_DIR)

jar: $(OUT)

runjar: $(OUT)
	@echo "Running $(OUT)..."
	@$(JAVA) -jar $(OUT_DIR)/$(JARFILE)

server: $(OBJ)
	@$(JAVA) -cp $(BUILD_DIR):$(BOUNCY_CASTLE) Main serve 2000 2001:ADMIN

generator: $(OBJ)
	@$(JAVA) -cp $(BUILD_DIR):$(BOUNCY_CASTLE) Main generate $(USER)

client: $(OBJ)
	@$(JAVA) -cp $(BUILD_DIR):$(BOUNCY_CASTLE) Main connect 2000