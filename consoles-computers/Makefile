

CFLAGS = -O2 -D_REENTRANT

# linking against luajit, which is in our resources folder,
#   which needs a relative path to be resolved at runtime.
#
# we also need to include JNI headers, so we just add those
#  (and the linux-specific headers). Make sure $JAVA_HOME
#  is set!
LDFLAGS = -fPIC -Lsrc/main/resources -lluajit -lffi -I"$(JAVA_HOME)/include" -I"$(JAVA_HOME)/include/linux" -I"target/include" -I"/usr/include/luajit-2.0" -I"/usr/include/luajit" -Wl,-rpath='$$ORIGIN/..' -shared

# source folder
SOURCES := $(wildcard src/main/c/*.c)
OBJECTS := $(addprefix src/main/c/,$(notdir $(SOURCES:.cpp=.o)))

# we're tossing the library in target/natives, maven will
#  expect it to be there during the packaging phase.
LIBRARY_NAME = computerimpl
LIBRARY_PATH = target/natives/lib$(LIBRARY_NAME).so

.PHONY: all
all: $(LIBRARY_PATH)

$(LIBRARY_PATH): $(OBJECTS)
	$(CC) ${LDFLAGS} -o $@ $^

$(SOURCES:.c=.d):%.d:%.c
	$(CC) $(CFLAGS) -MM $< >$@

include $(SOURCS:.c=.d)