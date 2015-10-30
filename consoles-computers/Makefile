

CFLAGS = -O2 -D_REENTRANT

LDFLAGS = -Lsrc/main/resources -lluajit-5.1 -lffi -I"$(JAVA_HOME)/include" -I"$(JAVA_HOME)/include/linux" -I"target/include" -I"/usr/include/luajit-2.0" -I"/usr/include/luajit" -shared -fPIC

# source folder
SOURCES := $(wildcard src/main/c/*.c)
OBJECTS := $(addprefix src/main/c/,$(notdir $(SOURCES:.c=.o)))

# we're tossing the library in target/natives, maven will
#  expect it to be there during the packaging phase.
LIBRARY_NAME = computerimpl
LIBRARY_PATH = target/natives/lib$(LIBRARY_NAME).so

.PHONY: all
all: $(LIBRARY_PATH)

$(LIBRARY_PATH): $(OBJECTS)
	$(CC) ${LDFLAGS} -o $@ $^

$(SOURCES:.c=.o):%.o:%.c
	$(CC) $(CFLAGS) -MM $< >$@

include $(SOURCS:.c=.o)