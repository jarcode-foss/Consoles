rm Consoles.zip
mvn clean
mvn package -Pbukkit
mvn package -Pbungee
cd target
cd bukkit-final/
for FILENAME in *; do mv $FILENAME consoles.jar; done
cd ..
cd bungee-final/
for FILENAME in *; do mv $FILENAME bungee-consoles.jar; done
cd ..
zip Consoles.zip target/bukkit-final/* target/bungee-final/*