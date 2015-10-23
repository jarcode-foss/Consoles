CFLAGS = -D -O2
LDFLAGS = -fPIC -I"$(JAVA_HOME)/include" -I"$(JAVA_HOME)/include/linux" -I"target/include" -shared

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