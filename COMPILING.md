# Compiling Consoles

### Prerequisites

- You will need several server builds installed to your local repository. You can generate these by running `BuildTools.jar` with the `--rev` argument, followed by the server version.

	- 1.8.3-R0.1-SNAPSHOT (v1_8_R2)

	- 1.8.8-R0.1-SNAPSHOT (v1_8_R3)

- You need a maven installation. BuildTools will download one for itself, you can simply add its maven directory to your system path, or install maven normally (on most Linux distributions, you can run `apt-get install maven2`)

### Compiling

Maven will handle the rest of the work for you; running `mvn install` will generate artifacts in the modules' respective target directories. Consoles will have an usable plugin jar in the `consoles-core/target/final` folder, which has all the dependencies it needs to function.

You can use the builds for bungee straight from the `consoles-bungee/target` folder (it does not require any packaged dependencies), but if you try to use the jars in the `target` folder for other modules (instead of the jars in the `final` folder`), you will be missing a lot of dependencies that don't come with craftbukkit/spigot!

