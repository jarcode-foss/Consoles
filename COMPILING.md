# Compiling Consoles

### Prerequisites

- You will need several server builds installed to your local repository. You can generate these by running `BuildTools.jar` with the `--rev` argument, followed by the server version. The versions required are:

	- 1.8.3-R0.1-SNAPSHOT (v1_8_R2)

	- 1.8.8-R0.1-SNAPSHOT (v1_8_R3)

- You need a maven installation. BuildTools will download one for itself, you can simply add its maven directory to your system path, or install maven normally (on most Linux distributions, you can run `apt-get install maven2`)

### Natives

`consoles-computers` contains natives that only compile and run on Linux, which means if you're trying to compile the plugin for any other platform, you need to remove the `consoles-computers` from the root `pom.xml`. If you are compiling on another platform, run `mvn install` instead of using a the build script.

If you are on Linux, there are two dependencies. The first is LuaJIT, which you do not need to provide (although you can replace it with your own build). The second is `libffi`, which _should_ come with your distribution. If it does not, you need to install it through your package manager.

### Compiling

The compile script will handle the rest of the work for you; running `./build.sh` will generate artifacts in the modules' respective target directories. Consoles will have an usable plugin jar in the `consoles-core/target/final` folder, which has all the dependencies it needs to function.

You can use the builds for bungee straight from the `consoles-bungee/target` folder (it does not require any packaged dependencies), but if you try to use the jars in the `target` folder for other modules (instead of the jars in the `final` folder`), you will be missing a lot of dependencies that don't come with craftbukkit/spigot!

