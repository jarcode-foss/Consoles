# Compiling Consoles

Maven will handle the majority of the work for you, and running build.sh (for Linux/OSX)
or build.bat (Windows) will produce artifacts if the source code compiles.

**Note for Linux users:** you may have to run `chmod a+x build.sh` to change file permissions
 for the build script before running it.

You will find the final .jar files for BungeeCord and Bukkit in the following folders:

- `PROJECT/target/bukkit-final`
- `PROJECT/target/bungee-final`

Where `PROJECT` is the root repository folder, containing the `pom.xml` file.