# Compiling Consoles

### Prerequisites

- You will need several server builds installed to your local repository. You can generate these by running `BuildTools.jar` with the `--rev` argument, followed by the server version. The versions required are:

	- 1.8.3-R0.1-SNAPSHOT (v1_8_R2)

	- 1.8.8-R0.1-SNAPSHOT (v1_8_R3)

- You need a maven installation. BuildTools will download one for itself, you can simply add its maven directory to your system path, or install maven normally (on most Linux distributions, you can run `apt-get install maven2`)

### Natives

`consoles-computers` contains C sources that need to be compiled into `libcomputerimpl.so`, which means if you're only interested in the core plugin, you need to remove the `consoles-computers` module from the root `pom.xml`.

If you are on Linux, there are two dependencies, which are libffi and LuaJIT (5.1). These are available on a number of repositories, on Debian/Ubuntu you should be able to run (assuming you are on a 64-bit system):

    sudo apt-get install build-essential libffi6 libluajit-5.1-2 libluajit-5.1-2-dev libluajit-5.1-2:i386 libffi6:i386 lib32z1

### Compiling

The maven configuration should do everything for you; running `mvn install` will generate artifacts in the modules' respective target directories. Consoles will have an usable plugin jar in the `consoles-core/target/final` folder, which has all the dependencies it needs to function.

You can use the builds for bungee straight from the `consoles-bungee/target` folder (it does not require any packaged dependencies), but if you try to use the jars in the `target` folder for other modules (instead of the jars in the `final` folder`), you will be missing a lot of dependencies that don't come with craftbukkit/spigot!

### Not compiling for/on Linux

If you're not compiling on Linux, you're going to need to be willing to spend a few hours getting it to compile for your platform. The code itself will compile through MinGW and on OSX, however:

 - on Windows, you will need to link the library to paths in your system pointing to `libffi-6.dll` and `lua51.dll`, which are obtainable by compiling the respective libraries or getting pre-compiled builds.

 - on OSX, you'll probably want homebrew installed so you can easily grab libffi6 and luajit

You'll also need to modify the `pom.xml` file for `consoles-computers` to reflect the platform you're trying to build for. Edit the configurations for `gcc-maven-plugin` with the proper parameters and `<targetPlatforms>` entries. For OSX, you should simply need to change the target to `OSX`, and on Windows you'll need to add `WIN32`/`WIN64` and change multiple parameters so the right include directories are added, and the correct libraries are linked against.

