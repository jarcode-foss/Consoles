# Compiling Consoles

Maven will handle the majority of the work for you, and running build.sh (for Linux/OSX)
or build.bat (Windows) will produce artifacts if the source code compiles.

**note:** you may have to run `chmod a+x build.sh` to change file permissions before
running the build script (Linux only).

You will find the final .jar files for BungeeCord and Bukkit in the following folders:

- `PROJECT/target/bukkit-final`
- `PROJECT/target/bungee-final`

Where `PROJECT` is the root repository folder, containing the `pom.xml` file.

Do not try to use the jar files that are produced in the `target` folder, these artifacts _do not_ have dependancies packaged with them, so they will not work in a server.

