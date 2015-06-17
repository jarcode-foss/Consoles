#!/usr/bin/env bash
rm target/Consoles.zip
for FILENAME in target/mc-consoles-*; do rm ${FILENAME} consoles.jar; done
mvn package -Pbukkit -Dmaven.test.skip=true | egrep -v "(^\[WARNING\])|(already added\, skipping)"
mvn package -Pbungee -Dmaven.test.skip=true | egrep -v "(^\[WARNING\])|(already added\, skipping)"
cd target
cd bukkit-final/
for FILENAME in *; do mv ${FILENAME} consoles.jar; done
zip ../Consoles.zip consoles.jar
cd ../bungee-final/
for FILENAME in *; do mv ${FILENAME} bungee-consoles.jar; done
zip ../Consoles.zip bungee-consoles.jar
cd ../..