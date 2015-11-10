# Debuging Consoles

This document is intended for those who are working with source code of Consoles, notably the `consoles-computers` module. For the most part, stack traces will lead you to the problem and you can pray that the issue resides in java source. If the issue lies in a native method (via LuaN):

- try to eliminate that the issue could be caused by incorrect use on the java side.

- set up GDB for `consoles-computers`. You should be able to do this in the `config.yml`, and the default commands should work on your system (given you _are_ on Linux).

- set breakpoints via GDB and/or interrupt the relevant signals. Interrupting SIGSEGV's will be near-impossible, since the JVM will be throwing dozens of them due to signal handling related to GC cycles and JIT compilation.

### Debugging failed tests

`consoles-computers` has tests for the native layer that can fail, so starting a GDB hook for those tests may be nessecary. A benefit of debugging the native layer during the test phase is that you can avoid most bogus SIGSEGV's from the server that would normally be running.

To debug during the test phase, change `DEBUG` to `true` in `consoles-computers/src/test/NativeLayerTest.java`.

### Attaching GDB

Your system may not allow GDB to attach to non-child processes (a kernel security measure of `ptrace`), to fix this you either need to set kernel options to allow GDB to ptrace any process, or run as root. If you are on Ubunutu/Debian, the following is a secure way to grant GDB these permissions:

    sudo apt-get install libcap2-bin
    sudo setcap cap_sys_ptrace=eip /usr/bin/gdb