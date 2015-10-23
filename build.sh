#!/usr/bin/env bash
mvn clean
mvn compile
mkdir consoles-computers/target/natives
cd consoles-computers
make all
cd ..
mvn install