#!/usr/bin/env bash

# This script is used to build and package Consoles locally


rm target/Consoles.zip
cd target
for FILENAME in *.jar; do rm -f ${FILENAME}; done
cd bukkit-final
for FILENAME in *.jar; do rm -f ${FILENAME}; done
cd ../../bungee/target/
for FILENAME in *.jar; do rm -f ${FILENAME}; done
cd ../..
mvn package -Dmaven.test.skip=true | egrep -v "(^\[WARNING\])|(already added\, skipping)"
mvn package -pl bungee -am -Dmaven.test.skip=true | egrep -v "(^\[WARNING\])|(already added\, skipping)"
cd target/bukkit-final/
for FILENAME in *; do mv ${FILENAME} consoles.jar; done
zip ../Consoles.zip consoles.jar
cd ../../bungee/target/
for FILENAME in *.jar; do mv ${FILENAME} bungee-consoles.jar; done
zip ../../target/Consoles.zip bungee-consoles.jar
cd ../..