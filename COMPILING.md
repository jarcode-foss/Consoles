# Compiling Consoles

### Prerequisites

- You will need several server builds installed to your local repository. You can generate these by running `BuildTools.jar` with the `--rev` argument, followed by the server version. The versions required are:

	- 1.8.3-R0.1-SNAPSHOT (v1_8_R2)

	- 1.8.8-R0.1-SNAPSHOT (v1_8_R3)

- You need a maven installation. BuildTools will download one for itself, you can simply add its maven directory to your system path, or install maven normally (on most Linux distributions, you can run `apt-get install maven2`)

### Natives

`consoles-computers` contains natives that only compile and run on Linux, which means if you're trying to compile the plugin for any other platform, you need to remove the `consoles-computers` module from the root `pom.xml`.

If you are on Linux, there are two dependencies, which are libffi and LuaJIT (5.1). These are available on a number of repositories, on Ubuntu you should be able to run:

    sudo apt-get install libffi6 luajit

### Compiling

The maven configuration should do everything for you; running `mvn install` will generate artifacts in the modules' respective target directories. Consoles will have an usable plugin jar in the `consoles-core/target/final` folder, which has all the dependencies it needs to function.

You can use the builds for bungee straight from the `consoles-bungee/target` folder (it does not require any packaged dependencies), but if you try to use the jars in the `target` folder for other modules (instead of the jars in the `final` folder`), you will be missing a lot of dependencies that don't come with craftbukkit/spigot!

### Porting

If you wish to port `consoles-computers` to Windows, you need to:

- actually port the code. I will only compile builds with `gcc` using mingw or cygwin.
- modify the `pom.xml` in `consoles-computers` to have the compile and linking goals for the JNI library of your port, using my `gcc-maven-plugin`.

For OSX, it is very likely that `consoles-computers` will compile without an issue. The only OS-specific functions that it uses are from `dlsym.h` and pthreads, which are available on OSX. If there is a demand for OSX support, I will find a way to compile it on Linux.

