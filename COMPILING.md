# Compiling Consoles

### Prerequisites

- You will need to run `BuildTools.jar` at least once on your local machine, as it will install entries for spigot and craftbukkit in your local maven repository.

- You need a maven installation. BuildTools will download one for itself, you can simply add its maven directory to your system path, or install maven normally (on most Linux distributions, you can run `apt-get install maven2`)

### Compiling

Maven will handle the rest of the work for you; running build.sh (for Linux and OSX) or build.bat (Windows) will produce artifacts if the source code compiles.

**note:** you may have to run `chmod a+x build.sh` to change file permissions before running the build script (Linux only).

You will find the final .jar files for BungeeCord and Bukkit in the following folders:

- `PROJECT/target/bukkit-final`
- `PROJECT/target/bungee-final`

Where `PROJECT` is the root repository folder, containing the `pom.xml` file.

Do not try to use the jar files that are produced in the `target` folder, these artifacts _do not_ have dependencies packaged with them, so they will not work in a server.

