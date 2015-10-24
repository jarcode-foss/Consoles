CFLAGS = -D -O2
LDFLAGS = -fPIC -Lsrc/main/resources -lluajit -I"$(JAVA_HOME)/include" -I"$(JAVA_HOME)/include/linux" -I"target/include" -I"include" -rpath='\$\$ORIGIN/..' -shared

SOURCES := $(wildcard src/main/c/*.c)
OBJECTS := $(addprefix src/main/c/,$(notdir $(SOURCES:.cpp=.o)))
LIBRARY_NAME = computerimpl
LIBRARY_PATH = target/natives/lib$(LIBRARY_NAME).so

.PHONY: all
all: $(LIBRARY_PATH)

$(LIBRARY_PATH): $(OBJECTS)
	$(CC) ${LDFLAGS} -o $@ $^

$(SOURCES:.c=.d):%.d:%.c
	$(CC) $(CFLAGS) -MM $< >$@

include $(SOURCS:.c=.d)