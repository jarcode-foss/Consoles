# Compiling Consoles

### Prerequisites

- You will need to run `BuildTools.jar` at least once on your local machine, as it will install entries for spigot and craftbukkit in your local maven repository.

- You need a maven installation. BuildTools will download one for itself, you can simply add its maven directory to your system path, or install maven normally (on most Linux distributions, you can run `apt-get install maven2`)

### Compiling

Maven will handle the rest of the work for you; running `mvn package` will generate artifacts in the modules' respective target directories. Consoles will have an usable plugin jar in the `craftbukkit/target/final` folder, which has all the dependencies it needs to function.

You can use the builds for bungee straight from the `bungee/target` folder (it does not require packaged dependencies), but if you try to use the jars in the `craftbukkit/target` folder (instead of the jars in the `final` folder`), you will be missing a lot of dependencies that don't come with craftbukkit/spigot!

